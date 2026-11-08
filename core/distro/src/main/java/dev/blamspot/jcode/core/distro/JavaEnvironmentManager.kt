package dev.blamspot.jcode.core.distro

import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/** Native workflow state survives closing its editor tab; no extension host or second PTY system. */
data class JavaEnvironmentState(
    val busy: Boolean = false,
    val installing: Boolean = false,
    val release: JavaRelease? = null,
    val distroId: String? = null,
    val message: String = "Query the latest GA JDK to review its source, file and installation steps.",
    val log: String = "",
    val logPath: String? = null,
    val installations: List<JavaInstallation> = emptyList(),
)

class JavaEnvironmentManager(private val distro: DistroService) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(JavaEnvironmentState())
    val state = mutableState.asStateFlow()
    private var job: Job? = null

    /** Listing never executes unknown binaries and never follows an installation symlink. */
    @Synchronized fun refreshInstalled() {
        if(job?.isActive==true) return
        job=scope.launch { runCatching { inventory() }.onFailure { e -> mutableState.update { it.copy(message=e.message ?: "Cannot read installed JDKs") } } }
    }
    private fun inventory() {
        val root=distro.activeRootfsPath()
        val env=File(root,"opt/sui-code/java.env").takeIf { it.isFile }?.readText().orEmpty()
        val installations=JavaInstallationScript.managedDirectory(root).listFiles().orEmpty().filter {
            it.isDirectory && !java.nio.file.Files.isSymbolicLink(it.toPath()) && it.name.matches(Regex("temurin-[A-Za-z0-9.+_-]+-(aarch64|x64)"))
        }.map { file ->
            val home="/opt/sui-code/jdks/${file.name}"
            JavaInstallation(home,Regex("(?m)^export JAVA_HOME=['\"]?"+Regex.escape(home)+"['\"]?$").containsMatchIn(env),
                File(file,"release").takeIf { it.isFile }?.useLines { lines -> lines.firstOrNull { it.startsWith("JAVA_VERSION=") }?.substringAfter('=')?.trim('\"') }.orEmpty())
        }.sortedByDescending { it.active }
        mutableState.update { it.copy(installations=installations) }
    }
    @Synchronized fun manage(home: String,remove: Boolean) {
        if(job?.isActive==true) return
        JavaInstallationScript.checkedHome(home)
        val runtime=distro.environmentState.value.runtime
        job=scope.launch {
            mutableState.update { it.copy(busy=true) }
            try {
                check(distro.isRuntimeReady() && distro.environmentState.value.runtime.selectedDistro.id==runtime.selectedDistro.id) { "Ubuntu changed; refresh the installed JDK list" }
                inventory()
                val item=mutableState.value.installations.single { it.home==home }
                val root=distro.activeRootfsPath()
                val actual=File(root,home.removePrefix("/")).canonicalFile
                check(actual.parentFile==JavaInstallationScript.managedDirectory(root).canonicalFile) { "Invalid managed JDK path" }
                val inUse=File("/proc").listFiles().orEmpty().any { proc ->
                    proc.name.toIntOrNull()!=null && runCatching {
                        val executable=File(proc,"exe").canonicalPath
                        executable.startsWith(actual.path+"/") || File(proc,"cmdline").readText().split('\u0000').any { it.startsWith(home+"/bin/") || it.startsWith(actual.path+"/bin/") }
                    }.getOrDefault(false)
                }
                check(!inUse) { "This JDK is in use. Stop its tasks and language services before changing it." }
                if(remove) {
                    check(!item.active) { "The active JDK cannot be removed. Select another verified JDK first." }
                    check(!java.nio.file.Files.isSymbolicLink(File(root,home.removePrefix("/")).toPath())) { "Refusing a symlink installation" }
                    // NOFOLLOW walk: a user-created link inside the JDK cannot delete its target.
                    java.nio.file.Files.walkFileTree(actual.toPath(),object: java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
                        override fun visitFile(path: java.nio.file.Path,attrs: java.nio.file.attribute.BasicFileAttributes): java.nio.file.FileVisitResult { java.nio.file.Files.delete(path);return java.nio.file.FileVisitResult.CONTINUE }
                        override fun postVisitDirectory(path: java.nio.file.Path,error: java.io.IOException?): java.nio.file.FileVisitResult { if(error!=null) throw error;java.nio.file.Files.delete(path);return java.nio.file.FileVisitResult.CONTINUE }
                    })
                    mutableState.update { it.copy(message="Removed inactive JDK; Ubuntu and all other installations retained.") }
                } else {
                    val result=distro.interactiveJavaRunner?.invoke("Select Java",JavaInstallationScript.select(home),120_000L,runtime.selectedDistro.id)
                    check(result?.succeeded==true) { "JDK verification or activation failed; inspect the Setup terminal" }
                    mutableState.update { it.copy(message="Selected verified JDK. Restart existing terminal and Java language services to load the new selection.") }
                }
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { mutableState.update { it.copy(message=e.message ?: "JDK management failed") } }
            finally { runCatching { inventory() };mutableState.update { it.copy(busy=false) } }
        }
    }

    @Synchronized
    fun query() {
        if (job?.isActive == true) return
        mutableState.update { it.copy(busy = true, message = "Querying Eclipse Adoptium GA releases…") }
        val runtime = distro.environmentState.value.runtime
        job = scope.launch {
            try {
                check(runtime.selectedDistro.arch == Arch.ARM64) { "This first device milestone supports Linux ARM64 only." }
                val release = JavaReleaseProvider.latest(runtime.selectedDistro.arch.rootfsKey)
                coroutineContext.ensureActive()
                mutableState.update { it.copy(release = release, distroId = runtime.selectedDistro.id, message = "Review the plan, then confirm installation in the Ubuntu Setup terminal.") }
            } catch (cancel: CancellationException) {
                mutableState.update { it.copy(message = "Query cancelled. You can retry.") }
                throw cancel
            } catch (error: Exception) {
                mutableState.update { it.copy(message = error.message ?: "JDK query failed. Retry when connected.") }
            } finally { mutableState.update { it.copy(busy = false) } }
        }
    }

    /** The exact release displayed by the confirmation dialog is passed back, never re-resolved. */
    @Synchronized
    fun install(confirmed: JavaRelease) {
        if (job?.isActive == true) return
        val plan = mutableState.value
        if (confirmed != plan.release) return
        job = scope.launch {
            var poller: Job? = null
            var installLog: File? = null
            try {
                check(distro.isRuntimeReady()) { "Complete Ubuntu environment setup first." }
                check(distro.environmentState.value.runtime.selectedDistro.id == plan.distroId) { "Environment changed. Query the JDK again." }
                check(distro.environmentState.value.runtime.selectedDistro.arch == Arch.ARM64) { "Linux ARM64 is required." }
                val run = distro.interactiveJavaRunner ?: error("The Setup terminal is unavailable. Reopen the workbench and retry.")
                val logPath = "/opt/sui-code/logs/java-${System.currentTimeMillis()}.log"
                val root = distro.activeRootfsPath()
                val logFile = File(root, logPath.removePrefix("/"))
                installLog = logFile
                mutableState.update { it.copy(busy = true, installing = true, message = "Installing ${confirmed.version} — see Setup terminal. Cancel stops this install.", log = "", logPath = logPath) }
                poller = scope.launch {
                    while (true) {
                        if (logFile.isFile) mutableState.update { it.copy(log = readLog(logFile)) }
                        delay(750)
                    }
                }
                // Bash pipefail preserves the install's status through tee. Nothing is installed quietly.
                val body = "mkdir -p /opt/sui-code/logs && /bin/bash -o pipefail -c " + shellQuote(
                    "/bin/bash -c ${shellQuote(JavaInstallScript.build(confirmed))} 2>&1 | tee ${shellQuote(logPath)}",
                )
                val result = run("Java GA ${confirmed.version}", body, 2_100_000L, plan.distroId ?: error("Query the JDK again."))
                    ?: error("Could not open the Ubuntu Setup terminal. No silent fallback was used.")
                mutableState.update { it.copy(message = if (result.succeeded) "Installed and verified java, javac, jshell and HelloWorld. Existing JDKs preserved." else "Install failed (${result.exitCode ?: "terminal unavailable"}). ${result.internalError.orEmpty()} Inspect the log, then retry.") }
            } catch (cancel: CancellationException) {
                mutableState.update { it.copy(message = "Install cancelled. Existing JDKs are preserved. Inspect the log before retrying.") }
                throw cancel
            } catch (error: Exception) {
                mutableState.update { it.copy(message = error.message ?: "Installation failed; inspect the log and retry.") }
            } finally {
                withContext(NonCancellable) {
                    poller?.cancelAndJoin()
                    installLog?.takeIf { it.isFile }?.let { file ->
                        mutableState.update { it.copy(log = readLog(file)) }
                    }
                    runCatching { inventory() }
                    mutableState.update { it.copy(busy = false, installing = false) }
                }
            }
        }
    }

    @Synchronized
    fun cancel() {
        if (job?.isActive == true) {
            mutableState.update { it.copy(message = "Cancelling; waiting for the task to stop…") }
            job?.cancel()
        }
    }

    private fun readLog(file: File): String = runCatching {
        java.io.RandomAccessFile(file, "r").use { input ->
            input.seek((input.length() - 64 * 1024).coerceAtLeast(0))
            ByteArray((input.length() - input.filePointer).toInt()).also { input.readFully(it) }.toString(Charsets.UTF_8)
        }
    }.getOrDefault("Unable to read the current install log; see Setup terminal.")
}
