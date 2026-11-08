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
                TextButton(onClick={advancedVisible=false}) { Text("← Language readiness") }
                TextButton(enabled=!busy && !setup.busy,onClick={busy=true;scope.launch { try { distro.checkAvailableCatalogUpdates() } catch(e: Exception) { error=e.message } finally { busy=false } }}) { Text("Check trusted-source updates") }
            }
            error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
            Box(Modifier.weight(1f)) { advanced() }
        }
    } else Column(modifier.padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(dev.blamspot.jcode.design.uiText("languages.title"),style=MaterialTheme.typography.headlineSmall)
        Text("Ubuntu ${environment.runtime.selectedDistro.label} · ${environment.runtime.selectedDistro.arch.rootfsKey}\nReuse installed runtimes. IDE Kits deploy their required development tools after one reviewed plan.")
        if(busy || setup.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        if(setup.message.isNotBlank()) {
            Text(setup.message)
            if(setup.installing) { LinearProgressIndicator(progress={setup.progress/100f},modifier=Modifier.fillMaxWidth());TextButton(onClick=host.setup::cancel) { Text("Cancel safely") } }
            var logs by remember { mutableStateOf(false) }
            TextButton(onClick={logs=!logs}) { Text(if(logs) "Hide installation log" else "Installation log") }
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
                    Text("Basic plugin: ${installedBasic?.let { "${it.record.version} · ${if(it.record.enabled) "enabled" else "disabled"}" } ?: "not installed"}")
                    Text("IDE Kit: ${kit?.let { "${it.record.version} · ${if(it.record.enabled) "enabled" else "disabled"}" } ?: "optional · not installed"}")
                    if(kit!=null && library.firstOrNull { it.id==id }?.version!=kit.record.version) Text("Updated Kit available. Review its signed dependency plan to adopt the v0.5 workflow.",color=MaterialTheme.colorScheme.primary)
                    Text(when { status==null -> "Development tools: checking when a Kit is installed";status.isEmpty() -> "Legacy Kit: review updated prerequisites";status.all { it.ready } -> "Ready · runtime and all required tools detected";else -> "Partially Ready · missing: "+status.filter { !it.ready }.joinToString { it.requirement.name } })
                    status?.forEach { Text("${if(it.ready) "✓" else "○"} ${it.requirement.name} · ${it.installedVersion}",style=MaterialTheme.typography.bodySmall) }
                    FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        if(installedBasic==null) TextButton(enabled=!busy && !setup.busy,onClick={busy=true;scope.launch { try { pending=host.download(basic) } catch(e: Exception) { error=e.message } finally { busy=false } }}) { Text("Basic run setup") }
                        Button(enabled=!busy && !setup.busy,onClick={busy=true;scope.launch { try { pending=host.setupPreview(id) } catch(e: Exception) { error=e.message } finally { busy=false } }}) { Text(if(kit==null) "Set up complete IDE Kit" else "Review / repair dependencies") }
                    }
                }
            }
        }
        TextButton(enabled=!busy && !setup.busy,onClick={revision++}) { Text("Refresh readiness") }
        TextButton(onClick={advancedVisible=true}) { Text("Advanced environment and server controls") }
    }
    pending?.let { NativePackageReview(host,it,onDismiss={pending=null},onInstalled={revision++}) }
}
