package dev.suicode.nativehost

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.suicode.packages.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Packages owns consent. Selection is reviewed again before an installer can execute. */
@Composable fun NativePackageReview(host: NativePackageHost,preview: PackagePreview,onDismiss: ()->Unit,onInstalled: ()->Unit={}) {
    val scope=rememberCoroutineScope()
    val state by host.setup.state.collectAsState()
    val strings=dev.blamspot.jcode.design.LocalUiTranslations.current
    var plan by remember(preview.pack.digest) { mutableStateOf<KitSetupPlan?>(null) }
    var error by remember(preview.pack.digest) { mutableStateOf<String?>(null) }
    var preparing by remember(preview.pack.digest) { mutableStateOf(false) }
    var grants by remember(preview.pack.digest) { mutableStateOf(emptySet<Capability>()) }
    var trusted by remember(preview.pack.digest) { mutableStateOf(preview.pack.official) }
    var retry by remember { mutableIntStateOf(0) }
    var chosen by remember(preview.pack.digest) { mutableStateOf(if(!host.setup.runtimeAvailable()) emptySet() else KitRequirements.forPack(preview.pack).filter { it.provider!="environment" }.map { it.id }.toSet()) }
    var updateInstalled by remember(preview.pack.digest) { mutableStateOf(false) }
    var reviewed by remember(preview.pack.digest) { mutableStateOf<Pair<Set<String>,Boolean>?>(null) }
    val kit=preview.pack.kind==PackKind.KIT
    LaunchedEffect(preview.pack.digest,retry) {
        if(kit) {
            preparing=true;error=null
            try { plan=host.setup.prepare(preview,chosen,if(updateInstalled) chosen else emptySet());reviewed=chosen to updateInstalled }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { error=e.message }
            finally { preparing=false }
        }
    }
    val permissions=preview.pack.permissions+(plan?.dependencies?.flatMap { it.pack.permissions } ?: emptyList())
    dev.blamspot.jcode.workbench.WorkbenchReviewDialog(onDismissRequest=onDismiss,title={Text(strings.text("management.reviewInstallation"))},
        text={Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("${preview.pack.id} · ${preview.pack.version}\n${preview.source}\n${preview.bytes.size} bytes · SHA-256 ${preview.pack.digest}")
            Text(UiTextKeys.english.getValue(if(preview.pack.official) "management.verified" else "management.unverified"),color=if(preview.pack.official) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            if(preparing) { LinearProgressIndicator(Modifier.fillMaxWidth());Text(strings.text("packages.checking")) }
            error?.let { Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick={retry++}) { Text(strings.text("management.retryPlan")) } }
            plan?.let { p ->
                if(p.status.isNotEmpty()) Text("Ubuntu ${p.distroId} · ${p.architecture} · Core API ${CoreApi.VERSION}")
                p.aptDownloadBytes?.let { Text(strings.text("management.downloadBytes",mapOf("bytes" to it.toString()))) }
                p.dependencies.forEach { d -> Text("${strings.text("management.basicPlugin")}: ${d.pack.id} ${d.pack.version}\n${d.source}\n${d.bytes.size} bytes · SHA-256 ${d.pack.digest}\n${strings.text("management.permissions")}: ${d.pack.permissions.joinToString { it.wire }}") }
                if(p.status.isNotEmpty()) {
                    Text(strings.text("management.selectPlan"))
                    Row { Checkbox(updateInstalled,{updateInstalled=it});Text(strings.text("management.updateSelectedInstalled"),Modifier.padding(top=12.dp)) }
                    TextButton(enabled=!preparing,onClick={retry++}) { Text(strings.text("management.refreshPlan")) }
                }
                p.status.forEach { row ->
                    val r=row.requirement
                    HorizontalDivider()
                    if(r.provider=="environment") Text(strings.text("management.environmentOnly"))
                    else Row {
                        Checkbox(r.id in chosen,onCheckedChange={yes -> chosen=if(yes) chosen+r.id else chosen-r.id},modifier=Modifier.testTag("component-${r.id}"))
                        Text(r.name,Modifier.padding(top=12.dp))
                    }
                    Text("${r.name}: "+strings.text(if(r.id in p.force) "management.installRepair" else if(row.ready) "management.reuseInstalled" else if(r.id in p.selected && r.provider!="environment") "management.installRepair" else "management.notSelected"),style=MaterialTheme.typography.titleSmall)
                    Text("${row.installedVersion}\n${r.version}\n${r.source}")
                    Text(if(row.ready && r.id !in p.force) strings.text("management.noDownload") else r.bytes?.let { strings.text("management.downloadBytes",mapOf("bytes" to it.toString())) } ?: strings.text("management.sizeUnavailable"))
                    if((!row.ready || r.id in p.force) && r.id in p.selected && r.provider!="environment") Text("${strings.text("management.behavior")}: ${r.provider} · ${r.packages.joinToString()}${r.resource?.let { " · $it" }.orEmpty()}")
                }
                Text(strings.text("management.retention"))
            }
            LocalizationResources.forPack(preview.pack).forEach { b -> Text("${b.displayName} (${b.locale}) · ${b.entries.size}\n"+strings.text("ui.fallback")) }
            if(Capability.RUN in permissions || plan?.scripts?.isNotEmpty()==true) Text(strings.text("management.executionConsent"))
            if(!preview.pack.official) Row { Checkbox(trusted,{trusted=it},Modifier.testTag("trust-publisher"));Text(UiTextKeys.english.getValue("management.trustPublisher")) }
            TextButton(onClick={grants=permissions},modifier=Modifier.testTag("approve-permissions")) { Text(UiTextKeys.english.getValue("management.approvePermissions")) }
            permissions.sortedBy { it.wire }.forEach { p -> Row { Checkbox(p in grants,{yes -> grants=if(yes) grants+p else grants-p});Text(p.wire,Modifier.padding(top=12.dp)) } }
            if(!kit) preview.pack.declarations().forEach { Text("${it.type}: ${it.id}\n${it.descriptor}",style=MaterialTheme.typography.bodySmall) }
            if(state.installing) { LinearProgressIndicator(progress={state.progress/100f},modifier=Modifier.fillMaxWidth());Text(state.message);Text(state.log.takeLast(6000),style=MaterialTheme.typography.bodySmall) }
        }},
        confirmButton={TextButton(enabled=!preparing && !state.busy && trusted && grants.containsAll(permissions) && (!kit || plan!=null && reviewed==(chosen to updateInstalled)),modifier=Modifier.testTag("confirm-package-plan"),onClick={
            if(kit) { host.setup.install(plan!!,grants,!preview.pack.official);onDismiss();onInstalled() }
            else scope.launch { try { host.install(preview,grants,!preview.pack.official);onDismiss();onInstalled() } catch(e: CancellationException) { throw e } catch(e: Exception) { error=e.message } }
        }) { Text(UiTextKeys.english.getValue("management.confirmPlan")) }},
        dismissButton={TextButton(onClick={if(state.installing) host.setup.cancel() else onDismiss()}) { Text(strings.text(if(state.installing) "ui.cancelSafely" else "ui.cancel")) }})
}
