package dev.blamspot.jcode.debug

import dev.blamspot.jcode.core.debug.DapEvaluation
import dev.blamspot.jcode.core.debug.DapStackFrame
import dev.blamspot.jcode.core.debug.DapVariable
import dev.blamspot.jcode.core.debug.DapTransport
import dev.blamspot.jcode.core.debug.DebugSession
import dev.blamspot.jcode.core.debug.DebugState
import dev.blamspot.jcode.core.debug.StoppedInfo
import dev.blamspot.jcode.core.distro.DebugEngineCatalog
import dev.blamspot.jcode.core.distro.DebugEngineEntry
import dev.blamspot.jcode.core.distro.DistroService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** A source location the debugger is stopped at. [line] is 0-based (editor convention). */
data class DebugLocation(val hostPath: String, val line: Int)

/** A flattened variables/scopes row for the Variables panel. depth 0 = scope, 1 = variable. */
data class VariableRow(val name: String, val value: String, val type: String?, val depth: Int)

/**
 * Orchestrates a single debug session: resolves the engine for a file, launches the adapter (via the
 * distro), pushes breakpoints, and exposes UI state (call stack, variables, output, stopped location).
 * Mirrors the LSP client's shape but for DAP. The transport is a proot child process's stdio pipes.
 */
class DebugController(
    private val distroService: DistroService,
    parentScope: CoroutineScope,
) {
    // Every DAP request writes to the adapter's transport, and for a TCP adapter (js-debug) that is a
    // socket write — illegal on Android's main thread. The caller passes viewModelScope, which is
    // Dispatchers.Main, so requests raised from here (stackTrace/scopes/variables after a `stopped`
    // event, step/continue) threw NetworkOnMainThreadException inside the transport's catch-all and
    // were silently dropped: js-debug paused correctly but the call stack and variables stayed empty.
    // Keep the caller's lifecycle (cancellation still cascades) but do the work off the main thread.
    private val scope = CoroutineScope(parentScope.coroutineContext + Dispatchers.IO)

    var resolveKitPlan: (suspend (DebugEngineEntry,String,String)->dev.suicode.packages.ResolvedDebugPlan)? = null
    private var preparation: kotlinx.coroutines.Job? = null
    private val generation = java.util.concurrent.atomic.AtomicLong()
    @Volatile private var ownerPackage: String? = null
    private var session: DebugSession? = null
    /** The session that owns the current stopped thread (root, or a js-debug child) — step/continue target. */
    private var activeSession: DebugSession? = null
    /** js-debug child sessions (the debuggee runs here); empty for single-session adapters. */
    private val children = java.util.concurrent.CopyOnWriteArrayList<DebugSession>()
    private val observerJobs=java.util.concurrent.ConcurrentHashMap<DebugSession,java.util.concurrent.CopyOnWriteArrayList<kotlinx.coroutines.Job>>()
    private fun observe(session: DebugSession,job: kotlinx.coroutines.Job) { observerJobs.computeIfAbsent(session) { java.util.concurrent.CopyOnWriteArrayList() }.add(job) }
    private fun clearObservers() { observerJobs.values.forEach { jobs -> jobs.forEach { it.cancel() } };observerJobs.clear() }
    /** Current breakpoints (host path -> 0-based lines), so freshly-spawned child sessions get them too. */
    @Volatile private var launchSource: String?=null
    @Volatile private var launchProject: String?=null
    private var currentBps: Map<String, Set<Int>> = emptyMap()
    /** Distro cwd of the active launch, reused as the child sessions' project root. */
    private var sessionCwd: String = ""
    /** The root js-debug adapter's TCP port, used as a fallback child-server port. */
    private var rootTcpPort: Int? = null
    /** Guards [endSession] so a terminated event + child cleanup can't tear down twice. */
    private var ended = false
    /** Device-side state of an Android attach, undone when the session ends. */
    private var androidAttach: AndroidDebugAttach? = null

    private val _state = MutableStateFlow(DebugState.DISCONNECTED)
    val state: StateFlow<DebugState> = _state.asStateFlow()
    private val _callStack = MutableStateFlow<List<DapStackFrame>>(emptyList())
    val callStack: StateFlow<List<DapStackFrame>> = _callStack.asStateFlow()
    private val _variables = MutableStateFlow<List<VariableRow>>(emptyList())
    val variables: StateFlow<List<VariableRow>> = _variables.asStateFlow()
    private val _output = MutableStateFlow<List<String>>(emptyList())
    val output: StateFlow<List<String>> = _output.asStateFlow()
    private val _location = MutableStateFlow<DebugLocation?>(null)
    val currentLocation: StateFlow<DebugLocation?> = _location.asStateFlow()

    /**
     * How a session should be launched, after any per-language preparation (e.g. building a .NET
     * project). [tcpPort] non-null selects the TCP transport (js-debug); null uses the adapter's stdio.
     */
    private data class LaunchPlan(
        val distroCwd: String,
        val config: JSONObject,
        val adapterCommand: String,
        val tcpPort: Int?,
        /** Adapter process user override (netcoredbg needs root for /root/.dotnet); null = runtime user. */
        val user: String? = null,
        /** Prepended to the adapter's PATH (e.g. /root/.dotnet). */
        val adapterPath: String = "",
        /** DAP request: "launch" spawns the debuggee, "attach" joins one that is already running. */
        val request: String = "launch",
    )

    /**
     * Why a C# session dies, in the user's words. Established on-device rather than guessed: under proot
     * a process may only ptrace its OWN children — PTRACE_ATTACH to any other pid returns ESRCH even
     * though the pid exists. That kills both of netcoredbg's routes. Attach reports
     * CORDBG_E_DEBUG_COMPONENT_MISSING because dbgshim cannot read the target at all, and launch, which
     * does spawn its own child, still exits without answering (its own --interpreter=cli segfaults the
     * same way, with JCode out of the picture) on net8.0, net9.0 and net10.0 alike. Building and RUNNING
     * .NET works fine, so point there instead of leaving a bare adapter error on screen.
     */
    /** Start debugging [hostPath] (its language picks the engine) with the current [bps] breakpoints. */
    @Synchronized fun startDebug(hostPath: String, projectDir: String, bps: Map<String, Set<Int>>) {
        stop()
        launchSource=hostPath;launchProject=projectDir
        currentBps = bps
        ended = false
        val engine = engineForFile(hostPath, projectDir)
        _output.value = emptyList()
        if (engine == null) {
            pushOutput("No debug engine is installed for ${hostPath.substringAfterLast('/')}.\n")
            _state.value = DebugState.ERROR
            return
        }
        _state.value = DebugState.STARTING
        pushOutput("Preparing ${engine.name}…\n")
        ownerPackage = engine.ownerPackage
        val ticket = generation.get()
        preparation = scope.launch {
            val plan = try { prepareLaunch(engine, hostPath, projectDir) }
                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (e: Exception) { if (generation.get()==ticket) pushOutput("Debug setup failed: ${e.message}\n"); null }
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            if (generation.get()!=ticket) return@launch
            if (plan == null) {
                _state.value = DebugState.ERROR
                return@launch
            }
            synchronized(this@DebugController) { if (generation.get()==ticket) beginSession(engine, plan, hostPath) }
        }
    }

    private fun beginSession(engine: DebugEngineEntry, plan: LaunchPlan, hostPath: String) {
        val transportFactory: (String) -> DapTransport? = { command ->
            android.util.Log.d(
                "JCodeDAP-adapter",
                "spawn user=${plan.user ?: "<default>"} cwd=${plan.distroCwd} cmd=$command",
            )
            if (engine.transport=="lsp" && plan.tcpPort!=null) {
                SocketTransport.connect("127.0.0.1",plan.tcpPort,12_000L)
            } else {
            val proc = distroService.spawnStdioProcess(
                command, workdir = plan.distroCwd, userOverride = plan.user, extraPath = plan.adapterPath,owner="sui-kit:${engine.ownerPackage}/dap",
            )
            when {
                proc == null -> null
                // TCP adapters (js-debug) listen on a port; connect a socket to it once it's up.
                plan.tcpPort != null -> TcpTransport.connect("127.0.0.1", plan.tcpPort, proc, 12_000L)
                else -> ProcessTransport(proc)
            }
            }
        }
        sessionCwd = plan.distroCwd
        rootTcpPort = plan.tcpPort
        val s = DebugSession(engine.debugType, plan.distroCwd, transportFactory)
        session = s
        activeSession = s
        s.onOutput = { _, text -> if (session===s) pushOutput(text) }
        s.onTerminated = { if (session===s) endSession() }
        // js-debug (multi-session): the root adapter asks us — via a `startDebugging` reverse request —
        // to open the CHILD session where the debuggee runs and breakpoints bind. Single-session adapters
        // (python/coreclr/lldb) never fire this, so this is inert for them.
        s.onStartDebugging = { request, config -> spawnChild(request, config) }
        // While preparing we hold STARTING; ignore the fresh session's initial DISCONNECTED so the
        // panel doesn't flicker back to the launch row between build and adapter start.
        observe(s,scope.launch { s.state.collect { if (session===s && !(it == DebugState.DISCONNECTED && _state.value == DebugState.STARTING)) _state.value = it } })
        observe(s,scope.launch { s.stopped.collect { st -> if (st != null) onStopped(s, st) else clearStoppedView() } })

        val distroBreakpoints = distroBps() // DAP lines are 1-based; applied on `initialized`
        pushOutput("Starting ${engine.name} on ${hostPath.substringAfterLast('/')}…\n")
        scope.launch {
            if(session!==s) return@launch
            runCatching { s.start(plan.adapterCommand, plan.request, plan.config, distroBreakpoints) }
                .onFailure { pushOutput("Debug failed: ${it.message}\n"); _state.value = DebugState.ERROR }
            // start() swallows a failed adapter launch (bad transport / handshake) into DISCONNECTED
            // without rethrowing, and the STARTING guard on the state collector can eat that final
            // DISCONNECTED — leaving the panel stuck on "Starting…" forever. If we never advanced past
            // STARTING, the adapter never became reachable: surface it as an error instead of hanging.
            if (_state.value == DebugState.STARTING) {
                pushOutput(
                    "Couldn't reach the ${engine.name} debug adapter — it started but the connection " +
                        "timed out. See the log (tag JCodeDAP-adapter) for details.\n",
                )
                _state.value = DebugState.ERROR
            }
        }
    }

    private suspend fun prepareLaunch(engine: DebugEngineEntry, hostPath: String, projectDir: String): LaunchPlan {
        val result=resolveKitPlan?.invoke(engine,hostPath,projectDir)
            ?: throw dev.blamspot.jcode.core.debug.DebugException("No enabled native IDE Kit provides this launch workflow")
        return LaunchPlan(result.cwd,JSONObject(result.launch.toString()),engine.adapterCommand,tcpPort=result.port)
    }

    /** Current breakpoints as distro-path -> 1-based lines (DAP convention). */
    private fun distroBps(): Map<String, List<Int>> =
        currentBps.mapKeys { hostToDistro(it.key) }.mapValues { e -> e.value.sorted().map { it + 1 } }

    /**
     * js-debug multi-session: open a CHILD DAP session by connecting to the `__jsDebugChildServer` port
     * the root adapter handed us. The debuggee runs and breakpoints bind in the child, so its stopped /
     * call-stack / variables / output feed the same UI; [activeSession] follows whichever child last
     * stopped so step/continue target the right one. Children can nest (workers / child processes).
     */
    @Synchronized private fun spawnChild(request: String, config: JSONObject) {
        if(ended || session==null) return
        val port = config.optString("__jsDebugChildServer", "").toIntOrNull() ?: rootTcpPort
        if (port == null) {
            pushOutput("js-debug requested a child session without a server port; cannot attach.\n")
            return
        }
        val childType = config.optString("type", session?.debugType ?: "pwa-node")
        // We advertise no runInTerminal support and reject it, so force internalConsole (program output
        // via DAP `output` events) — otherwise a propagated terminal console would strand the debuggee.
        config.put("console", "internalConsole")
        val childFactory: (String) -> DapTransport? = { _ -> SocketTransport.connect("127.0.0.1", port, 12_000L) }
        val child = DebugSession(childType, sessionCwd, childFactory)
        children.add(child)
        activeSession = child
        child.onOutput = { _, text -> pushOutput(text) }
        child.onStartDebugging = { req, cfg -> spawnChild(req, cfg) }
        child.onTerminated = { onChildTerminated(child) }
        observe(child,scope.launch { child.stopped.collect { st -> if (st != null) onStopped(child, st) else clearStoppedView() } })
        // Only the child's STOPPED/RUNNING drive the UI; its start-up states would flicker the panel.
        observe(child,scope.launch {
            child.state.collect { st ->
                if (st == DebugState.STOPPED || st == DebugState.RUNNING) _state.value = st
            }
        })
        scope.launch {
            runCatching { child.start(adapterCommand = "", request = request, configuration = config, breakpoints = distroBps()) }
            // start() swallows its own failures into DISCONNECTED/ERROR; if the child never reached a live
            // session (e.g. it couldn't connect to the child server), clean it up so it doesn't hang the run.
            if (child.state.value == DebugState.DISCONNECTED || child.state.value == DebugState.ERROR) {
                pushOutput("Child debug session couldn't connect to the js-debug child server.\n")
                onChildTerminated(child)
            }
        }
    }

    private fun onChildTerminated(child: DebugSession) {
        if (activeSession === child) { activeSession = session; _location.value = null }
        children.remove(child)
        observerJobs.remove(child)?.forEach { it.cancel() }
        runCatching { child.close() }
        // The debuggee(s) ended once no child remains — end the whole session.
        if (children.isEmpty()) endSession()
    }

    private fun onStopped(s: DebugSession, st: StoppedInfo) {
        activeSession = s
        scope.launch {
            runCatching {
                val frames = s.stackTrace(st.threadId)
                _callStack.value = frames
                val top = frames.firstOrNull()
                _location.value = top?.sourcePath?.let { DebugLocation(it, (top.line - 1).coerceAtLeast(0)) }
                if (top != null) refreshVariables(s, top.id)
            }
        }
    }

    /**
     * Fetch each non-expensive scope's variables for [frameId], publishing incrementally so a slow or
     * large scope (e.g. Globals) never hides the ones already resolved. Resilient to one scope failing.
     */
    private suspend fun refreshVariables(s: DebugSession, frameId: Int) {
        val scopes = runCatching { s.scopes(frameId) }.getOrDefault(emptyList())
        val rows = mutableListOf<VariableRow>()
        _variables.value = emptyList()
        for (sc in scopes) {
            if (sc.expensive || sc.variablesReference == 0) continue
            rows.add(VariableRow(sc.name, "", null, depth = 0))
            _variables.value = rows.toList()
            val vars = runCatching { s.variables(sc.variablesReference) }.getOrDefault(emptyList())
            for (v in vars.take(200)) rows.add(VariableRow(v.name, v.value, v.type, depth = 1))
            _variables.value = rows.toList()
        }
    }

    private fun clearStoppedView() {
        _location.value = null
        _callStack.value = emptyList()
        _variables.value = emptyList()
    }

    fun sourceMoved(from: String) {
        if(launchSource?.let { it==from || it.startsWith(from+"/") }==true || launchProject?.let { from==it || from.startsWith(it+"/") || it.startsWith(from+"/") }==true) {
            stop();pushOutput("Debug source moved/deleted; launch again using its current path.\n")
        }
    }

    /** Called when the user toggles a breakpoint; remembers it and pushes to every live session. */
    fun onBreakpointsChanged(hostPath: String, lines: Set<Int>) {
        currentBps = currentBps.toMutableMap().apply { if (lines.isEmpty()) remove(hostPath) else put(hostPath, lines) }
        val distroPath = hostToDistro(hostPath)
        val distroLines = lines.sorted().map { it + 1 }
        // Breakpoints bind in the child sessions (js-debug); harmless on a single-session root.
        val targets = buildList { session?.let { add(it) }; addAll(children) }
        targets.forEach { s -> scope.launch { runCatching { s.setBreakpoints(distroPath, distroLines) } } }
    }

    fun resume() = withThread { s, t -> s.continueThread(t) }

    /** Interrupt a running debuggee (DAP `pause`). Optional in the protocol — an adapter that does
     *  not implement it simply reports an error, which [DebugSession.pause] already swallows. */
    fun pause() = withThread { s, t -> s.pause(t) }
    fun stepOver() = withThread { s, t -> s.next(t) }
    fun stepInto() = withThread { s, t -> s.stepIn(t) }
    fun stepOut() = withThread { s, t -> s.stepOut(t) }

    /**
     * Evaluate [expression] in the top stopped frame (DAP `evaluate`, context "hover") and deliver the
     * result — or null if there is no stopped frame or the expression has no value — on the main thread.
     * Backs the editor's variable inspection; the result carries the children handle when the value is
     * structured, so the inspector can expand it rather than showing a flattened `toString()`.
     */
    fun evaluate(expression: String, onResult: (DapEvaluation?) -> Unit) {
        val s = activeSession ?: session
        val frameId = _callStack.value.firstOrNull()?.id
        if (s == null || frameId == null || _state.value != DebugState.STOPPED) {
            onResult(null)
            return
        }
        scope.launch {
            val value = runCatching { s.evaluate(expression, frameId, "hover") }
                .getOrNull()
                ?.takeIf { it.result.isNotBlank() || it.expandable }
            withContext(Dispatchers.Main) { onResult(value) }
        }
    }

    /**
     * Children of an expandable value (DAP `variables`), for the inspector's tree. Empty on any
     * failure — a node that cannot be expanded should render as a leaf, not as an error.
     */
    fun variables(reference: Int, onResult: (List<DapVariable>) -> Unit) {
        val s = activeSession ?: session
        if (s == null || reference <= 0 || _state.value != DebugState.STOPPED) {
            onResult(emptyList())
            return
        }
        scope.launch {
            val rows = runCatching { s.variables(reference) }.getOrDefault(emptyList())
            withContext(Dispatchers.Main) { onResult(rows) }
        }
    }

    fun ownedBy(id: String): Boolean = ownerPackage==id

    fun stopPackage(id: String) { if (ownerPackage==id) stop() }

    @Synchronized fun stop() {
        generation.incrementAndGet()
        preparation?.cancel(); preparation=null
        ownerPackage=null;launchSource=null;launchProject=null
        clearObservers()
        ended = true // a late `terminated` event must not resurrect the session as TERMINATED
        releaseAndroidAttachment()
        children.forEach { runCatching { it.close() } }
        children.clear()
        session?.close()
        session = null
        activeSession = null
        _state.value = DebugState.DISCONNECTED
        clearStoppedView()
    }

    /** A debuggee-driven end (root or last child terminated): tear everything down, exactly once. */
    @Synchronized private fun endSession() {
        if (ended) return
        ended = true
        clearObservers()
        _location.value = null
        releaseAndroidAttachment()
        children.forEach { runCatching { it.close() } }
        children.clear()
        // Close the root so proot's --kill-on-exit reaps the adapter tree instead of leaking it.
        session?.let { runCatching { it.close() } }
        session = null
        activeSession = null
        _state.value = DebugState.TERMINATED
    }

    /** Clear the device-side `set-debug-app -w` flag left by an Android attach, exactly once. */
    private fun releaseAndroidAttachment() {
        val attach = androidAttach ?: return
        androidAttach = null
        scope.launch { runCatching { attach.detach() } }
    }

    private inline fun withThread(crossinline block: suspend (DebugSession, Int) -> Unit) {
        val s = activeSession ?: session ?: return
        val t = s.stopped.value?.threadId ?: s.threads.value.firstOrNull()?.id ?: 1
        scope.launch { runCatching { block(s, t) } }
    }

    private fun pushOutput(text: String) {
        val line = text.trimEnd('\n')
        if (line.isNotEmpty()) _output.value = (_output.value + line).takeLast(500)
    }

    private fun engineForFile(hostPath: String, projectDir: String? = null): DebugEngineEntry? {
        val ext = "." + hostPath.substringAfterLast('.', "")
        DebugEngineCatalog.all.firstOrNull { ext in it.extensions }?.let { return it }
        return null
    }

    /** True if [hostPath]'s language has a built-in DAP engine — the single source of truth for
     *  whether the Debug action can launch it (used by run-config entry derivation). */
    fun canDebugFile(hostPath: String): Boolean = engineForFile(hostPath) != null

    /** The catalog id of the engine that would debug [hostPath], for callers that also have to check
     *  the engine is installed. Resolves .kt in an Android app to java-debug, which a plain extension
     *  match over the catalog cannot. */
    fun debugEngineIdFor(hostPath: String): String? = engineForFile(hostPath)?.id

    private fun hostToDistro(p: String): String =
        dev.blamspot.jcode.core.distro.WorkspaceHostPaths.hostToGuest(p)

    private companion object {
        const val KOTLIN_EXT = ".kt"
        const val JAVA_ENGINE_ID = "java-debug"
    }
}

/** Adapts a proot child process's stdio pipes to [DapTransport] (blocking reads, no PTY echo). */
private class ProcessTransport(private val process: Process) : DapTransport {
    private val input = process.inputStream
    private val output = process.outputStream

    init {
        // Drain the adapter's stderr so its pipe never fills and blocks the adapter — and log what it
        // says. Discarding it left a stdio adapter that fails at startup completely mute: the session
        // just timed out after 30s with nothing to go on, while the TCP path had logged its adapter's
        // output all along.
        Thread {
            runCatching {
                process.errorStream.bufferedReader().forEachLine { line ->
                    android.util.Log.d("JCodeDAP-adapter", "[err] $line")
                }
            }
        }.apply { isDaemon = true }.start()
    }

    override fun read(buffer: ByteArray): Int = try { input.read(buffer) } catch (e: Exception) { -1 }
    override fun write(bytes: ByteArray) {
        // Never swallow silently: a dropped write looks exactly like an adapter that stopped
        // answering, and the request then just times out 30s later with no clue why.
        try {
            output.write(bytes); output.flush()
        } catch (e: Exception) {
            android.util.Log.w("JCodeDAP-adapter", "DAP write failed", e)
        }
    }
    override fun close() {
        dev.blamspot.jcode.core.distro.GuestProcessTree.destroy(process)
    }

    // Linux reports a signal-killed child as 128 + signal, which is how a segfaulting adapter
    // (netcoredbg under proot does exactly this) becomes an actionable message instead of a timeout.
    override fun terminationDetail(): String? = runCatching {
        // EOF on the pipe races the child being reaped, so give it a moment before asking.
        if (!process.waitFor(1, java.util.concurrent.TimeUnit.SECONDS)) return@runCatching null
        val code = process.exitValue()
        if (code > 128) "crashed with signal ${code - 128}" else "exit code $code"
    }.getOrNull()
}

/**
 * Adapts a TCP DAP adapter (js-debug's `dapDebugServer`, which listens on a port inside proot) to
 * [DapTransport]. proot shares the host network namespace, so the guest's 127.0.0.1:port is directly
 * reachable from the app. The listener [process] is held so closing tears down its proot tree too.
 */
private class TcpTransport private constructor(
    private val socket: java.net.Socket,
    private val process: Process,
) : DapTransport {
    private val input = socket.getInputStream()
    private val output = socket.getOutputStream()

    // stdout/stderr are already being drained by connect()'s logging threads (started before the
    // connect loop so the adapter's pipes never fill and any startup error is captured) — the DAP
    // stream itself flows over the socket, so nothing more to drain here.

    override fun read(buffer: ByteArray): Int = try { input.read(buffer) } catch (e: Exception) { -1 }
    override fun write(bytes: ByteArray) {
        // Never swallow silently: a dropped write looks exactly like an adapter that stopped
        // answering, and the request then just times out 30s later with no clue why.
        try {
            output.write(bytes); output.flush()
        } catch (e: Exception) {
            android.util.Log.w("JCodeDAP-adapter", "DAP write failed", e)
        }
    }
    override fun close() {
        runCatching { socket.close() }
        dev.blamspot.jcode.core.distro.GuestProcessTree.destroy(process)
    }

    companion object {
        /**
         * Retry-connect until the adapter is listening or [timeoutMs] elapses / the process dies.
         * A TCP adapter (js-debug) talks DAP over the socket, but still prints startup logs/errors to
         * its stdout/stderr pipes. Drain BOTH from the moment it spawns — otherwise a full pipe blocks
         * node before it binds the port (a silent "connect timeout"). The drained lines go to logcat
         * (tag JCodeDAP-adapter) and a bounded tail so a died/never-bound adapter reports WHY.
         */
        fun connect(host: String, port: Int, process: Process, timeoutMs: Long): TcpTransport? {
            val tail = java.util.concurrent.ConcurrentLinkedQueue<String>()
            fun drain(stream: java.io.InputStream, tag: String) = Thread {
                runCatching {
                    stream.bufferedReader().forEachLine { line ->
                        android.util.Log.d("JCodeDAP-adapter", "[$tag] $line")
                        tail.add("[$tag] $line"); while (tail.size > 60) tail.poll()
                    }
                }
            }.apply { isDaemon = true }.start()
            drain(process.inputStream, "out")
            drain(process.errorStream, "err")
            val deadline = System.currentTimeMillis() + timeoutMs
            var lastConnectError: Throwable? = null
            while (System.currentTimeMillis() < deadline) {
                if (!process.isAlive) {
                    val code = runCatching { process.exitValue() }.getOrNull()
                    android.util.Log.e(
                        "JCodeDAP-adapter",
                        "adapter exited early (code=$code) before $host:$port was reachable; output:\n" +
                            tail.joinToString("\n"),
                    )
                    return null
                }
                val attempt = runCatching {
                    java.net.Socket().apply { connect(java.net.InetSocketAddress(host, port), 1000) }
                }
                attempt.getOrNull()?.let { return TcpTransport(it, process) }
                lastConnectError = attempt.exceptionOrNull()
                Thread.sleep(200)
            }
            val reason = lastConnectError?.let { "${it::class.java.simpleName}: ${it.message}" } ?: "none"
            android.util.Log.e(
                "JCodeDAP-adapter",
                "adapter never became reachable on $host:$port within ${timeoutMs}ms " +
                    "(last connect error: $reason); output:\n" +
                    tail.joinToString("\n"),
            )
            // Best-effort teardown of the still-running adapter. NOTE: a TCP adapter that we never
            // connected to (js-debug) can't be told to `disconnect`, and destroy() doesn't reliably
            // trip proot's --kill-on-exit here, so the node/proot tree may linger until the app is
            // restarted. This only happens on the js-debug transport failure (see JCodeDAP-adapter log).
            dev.blamspot.jcode.core.distro.GuestProcessTree.destroy(process)
            return null
        }
    }
}

/**
 * A plain socket [DapTransport] for a js-debug CHILD session: connects to the `__jsDebugChildServer`
 * port the root adapter provides. Unlike [TcpTransport] it owns no process — the root adapter process
 * hosts every child server, so closing the root (proot `--kill-on-exit`) reaps the children too; this
 * only closes its own socket.
 */
private class SocketTransport private constructor(private val socket: java.net.Socket) : DapTransport {
    private val input = socket.getInputStream()
    private val output = socket.getOutputStream()

    override fun read(buffer: ByteArray): Int = try { input.read(buffer) } catch (e: Exception) { -1 }
    override fun write(bytes: ByteArray) {
        // Never swallow silently: a dropped write looks exactly like an adapter that stopped
        // answering, and the request then just times out 30s later with no clue why.
        try {
            output.write(bytes); output.flush()
        } catch (e: Exception) {
            android.util.Log.w("JCodeDAP-adapter", "DAP write failed", e)
        }
    }
    override fun close() {
        runCatching { socket.close() }
    }

    companion object {
        /** Retry-connect to the child DAP server until it accepts or [timeoutMs] elapses. */
        fun connect(host: String, port: Int, timeoutMs: Long): SocketTransport? {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                val sock = runCatching {
                    java.net.Socket().apply { connect(java.net.InetSocketAddress(host, port), 1000) }
                }.getOrNull()
                if (sock != null) return SocketTransport(sock)
                Thread.sleep(100)
            }
            return null
        }
    }
}
