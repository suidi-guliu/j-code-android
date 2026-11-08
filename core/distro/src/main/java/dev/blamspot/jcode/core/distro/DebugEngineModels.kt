package dev.blamspot.jcode.core.distro

/**
 * A debug engine (Debug Adapter Protocol adapter) in the debug catalog. Single source of truth for
 * both the Debug Engine Manager (install/verify/uninstall) and the runtime DAP launcher
 * (`adapterCommand`/`debugType`/`languageIds`). It lives in `:core:distro` for the same reason as
 * [LspCatalogEntry]: the install machinery ([DistroService]) must not depend on `:core:debug`
 * (that would close a module cycle); `:core:debug` derives its runtime descriptors from this list.
 */
data class DebugEngineEntry(
    val id: String,
    val category: String,
    val name: String,
    val description: String,
    val installCommand: String,
    val verifyCommand: String,
    val uninstallCommand: String,
    /** Shell command that launches the debug adapter speaking DAP. `{{port}}` is substituted for tcp adapters. */
    val adapterCommand: String,
    /** "stdio" (adapter talks DAP over its stdin/stdout) or "tcp" (adapter listens on a port). */
    val transport: String = "stdio",
    /** The DAP `type` these launch/attach configs use (e.g. "python", "lldb", "coreclr", "pwa-node"). */
    val debugType: String = "",
    /** Optional: exits 0 when a newer version is available. Empty = update detection skipped. */
    val updateCheckCommand: String = "",
    val languageIds: List<String> = emptyList(),
    val extensions: List<String> = emptyList(),
    /** SDK catalog ids this adapter needs (e.g. netcoredbg needs the dotnet SDK). Installed first. */
    val requiredSdks: List<String> = emptyList(),
    /** False for entries that don't ship a real DAP adapter (the JVM/JDWP placeholder): the runtime
     *  must not try to launch them, or the `initialize` request just hangs until it times out. */
    val dapAdapter: Boolean = true,
    val ownerPackage: String = "",
)

enum class DebugEngineAction(val label: String) {
    Install("Install"),
    Uninstall("Remove"),
}

data class DebugEngineCatalogState(
    val entries: List<DebugEngineEntry> = emptyList(),
    val installedEntryIds: Set<String> = emptySet(),
    val updatableEntryIds: Set<String> = emptySet(),
    val checking: Boolean = false,
    val runningEntryId: String? = null,
    val runningAction: DebugEngineAction? = null,
    val executionLabel: String? = null,
    val logLines: List<String> = emptyList(),
    val selectedDistroId: String? = null,
    val errorMessage: String? = null,
)

/**
 * The debug adapters the Debug Engine manager offers: the ones JCode ships, plus the ones installed
 * extensions bring.
 *
 * An adapter for one language belongs to that language's Dev Pack rather than to the IDE — JCode is
 * generic, and a Python-only user has no use for a JVM debugger. [BUILT_IN] is what is left after
 * that: adapters that serve languages with no pack of their own, or that several packs share.
 */
object DebugEngineCatalog {
    @Volatile private var nativeEntries: List<DebugEngineEntry> = emptyList()
    val BUILT_IN: List<DebugEngineEntry> get()=nativeEntries
    val all: List<DebugEngineEntry> get()=nativeEntries
    fun setContributed(entries: List<DebugEngineEntry>) { nativeEntries=entries.distinctBy { it.id } }
    fun findById(id: String): DebugEngineEntry? = nativeEntries.firstOrNull { it.id==id }
}
