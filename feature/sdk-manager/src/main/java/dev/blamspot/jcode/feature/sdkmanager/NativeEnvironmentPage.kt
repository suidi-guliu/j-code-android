package dev.blamspot.jcode.feature.sdkmanager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.blamspot.jcode.core.distro.*

/** Native package setup inside the existing Toolchains detail slot; no new visual system. */
@Composable
internal fun NativeEnvironmentPage(language: NativeEnvironment, environment: DistroEnvironmentState, modifier: Modifier) {
    val context = LocalContext.current
    val manager = remember(context) { DistroServiceLocator.distroService(context).nativeEnvironmentManager }
    val state by manager.state.collectAsState()
    var confirmation by remember(language) { mutableStateOf<NativeInstallPlan?>(null) }
    val ready = environment.distroInstalled == true && environment.jcodeUserReady == true
    val plan = state.plan?.takeIf { it.language == language && it.distroId == environment.runtime.selectedDistro.id }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(language.title, style = MaterialTheme.typography.titleLarge)
        Text("Native Ubuntu environment · signed distribution packages")
        Text("Environment: ${environment.runtime.selectedDistro.label}")
        Text(state.message)
        if (!ready) Text("Complete Ubuntu setup first. Existing installations and Java data will not be reset.")
        if (state.busy) CircularProgressIndicator()
        Button(onClick = { manager.query(language) }, enabled = ready && !state.busy) { Text("Query Ubuntu versions / retry") }
        Button(onClick={manager.verify(language)},enabled=ready && !state.busy) { Text("Verify selected environment") }
        if(language==NativeEnvironment.Python) {
            TextButton(onClick=manager::queryLatestPython,enabled=!state.busy) { Text("Query latest official Python stable version") }
            state.latestPython?.let { Text("Latest upstream: $it\nSource: ${PythonReleaseProvider.SOURCE}\nUbuntu's supported package version may be older; it is installed and updated through signed Ubuntu repositories.") }
        }
        plan?.let { reviewed ->
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Architecture: ${reviewed.architecture}")
                    Text("Source: the configured, signed Ubuntu package repositories. No extension marketplace is used.")
                    reviewed.packages.forEach { pkg -> Text("${pkg.name}: ${pkg.selected} · ${if (pkg.installed != null) "keep installed version" else "Ubuntu candidate"}") }
                    Text("1. Preserve an existing usable managed tool.\n2. Finish pending dpkg configuration, if any.\n3. Download reviewed packages with apt signature/hash verification.\n4. Keep installed requested versions; apt may configure required dependencies.\n5. Run a compile/interpreter smoke test, then atomically configure the native QuickRun environment.")
                    Text("Cancellation is cooperative: apt/dpkg is allowed to finish its current operation. Completed packages are retained; no lock files are removed. Other language environments, including Java, are not overwritten.")
                }
            }
            if(reviewed.packages.any { it.candidate!=it.selected }) TextButton(onClick={confirmation=reviewed.copy(upgrade=true)},enabled=ready && !state.busy) { Text("Review Ubuntu package updates") }
            Button(onClick = { confirmation = reviewed }, enabled = ready && !state.busy) { Text("Review and install / retry") }
        }
        if (state.busy) TextButton(onClick = manager::cancel) { Text("Cancel safely") }
        Text("Open a .${language.let { when (it) { NativeEnvironment.C -> "c"; NativeEnvironment.Cpp -> "cpp"; NativeEnvironment.Python -> "py" } }} file in the editor and press Ctrl+F5. Runtime execution never installs packages automatically.")
        Text("Install the basic language plugin from Plugin Center. IDE Kit Center adds intelligence, debugging and project tools. Environment installation is independent of both package formats.")
        state.logPath?.let { Text("Persistent Ubuntu log: $it") }
        if (state.log.isNotBlank()) SelectionContainer { Text(state.log, style = MaterialTheme.typography.bodySmall) }
    }
    confirmation?.let { reviewed -> AlertDialog(
        onDismissRequest = { confirmation = null },
        title = { Text("Install ${language.title}?") },
        text = { Text("Ubuntu packages: ${reviewed.packages.joinToString { "${it.name}=${if(reviewed.upgrade) it.candidate else it.selected}" }}\n\nThe existing Setup terminal will open. Review apt output there. Cancellation waits for a safe boundary; this action does not remove or upgrade your JDK.") },
        confirmButton = { TextButton(onClick = { confirmation = null; manager.install(reviewed) }) { Text("Confirm and open terminal") } },
        dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancel") } },
    ) }
}
