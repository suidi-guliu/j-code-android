package dev.blamspot.jcode.core.distro

/**
 * A language server in the LSP catalog. This is the single source of truth for both the LSP Manager
 * (install/verify/uninstall) and the runtime launcher (run/languageIds/extensions). It lives in
 * `:core:distro` because the install machinery (`DistroService`) cannot depend on `:core:lsp`
 * (that would close a module cycle); `:core:lsp` derives its runtime descriptors from this list.
 */
data class LspCatalogEntry(
    val id: String,
    val category: String,
    val name: String,
    val description: String,
    val installCommand: String,
    val verifyCommand: String,
    val uninstallCommand: String,
    val runCommand: String,
    /** Optional: exits 0 when a newer version is available. Empty = update detection skipped. */
    val updateCheckCommand: String = "",
    val languageIds: List<String> = emptyList(),
    val extensions: List<String> = emptyList(),
    val rootDetectors: List<String> = emptyList(),
    /** SDK catalog ids this server needs (e.g. csharp-ls needs the dotnet SDK). Installed first. */
    val requiredSdks: List<String> = emptyList(),
    val initializationOptions: String = "{}",
    val ownerPackage: String = "",
)

enum class LspCatalogAction(val label: String) {
    Install("Install"),
    Uninstall("Remove"),
}

data class LspCatalogState(
    val entries: List<LspCatalogEntry> = emptyList(),
    val installedEntryIds: Set<String> = emptySet(),
    /**
     * Whether [installedEntryIds] has been loaded for the active distro at least once.
     *
     * Until then an empty set means "not known yet", not "nothing installed" — and the two are
     * indistinguishable by timing, because the load is quiet for ~10s on a cold launch while the
     * environment is probed first. Anything that would accuse a server of being missing must wait
     * for this.
     */
    val loaded: Boolean = false,
    val updatableEntryIds: Set<String> = emptySet(),
    val checking: Boolean = false,
    val runningEntryId: String? = null,
    val runningAction: LspCatalogAction? = null,
    val executionLabel: String? = null,
    val logLines: List<String> = emptyList(),
    val selectedDistroId: String? = null,
    val errorMessage: String? = null,
)

/** Catalog entries are registered by enabled, authorized sui-code IDE Kits. */
object LspServerCatalog {
    @Volatile private var entries: List<LspCatalogEntry> = emptyList()
    val BUILT_IN: List<LspCatalogEntry> get() = entries
    fun registerNative(values: List<LspCatalogEntry>) { entries=values.distinctBy { it.id } }
    fun findById(id: String): LspCatalogEntry? = entries.firstOrNull { it.id==id }
}
