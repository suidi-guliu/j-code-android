package dev.suicode.nativehost

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.suicode.packages.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Shared by Packages and Languages. One consent covers exact archives, capabilities and tools. */
@Composable fun NativePackageReview(host: NativePackageHost,preview: PackagePreview,onDismiss: ()->Unit,onInstalled: ()->Unit={}) {
    val scope=rememberCoroutineScope()
    val state by host.setup.state.collectAsState()
    var plan by remember(preview.pack.digest) { mutableStateOf<KitSetupPlan?>(null) }
    var error by remember(preview.pack.digest) { mutableStateOf<String?>(null) }
    var preparing by remember(preview.pack.digest) { mutableStateOf(false) }
    var grants by remember(preview.pack.digest) { mutableStateOf(emptySet<Capability>()) }
    var trusted by remember(preview.pack.digest) { mutableStateOf(preview.pack.official) }
    var retry by remember { mutableIntStateOf(0) }
    val kit=preview.pack.kind==PackKind.KIT
    LaunchedEffect(preview.pack.digest,retry) {
        if(kit) {
            preparing=true;error=null
            try { plan=host.setup.prepare(preview) }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { error=e.message }
            finally { preparing=false }
        }
    }
    val permissions=preview.pack.permissions+(plan?.dependencies?.flatMap { it.pack.permissions } ?: emptyList())
    AlertDialog(
        onDismissRequest={if(!state.installing) onDismiss()},
        title={Text("Review ${if(kit) "IDE Kit installation plan" else "native plugin"}")},
        text={Column(Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("${preview.pack.id} · ${preview.pack.version}\n${preview.source}\n${preview.bytes.size} bytes · SHA-256 ${preview.pack.digest}")
            Text(if(preview.pack.official) "Official signature verified" else "Unverified publisher",color=if(preview.pack.official) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            if(preparing) { LinearProgressIndicator(Modifier.fillMaxWidth());Text("Checking installed tools and trusted source metadata on background threads…") }
            error?.let { Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick={retry++}) { Text("Retry plan") } }
            plan?.let { p ->
                Text("Ubuntu ${p.distroId} · ${p.architecture} · Core API ${CoreApi.VERSION}")
                p.aptDownloadBytes?.let { Text("Shared Ubuntu package download: $it bytes (including apt dependencies)") }
                p.dependencies.forEach { d -> Text("Required package: ${d.pack.id} ${d.pack.version}\n${d.source}\n${d.bytes.size} bytes · SHA-256 ${d.pack.digest}\nPermissions: ${d.pack.permissions.joinToString { it.wire }}") }
                p.status.forEach { row ->
                    val r=row.requirement
                    HorizontalDivider()
                    Text("${r.name}: ${if(row.ready) "Reuse installed" else "Install / repair"}",style=MaterialTheme.typography.titleSmall)
                    Text("${row.installedVersion}\nRequired/source version: ${r.version}\nSource: ${r.source}\nDownload: ${if(row.ready) "0 bytes · reuse installed" else if(r.provider=="ubuntu-apt" && p.aptDownloadBytes!=null) "included in shared Ubuntu transaction" else r.bytes?.let { "$it bytes" } ?: "size unavailable from source; shown in terminal before transfer"}")
                    if(r.provider=="ubuntu-apt" && p.aptDownloadBytes!=null) Text("Uses the shared Ubuntu download shown above.")
                    if(!row.ready) Text("Behavior: ${r.provider} · ${r.packages.joinToString()}${r.resource?.let { " · authenticated $it" }.orEmpty()}")
                }
                Text("Completed tools stay installed on cancellation or uninstall. Package archives update atomically and support rollback. No environment, project or user data is removed.")
            }
            Text("Permissions and execution: Ubuntu processes can access app/guest files; PRoot is not a hostile-code sandbox. Install only publishers you trust. Installation uses the existing Setup PTY. apt/dpkg cancellation waits for a safe boundary.")
            if(!preview.pack.official) Row { Checkbox(trusted,{trusted=it});Text("I trust this unverified publisher") }
            TextButton(onClick={grants=permissions}) { Text("Approve all listed permissions") }
            permissions.sortedBy { it.wire }.forEach { p -> Row { Checkbox(p in grants,{yes -> grants=if(yes) grants+p else grants-p});Text(p.wire,Modifier.padding(top=12.dp)) } }
            if(!kit) preview.pack.declarations().forEach { Text("${it.type}: ${it.id}\n${it.descriptor}",style=MaterialTheme.typography.bodySmall) }
            if(state.installing) { LinearProgressIndicator(progress={state.progress/100f},modifier=Modifier.fillMaxWidth());Text(state.message);Text(state.log.takeLast(6000),style=MaterialTheme.typography.bodySmall) }
        }},
        confirmButton={TextButton(enabled=!preparing && !state.busy && trusted && grants.containsAll(permissions) && (!kit || plan!=null),onClick={
            if(kit) { host.setup.install(plan!!,grants,!preview.pack.official);onDismiss();onInstalled() }
            else scope.launch { try { host.install(preview,grants,!preview.pack.official);onDismiss();onInstalled() } catch(e: Exception) { error=e.message } }
        }) { Text("Confirm plan and install") }},
        dismissButton={TextButton(onClick={if(state.installing) host.setup.cancel() else onDismiss()}) { Text(if(state.installing) "Cancel safely" else "Cancel") }}
    )
}
