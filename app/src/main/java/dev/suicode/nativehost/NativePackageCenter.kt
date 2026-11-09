package dev.suicode.nativehost

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.suicode.packages.*
import kotlinx.coroutines.launch

/** Real document import, authenticated download, consent and lifecycle UI. No legacy market bridge. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NativePackageCenter(modifier: Modifier=Modifier,initialSection: Int=0) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    val context=LocalContext.current
    val host=remember { NativePackageHost.get(context) }
    val rows by host.rows.collectAsState()
    val library by host.library.collectAsState()
    val source by host.source.collectAsState()
    val hostError by host.error.collectAsState()
    val panel by host.panel.collectAsState()
    val scope=rememberCoroutineScope()
    val setupState by host.setup.state.collectAsState()
    var section by remember(initialSection) { mutableIntStateOf(initialSection.coerceIn(0,2)) }
    var pending by remember { mutableStateOf<PackagePreview?>(null) }
    var replyDialog by remember { mutableStateOf<Pair<String,String>?>(null) }
    var languageDialog by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun perform(action: suspend ()->Unit) {
        if(busy) return
        busy=true;error=null
        scope.launch {
            try { action() } catch(e: Exception) { error=e.message ?: "Package operation failed" }
            finally { busy=false }
        }
    }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) perform { pending=host.importDocument(uri) }
    }
    Column(modifier.padding(16.dp)) {
        Text(dev.blamspot.jcode.design.uiText("packages.title"),style=MaterialTheme.typography.titleLarge)
        Text(source?.let { strings.text("packages.sourceContext", mapOf("path" to (it).toString())) } ?: strings.text("ui.openASourceFileForLanguageActionsProjectWorkflows"),style=MaterialTheme.typography.bodySmall)
        TextButton(onClick={languageDialog=true}) { Text(dev.blamspot.jcode.design.uiText("ui.language")) }
        Spacer(Modifier.height(12.dp))
        ScrollableTabRow(selectedTabIndex=section,edgePadding=0.dp) {
            listOf(strings.text("option.PluginLibrary"),strings.text("option.IDEKitLibrary"),strings.text("option.Installed")).forEachIndexed { i,title -> Tab(selected=section==i,onClick={section=i},text={Text(title)}) }
        }
        FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled=!busy,onClick={perform { host.refreshLibrary() }}) { Text(strings.text("files.refresh")) }
            OutlinedButton(enabled=!busy,onClick={picker.launch(arrayOf("*/*"))}) { Text(strings.text("ui.importSuicSuik")) }
        }
        if(setupState.message.isNotEmpty()) {
            Text(when(setupState.message) { "Checking prerequisites…" -> strings.text("packages.checking"); "Installing reviewed package resources" -> strings.text("packages.installingResources"); "Ready · package resources enabled; no language environment required" -> strings.text("packages.resourcesReady"); else -> setupState.message })
            if(setupState.installing) {
                LinearProgressIndicator(progress={setupState.progress/100f},modifier=Modifier.fillMaxWidth())
                TextButton(onClick=host.setup::cancel) { Text(strings.text("ui.cancelInstallationSafely")) }
            }
            var showLog by remember { mutableStateOf(false) }
            TextButton(onClick={showLog=true}) { Text(strings.text("ui.installationLog")) }
            if(showLog) AlertDialog(onDismissRequest={showLog=false},title={Text(setupState.logPath ?: strings.text("ui.setupLog"))},text={Text(setupState.log.takeLast(20000),Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState()))},confirmButton={TextButton(onClick={showLog=false}) { Text(strings.text("ui.close")) }})
        }
        feedback?.let { Text(it) }
        if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        (error ?: hostError)?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(section<2) {
                val kind=if(section==0) "suic" else "suik"
                library.filter { it.kind==kind }.forEach { item ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(item.name,style=MaterialTheme.typography.titleMedium)
                            Text(strings.text("packages.metadata", mapOf("version" to (item.version).toString(), "api" to (CoreApi.VERSION).toString(), "bytes" to (item.bytes).toString())))
                            Text(strings.text("packages.dependencies", mapOf("author" to (item.author).toString(), "description" to (item.description).toString(), "dependencies" to (item.dependencies).toString())))
                            Text("SHA-256: ${item.sha256}",style=MaterialTheme.typography.bodySmall)
                            Text(item.url,style=MaterialTheme.typography.bodySmall)
                            val installed=rows.firstOrNull { it.record.id==item.id }
                            if(installed!=null) Text(strings.text("packages.installedStatus", mapOf("version" to (installed.record.version).toString(), "status" to (strings.text(if(installed.record.enabled) "status.enabled" else "status.disabled")).toString())))
                            Button(enabled=!busy,onClick={perform { pending=host.download(item) }}) { Text(if(installed==null) strings.text("ui.downloadAndReview") else strings.text("ui.reviewUpdateReinstall")) }
                        }
                    }
                }
            } else {
                if(rows.isEmpty()) Text(strings.text("ui.noNativePackagesInstalledChooseALibraryOrImport"))
                rows.forEach { row ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(row.pack.manifest.getValue("name").toString().trim('"'),style=MaterialTheme.typography.titleMedium)
                            Text(strings.text("packages.record", mapOf("id" to (row.record.id).toString(), "version" to (row.record.version).toString(), "kind" to (row.record.kind).toString(), "status" to (strings.text(if(row.record.enabled) "status.enabled" else "status.disabled")).toString())))
                            Text(if(row.pack.official) "Verified sui-code signature" else "Unverified publisher")
                            Text("Permissions: "+row.record.grants.joinToString { it.wire })
                            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                TextButton(enabled=!busy,onClick={perform { if(row.record.enabled) host.disable(row.record.id) else if(row.pack.kind==PackKind.KIT) pending=host.setupPreview(row.record.id) else host.enable(row.record.id) }}) { Text(if(row.record.enabled) strings.text("ui.disable") else strings.text("ui.enable")) }
                                TextButton(enabled=!busy,onClick={perform { pending=host.rollbackPreview(row.record.id) }}) { Text(strings.text("ui.rollback")) }
                            }
                            if(row.pack.declarations().all { it.type=="localization" }) Text(strings.text("ui.uILanguageResourcesNoLanguageEnvironmentRequired"))
                            else if(row.record.kind=="suik") {
                                Text(setupState.ready[row.record.id] ?: strings.text("ui.prerequisitesNotCheckedThisSession"))
                                TextButton(enabled=!setupState.busy && !busy,onClick={perform { pending=host.setupPreview(row.record.id) }}) { Text(strings.text("ui.reviewDependenciesRepair")) }
                            }
                            if(row.record.kind=="suik" && row.record.enabled) row.pack.declarations().filter { it.type !in setOf("project","language","environment","setup","panel","localization") }.forEach { component ->
                                TextButton(enabled=!busy,onClick={perform {
                                    val reply=host.invoke(row.record.id,component.id)
                                    check(reply.accepted) { reply.code+": "+reply.values.toString() }
                                    feedback="${component.id}: ${reply.code}"
                                    if(reply.values.isNotEmpty()) replyDialog=component.id to reply.values.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                                }}) { Text(component.id) }
                            }
                            var confirmRemove by remember(row.record.id) { mutableStateOf(false) }
                            TextButton(enabled=!busy,onClick={confirmRemove=true}) { Text(strings.text("ui.uninstall"),color=MaterialTheme.colorScheme.error) }
                            if(confirmRemove) AlertDialog(onDismissRequest={confirmRemove=false},title={Text(strings.text("ui.uninstallPackage"))},text={Text(strings.text("packages.removeDescription", mapOf("id" to (row.record.id).toString())))},confirmButton={TextButton(onClick={confirmRemove=false;perform { host.uninstall(row.record.id) }}) { Text(strings.text("ui.uninstall")) }},dismissButton={TextButton(onClick={confirmRemove=false}) { Text(strings.text("ui.cancel")) }})
                        }
                    }
                }
            }
        }
    }
    if(languageDialog) AlertDialog(onDismissRequest={languageDialog=false},title={Text(strings.text("ui.language"))},text={Column(Modifier.verticalScroll(rememberScrollState())) { NativeLanguageSelection(host) }},confirmButton={TextButton(onClick={languageDialog=false}) { Text(strings.text("ui.close")) }})
    replyDialog?.let { (title,text) -> AlertDialog(onDismissRequest={replyDialog=null},title={Text(title)},text={Text(text)},confirmButton={TextButton(onClick={replyDialog=null}) { Text(strings.text("ui.close")) }}) }
    pending?.let { preview -> NativePackageReview(host,preview,onDismiss={pending=null},onInstalled={section=2}) }
}
