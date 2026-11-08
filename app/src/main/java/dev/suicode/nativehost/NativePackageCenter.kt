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
        Text(source?.let { "Source context: $it" } ?: "Open a source file for language actions; project workflows use the selected Project",style=MaterialTheme.typography.bodySmall)
        TextButton(onClick={languageDialog=true}) { Text(dev.blamspot.jcode.design.uiText("ui.language")) }
        Spacer(Modifier.height(12.dp))
        ScrollableTabRow(selectedTabIndex=section,edgePadding=0.dp) {
            listOf("Plugin Library","IDE Kit Library","Installed").forEachIndexed { i,title -> Tab(selected=section==i,onClick={section=i},text={Text(title)}) }
        }
        FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled=!busy,onClick={perform { host.refreshLibrary() }}) { Text("Refresh") }
            OutlinedButton(enabled=!busy,onClick={picker.launch(arrayOf("*/*"))}) { Text("Import .suic / .suik") }
        }
        if(setupState.message.isNotEmpty()) {
            Text(setupState.message)
            if(setupState.installing) {
                LinearProgressIndicator(progress={setupState.progress/100f},modifier=Modifier.fillMaxWidth())
                TextButton(onClick=host.setup::cancel) { Text("Cancel installation safely") }
            }
            var showLog by remember { mutableStateOf(false) }
            TextButton(onClick={showLog=true}) { Text("Installation log") }
            if(showLog) AlertDialog(onDismissRequest={showLog=false},title={Text(setupState.logPath ?: "Setup log")},text={Text(setupState.log.takeLast(20000),Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState()))},confirmButton={TextButton(onClick={showLog=false}) { Text("Close") }})
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
                            Text("${item.version} · Core API ${CoreApi.VERSION} · ${item.bytes} bytes")
                            Text("${item.author} · ${item.description}\nDependencies: ${item.dependencies}")
                            Text("SHA-256: ${item.sha256}",style=MaterialTheme.typography.bodySmall)
                            Text(item.url,style=MaterialTheme.typography.bodySmall)
                            val installed=rows.firstOrNull { it.record.id==item.id }
                            if(installed!=null) Text("Installed ${installed.record.version} · ${if(installed.record.enabled) "enabled" else "disabled"}")
                            Button(enabled=!busy,onClick={perform { pending=host.download(item) }}) { Text(if(installed==null) "Download and review" else "Review update / reinstall") }
                        }
                    }
                }
            } else {
                if(rows.isEmpty()) Text("No native packages installed. Choose a library or import a package.")
                rows.forEach { row ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(row.pack.manifest.getValue("name").toString().trim('"'),style=MaterialTheme.typography.titleMedium)
                            Text("${row.record.id}\n${row.record.version} · .${row.record.kind} · ${if(row.record.enabled) "enabled" else "disabled"}")
                            Text(if(row.pack.official) "Verified sui-code signature" else "Unverified publisher")
                            Text("Permissions: "+row.record.grants.joinToString { it.wire })
                            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                TextButton(enabled=!busy,onClick={perform { if(row.record.enabled) host.disable(row.record.id) else if(row.pack.kind==PackKind.KIT) pending=host.setupPreview(row.record.id) else host.enable(row.record.id) }}) { Text(if(row.record.enabled) "Disable" else "Enable") }
                                TextButton(enabled=!busy,onClick={perform { pending=host.rollbackPreview(row.record.id) }}) { Text("Rollback") }
                            }
                            if(row.pack.declarations().all { it.type=="localization" }) Text("UI language resources · no language environment required")
                            else if(row.record.kind=="suik") {
                                Text(setupState.ready[row.record.id] ?: "Prerequisites not checked this session")
                                TextButton(enabled=!setupState.busy && !busy,onClick={perform { pending=host.setupPreview(row.record.id) }}) { Text("Review dependencies / repair") }
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
                            TextButton(enabled=!busy,onClick={confirmRemove=true}) { Text("Uninstall",color=MaterialTheme.colorScheme.error) }
                            if(confirmRemove) AlertDialog(onDismissRequest={confirmRemove=false},title={Text("Uninstall package?")},text={Text("${row.record.id}\nUbuntu, language environments, projects and source files are retained.")},confirmButton={TextButton(onClick={confirmRemove=false;perform { host.uninstall(row.record.id) }}) { Text("Uninstall") }},dismissButton={TextButton(onClick={confirmRemove=false}) { Text("Cancel") }})
                        }
                    }
                }
            }
        }
    }
    if(languageDialog) AlertDialog(onDismissRequest={languageDialog=false},title={Text("Interface language")},text={Column(Modifier.verticalScroll(rememberScrollState())) { NativeLanguageSelection(host) }},confirmButton={TextButton(onClick={languageDialog=false}) { Text("Close") }})
    replyDialog?.let { (title,text) -> AlertDialog(onDismissRequest={replyDialog=null},title={Text(title)},text={Text(text)},confirmButton={TextButton(onClick={replyDialog=null}) { Text("Close") }}) }
    pending?.let { preview -> NativePackageReview(host,preview,onDismiss={pending=null},onInstalled={section=2}) }
}
