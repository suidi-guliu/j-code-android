package dev.suicode.nativehost

import dev.blamspot.jcode.core.distro.DebugEngineEntry
import dev.blamspot.jcode.core.distro.DistroService
import dev.blamspot.jcode.core.distro.WorkspaceHostPaths
import dev.blamspot.jcode.core.distro.shellQuote
import dev.suicode.packages.*
import kotlinx.serialization.json.*
import org.json.JSONArray
import java.io.File
import java.util.UUID

/** Kit-owned launch preparation; the IDE contains no language-specific launch recipe. */
object NativeDebugPlans {
    suspend fun resolve(host: NativePackageHost,distro: DistroService,engine: DebugEngineEntry,sourcePath: String,projectPath: String,
                        rpc: suspend (String,String,JSONArray)->Any?): ResolvedDebugPlan {
        val row=host.rows.value.single { row -> row.record.enabled && Capability.DAP in row.record.grants && Capability.RUN in row.record.grants && row.pack.declarations().any { it.type=="dap" && it.descriptor.getValue("arguments").jsonObject["id"]?.jsonPrimitive?.content==engine.id } }
        val component=row.pack.declarations().single { it.type=="dap" && it.descriptor.getValue("arguments").jsonObject["id"]?.jsonPrimitive?.content==engine.id }
        val args=component.descriptor.getValue("arguments").jsonObject
        val source=WorkspaceHostPaths.hostToGuest(sourcePath)
        val project=WorkspaceHostPaths.hostToGuest(projectPath)
        val context=mutableMapOf<String,JsonElement>("source" to JsonPrimitive(source),"project" to JsonPrimitive(project),"sourceUri" to JsonPrimitive(File(source).toURI().toASCIIString()),"projectUri" to JsonPrimitive(File(project).toURI().toASCIIString()))
        args["preLaunchResource"]?.jsonPrimitive?.content?.let { resource ->
            require(resource.startsWith("resources/") && resource.split('/').none { it==".." || it=="." })
            val script="/tmp/sui-debug-${UUID.randomUUID()}.sh"
            check(distro.stageGuestFile(script,row.pack.content(resource).toString(Charsets.UTF_8))) { "Cannot stage Kit preparation script" }
            try {
                val result=distro.execOwned(owner="sui-kit:${row.record.id}/prepare",command="/bin/bash ${shellQuote(script)} ${shellQuote(source)} ${shellQuote(project)}",workdir=project,timeoutMs=300000L)
                check(result.succeeded) { "Kit preparation failed: ${result.stderr.takeLast(2000)}" }
                context["prepared"]=Json.parseToJsonElement(result.stdout.trim())
            } finally { File(distro.activeRootfsPath(),script.removePrefix("/")).delete() }
        }
        return DebugWorkflow.resolve(Json.parseToJsonElement(args.getValue("workflow").jsonPrimitive.content).jsonObject,context) { command,arguments ->
            val result=rpc(sourcePath,command,JSONArray(arguments.toString())) ?: error("Kit RPC $command needs a ready language server")
            when(result) {
                is String -> JsonPrimitive(result)
                is Number -> JsonPrimitive(result)
                is Boolean -> JsonPrimitive(result)
                else -> Json.parseToJsonElement(result.toString())
            }
        }
    }
}
