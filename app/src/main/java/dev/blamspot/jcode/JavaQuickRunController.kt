package dev.blamspot.jcode

import android.content.Context
import dev.blamspot.jcode.core.distro.DistroBind
import dev.blamspot.jcode.core.distro.DistroService
import dev.blamspot.jcode.core.distro.QuickRunGate
import dev.blamspot.jcode.core.distro.QuickRunLanguages
import dev.blamspot.jcode.core.distro.prepareQuickRun
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/** Owns only QuickRun sessions; ProjectRunner and Setup sessions are never stopped or reused. */
class JavaQuickRunController(
    private val context: Context,
    private val distro: DistroService,
    private val scope: CoroutineScope,
    private val message: suspend (String) -> Unit,
) {
    private val gate = QuickRunGate()
    private val generation = AtomicLong()
    private val mutableActive = MutableStateFlow(false)
    val active = mutableActive.asStateFlow()
    private val focus = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val focusRequests = focus.asSharedFlow()
    private var job: Job? = null
    @Volatile private var sessionId: String? = null
    private val terminals get() = TerminalSessionHost.manager(context)

    fun start(file: File, repeatCount: Int, save: suspend () -> Boolean): Boolean {
        val adapter = QuickRunLanguages.forFile(file.name) ?: return false
        if (!gate.begin(repeatCount)) return true
        val ticket = generation.incrementAndGet()
        mutableActive.value = true
        job = scope.launch {
            try {
                check(distro.isRuntimeReady()) { "QuickRun requires the existing Ubuntu environment. Finish environment setup first." }
                val runtime = distro.environmentState.value.runtime
                val path = prepareQuickRun(save) {
                    val actual = file.canonicalFile
                    check(actual.isFile && actual.canRead()) { "QuickRun cannot read the saved Java file." }
                    actual.path
                }
                coroutineContext.ensureActive()
                check(distro.environmentState.value.runtime.selectedDistro.id == runtime.selectedDistro.id) { "Ubuntu environment changed while saving. Run again." }
                sessionId?.let { close(it) }
                val actual = File(path)
                val token = UUID.randomUUID().toString()
                val guestFile = "/sui-quickrun/${actual.name}"
                val scriptPath = "/tmp/sui-quickrun-$token.sh"
                val mountPoint = File(distro.activeRootfsPath(), "sui-quickrun")
                check(mountPoint.isDirectory || mountPoint.mkdirs()) { "Cannot prepare the QuickRun mount point in Ubuntu." }
                check(distro.stageGuestFile(scriptPath, adapter.command(guestFile))) { "Cannot stage the QuickRun script in Ubuntu." }
                val session = terminals.createOwnedSession(
                    distroId = runtime.selectedDistro.id,
                    binds = listOf(DistroBind(actual.parentFile!!.path, "/sui-quickrun")),
                    workdir = "/sui-quickrun",
                    user = runtime.user,
                    rootfsArch = runtime.selectedDistro.arch,
                    label = "Java · ${actual.name}",
                    processOwner = "sui-quickrun:$token",
                ) ?: error("Cannot create the QuickRun terminal (check Ubuntu and the session limit).")
                sessionId = session.id
                TerminalSessionHost.onSessionStarted(context, session.id, session.label)
                OutputLog.append("[QuickRun] ${actual.path}")
                OutputLog.captureSession(session.id)
                terminals.sendInput(session.id, "/bin/bash $scriptPath; result=\$?; rm -f $scriptPath; printf '\\033]7713;quickrun;%s\\007' \"\$result\"\n")
                focus.emit(session.id)
                while (generation.get() == ticket && mutableActive.value && terminals.getSession(session.id) != null) delay(150)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                message(error.message ?: "Java QuickRun failed.")
            } finally {
                if (generation.get() == ticket) { mutableActive.value = false; gate.finish() }
            }
        }
        return true
    }

    fun complete(id: String, payload: String) {
        if (id == sessionId && payload.substringBefore(';') == "quickrun") {
            mutableActive.value = false
            gate.finish()
        }
    }

    /** Closing invokes the existing tagged-tree cleanup, including Java children ignoring SIGINT. */
    fun stop(): Boolean {
        val wasActive = mutableActive.value
        generation.incrementAndGet()
        job?.cancel()
        sessionId?.let { close(it) }
        sessionId = null
        mutableActive.value = false
        gate.finish()
        return wasActive
    }

    private fun close(id: String) {
        OutputLog.releaseSession(id)
        terminals.closeSession(id)
        TerminalSessionHost.onSessionStopped(id)
    }
}
