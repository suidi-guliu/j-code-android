package dev.suicode.nativehost

import android.content.Context
import android.net.Uri
import dev.suicode.packages.*
import java.io.File
import java.net.URL
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.X509EncodedKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

data class PackagePreview(val bytes: ByteArray,val suffix: String,val pack: VerifiedPack,val source: String)
data class NativePackageRow(val record: InstalledPackage,val pack: VerifiedPack)


/** Application-owned host, isolated from all old extension services and persistence. */
class NativePackageHost private constructor(private val context: Context) {
    private val root=File(context.filesDir,"sui-code/native-packages")
    private val keyBytes=context.assets.open("sui-native/official-p256.der").use { it.readBytes() }
    private val rootKey=KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(keyBytes))
    private val verifier=PackageVerifier(mapOf(hash(keyBytes) to rootKey))
    private val store=PackageStore(root.toPath(),verifier)
    val providers=NativeProviders()
    var onDeactivated: (String)->Unit = {}
    private val sessions=java.util.concurrent.ConcurrentHashMap<String,CoreSession>()
    private val mutation=Mutex()
    private val mutableRows=MutableStateFlow<List<NativePackageRow>>(emptyList())
    val rows=mutableRows.asStateFlow()
    private val mutableError=MutableStateFlow<String?>(null)
    val error=mutableError.asStateFlow()
    private val backends=java.util.concurrent.ConcurrentHashMap<Operation,CoreBackend>()
    private val mutablePanel=MutableStateFlow<PanelState?>(null)
    val panel=mutablePanel.asStateFlow()
    @Volatile var panelOwner: String? = null
        private set
    fun dismissPanel() { mutablePanel.value=null }
    fun bind(operation: Operation,backend: CoreBackend) { backends[operation]=backend }
    suspend fun invoke(id: String,componentId: String,inputs: Map<String,String> = emptyMap()): Reply = withContext(Dispatchers.IO) {
        mutation.withLock {
            require(inputs.size<=64 && inputs.all { (k,v) -> k.matches(Regex("[a-z][a-z0-9.-]{0,63}")) && v.length<=4096 && '\u0000' !in v })
            val pack=store.load(id)
            val component=pack.declarations().first { it.id==componentId }
            val operation=when(component.type) { "panel" -> Operation.PANEL_OPEN;"simpleRun","basicBuild" -> Operation.SIMPLE_RUN;"language","environment" -> return@withLock Reply(false,"E_COMPONENT");else -> Operation.valueOf(component.descriptor.getValue("operation").jsonPrimitive.content) }
            val args=if(component.type=="panel") component.descriptor.mapValues { if(it.value is JsonPrimitive) it.value.jsonPrimitive.content else it.value.toString() }
                else if(component.type in setOf("simpleRun","basicBuild")) emptyMap()
                else component.descriptor.getValue("arguments").jsonObject.mapValues { it.value.jsonPrimitive.content }
            val session=sessions[id] ?: return@withLock Reply(false,"E_DISABLED")
            if(operation==Operation.PANEL_OPEN) backends[Operation.PANEL_OPEN]=CoreBackend { request ->
                panelOwner=request.packageId
                mutablePanel.value=PanelState(request.component,request.arguments.getValue("title"),request.arguments.getValue("text"),request.arguments.getValue("status"),(request.arguments["nodes"]?.let { Json.parseToJsonElement(it).jsonArray } ?: JsonArray(emptyList())))
                Reply(true,"OPENED")
            }
            session.request(Request(operation,componentId,args+inputs.mapKeys { "input.${it.key}" }))
        }
    }
    private val mutableLibrary=MutableStateFlow(OfficialLibrary.verify(
        context.assets.open("sui-native/index.json").use { it.readBytes() },context.assets.open("sui-native/index.sig").use { it.readBytes() },rootKey))
    val library=mutableLibrary.asStateFlow()
    suspend fun refreshLibrary()=withContext(Dispatchers.IO) {
        fun fetch(path: String,limit: Int): ByteArray {
            val connection=URL("https://raw.githubusercontent.com/suidi-guliu/j-code-android/sui-code/final-product/native-library/v1/$path").openConnection() as java.net.HttpURLConnection
            connection.instanceFollowRedirects=false;connection.connectTimeout=20000;connection.readTimeout=30000
            try {
                check(connection.responseCode==200) { "Official library HTTP ${connection.responseCode}" }
                val data=connection.inputStream.use { it.readNBytes(limit+1) };check(data.size<=limit) { "Library response exceeds limit" };return data
            } finally { connection.disconnect() }
        }
        mutableLibrary.value=OfficialLibrary.verify(fetch("index.json",1024*1024),fetch("index.sig",1024),rootKey)
    }

    suspend fun restore()=action {
        reconcile()
        publish()
    }
    fun detach() {
        sessions.keys.forEach { providers.unregister(it) }
        sessions.values.forEach(CoreSession::close);sessions.clear();store.close()
        backends.clear()
        mutablePanel.value=null
    }
    private val mutableSource=MutableStateFlow<String?>(null)
    val source=mutableSource.asStateFlow()
    fun setSource(path: String?) { mutableSource.value=path }
    fun templates(): List<NativeProjectTemplate> = mutableRows.value.filter { it.record.enabled && it.pack.kind==PackKind.KIT && Capability.PROJECT in it.record.grants && Capability.FILE_WRITE in it.record.grants }.flatMap { row ->
        row.pack.declarations().filter { it.type=="project" }.mapNotNull { component ->
            runCatching {
                val args=component.descriptor.getValue("arguments").jsonObject
                if(args["role"]?.jsonPrimitive?.content!="template") return@mapNotNull null
                NativeProjectTemplate("sui-native/${row.record.id}/${component.id}",row.record.id,component.id,args.getValue("title").jsonPrimitive.content,args.getValue("description").jsonPrimitive.content,
                    Json.parseToJsonElement(args.getValue("files").jsonPrimitive.content).jsonObject.mapValues { it.value.jsonPrimitive.content })
            }.getOrNull()
        }
    }
    suspend fun applyTemplate(template: NativeProjectTemplate,root: File)=action {
        val session=sessions[template.packageId] ?: error("E_DISABLED")
        check(session.hasCapability(Capability.FILE_WRITE) && session.authorize(Request(Operation.PROJECT_READ,template.componentId,emptyMap())).accepted) { "Template permissions are missing" }
        val pack=store.load(template.packageId)
        val current=templates().single { it.id==template.id }
        check(current==template) { "Template changed; review it again" }
        dev.blamspot.jcode.fs.TemplatePublisher.apply(root,template.files.mapValues { (_,resource) ->
            require(resource.startsWith("resources/") && resource.split('/').none { it==".." || it=="." })
            pack.content(resource)
        })
    }
    fun recipe(id: String,component: String,inputs: Map<String,String> = emptyMap()): NativeRecipe = NativeRecipe.forComponent(store.load(id),sessions[id] ?: error("E_DISABLED"),component,inputs)
    fun resolve(fileName: String): RunProvider? = providers.resolve(fileName)
    suspend fun importDocument(uri: Uri): PackagePreview = withContext(Dispatchers.IO) {
        val data=context.contentResolver.openInputStream(uri)?.use { it.readNBytes(32*1024*1024+1) } ?: error("Cannot open selected file")
        require(data.size<=32*1024*1024) { "Package exceeds 32 MiB" }
        // Recognize authenticated binary kind rather than trusting a provider-supplied filename.
        val suffix=PackKind.entries.firstOrNull { data.size>=8 && data.copyOfRange(0,8).contentEquals(it.magic.toByteArray()) }?.suffix ?: error("E_MAGIC: Not a .suic or .suik package")
        PackagePreview(data,suffix,store.inspect(data,suffix),"Local document: $uri")
    }
    suspend fun download(item: LibraryItem): PackagePreview = withContext(Dispatchers.IO) {
        val url=URL(item.url)
        require(url.protocol=="https" && url.host=="raw.githubusercontent.com" && url.path.startsWith("/suidi-guliu/j-code-android/")) { "Unapproved package origin" }
        val connection=url.openConnection() as java.net.HttpURLConnection
        connection.instanceFollowRedirects=false;connection.connectTimeout=20000;connection.readTimeout=30000
        try {
            check(connection.responseCode==200) { "Package download HTTP ${connection.responseCode}" }
            val data=connection.inputStream.use { it.readNBytes(32*1024*1024+1) }
            check(data.size==item.bytes && hash(data)==item.sha256) { "Package size/SHA-256 mismatch" }
            val pack=store.inspect(data,item.kind)
            check(pack.official && pack.id==item.id && pack.version==item.version) { "Official signature/identity mismatch" }
            PackagePreview(data,item.kind,pack,item.url)
        } finally { connection.disconnect() }
    }
    suspend fun install(preview: PackagePreview,grants: Set<Capability>,allowUnverified: Boolean)=action {
        store.install(preview.bytes,preview.suffix,Consent(preview.pack.digest,grants,allowUnverified))
        onDeactivated(preview.pack.id)
        activate(preview.pack.id)
        publish()
    }
    suspend fun enable(id: String)=action { activate(id);publish() }
    suspend fun disable(id: String)=action { store.disable(id);onDeactivated(id);sessions.remove(id)?.close();providers.unregister(id);mutablePanel.value=null;publish() }
    suspend fun uninstall(id: String)=action { store.uninstall(id);onDeactivated(id);sessions.remove(id)?.close();providers.unregister(id);mutablePanel.value=null;publish() }
    suspend fun rollbackPreview(id: String): PackagePreview = withContext(Dispatchers.IO) {
        val pack=store.previous(id)
        PackagePreview(pack.first,pack.second.kind.suffix,pack.second,"Previous installed version")
    }
    private fun activate(id: String) {
        val pack=store.load(id)
        val enabled=store.records().filter { it.enabled }.map { it.id }.toSet()
        check(pack.manifest.getValue("dependencies").jsonArray.all { it.jsonObject.getValue("id").jsonPrimitive.content in enabled }) { "E_DEPENDENCY: enable the required Basic Plugin first" }
        val claimed=pack.declarations().filter { it.type in setOf("lsp","dap") }.map { c -> c.type to c.descriptor.getValue("arguments").jsonObject.getValue(if(c.type=="lsp") "serverId" else "id").jsonPrimitive.content }.toSet()
        sessions.keys.filter { it!=id }.forEach { other ->
            val conflict=store.load(other).declarations().filter { it.type in setOf("lsp","dap") }.any { c -> (c.type to c.descriptor.getValue("arguments").jsonObject.getValue(if(c.type=="lsp") "serverId" else "id").jsonPrimitive.content) in claimed }
            check(!conflict) { "E_CONFLICT: adapter ID already belongs to $other" }
        }
        val session=store.enable(id,Operation.entries.associateWith { op -> CoreBackend { request -> backends[op]?.invoke(request) ?: Reply(false,"E_UNAVAILABLE") } })
        try { providers.register(pack,session) }
        catch(e: Exception) { store.disable(id);session.close();throw e }
        sessions.put(id,session)?.close()
    }
    private fun reconcile() {
        val records=store.records()
        fun usable(id: String,visited: Set<String> = emptySet()): Boolean {
            if(id in visited) return false
            val record=records.firstOrNull { it.id==id } ?: return false
            if(!record.enabled) return false
            val pack=runCatching { store.load(id) }.getOrElse { mutableError.value="$id: ${it.message}";return false }
            return pack.manifest.getValue("dependencies").jsonArray.all { usable(it.jsonObject.getValue("id").jsonPrimitive.content,visited+id) }
        }
        sessions.keys.toList().filter { !usable(it) }.forEach { id -> onDeactivated(id);sessions.remove(id)?.close();providers.unregister(id) }
        records.filter { it.enabled && !sessions.containsKey(it.id) && usable(it.id) }.forEach { r ->
            runCatching { activate(r.id) }.onFailure { mutableError.value="${r.id}: ${it.message}" }
        }
    }
    private fun publish() { mutableRows.value=store.records().mapNotNull {
        runCatching { NativePackageRow(it,store.load(it.id)) }.onFailure { error -> mutableError.value="${it.id}: ${error.message}" }.getOrNull()
    } }
    private suspend fun action(block: ()->Unit) = withContext(Dispatchers.IO) {
        mutation.withLock {
            try { mutableError.value=null;block();reconcile();publish() }
            catch(e: Exception) { mutableError.value=e.message ?: "Native package operation failed";runCatching { publish() };throw e }
        }
    }
    companion object {
        @Volatile private var instance: NativePackageHost?=null
        fun get(context: Context): NativePackageHost = instance ?: synchronized(this) { instance ?: NativePackageHost(context.applicationContext).also { instance=it } }
        private fun hash(bytes: ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
