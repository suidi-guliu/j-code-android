package dev.suicode.packages

/** Version 1 protocol. No Android Context, filesystem handle, classloader or shell is exposed. */
object CoreApi { const val VERSION = 4; const val MIN_SUPPORTED = 1; fun compatible(min: Int,max: Int)=min>=1 && min<=max && min<=VERSION && max>=MIN_SUPPORTED }
enum class PackKind(val magic: String, val suffix: String, val mime: String) {
    PLUGIN("SUICPKG\u0000", "suic", "application/vnd.suicode.plugin"),
    KIT("SUIKPKG\u0000", "suik", "application/vnd.suicode.ide-kit")
}
enum class Capability(val wire: String) {
    FILE_READ("file.read"), FILE_WRITE("file.write"), RUN("process.run"),
    LSP("language.lsp"), DAP("debug.dap"), PROJECT("project.read"), UI("ui.panel"), LOCALIZATION("ui.localization")
}
enum class Operation(val permission: Capability) {
    SIMPLE_RUN(Capability.RUN), LSP_START(Capability.LSP), DAP_START(Capability.DAP),
    BUILD(Capability.RUN), TEST(Capability.RUN), FORMAT(Capability.FILE_WRITE),
    LINT(Capability.FILE_READ), PROJECT_READ(Capability.PROJECT), PANEL_OPEN(Capability.UI)
}
data class Request(val operation: Operation, val component: String, val arguments: Map<String, String>, val packageId: String = "", val digest: String = "")
data class Reply(val accepted: Boolean, val code: String, val values: Map<String, String> = emptyMap())
fun interface CoreBackend { fun invoke(request: Request): Reply }

/** A host explicitly supplies backends. Missing services fail closed; declarations do not grant rights. */
class CoreSession internal constructor(
    val packageId: String, val digest: String, private val kind: PackKind,
    private val declared: Set<Capability>, private val grants: Set<Capability>,
    private val components: Map<String, Operation?>, private val backends: Map<Operation, CoreBackend>
) : AutoCloseable {
    private var active = true
    @Synchronized fun hasCapability(capability: Capability): Boolean = active && capability in declared && capability in grants
    @Synchronized fun authorize(request: Request): Reply {
        if (!active) return Reply(false, "E_DISABLED")
        if (request.component !in components) return Reply(false, "E_COMPONENT")
        if (components[request.component] != request.operation) return Reply(false, "E_BOUNDARY")
        if (kind == PackKind.PLUGIN && request.operation != Operation.SIMPLE_RUN)
            return Reply(false, "E_BOUNDARY")
        if (request.operation.permission !in declared || request.operation.permission !in grants)
            return Reply(false, "E_PERMISSION")
        if (request.operation in setOf(Operation.SIMPLE_RUN,Operation.BUILD,Operation.TEST,Operation.LSP_START,Operation.DAP_START) &&
            (Capability.FILE_READ !in declared || Capability.FILE_READ !in grants || Capability.RUN !in declared || Capability.RUN !in grants)) return Reply(false,"E_PERMISSION")
        return Reply(true,"AUTHORIZED")
    }
    fun request(request: Request): Reply {
        val authority=authorize(request)
        if(!authority.accepted) return authority
        val trusted=request.copy(packageId=packageId,digest=digest)
        return try { backends[request.operation]?.invoke(trusted) ?: Reply(false, "E_UNAVAILABLE") }
        catch (e: Exception) { Reply(false, "E_BACKEND", mapOf("reason" to (e.message ?: e.javaClass.simpleName).take(2000), "component" to request.component)) }
    }
    @Synchronized override fun close() { active = false }
}

/** Two distinct loaders share the verifier, never the JCode extension lifecycle. */
class PluginLoader {
    fun activate(pack: VerifiedPack, consent: Consent, backends: Map<Operation, CoreBackend>): CoreSession {
        require(pack.kind == PackKind.PLUGIN) { "E_KIND" }
        return pack.activate(consent, backends)
    }
}
class IdeKitLoader {
    fun activate(pack: VerifiedPack, consent: Consent, backends: Map<Operation, CoreBackend>): CoreSession {
        require(pack.kind == PackKind.KIT) { "E_KIND" }
        return pack.activate(consent, backends)
    }
}
data class Consent(val digest: String, val grants: Set<Capability>, val allowUnverified: Boolean)

/** Declarative UI only; external DEX/APK/JS/HTML code is never loaded into the IDE process. */
data class PanelState(val id: String, val title: String, val text: String, val status: String = "ready",val nodes: kotlinx.serialization.json.JsonArray = kotlinx.serialization.json.JsonArray(emptyList())) {
    init {
        require(id.matches(Regex("[a-z][a-z0-9.-]{0,63}")))
        require(title.length in 1..80 && text.length <= 65536)
        UiNodes.validate(nodes)
        require(status in setOf("ready", "loading", "empty", "error"))
    }
}
class PanelStore {
    private val panels = mutableMapOf<String, PanelState>()
    @Synchronized fun put(session: CoreSession, panel: PanelState): Reply {
        val reply = session.request(Request(Operation.PANEL_OPEN, panel.id, mapOf("title" to panel.title, "text" to panel.text, "status" to panel.status,"nodes" to panel.nodes.toString())))
        if (reply.accepted) panels["${session.packageId}:${panel.id}"] = panel
        return reply
    }
    @Synchronized fun snapshot(): List<PanelState> = panels.values.toList()
    @Synchronized fun release(packageId: String) { panels.keys.removeAll { it.startsWith("$packageId:") } }
}
