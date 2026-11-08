package dev.blamspot.jcode.core.distro

import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Shares the existing Setup PTY. Cancellation is cooperative across apt/dpkg safe boundaries. */
class NativeEnvironmentManager(private val distro: DistroService) {
    data class State(val busy: Boolean = false, val installing: Boolean = false, val plan: NativeInstallPlan? = null,
        val latestPython: String? = null, val message: String = "Query Ubuntu packages, review the versions, then confirm installation.", val log: String = "", val logPath: String? = null)
    private val mutable = MutableStateFlow(State())
    val state = mutable.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    @Volatile private var cancellationFile: File? = null

    @Synchronized fun queryLatestPython() {
        if(job?.isActive==true) return
        mutable.update { it.copy(busy=true,message="Querying the official CPython stable tag feed…") }
        job=scope.launch {
            try { val version=PythonReleaseProvider.latest();mutable.update { it.copy(latestPython=version,message="Official upstream Python $version. Ubuntu's signed, supported package version is shown separately; no interpreter is changed by this query.") } }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { mutable.update { it.copy(message=e.message ?: "Python release query failed") } }
            finally { mutable.update { it.copy(busy=false) } }
        }
    }
    @Synchronized fun verify(language: NativeEnvironment) {
        if(job?.isActive==true) return
        mutable.update { it.copy(busy=true,message="Verifying the selected native environment…") }
        job=scope.launch {
            try {
                val command="set -eu; TMP=\$(mktemp -d); trap 'rm -rf -- \"\$TMP\"' EXIT; "+
                    "TOOL=/usr/bin/${language.tool}; if [ -r /opt/sui-code/${language.key}.env ]; then . /opt/sui-code/${language.key}.env; TOOL=\"\${${language.variable}:-}\"; fi; [ -n \"\$TOOL\" ] && [ -x \"\$TOOL\" ]; \"\$TOOL\" --version; "+
                    language.smoke("/usr/bin/${language.tool}").replace(shellQuote("/usr/bin/${language.tool}"),"\"\$TOOL\"")
                val result=distro.exec(command,timeoutMs=120_000L)
                mutable.update { it.copy(message=if(result.succeeded) "${language.title} verification passed" else "Environment verification failed",log=(result.stdout+result.stderr+result.internalError.orEmpty()).takeLast(65536)) }
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { mutable.update { it.copy(message=e.message ?: "Verification failed") } }
            finally { mutable.update { it.copy(busy=false) } }
        }
    }

    @Synchronized fun query(language: NativeEnvironment) {
        if (job?.isActive == true) return
        mutable.value = State(busy = true, message = "Refreshing signed Ubuntu package metadata…")
        val runtime = distro.environmentState.value.runtime
        job = scope.launch {
            try {
                check(distro.isRuntimeReady()) { "Complete the existing Ubuntu setup first." }
                val result = distro.exec(language.queryScript(), timeoutMs = 240_000)
                coroutineContext.ensureActive()
                check(result.succeeded) { result.stderr.ifBlank { result.internalError ?: "Ubuntu package query failed." } }
                val plan = NativeInstallPlan.parse(language, runtime.selectedDistro.id, result.stdout)
                check(plan.architecture == runtime.selectedDistro.arch.rootfsKey) { "Ubuntu architecture does not match the selected environment." }
                mutable.update { it.copy(plan = plan, log = result.stderr.takeLast(65536), message = "Review the Ubuntu package versions before installation. Installed versions are preserved.") }
            } catch (cancel: CancellationException) { mutable.update { it.copy(message = "Query cancelled.") }; throw cancel
            } catch (error: Exception) { mutable.update { it.copy(message = error.message ?: "Package query failed; retry when connected.") }
            } finally { mutable.update { it.copy(busy = false) } }
        }
    }

    @Synchronized fun install(plan: NativeInstallPlan) {
        if (job?.isActive == true || (plan != mutable.value.plan && plan != mutable.value.plan?.copy(upgrade=true))) return
        job = scope.launch {
            var poller: Job? = null
            var logFile: File? = null
            try {
                check(distro.isRuntimeReady() && distro.environmentState.value.runtime.selectedDistro.id == plan.distroId) { "Ubuntu changed; query again." }
                val runner = distro.interactiveJavaRunner ?: error("Setup terminal is unavailable; reopen the workbench.")
                val token = UUID.randomUUID().toString()
                val cancelPath = "/tmp/sui-env-cancel-$token"
                cancellationFile = File(distro.activeRootfsPath(), cancelPath.removePrefix("/"))
                val logPath = "/opt/sui-code/logs/${plan.language.key}-$token.log"
                val file = File(distro.activeRootfsPath(), logPath.removePrefix("/")); logFile = file
                mutable.update { it.copy(busy = true, installing = true, log = "", logPath = logPath, message = "Installing ${plan.language.title}. See Setup terminal; cancellation waits for a safe package boundary.") }
                poller = scope.launch { while (true) {
                    if (file.isFile) mutable.update { it.copy(log = tail(file)) }
                    delay(750)
                } }
                val command = "mkdir -p /opt/sui-code/logs && /bin/bash -o pipefail -c " + shellQuote("/bin/bash -c ${shellQuote(plan.script(cancelPath))} 2>&1 | tee ${shellQuote(logPath)}")
                val result = runner("${plan.language.title} environment", command, 2_100_000, plan.distroId)
                    ?: error("Could not open Setup terminal. Nothing was installed silently.")
                val required=plan.language.packages.joinToString(" && ") { "dpkg-query -W -f='\${db:Status-Abbrev}' ${shellQuote(it)} 2>/dev/null | grep -q '^ii '" }
                val verify=distro.execOwned("set -eu; . /opt/sui-code/${plan.language.key}.env; TOOL=\"\${${plan.language.variable}}\"; test -x \"\$TOOL\"; \"\$TOOL\" --version; $required","/tmp","sui-kit:native-environment-verify",60000)
                val outcome=InstallationOutcome.assess(result.exitCode,verify.succeeded,result.exitCode==130)
                distro.invalidateCatalogProbes()
                scope.launch { runCatching { distro.checkSdkStatuses() } }
                mutable.update { it.copy(message="${plan.language.title}: ${outcome.message}. Java environment unchanged."+(outcome.warning?.let { "\n$it" } ?: ""),log=(it.log+"\n[post-verification] "+verify.stdout+verify.stderr).takeLast(65536)) }

            } catch (error: Exception) { mutable.update { it.copy(message = error.message ?: "Native environment setup failed.") }
            } finally { withContext(NonCancellable) {
                poller?.cancelAndJoin()
                logFile?.takeIf { it.isFile }?.let { f -> mutable.update { it.copy(log = tail(f)) } }
                cancellationFile?.delete(); cancellationFile = null
                mutable.update { it.copy(busy = false, installing = false) }
            } }
        }
    }

    fun cancel() {
        if (mutable.value.installing) {
            val written = runCatching { (cancellationFile ?: error("No active installation")).writeText("cancel\n") }
            if (written.isFailure) { mutable.update { it.copy(message = "Could not request cancellation. Keep Setup open and wait for the current package operation.") }; return }
            mutable.update { it.copy(message = "Cancellation requested. Waiting for apt/dpkg to finish safely; do not force-close the Setup terminal.") }
        } else job?.cancel()
    }

    private fun tail(file: File): String = runCatching {
        java.io.RandomAccessFile(file, "r").use { input ->
            input.seek((input.length() - 65536).coerceAtLeast(0))
            ByteArray((input.length() - input.filePointer).toInt()).also { input.readFully(it) }.toString(Charsets.UTF_8)
        }
    }.getOrDefault("See Setup terminal for logs.")
}
