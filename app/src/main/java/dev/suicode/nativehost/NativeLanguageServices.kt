package dev.suicode.nativehost

import dev.blamspot.jcode.core.distro.LspCatalogEntry
import dev.suicode.packages.Capability
import dev.suicode.packages.PackKind
import kotlinx.serialization.json.*
import kotlinx.coroutines.sync.withLock

/** Translates authorized native Kit declarations to the existing generic LSP client. */
object NativeLanguageServices {
    private val refreshMutex=kotlinx.coroutines.sync.Mutex()
    suspend fun refreshAndCheck(host: NativePackageHost,distro: dev.blamspot.jcode.core.distro.DistroService,onError: (String)->Unit) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        refreshMutex.withLock {
            val latest=host.rows.value
            dev.blamspot.jcode.core.distro.LspServerCatalog.registerNative(entries(latest,onError))
            dev.blamspot.jcode.core.distro.DebugEngineCatalog.setContributed(engines(latest,onError))
            distro.refreshLspCatalog();distro.refreshDebugEngineCatalog();distro.invalidateCatalogProbes()
            if(distro.isRuntimeReady()) { distro.checkLspStatuses();distro.checkDebugEngineStatuses() }
        }
    }
    private fun dependenciesEnabled(row: NativePackageRow,rows: List<NativePackageRow>): Boolean = row.pack.manifest.getValue("dependencies").jsonArray.all { dep ->
        rows.any { it.record.enabled && it.record.id==dep.jsonObject.getValue("id").jsonPrimitive.content }
    }
    fun entries(rows: List<NativePackageRow>,onError: (String)->Unit): List<LspCatalogEntry> = rows.filter {
        it.record.enabled && dependenciesEnabled(it,rows) && it.pack.kind==PackKind.KIT && Capability.LSP in it.record.grants && Capability.RUN in it.record.grants && Capability.FILE_READ in it.record.grants
    }.flatMap { row ->
        row.pack.declarations().filter { it.type=="lsp" }.mapNotNull { c ->
            runCatching {
                val args=c.descriptor.getValue("arguments").jsonObject
                fun text(key: String)=args.getValue(key).jsonPrimitive.content
                fun list(key: String)=Json.parseToJsonElement(text(key)).jsonArray.map { it.jsonPrimitive.content }
                LspCatalogEntry(
                    ownerPackage=row.record.id,id=text("serverId"),category="sui-code IDE Kit",name=text("name"),description=row.record.id,
                    installCommand="printf '%s\\n' 'Manage this IDE component in Packages > IDE Kit Library' >&2; exit 78",verifyCommand=text("verifyCommand"),uninstallCommand="printf '%s\\n' 'Manage this IDE component in Packages > IDE Kit Library; installed tools and runtime data are retained' >&2; exit 78",
                    runCommand=text("runCommand"),languageIds=list("languageIds"),extensions=list("extensions"),rootDetectors=list("rootDetectors"),initializationOptions=text("initializationOptions")
                )
            }.onFailure { onError("${row.record.id}/${c.id}: incomplete LSP adapter (${it.message})") }.getOrNull()
        }
    }
    fun engines(rows: List<NativePackageRow>,onError: (String)->Unit): List<dev.blamspot.jcode.core.distro.DebugEngineEntry> = rows.filter {
        it.record.enabled && dependenciesEnabled(it,rows) && it.pack.kind==PackKind.KIT && Capability.DAP in it.record.grants && Capability.RUN in it.record.grants && Capability.FILE_READ in it.record.grants
    }.flatMap { row ->
        row.pack.declarations().filter { it.type=="dap" }.mapNotNull { c ->
            runCatching {
                val args=c.descriptor.getValue("arguments").jsonObject
                fun text(key: String)=args.getValue(key).jsonPrimitive.content
                dev.blamspot.jcode.core.distro.DebugEngineEntry(
                    id=text("id"),category="sui-code IDE Kit",name=text("name"),description=row.record.id,
                    installCommand="printf '%s\\n' 'Manage this IDE component in Packages > IDE Kit Library' >&2; exit 78",verifyCommand=text("verifyCommand"),uninstallCommand="printf '%s\\n' 'Manage this IDE component in Packages > IDE Kit Library; installed tools and runtime data are retained' >&2; exit 78",
                    ownerPackage=row.record.id,adapterCommand=text("adapterCommand"),transport=text("transport"),debugType=text("debugType"),
                    extensions=Json.parseToJsonElement(text("extensions")).jsonArray.map { it.jsonPrimitive.content }
                )
            }.onFailure { onError("${row.record.id}/${c.id}: incomplete DAP adapter (${it.message})") }.getOrNull()
        }
    }

}
