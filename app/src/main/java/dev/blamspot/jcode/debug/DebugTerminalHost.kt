package dev.blamspot.jcode.debug

import android.content.Context
import dev.blamspot.jcode.OutputLog
import dev.blamspot.jcode.TerminalSessionHost
import dev.blamspot.jcode.core.debug.RunInTerminal
import dev.blamspot.jcode.core.debug.TerminalProcess
import dev.blamspot.jcode.core.distro.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Adapter pipes remain DAP-only. Each debuggee has an owned, continuously drained existing PTY. */
class DebugTerminalHost(private val context: Context,private val distro: DistroService) {
    data class Review(val request: RunInTerminal,val publisher: String,val decision: CompletableDeferred<Boolean>)
    private val mutableReview=MutableStateFlow<Review?>(null)
    val review=mutableReview.asStateFlow()
    private val focus=MutableSharedFlow<String>(extraBufferCapacity=4)
    val focusRequests=focus.asSharedFlow()
    private val lifecycle=Any()
    private var epoch=0L
    private val cleanup=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val owners=java.util.concurrent.ConcurrentHashMap<String,String>()
    private val terminalIds=java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val stdioHolders=java.util.concurrent.ConcurrentHashMap<String,File>()
    private val directories=java.util.concurrent.ConcurrentHashMap<String,File>()
    private val history get()=dev.blamspot.jcode.tasks.NativeTaskHistory.get(context)
    private val directProgram=java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val terminalCodes=java.util.concurrent.ConcurrentHashMap<String,Int>()
    private val terminalTokens=java.util.concurrent.ConcurrentHashMap<String,String>()
    private val records=java.util.concurrent.ConcurrentHashMap<String,String>()
    var onSessionClosed: ((Boolean) ->Unit)?=null
    fun ownsTask(taskId: String): Boolean=records.any { (session,id) -> id==taskId && (owners.containsKey(session) || stdioHolders.containsKey(session)) }
    fun closed(id: String,cancelled: Boolean) {
        if(owners.containsKey(id) || stdioHolders.containsKey(id)) onSessionClosed?.invoke(cancelled)
        terminalIds.remove(id);records.remove(id);directProgram.remove(id);terminalCodes.remove(id);terminalTokens.remove(id)
    }
    fun decide(allow: Boolean) { mutableReview.value?.decision?.complete(allow) }
    private suspend fun approve(request: RunInTerminal,project: String,publisher: String,authorized: ()->Boolean): Long {
        val ticket=synchronized(lifecycle) { epoch }
        check(distro.isRuntimeReady()) { "Ubuntu is not ready for interactive debugging" }
        check(authorized()) { "The IDE Kit is disabled or lacks Debug/Run permission" }
        val expected=WorkspaceHostPaths.hostToGuest(project).trimEnd('/')
        check(request.cwd==expected || request.cwd.startsWith(expected+"/")) { "Debug terminal cwd is outside the selected project: ${request.cwd}" }
        val canonical=File(WorkspaceHostPaths.guestToHost(request.cwd)).canonicalFile.toPath()
        check(canonical.startsWith(File(project).canonicalFile.toPath())) { "Debug terminal directory resolves outside the selected project" }
        val item=Review(request,publisher,CompletableDeferred())
        check(mutableReview.compareAndSet(null,item)) { "A debug terminal review is already pending" }
        try { check(withTimeout(120_000) { item.decision.await() }) { "Debug terminal launch cancelled by user" } }
        finally { mutableReview.compareAndSet(item,null) }
        currentCoroutineContext().ensureActive()
        check(authorized()) { "IDE Kit authorization changed during review" }
        return ticket
    }
    suspend fun launch(request: RunInTerminal,project: String,publisher: String,source: String=project,terminalExitIsProgram: Boolean=false,authorized: ()->Boolean): TerminalProcess {
        val ticket=approve(request,project,publisher,authorized)
        return withContext(Dispatchers.IO) {
            val runtime=distro.environmentState.value.runtime
            val paths=NativeTaskPaths.resolve(File(project),File(project),runtime.binds)
            val access=distro.execOwned("test -d ${shellQuote(request.cwd)} && test -r ${shellQuote(request.cwd)} && test -x ${shellQuote(request.cwd)}",request.cwd,"sui-kit:debug-terminal/preflight",10000)
            check(access.succeeded) { "E_DEBUG_MAPPING: ${request.cwd}: ${access.stderr}" }
            val token=UUID.randomUUID().toString();val pathsForTerminal=dev.blamspot.jcode.core.debug.DebugTerminalPaths(token)
            val owner=pathsForTerminal.owner;val directory=pathsForTerminal.directory;val script=pathsForTerminal.script;val pidFile=pathsForTerminal.pid
            val body=dev.blamspot.jcode.core.debug.DebugTerminalLaunch.script(request,pathsForTerminal)
            check(distro.stageGuestFile(script,body)) { "Cannot stage reviewed debug terminal" }
            val manager=TerminalSessionHost.manager(context)
            val session=manager.createOwnedSession(runtime.selectedDistro.id,paths.binds,request.cwd,runtime.user,"/bin/bash --noprofile --norc ${shellQuote(script)}",runtime.selectedDistro.arch,"${request.title} · Debug", "sui-quickrun:debug-$token") ?: error("Cannot open debug terminal; check the session limit and Ubuntu state")
            session.managedLabel=true;session.taskState="RUNNING"
            synchronized(lifecycle) {
                if(ticket!=epoch || !authorized()) { manager.closeSession(session.id);throw CancellationException("Debug session cancelled during PTY creation") }
                terminalIds.add(session.id);terminalTokens[session.id]=token;if(terminalExitIsProgram) directProgram.add(session.id);owners[session.id]=owner;directories[session.id]=File(distro.activeRootfsPath(),directory.removePrefix("/"))
            }
            val record=history.start(session.label,publisher,"debug-session",source,project,kind="debug")
            history.bind(record.id,session.id)
            val retained=synchronized(lifecycle) { records[session.id]=record.id;ticket==epoch && authorized() }
            if(!retained) {
                history.finish(record.id,null,true,"Debug session cancelled during terminal registration")
                throw CancellationException("Debug session cancelled during terminal registration")
            }
            TerminalSessionHost.onSessionStarted(context,session.id,session.label)
            OutputLog.captureSession(session.id)
            check(distro.stageGuestFile("$directory/ready","ready"))
            focus.emit(session.id)
            val file=File(distro.activeRootfsPath(),pidFile.removePrefix("/"))
            try {
                withTimeout(8000) { while(!file.isFile || file.length()==0L) { check(authorized() && ticket==synchronized(lifecycle) { epoch }) { "Debug session cancelled during launch" };check(manager.getSession(session.id)!=null) { "Debug terminal exited during launch" };delay(20) } }
                val ids=file.readText().trim().split(' ').map(String::toInt)
                check(ids.size==2 && ids.all { it>0 }) { "Invalid debug terminal process acknowledgement" }
                synchronized(lifecycle) {
                    check(ticket==epoch && authorized()) { "Debug authorization changed before program execution" }
                    check(distro.stageGuestFile(pathsForTerminal.go,"go")) { "Cannot release debugger execution gate" }
                }
                TerminalProcess(ids[0],ids[1])
            } catch(e: Exception) { runCatching { File(distro.activeRootfsPath(),pathsForTerminal.cancel.removePrefix("/")).writeText("cancelled") };GuestProcessTree.terminateOwnerBlocking(owner);throw e }
        }
    }
    /** LLDB's attach-based reverse launcher cannot attach through a second PRoot tracer.
     * Keep LLDB's own-child launch, supplying an actual PTY through its target stdio settings. */
    suspend fun prepareStdio(request: RunInTerminal,project: String,publisher: String,source: String=project,authorized: ()->Boolean): String {
        val ticket=approve(request,project,publisher,authorized)
        return withContext(Dispatchers.IO) {
            val runtime=distro.environmentState.value.runtime
            val paths=NativeTaskPaths.resolve(File(project),File(project),runtime.binds)
            val token=UUID.randomUUID().toString();val directory="/tmp/sui-debug-terminal-$token"
            val script="$directory/stdio.sh";val ttyPath="$directory/tty";val done="$directory/done"
            val body="""
                #!/bin/bash
                trap ':' INT
                tty > ${shellQuote(ttyPath)}
                while [ ! -e ${shellQuote(done)} ]; do sleep .2; done
                rm -f -- ${shellQuote(done)} ${shellQuote(ttyPath)} ${shellQuote(script)}
                rmdir -- ${shellQuote(directory)} 2>/dev/null
                exec /bin/bash --noprofile --norc -i
            """.trimIndent()
            check(distro.stageGuestFile(script,body))
            val manager=TerminalSessionHost.manager(context)
            val session=manager.createOwnedSession(runtime.selectedDistro.id,paths.binds,request.cwd,runtime.user,"/bin/bash --noprofile --norc ${shellQuote(script)}",runtime.selectedDistro.arch,"${request.title} · Debug","sui-quickrun:debug-$token") ?: error("Cannot create debugger stdio terminal")
            session.managedLabel=true;session.taskState="RUNNING"
            synchronized(lifecycle) {
                if(ticket!=epoch || !authorized()) { manager.closeSession(session.id);throw CancellationException("Debug session cancelled during PTY creation") }
                terminalIds.add(session.id);stdioHolders[session.id]=File(distro.activeRootfsPath(),done.removePrefix("/"))
            }
            val record=history.start(session.label,publisher,"debug-session",source,project,kind="debug")
            history.bind(record.id,session.id)
            val retained=synchronized(lifecycle) { records[session.id]=record.id;ticket==epoch && authorized() }
            if(!retained) {
                history.finish(record.id,null,true,"Debug session cancelled during terminal registration")
                throw CancellationException("Debug session cancelled during terminal registration")
            }
            TerminalSessionHost.onSessionStarted(context,session.id,session.label);OutputLog.captureSession(session.id)
            focus.emit(session.id)
            val file=File(distro.activeRootfsPath(),ttyPath.removePrefix("/"))
            try {
                withTimeout(8000) { while(!file.isFile || file.length()==0L) {
                    check(ticket==synchronized(lifecycle) { epoch } && authorized()) { "Debug session cancelled during PTY acknowledgement" }
                    check(manager.getSession(session.id)!=null) { "Debug terminal exited before PTY acknowledgement" };delay(20)
                } }
                check(ticket==synchronized(lifecycle) { epoch } && authorized()) { "Debug session cancelled before debugger launch" }
                file.readText().trim().also { check(it.matches(Regex("/dev/pts/[0-9]+"))) { "Invalid debugger PTY path" } }
            } catch(e: Exception) {
                runCatching { File(distro.activeRootfsPath(),done.removePrefix("/")).writeText("done") }
                history.failedToStart(record.id,e.message ?: "Debug terminal initialization failed")
                throw e
            }
        }
    }
    fun exited(code: Int) {
        (owners.keys+stdioHolders.keys).forEach { id -> TerminalSessionHost.existingManager()?.getSession(id)?.let {
            // Some adapters report zero even when their direct java process exits with an exception.
            // A declared direct-program terminal is authoritative; never infer a code for a launcher.
            val actual=if(id in directProgram) terminalCodes[id] else code
            if(actual!=null) {
                if(it.taskState!="CANCELLED") it.taskState=if(actual==0) "SUCCEEDED" else "FAILED"
                it.taskExitCode=actual
                records[id]?.let { record -> history.finish(record,actual,detail=if(actual!=code) "Adapter reported $code; actual program PTY reported $actual" else "") }
            }
        } }
    }
    fun completed(id: String,payload: String) {
        if(payload.substringBefore(';')!="debug-${terminalTokens[id]}" || id !in terminalIds) return
        val code=payload.substringAfter(';').toIntOrNull() ?: return
        terminalCodes[id]=code
        TerminalSessionHost.existingManager()?.getSession(id)?.let { session ->
            if(id in directProgram) {
                if(session.taskState!="CANCELLED") session.taskState=if(code==0) "SUCCEEDED" else "FAILED"
                session.taskExitCode=code
                records[id]?.let { history.finish(it,code,detail="Actual direct-program PTY exit") }
            } else if(session.taskState=="RUNNING") session.taskState="FINISHED"
        }
    }
    fun stop(cancelled: Boolean,failure: String?=null) {
        mutableReview.value?.decision?.complete(false)
        val snapshots=synchronized(lifecycle) {
            epoch++
            (owners.toMap() to stdioHolders.toMap()).also { owners.clear();stdioHolders.clear() }
        }
        val retiring=snapshots.first;val holders=snapshots.second
        cleanup.launch {
            (retiring.keys+holders.keys).forEach { id -> records[id]?.let { task ->
                val record=history.records.value.firstOrNull { it.id==task }
                if(record?.outcome in setOf(dev.blamspot.jcode.tasks.TaskOutcome.STARTING,dev.blamspot.jcode.tasks.TaskOutcome.RUNNING)) {
                    if(failure!=null) history.failedToStart(task,failure) else if(cancelled) history.finish(task,null,true,"Debug session stopped") else history.finishedWithoutCode(task,"Debug session ended; waiting for any direct-program exit acknowledgement")
                }
            } }
        }
        holders.forEach { (id,file) ->
            TerminalSessionHost.existingManager()?.getSession(id)?.let { if(it.taskState=="RUNNING") it.taskState=if(cancelled) "CANCELLED" else "FINISHED" }
            cleanup.launch { runCatching { file.writeText("done") }.onFailure {
                android.util.Log.e("SuiDebugTerminal","Cannot release retained debugger terminal $id",it)
                OutputLog.append("Cannot release debugger terminal: ${it.message}; close this terminal explicitly",dev.blamspot.jcode.OutputKind.Error)
            } }
        }
        cleanup.launch {
            retiring.forEach { (id,owner) ->
                directories[id]?.let { runCatching { File(it,"cancel").writeText("cancelled") } }
                GuestProcessTree.terminateOwnerBlocking(owner)
                TerminalSessionHost.existingManager()?.getSession(id)?.let { session -> if(session.taskState=="RUNNING") session.taskState=if(cancelled) "CANCELLED" else "FINISHED" }
                directories.remove(id) // The retained wrapper cleans its IPC files after acknowledging cancellation.
            }
        }
    }
}
