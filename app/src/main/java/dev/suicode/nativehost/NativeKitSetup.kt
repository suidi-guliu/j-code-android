package dev.suicode.nativehost

import android.content.Context
import dev.blamspot.jcode.core.distro.*
import dev.suicode.packages.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

data class KitDependencyStatus(val requirement: KitRequirement,val ready: Boolean,val installedVersion: String="Not detected")
data class KitSetupPlan(val preview: PackagePreview,val dependencies: List<PackagePreview>,val status: List<KitDependencyStatus>,val scripts: Map<String,String>,val distroId: String,val architecture: String,val created: Long=System.currentTimeMillis(),val cancellationPath: String="/tmp/sui-env-cancel-kit-"+UUID.randomUUID(),val aptDownloadBytes: Long?=null,val selected: Set<String> = emptySet(),val force: Set<String> = emptySet())
data class KitSetupState(val busy: Boolean=false,val installing: Boolean=false,val cancelling: Boolean=false,val progress: Int=0,val message: String="",val log: String="",val logPath: String?=null,val ready: Map<String,String> = emptyMap())

/** One application-owned coordinator. No package script runs until the complete plan is confirmed. */
class NativeKitSetup(private val context: Context,private val host: NativePackageHost,private val distro: DistroService) {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val mutable=MutableStateFlow(KitSetupState())
    val state=mutable.asStateFlow()
    @Volatile private var cancelFile: File?=null
    private var job: Job?=null
    private fun downloadSize(output: String): Long = output.lineSequence().mapNotNull { line -> Regex("^'[^']+' \\S+ ([0-9]+) ").find(line)?.groupValues?.get(1)?.toLongOrNull() }.sum()
    private val cache=java.util.concurrent.ConcurrentHashMap<String,Pair<Long,List<KitDependencyStatus>>>()
    private fun verify(r: KitRequirement): String = buildList {
        if(r.provider!="environment" || r.environmentId=="java") r.files.forEach { add("test -r ${shellQuote(it)}") }
        r.pythonModules.forEach { add("python3 -c ${shellQuote("import $it")}") }
        r.commands.forEach { add("command -v ${shellQuote(it)} >/dev/null 2>&1") }
        if(r.files.isEmpty() && r.commands.isEmpty() && r.anyCommands.isEmpty() && r.pythonModules.isEmpty()) r.packages.forEach { add("dpkg-query -W -f='\${db:Status-Abbrev}' ${shellQuote(it)} 2>/dev/null | grep -q '^ii '") }
        if(r.environmentId=="java") add("( . /opt/sui-code/java.env && test -x \"\$JAVA_HOME/bin/java\" && \"\$JAVA_HOME/bin/java\" -version && \"\$JAVA_HOME/bin/javac\" -version ) >/dev/null 2>&1")
        if(r.environmentId in setOf("c","cpp")) add("command -v gcc >/dev/null && command -v g++ >/dev/null")
        if(r.environmentId=="python") add("command -v python3 >/dev/null")
        // DAP executable names are versioned on Ubuntu. Package metadata alone isn't readiness.
        if(r.anyCommands.isNotEmpty()) add("("+r.anyCommands.joinToString(" || ") { "command -v ${shellQuote(it)} >/dev/null 2>&1" }+")")
    }.joinToString(" && ")

    suspend fun inspect(pack: VerifiedPack,force: Boolean=false): List<KitDependencyStatus> = withContext(Dispatchers.IO) {
        val key=distro.environmentState.value.runtime.selectedDistro.id+":"+pack.digest
        cache[key]?.takeIf { !force && android.os.SystemClock.elapsedRealtime()-it.first<60_000 }?.let { return@withContext it.second }
        val requirements=KitRequirements.forPack(pack)
        if(requirements.isEmpty()) return@withContext emptyList()
        if(!distro.isRuntimeReady()) return@withContext requirements.map { KitDependencyStatus(it,false,"Ubuntu setup required") }
        // Only Core-composed existence/dpkg checks; never a prerequisite's installation resource.
        val checks=requirements.map { CatalogProbeBatch.Probe(it.id,verify(it)) }
        val versions=requirements.joinToString("\n") { r ->
            val command=when {
                r.commands.isNotEmpty() -> shellQuote(r.commands.first())+" --version 2>&1 | head -1"
                r.pythonModules.isNotEmpty() -> "python3 -c "+shellQuote("import ${r.pythonModules.first()} as m; print(getattr(m, '__version__', 'installed'))")
                r.packages.isNotEmpty() -> "dpkg-query -W -f='\${Version}' "+shellQuote(r.packages.first())
                else -> "printf '%s' "+shellQuote("Verified files present; required/source version: "+r.version)
            }
            "printf 'SUI_VERSION\\t%s\\t' ${shellQuote(r.id)}; timeout 3 /bin/bash --noprofile --norc -c ${shellQuote(command)} 2>/dev/null | tr '\\n\\r\\t' '   '; printf '\\n'"
        }
        val result=distro.execOwned(CatalogProbeBatch.script(checks)+"\n"+versions,"/tmp","sui-kit:dependency-probe",45000)
        val parsed=CatalogProbeBatch.parse(result.stdout)
        val detected=result.stdout.lineSequence().map { it.split('\t',limit=3) }.filter { it.size==3 && it[0]=="SUI_VERSION" }.associate { it[1] to it[2] }
        val rows=requirements.map { r -> KitDependencyStatus(r,r.id in parsed.installed,if(r.id in parsed.installed) detected[r.id]?.takeIf { it.isNotBlank() } ?: "Installed checks passed" else if(r.id in parsed.checked) "Missing / incomplete" else "Probe timed out; retry") }
        if(parsed.checked.size==requirements.size) cache[key]=android.os.SystemClock.elapsedRealtime() to rows
        rows
    }
    fun runtimeAvailable(): Boolean = distro.isRuntimeReady()
    suspend fun prepare(preview: PackagePreview,selectedComponents: Set<String>?=null,forceComponents: Set<String> = emptySet()): KitSetupPlan = withContext(Dispatchers.IO) {
        check(!mutable.value.installing) { "An IDE Kit installation is already running" }
        mutable.update { it.copy(busy=true,message="Checking prerequisites…") }
        try {
            val deps=mutableListOf<PackagePreview>()
            val visiting=mutableSetOf<String>()
            suspend fun collect(pack: VerifiedPack) {
                check(visiting.add(pack.id) && visiting.size<=32) { "Dependency cycle / too many packages" }
                pack.manifest.getValue("dependencies").jsonArray.forEach { item ->
                    val id=item.jsonObject.getValue("id").jsonPrimitive.content
                    fun compatible(version: String): Boolean {
                        val low=item.jsonObject.getValue("min").jsonPrimitive.content
                        val high=item.jsonObject.getValue("max").jsonPrimitive.content
                        fun compare(a: String,b: String): Int = a.split('.').map(String::toInt).zip(b.split('.').map(String::toInt)).firstOrNull { it.first!=it.second }?.let { it.first.compareTo(it.second) } ?: 0
                        return compare(version,low)>=0 && compare(version,high)<0
                    }
                    if(host.rows.value.none { it.record.id==id && it.record.enabled && compatible(it.record.version) }) {
                        val entry=host.library.value.firstOrNull { it.id==id } ?: error("Missing local dependency $id. Import and review it first.")
                        check(compatible(entry.version)) { "No compatible official package for $id; import a compatible local dependency and review it first" }
                        val dep=host.download(entry)
                        collect(dep.pack)
                        if(deps.none { it.pack.id==id }) deps+=dep
                    }
                }
                visiting.remove(pack.id)
            }
            collect(preview.pack)
            var status=inspect(preview.pack,true)
            val selectedOrder=KitRequirements.selection(status.map { it.requirement },selectedComponents ?: status.filter { it.requirement.provider!="environment" }.map { it.requirement.id }.toSet()).map { it.id }
            check(distro.isRuntimeReady() || selectedOrder.none { id -> status.any { it.requirement.id==id && it.requirement.provider!="environment" } }) { "Ubuntu is unavailable. Review an empty component selection to install package metadata only, or complete Ubuntu setup before deploying IDE tools. Existing data is retained." }
            val selected=selectedOrder.toSet()
            check(status.none { it.requirement.id in forceComponents && it.requirement.provider=="kit-resource" && !it.requirement.explicitDependencies }) { "This legacy Kit does not declare selective repair support. Update its package before forcing an installer; installed tools are retained." }
            check(forceComponents.all { it in selected && status.any { row -> row.requirement.id==it && row.requirement.provider!="environment" } }) { "Only selected IDE components may be updated or repaired" }
            status=status.sortedBy { selectedOrder.indexOf(it.requirement.id).takeIf { index -> index>=0 } ?: Int.MAX_VALUE }
            val runtime=distro.environmentState.value.runtime
            val scripts=mutableMapOf<String,String>()
            val cancellationPath="/tmp/sui-env-cancel-kit-"+UUID.randomUUID()
            status.filter { (!it.ready || it.requirement.id in forceComponents) && it.requirement.id in selected && it.requirement.provider!="environment" }.forEach { row ->
                val r=row.requirement
                when(r.provider) {
                    "ubuntu-apt" -> scripts[r.id]="""
                        set -eu
                        sudo -n apt-get -o Acquire::AllowInsecureRepositories=false -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 update
                        sudo -n dpkg --configure -a
                        sudo -n apt-get -y --no-upgrade -o APT::Get::AllowUnauthenticated=false --download-only install ${r.packages.joinToString(" ",transform=::shellQuote)}
                        [ ! -f "${'$'}{SUI_KIT_CANCEL}" ] || exit 130
                        sudo -n apt-get -y --no-upgrade -o APT::Get::AllowUnauthenticated=false --no-download install ${r.packages.joinToString(" ",transform=::shellQuote)}
                    """.trimIndent()
                    "kit-resource" -> scripts[r.id]=preview.pack.content(r.resource!!).toString(Charsets.UTF_8)
                }
            }
            var aptDownloadBytes: Long?=null
            val aptRows=status.filter { (!it.ready || it.requirement.id in forceComponents) && it.requirement.id in selected && it.requirement.provider=="ubuntu-apt" }
            if(aptRows.isNotEmpty()) {
                val packages=aptRows.flatMap { it.requirement.packages }.distinct()
                val query="""
                    set -eu
                    sudo -n apt-get -o Acquire::AllowInsecureRepositories=false -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 update >&2
                    for p in ${packages.joinToString(" ",transform=::shellQuote)}; do
                        printf 'SUI_APT\t%s\t' "${'$'}p"
                        apt-cache policy "${'$'}p" | sed -n 's/^[[:space:]]*Candidate: //p' | head -1
                    done
                """.trimIndent()
                val metadata=distro.execOwned(query,"/tmp","sui-kit:dependency-metadata",300000)
                check(metadata.succeeded) { "Signed repository metadata unavailable: ${metadata.stderr.takeLast(1500)}. Retry after checking the network; installed tools retained." }
                val candidates=metadata.stdout.lineSequence().filter { it.startsWith("SUI_APT\t") }.map { it.split('\t') }.filter { it.size==3 }.associate { it[1] to it[2] }
                check(packages.all { candidates[it]?.matches(Regex("[A-Za-z0-9:.+~_-]+"))==true }) { "No trusted repository candidate for all prerequisites. Check Ubuntu sources and retry." }
                val packageArgs=packages.joinToString(" ") { shellQuote(it+"="+candidates.getValue(it)) }
                val updatePolicy=if(aptRows.any { it.requirement.id in forceComponents }) "" else "--no-upgrade"
                val downloadProbe=distro.execOwned("sudo -n apt-get --print-uris --download-only --yes $updatePolicy -o APT::Get::AllowUnauthenticated=false install $packageArgs","/tmp","sui-kit:dependency-size",60000)
                if(downloadProbe.succeeded) aptDownloadBytes=downloadSize(downloadProbe.stdout)
                val script="""
                    set -eu
                    sudo -n dpkg --configure -a
                    sudo -n apt-get -y $updatePolicy -o APT::Get::AllowUnauthenticated=false -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 --download-only install $packageArgs
                    [ ! -f ${shellQuote(cancellationPath)} ] || exit 130
                    sudo -n apt-get -y $updatePolicy -o APT::Get::AllowUnauthenticated=false --no-download install $packageArgs
                """.trimIndent()
                aptRows.forEach { scripts[it.requirement.id]=script }
                status=status.map { row -> if(row.requirement.provider=="ubuntu-apt" && (!row.ready || row.requirement.id in forceComponents) && row.requirement.id in selected) row.copy(requirement=row.requirement.copy(version=row.requirement.packages.joinToString { "$it=${candidates[it]}" })) else row }
            }
            KitSetupPlan(preview,deps,status,scripts,runtime.selectedDistro.id,runtime.selectedDistro.arch.rootfsKey,cancellationPath=cancellationPath,aptDownloadBytes=aptDownloadBytes,selected=selected,force=forceComponents)
        } finally { mutable.update { it.copy(busy=false) } }
    }

    @Synchronized fun install(plan: KitSetupPlan,grants: Set<Capability>,allowUnverified: Boolean) {
        if(job?.isActive==true) return
        check((plan.preview.pack.official || allowUnverified) && (plan.scripts.isEmpty() || grants.containsAll(setOf(Capability.RUN,Capability.FILE_READ,Capability.FILE_WRITE)))) { "IDE dependency deployment needs reviewed file.read, file.write and process.run permissions" }
        job=scope.launch {
            // Metadata-only Kits (including localization) never require Ubuntu, a PTY or a runtime.
            if(plan.scripts.isEmpty()) {
                mutable.update { it.copy(busy=true,installing=true,message="Installing reviewed package resources",logPath=null,log="") }
                try {
                    check(System.currentTimeMillis()-plan.created<3600000) { "Plan expired; review again" }
                    plan.dependencies.forEach { dep -> host.install(dep,grants.intersect(dep.pack.permissions),allowUnverified || dep.pack.official) }
                    host.install(plan.preview,grants.intersect(plan.preview.pack.permissions),allowUnverified)
                    mutable.update { it.copy(progress=100,message=if(plan.status.any { row -> !row.ready }) "Partially Ready: "+plan.status.filter { row -> !row.ready }.joinToString { row -> row.requirement.name } else "Ready · package resources enabled; no language environment required",ready=it.ready+(plan.preview.pack.id to if(plan.status.any { row -> !row.ready }) "Partially Ready" else "Ready")) }
                } catch(e: CancellationException) { throw e }
                catch(e: Exception) { mutable.update { it.copy(message="Package resources could not be enabled: ${e.message}. Review and retry; installed environments are unchanged.") } }
                finally { mutable.update { it.copy(busy=false,installing=false,cancelling=false) } }
                return@launch
            }
            var poller: Job?=null
            val token=UUID.randomUUID().toString()
            val cancelPath=plan.cancellationPath
            val logPath="/opt/sui-code/logs/kit-$token.log"
            val logFile=File(distro.activeRootfsPath(),logPath.removePrefix("/"))
            cancelFile=File(distro.activeRootfsPath(),cancelPath.removePrefix("/"))
            distro.kitInstallationActive=true
            mutable.update { it.copy(busy=true,installing=true,cancelling=false,progress=0,message="Installing reviewed IDE Kit plan",log="",logPath=logPath) }
            try {
                check(distro.environmentState.value.runtime.selectedDistro.id==plan.distroId && System.currentTimeMillis()-plan.created<3600000) { "Plan expired or Ubuntu changed. Review a fresh plan." }
                val runner=distro.interactiveJavaRunner ?: error("Setup terminal is unavailable; reopen workbench")
                poller=scope.launch { while(isActive) { if(logFile.isFile) mutable.update { it.copy(log=java.io.RandomAccessFile(logFile,"r").use { input -> input.seek((input.length()-65536).coerceAtLeast(0));ByteArray((input.length()-input.filePointer).toInt()).also { input.readFully(it) }.toString(Charsets.UTF_8) }) };delay(750) } }
                val completed=mutableSetOf<String>()
                val warnings=mutableListOf<String>()
                val selectedRows=plan.status.filter { it.requirement.id in plan.selected && it.requirement.provider!="environment" }
                val blocked=plan.status.filter { it.requirement.id in plan.selected && it.requirement.provider=="environment" && !it.ready }
                check(blocked.isEmpty()) { "Required runtime unavailable: ${blocked.joinToString { it.requirement.name }}. Verify or repair it in Languages; IDE Kit deployment never downloads a runtime." }
                for((index,row) in selectedRows.withIndex()) {
                    currentCoroutineContext().ensureActive()
                    if(cancelFile!!.exists()) break
                    val r=row.requirement
                    mutable.update { it.copy(progress=index*100/selectedRows.size.coerceAtLeast(1),message="${index+1}/${selectedRows.size}: ${r.name}") }
                    if(inspect(plan.preview.pack,true).firstOrNull { it.requirement.id==r.id }?.ready==true && r.id !in plan.force) continue
                    val script=plan.scripts[r.id] ?: error("Plan has no installer for ${r.id}")
                    val actionKey=if(r.provider=="ubuntu-apt") "ubuntu-apt" else r.resource ?: r.id
                    if(!completed.add(actionKey)) continue
                    val group=selectedRows.filter { (!it.ready || it.requirement.id in plan.force) && it.requirement.resource==r.resource }.joinToString(" ") { it.requirement.id }
                    val body="export SUI_KIT_REPAIR=${if(selectedRows.any { it.requirement.id in plan.force && it.requirement.resource==r.resource }) "1" else "0"}\nexport SUI_KIT_COMPONENTS=${shellQuote(group)}\nexport SUI_KIT_CANCEL=${shellQuote(cancelPath)}\n"+script.replace("'\${SUI_KIT_CANCEL}'",shellQuote(cancelPath)).replace("\${SUI_KIT_CANCEL}",cancelPath)
                    val command="mkdir -p /opt/sui-code/logs; /bin/bash -o pipefail -c "+shellQuote("/bin/bash -c ${shellQuote(body)} 2>&1 | tee -a ${shellQuote(logPath)}")
                    val result=runner("IDE Kit · ${r.name}",command,2100000,plan.distroId) ?: error("Cannot start Setup terminal")
                    if(!result.succeeded) {
                        warnings+="${r.name}: exit ${result.exitCode}"
                        logFile.appendText("\n[warning] ${warnings.last()}; final readiness determined by independent verification.\n")
                        mutable.update { it.copy(message="${r.name}: command exited ${result.exitCode}. Verifying actual state; see log.") }
                    }
                }
                val verified=inspect(plan.preview.pack,true)
                val missing=verified.filter { !it.ready }
                if(cancelFile!!.exists()) {
                    mutable.update { it.copy(message="Cancelled at a safe boundary. Completed IDE tools retained; package activation unchanged.",ready=it.ready+(plan.preview.pack.id to "Partially Ready")) }
                    return@launch
                }
                // Core package transactions are atomic; package rollback retains all installed tools/data.
                plan.dependencies.forEach { check(grants.containsAll(it.pack.permissions)) { "Dependency permissions were not approved" };host.install(it,it.pack.permissions,false) }
                host.install(plan.preview,grants,allowUnverified)
                val readiness=if(missing.isEmpty()) "Ready"+(if(warnings.isEmpty()) "" else " · verified after installer warnings: ${warnings.joinToString()}") else "Partially Ready: "+missing.joinToString { it.requirement.name }
                mutable.update { it.copy(progress=100,message=readiness+if(cancelFile!!.exists()) " · cancelled at safe boundary; retry to finish" else "",ready=it.ready+(plan.preview.pack.id to readiness)) }
                distro.invalidateCatalogProbes()
                NativeLanguageServices.refreshAndCheck(host,distro) { error -> mutable.update { it.copy(message=error) } }
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { mutable.update { it.copy(message="Installation paused: ${e.message}. Existing tools and package archives retained; review and retry.") } }
            finally { withContext(NonCancellable) {
                poller?.cancelAndJoin()
                plan.status.filter { it.requirement.id in plan.selected && it.requirement.provider!="environment" }.forEach { row -> runCatching { distro.installationLogs.record("ide-component",plan.preview.pack.id+"/"+row.requirement.id,row.requirement.source,plan.distroId,mutable.value.message,logFile) }.onFailure { android.util.Log.e("SuiInstallLog","Cannot archive IDE component log",it) } }
                cancelFile?.delete();cancelFile=null
                distro.kitInstallationActive=false
                mutable.update { it.copy(busy=false,installing=false,cancelling=false) }
            } }
        }
    }
    fun cancel() {
        if(mutable.value.installing) {
            if(runCatching { cancelFile?.writeText("cancel\n") }.isFailure) { mutable.update { it.copy(message="Could not write cancellation request. Keep Setup open until the current transaction completes.") };return }
            mutable.update { it.copy(cancelling=true,message="Cancellation requested. Current apt/dpkg transaction will finish safely; no locks or installed tools will be deleted.") }
        }
    }
}
