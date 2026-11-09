package dev.suicode.nativehost

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.suicode.packages.*
import kotlinx.coroutines.launch

/** Real document import, authenticated download, consent and lifecycle UI. No legacy market bridge. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NativePackageCenter(modifier: Modifier=Modifier,initialSection: Int=0,serverStates: kotlinx.coroutines.flow.StateFlow<List<dev.blamspot.jcode.lsp.LspServerStatus>>? = null,restartServer: (String)->Unit = {},serverLog: (String)->String = { "" },dapState: kotlinx.coroutines.flow.StateFlow<dev.blamspot.jcode.core.debug.DebugState>? = null,ownsDebug: (String)->Boolean = { false }) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    val debugging=dapState?.collectAsState()?.value?.name
    val liveServers=serverStates?.collectAsState()?.value.orEmpty().groupBy { it.serverId }.mapValues { (_,states) -> states.joinToString("\n") { "${it.state}: ${it.root} ${it.detail.orEmpty()}" } }
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
    var search by rememberSaveable { mutableStateOf("") }
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
            try { action() } catch(e: CancellationException) { throw e } catch(e: Exception) { error=e.message ?: "Package operation failed" }
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
        OutlinedTextField(search,{search=it},Modifier.fillMaxWidth().testTag("packages-search"),singleLine=true,label={Text(strings.text("management.searchPackages"))},trailingIcon={if(search.isNotEmpty()) TextButton(onClick={search=""}) { Text(strings.text("management.clear")) }})
        Spacer(Modifier.height(12.dp))
        ScrollableTabRow(selectedTabIndex=section,edgePadding=0.dp) {
            listOf(strings.text("option.PluginLibrary"),strings.text("option.IDEKitLibrary"),strings.text("option.Installed")).forEachIndexed { i,title -> Tab(selected=section==i,onClick={section=i},text={Text(title)}) }
        }
        FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled=!busy,onClick={perform { host.refreshLibrary() }}) { Text(strings.text("files.refresh")) }
            OutlinedButton(enabled=!busy,onClick={picker.launch(arrayOf("*/*"))}) { Text(strings.text("ui.importSuicSuik")) }
        }
        dev.blamspot.jcode.feature.sdkmanager.InstallationLogHistory(remember(context) { dev.blamspot.jcode.core.distro.DistroServiceLocator.distroService(context) },"ide-component")
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
            val ids=if(section==2) rows.map { it.record.id } else (library.filter { it.kind==if(section==0) "suic" else "suik" }.map { it.id }+rows.filter { it.record.kind==if(section==0) "suic" else "suik" }.map { it.record.id }).distinct()
            val visible=ids.filter { id ->
                val row=rows.firstOrNull { it.record.id==id };val item=library.firstOrNull { it.id==id }
                val category=row?.pack?.let { PackageCards.forPack(it).category.wire } ?: item?.category ?: "general"
                val metadata=listOf(category,strings.text("management.category."+category.replace("-","")),if(row==null) "not installed 未安装" else "installed 已安装",id,item?.name,item?.author,item?.description,item?.version,row?.pack?.manifest?.toString(),row?.record?.version,strings.text(if(row==null) "status.notInstalled" else if(row.record.enabled) "status.enabled" else "status.disabled"),if(row?.pack?.official==true) "official 官方" else "third-party 第三方",if(row?.pack?.declarations()?.any { it.type=="localization" }==true) "中文 简体 Chinese localization" else "").joinToString(" ")
                search.trim().split(Regex("\\s+")).all { metadata.contains(it,ignoreCase=true) }
            }
            if(visible.isEmpty()) Text(strings.text("management.noResults"),Modifier.testTag("packages-empty"))
            visible.forEach { id ->
                key(id) {
                    val row=rows.firstOrNull { it.record.id==id };val item=library.firstOrNull { it.id==id }
                    NativePackageCard(host,item,row,busy,review={perform { pending=if(item!=null && (row==null || item.version!=row.record.version)) host.download(item) else host.setupPreview(id) }},rollback={perform { pending=host.rollbackPreview(id) }},perform=::perform,showLanguage={languageDialog=true},
                        invoke={component -> perform { val reply=host.invoke(id,component);check(reply.accepted) { reply.code+": "+reply.values.toString() };feedback="$component: ${reply.code}";if(reply.values.isNotEmpty()) replyDialog=component to reply.values.entries.joinToString("\n") { "${it.key}: ${it.value}" } }},
                        showText={title,text -> replyDialog=title to text},serverStates=liveServers,restartServer=restartServer,serverLog=serverLog,dapStatus=debugging?.takeIf { ownsDebug(id) })
                }
            }
        }
    }
    if(languageDialog) AlertDialog(onDismissRequest={languageDialog=false},title={Text(strings.text("ui.language"))},text={Column(Modifier.verticalScroll(rememberScrollState())) { NativeLanguageSelection(host) }},confirmButton={TextButton(onClick={languageDialog=false}) { Text(strings.text("ui.close")) }})
    replyDialog?.let { (title,text) -> AlertDialog(onDismissRequest={replyDialog=null},title={Text(title)},text={Text(text)},confirmButton={TextButton(onClick={replyDialog=null}) { Text(strings.text("ui.close")) }}) }
    pending?.let { preview -> NativePackageReview(host,preview,onDismiss={pending=null},onInstalled={section=2}) }
}
