package dev.blamspot.jcode.feature.explorer
import dev.blamspot.jcode.design.FileTypeIcon
import dev.blamspot.jcode.design.JCodeIcon
import dev.blamspot.jcode.design.Space
import dev.blamspot.jcode.design.jcIcon

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import dev.blamspot.jcode.design.uiText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import dev.blamspot.jcode.design.AlertDialog
import dev.blamspot.jcode.design.CompactDestructiveButton
import dev.blamspot.jcode.design.CompactFilledButton
import dev.blamspot.jcode.design.CompactOutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import dev.blamspot.jcode.fs.moveFileOrDir
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.blamspot.jcode.design.CompactContextMenu
import dev.blamspot.jcode.design.ContextAction
import dev.blamspot.jcode.design.JCodeTheme
import dev.blamspot.jcode.design.JcTooltip
import dev.blamspot.jcode.design.LocalTrashSettings
import dev.blamspot.jcode.design.trashRetentionLabel
import dev.blamspot.jcode.design.DenseRow
import dev.blamspot.jcode.design.IconSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import dev.blamspot.jcode.fs.Fs
import dev.blamspot.jcode.fs.FsKind
import dev.blamspot.jcode.fs.FsNode
import dev.blamspot.jcode.fs.FsPath
import dev.blamspot.jcode.fs.Project
import dev.blamspot.jcode.fs.Workspace
import dev.blamspot.jcode.fs.copyFileOrDir
import dev.blamspot.jcode.fs.copyLocalTreeToDocumentTree
import dev.blamspot.jcode.fs.createFile
import dev.blamspot.jcode.fs.createDirectory
import dev.blamspot.jcode.fs.deletePermanently
import dev.blamspot.jcode.fs.exportFileToUri
import dev.blamspot.jcode.fs.importContentUris
import dev.blamspot.jcode.fs.renameFile
import dev.blamspot.jcode.fs.TrashSource
import dev.blamspot.jcode.fs.WorkspaceServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main explorer composable with tree/list toggle, breadcrumb, and file operations.
 *
 * @param workspace current workspace
 * @param project the project to explore
 * @param fs filesystem implementation for the project
 * @param context android context for SAF operations
 * @param onFileSelected callback when a file is tapped
 * @param onSnackbar callback for showing user messages
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ExplorerView(
    workspace: Workspace?,
    project: Project,
    fs: Fs,
    context: Context,
    modifier: Modifier = Modifier,
    viewMode: ExplorerViewMode = ExplorerViewMode.Tree,
    hiddenPatterns: List<String> = emptyList(),
    greyOutExcluded: Boolean = true,
    /** Auto-refresh the tree from filesystem events while the Explorer is actually on-screen. Gated
     *  by the caller (drawer open / sidebar visible) so watchers stop the moment it's hidden. */
    autoRefreshEnabled: Boolean = true,
    revealDirectory: String? = null,
    onFileSelected: ((FsNode) -> Unit)? = null,
    onSnackbar: ((String) -> Unit)? = null,
) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    // Key only on project + fs (not the workspace object, which gets a new identity on every
    // roster mutation). Otherwise the view model is rebuilt but LaunchedEffect(project) — keyed on
    // the data-equal project — doesn't re-fire, leaving the tree stuck on "No files yet".
    val viewModel = remember(project, fs) {
        TreeViewModel(workspace, fs, initialViewMode = viewMode)
    }
    val scope = rememberCoroutineScope()

    val treeRows by viewModel.treeRows.collectAsStateWithLifecycle()
    val listRows by viewModel.listRows.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val breadcrumb by viewModel.breadcrumb.collectAsStateWithLifecycle()
    val currentPath by viewModel.currentPath.collectAsStateWithLifecycle()
    // The configured [viewMode] is pushed into the view model below; [activeViewMode] reflects the
    // shape the view model has actually materialized, so the UI never renders an empty mismatched flow.
    val activeViewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val selectionState = viewModel.selectionState
    val selectedIds by selectionState.selected.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf<CreateTarget?>(null) }
    var showRenameDialog by remember { mutableStateOf<RenameTarget?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<TreeRow?>(null) }

    // Clipboard for copy/cut operations
    var clipboard by remember { mutableStateOf<List<ClipboardEntry>>(emptyList()) }
    var multi by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val searchRows=remember(query,treeRows,listRows,activeViewMode) { (if(activeViewMode==ExplorerViewMode.Tree) treeRows else listRows).filter { !it.isPlaceholder && it.node.name.contains(query,ignoreCase=true) } }
    val searchNote=if(query.isBlank()) "" else uiText("files.matches",mapOf("count" to searchRows.size.toString()))
    var sort by remember { mutableStateOf(TreeViewModel.Sort.Name) }
    var properties by remember { mutableStateOf<String?>(null) }
    var batchDelete by remember { mutableStateOf(false) }
    val selectedRows=(if(query.isNotBlank()) searchRows else treeRows+listRows).distinctBy { it.id }.filter { it.id in selectedIds && !it.isPlaceholder }
    // If a directory and its child are selected, operate only on the directory.
    val batchRows=selectedRows.filter { child -> selectedRows.none { parent -> parent.id!=child.id && child.id.startsWith(parent.id+"/") } }

    // The bin. Its button stays on the toolbar while there is anything in it even after the setting
    // is turned off — opting out should stop deletes going there, not strand what already has.
    val trashSettings = LocalTrashSettings.current
    val trash = remember(context) { WorkspaceServiceLocator.trash(context) }
    var trashHasItems by remember { mutableStateOf(false) }
    var trashRevision by remember { mutableStateOf(0) }
    // Re-checked whenever the Explorer comes back on screen as well as after a delete: the bin is
    // emptied on its own page, so this cannot be the only thing that knows what is in it.
    LaunchedEffect(trashRevision, autoRefreshEnabled) { trashHasItems = !trash.isEmpty() }

    // Load project root on first composition
    LaunchedEffect(project,revealDirectory) {
        viewModel.loadProjectRoot(project)
        revealDirectory?.let { requested ->
            val root=(project.fsPath as? FsPath.Local)?.file
            val target=File(requested)
            if(root!=null && runCatching { dev.blamspot.jcode.fs.DocumentPaths(root).resolve(requested).isDirectory }.getOrDefault(false)) {
                viewModel.setViewMode(ExplorerViewMode.List);viewModel.navigateTo(FsPath.Local(target),target.name)
            }
        }
    }

    // The view mode is an app preference (Settings); react when it changes.
    LaunchedEffect(viewMode) {
        viewModel.setViewMode(viewMode)
    }

    // The project-root exclude-list + grey-out-vs-hide effect (Settings); re-apply when either changes
    // without remounting the tree.
    LaunchedEffect(hiddenPatterns, greyOutExcluded) {
        viewModel.setHiddenPatterns(hiddenPatterns, greyOutExcluded)
    }

    // VCS decorations pushed by the shell; re-badge rows when the extension reports new status (also
    // keyed on the view model so a recreated one is fed the current maps, not just future changes).
    val scmUi = LocalExplorerScmUi.current
    LaunchedEffect(viewModel, scmUi.status, scmUi.submodules) {
        viewModel.setScmDecorations(scmUi.status, scmUi.submodules)
    }

    // Live-update: while the Explorer is on-screen, watch the directories whose contents are actually
    // shown — the project root plus every expanded folder (Tree) or the current folder (List) — and
    // repaint on filesystem changes. Only visible dirs are watched (never the whole recursive tree),
    // so a large repo costs a handful of inotify watches, not thousands. The watch key is the *set* of
    // shown dirs, so a change inside an already-watched dir doesn't re-subscribe — only expand/collapse
    // re-targets. The whole watch tears down the instant [autoRefreshEnabled] goes false.
    val watchDirs = remember(treeRows, listRows, activeViewMode, currentPath) {
        when (activeViewMode) {
            ExplorerViewMode.Tree -> treeRows.asSequence()
                .filter { it.isExpanded && it.node.kind == FsKind.Directory }
                .map { it.node.path }
                .distinctBy { it.stableId }
                .toList()
            ExplorerViewMode.List -> listOfNotNull(currentPath)
        }
    }
    // NUL separator so it can't collide with anything inside a path; written as an escape because a
    // raw NUL byte in the source makes grep treat this whole file as binary and skip it.
    val watchKey = remember(watchDirs) { watchDirs.map { it.stableId }.sorted().joinToString("\u0000") }
    // Reopening the drawer does one catch-up refresh for changes missed while it was hidden.
    LaunchedEffect(autoRefreshEnabled) {
        if (autoRefreshEnabled) viewModel.refresh()
    }
    LaunchedEffect(autoRefreshEnabled, watchKey) {
        if (!autoRefreshEnabled || watchDirs.isEmpty()) return@LaunchedEffect
        merge(*watchDirs.map { dir -> fs.watch(dir).catch { } }.toTypedArray())
            .collectLatest {
                // Debounce: a fresh event cancels this wait, folding a burst into a single refresh.
                delay(300)
                viewModel.refresh()
                scmUi.onFsActivity?.invoke()
            }
    }

    // A watch alone is not enough to bound how stale the tree can get. inotify drops events when its
    // queue overflows — exactly what a build or a git checkout in the guest produces — it is only
    // registered on the directories currently shown, and not every backing filesystem raises it (a
    // SAF folder has no inotify at all; [SafFs.watch] is itself a poll). Device-checked: a guest
    // write through proot into an app-private project DOES reach this process's inotify, so the poll
    // is the backstop rather than the only path — but it is what makes "what is on screen is what is
    // on disk" true within a bounded time instead of hopefully.
    //
    // Poll on the same terms Source Control takes: only while the Explorer is the open tool (this
    // composable is composed for no other) and the app is actually in front of the user.
    // A pass lists the root plus every expanded directory, and [TreeRow]/[FsNode] compare by value,
    // so a tick that finds nothing new publishes an equal list — the StateFlow drops it and nothing
    // recomposes. It does not raise onFsActivity: Source Control runs its own poll when it is open,
    // and a `git status` per tick for a panel nobody is looking at is what that gate exists to stop.
    val pollLifecycle = LocalLifecycleOwner.current
    val lifecycleState by pollLifecycle.lifecycle.currentStateAsState()
    val pollActive = autoRefreshEnabled && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(pollActive, viewModel) {
        if (!pollActive) return@LaunchedEffect
        while (true) {
            // Delay first: opening the panel already refreshes through the effect above.
            delay(EXPLORER_VISIBLE_REFRESH_MS)
            viewModel.refresh()
        }
    }

    // SAF import/export: import streams device-storage picks into a target dir (row menu or toolbar);
    // export saves a file via a create-document picker or copies a local folder into a picked tree.
    // Pending targets are saveable tokens (a process death behind the system picker would drop plain
    // remember state and silently ignore the pick), and the copies run in their own supervisor scope:
    // leaving the Explorer must not cancel a tree copy midway.
    val fileOpScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    var importTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        val target = importTarget?.let(::fsPathFromToken)
        importTarget = null
        if (target != null && uris.isNotEmpty()) fileOpScope.launch {
            val result = runCatching { importContentUris(fs, context, uris, target) }
            // Refresh either way: a partial failure may have landed some files already.
            viewModel.refresh()
            scmUi.onFsActivity?.invoke()
            result.onSuccess { names -> onSnackbar?.invoke(strings.text("status.imported", mapOf("count" to (names.size).toString()))) }
                .onFailure { onSnackbar?.invoke(strings.text("status.importFailed", mapOf("reason" to (it.message ?: "").toString()))) }
        }
    }
    var exportFileSource by rememberSaveable { mutableStateOf<String?>(null) }
    val exportFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val source = exportFileSource?.let(::fsPathFromToken)
        exportFileSource = null
        if (source != null && uri != null) fileOpScope.launch {
            runCatching { exportFileToUri(context, source, uri) }
                .onSuccess { onSnackbar?.invoke(strings.text("ui.exportedFile")) }
                .onFailure { onSnackbar?.invoke(strings.text("status.exportFailed", mapOf("reason" to (it.message ?: "").toString()))) }
        }
    }
    var exportDirSource by rememberSaveable { mutableStateOf<String?>(null) }
    val exportDirLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        val source = exportDirSource?.let(::File)
        exportDirSource = null
        if (source != null && uri != null) fileOpScope.launch {
            runCatching { copyLocalTreeToDocumentTree(context, source, uri, source.name) }
                .onSuccess { n -> onSnackbar?.invoke(strings.text("status.exportedFrom", mapOf("count" to (n).toString(), "name" to (source.name).toString()))) }
                .onFailure { onSnackbar?.invoke(strings.text("status.exportFailed", mapOf("reason" to (it.message ?: "").toString()))) }
        }
    }

    // Where toolbar create/paste should land: the viewed dir in List mode; the selected dir (or the
    // selected file's parent) in Tree mode, falling back to the project root.
    fun resolveCreateParent(): FsPath {
        if (activeViewMode == ExplorerViewMode.List) return currentPath ?: project.fsPath
        val selected = treeRows.firstOrNull { it.id in selectedIds && !it.isPlaceholder }
            ?: return project.fsPath
        return when (selected.node.kind) {
            FsKind.Directory -> selected.node.path
            else -> parentPathOf(selected.node.path) ?: project.fsPath
        }
    }

    val protectedProjects = LocalProtectedProjectRoots.current
    fun isProtectedPath(path: FsPath): Boolean = path.stableId == project.fsPath.stableId ||
        (path as? FsPath.Local)?.let { dev.blamspot.jcode.fs.InternalFiles.protectsProject(it.file, protectedProjects) } == true
    fun runRowAction(row: TreeRow, action: RowAction) {
        if (isProtectedPath(row.node.path) && action in setOf(RowAction.Rename, RowAction.Cut, RowAction.Delete)) {
            onSnackbar?.invoke(strings.text("ui.theRootFolderIsPermanentManageProjectNamesThrough"))
            return
        }
        when (action) {
            RowAction.Properties -> properties="${row.node.name}\n${row.node.path.stableId}\n${row.node.kind} · ${row.node.sizeBytes} bytes\nModified: ${java.util.Date(row.node.modifiedAtMillis)}"
            RowAction.Open -> onFileSelected?.invoke(row.node)
            RowAction.NewFile -> showCreateDialog = CreateTarget(row.node.path, isDirectory = false)
            RowAction.NewFolder -> showCreateDialog = CreateTarget(row.node.path, isDirectory = true)
            RowAction.Rename -> showRenameDialog = RenameTarget(row.node.path, row.node.name)
            RowAction.Copy -> {
                clipboard = listOf(ClipboardEntry(row.node.path, row.node.name, isCut = false))
                onSnackbar?.invoke(strings.text("status.copied", mapOf("name" to (row.node.name).toString())))
            }
            RowAction.Cut -> {
                clipboard = listOf(ClipboardEntry(row.node.path, row.node.name, isCut = true))
                onSnackbar?.invoke(strings.text("status.cut", mapOf("name" to (row.node.name).toString())))
            }
            RowAction.Delete -> showDeleteConfirm = row
            RowAction.ImportHere -> {
                importTarget = fsPathToken(row.node.path)
                importLauncher.launch(arrayOf("*/*"))
            }
            RowAction.Export -> {
                val path = row.node.path
                when {
                    row.node.kind != FsKind.Directory -> {
                        exportFileSource = fsPathToken(path)
                        exportFileLauncher.launch(row.node.name)
                    }
                    path is FsPath.Local -> {
                        exportDirSource = path.file.absolutePath
                        exportDirLauncher.launch(null)
                    }
                    else -> onSnackbar?.invoke(strings.text("ui.folderExportSupportsLocalFoldersOnly"))
                }
            }
        }
    }

    CompositionLocalProvider(LocalProjectRootId provides project.fsPath.stableId,LocalExplorerMultiSelection provides multi) {
    Column(modifier = modifier.fillMaxSize()) {
        // Compact action toolbar (always visible) with create/refresh/paste. The Tree|List view mode
        // is set in Settings, not here.
        OutlinedTextField(value=query,onValueChange={query=it.take(256)},label={Text(uiText("files.filter"))},singleLine=true,modifier=Modifier.fillMaxWidth())
        Text(uiText("files.filterHint"),style=MaterialTheme.typography.bodySmall)
        if(searchNote.isNotBlank()) Text(searchNote,style=MaterialTheme.typography.bodySmall)
        FlowRow(Modifier.fillMaxWidth()) {
            TextButton(onClick={multi=!multi;selectionState.clear()}) { Text(if(multi) strings.text("files.doneSelection", mapOf("count" to (selectedIds.size).toString())) else strings.text("ui.select")) }
            TextButton(onClick={sort=TreeViewModel.Sort.entries[(sort.ordinal+1)%TreeViewModel.Sort.entries.size];scope.launch { viewModel.setSort(sort) }}) { Text(strings.text("files.sort", mapOf("sort" to (strings.text(when(sort) { TreeViewModel.Sort.Name -> "ui.name";TreeViewModel.Sort.Modified -> "option.Modified";TreeViewModel.Sort.Size -> "option.Size" })).toString()))) }
            var actionsExpanded by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick={actionsExpanded=true}) { Text(strings.text("ui.selectionActions")) }
                CompactContextMenu(expanded=actionsExpanded,onDismissRequest={actionsExpanded=false},listActions=buildList {
                    if(multi) {
                        add(ContextAction(JCodeIcon.SelectAll,strings.text("ui.selectAllVisible")) { (if(query.isNotBlank()) searchRows else if(activeViewMode==ExplorerViewMode.Tree) treeRows else listRows).filter { !it.isPlaceholder && it.id!=project.fsPath.stableId }.forEach { if(it.id !in selectedIds) selectionState.toggle(it.id) } })
                        add(ContextAction(JCodeIcon.Copy,strings.text("ui.copy"),enabled=batchRows.isNotEmpty()) { clipboard=batchRows.map { ClipboardEntry(it.node.path,it.node.name,false) } })
                        add(ContextAction(JCodeIcon.Cut,strings.text("ui.cutMove"),enabled=batchRows.isNotEmpty()) { if(batchRows.any { isProtectedPath(it.node.path) }) onSnackbar?.invoke(strings.text("ui.projectRootFoldersMustBeManagedThroughProjects")) else clipboard=batchRows.map { ClipboardEntry(it.node.path,it.node.name,true) } })
                        add(ContextAction(JCodeIcon.Trash,strings.text("ui.moveToTrash"),enabled=batchRows.isNotEmpty()) { batchDelete=true })
                    }
                    add(ContextAction(JCodeIcon.Settings,strings.text("ui.propertiesFullNames"),enabled=selectedRows.isNotEmpty()) { properties=selectedRows.joinToString("\n\n") { strings.text("files.properties", mapOf("name" to (it.node.name).toString(), "path" to (it.node.path.stableId).toString(), "kind" to (strings.text(if(it.node.kind==FsKind.Directory) "files.kindDirectory" else "files.kindFile")).toString(), "bytes" to (it.node.sizeBytes).toString(), "modified" to (java.util.Date(it.node.modifiedAtMillis)).toString())) } })
                })
            }
        }
        ExplorerToolbar(
            viewMode = activeViewMode,
            onCreateFile = { showCreateDialog = CreateTarget(resolveCreateParent(), isDirectory = false) },
            onCreateFolder = { showCreateDialog = CreateTarget(resolveCreateParent(), isDirectory = true) },
            onImport = {
                importTarget = fsPathToken(resolveCreateParent())
                importLauncher.launch(arrayOf("*/*"))
            },
            onCollapseAll = { scope.launch { viewModel.collapseAll() } },
            onRefresh = {
                scope.launch { viewModel.refresh() }
                scmUi.onFsActivity?.invoke()
            },
            onPaste = {
                val entries=clipboard
                val parent=resolveCreateParent()
                fileOpScope.launch {
                    for(entry in entries) {
                        try {
                            if(entry.isCut) {
                                check(!isProtectedPath(entry.sourcePath)) { "Manage registered project folders through Project management" }
                                moveFileOrDir(fs,context,entry.sourcePath,parent)
                            } else copyFileOrDir(fs,context,entry.sourcePath,parent)
                        } catch(e: Exception) {
                            onSnackbar?.invoke(strings.text("files.pasteStopped", mapOf("name" to (entry.name).toString(), "reason" to (e.message ?: "").toString())))
                            clipboard=entries.dropWhile { it!=entry };viewModel.refresh();return@launch
                        }
                    }
                    clipboard=emptyList();viewModel.refresh();scmUi.onFsActivity?.invoke()
                    onSnackbar?.invoke(strings.text("files.pasted", mapOf("count" to (entries.size).toString())))
                }
            },
            canPaste = clipboard.isNotEmpty(),
            showTrash = trashSettings.enabled || trashHasItems,
            onOpenTrash = trashSettings.onOpen,
        )

        // Content. Tree shows the hierarchy from the root (no breadcrumb); List is a flat
        // file-manager for one directory, navigated via the breadcrumb + up button. The scrollable
        // body must take the *remaining* height (weight) so its LazyColumn is bounded and scrolls;
        // a bare fillMaxSize would overflow under the toolbar and hide the last rows.
        if(query.isNotBlank()) Box(Modifier.weight(1f)) {
            ListViewContent(rows=searchRows,isLoading=false,selectedIds=selectedIds,selectionState=selectionState,
                onRowClick={row -> if(row.node.kind==FsKind.Directory) { query="";scope.launch { viewModel.setViewMode(ExplorerViewMode.List);viewModel.navigateTo(row.node.path,row.node.name) } } else onFileSelected?.invoke(row.node) },onAction={row,action -> runRowAction(row,action)})
        } else when (activeViewMode) {
            ExplorerViewMode.Tree -> Box(modifier = Modifier.weight(1f)) {
                TreeViewContent(
                    rows = treeRows,
                    isLoading = isLoading,
                    selectedIds = selectedIds,
                    selectionState = selectionState,
                    onToggleExpand = { row -> scope.launch { viewModel.toggleExpand(row) } },
                    onRowClick = { row ->
                        if (row.node.kind == FsKind.Directory) {
                            scope.launch { viewModel.toggleExpand(row) }
                        } else {
                            onFileSelected?.invoke(row.node)
                        }
                    },
                    onAction = { row, action -> runRowAction(row, action) },
                )
            }

            ExplorerViewMode.List -> {
                ExplorerBreadcrumb(
                    entries = breadcrumb,
                    onNavigate = { entry -> scope.launch { viewModel.navigateTo(entry.path, entry.label) } },
                    onNavigateUp = { scope.launch { viewModel.navigateUp() } },
                )
                Box(modifier = Modifier.weight(1f)) {
                    ListViewContent(
                        rows = listRows,
                        isLoading = isLoading,
                        selectedIds = selectedIds,
                        selectionState = selectionState,
                        onRowClick = { row ->
                            if (row.node.kind == FsKind.Directory) {
                                scope.launch { viewModel.navigateTo(row.node.path, row.node.name) }
                            } else {
                                onFileSelected?.invoke(row.node)
                            }
                        },
                        onAction = { row, action -> runRowAction(row, action) },
                    )
                }
            }
        }
    }
    }

    properties?.let { value -> AlertDialog(onDismissRequest={properties=null},title={Text(strings.text("ui.fileProperties"))},text={Text(value,Modifier.verticalScroll(rememberScrollState()))},confirmButton={TextButton(onClick={properties=null}) { Text(strings.text("ui.close")) }}) }
    if(batchDelete) AlertDialog(onDismissRequest={batchDelete=false},title={Text(strings.text("files.trashSelected", mapOf("count" to (batchRows.size).toString())))},text={Text(strings.text("ui.filesRemainRecoverableFromTrashRegisteredProjectRootsCannot"))},confirmButton={TextButton(onClick={
        batchDelete=false
        fileOpScope.launch {
            try {
                check(batchRows.none { isProtectedPath(it.node.path) }) { "Manage project roots through Projects" }
                batchRows.forEach { trash.put(context,it.node.path,project.name,(project.fsPath as? FsPath.Local)?.file?.path,TrashSource.Explorer) }
                selectionState.clear();trashRevision++;viewModel.refresh()
            } catch(e: Exception) { onSnackbar?.invoke(strings.text("files.trashPaused", mapOf("reason" to (e.message ?: "").toString())));viewModel.refresh() }
        }
    }) { Text(strings.text("ui.moveToTrash")) }},dismissButton={TextButton(onClick={batchDelete=false}) { Text(strings.text("ui.cancel")) }})
    // Create dialog
    showCreateDialog?.let { target ->
        CreateRenameDialog(
            title = if (target.isDirectory) strings.text("files.newFolder") else strings.text("files.newFile"),
            initialName = if (target.isDirectory) "new-folder" else "new-file.txt",
            onDismiss = { showCreateDialog = null },
            onConfirm = { name ->
                scope.launch {
                    runCatching {
                        if (target.isDirectory) {
                            createDirectory(fs, context, target.parentPath, name)
                        } else {
                            createFile(fs, context, target.parentPath, name)
                        }
                        viewModel.refresh()
                        scmUi.onFsActivity?.invoke()
                        onSnackbar?.invoke(strings.text("status.created", mapOf("name" to (name).toString())))
                    }.onFailure {
                        onSnackbar?.invoke(strings.text("status.createFailed", mapOf("reason" to (it.message ?: "").toString())))
                    }
                }
                showCreateDialog = null
            },
        )
    }

    // Rename dialog
    showRenameDialog?.let { target ->
        CreateRenameDialog(
            title = strings.text("ui.rename"),
            initialName = target.currentName,
            onDismiss = { showRenameDialog = null },
            onConfirm = { newName ->
                scope.launch {
                    runCatching {
                        check(!isProtectedPath(target.path)) { "Manage registered project folders through Project management" }
                        renameFile(fs, context, target.path, newName)
                        viewModel.refresh()
                        scmUi.onFsActivity?.invoke()
                        onSnackbar?.invoke(strings.text("status.renamed", mapOf("name" to (newName).toString())))
                    }.onFailure {
                        onSnackbar?.invoke(strings.text("status.renameFailed", mapOf("reason" to (it.message ?: "").toString())))
                    }
                }
                showRenameDialog = null
            },
        )
    }

    // Delete confirmation. With the Trash on this is a move and says so; with it off the delete is
    // final, and the wording has to be the one the button will actually honour.
    showDeleteConfirm?.let { target ->
        val isDir = target.node.kind == FsKind.Directory
        val subject = if (isDir) "The folder, with everything in it," else "The file"
        val what = if (isDir) "folder and everything in it" else "file"
        val toTrash = trashSettings.enabled
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text(strings.text("files.deleteNamed", mapOf("name" to (target.node.name).toString()))) },
            text = {
                Text(
                    if (toTrash) strings.text("files.trashDescription", mapOf("retention" to (if(trashSettings.retentionDays==0) strings.text("trash.untilEmpty") else strings.text("trash.days",mapOf("count" to trashSettings.retentionDays.toString()))).toString())) else strings.text("files.deleteDescription")
                )
            },
            // Emphasis follows the consequence: a move to the Trash is recoverable and gets the
            // ordinary filled button, while a real delete is drawn destructive.
            confirmButton = {
                val confirmAction: () -> Unit = {
                    val node = target.node
                    showDeleteConfirm = null
                    scope.launch {
                        runCatching {
                            check(!isProtectedPath(node.path)) { "Manage registered project folders through Project management" }
                            if (toTrash) {
                                trash.put(
                                    context = context,
                                    path = node.path,
                                    projectName = project.name,
                                    projectRoot = (project.fsPath as? FsPath.Local)?.file?.absolutePath,
                                    source = TrashSource.Explorer,
                                )
                                trashRevision++
                            } else {
                                deletePermanently(fs, context, node.path)
                            }
                            viewModel.refresh()
                            scmUi.onFsActivity?.invoke()
                            onSnackbar?.invoke(
                                if (toTrash) strings.text("status.movedToTrash", mapOf("name" to (node.name).toString())) else strings.text("status.deleted", mapOf("name" to (node.name).toString())),
                            )
                        }.onFailure { onSnackbar?.invoke(strings.text("status.deleteFailed", mapOf("reason" to (it.message ?: "").toString()))) }
                    }
                }
                if (toTrash) {
                    CompactFilledButton(text = strings.text("ui.moveToTrash"), onClick = confirmAction)
                } else {
                    CompactDestructiveButton(text = strings.text("ui.delete"), onClick = confirmAction)
                }
            },
            dismissButton = {
                CompactOutlinedButton(text = strings.text("ui.cancel"), onClick = { showDeleteConfirm = null })
            },
        )
    }
}

data class CreateTarget(val parentPath: FsPath, val isDirectory: Boolean)
data class RenameTarget(val path: FsPath, val currentName: String)

/** Represents a file/directory in the clipboard for copy/cut operations. */
data class ClipboardEntry(
    val sourcePath: FsPath,
    val name: String,
    val isCut: Boolean, // true if cut, false if copy
)

/** Per-row actions surfaced via the visible overflow (⋮) menu. */
enum class RowAction { Open, NewFile, NewFolder, Rename, Copy, Cut, Delete, ImportHere, Export, Properties }

/** Parent of a path, or null when there is no addressable parent (SAF tree URIs). */
private fun parentPathOf(path: FsPath): FsPath? = when (path) {
    is FsPath.Local -> path.file.parentFile?.let { FsPath.Local(it) }
    is FsPath.Saf -> null
}

// FsPath as a saveable string, for pending SAF-picker targets that must survive process death.
private fun fsPathToken(path: FsPath): String = when (path) {
    is FsPath.Local -> "local:" + path.file.absolutePath
    is FsPath.Saf -> "saf:" + path.uri
}

private fun fsPathFromToken(token: String): FsPath? = when {
    token.startsWith("local:") -> FsPath.Local(File(token.removePrefix("local:")))
    token.startsWith("saf:") -> FsPath.Saf(Uri.parse(token.removePrefix("saf:")))
    else -> null
}

// --- Row overflow menu (visible ⋮ on every row) ---

@Composable
private fun RowOverflowMenu(
    row: TreeRow,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAction: (TreeRow, RowAction) -> Unit,
) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    val isDir = row.node.kind == FsKind.Directory
    val scmUi = LocalExplorerScmUi.current
    // The project root must not be moved or deleted from the tree, so Cut/Delete are hidden on it.
    val protectedProjects = LocalProtectedProjectRoots.current
    val isProjectRoot = LocalProjectRootId.current == row.node.path.stableId || (row.node.path as? FsPath.Local)?.let { dev.blamspot.jcode.fs.InternalFiles.protectsProject(it.file, protectedProjects) } == true
    Box {
        JcTooltip(strings.text("ui.moreActions")) {
            IconButton(
                onClick = { onExpandedChange(true) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    painter = jcIcon(JCodeIcon.MoreVert),
                    contentDescription = strings.text("ui.moreActions"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(IconSize.md),
                )
            }
        }
        CompactContextMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            quickActions = buildList {
                add(ContextAction(JCodeIcon.Copy, strings.text("ui.copy")) { onAction(row, RowAction.Copy) })
                if (!isProjectRoot) add(ContextAction(JCodeIcon.Cut, strings.text("ui.cut")) { onAction(row, RowAction.Cut) })
                if (!isProjectRoot) add(ContextAction(JCodeIcon.Rename, strings.text("ui.rename")) { onAction(row, RowAction.Rename) })
                if (!isProjectRoot) {
                    add(ContextAction(JCodeIcon.Delete, strings.text("ui.delete"), destructive = true) { onAction(row, RowAction.Delete) })
                }
            },
            listActions = buildList {
                if (!isDir) add(ContextAction(JCodeIcon.Open, strings.text("ui.open")) { onAction(row, RowAction.Open) })
                if (isDir) {
                    add(ContextAction(JCodeIcon.NewFile, strings.text("ui.newFile")) { onAction(row, RowAction.NewFile) })
                    add(ContextAction(JCodeIcon.NewFolder, strings.text("ui.newFolder")) { onAction(row, RowAction.NewFolder) })
                    add(ContextAction(JCodeIcon.Add, strings.text("ui.importFilesTextc98b4f")) { onAction(row, RowAction.ImportHere) })
                }
                add(ContextAction(JCodeIcon.Settings, strings.text("ui.propertiesFullName")) { onAction(row, RowAction.Properties) })
                add(ContextAction(JCodeIcon.Save, strings.text("ui.export")) { onAction(row, RowAction.Export) })
                // Extension-contributed actions (e.g. "Add to .gitignore") target a path inside the
                // repo — the project root isn't a valid target, so omit them there.
                if (!isProjectRoot) {
                    scmUi.onContextAction?.let { dispatch ->
                        scmUi.contextActions
                            .filter { explorerActionAppliesTo(it, row.node.name, isDir) }
                            .forEach { a -> add(ContextAction(a.icon, a.label) { dispatch(a, row.node) }) }
                    }
                }
            },
        )
    }
}

// --- Breadcrumb ---

@Composable
private fun ExplorerBreadcrumb(
    entries: List<TreeViewModel.BreadcrumbEntry>,
    onNavigate: (TreeViewModel.BreadcrumbEntry) -> Unit,
    onNavigateUp: () -> Unit,
) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = Space.sm, vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (entries.size > 1) {
            JcTooltip(strings.text("ui.upOneLevel")) {
                IconButton(onClick = onNavigateUp, modifier = Modifier.size(48.dp)) {
                    Icon(
                        painter = jcIcon(JCodeIcon.ArrowUp),
                        contentDescription = strings.text("ui.upOneLevel"),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(IconSize.md),
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
        entries.forEachIndexed { index, entry ->
            if (index > 0) {
                Text(
                    text = "›",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Space.xxs),
                )
            }
            Text(
                text = entry.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (index == entries.lastIndex) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier
                    .clickable { onNavigate(entry) }
                    .padding(vertical = Space.xxs),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// --- Tree View ---

@Composable
private fun TreeViewContent(
    rows: List<TreeRow>,
    isLoading: Boolean,
    selectedIds: Set<String>,
    selectionState: ExplorerSelectionState,
    onToggleExpand: (TreeRow) -> Unit,
    onRowClick: (TreeRow) -> Unit,
    onAction: (TreeRow, RowAction) -> Unit,
) {
    if (isLoading && rows.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.padding(Space.lg))
        }
        return
    }
    if (rows.isEmpty()) {
        EmptyExplorerHint()
        return
    }

    val multi=LocalExplorerMultiSelection.current
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(rows, key = { it.id }) { row ->
            TreeRowItem(
                row = row,
                isSelected = row.id in selectedIds,
                onToggleExpand = onToggleExpand,
                onClick = {
                    if(multi) selectionState.toggle(row.id) else { selectionState.selectSingle(row.id);onRowClick(row) }
                },
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun EmptyExplorerHint() {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    Box(modifier = Modifier.fillMaxSize().padding(Space.xxl), contentAlignment = Alignment.Center) {
        Text(
            text = strings.text("ui.noFilesYetUseTheNewFileNewFolder"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Width reserved for the disclosure chevron so files (no chevron) align under folders. */
private val ChevronSlot = 28.dp

/** Opacity of a project-root entry that is excluded under the "grey out" effect. */
private const val EXCLUDED_ROW_ALPHA = 0.4f

/** How often the tree re-reads its shown directories while the panel is open and focused. Faster
 *  than Source Control's tick because a pass is a handful of local directory listings rather than a
 *  `git status` in the guest, and slow enough that a large expanded tree isn't re-listed constantly. */
private const val EXPLORER_VISIBLE_REFRESH_MS = 2_000L

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun TreeRowItem(
    row: TreeRow,
    isSelected: Boolean,
    onToggleExpand: (TreeRow) -> Unit,
    onClick: () -> Unit,
    onAction: (TreeRow, RowAction) -> Unit,
) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    val iconSize = IconSize.md
    val indent = (row.depth.coerceAtMost(4) * 16).dp

    if (row.isPlaceholder) {
        DenseRow(
            leading = { Spacer(modifier = Modifier.width(indent + ChevronSlot + iconSize)) },
            content = {
                Text(
                    text = row.node.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            },
        )
        return
    }

    var menuExpanded by remember { mutableStateOf(false) }
    val isDir = row.node.kind == FsKind.Directory

    DenseRow(
        height = 48.dp,
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menuExpanded = true },
            )
            .then(
                if (isSelected) {
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                } else {
                    Modifier
                }
            )
            // Excluded (grey-out effect): kept in the tree but dimmed so it reads as de-emphasized.
            .then(if (row.isExcluded) Modifier.alpha(EXCLUDED_ROW_ALPHA) else Modifier),
        leading = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(indent))
                if (isDir) {
                    Box(
                        modifier = Modifier
                            .size(ChevronSlot)
                            .clickable { onToggleExpand(row) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = if (row.isExpanded) jcIcon(JCodeIcon.ChevronDown) else jcIcon(JCodeIcon.ChevronRight),
                            contentDescription = if (row.isExpanded) strings.text("ui.collapse") else strings.text("ui.expand"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(iconSize),
                        )
                    }
                    FileTypeIcon(
                        name = row.node.name,
                        isDirectory = true,
                        isExpanded = row.isExpanded,
                        size = iconSize,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Spacer(modifier = Modifier.width(ChevronSlot))
                    FileTypeIcon(
                        name = row.node.name,
                        isDirectory = false,
                        size = iconSize,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        content = {
            Text(
                text = row.node.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = row.vcsStatus?.let { vcsStatusColor(it) } ?: Color.Unspecified,
            )
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RowVcsBadges(row)
                RowOverflowMenu(
                    row = row,
                    expanded = menuExpanded,
                    onExpandedChange = { menuExpanded = it },
                    onAction = onAction,
                )
            }
        },
    )
}

// --- List View ---

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ListViewContent(
    rows: List<TreeRow>,
    isLoading: Boolean,
    selectedIds: Set<String>,
    selectionState: ExplorerSelectionState,
    onRowClick: (TreeRow) -> Unit,
    onAction: (TreeRow, RowAction) -> Unit,
) {
    if (isLoading && rows.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.padding(Space.lg))
        }
        return
    }
    if (rows.isEmpty()) {
        EmptyExplorerHint()
        return
    }

    val multi=LocalExplorerMultiSelection.current
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(rows, key = { it.id }) { row ->
            var menuExpanded by remember { mutableStateOf(false) }
            val iconSize = IconSize.md
            DenseRow(
                modifier = Modifier
                    .combinedClickable(
                        onClick = {
                            if(multi) selectionState.toggle(row.id) else { selectionState.selectSingle(row.id);onRowClick(row) }
                        },
                        onLongClick = { menuExpanded = true },
                    )
                    .then(
                        if (row.id in selectedIds) {
                            Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        } else {
                            Modifier
                        }
                    )
                    .then(if (row.isExcluded) Modifier.alpha(EXCLUDED_ROW_ALPHA) else Modifier),
                leading = {
                    val isDirectory = row.node.kind == FsKind.Directory
                    FileTypeIcon(
                        name = row.node.name,
                        isDirectory = isDirectory,
                        size = iconSize,
                        tint = if (isDirectory) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
                content = {
                    Column {
                        Text(
                            text = row.node.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = row.vcsStatus?.let { vcsStatusColor(it) } ?: Color.Unspecified,
                        )
                        if (row.node.kind == FsKind.File) {
                            Text(
                                text = "${formatSize(row.node.sizeBytes)} · ${formatModified(row.node.modifiedAtMillis)}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                },
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RowVcsBadges(row)
                        RowOverflowMenu(
                            row = row,
                            expanded = menuExpanded,
                            onExpandedChange = { menuExpanded = it },
                            onAction = onAction,
                        )
                    }
                },
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

private val listDateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

private fun formatModified(millis: Long): String =
    if (millis <= 0L) "—" else listDateFormat.format(Date(millis))

// --- Toolbar ---

@Composable
private fun ExplorerToolbar(
    viewMode: ExplorerViewMode,
    onCreateFile: () -> Unit,
    onCreateFolder: () -> Unit,
    onImport: () -> Unit,
    onCollapseAll: () -> Unit,
    onRefresh: () -> Unit,
    onPaste: () -> Unit,
    canPaste: Boolean,
    showTrash: Boolean,
    onOpenTrash: () -> Unit,
) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    Row(Modifier.fillMaxWidth().padding(horizontal=Space.xs),verticalAlignment=Alignment.CenterVertically) {
        ToolbarIcon(jcIcon(JCodeIcon.NewFile),uiText("files.newFile"),onCreateFile)
        ToolbarIcon(jcIcon(JCodeIcon.NewFolder),uiText("files.newFolder"),onCreateFolder)
        ToolbarIcon(jcIcon(JCodeIcon.Refresh),uiText("files.refresh"),onRefresh)
        Spacer(Modifier.weight(1f))
        var more by remember { mutableStateOf(false) }
        Box {
            ToolbarIcon(jcIcon(JCodeIcon.MoreVert),uiText("files.more"),{more=true})
            CompactContextMenu(expanded=more,onDismissRequest={more=false},listActions=buildList {
                add(ContextAction(JCodeIcon.Add,strings.text("ui.importFiles"),onClick=onImport))
                if(canPaste) add(ContextAction(JCodeIcon.Paste,strings.text("ui.paste"),onClick=onPaste))
                if(viewMode==ExplorerViewMode.Tree) add(ContextAction(JCodeIcon.Collapse,strings.text("ui.collapseAll"),onClick=onCollapseAll))
                if(showTrash) add(ContextAction(JCodeIcon.Trash,strings.text("ui.trashRestore"),onClick=onOpenTrash))
            })
        }
    }
}

@Composable
private fun ToolbarIcon(
    icon: Painter,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    JcTooltip(description) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
            Icon(
                painter = icon,
                contentDescription = description,
                modifier = Modifier.size(IconSize.md),
                tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        }
    }
}

// --- Create/Rename Dialog ---

@Composable
private fun CreateRenameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    var textState by remember { mutableStateOf(TextFieldValue(initialName, TextRange(initialName.length))) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = textState,
                onValueChange = { textState = it },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (textState.text.isNotBlank()) {
                        onConfirm(textState.text)
                    }
                }),
                singleLine = true,
            )
        },
        confirmButton = {
            CompactFilledButton(
                text = strings.text("ui.oK"),
                onClick = {
                    if (textState.text.isNotBlank()) {
                        onConfirm(textState.text)
                    }
                },
                enabled = textState.text.isNotBlank(),
            )
        },
        dismissButton = {
            CompactOutlinedButton(text = strings.text("ui.cancel"), onClick = onDismiss)
        },
    )
}

// --- VCS badges ---

/** Trailing VCS chips for a row: an "S" submodule marker and/or the status letter. */
@Composable
private fun RowVcsBadges(row: TreeRow) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    if (row.isSubmodule) {
        JcTooltip(strings.text("ui.gitSubmodule")) {
            Text(
                text = "S",
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = Space.xxs),
            )
        }
    }
    row.vcsStatus?.let { status ->
        Text(
            text = status,
            color = vcsStatusColor(status),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Space.xxs),
        )
    }
}

/** Shared status-letter → color mapping (badge and filename tint): modified amber, added/untracked
 *  green, deleted/conflicted red, renamed primary. */
@Composable
private fun vcsStatusColor(status: String): Color = when (status.firstOrNull()) {
    'M', DIR_CONTAINS_CHANGES.first() -> JCodeTheme.semanticColors.warning
    'A', '?' -> JCodeTheme.semanticColors.success
    'D', 'U' -> MaterialTheme.colorScheme.error
    'R', 'C' -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private val LocalExplorerMultiSelection=androidx.compose.runtime.compositionLocalOf { false }
