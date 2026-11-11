package dev.blamspot.jcode.tasks

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import org.json.JSONArray

enum class TaskOutcome { STARTING,RUNNING,SUCCEEDED,FAILED,CANCELLED,INTERRUPTED,FINISHED }
data class NativeTaskRecord(val id: String,val name: String,val packageId: String,val component: String,val source: String,val project: String?,val started: Long,val ended: Long?=null,val sessionId: String?=null,val outcome: TaskOutcome=TaskOutcome.STARTING,val exitCode: Int?=null,val detail: String="",val truncated: Boolean=false,val kind: String="task",val packageVersion: String="") {
    val durationMs get()=((ended ?: System.currentTimeMillis())-started).coerceAtLeast(0)
}

/** Application-owned records; no shell-prompt inference. All disk methods run off the UI thread. */
class TaskHistoryStore(private val root: File) {
    private val mutable=MutableStateFlow<List<NativeTaskRecord>>(emptyList())
    val records=mutable.asStateFlow()
    private val warningState=MutableStateFlow<String?>(null)
    val warning=warningState.asStateFlow()
    private var loaded=false
    private val sessionToTask=HashMap<String,String>()
    private val limit=16L*1024*1024
    @Synchronized fun load() {
        if(loaded) return
        root.mkdirs()
        val index=File(root,"index.json")
        if(index.isFile) try {
            val array=JSONArray(index.readText())
            mutable.value=(maxOf(0,array.length()-200) until array.length()).map { i -> decode(array.getJSONObject(i)).let { r ->
                if(r.outcome in setOf(TaskOutcome.STARTING,TaskOutcome.RUNNING)) r.copy(outcome=TaskOutcome.INTERRUPTED,ended=System.currentTimeMillis(),detail="Application process ended; no exit acknowledgement was recorded") else r
            } }
        } catch(e: Exception) {
            runCatching { Files.copy(index.toPath(),File(root,"unreadable-${System.currentTimeMillis()}.json").toPath()) }.onFailure { warningState.value=it.message }
            mutable.value=emptyList()
        }
        loaded=true;persist()
    }
    @Synchronized fun start(name: String,packageId: String,component: String,source: String,project: String?,id: String=UUID.randomUUID().toString(),kind: String="task",packageVersion: String=""): NativeTaskRecord {
        load();val r=NativeTaskRecord(id,name,packageId,component,source,project,System.currentTimeMillis(),kind=kind,packageVersion=packageVersion)
        mutable.value=(mutable.value+r).takeLast(200);persist();return r
    }
    @Synchronized fun bind(id: String,sessionId: String) { sessionToTask[sessionId]=id;update(id) { it.copy(sessionId=sessionId,outcome=if(it.outcome==TaskOutcome.STARTING) TaskOutcome.RUNNING else it.outcome) } }
    @Synchronized fun finish(id: String,exitCode: Int?,cancelled: Boolean=false,detail: String="") {
        update(id) { r -> r.copy(ended=r.ended ?: System.currentTimeMillis(),exitCode=exitCode ?: r.exitCode,detail=detail.ifBlank { r.detail },outcome=when { cancelled || r.outcome==TaskOutcome.CANCELLED -> TaskOutcome.CANCELLED;exitCode==0 -> TaskOutcome.SUCCEEDED;exitCode!=null -> TaskOutcome.FAILED;else -> TaskOutcome.INTERRUPTED }) }
    }
    @Synchronized fun finishedWithoutCode(id: String,detail: String) { update(id) { if(it.outcome in setOf(TaskOutcome.STARTING,TaskOutcome.RUNNING)) it.copy(ended=System.currentTimeMillis(),outcome=TaskOutcome.FINISHED,detail=detail) else it } }
    @Synchronized fun failedToStart(id: String,detail: String) { update(id) { if(it.outcome==TaskOutcome.CANCELLED) it else it.copy(ended=System.currentTimeMillis(),outcome=TaskOutcome.FAILED,detail=detail) } }
    @Synchronized fun sessionClosed(sessionId: String,cancelled: Boolean=false) {
        val id=sessionToTask[sessionId] ?: return
        val record=mutable.value.firstOrNull { it.id==id } ?: return
        if(record.outcome in setOf(TaskOutcome.STARTING,TaskOutcome.RUNNING)) finish(id,null,cancelled,"Terminal session closed before an exit acknowledgement")
    }
    @Synchronized fun append(sessionId: String,data: ByteArray,length: Int) {
        val id=sessionToTask[sessionId] ?: return
        val path=logFile(id);val room=(limit-path.length()).coerceAtLeast(0).toInt()
        // Do not decode chunks individually: UTF-8 characters may span PTY reads.
        if(room>0) try { java.io.FileOutputStream(path,true).use { it.write(data,0,minOf(room,length)) } } catch(e: Exception) { warningState.value="${path.path}: ${e.message}";update(id) { it.copy(truncated=true) } }
        if(length>room && mutable.value.firstOrNull { it.id==id }?.truncated==false) update(id) { it.copy(truncated=true) }
    }
    fun logFile(id: String): File { require(id.matches(Regex("[a-f0-9-]{36}")));return File(root,"$id.log") }
    private fun update(id: String,transform: (NativeTaskRecord)->NativeTaskRecord) { mutable.value=mutable.value.map { if(it.id==id) transform(it) else it };persist() }
    private fun persist() {
        var tmp: File?=null
        try {
            tmp=File.createTempFile("task-history-",".tmp",root)
            tmp.writeText(JSONArray(mutable.value.map(::encode)).toString())
            Files.move(tmp.toPath(),File(root,"index.json").toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE)
        } catch(e: Exception) { warningState.value="${root.path}: ${e.message}" }
        finally { tmp?.delete() }
    }
    private fun encode(r: NativeTaskRecord)=JSONObject().put("id",r.id).put("name",r.name).put("package",r.packageId).put("component",r.component).put("source",r.source).put("project",r.project ?: JSONObject.NULL).put("started",r.started).put("ended",r.ended ?: JSONObject.NULL).put("session",r.sessionId ?: JSONObject.NULL).put("outcome",r.outcome.name).put("exit",r.exitCode ?: JSONObject.NULL).put("detail",r.detail).put("truncated",r.truncated).put("kind",r.kind).put("packageVersion",r.packageVersion)
    private fun decode(j: JSONObject): NativeTaskRecord { val id=j.getString("id");logFile(id);return NativeTaskRecord(id,j.getString("name"),j.getString("package"),j.getString("component"),j.getString("source"),if(j.isNull("project")) null else j.getString("project"),j.getLong("started"),if(j.isNull("ended")) null else j.getLong("ended"),if(j.isNull("session")) null else j.getString("session"),TaskOutcome.valueOf(j.getString("outcome")),if(j.isNull("exit")) null else j.getInt("exit"),j.optString("detail"),j.optBoolean("truncated"),j.optString("kind","task"),j.optString("packageVersion")) }
}

object NativeTaskHistory {
    @Volatile private var store: TaskHistoryStore?=null
    fun get(context: android.content.Context): TaskHistoryStore=store ?: synchronized(this) { store ?: TaskHistoryStore(File(context.applicationContext.filesDir,"sui-code/task-history")).also { store=it } }
}
