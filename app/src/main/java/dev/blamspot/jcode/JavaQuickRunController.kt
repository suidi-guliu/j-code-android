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
import kotlinx.serialization.json.jsonPrimitive
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
    private var taskId: String?=null
    @Volatile private var launchCancelFile: File?=null
    private val completions=ConcurrentHashMap<String,String>()
    private val history get()=dev.blamspot.jcode.tasks.NativeTaskHistory.get(context)
    private val payloads=ConcurrentHashMap<String,String>()
    private val terminals get()=TerminalSessionHost.manager(context)

    fun start(file: File,repeatCount: Int,save: suspend ()->Boolean): Boolean {
        val provider=try { NativePackageHost.get(context).resolve(file.name) } catch(e: Exception) { scope.launch { message(e.message ?: "Provider conflict") };return true } ?: return false
        return launch(file,null,provider,provider.languageId,repeatCount,true,save)
    }
    fun startSource(file: File,recipe: dev.suicode.packages.RunProvider,save: suspend ()->Boolean): Boolean =
        launch(file,null,recipe,recipe.languageId,0,false,save)

    fun startTool(source: File,project: File,recipe: NativeRecipe,save: suspend ()->Boolean): Boolean =
        launch(source,project,recipe,recipe.pack.manifest.getValue("name").jsonPrimitive.content.removeSuffix(" IDE Kit")+" "+recipe.componentId,0,false,save)

    private fun launch(file: File,project: File?,recipe: NativeRecipe,label: String,repeatCount: Int,consumeBusy: Boolean,save: suspend ()->Boolean): Boolean {
        if(!gate.begin(repeatCount)) return consumeBusy
        ownerPackage=recipe.pack.id
        val ticket=generation.incrementAndGet()
        val recordKey=UUID.randomUUID().toString()
        taskId=recordKey;sessionId=null;launchCancelFile=null
        mutableActive.value=true
        job=scope.launch {
            var resourcePath: File?=null
            var stagedScript: File?=null
            var wrapperFile: File?=null
            val recordId: String=recordKey
            try {
                withContext(Dispatchers.IO) {
                    history.start("$label · ${file.name}",recipe.pack.id,recipe.componentId,file.path,project?.path,id=recordKey,packageVersion=recipe.pack.version)
                    if(generation.get()!=ticket) history.finish(recordKey,null,cancelled=true)
                }
                check(distro.isRuntimeReady()) { "Finish Ubuntu setup before running native tasks" }
                val runtime=distro.environmentState.value.runtime
                val path=prepareQuickRun(save) {
                    val actual=file.canonicalFile
                    check((actual.isFile || (project!=null && actual.isDirectory)) && actual.canRead()) { "Cannot read the saved task source" }
                    actual.path
                }
                coroutineContext.ensureActive()
                check(distro.environmentState.value.runtime.selectedDistro.id==runtime.selectedDistro.id) { "Ubuntu environment changed while saving; retry" }
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
                val wrapper="/tmp/sui-task-launch-$token.sh"
                wrapperFile=File(distro.activeRootfsPath(),wrapper.removePrefix("/"))
                val ready="$wrapper.ready"
                launchCancelFile=File(distro.activeRootfsPath(),(ready+".cancel").removePrefix("/"))
                check(distro.stageGuestFile(wrapper,NativeTaskLauncher.script(scriptPath,"sui-quickrun:$token/task",token,ready)))
                val session=terminals.createOwnedSession(distroId=runtime.selectedDistro.id,binds=taskBinds,workdir=guestProject,user=runtime.user,shellCommand="/bin/bash --noprofile --norc ${shellQuote(wrapper)}",rootfsArch=runtime.selectedDistro.arch,label="$label · ${actual.name}",processOwner="sui-quickrun:$token") ?: error("Cannot create terminal session")
                sessionId=session.id
                session.managedLabel=true;session.taskState="RUNNING"
                completions[session.id]="native-$token"
                payloads[session.id]="sui-quickrun:$token/task"
                TerminalSessionHost.onSessionStarted(context,session.id,session.label)
                OutputLog.append("[Native task] ${recipe.pack.id}/${recipe.componentId} · ${actual.path}")
                OutputLog.captureSession(session.id)
                focus.tryEmit(session.id)
                withContext(Dispatchers.IO) { history.bind(recordId,session.id) }
                check(distro.stageGuestFile(ready,"ready")) { "Cannot release task launch gate" }
                withContext(Dispatchers.IO) {
                    val ack=File(distro.activeRootfsPath(),(ready+".pid").removePrefix("/"))
                    withTimeout(8000) { while(!ack.isFile || ack.length()==0L) { currentCoroutineContext().ensureActive();check(generation.get()==ticket);delay(20) } }
                    check(ack.readText().trim().toIntOrNull()?.let { it>0 }==true) { "Invalid task process acknowledgement" }
                    currentCoroutineContext().ensureActive();check(generation.get()==ticket)
                    check(distro.stageGuestFile(ready+".go","go")) { "Cannot release task execution gate" }
                }

                focus.emit(session.id)
                while(generation.get()==ticket && mutableActive.value && terminals.getSession(session.id)!=null) delay(150)
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { recordId?.let { id -> withContext(Dispatchers.IO) { history.failedToStart(id,e.message ?: "Native task failed") } };message(e.message ?: "Native task failed") }
            finally {
                withContext(NonCancellable+Dispatchers.IO) { stagedScript?.delete();wrapperFile?.delete();resourcePath?.let { runCatching { SafeLocalFiles.remove(it) } } }
                if(generation.get()==ticket) {
                    recordId?.let { id -> withContext(NonCancellable+Dispatchers.IO) { if(history.records.value.firstOrNull { it.id==id }?.outcome in setOf(dev.blamspot.jcode.tasks.TaskOutcome.STARTING,dev.blamspot.jcode.tasks.TaskOutcome.RUNNING)) history.finish(id,null,detail="Task ended without an exit acknowledgement") } }
                    mutableActive.value=false;gate.finish()
                }
            }
        }
        return true
    }
    fun complete(id: String,payload: String) {
        if(payload.substringBefore(';')!=completions[id]) return
        val code=payload.substringAfter(';').trim().toIntOrNull() ?: return
        completions.remove(id)
        terminals.getSession(id)?.let { it.taskExitCode=code;if(it.taskState!="CANCELLED") it.taskState=if(code==0) "SUCCEEDED" else "FAILED" }
        val record=history.records.value.firstOrNull { it.sessionId==id } ?: return
        teardownScope.launch { history.finish(record.id,code);OutputLog.append("[${record.name}] ${if(code==0) "Succeeded" else "Failed"} · exit $code · ${System.currentTimeMillis()-record.started} ms") }
        if(!stopping && id==sessionId) { mutableActive.value=false;gate.finish();payloads.remove(id)?.let { owner -> teardownScope.launch { GuestProcessTree.terminateOwnerBlocking(owner) } } }
    }
    fun closed(id: String,cancelled: Boolean) {
        completions.remove(id)
        val payload=payloads.remove(id)
        teardownScope.launch { history.sessionClosed(id,cancelled);payload?.let(GuestProcessTree::terminateOwnerBlocking) }
        if(id==sessionId) { generation.incrementAndGet();job?.cancel();mutableActive.value=false;gate.finish();sessionId=null }
    }
    fun startRecorded(file: File,project: File?,recipe: NativeRecipe,save: suspend ()->Boolean): Boolean = launch(file,project,recipe,recipe.pack.manifest.getValue("name").jsonPrimitive.content+" "+recipe.componentId,0,false,save)
    fun reveal(id: String) { scope.launch { if(terminals.getSession(id)!=null) focus.emit(id) } }
    fun stopTask(id: String) { if(id==taskId) stop() }
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
        val cancelFile=launchCancelFile
        val record=taskId
        teardownScope.launch {
            try {
                record?.let { history.finish(it,null,cancelled=true) }
                cancelFile?.let { runCatching { it.writeText("cancelled") } }
                id?.let { terminals.getSession(it)?.taskState="CANCELLED" }
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
