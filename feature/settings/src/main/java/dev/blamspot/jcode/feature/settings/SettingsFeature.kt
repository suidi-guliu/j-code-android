package dev.blamspot.jcode.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import dev.blamspot.jcode.design.AlertDialog
import dev.blamspot.jcode.design.CompactFilledButton
import dev.blamspot.jcode.design.CompactOutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.blamspot.jcode.core.config.ConfigScope
import dev.blamspot.jcode.core.config.EffectiveConfig
import dev.blamspot.jcode.core.config.ProjectConfig
import dev.blamspot.jcode.core.config.WorkspaceConfig
import dev.blamspot.jcode.design.FileIconSet
import dev.blamspot.jcode.design.painter
import dev.blamspot.jcode.design.LocalFileIconSet
import dev.blamspot.jcode.design.FileTypeIcon
import dev.blamspot.jcode.design.FileIconSetRegistry
import dev.blamspot.jcode.design.LocalIconSetSettings
import dev.blamspot.jcode.design.UiIconSet
import dev.blamspot.jcode.design.UiIconSetRegistry
import dev.blamspot.jcode.design.IconSize
import dev.blamspot.jcode.design.JCodeIcon
import dev.blamspot.jcode.design.Radius
import dev.blamspot.jcode.design.SettingsActionRow
import dev.blamspot.jcode.design.Space
import dev.blamspot.jcode.design.StrokeWidth
import dev.blamspot.jcode.design.jcIcon
import dev.blamspot.jcode.design.BottomBarVisibility
import dev.blamspot.jcode.design.ExtraKeysVisibility
import dev.blamspot.jcode.design.LocalBottomBarSetting
import dev.blamspot.jcode.design.LocalFontSettings
import dev.blamspot.jcode.design.LocalEditorDragMovesCursor
import dev.blamspot.jcode.design.LocalDiagnosticsSetting
import dev.blamspot.jcode.design.LocalEditorFontSizeSetting
import dev.blamspot.jcode.design.LocalExtensionFontSizeSetting
import dev.blamspot.jcode.design.LocalTerminalFontSizeSetting
import dev.blamspot.jcode.design.LocalEditorWordWrapSetting
import dev.blamspot.jcode.design.LocalExtraKeysSetting
import dev.blamspot.jcode.design.LocalPerformanceSettings
import dev.blamspot.jcode.design.LocalRightDrawerSetting
import dev.blamspot.jcode.design.ExplorerExcludeEffect
import dev.blamspot.jcode.design.ExplorerHiddenMode
import dev.blamspot.jcode.design.LocalAndroidDevice
import dev.blamspot.jcode.design.LocalAppUpdate
import dev.blamspot.jcode.design.LocalSettingsBackup
import dev.blamspot.jcode.design.EnvVarSettings
import dev.blamspot.jcode.design.LocalEnvVarSettings
import dev.blamspot.jcode.design.LocalEnvironmentBackup
import dev.blamspot.jcode.design.LocalCutoutSetting
import dev.blamspot.jcode.design.LocalExplorerHiddenSetting
import dev.blamspot.jcode.design.LocalTrashSettings
import dev.blamspot.jcode.design.TRASH_RETENTION_CHOICES
import dev.blamspot.jcode.design.trashRetentionLabel
import dev.blamspot.jcode.design.LocalTabColoringSetting
import dev.blamspot.jcode.design.LocalTabMaxSize
import dev.blamspot.jcode.design.TabColoring
import dev.blamspot.jcode.design.TabMaxSize
import dev.blamspot.jcode.design.HeaderActionButton
import dev.blamspot.jcode.design.LocalCommandPaletteSetting
import dev.blamspot.jcode.design.LocalHeaderActionSetting
import dev.blamspot.jcode.design.LocalDeveloperSetting
import dev.blamspot.jcode.design.LocalMarkdownPreviewSetting
import dev.blamspot.jcode.design.LocalVolumeKeysSetting
import dev.blamspot.jcode.design.PaletteCommandCatalog
import dev.blamspot.jcode.design.VolumeKeyAction
import dev.blamspot.jcode.design.LocalRestoreSession
import dev.blamspot.jcode.design.WebPreviewBrowsers
import dev.blamspot.jcode.design.LocalWebPreviewBrowsers
import dev.blamspot.jcode.design.LocalTabCloseButtonSetting
import dev.blamspot.jcode.design.SettingsDefaults
import dev.blamspot.jcode.design.SettingsDropdownRow
import dev.blamspot.jcode.design.SettingsResettableRow
import dev.blamspot.jcode.design.SettingsTextFieldRow
import dev.blamspot.jcode.design.ThemeBundleRegistry
import dev.blamspot.jcode.core.diag.DiagLevel
import dev.blamspot.jcode.core.distro.AppProcesses
import dev.blamspot.jcode.core.distro.DistroEnvironmentState
import dev.blamspot.jcode.design.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

object SettingsFeature {

    /** Group the next composition should jump to, or null. Set by [revealGroup]. */
    private val pendingReveal = mutableStateOf<String?>(null)

    /**
     * Ask the Settings screen to reveal a group by title — switch to the GLOBAL tab, clear any
     * search, expand it, and scroll it into view. Used by the "update available" toast so its Update
     * action lands the user on the progress it starts. Safe to call before Settings is composed: the
     * request is consumed by whichever composition runs next.
     */
    fun revealGroup(title: String) {
        pendingReveal.value = title
    }

    @Composable
    fun Content(
        effectiveConfig: EffectiveConfig,
        workspaceConfig: WorkspaceConfig?,
        projectConfig: ProjectConfig?,
        workspaceError: String?,
        projectError: String?,
        projectOverridesAvailable: Boolean,
        environmentState: DistroEnvironmentState,
        onOpenWorkspaceConfig: () -> Unit,
        onOpenProjectConfig: () -> Unit,
        onOpenEnvironmentWizard: () -> Unit,
        onRefreshEnvironment: () -> Unit,
        // A null value clears the override from the scope's .jcode (see MainViewModel).
        onUpdateFontSize: (ConfigScope, Float?) -> Unit,
        onUpdateTabSize: (ConfigScope, Int?) -> Unit,
        onUpdateTabColoring: (ConfigScope, String?) -> Unit,
        onUpdateLigatures: (ConfigScope, Boolean?) -> Unit,
        onUpdateExplorerViewMode: (ConfigScope, String?) -> Unit,
        themeMode: ThemeMode,
        onUpdateThemeMode: (ThemeMode?) -> Unit,
        themeBundleId: String,
        onUpdateThemeBundle: (String) -> Unit,
        formatterId: String,
        formatterOptions: List<Pair<String, String>>,
        onSelectFormatter: (String) -> Unit,
        isUserWorkspace: Boolean = false,
        modifier: Modifier = Modifier,
    ) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

        val iconSettings = LocalIconSetSettings.current
        val tabCloseSetting = LocalTabCloseButtonSetting.current
        val editorDragSetting = LocalEditorDragMovesCursor.current
        val restoreSessionSetting = LocalRestoreSession.current
        val explorerHiddenSetting = LocalExplorerHiddenSetting.current
        val trashSettings = LocalTrashSettings.current
        val cutoutSetting = LocalCutoutSetting.current
        val volumeKeysSetting = LocalVolumeKeysSetting.current
        val tabColoringSetting = LocalTabColoringSetting.current
        val tabMaxSizeSetting = LocalTabMaxSize.current
        val extraKeysSetting = LocalExtraKeysSetting.current
        val bottomBarSetting = LocalBottomBarSetting.current
        val fontSettings = LocalFontSettings.current
        val perf = LocalPerformanceSettings.current
        val webPreview = LocalWebPreviewBrowsers.current
        // The tab IS the scope — no separate "Edit scope" selector. Index 0 = Global (app-level);
        // each further tab edits one .jcode scope: WORKSPACE appears when a User Workspace is open,
        // PROJECT when a local project is selected, and the Default Workspace's own scope is offered
        // only when there is no project to scope to.
        var selectedTab by rememberSaveable { mutableStateOf(0) }
        val tabScopes: List<ConfigScope?> = buildList {
            add(null)
            if (isUserWorkspace) add(ConfigScope.Workspace)
            if (projectOverridesAvailable) add(ConfigScope.Project)
            if (size == 1) add(ConfigScope.Workspace)
        }
        // The trailing "ENV VAR" tab lives at index tabScopes.size (it is not a ConfigScope).
        val safeTab = selectedTab.coerceIn(0, tabScopes.size)
        val isEnvVarTab = safeTab == tabScopes.size
        // Scoped cards also render while a search is active (from any tab); they then edit the most
        // specific scope available. getOrNull guards the ENV VAR tab index (out of tabScopes range).
        val selectedScope = tabScopes.getOrNull(safeTab)
            ?: if (projectOverridesAvailable) ConfigScope.Project else ConfigScope.Workspace

        val scopedEditor = when (selectedScope) {
            ConfigScope.Workspace -> workspaceConfig?.editor
            ConfigScope.Project -> projectConfig?.editor
        }

        val fontSize = scopedEditor?.fontSize ?: effectiveConfig.editor.fontSize
        val tabSize = scopedEditor?.tabSize ?: effectiveConfig.editor.tabSize
        val ligatures = scopedEditor?.ligatures ?: effectiveConfig.editor.ligatures

        val scopedExplorer = when (selectedScope) {
            ConfigScope.Workspace -> workspaceConfig?.explorer
            ConfigScope.Project -> projectConfig?.explorer
        }
        val explorerViewMode = scopedExplorer?.viewMode ?: effectiveConfig.explorer.viewMode

        var query by rememberSaveable { mutableStateOf("") }
        val scrollState = rememberScrollState()
        // A reveal request (e.g. the update toast's Update action) puts the named group on screen:
        // the GLOBAL tab because that is where they live, no search because a query bypasses grouping
        // entirely, then expand and scroll. The group's offset is only known once it has been laid
        // out, and Settings may have opened on this very frame, so wait for it rather than guessing.
        val reveal = pendingReveal.value
        LaunchedEffect(reveal) {
            val title = reveal ?: return@LaunchedEffect
            selectedTab = 0
            query = ""
            settingsGroupExpanded.getOrPut(title) { mutableStateOf(false) }.value = true
            var frames = 0
            while (settingsGroupOffsets[title] == null && frames++ < 30) withFrameNanos { }
            settingsGroupOffsets[title]?.let { scrollState.animateScrollTo(it.roundToInt()) }
            pendingReveal.value = null
        }
        // Fresh each composition; cards increment it when they pass the filter, and the trailing
        // empty-state reads it after all cards have composed.
        val matchSink = SettingsMatchSink()
        CompositionLocalProvider(
            LocalSettingsQuery provides query,
            LocalSettingsMatchSink provides matchSink,
        ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                // Less room above the scope tabs than around everything else: they are a header, and
                // the editor's own tab strip is already a horizontal edge directly above them, so a
                // full margin there read as a band of nothing between two rows of tabs.
                .padding(start = Space.md, end = Space.md, top = Space.xs, bottom = Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.ms),
        ) {
            // Material underline tabs, left-packed. ScrollableTabRow's own divider only spans the
            // tab content, so it is suppressed and a full-width one is drawn behind the row.
            Box(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(modifier = Modifier.align(Alignment.BottomStart))
                ScrollableTabRow(
                    selectedTabIndex = safeTab,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                ) {
                    tabScopes.forEachIndexed { index, scope ->
                        Tab(
                            selected = safeTab == index,
                            onClick = { selectedTab = index },
                            modifier = Modifier.height(40.dp),
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            text = {
                                Text(
                                    text = when (scope) {
                                        null -> strings.text("ui.gLOBAL")
                                        ConfigScope.Workspace -> strings.text("ui.fILES")
                                        ConfigScope.Project -> strings.text("ui.pROJECT")
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                        )
                    }
                    // Trailing content tab (not a scope): the environment-variable editor.
                    Tab(
                        selected = isEnvVarTab,
                        onClick = { selectedTab = tabScopes.size },
                        modifier = Modifier.height(40.dp),
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = {
                            Text(
                                text = strings.text("ui.eNVVAR"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                    )
                }
            }
            if (isEnvVarTab) {
                EnvVarEditor(LocalEnvVarSettings.current)
            } else {
            SettingsSearchField(query = query, onQueryChange = { query = it })
            }
            // Search is scoped to the SELECTED tab (like VS Code's User/Workspace split): the GLOBAL
            // tab shows only app-level settings, WORKSPACE/PROJECT only the .jcode-scoped ones — so a
            // search on the Project tab never surfaces global settings that aren't project-overridable.
            val showGlobalTab = safeTab == 0
            val showScopedTab = safeTab in 1 until tabScopes.size
            if (showGlobalTab) {
            SettingsGroup("ui.appearance") {
            SettingsCard(
                title = strings.text("ui.appearance"),
                description = strings.text("ui.systemFollowsYourDeviceSLightDarkSetting"),
                keywords = "appearance theme dark light system color mode scheme",
            ) {
                SettingsDropdownRow(
                    label = strings.text("ui.mode"),
                    options = ThemeMode.entries.map { it.name },
                    optionLabel = { strings.text(when(ThemeMode.valueOf(it)) { ThemeMode.System -> "option.System"; ThemeMode.Light -> "option.Light"; ThemeMode.Dark -> "option.Dark" }) },
                    selected = themeMode.name,
                    onSelect = { onUpdateThemeMode(ThemeMode.valueOf(it)) },
                    modified = workspaceConfig?.theme?.id != null || projectConfig?.theme?.id != null,
                    onReset = { onUpdateThemeMode(null) },
                )
            }

            SettingsCard(
                title = strings.text("ui.themeBundle"),
                description = strings.text("ui.colorPaletteAppliedAcrossTheApp"),
                keywords = "theme bundle color palette catppuccin dracula midnight oled black scheme appearance",
            ) {
                val activeBundle = themeBundleId.ifEmpty { ThemeBundleRegistry.default.id }
                ThemeBundleRegistry.builtIns.forEach { bundle ->
                    BundleRow(
                        name = bundle.name,
                        description = bundle.description,
                        selected = activeBundle == bundle.id,
                        swatch = listOf(
                            bundle.dark.primary,
                            bundle.dark.secondary,
                            bundle.dark.tertiary,
                            bundle.dark.surface,
                        ),
                        onClick = { onUpdateThemeBundle(bundle.id) },
                    )
                }
            }

            SettingsCard(
                title = strings.text("ui.uIIcons"),
                description = strings.text("ui.iconSetUsedForTheAppSOwnToolbars"),
                keywords = "icon bundle icons set ui material rounded jcode line appearance " +
                    iconSettings.uiSets.joinToString(" ") { it.name },
            ) {
                val activeUi = iconSettings.activeUiSetId
                iconSettings.uiSets.forEach { set ->
                    UiIconSetRow(
                        set = set,
                        selected = activeUi == set.id,
                        onClick = { iconSettings.onSelectUiSet(set.id) },
                    )
                }
            }

            // Hidden until something can fill it: JCode ships no file icon set, so with no icon-pack
            // extension installed this card would offer exactly one choice — the one already in use.
            if (iconSettings.fileSets.isNotEmpty()) {
                SettingsCard(
                    title = strings.text("ui.fileIcons"),
                    description = strings.text("ui.iconSetUsedForFilesAndFoldersInThe"),
                    keywords = "icon bundle icons set file files folder explorer appearance " +
                        iconSettings.fileSets.joinToString(" ") { it.name },
                ) {
                    val activeFiles = iconSettings.activeFileSetId
                    FileIconSetRow(
                        name = "None",
                        description = strings.text("ui.jCodeSOwnFolderAndFileGlyphsFromThe"),
                        detail = null,
                        selected = activeFiles == FileIconSetRegistry.NONE_ID,
                        onClick = { iconSettings.onSelectFileSet(FileIconSetRegistry.NONE_ID) },
                    )
                    iconSettings.fileSets.forEach { set ->
                        FileIconSetRow(
                            name = set.name,
                            description = set.description,
                            detail = set,
                            selected = activeFiles == set.id,
                            onClick = { iconSettings.onSelectFileSet(set.id) },
                        )
                    }
                }
            }

            SettingsCard(
                title = strings.text("ui.fonts"),
                description = strings.text("ui.monospaceTypefacesForTheCodeEditorAndTheTerminal"),
                keywords = "font fonts family typeface monospace editor terminal jetbrains mono system code appearance " +
                    fontSettings.options.joinToString(" ") { it.name },
            ) {
                // Re-scan the environment's installed fonts each time this card is shown, so fonts the
                // user apt-installed since launch appear without a restart.
                LaunchedEffect(Unit) { fontSettings.onScanFonts() }
                val fontOptionIds = fontSettings.options.map { it.id }
                val fontLabel: (String) -> String =
                    { id -> fontSettings.options.firstOrNull { it.id == id }?.name ?: id }
                SettingsDropdownRow(
                    label = strings.text("ui.editorFont"),
                    options = fontOptionIds,
                    selected = fontSettings.editorFontId,
                    onSelect = fontSettings.onSelectEditorFont,
                    optionLabel = fontLabel,
                    modified = fontSettings.editorFontId != fontSettings.editorDefaultId,
                    onReset = { fontSettings.onSelectEditorFont(fontSettings.editorDefaultId) },
                )
                SettingsDropdownRow(
                    label = strings.text("ui.terminalFont"),
                    options = fontOptionIds,
                    selected = fontSettings.terminalFontId,
                    onSelect = fontSettings.onSelectTerminalFont,
                    optionLabel = fontLabel,
                    modified = fontSettings.terminalFontId != fontSettings.terminalDefaultId,
                    onReset = { fontSettings.onSelectTerminalFont(fontSettings.terminalDefaultId) },
                )
            }

            SettingsCard(
                title = strings.text("ui.terminal"),
                description = strings.text("ui.textSizeForTerminalSessionsAppliesToEveryOpen"),
                keywords = "terminal font size text scale sp bigger smaller zoom console shell tty readable",
            ) {
                val terminalFontSizeSetting = LocalTerminalFontSizeSetting.current
                StepperRow(
                    label = strings.text("ui.fontSize"),
                    value = "${terminalFontSizeSetting.value.toInt()} sp",
                    onDecrease = { terminalFontSizeSetting.onChange((terminalFontSizeSetting.value - 1f).coerceAtLeast(6f)) },
                    onIncrease = { terminalFontSizeSetting.onChange((terminalFontSizeSetting.value + 1f).coerceAtMost(40f)) },
                    modified = terminalFontSizeSetting.value != SettingsDefaults.TERMINAL_FONT_SIZE,
                    onReset = { terminalFontSizeSetting.onChange(SettingsDefaults.TERMINAL_FONT_SIZE) },
                )
            }

            SettingsCard(
                title = strings.text("ui.extensions"),
                description = strings.text("ui.textSizeInsideImportedVsixExtensionsAScaleRather"),
                keywords = "extension extensions vsix font size text scale zoom bigger smaller " +
                    "readable webview marketplace imported",
            ) {
                val extensionFontSize = LocalExtensionFontSizeSetting.current
                StepperRow(
                    label = strings.text("ui.fontSize"),
                    value = "${extensionFontSize.percent}%",
                    onDecrease = { extensionFontSize.onChange((extensionFontSize.percent - 10).coerceAtLeast(50)) },
                    onIncrease = { extensionFontSize.onChange((extensionFontSize.percent + 10).coerceAtMost(300)) },
                    modified = extensionFontSize.percent != SettingsDefaults.EXTENSION_FONT_SCALE,
                    onReset = { extensionFontSize.onChange(SettingsDefaults.EXTENSION_FONT_SCALE) },
                )
            }

            // Hidden on displays without a cutout (desktop mode, external display, notchless devices).
            if (cutoutSetting.hasCutout) {
                SettingsCard(
                    title = strings.text("ui.displayCutout"),
                    description = strings.text("ui.keepTheAppClearOfTheCameraNotchOr"),
                    keywords = "cutout notch punch hole camera display safe area letterbox fullscreen screen edge insets",
                ) {
                    ToggleRow(
                        label = strings.text("ui.respectDeviceCutout"),
                        supporting = strings.text("ui.layOutTheAppInsideTheCutoutSSafe"),
                        checked = cutoutSetting.respect,
                        onCheckedChange = cutoutSetting.onChange,
                        modified = cutoutSetting.respect != SettingsDefaults.RESPECT_DEVICE_CUTOUT,
                        onReset = { cutoutSetting.onChange(SettingsDefaults.RESPECT_DEVICE_CUTOUT) },
                    )
                }
            }

            SettingsCard(
                title = strings.text("ui.terminalPanel"),
                description = strings.text("ui.terminalOutputIssuesAndDebuggingShareTheBottomPanel"),
                keywords = "terminal panel height split resize bottom output debug",
            ) {
                val setting=LocalRightDrawerSetting.current
                Text(strings.text("settings.panelHeight", mapOf("percent" to ((setting.widthFraction.coerceIn(0.20f,0.55f)*100).toInt()).toString())))
                androidx.compose.material3.Slider(value=setting.widthFraction.coerceIn(0.20f,0.55f),onValueChange=setting.onSetWidthFraction,valueRange=0.20f..0.55f,steps=6)
                Text(strings.text("ui.dragTheDividerOrUseThisKeyboardAccessibleSlider"))
            }

            SettingsCard(
                title = strings.text("ui.header"),
                description = strings.text("ui.theBarAcrossTheTopOfTheWorkbenchWith"),
                keywords = "header top bar app bar terminal command palette button action hide disable remove",
            ) {
                val headerActionSetting = LocalHeaderActionSetting.current
                SettingsDropdownRow(
                    label = strings.text("ui.actionButton"),
                    supporting = strings.text("ui.theButtonBesideRunHidingItLeavesTheTerminal"),
                    options = HeaderActionButton.entries.map { it.name },
                    selected = headerActionSetting.button.name,
                    onSelect = { headerActionSetting.onChange(HeaderActionButton.valueOf(it)) },
                    optionLabel = { headerActionButtonLabel(strings, HeaderActionButton.valueOf(it)) },
                    modified = headerActionSetting.button != SettingsDefaults.HEADER_ACTION_BUTTON,
                    onReset = { headerActionSetting.onChange(SettingsDefaults.HEADER_ACTION_BUTTON) },
                )
            }

            SettingsCard(
                title = strings.text("ui.bottomStatusBar"),
                description = strings.text("ui.theBarAtTheBottomOfTheWorkbenchShowing"),
                keywords = "bottom status bar branch distro cursor position hide always show soft keyboard chrome space",
            ) {
                SettingsDropdownRow(
                    label = strings.text("ui.show"),
                    options = BottomBarVisibility.entries.map { it.name },
                    selected = bottomBarSetting.visibility.name,
                    onSelect = { bottomBarSetting.onChange(BottomBarVisibility.valueOf(it)) },
                    optionLabel = { bottomBarVisibilityLabel(strings, BottomBarVisibility.valueOf(it)) },
                    modified = bottomBarSetting.visibility != SettingsDefaults.BOTTOM_STATUS_BAR,
                    onReset = { bottomBarSetting.onChange(SettingsDefaults.BOTTOM_STATUS_BAR) },
                )
            }

            SettingsCard(
                title = strings.text("ui.extraKeysRow"),
                description = strings.text("ui.aTermuxStyleKeyRowEscTabCtrlArrows"),
                keywords = "extra keys row esc ctrl alt tab arrows home end pgup pgdn page terminal editor keyboard termux orientation portrait landscape hidden always with soft keyboard function keys f1 f2 f3 f4 f5 f6 f7 f8 f9 f10 f11 f12 fn htop midnight commander",
            ) {
                SettingsDropdownRow(
                    label = strings.text("ui.portrait"),
                    options = ExtraKeysVisibility.entries.map { it.name },
                    selected = extraKeysSetting.portrait.name,
                    onSelect = { extraKeysSetting.onChangePortrait(ExtraKeysVisibility.valueOf(it)) },
                    optionLabel = { extraKeysVisibilityLabel(strings, ExtraKeysVisibility.valueOf(it)) },
                    modified = extraKeysSetting.portrait != SettingsDefaults.EXTRA_KEYS_PORTRAIT,
                    onReset = { extraKeysSetting.onChangePortrait(SettingsDefaults.EXTRA_KEYS_PORTRAIT) },
                )
                SettingsDropdownRow(
                    label = strings.text("ui.landscape"),
                    options = ExtraKeysVisibility.entries.map { it.name },
                    selected = extraKeysSetting.landscape.name,
                    onSelect = { extraKeysSetting.onChangeLandscape(ExtraKeysVisibility.valueOf(it)) },
                    optionLabel = { extraKeysVisibilityLabel(strings, ExtraKeysVisibility.valueOf(it)) },
                    modified = extraKeysSetting.landscape != SettingsDefaults.EXTRA_KEYS_LANDSCAPE,
                    onReset = { extraKeysSetting.onChangeLandscape(SettingsDefaults.EXTRA_KEYS_LANDSCAPE) },
                )
                ToggleRow(
                    label = strings.text("ui.functionKeys"),
                    supporting = strings.text("ui.appendF1F12ChipsToTheRowWhileA"),
                    checked = extraKeysSetting.functionKeys,
                    onCheckedChange = { extraKeysSetting.onChangeFunctionKeys(it) },
                    modified = extraKeysSetting.functionKeys != SettingsDefaults.EXTRA_KEYS_FUNCTION_KEYS,
                    onReset = { extraKeysSetting.onChangeFunctionKeys(SettingsDefaults.EXTRA_KEYS_FUNCTION_KEYS) },
                )
            }

            } // end Appearance

            SettingsGroup("option.Input") {
            SettingsCard(
                title = strings.text("ui.volumeKeys"),
                description = strings.text("ui.remapTheHardwareVolumeButtonsToEditorTerminalActions"),
                keywords = "volume keys button hardware remap bind binding shortcut undo redo arrow scroll " +
                    "command palette input up down page rocker media",
            ) {
                SettingsDropdownRow(
                    label = strings.text("ui.volumeUp"),
                    options = VolumeKeyAction.entries.map { it.name },
                    selected = volumeKeysSetting.up.name,
                    onSelect = { volumeKeysSetting.onChangeUp(VolumeKeyAction.valueOf(it)) },
                    optionLabel = { volumeKeyActionLabel(strings, VolumeKeyAction.valueOf(it), "Vol Up") },
                    modified = volumeKeysSetting.up != SettingsDefaults.VOLUME_UP_ACTION,
                    onReset = { volumeKeysSetting.onChangeUp(SettingsDefaults.VOLUME_UP_ACTION) },
                )
                SettingsDropdownRow(
                    label = strings.text("ui.volumeDown"),
                    options = VolumeKeyAction.entries.map { it.name },
                    selected = volumeKeysSetting.down.name,
                    onSelect = { volumeKeysSetting.onChangeDown(VolumeKeyAction.valueOf(it)) },
                    optionLabel = { volumeKeyActionLabel(strings, VolumeKeyAction.valueOf(it), "Vol Down") },
                    modified = volumeKeysSetting.down != SettingsDefaults.VOLUME_DOWN_ACTION,
                    onReset = { volumeKeysSetting.onChangeDown(SettingsDefaults.VOLUME_DOWN_ACTION) },
                )
            }

            SettingsCard(
                title = strings.text("ui.commandPalette"),
                description = strings.text("ui.chooseWhichBuiltInCommandsThePaletteOffersContext"),
                keywords = "command palette commands orientation lock fullscreen keep awake screen on " +
                    "hide header tabs zen go to line color search picker eyedropper format document",
            ) {
                val paletteSetting = LocalCommandPaletteSetting.current
                PaletteCommandCatalog.forEach { command ->
                    val enabled = command.id !in paletteSetting.disabledIds
                    ToggleRow(
                        label = command.label,
                        supporting = command.description,
                        checked = enabled,
                        onCheckedChange = { paletteSetting.onSetEnabled(command.id, it) },
                        modified = !enabled,
                        onReset = { paletteSetting.onSetEnabled(command.id, true) },
                    )
                }
            }

            } // end Input

            SettingsGroup("option.Startup") {
            SettingsCard(
                title = strings.text("ui.restoreLastSession"),
                description = strings.text("ui.pickUpWhereYouLeftOffAfterClosingThe"),
                keywords = "restore session reopen tabs workspace project unsaved recover startup launch",
            ) {
                ToggleRow(
                    label = strings.text("ui.restoreLastSessionOnLaunch"),
                    supporting = strings.text("ui.reopenTheLastWorkspaceProjectAndEditorTabsIncluding"),
                    checked = restoreSessionSetting.enabled,
                    onCheckedChange = restoreSessionSetting.onChange,
                    modified = restoreSessionSetting.enabled != SettingsDefaults.RESTORE_LAST_SESSION,
                    onReset = { restoreSessionSetting.onChange(SettingsDefaults.RESTORE_LAST_SESSION) },
                )
            }

            } // end Startup

            // Per-extension settings now live on the Extension Settings screen (Extensions list → gear),
            // alongside each extension's permissions — not here in App Settings.

            SettingsGroup("option.Performance") {
            SettingsCard(
                title = strings.text("ui.rendering"),
                description = strings.text("ui.howJCodeDrawsTheUIEditorAndTerminal"),
                keywords = "performance rendering hardware acceleration gpu software draw graphics lag smooth",
            ) {
                ToggleRow(
                    label = strings.text("ui.hardwareAcceleration"),
                    supporting = strings.text("ui.renderTheUIEditorAndTerminalOnTheGPU"),
                    checked = perf.hardwareAcceleration,
                    onCheckedChange = perf.onSetHardwareAcceleration,
                    modified = perf.hardwareAcceleration != SettingsDefaults.HARDWARE_ACCELERATION,
                    onReset = { perf.onSetHardwareAcceleration(SettingsDefaults.HARDWARE_ACCELERATION) },
                )
            }
            SettingsCard(
                title = strings.text("ui.resourceManagement"),
                description = strings.text("ui.keepTheLinuxRuntimeLeanByStoppingWorkYou"),
                keywords = "performance memory cpu battery proot process terminal kill close idle background resource optimize swipe away warn running max instances timeout auto-close nested sub-shell subshell relocate tab bash zsh install toolchain sdk download timeout minutes android",
            ) {
                ToggleRow(
                    label = strings.text("ui.warnBeforeClosingRunningProcesses"),
                    supporting = strings.text("ui.whenClosingAProjectOrWorkspaceWithARunning"),
                    checked = perf.confirmCloseRunning,
                    onCheckedChange = perf.onSetConfirmCloseRunning,
                    modified = perf.confirmCloseRunning != SettingsDefaults.CONFIRM_CLOSE_RUNNING,
                    onReset = { perf.onSetConfirmCloseRunning(SettingsDefaults.CONFIRM_CLOSE_RUNNING) },
                )
                ToggleRow(
                    label = strings.text("ui.closeAppFullyOnSwipeAway"),
                    supporting = strings.text("ui.whenYouSwipeJCodeOffTheAndroidRecentsScreen"),
                    checked = perf.exitOnSwipeAway,
                    onCheckedChange = perf.onSetExitOnSwipeAway,
                    modified = perf.exitOnSwipeAway != SettingsDefaults.EXIT_ON_SWIPE_AWAY,
                    onReset = { perf.onSetExitOnSwipeAway(SettingsDefaults.EXIT_ON_SWIPE_AWAY) },
                )
                ToggleRow(
                    label = strings.text("ui.autoCloseIdleTerminals"),
                    supporting = strings.text("ui.automaticallyCloseTerminalsLeftIdleAtThePromptNo"),
                    checked = perf.autoCloseIdleTerminals,
                    onCheckedChange = perf.onSetAutoCloseIdleTerminals,
                    modified = perf.autoCloseIdleTerminals != SettingsDefaults.AUTO_CLOSE_IDLE_TERMINALS,
                    onReset = { perf.onSetAutoCloseIdleTerminals(SettingsDefaults.AUTO_CLOSE_IDLE_TERMINALS) },
                )
                if (perf.autoCloseIdleTerminals) {
                    StepperRow(
                        label = strings.text("ui.idleTimeout"),
                        value = "${perf.idleTimeoutMinutes} min",
                        onDecrease = { perf.onSetIdleTimeoutMinutes(perf.idleTimeoutMinutes - 5) },
                        onIncrease = { perf.onSetIdleTimeoutMinutes(perf.idleTimeoutMinutes + 5) },
                        modified = perf.idleTimeoutMinutes != SettingsDefaults.IDLE_TIMEOUT_MINUTES,
                        onReset = { perf.onSetIdleTimeoutMinutes(SettingsDefaults.IDLE_TIMEOUT_MINUTES) },
                    )
                }
                StepperRow(
                    label = strings.text("ui.maxTerminalInstances"),
                    value = "${perf.maxTerminalSessions}",
                    onDecrease = { perf.onSetMaxTerminalSessions((perf.maxTerminalSessions - 1).coerceAtLeast(1)) },
                    onIncrease = { perf.onSetMaxTerminalSessions((perf.maxTerminalSessions + 1).coerceAtMost(24)) },
                    modified = perf.maxTerminalSessions != SettingsDefaults.MAX_TERMINAL_SESSIONS,
                    onReset = { perf.onSetMaxTerminalSessions(SettingsDefaults.MAX_TERMINAL_SESSIONS) },
                )
                StepperRow(
                    label = strings.text("ui.toolchainInstallTimeout"),
                    supporting = strings.text("ui.howLongAToolchainInstallSDKLanguageServerDebugger"),
                    value = "${perf.installTimeoutMinutes} min",
                    onDecrease = { perf.onSetInstallTimeoutMinutes((perf.installTimeoutMinutes - 5).coerceAtLeast(5)) },
                    onIncrease = { perf.onSetInstallTimeoutMinutes((perf.installTimeoutMinutes + 5).coerceAtMost(180)) },
                    modified = perf.installTimeoutMinutes != SettingsDefaults.INSTALL_TIMEOUT_MINUTES,
                    onReset = { perf.onSetInstallTimeoutMinutes(SettingsDefaults.INSTALL_TIMEOUT_MINUTES) },
                )
                ToggleRow(
                    label = strings.text("ui.subShellsOpenInTheirOwnTab"),
                    supporting = strings.text("ui.whenYouStartAnInteractiveShellBashZshInside"),
                    checked = perf.nestedShellTabs,
                    onCheckedChange = perf.onSetNestedShellTabs,
                    modified = perf.nestedShellTabs != SettingsDefaults.NESTED_SHELL_TABS,
                    onReset = { perf.onSetNestedShellTabs(SettingsDefaults.NESTED_SHELL_TABS) },
                )
            }
            } // end Performance

            } // end Global-only cards; the Web preview card below renders on every scope tab.

            // "Open web previews in" edits the app-wide default on the GLOBAL tab and a per-project
            // override on the PROJECT tab (INHERIT = fall back to that default). It renders on every
            // tab; the raw selected tab (not [selectedScope], which coalesces GLOBAL into Project when
            // a project is open) decides which it edits, so the GLOBAL tab always edits the default.
            // Web preview renders on every scope tab, but not on the ENV VAR content tab.
            if (!isEnvVarTab) {
            val projectBrowserScope =
                tabScopes.getOrNull(safeTab) == ConfigScope.Project && webPreview.currentProjectKey.isNotBlank()
            SettingsGroup("option.Webpreview") {
            SettingsCard(
                title = strings.text("ui.openWebPreviewsIn"),
                description = if (projectBrowserScope) {
                    strings.text("ui.theBrowserThisProjectUsesWhenYouOpenA")
                } else {
                    strings.text("ui.theBrowserUsedWhenYouOpenARunningDev")
                },
                keywords = "browser web preview open url chrome firefox default run dev server " +
                    "system always ask chooser built-in builtin inherit global project override " +
                    webPreview.available.joinToString(" ") { it.label },
            ) {
                val options = buildList {
                    if (projectBrowserScope) add(WebPreviewBrowsers.INHERIT)
                    add(WebPreviewBrowsers.SYSTEM)
                    add(WebPreviewBrowsers.ASK)
                    add(WebPreviewBrowsers.BUILTIN)
                    webPreview.available.forEach { add(it.packageName) }
                }
                val selectedChoice = if (projectBrowserScope) {
                    webPreview.projectChoice(webPreview.currentProjectKey)
                } else {
                    webPreview.globalChoice
                }
                options.forEach { choice ->
                    BundleRow(
                        name = webPreview.label(choice),
                        description = when (choice) {
                            WebPreviewBrowsers.INHERIT -> strings.text("ui.fallBackToTheAppWideDefault")
                            WebPreviewBrowsers.SYSTEM -> strings.text("ui.theDeviceSDefaultBrowserApp")
                            WebPreviewBrowsers.ASK -> strings.text("ui.showTheAndroidAppChooserEachTime")
                            WebPreviewBrowsers.BUILTIN -> strings.text("ui.jCodeSOwnInEditorBrowserWithDevTools")
                            else -> choice
                        },
                        selected = selectedChoice == choice,
                        swatch = emptyList(),
                        onClick = {
                            if (projectBrowserScope) {
                                webPreview.onSetProject(webPreview.currentProjectKey, choice)
                            } else {
                                webPreview.onSetGlobal(choice)
                            }
                        },
                    )
                }
            }

            SettingsCard(
                title = strings.text("ui.webEngine"),
                description = strings.text("ui.theChromiumEngineBehindJCodeSBuiltInBrowser"),
                keywords = "web engine webview chromium version provider outdated update play store " +
                    "browser render blank dvh modern developer options implementation",
            ) {
                val ctx = LocalContext.current
                // Re-read on each composition of the card: the user may return from Play or the
                // provider picker with the engine changed, and a stale number here would claim the
                // trip changed nothing.
                val enginePackage = remember { runCatching { WebView.getCurrentWebViewPackage() }.getOrNull() }
                val engineVersion = enginePackage?.versionName ?: "unknown"
                val engineMajor = engineVersion.substringBefore('.').toIntOrNull() ?: 0
                // Chromium 108 shipped dynamic viewport units (dvh) — the line below which modern
                // sites visibly break. A margin above it counts as "current enough".
                val outdated = engineMajor in 1 until WEBVIEW_MODERN_MAJOR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Chromium $engineVersion",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (outdated) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                text = strings.text("ui.outdated"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = Space.ms, vertical = Space.xs),
                            )
                        }
                    } else if (engineMajor > 0) {
                        Text(
                            text = strings.text("ui.current"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                enginePackage?.packageName?.let { SummaryRow(label = strings.text("ui.provider"), value = it) }
                if (outdated) {
                    Text(
                        text = strings.text("ui.modernSitesCanRenderBlankOrBrokenOnThis"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                CompactFilledButton(
                    text = strings.text("ui.getLatestWebView"),
                    onClick = {
                        val play = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=$GOOGLE_WEBVIEW_PACKAGE"),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { ctx.startActivity(play) }.onFailure {
                            runCatching {
                                ctx.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=$GOOGLE_WEBVIEW_PACKAGE"),
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        }
                    },
                )
                CompactOutlinedButton(
                    text = strings.text("ui.chooseProvider"),
                    onClick = {
                        runCatching {
                            ctx.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    },
                )
                }
            }
            } // end Web preview

            } // end web-preview (hidden on the ENV VAR tab)

            if (showGlobalTab) {
            SettingsGroup("ui.environment") {
            SettingsCard(
                title = strings.text("ui.environment"),
                description = strings.text("ui.environmentSetupProotDistroBootstrapAndTheFinalSmoke"),
                keywords = "environment proot distro toolchain smoke test bind runtime setup manage refresh install " +
                    "ready passed failed not installed not run unknown update upgrade packages apt system " +
                    environmentState.runtime.selectedDistro.label,
            ) {
                SummaryRow(
                    label = "proot",
                    value = if (environmentState.prootInstalled) strings.text("option.Ready") else strings.text("option.Notinstalled"),
                )
                SummaryRow(
                    label = strings.text("ui.distro"),
                    value = when (environmentState.distroInstalled) {
                        true -> environmentState.runtime.selectedDistro.label
                        false -> strings.text("option.Notinstalled")
                        null -> strings.text("option.Unknown")
                    },
                )
                SummaryRow(
                    label = strings.text("ui.toolchain"),
                    value = when (environmentState.toolchainReady) {
                        true -> strings.text("option.Ready")
                        false -> strings.text("option.Notready")
                        null -> strings.text("option.Unknown")
                    },
                )
                SummaryRow(
                    label = strings.text("ui.smokeTest"),
                    value = when (environmentState.smokeTestPassed) {
                        true -> strings.text("option.Passed")
                        false -> strings.text("option.Failed")
                        null -> strings.text("option.Notrun")
                    },
                )
                SummaryRow(
                    "Primary bind",
                    environmentState.runtime.binds.firstOrNull()?.target ?: "/workspace",
                )
                environmentState.runningStep?.let { runningStep ->
                    Text(
                        text = strings.text("settings.running", mapOf("step" to (runningStep.key).toString())),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                environmentState.activityLog.takeLast(3).forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    CompactFilledButton(text = strings.text("ui.manageEnvironments"), onClick = onOpenEnvironmentWizard)
                    CompactOutlinedButton(text = strings.text("ui.refreshChecks"), onClick = onRefreshEnvironment)
                }
                LocalEnvironmentBackup.current.migrationSummary?.let { summary ->
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CompactFilledButton(
                        text = strings.text("ui.importFromPreviousInstall"),
                        onClick = LocalEnvironmentBackup.current.onImportMigration,
                    )
                }
                if (environmentState.distroInstalled == true) {
                    val envBackup = LocalEnvironmentBackup.current
                    Text(
                        text = strings.text("ui.backUpTheWholeLinuxEnvironment25GB"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        CompactFilledButton(text = strings.text("ui.backUpTarGz"), onClick = envBackup.onBackup)
                        CompactOutlinedButton(text = strings.text("ui.restoreText8ff034"), onClick = envBackup.onRestore)
                    }
                    // Moving to an install with a different package name. Android gives that install
                    // its own data directory and no way to read this one's, so everything has to go
                    // out through shared storage first — see MigrationBundle.
                    Text(
                        text = strings.text("ui.movingToADifferentlyNamedBuildWriteTheEnvironment"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CompactOutlinedButton(
                        text = strings.text("ui.exportForMigration"),
                        onClick = envBackup.onExportMigration,
                    )
                    Text(
                        text = strings.text("ui.refreshPackageListsAndUpgradeInstalledPackagesAptGet"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CompactOutlinedButton(
                        text = if (envBackup.updatingPackages) strings.text("ui.updatingPackages") else strings.text("ui.updateSystemPackages"),
                        onClick = envBackup.onUpdatePackages,
                        enabled = !envBackup.updatingPackages,
                    )
                }
            }

            SettingsCard(
                title = strings.text("ui.backgroundProcessLimit"),
                description = strings.text("ui.androidCapsHowManyProcessesAnAppMayFork"),
                keywords = "phantom process limit killed died crashed dies terminal closes distro proot stopped " +
                    "background max_phantom_processes device_config adb activity manager trimming long session " +
                    "claude agent build gradle npm disappears exits by itself",
            ) {
                val clipboard = LocalClipboardManager.current
                var processCount by remember { mutableStateOf<Int?>(null) }
                // Only polls while this card is actually on screen (the group is collapsed by default).
                LaunchedEffect(Unit) {
                    while (true) {
                        processCount = withContext(Dispatchers.IO) { AppProcesses.count() }
                        delay(3_000L)
                    }
                }
                SummaryRow(
                    label = strings.text("ui.linuxProcesses"),
                    value = processCount?.let { "$it of ${AppProcesses.DEFAULT_PHANTOM_LIMIT} (default cap)" }
                        ?: "Unknown",
                )
                Text(
                    text = strings.text("ui.android12KillsAnAppSForkedProcessesOnce"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = AppProcesses.RAISE_LIMIT_COMMANDS,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                CompactOutlinedButton(
                    text = strings.text("ui.copyCommands"),
                    onClick = { clipboard.setText(AnnotatedString(AppProcesses.RAISE_LIMIT_COMMANDS)) },
                )
            }

            SettingsCard(
                title = strings.text("ui.androidDevice"),
                description = strings.text("ui.pairJCodeWithThisPhoneSOwnAdbSo"),
                keywords = "android device adb bridge wireless debugging pair pairing code relay serial apk " +
                    "install launch logcat gradle installdebug flutter run",
            ) {
                val androidDevice = LocalAndroidDevice.current
                SummaryRow(label = "ADB bridge", value = androidDevice.status)
                androidDevice.serial?.let { SummaryRow(label = strings.text("ui.serial"), value = it) }
                CompactFilledButton(
                    text = if (androidDevice.ready) strings.text("ui.manageDevice") else strings.text("ui.setUpADB"),
                    onClick = androidDevice.onOpenPage,
                )
            }


            } // end Environment

            SettingsGroup("option.About") {
            SettingsCard(
                title = "JCode",
                description = strings.text("ui.appVersionAndUpdatesFromGitHubReleases"),
                keywords = "about version update check release github changelog build app",
            ) {
                val appUpdate = LocalAppUpdate.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = strings.text("common.version", mapOf("version" to (appUpdate.currentVersion).toString())),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (appUpdate.updateAvailable) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                text = strings.text("settings.updateVersion", mapOf("version" to (appUpdate.latestVersion ?: "").toString())),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = Space.ms, vertical = Space.xs),
                            )
                        }
                    } else if (appUpdate.latestVersion != null) {
                        Text(
                            text = strings.text("ui.upToDate"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // A single button: "Install Update" when a newer release is available (its label shows
                // download/install progress while running), otherwise "Check for updates".
                if (appUpdate.updateAvailable) {
                    CompactFilledButton(
                        text = when {
                            !appUpdate.installing -> strings.text("ui.installUpdate")
                            appUpdate.installProgress in 1..99 -> strings.text("settings.downloading", mapOf("percent" to (appUpdate.installProgress).toString()))
                            else -> strings.text("ui.installing")
                        },
                        onClick = appUpdate.onInstallUpdate,
                        enabled = !appUpdate.installing,
                    )
                } else {
                    CompactOutlinedButton(
                        text = if (appUpdate.checking) strings.text("ui.checking") else strings.text("ui.checkForUpdates"),
                        onClick = appUpdate.onCheck,
                        enabled = !appUpdate.checking,
                    )
                }
            }

            SettingsCard(
                title = strings.text("ui.backupRestore"),
                description = strings.text("ui.saveYourAppPreferencesToAFileThenRestore"),
                keywords = "backup restore export import settings preferences file save load transfer migrate json device",
            ) {
                val backup = LocalSettingsBackup.current
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    CompactFilledButton(text = strings.text("ui.exportSettings"), onClick = backup.onExport)
                    CompactOutlinedButton(text = strings.text("ui.importSettings"), onClick = backup.onImport)
                }
            }

            } // end About

            SettingsGroup("option.Diagnostics") {
            SettingsCard(
                title = strings.text("ui.diagnosticLogging"),
                description = strings.text("ui.offUnlessYouTurnItOnWhenSomethingMisbehaves"),
                keywords = "diagnostic diagnostics log logging logcat debug trace record capture crash report " +
                    "bug issue troubleshoot export share verbose file",
            ) {
                val diagnostics = LocalDiagnosticsSetting.current
                var showLog by remember { mutableStateOf(false) }
                // Size/location only move while recording, and only matter while this card is open.
                LaunchedEffect(diagnostics.enabled) {
                    while (diagnostics.enabled) {
                        diagnostics.onRefresh()
                        delay(2_000L)
                    }
                }
                ToggleRow(
                    label = strings.text("ui.recordDiagnostics"),
                    supporting = strings.text("ui.writesAppEventsToALogFileOnThis"),
                    checked = diagnostics.enabled,
                    onCheckedChange = diagnostics.onSetEnabled,
                    modified = diagnostics.enabled != SettingsDefaults.DIAGNOSTIC_LOGGING,
                    onReset = { diagnostics.onSetEnabled(SettingsDefaults.DIAGNOSTIC_LOGGING) },
                )
                if (diagnostics.enabled) {
                    SettingsDropdownRow(
                        label = strings.text("ui.detail"),
                        options = DiagLevel.entries.map { it.name },
                        selected = diagnostics.level.name,
                        onSelect = { diagnostics.onSetLevel(DiagLevel.valueOf(it)) },
                        optionLabel = { DiagLevel.valueOf(it).label },
                        modified = diagnostics.level != SettingsDefaults.DIAGNOSTIC_LEVEL,
                        onReset = { diagnostics.onSetLevel(SettingsDefaults.DIAGNOSTIC_LEVEL) },
                    )
                    ToggleRow(
                        label = strings.text("ui.includeTheSystemLog"),
                        supporting = strings.text("ui.addsJCodeSLogcatOutputIncludingProotAndThe"),
                        checked = diagnostics.captureSystemLog,
                        onCheckedChange = diagnostics.onSetCaptureSystemLog,
                        modified = diagnostics.captureSystemLog != SettingsDefaults.DIAGNOSTIC_SYSTEM_LOG,
                        onReset = { diagnostics.onSetCaptureSystemLog(SettingsDefaults.DIAGNOSTIC_SYSTEM_LOG) },
                    )
                    ToggleRow(
                        label = strings.text("ui.recordCrashes"),
                        supporting = strings.text("ui.appendTheStackTraceWhenTheAppCrashesSo"),
                        checked = diagnostics.captureCrashes,
                        onCheckedChange = diagnostics.onSetCaptureCrashes,
                        modified = diagnostics.captureCrashes != SettingsDefaults.DIAGNOSTIC_CRASHES,
                        onReset = { diagnostics.onSetCaptureCrashes(SettingsDefaults.DIAGNOSTIC_CRASHES) },
                    )
                    SummaryRow(label = strings.text("ui.recorded"), value = formatLogSize(diagnostics.sizeBytes))
                    SummaryRow(label = strings.text("ui.location"), value = diagnostics.location.ifBlank { "Starting…" })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    CompactFilledButton(
                        text = strings.text("ui.view"),
                        onClick = { showLog = true },
                        enabled = diagnostics.sizeBytes > 0L,
                    )
                    CompactOutlinedButton(
                        text = strings.text("ui.export"),
                        onClick = diagnostics.onExport,
                        enabled = diagnostics.sizeBytes > 0L,
                    )
                    CompactOutlinedButton(
                        text = strings.text("ui.clear"),
                        onClick = diagnostics.onClear,
                        enabled = diagnostics.sizeBytes > 0L,
                    )
                }
                if (showLog) {
                    DiagnosticLogDialog(lines = diagnostics.recentLines(), onDismiss = { showLog = false })
                }
            }

            } // end Diagnostics

            SettingsGroup("option.Editor") {
            SettingsCard(
                title = strings.text("ui.editorDefaults"),
                description = strings.text("ui.defaultFontSizeAndWordWrapForTheCode"),
                keywords = "editor font size text scale sp word wrap soft wrap line long lines default global",
            ) {
                val editorFontSizeSetting = LocalEditorFontSizeSetting.current
                val editorWordWrapSetting = LocalEditorWordWrapSetting.current
                StepperRow(
                    label = strings.text("ui.fontSize"),
                    value = "${editorFontSizeSetting.value.toInt()} sp",
                    onDecrease = { editorFontSizeSetting.onChange((editorFontSizeSetting.value - 1f).coerceAtLeast(8f)) },
                    onIncrease = { editorFontSizeSetting.onChange((editorFontSizeSetting.value + 1f).coerceAtMost(72f)) },
                    modified = editorFontSizeSetting.value != SettingsDefaults.EDITOR_FONT_SIZE,
                    onReset = { editorFontSizeSetting.onChange(SettingsDefaults.EDITOR_FONT_SIZE) },
                )
                ToggleRow(
                    label = strings.text("ui.wordWrap"),
                    supporting = strings.text("ui.wrapLongLinesToTheEditorWidthInsteadOf"),
                    checked = editorWordWrapSetting.enabled,
                    onCheckedChange = { editorWordWrapSetting.onChange(it) },
                    modified = editorWordWrapSetting.enabled != SettingsDefaults.EDITOR_WORD_WRAP,
                    onReset = { editorWordWrapSetting.onChange(SettingsDefaults.EDITOR_WORD_WRAP) },
                )
            }
            SettingsCard(
                title = strings.text("ui.editorGestures"),
                description = strings.text("ui.howTouchInputBehavesInTheEditorAppliesApp"),
                keywords = "editor gestures drag move cursor speed vertical horizontal touch scroll",
            ) {
                ToggleRow(
                    label = strings.text("ui.dragToMoveCursor"),
                    supporting = strings.text("ui.dragAFingerOnTheEditorToMoveThe"),
                    checked = editorDragSetting.enabled,
                    onCheckedChange = editorDragSetting.onChange,
                    modified = editorDragSetting.enabled != SettingsDefaults.EDITOR_DRAG_MOVES_CURSOR,
                    onReset = { editorDragSetting.onChange(SettingsDefaults.EDITOR_DRAG_MOVES_CURSOR) },
                )
                if (editorDragSetting.enabled) {
                    StepperRow(
                        label = strings.text("ui.cursorDragSpeedVertical"),
                        value = "${editorDragSetting.verticalLevel} / 5",
                        onDecrease = { editorDragSetting.onVerticalLevelChange((editorDragSetting.verticalLevel - 1).coerceAtLeast(1)) },
                        onIncrease = { editorDragSetting.onVerticalLevelChange((editorDragSetting.verticalLevel + 1).coerceAtMost(5)) },
                        modified = editorDragSetting.verticalLevel != SettingsDefaults.CURSOR_DRAG_LEVEL,
                        onReset = { editorDragSetting.onVerticalLevelChange(SettingsDefaults.CURSOR_DRAG_LEVEL) },
                    )
                    StepperRow(
                        label = strings.text("ui.cursorDragSpeedHorizontal"),
                        value = "${editorDragSetting.horizontalLevel} / 5",
                        onDecrease = { editorDragSetting.onHorizontalLevelChange((editorDragSetting.horizontalLevel - 1).coerceAtLeast(1)) },
                        onIncrease = { editorDragSetting.onHorizontalLevelChange((editorDragSetting.horizontalLevel + 1).coerceAtMost(5)) },
                        modified = editorDragSetting.horizontalLevel != SettingsDefaults.CURSOR_DRAG_LEVEL,
                        onReset = { editorDragSetting.onHorizontalLevelChange(SettingsDefaults.CURSOR_DRAG_LEVEL) },
                    )
                }
            }

            SettingsCard(
                title = strings.text("ui.tabs"),
                description = strings.text("ui.howEditorAndTerminalTabsBehaveAppliesAppWide"),
                keywords = "tabs tab close button hide editor terminal accidental coloring color accent random directory width size small medium large shorten ellipsis truncate",
            ) {
                ToggleRow(
                    label = strings.text("ui.hideTabCloseButton"),
                    supporting = strings.text("ui.removesTheOnEditorAndTerminalTabsToAvoid"),
                    checked = tabCloseSetting.hidden,
                    onCheckedChange = tabCloseSetting.onChange,
                    modified = tabCloseSetting.hidden != SettingsDefaults.HIDE_TAB_CLOSE_BUTTON,
                    onReset = { tabCloseSetting.onChange(SettingsDefaults.HIDE_TAB_CLOSE_BUTTON) },
                )
                SettingsDropdownRow(
                    label = strings.text("ui.tabWidth"),
                    supporting = strings.text("ui.theMostAnEditorOrTerminalTabWidensBefore"),
                    options = TabMaxSize.entries.map { it.name },
                    selected = tabMaxSizeSetting.size.name,
                    onSelect = { tabMaxSizeSetting.onChange(TabMaxSize.valueOf(it)) },
                    modified = tabMaxSizeSetting.size != SettingsDefaults.TAB_MAX_SIZE,
                    onReset = { tabMaxSizeSetting.onChange(SettingsDefaults.TAB_MAX_SIZE) },
                )
                SettingsDropdownRow(
                    label = strings.text("ui.tabColoring"),
                    supporting = strings.text("ui.colorCodeEditorFileTabsLongPressAFile"),
                    options = TabColoring.entries.map { it.name },
                    selected = tabColoringSetting.mode.name,
                    onSelect = { tabColoringSetting.onChange(TabColoring.valueOf(it)) },
                    optionLabel = { tabColoringLabel(strings, TabColoring.valueOf(it)) },
                    modified = tabColoringSetting.mode != SettingsDefaults.TAB_COLORING,
                    onReset = { tabColoringSetting.onChange(SettingsDefaults.TAB_COLORING) },
                )
            }

            SettingsCard(
                title = strings.text("ui.formatter"),
                description = strings.text("ui.whichFormatterTheEditorUsesBuiltInIsRule"),
                keywords = "formatter format prettier indent on-save whitespace built-in " +
                    formatterOptions.joinToString(" ") { it.second },
            ) {
                formatterOptions.forEach { (id, label) ->
                    BundleRow(
                        name = label,
                        description = if (id == "builtin") strings.text("ui.builtInRuleBasedFormatter") else strings.text("ui.formatterExtension"),
                        selected = formatterId == id,
                        swatch = emptyList(),
                        onClick = { onSelectFormatter(id) },
                    )
                }
            }

            SettingsCard(
                title = strings.text("ui.markdownPreview"),
                description = strings.text("ui.howTheRenderedMarkdownPreviewLaysOut"),
                keywords = "markdown preview word wrap portrait landscape width horizontal scroll pan wide tables code",
            ) {
                val markdownPreviewSetting = LocalMarkdownPreviewSetting.current
                ToggleRow(
                    label = strings.text("ui.wordWrapInPortrait"),
                    supporting = strings.text("ui.offAPortraitPreviewLaysOutAtLandscapeWidth"),
                    checked = markdownPreviewSetting.wrapInPortrait,
                    onCheckedChange = { markdownPreviewSetting.onSetWrapInPortrait(it) },
                    modified = markdownPreviewSetting.wrapInPortrait != SettingsDefaults.MARKDOWN_WRAP_PORTRAIT,
                    onReset = { markdownPreviewSetting.onSetWrapInPortrait(SettingsDefaults.MARKDOWN_WRAP_PORTRAIT) },
                )
            }

            } // end Editor

            SettingsGroup("ui.explorer") {
            SettingsCard(
                title = strings.text("ui.excludeFilesFolders"),
                description = strings.text("ui.excludeFilesAndFoldersAtTheProjectRootIn"),
                keywords = "explorer files folder exclude hide hidden grey greyed grey-out dim de-emphasize project root gitignore jcode ignore injected specified show reveal by-line effect",
            ) {
                SettingsDropdownRow(
                    label = strings.text("ui.mode"),
                    options = ExplorerHiddenMode.entries.map { it.name },
                    selected = explorerHiddenSetting.mode.name,
                    onSelect = { explorerHiddenSetting.onSetMode(ExplorerHiddenMode.valueOf(it)) },
                    optionLabel = { explorerHiddenModeLabel(strings, ExplorerHiddenMode.valueOf(it)) },
                    modified = explorerHiddenSetting.mode != SettingsDefaults.HIDDEN_ROOT_MODE,
                    onReset = { explorerHiddenSetting.onSetMode(SettingsDefaults.HIDDEN_ROOT_MODE) },
                )
                SettingsDropdownRow(
                    label = strings.text("ui.whenExcluded"),
                    options = ExplorerExcludeEffect.entries.map { it.name },
                    selected = explorerHiddenSetting.effect.name,
                    onSelect = { explorerHiddenSetting.onSetEffect(ExplorerExcludeEffect.valueOf(it)) },
                    optionLabel = { explorerExcludeEffectLabel(strings, ExplorerExcludeEffect.valueOf(it)) },
                    modified = explorerHiddenSetting.effect != SettingsDefaults.EXCLUDE_EFFECT,
                    onReset = { explorerHiddenSetting.onSetEffect(SettingsDefaults.EXCLUDE_EFFECT) },
                )
                var hidePatterns by remember(explorerHiddenSetting.specifiedRaw) {
                    mutableStateOf(explorerHiddenSetting.specifiedRaw)
                }
                SettingsTextFieldRow(
                    label = strings.text("ui.specifiedOnePatternPerLine"),
                    value = hidePatterns,
                    onValueChange = { hidePatterns = it },
                    onCommit = { explorerHiddenSetting.onSetSpecifiedRaw(hidePatterns) },
                    placeholder = ".jcode",
                    singleLine = false,
                    minLines = 3,
                )
            }
            SettingsCard(
                title = strings.text("ui.trash"),
                description = strings.text("ui.whereDeletedFilesGoBeforeTheyAreGoneCovers"),
                keywords = "trash bin recycle delete deleted remove restore recover undelete discard scm source control retention keep days empty permanently",
            ) {
                ToggleRow(
                    label = strings.text("ui.moveDeletedFilesToTrash"),
                    supporting = strings.text("ui.deletingAFileOrFolderOrDiscardingAChange"),
                    checked = trashSettings.enabled,
                    onCheckedChange = trashSettings.onSetEnabled,
                    modified = trashSettings.enabled != SettingsDefaults.TRASH_ENABLED,
                    onReset = { trashSettings.onSetEnabled(SettingsDefaults.TRASH_ENABLED) },
                )
                if (trashSettings.enabled) {
                    SettingsDropdownRow(
                        label = strings.text("ui.keepDeletedFilesFor"),
                        supporting = strings.text("ui.olderItemsAreRemovedWhenJCodeStartsAndWhen"),
                        options = TRASH_RETENTION_CHOICES.map { it.toString() },
                        selected = trashSettings.retentionDays.toString(),
                        onSelect = { trashSettings.onSetRetentionDays(it.toInt()) },
                        optionLabel = { trashRetentionLabel(it.toInt()) },
                        modified = trashSettings.retentionDays != SettingsDefaults.TRASH_RETENTION_DAYS,
                        onReset = { trashSettings.onSetRetentionDays(SettingsDefaults.TRASH_RETENTION_DAYS) },
                    )
                }
            }

            } // end Explorer

            SettingsGroup("option.Developer") {
            SettingsCard(
                title = strings.text("ui.developerOptions"),
                description = strings.text("ui.toolsForBuildingAndTestingJCodeExtensions"),
                keywords = "developer options extension sideload unsigned jext debug dev tools inspector validator log console reload make tool third party",
            ) {
                val developerSetting = LocalDeveloperSetting.current
                ToggleRow(
                    label = strings.text("ui.enableDeveloperOptions"),
                    supporting = strings.text("ui.addsAnExtDevTabToTheRightPanel"),
                    checked = developerSetting.enabled,
                    onCheckedChange = { developerSetting.onSetEnabled(it) },
                    modified = developerSetting.enabled != SettingsDefaults.DEVELOPER_OPTIONS,
                    onReset = { developerSetting.onSetEnabled(SettingsDefaults.DEVELOPER_OPTIONS) },
                )
                if (developerSetting.enabled) {
                    Text(
                        "Compile and pack your extension with the JCode extension make tool, then import the " +
                            "unsigned .jext from the Extensions panel — the Ext Dev tab auto-reloads it on each " +
                            "rebuild. Only signed packages (signed privately by the JCode maintainers) reach the " +
                            "marketplace.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            } // end Developer

            } // end Global tab

            if (showScopedTab) {
            // The active tab already names the scope; this caption just states its reach.
            if (query.isBlank()) {
                Text(
                    text = when (selectedScope) {
                        ConfigScope.Workspace -> strings.text("ui.theseSettingsPreserveTheSharedFolderJcodeConfigurationAnd")
                        ConfigScope.Project -> strings.text("ui.theseSettingsSaveToTheProjectJcodeAndOnly")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Space.xxs),
                )
            }

            workspaceError?.let { message ->
                WarningCard(title = strings.text("ui.sharedFolderYAMLWarning"), message = message)
            }

            if (projectOverridesAvailable) {
                projectError?.let { message ->
                    WarningCard(title = strings.text("ui.projectYAMLWarning"), message = message)
                }
            }

            environmentState.errorMessage?.let { message ->
                WarningCard(title = strings.text("ui.environmentWarning"), message = message)
            }

            SettingsGroup("option.Editor", stateKey = "scoped.Editor") {
            SettingsCard(
                title = strings.text("ui.editorBehavior"),
                description = strings.text("ui.theseControlsWriteBackToYAMLAndUpdateThe"),
                keywords = "editor behavior font size tab size ligatures indent tab coloring color accent",
            ) {
                StepperRow(
                    label = strings.text("ui.fontSize"),
                    value = "${fontSize.toInt()} sp",
                    onDecrease = { onUpdateFontSize(selectedScope, (fontSize - 1f).coerceAtLeast(8f)) },
                    onIncrease = { onUpdateFontSize(selectedScope, (fontSize + 1f).coerceAtMost(72f)) },
                    modified = scopedEditor?.fontSize != null,
                    onReset = { onUpdateFontSize(selectedScope, null) },
                )
                SettingsDropdownRow(
                    label = strings.text("ui.tabSize"),
                    supporting = strings.text("ui.goodDefaultsAre24Or8SpacesDepending"),
                    options = listOf("2", "4", "8"),
                    selected = tabSize.toString(),
                    onSelect = { onUpdateTabSize(selectedScope, it.toInt()) },
                    optionLabel = { "$it spaces" },
                    modified = scopedEditor?.tabSize != null,
                    onReset = { onUpdateTabSize(selectedScope, null) },
                )
                ToggleRow(
                    label = strings.text("ui.ligatures"),
                    supporting = strings.text("ui.keepEnabledForTheEditorSurfaceButLetUsers"),
                    checked = ligatures,
                    onCheckedChange = { onUpdateLigatures(selectedScope, it) },
                    modified = scopedEditor?.ligatures != null,
                    onReset = { onUpdateLigatures(selectedScope, null) },
                )
                // Sanitize: a hand-edited .jcode may hold an unknown enum name; fall back to the
                // app default rather than crashing composition on TabColoring.valueOf.
                val tabColoring = (scopedEditor?.tabColoring ?: effectiveConfig.editor.tabColoring)
                    ?.let { runCatching { TabColoring.valueOf(it) }.getOrNull() }
                    ?.name
                    ?: tabColoringSetting.mode.name
                SettingsDropdownRow(
                    label = strings.text("ui.tabColoring"),
                    supporting = strings.text("ui.overridesTheAppLevelDefaultForThisScope"),
                    options = TabColoring.entries.map { it.name },
                    selected = tabColoring,
                    onSelect = { onUpdateTabColoring(selectedScope, it) },
                    optionLabel = { runCatching { tabColoringLabel(strings, TabColoring.valueOf(it)) }.getOrDefault(it) },
                    modified = scopedEditor?.tabColoring != null,
                    onReset = { onUpdateTabColoring(selectedScope, null) },
                )
            }

            SettingsCard(
                title = strings.text("ui.explorer"),
                description = strings.text("ui.chooseHowTheFileExplorerIsLaidOutApplies"),
                keywords = "explorer view mode tree list file manager layout breadcrumbs",
            ) {
                OptionRow(
                    label = strings.text("ui.viewMode"),
                    supporting = strings.text("ui.treeShowsTheWholeProjectHierarchyListIsA"),
                    modified = scopedExplorer?.viewMode != null,
                    onReset = { onUpdateExplorerViewMode(selectedScope, null) },
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        listOf("Tree", "List").forEach { option ->
                            val selected = explorerViewMode == option
                            if (selected) {
                                CompactFilledButton(
                                    text = strings.text(if(option=="Tree") "option.Tree" else "option.List"),
                                    onClick = { onUpdateExplorerViewMode(selectedScope, option) },
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                CompactOutlinedButton(
                                    text = strings.text(if(option=="Tree") "option.Tree" else "option.List"),
                                    onClick = { onUpdateExplorerViewMode(selectedScope, option) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            } // end Editor (scoped)

            SettingsGroup("nav.files") {
            SettingsCard(
                title = strings.text("ui.yAMLFiles"),
                description = strings.text("ui.openTheBackingConfigFilesDirectlyWhenYouWant"),
                keywords = "yaml files config workspace project open backing edit",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    CompactFilledButton(text = strings.text("ui.openSharedFolderYAMLAdvanced"), onClick = onOpenWorkspaceConfig)
                    CompactOutlinedButton(
                        text = strings.text("ui.openProjectYAMLText26247e"),
                        onClick = onOpenProjectConfig,
                        enabled = projectOverridesAvailable,
                    )
                }
            }
            } // end Files

            } // end Project/Workspace tab

            // Composed after every card, so matchSink.count reflects the whole page.
            if (!isEnvVarTab && query.isNotBlank() && matchSink.count == 0) {
                SettingsNoResults(query)
            }
        }
        }
    }
}

/** Human-readable labels for the exclude "Mode" dropdown — WHICH entries are excluded. */
/**
 * The floor below which the device's Chromium counts as outdated in the Web engine card.
 *
 * Chromium 108 shipped dynamic viewport units (`dvh`), the first modern-CSS line whose absence
 * makes whole sites render blank rather than merely imperfect; a small margin above it counts as
 * current enough. Deliberately far below the actual current release: the card exists to flag
 * engines that *break* pages, not to nag every device that trails by a few versions.
 */
private const val WEBVIEW_MODERN_MAJOR = 110

/** Google's updatable WebView provider on Play — the install target for outdated engines. */
private const val GOOGLE_WEBVIEW_PACKAGE = "com.google.android.webview"

private fun explorerHiddenModeLabel(strings: dev.suicode.packages.UiTranslations, mode: ExplorerHiddenMode): String = when (mode) {
    ExplorerHiddenMode.HideSpecifiedAndInjected -> strings.text("option.SpecifiedByInjected")
    ExplorerHiddenMode.HideInjected -> strings.text("option.ByInjectedonly")
    ExplorerHiddenMode.None -> strings.text("option.Off")
}

/** Human-readable labels for the "When excluded" dropdown — HOW excluded entries appear. */
private fun explorerExcludeEffectLabel(strings: dev.suicode.packages.UiTranslations, effect: ExplorerExcludeEffect): String = when (effect) {
    ExplorerExcludeEffect.GreyOut -> strings.text("option.Greyout")
    ExplorerExcludeEffect.Hide -> strings.text("option.Hide")
}

/** Human-readable label for an [ExtraKeysVisibility] dropdown option. */
private fun extraKeysVisibilityLabel(strings: dev.suicode.packages.UiTranslations, mode: ExtraKeysVisibility): String = when (mode) {
    ExtraKeysVisibility.Hidden -> strings.text("option.Hidden")
    ExtraKeysVisibility.WithKeyboard -> strings.text("option.Withkeyboard")
    ExtraKeysVisibility.Always -> strings.text("option.Always")
}

/** Human-readable label for a [BottomBarVisibility] dropdown option. */
private fun bottomBarVisibilityLabel(strings: dev.suicode.packages.UiTranslations, mode: BottomBarVisibility): String = when (mode) {
    BottomBarVisibility.Hidden -> strings.text("option.Hidden")
    BottomBarVisibility.HideOnKeyboard -> strings.text("option.HideonSoftKeyboard")
    BottomBarVisibility.AlwaysShow -> strings.text("option.AlwaysShow")
}

private fun headerActionButtonLabel(strings: dev.suicode.packages.UiTranslations, button: HeaderActionButton): String = when (button) {
    HeaderActionButton.Terminal -> strings.text("ui.terminal")
    HeaderActionButton.CommandPalette -> strings.text("ui.commandPalette")
    HeaderActionButton.Hidden -> strings.text("option.Hidden")
}

private fun tabColoringLabel(strings: dev.suicode.packages.UiTranslations, mode: TabColoring): String = when (mode) {
    TabColoring.RandomRemember -> strings.text("option.Randomifnotexistthenremember")
    TabColoring.Random -> strings.text("option.Random")
    TabColoring.DirectoryBased -> strings.text("option.Directorybasedthenremember")
    TabColoring.Disabled -> strings.text("option.Disabled")
}

/** [defaultSuffix] disambiguates the per-button System Default label, e.g. "System Default (Vol Up)". */
private fun volumeKeyActionLabel(strings: dev.suicode.packages.UiTranslations, action: VolumeKeyAction, defaultSuffix: String): String = when (action) {
    VolumeKeyAction.SystemDefault -> strings.text("settings.systemVolume", mapOf("key" to (defaultSuffix).toString()))
    VolumeKeyAction.Undo -> strings.text("ui.undo")
    VolumeKeyAction.Redo -> strings.text("ui.redo")
    VolumeKeyAction.KeyLeft -> strings.text("option.KeyLeft")
    VolumeKeyAction.KeyRight -> strings.text("option.KeyRight")
    VolumeKeyAction.KeyUp -> strings.text("option.KeyUp")
    VolumeKeyAction.KeyDown -> strings.text("option.KeyDown")
    VolumeKeyAction.ScrollUp -> strings.text("option.ScrollUp")
    VolumeKeyAction.ScrollDown -> strings.text("option.ScrollDown")
    VolumeKeyAction.CommandPalette -> strings.text("ui.commandPalette")
}

/** Current Settings search query; cards/headers self-filter on it. */
val LocalSettingsQuery = compositionLocalOf { "" }

/** Counts how many cards passed the search filter this composition, so a no-match query can show an
 *  empty state. A fresh instance is provided each composition (see Content), so every card recomposes
 *  and re-counts on any page change — the count read by the trailing empty-state is always accurate. */
private class SettingsMatchSink { var count = 0 }
private val LocalSettingsMatchSink = compositionLocalOf { SettingsMatchSink() }

/** True when EVERY whitespace-separated term in [query] appears (case-insensitive) somewhere in the
 *  card's searchable text ([haystacks] = title + description + keywords). Term-wise AND matching lets
 *  "tab close" find the Tabs card, where a single-substring match would not. */
private fun matchesSettingsQuery(query: String, vararg haystacks: String): Boolean {
    val terms = query.split(' ', '\t', '\n', '-').filter { it.isNotBlank() }
    if (terms.isEmpty()) return true
    val hay = haystacks.joinToString(" ").lowercase()
    return terms.all { hay.contains(it.lowercase()) }
}

/** Compact, single-line search field (smaller than a default OutlinedTextField). */
@Composable
private fun SettingsSearchField(query: String, onQueryChange: (String) -> Unit) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.xl),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .padding(horizontal = Space.ms),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(IconSize.md),
            )
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = strings.text("ui.searchSettings"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = strings.text("ui.clearSearch"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onQueryChange("") },
                )
            }
        }
    }
}

/** Shown when a search query matches no card, so an empty page reads as "no results" not "broken". */
@Composable
private fun SettingsNoResults(query: String) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Text(
            text = strings.text("settings.noMatches", mapOf("query" to (query).toString())),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = strings.text("ui.tryAShorterOrDifferentTermLikeFontOr"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Which settings groups are open, held outside the composition.
 *
 * `rememberSaveable` is not enough here: rotating the device makes the workbench swap between its
 * modal and docked layouts, which disposes this whole subtree along with its saveable registry, so
 * every group would snap shut on rotation. Session-scoped by design — a fresh launch starts
 * collapsed. Each group owns its own [MutableState] so toggling one doesn't invalidate the rest.
 */
private val settingsGroupExpanded = mutableMapOf<String, MutableState<Boolean>>()

/** Each group's y offset inside the scrolling column, published as it is laid out, so
 *  [SettingsFeature.revealGroup] can scroll to one. */
private val settingsGroupOffsets = mutableMapOf<String, Float>()

/**
 * A run of [SettingsCard]s under one heading, collapsed by default so the page opens as a short list
 * of headings instead of one long scroll.
 *
 * While a search is running the heading and the collapse are bypassed entirely and [content] is
 * emitted straight into the caller's Column — not merely un-collapsed. Cards filter themselves and
 * count themselves into [LocalSettingsMatchSink], so wrapping them at all would both hide matches
 * inside collapsed groups and, for a group whose cards all filtered out, leave an empty child behind
 * that the parent's `spacedBy` would still pad around.
 *
 * [stateKey] separates groups that share a title — "Editor" is a heading on both the global and the
 * scoped tab.
 */
@Composable
private fun ColumnScope.SettingsGroup(
    title: String,
    stateKey: String = title,
    content: @Composable () -> Unit,
) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current
    val displayTitle = strings.text(title)
    if (LocalSettingsQuery.current.isNotBlank()) {
        content()
        return
    }
    var expanded by remember(stateKey) { settingsGroupExpanded.getOrPut(stateKey) { mutableStateOf(false) } }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { settingsGroupOffsets[stateKey] = it.positionInParent().y }
            .clickable { expanded = !expanded }
            .padding(top = Space.s, start = Space.xxs, end = Space.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = displayTitle,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = jcIcon(if (expanded) JCodeIcon.ChevronUp else JCodeIcon.ChevronDown),
            contentDescription = if (expanded) strings.text("settings.collapse", mapOf("title" to (displayTitle).toString())) else strings.text("settings.expand", mapOf("title" to (displayTitle).toString())),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(IconSize.md),
        )
    }
    // AnimatedVisibility stacks its children like a Box, so the cards need their own Column to keep
    // the page's 10dp rhythm instead of drawing on top of each other.
    AnimatedVisibility(visible = expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.ms)) { content() }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    keywords: String = "",
    content: @Composable () -> Unit,
) {
    val query = LocalSettingsQuery.current.trim()
    if (query.isNotEmpty() && !matchesSettingsQuery(query, title, description, keywords)) return
    LocalSettingsMatchSink.current.count++
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
        shape = RoundedCornerShape(Radius.xxl),
        border = BorderStroke(StrokeWidth.hairline, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier.padding(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.ms),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            content()
        }
    }
}

@Composable
private fun WarningCard(
    title: String,
    message: String,
) {
    // Participate in the search filter/count like SettingsCard, so a warning neither leaks into
    // unrelated results nor sits above a "No results" empty state.
    val query = LocalSettingsQuery.current.trim()
    if (query.isNotEmpty() && !matchesSettingsQuery(query, title, message, "warning error yaml")) return
    LocalSettingsMatchSink.current.count++
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
        shape = RoundedCornerShape(Radius.xxl),
        border = BorderStroke(StrokeWidth.hairline, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun BundleRow(
    name: String,
    description: String,
    selected: Boolean,
    swatch: List<Color>,
    onClick: () -> Unit,
) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .clickable(onClick = onClick)
            .padding(vertical = Space.s, horizontal = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.ms),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xxs)) {
            swatch.take(4).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(color),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = strings.text("ui.selected"),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(IconSize.md),
            )
        }
    }
}

@Composable
private fun UiIconSetRow(
    set: UiIconSet,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    // Five slots a set is most likely to have restyled, so two packs are told apart at a glance
    // rather than by their names.
    val sample = listOf(JCodeIcon.Files, JCodeIcon.Run, JCodeIcon.Terminal, JCodeIcon.Search, JCodeIcon.Settings)
    IconSetRow(
        name = set.name,
        description = set.description.ifBlank { strings.text("settings.icons", mapOf("count" to (set.filledSlots).toString())) },
        selected = selected,
        onClick = onClick,
        preview = {
            sample.forEach { slot ->
                Icon(
                    painter = set.art(slot).painter(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(IconSize.sm),
                )
            }
        },
    )
}

@Composable
private fun FileIconSetRow(
    name: String,
    description: String,
    detail: FileIconSet?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    // Previewed through the same resolver the Explorer uses, on names a pack of any language is
    // likely to answer — so the row shows what the set will actually draw, not a curated sample.
    val sample = listOf("src" to true, "index.ts" to false, "app.py" to false, "README.md" to false)
    IconSetRow(
        name = name,
        description = description.ifBlank { detail?.let { strings.text("settings.icons", mapOf("count" to (it.iconCount).toString())) } ?: "" },
        selected = selected,
        onClick = onClick,
        preview = {
            CompositionLocalProvider(LocalFileIconSet provides detail) {
                sample.forEach { (fileName, isDirectory) ->
                    FileTypeIcon(
                        name = fileName,
                        isDirectory = isDirectory,
                        size = IconSize.sm,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

@Composable
private fun IconSetRow(
    name: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    preview: @Composable () -> Unit,
) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .clickable(onClick = onClick)
            .padding(vertical = Space.s, horizontal = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.ms),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
            verticalAlignment = Alignment.CenterVertically,
            content = { preview() },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = strings.text("ui.selected"),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(IconSize.md),
            )
        }
    }
}

/** Human-readable size for the Diagnostics card's "Recorded" row. */
private fun formatLogSize(bytes: Long): String = when {
    bytes <= 0L -> "Nothing yet"
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    else -> String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0))
}

/**
 * The tail of the current diagnostic session. Shown so a user can see exactly what is being recorded
 * before deciding to share it — opting in should not mean opting in blind.
 */
@Composable
private fun DiagnosticLogDialog(lines: List<String>, onDismiss: () -> Unit) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.text("ui.recentDiagnostics")) },
        text = {
            if (lines.isEmpty()) {
                Text(strings.text("ui.nothingRecordedYet"), style = MaterialTheme.typography.bodySmall)
            } else {
                // Newest last, scrolled to the bottom: the end of the log is what a report is about.
                val scroll = rememberScrollState()
                LaunchedEffect(lines.size) { scroll.scrollTo(scroll.maxValue) }
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(scroll)
                        .horizontalScroll(rememberScrollState()),
                ) {
                    lines.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                        )
                    }
                }
            }
        },
        confirmButton = { CompactFilledButton(text = strings.text("ui.close"), onClick = onDismiss) },
        dismissButton = {
            CompactOutlinedButton(
                text = strings.text("ui.copy"),
                onClick = {
                    clipboard.setText(AnnotatedString(buildString { lines.forEach { appendLine(it) } }))
                },
                enabled = lines.isNotEmpty(),
            )
        },
    )
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun EnvVarEditor(settings: EnvVarSettings) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    // Dialog state: null = closed; [adding] distinguishes a brand-new variable from editing [editTarget].
    var editTarget by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Space.ms)) {
        // A plain heading, not a SettingsGroup: this tab is one section and has no search field.
        Text(
            text = strings.text("ui.environmentVariables"),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = Space.s, start = Space.xxs),
        )
        Text(
            text = strings.text("ui.exportedIntoEveryTerminalAndBuildRunSessionE"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val entries = settings.vars.entries.sortedBy { it.key.lowercase() }
        if (entries.isEmpty()) {
            Text(
                text = strings.text("ui.noVariablesYetTapAddVariableToCreateOne"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Space.sm),
            )
        } else {
            entries.forEach { (name, value) ->
                SettingsResettableRow(modified = false, onReset = null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.sm),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                text = value.ifEmpty { strings.text("ui.empty") },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        CompactOutlinedButton(text = strings.text("ui.edit"), onClick = { editTarget = name; adding = false })
                        CompactOutlinedButton(text = strings.text("ui.delete"), onClick = { settings.onRemove(name) })
                    }
                }
            }
        }
        CompactFilledButton(text = strings.text("ui.addVariable"), onClick = { editTarget = ""; adding = true })
    }

    val target = editTarget
    if (target != null) {
        EnvVarDialog(
            initialName = if (adding) "" else target,
            initialValue = if (adding) "" else (settings.vars[target] ?: ""),
            existingNames = settings.vars.keys,
            editingName = if (adding) null else target,
            onDismiss = { editTarget = null },
            onSave = { name, value ->
                settings.onSet(name, value, if (adding) null else target)
                editTarget = null
            },
        )
    }
}

@Composable
private fun EnvVarDialog(
    initialName: String,
    initialValue: String,
    existingNames: Set<String>,
    editingName: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, value: String) -> Unit,
) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    var name by remember { mutableStateOf(initialName) }
    var value by remember { mutableStateOf(initialValue) }
    val nameValid = name.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))
    val duplicate = name != editingName && name in existingNames
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingName == null) strings.text("ui.addVariable") else strings.text("ui.editVariable")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.trim() },
                    label = { Text(strings.text("ui.name")) },
                    singleLine = true,
                    isError = name.isNotEmpty() && (!nameValid || duplicate),
                    supportingText = {
                        if (duplicate) {
                            Text(strings.text("settings.variableExists", mapOf("name" to (name).toString())))
                        } else if (name.isNotEmpty() && !nameValid) {
                            Text(strings.text("ui.lettersDigitsAndUnderscoreOnlyCanTStartWith"))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(strings.text("ui.value")) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            CompactFilledButton(
                text = strings.text("ui.save"),
                onClick = { onSave(name, value) },
                enabled = nameValid && !duplicate,
            )
        },
        dismissButton = { CompactOutlinedButton(text = strings.text("ui.cancel"), onClick = onDismiss) },
    )
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modified: Boolean = false,
    onReset: (() -> Unit)? = null,
    supporting: String? = null,
) {
    SettingsResettableRow(modified = modified, onReset = onReset) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (supporting != null) {
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Space.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepperButton(JCodeIcon.Minus, "Decrease $label", filled = false, onClick = onDecrease)
                StepperButton(JCodeIcon.Add, "Increase $label", filled = true, onClick = onIncrease)
            }
        }
    }
}

/**
 * One half of a stepper.
 *
 * An icon rather than a typed "-" and "+", which are a hyphen and a plus sign set at text size:
 * different weights, different widths, and sitting on a text baseline inside a button that holds no
 * text. Two glyphs meant to be a matched pair looked like neither.
 *
 * Round, because that is the shape of a button holding one glyph and nothing else: a pill is a shape
 * that expects a word in it, and reads as a button whose label failed to load. The emphasis pairing
 * stays — outlined to step down, tonal to step up, as elsewhere on the page.
 *
 * They are also the only controls on this page a screen reader could not name: "-" reads as a
 * hyphen and says nothing about what it steps. Each now says which setting it moves.
 */
@Composable
private fun StepperButton(
    icon: JCodeIcon,
    contentDescription: String,
    filled: Boolean,
    onClick: () -> Unit,
) {
    // Sized down from the 40dp default, and the interactive minimum relaxed with it. That minimum
    // is there for good reason and is not worth keeping here: it pads each button out to 48dp of
    // layout, which is most of the gap between the two, and a settings row is not a place anyone
    // taps in a hurry.
    val sizing = Modifier.size(34.dp)
    val glyph: @Composable () -> Unit = {
        Icon(jcIcon(icon), contentDescription, modifier = Modifier.size(IconSize.md))
    }
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        if (filled) {
            FilledTonalIconButton(onClick = onClick, modifier = sizing) { glyph() }
        } else {
            OutlinedIconButton(
                onClick = onClick,
                modifier = sizing,
                // Stated rather than defaulted. An outlined icon button draws its border from the
                // content colour, which in a dark theme is near-white and shouts across a page of
                // quiet rows. `outline` is what every switch on this page already draws its own
                // border with, and these sit in the same column as those switches.
                border = BorderStroke(StrokeWidth.thin, MaterialTheme.colorScheme.outline),
            ) { glyph() }
        }
    }
}

@Composable
private fun OptionRow(
    label: String,
    supporting: String,
    modified: Boolean = false,
    onReset: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    SettingsResettableRow(modified = modified, onReset = onReset) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    supporting: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modified: Boolean = false,
    onReset: (() -> Unit)? = null,
) {
    SettingsResettableRow(modified = modified, onReset = onReset) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
