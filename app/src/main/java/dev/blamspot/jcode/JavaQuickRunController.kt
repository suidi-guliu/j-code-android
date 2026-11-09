package dev.blamspot.jcode

import android.content.Context
import dev.blamspot.jcode.core.distro.*
import dev.blamspot.jcode.fs.SafeLocalFiles
import dev.suicode.nativehost.NativePackageHost
import dev.suicode.packages.NativeRecipe
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.coroutineContext

/** Shared native task engine; the historical property name is retained for editor ABI wiring. */
class JavaQuickRunController(private val context: Context,private val distro: DistroService,private val scope: CoroutineScope,private val message: suspend (String)->Unit) {
    private val gate=QuickRunGate()
    private val generation=AtomicLong()
    private val mutableActive=MutableStateFlow(false)
    val active=mutableActive.asStateFlow()
    private val focus=MutableSharedFlow<String>(extraBufferCapacity=1)
    val focusRequests=focus.asSharedFlow()
    private var job: Job?=null
    private var stopping=false
    private val teardownScope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    @Volatile private var ownerPackage: String?=null
    @Volatile private var sourcePath: String?=null
    @Volatile private var sessionId: String?=null
    private val payloads=ConcurrentHashMap<String,String>()
    private val terminals get()=TerminalSessionHost.manager(context)

    fun start(file: File,repeatCount: Int,save: suspend ()->Boolean): Boolean {
        val provider=try { NativePackageHost.get(context).resolve(file.name) } catch(e: Exception) { scope.launch { message(e.message ?: "Provider conflict") };return true } ?: return false
        return launch(file,null,provider,provider.languageId,repeatCount,true,save)
    }
    fun startSource(file: File,recipe: dev.suicode.packages.RunProvider,save: suspend ()->Boolean): Boolean =
        launch(file,null,recipe,recipe.languageId,0,false,save)

    fun startTool(source: File,project: File,recipe: NativeRecipe,save: suspend ()->Boolean): Boolean =
        launch(source,project,recipe,recipe.componentId,0,false,save)

    private fun launch(file: File,project: File?,recipe: NativeRecipe,label: String,repeatCount: Int,consumeBusy: Boolean,save: suspend ()->Boolean): Boolean {
        if(!gate.begin(repeatCount)) return consumeBusy
        ownerPackage=recipe.pack.id
        val ticket=generation.incrementAndGet()
        mutableActive.value=true
        job=scope.launch {
            var resourcePath: File?=null
            var stagedScript: File?=null
            try {
                check(distro.isRuntimeReady()) { "Finish Ubuntu setup before running native tasks" }
                val runtime=distro.environmentState.value.runtime
                val path=prepareQuickRun(save) {
                    val actual=file.canonicalFile
                    check((actual.isFile || (project!=null && actual.isDirectory)) && actual.canRead()) { "Cannot read the saved task source" }
                    actual.path
                }
                coroutineContext.ensureActive()
                check(distro.environmentState.value.runtime.selectedDistro.id==runtime.selectedDistro.id) { "Ubuntu environment changed while saving; retry" }
                sessionId?.let(::close)
                val actual=File(path)
                sourcePath=actual.path
                val token=UUID.randomUUID().toString()
                val paths=withContext(Dispatchers.IO) { NativeTaskPaths.resolve(actual,project,runtime.binds) }
                val guestSource=paths.guestSource
                val guestProject=paths.guestDirectory
                val taskBinds=paths.binds
                val access=distro.validateTaskDirectory(paths)
                check(access.succeeded) { "E_TASK_MAPPING: host=${paths.directory}; guest=$guestProject; Ubuntu user=${runtime.user}; ${access.internalError ?: access.stderr.ifBlank { "directory/source access check failed" }}" }
                val resourceDirectory="/tmp/sui-package-$token"
                resourcePath=File(distro.activeRootfsPath(),resourceDirectory.removePrefix("/"))
                recipe.resources().forEach { (name,bytes) -> check(distro.stageGuestBytes("$resourceDirectory/$name",bytes)) { "Cannot stage authenticated package resource" } }
                val scriptPath="/tmp/sui-native-task-$token.sh"
                stagedScript=File(distro.activeRootfsPath(),scriptPath.removePrefix("/"))
                check(distro.stageGuestFile(scriptPath,recipe.script(guestSource,resourceDirectory,guestProject))) { "Cannot stage native recipe" }
                val session=terminals.createOwnedSession(distroId=runtime.selectedDistro.id,binds=taskBinds,workdir=guestProject,user=runtime.user,rootfsArch=runtime.selectedDistro.arch,label="$label · ${actual.name}",processOwner="sui-quickrun:$token") ?: error("Cannot create terminal session")
                sessionId=session.id
                payloads[session.id]="sui-quickrun:$token/task"
                TerminalSessionHost.onSessionStarted(context,session.id,session.label)
                OutputLog.append("[Native task] ${recipe.pack.id}/${recipe.componentId} · ${actual.path}")
                OutputLog.captureSession(session.id)
                terminals.sendInput(session.id,"${GuestProcessTree.OWNER_ENV}=sui-quickrun:$token/task /bin/bash $scriptPath; result=\$?; rm -f $scriptPath; printf '\\033]7713;quickrun;%s\\007' \"\$result\"\n")
                focus.emit(session.id)
                while(generation.get()==ticket && mutableActive.value && terminals.getSession(session.id)!=null) delay(150)
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { message(e.message ?: "Native task failed") }
            finally {
                withContext(NonCancellable+Dispatchers.IO) { stagedScript?.delete(); resourcePath?.let { runCatching { SafeLocalFiles.remove(it) } } }
                if(generation.get()==ticket) { mutableActive.value=false;gate.finish() }
            }
        }
        return true
    }
    fun complete(id: String,payload: String) {
        if(!stopping && id==sessionId && payload.substringBefore(';')=="quickrun") {
            mutableActive.value=false;gate.finish()
            payloads.remove(id)?.let { owner -> teardownScope.launch { GuestProcessTree.terminateOwnerBlocking(owner) } }
        }
    }
    fun sourceMoved(from: String) { if(sourcePath?.let { it==from || it.startsWith(from+"/") }==true) stop() }
    fun stopPackage(id: String) { if(ownerPackage==id) stop() }
    fun stop(): Boolean {
        val wasActive=mutableActive.value
        if (!wasActive || stopping) return wasActive
        stopping=true
        generation.incrementAndGet()
        val taskJob=job
        val id=sessionId
        val payload=id?.let { payloads.remove(it) }
        teardownScope.launch {
            try {
                // Kill only the task subtree. The owning interactive shell and its transcript survive.
                withContext(Dispatchers.IO) { payload?.let(GuestProcessTree::terminateOwnerBlocking) }
                taskJob?.cancelAndJoin()
                OutputLog.append("[Native task] Stopped; terminal output retained.")
            } finally {
                ownerPackage=null;mutableActive.value=false;gate.finish();stopping=false
            }
        }
        return true
    }
    private fun close(id: String) {
        payloads.remove(id)
        OutputLog.releaseSession(id);terminals.closeSession(id);TerminalSessionHost.onSessionStopped(id)
    }
}
