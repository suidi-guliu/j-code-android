package dev.blamspot.jcode.feature.sdkmanager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.blamspot.jcode.core.distro.Arch
import dev.blamspot.jcode.core.distro.DistroEnvironmentState
import dev.blamspot.jcode.core.distro.DistroServiceLocator
import dev.blamspot.jcode.core.distro.JavaRelease

/** Uses the current Toolchains detail slot and theme; no tablet layout redesign. */
@Composable
internal fun JavaEnvironmentPage(environment: DistroEnvironmentState, modifier: Modifier = Modifier) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    val context = LocalContext.current
    val manager = remember(context) { DistroServiceLocator.distroService(context).javaEnvironmentManager }
    val state by manager.state.collectAsState()
    var confirmation by remember { mutableStateOf<JavaRelease?>(null) }
    var management by remember { mutableStateOf<Pair<String,Boolean>?>(null) }
    LaunchedEffect(environment.runtime.selectedDistro.id) { manager.refreshInstalled() }
    val supported = environment.runtime.selectedDistro.arch == Arch.ARM64
    val ready = environment.distroInstalled == true && environment.jcodeUserReady == true && supported
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(strings.text("ui.javaLatestGA"), style = MaterialTheme.typography.titleLarge)
        Text(strings.text("ui.nativeLanguageEnvironmentEclipseTemurinOpenJDK"))
        Text("Environment: ${environment.runtime.selectedDistro.label} · Linux ${environment.runtime.selectedDistro.arch.rootfsKey}")
        if (!ready) Text(if (!supported) strings.text("ui.unavailableThisMilestoneRequiresALinuxARM64Environment") else strings.text("ui.completeUbuntuSetupBeforeInstallingJava"))
        Text(state.message)
        if (state.busy) CircularProgressIndicator()
        Button(onClick = manager::query, enabled = ready && !state.busy) { Text(strings.text("ui.queryLatestGARetryQuery")) }
        state.release?.let { release ->
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Version: ${release.version}")
                    Text("Source: ${release.source}")
                    Text("File: ${release.fileName} (${release.size / 1024 / 1024} MiB)")
                    Text("Download: ${release.downloadUrl}")
                    Text("SHA-256: ${release.sha256}")
                    Text("Install location: ${release.home}")
                    Text("1. Download the reviewed archive over HTTPS.\n2. Check SHA-256 before extraction.\n3. Stage alongside existing JDKs.\n4. Verify java, javac, jshell; compile and run HelloWorld.\n5. Configure the managed JAVA_HOME and PATH. Preserve an existing usable system default.")
                }
            }
            Button(onClick = { confirmation = release }, enabled = ready && !state.busy && state.distroId == environment.runtime.selectedDistro.id) {
                Text(strings.text("ui.reviewAndInstallRetry"))
            }
        }
        if (state.busy) TextButton(onClick = manager::cancel) { Text(strings.text("ui.cancel")) }
        Text(strings.text("ui.installedJDKs"),style=MaterialTheme.typography.titleMedium)
        TextButton(onClick=manager::refreshInstalled,enabled=ready && !state.busy) { Text(strings.text("ui.refreshInstalledJDKs")) }
        state.installations.forEach { item ->
            Text("${if(item.active) "Active · " else ""}${item.version}\n${item.home}")
            if(!item.active) {
                TextButton(onClick={ management=item.home to false },enabled=!state.busy) { Text(strings.text("ui.verifyAndSelect")) }
                TextButton(onClick={ management=item.home to true },enabled=!state.busy) { Text(strings.text("ui.removeInactiveJDK")) }
            }
        }
        Text(strings.text("ui.quickRunOpenAJavaFileInTheEditorAnd"))
        Text(strings.text("ui.installJavaBasicPluginFromTheSuiCodePlugin"))
        state.logPath?.let { Text("Install log in Ubuntu: $it") }
        if (state.log.isNotBlank()) SelectionContainer { Text(state.log, style = MaterialTheme.typography.bodySmall) }
    }
    management?.let { (home,remove) ->
        AlertDialog(onDismissRequest={ management=null },title={ Text(if(remove) strings.text("ui.removeInactiveJDKText012cf8") else strings.text("ui.changeManagedJDK")) },
            text={ Text("$home\n${if(remove) "Only this inactive managed installation will be deleted. Active or in-use JDKs are protected." else "java, javac and jshell will be verified before updating java.env. Existing terminals and language services need a restart."}") },
            confirmButton={ TextButton(onClick={ management=null;manager.manage(home,remove) }) { Text(strings.text("ui.confirm")) } },dismissButton={ TextButton(onClick={ management=null }) { Text(strings.text("ui.cancel")) } })
    }
    confirmation?.let { release ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text("Install Java ${release.version}?") },
            text = { Text("${release.source}\n${release.fileName}\n\nThe Ubuntu Setup terminal will open. Download, checksum verification and HelloWorld must succeed before activation. Existing JDKs and an existing usable system default will be kept.\n\nNo language plugin will be downloaded.") },
            confirmButton = { TextButton(onClick = { confirmation = null; manager.install(release) }) { Text(strings.text("ui.confirmAndOpenTerminal")) } },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text(strings.text("ui.cancel")) } },
        )
    }
}
