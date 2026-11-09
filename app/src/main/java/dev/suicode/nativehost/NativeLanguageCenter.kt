package dev.suicode.nativehost

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.blamspot.jcode.core.distro.*
import dev.blamspot.jcode.feature.sdkmanager.JavaEnvironmentPage
import dev.blamspot.jcode.feature.sdkmanager.NativeEnvironmentPage
import kotlinx.coroutines.*

/** Runtime ownership only: no package host, Kit probes, IDE installers or Kit logs. */
@Composable fun NativeLanguageCenter(modifier: Modifier=Modifier,advanced: @Composable ()->Unit) {
    val strings=dev.blamspot.jcode.design.LocalUiTranslations.current
    val context=LocalContext.current
    val distro=remember(context) { DistroServiceLocator.distroService(context) }
    val environment by distro.environmentState.collectAsState()
    var search by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    var detected by remember { mutableStateOf<Map<String,String>>(emptyMap()) }
    var checking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val runtimes=remember { listOf("java" to "Java · JDK · Temurin · 爪哇", "c" to "C · GCC · C语言", "cpp" to "C++ · G++ · CPP", "python" to "Python · Python 3 · 蟒蛇") }
    LaunchedEffect(environment.runtime.selectedDistro.id,environment.distroInstalled,revision) {
        if(!distro.isRuntimeReady()) return@LaunchedEffect
        checking=true;error=null
        try {
            val script="""
                set +e
                for tool in java gcc g++ python3; do
                    case "${'$'}tool" in java) id=java; envfile=java;; gcc) id=c; envfile=c;; g++) id=cpp; envfile=cpp;; python3) id=python; envfile=python;; esac
                    (
                        config=missing
                        file=/opt/sui-code/"${'$'}envfile".env
                        if [ -d "${'$'}file" ]; then config=directory
                        elif [ -L "${'$'}file" ] && [ ! -r "${'$'}file" ]; then config=broken-link
                        elif [ -r "${'$'}file" ]; then config=readable; . "${'$'}file"
                        elif [ -e "${'$'}file" ]; then config=unreadable; fi
                        command -v "${'$'}tool" >/dev/null 2>&1 || { printf 'SUI_RUNTIME\t%s\tmissing · config=%s\n' "${'$'}id" "${'$'}config"; exit 0; }
                        version=$(timeout 5 "${'$'}tool" --version 2>&1 | head -1)
                        printf 'SUI_RUNTIME\t%s\t%s · config=%s\n' "${'$'}id" "${'$'}version" "${'$'}config"
                    )
                done
            """.trimIndent()
            val result=withContext(Dispatchers.IO) { distro.execOwned(script,"/tmp","sui-runtime:discovery",30000) }
            detected=result.stdout.lineSequence().map { it.split('\t',limit=3) }.filter { it.size==3 && it[0]=="SUI_RUNTIME" }.associate { it[1] to it[2] }
            if(!result.succeeded) error=result.stderr.takeLast(1500)
        } catch(e: CancellationException) { throw e }
        catch(e: Exception) { error=e.message }
        finally { checking=false }
    }
    Column(modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(strings.text("languages.title"),style=MaterialTheme.typography.titleLarge)
        Text(strings.text("management.runtimeOnly"),style=MaterialTheme.typography.bodySmall)
        OutlinedTextField(search,{search=it},Modifier.fillMaxWidth().testTag("languages-search"),singleLine=true,label={Text(strings.text("management.searchLanguages"))},trailingIcon={if(search.isNotEmpty()) TextButton(onClick={search=""}) { Text(strings.text("management.clear")) }})
        dev.blamspot.jcode.feature.sdkmanager.InstallationLogHistory(distro,"environment")
        if(checking) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        if(selected!=null) {
            TextButton(onClick={selected=null;revision++}) { Text(strings.text("management.backToEnvironments")) }
            Box(Modifier.weight(1f)) {
                when(selected) {
                    "java" -> JavaEnvironmentPage(environment)
                    else -> NativeEnvironmentPage(NativeEnvironment.entries.first { it.key==selected },environment,Modifier)
                }
            }
        } else {
            val visible=runtimes.filter { (id,name) -> "$name ${detected[id].orEmpty()} ${if(detected[id]?.contains("missing")==false) "installed 已安装 已检测" else "not installed 未安装"}".contains(search.trim(),ignoreCase=true) }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(visible.isEmpty()) Text(strings.text("management.noResults"),Modifier.testTag("languages-empty"))
                visible.forEach { (id,name) ->
                    OutlinedCard(Modifier.fillMaxWidth().testTag("runtime-$id")) {
                        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(name.substringBefore(" · "),style=MaterialTheme.typography.titleMedium)
                            Text(detected[id] ?: strings.text(if(distro.isRuntimeReady()) "packages.checking" else "ui.completeUbuntuSetupBeforeInstallingJava"))
                            Button(onClick={selected=id}) { Text(strings.text("management.manageEnvironment")) }
                        }
                    }
                }
            }
            TextButton(onClick={revision++},enabled=!checking) { Text(strings.text("files.refresh")) }
        }
    }
}
