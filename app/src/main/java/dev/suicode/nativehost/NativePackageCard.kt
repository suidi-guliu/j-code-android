package dev.suicode.nativehost

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.suicode.packages.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*

/** One renderer for library entries and local packages. A card never decides trust or permissions. */
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun NativePackageCard(
    host: NativePackageHost,item: LibraryItem?,row: NativePackageRow?,busy: Boolean,
    review: ()->Unit,rollback: ()->Unit,perform: (suspend ()->Unit)->Unit,showLanguage: ()->Unit,
    invoke: (String)->Unit,showText: (String,String)->Unit,
    serverStates: Map<String,String>,restartServer: (String)->Unit,serverLog: (String)->String,dapStatus: String?,expandedDetails: Boolean=false,
) {
    val uriHandler=androidx.compose.ui.platform.LocalUriHandler.current
    val strings=dev.blamspot.jcode.design.LocalUiTranslations.current
    val pack=row?.pack
    val id=pack?.id ?: item!!.id
    val card=remember(pack?.digest) { pack?.let(PackageCards::forPack) ?: PackageCard() }
    val setup by host.setup.state.collectAsState()
    var status by remember(pack?.digest) { mutableStateOf<List<KitDependencyStatus>>(emptyList()) }
    var failure by remember(pack?.digest) { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    var remove by remember { mutableStateOf(false) }
    LaunchedEffect(pack?.digest,setup.installing,revision) {
        if(pack==null || pack.kind!=PackKind.KIT || setup.installing) return@LaunchedEffect
        try { status=host.setup.inspect(pack,revision>0);failure=null }
        catch(e: CancellationException) { throw e }
        catch(e: Exception) { failure=e.message }
    }
    var largeDetails by remember { mutableStateOf(false) }
    if(largeDetails) dev.blamspot.jcode.workbench.WorkbenchDetailDialog(item?.name ?: id,{largeDetails=false}) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { NativePackageCard(host,item,row,busy,review,rollback,perform,showLanguage,invoke,showText,serverStates,restartServer,serverLog,dapStatus,true) }
    }
    val localization=item?.category=="localization" || card.category==CardCategory.LOCALIZATION || pack?.declarations()?.any { it.type=="localization" }==true
    OutlinedCard(Modifier.fillMaxWidth().testTag(if(expandedDetails) "package-details-$id" else "package-$id")) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if(!expandedDetails) TextButton(onClick={largeDetails=true}) { Text(strings.text("focus.details")) }
            Text(pack?.manifest?.getValue("name")?.jsonPrimitive?.content ?: item!!.name,style=MaterialTheme.typography.titleMedium)
            Text(id,style=MaterialTheme.typography.bodySmall)
            Text("${pack?.version ?: item!!.version} · ${pack?.manifest?.getValue("author")?.jsonPrimitive?.content ?: item!!.author}")
            Text(strings.text("management.category")+": "+strings.text("management.category.${if(localization) "localization" else (if(pack==null) item!!.category else card.category.wire).replace("-","")}"))
            Text(UiTextKeys.english.getValue(if(pack?.official==true || pack==null && item?.signer?.isNotEmpty()==true) "management.verified" else "management.unverified"),color=if(pack?.official==true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(strings.text(if(row==null) "status.notInstalled" else if(row.record.enabled) "status.enabled" else "status.disabled"))
            item?.let { Text(it.url,style=MaterialTheme.typography.bodySmall) }
            if(item!=null && row!=null && item.version!=row.record.version) Text(strings.text("management.updateAvailable",mapOf("version" to item.version)),color=MaterialTheme.colorScheme.primary)
            if(pack==null) {
                Text(item!!.description)
                Text("SHA-256: ${item.sha256}",style=MaterialTheme.typography.bodySmall)
            } else {
                Text(pack.manifest.getValue("description").jsonPrimitive.content)
                Text(strings.text("management.permissions")+": "+row!!.record.grants.joinToString { it.wire },style=MaterialTheme.typography.bodySmall)
                if(card.category==CardCategory.LANGUAGE_IDE_KIT) {
                    Text(strings.text("management.basicPlugin")+": "+(card.basicPlugin ?: strings.text("management.notProvided")))
                    LanguageKitSlot.entries.forEach { slot ->
                        val reference=card.slots[slot]
                        val component=pack.declarations().firstOrNull { it.id==reference }
                        val statusText=when {
                            reference==null -> strings.text("management.notProvided")
                            slot==LanguageKitSlot.RUNTIME -> status.firstOrNull { it.requirement.id==reference }?.let { if(it.ready) strings.text("management.detected") else strings.text("management.runtimeNeedsAttention") } ?: strings.text("packages.checking")
                            slot==LanguageKitSlot.DAP -> dapStatus ?: strings.text("management.notRunning")
                            slot==LanguageKitSlot.LSP -> component?.descriptor?.get("arguments")?.jsonObject?.get("serverId")?.jsonPrimitive?.content?.let { serverStates[it] } ?: strings.text("management.notRunning")
                            else -> strings.text("management.declared")
                        }
                        Text("${slot.wire.uppercase()}: $statusText",color=if(reference==null) MaterialTheme.colorScheme.onSurface.copy(alpha=0.45f) else MaterialTheme.colorScheme.onSurface)
                    }
                }
                card.fields.forEach { field ->
                    if(field.type=="link") TextButton(onClick={try { uriHandler.openUri(field.value) } catch(e: Exception) { showText(field.label,e.message.orEmpty()) }}) { Text("${field.label}: ${field.value}") }
                    else Text("${field.label}: ${field.value}",style=MaterialTheme.typography.bodySmall)
                }
                status.forEach { component ->
                    val r=component.requirement
                    Text("${if(component.ready) "✓" else "○"} ${r.name} · ${component.installedVersion}",style=MaterialTheme.typography.bodySmall)
                    if(r.provider=="environment") Text(strings.text("management.runtimeReadOnly"),style=MaterialTheme.typography.bodySmall)
                }
                failure?.let { Text(it,color=MaterialTheme.colorScheme.error) }
                if(localization) {
                    Text(strings.text("ui.uILanguageResourcesNoLanguageEnvironmentRequired"))
                    TextButton(onClick=showLanguage) { Text(strings.text("ui.language")) }
                }
                if(pack.kind==PackKind.KIT) {
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        TextButton(enabled=!busy && !setup.busy,onClick=review) { Text(strings.text("management.selectComponents")) }
                        TextButton(enabled=!setup.busy,onClick={revision++}) { Text(strings.text("files.refresh")) }
                    }
                    pack.declarations().filter { it.type=="lsp" }.forEach { component ->
                        val server=component.descriptor.getValue("arguments").jsonObject["serverId"]?.jsonPrimitive?.content
                        if(server.isNullOrBlank()) {
                            Text("E_COMPONENT: ${component.id}/serverId",color=MaterialTheme.colorScheme.error)
                            return@forEach
                        }
                        Text("$server · ${serverStates[server] ?: strings.text("management.notRunning")}")
                        FlowRow {
                            TextButton(onClick={restartServer(server)}) { Text(strings.text("management.restartServer")) }
                            TextButton(onClick={showText(server,serverLog(server))}) { Text(strings.text("management.serverLog")) }
                        }
                    }
                }
                if(row.record.enabled) {
                    val actions=if(card.actions.isNotEmpty()) card.actions else pack.declarations().filter { it.type in setOf("panel","command","build","test","format","lint","workflow") }.map { it.id }
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { actions.forEach { component -> TextButton(enabled=!busy,onClick={invoke(component)}) { Text(component) } } }
                }
            }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(enabled=!busy && !setup.busy,onClick=review) { Text(strings.text(if(row==null) "ui.downloadAndReview" else "ui.reviewUpdateReinstall")) }
                if(row!=null) {
                    TextButton(enabled=!busy,onClick={if(row.record.enabled) perform { host.disable(id) } else if(pack!!.kind==PackKind.KIT) review() else perform { host.enable(id) }}) { Text(strings.text(if(row.record.enabled) "ui.disable" else "ui.enable")) }
                    TextButton(enabled=!busy,onClick={perform { showText(strings.text("management.permissions"),row.record.grants.joinToString("\n") { it.wire }) }}) { Text(strings.text("management.permissions")) }
                    TextButton(enabled=!busy,onClick=rollback) { Text(strings.text("ui.rollback")) }
                    TextButton(enabled=!busy,onClick={remove=true}) { Text(strings.text("ui.uninstall"),color=MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
    if(remove) AlertDialog(onDismissRequest={remove=false},title={Text(strings.text("ui.uninstallPackage"))},text={Text(strings.text("packages.removeDescription",mapOf("id" to id)))},confirmButton={TextButton(onClick={remove=false;perform { host.uninstall(id) }}) { Text(strings.text("ui.uninstall")) }},dismissButton={TextButton(onClick={remove=false}) { Text(strings.text("ui.cancel")) }})
}
