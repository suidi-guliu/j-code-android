package dev.suicode.nativehost

import dev.blamspot.jcode.core.distro.LspCatalogEntry
import dev.suicode.packages.Capability
import dev.suicode.packages.PackKind
import kotlinx.serialization.json.*

/** Translates authorized native Kit declarations to the existing generic LSP client. */
object NativeLanguageServices {
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
                    installCommand=text("installCommand"),verifyCommand=text("verifyCommand"),uninstallCommand=text("uninstallCommand"),
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
                    installCommand=text("installCommand"),verifyCommand=text("verifyCommand"),uninstallCommand=text("uninstallCommand"),
                    ownerPackage=row.record.id,adapterCommand=text("adapterCommand"),transport=text("transport"),debugType=text("debugType"),
                    extensions=Json.parseToJsonElement(text("extensions")).jsonArray.map { it.jsonPrimitive.content }
                )
            }.onFailure { onError("${row.record.id}/${c.id}: incomplete DAP adapter (${it.message})") }.getOrNull()
        }
    }

}
