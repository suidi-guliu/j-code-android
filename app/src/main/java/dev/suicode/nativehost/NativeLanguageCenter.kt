package dev.suicode.nativehost

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.blamspot.jcode.core.distro.DistroServiceLocator
import dev.suicode.packages.*
import kotlinx.coroutines.*

/** The normal path is language readiness + one reviewed plan. Catalog internals remain advanced. */
@Composable fun NativeLanguageCenter(modifier: Modifier=Modifier,advanced: @Composable ()->Unit) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    val context=LocalContext.current
    val host=remember { NativePackageHost.get(context) }
    val distro=remember { DistroServiceLocator.distroService(context) }
    val rows by host.rows.collectAsState()
    val library by host.library.collectAsState()
    val setup by host.setup.state.collectAsState()
    val environment by distro.environmentState.collectAsState()
    var advancedVisible by remember { mutableStateOf(false) }
    var statuses by remember { mutableStateOf<Map<String,List<KitDependencyStatus>>>(emptyMap()) }
    var pending by remember { mutableStateOf<PackagePreview?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    val scope=rememberCoroutineScope()
    DisposableEffect(Unit) {
        val cookie=System.identityHashCode(host)
        android.os.Trace.beginAsyncSection("sui.languages.visible",cookie)
        android.util.Log.i("SuiPerformance","Languages enter pid=${android.os.Process.myPid()} at=${android.os.SystemClock.elapsedRealtime()}")
        onDispose { android.os.Trace.endAsyncSection("sui.languages.visible",cookie);android.util.Log.i("SuiPerformance","Languages leave") }
    }
    LaunchedEffect(rows,environment.runtime.selectedDistro.id,setup.installing,revision) {
        if(setup.installing) return@LaunchedEffect
        for(row in rows.filter { it.pack.kind==PackKind.KIT }) {
            try { val result=host.setup.inspect(row.pack,revision>0);statuses=statuses+(row.record.id to result) }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { error="Readiness check: ${e.message}. Retry without changing the installed environment." }
            yield()
        }
    }
    if(advancedVisible) {
        Column(modifier) {
            FlowRow {
                TextButton(onClick={advancedVisible=false}) { Text(strings.text("ui.languageReadiness")) }
                TextButton(enabled=!busy && !setup.busy,onClick={busy=true;scope.launch { try { distro.checkAvailableCatalogUpdates() } catch(e: Exception) { error=e.message } finally { busy=false } }}) { Text(strings.text("ui.checkTrustedSourceUpdates")) }
            }
            error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
            Box(Modifier.weight(1f)) { advanced() }
        }
    } else Column(modifier.padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(dev.blamspot.jcode.design.uiText("languages.title"),style=MaterialTheme.typography.headlineSmall)
        Text(strings.text("languages.context", mapOf("distro" to (environment.runtime.selectedDistro.label).toString(), "architecture" to (environment.runtime.selectedDistro.arch.rootfsKey).toString())))
        if(busy || setup.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        if(setup.message.isNotBlank()) {
            Text(when(setup.message) { "Checking prerequisites…" -> strings.text("packages.checking"); "Installing reviewed package resources" -> strings.text("packages.installingResources"); "Ready · package resources enabled; no language environment required" -> strings.text("packages.resourcesReady"); else -> setup.message })
            if(setup.installing) { LinearProgressIndicator(progress={setup.progress/100f},modifier=Modifier.fillMaxWidth());TextButton(onClick=host.setup::cancel) { Text(strings.text("ui.cancelSafely")) } }
            var logs by remember { mutableStateOf(false) }
            TextButton(onClick={logs=!logs}) { Text(if(logs) strings.text("ui.hideInstallationLog") else strings.text("ui.installationLog")) }
            if(logs) Text("${setup.logPath.orEmpty()}\n${setup.log.takeLast(12000)}",style=MaterialTheme.typography.bodySmall)
        }
        library.filter { it.kind=="suic" }.forEach { basic ->
            val id=basic.id.removeSuffix(".basic")+".kit"
            val installedBasic=rows.firstOrNull { it.record.id==basic.id }
            val kit=rows.firstOrNull { it.record.id==id }
            val status=statuses[id]
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(basic.name.removeSuffix(" Basic Plugin"),style=MaterialTheme.typography.titleLarge)
                    Text(strings.text("languages.basicStatus", mapOf("status" to (installedBasic?.let { it.record.version+" · "+strings.text(if(it.record.enabled) "status.enabled" else "status.disabled") } ?: strings.text("status.notInstalled")).toString())))
                    Text(strings.text("languages.kitStatus", mapOf("status" to (kit?.let { it.record.version+" · "+strings.text(if(it.record.enabled) "status.enabled" else "status.disabled") } ?: strings.text("status.optionalNotInstalled")).toString())))
                    if(kit!=null && library.firstOrNull { it.id==id }?.version!=kit.record.version) Text(strings.text("ui.updatedKitAvailableReviewItsSignedDependencyPlanTo"),color=MaterialTheme.colorScheme.primary)
                    Text(when { status==null -> strings.text("ui.developmentToolsCheckingWhenAKitIsInstalled");status.isEmpty() -> strings.text("ui.legacyKitReviewUpdatedPrerequisites");status.all { it.ready } -> strings.text("ui.readyRuntimeAndAllRequiredToolsDetected");else -> strings.text("languages.partial", mapOf("tools" to (status.filter { !it.ready }.joinToString { it.requirement.name }).toString())) })
                    status?.forEach { Text("${if(it.ready) "✓" else "○"} ${it.requirement.name} · ${it.installedVersion}",style=MaterialTheme.typography.bodySmall) }
                    FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        if(installedBasic==null) TextButton(enabled=!busy && !setup.busy,onClick={busy=true;scope.launch { try { pending=host.download(basic) } catch(e: Exception) { error=e.message } finally { busy=false } }}) { Text(strings.text("ui.basicRunSetup")) }
                        Button(enabled=!busy && !setup.busy,onClick={busy=true;scope.launch { try { pending=host.setupPreview(id) } catch(e: Exception) { error=e.message } finally { busy=false } }}) { Text(if(kit==null) strings.text("ui.setUpCompleteIDEKit") else strings.text("ui.reviewRepairDependencies")) }
                    }
                }
            }
        }
        TextButton(enabled=!busy && !setup.busy,onClick={revision++}) { Text(strings.text("ui.refreshReadiness")) }
        TextButton(onClick={advancedVisible=true}) { Text(strings.text("ui.advancedEnvironmentAndServerControls")) }
    }
    pending?.let { NativePackageReview(host,it,onDismiss={pending=null},onInstalled={revision++}) }
}
