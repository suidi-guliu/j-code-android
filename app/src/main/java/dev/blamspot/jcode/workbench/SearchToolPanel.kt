package dev.blamspot.jcode.workbench

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import dev.blamspot.jcode.design.uiText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.blamspot.jcode.core.search.SearchMatch
import dev.blamspot.jcode.core.search.SearchModule
import dev.blamspot.jcode.core.search.SearchOptions
import dev.blamspot.jcode.design.CompactSearchField
import dev.blamspot.jcode.design.FileTypeIcon
import dev.blamspot.jcode.design.IconSize
import dev.blamspot.jcode.design.LocalFileIconSet
import dev.blamspot.jcode.design.JCodeIcon
import dev.blamspot.jcode.design.ManagerFilterChip
import dev.blamspot.jcode.design.Radius
import dev.blamspot.jcode.design.Space
import dev.blamspot.jcode.design.StrokeWidth
import dev.blamspot.jcode.feature.editor.pane.EditorTab
import dev.blamspot.jcode.fs.FsPath
import dev.blamspot.jcode.fs.Project
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File

private const val MAX_DISPLAY_RESULTS = 2000
private const val MIN_QUERY_LENGTH = 2

/** Where the Search tool looks for matches. */
internal enum class SearchScope(val label: String, val placeholder: String) {
    Content("Content", "Search in files"),
    Names("Names", "Search file and folder names"),
    CurrentDoc("Current", "Search in current document"),
}

/** Compact 3-way scope switch: a segmented control that fills its row so the labels always fit
 *  (equal thirds, no horizontal scroll, no truncation) regardless of drawer width. */
@Composable
private fun SearchScopeSelector(
    selected: SearchScope,
    onSelect: (SearchScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val key=when(selected) { SearchScope.Content -> "find.content";SearchScope.Names -> "find.names";SearchScope.CurrentDoc -> "find.current" }
    Box(modifier) {
        TextButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) { Text(uiText(key)+" ▾") }
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
            SearchScope.entries.forEach { mode -> DropdownMenuItem(text={Text(uiText(when(mode) { SearchScope.Content -> "find.content";SearchScope.Names -> "find.names";SearchScope.CurrentDoc -> "find.current" }))},onClick={onSelect(mode);expanded=false}) }
        }
    }
}

/**
 * Find in Files panel (left-drawer Search tool). Streams matches from [SearchModule.engine]
 * (native ripgrep FFI when present, Kotlin fallback otherwise) and opens a tapped result at its
 * line/column via [onOpenResult], which expects a `path:line:col` token (1-based).
 *
 * Three scopes: file content across the project (default), file names only, and the current
 * document — the active editor tab's live buffer, so unsaved edits are searched too.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SearchToolPanel(
    project: Project?,
    activeTab: EditorTab?,
    internalRoot: File? = null,
    onOpenDirectory: (File)->Unit = {},
    onOpenResult: (String) -> Unit,
    seed: Pair<Int, String>? = null,
    onSeedConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    var searchInternal by remember { mutableStateOf(false) }
    val rootFile = if(searchInternal) internalRoot else (project?.fsPath as? FsPath.Local)?.file
    var searchError by remember { mutableStateOf<String?>(null) }

    if (rootFile == null) {
        SearchHint("Open a local project to search its files.", modifier)
        return
    }

    var query by remember { mutableStateOf("") }
    // Seed the query when "Find text" is invoked from the editor menu (keyed by the request nonce so
    // repeating the same word re-seeds), then consume it one-shot so re-entering the panel (e.g.
    // switching sidebar tools and back) doesn't re-inject over a query the user cleared or edited.
    LaunchedEffect(seed?.first) {
        seed?.second?.takeIf { it.isNotBlank() }?.let { query = it }
        if (seed != null) onSeedConsumed()
    }
    var caseSensitive by remember { mutableStateOf(false) }
    var regex by remember { mutableStateOf(false) }
    var scope by remember { mutableStateOf(SearchScope.Content) }
    var searching by remember { mutableStateOf(false) }
    var truncated by remember { mutableStateOf(false) }
    val results = remember { mutableStateListOf<SearchMatch>() }

    // Page tabs and never-saved buffers have no path to reopen a result at, so Current Doc needs a
    // real file-backed tab.
    val activeDocTab = activeTab?.takeIf {
        !it.isPage && it.editorState != null && it.filePath.path.isNotBlank()
    }

    // Re-run on any input change, debounced so each keystroke doesn't spawn a walk. Changing the key
    // cancels the prior collection, which closes the flow and stops the native search.
    LaunchedEffect(rootFile, caseSensitive, regex, scope, activeDocTab?.id) {
        snapshotFlow { query }
            .distinctUntilChanged()
            .collectLatest { q ->
                results.clear()
                searching=false
                searchError=null
                truncated = false
                kotlinx.coroutines.delay(250)
                if (q.length < MIN_QUERY_LENGTH || (scope == SearchScope.CurrentDoc && activeDocTab == null)) {
                    searching = false
                    return@collectLatest
                }
                searching = true
                searchError=null
                val options = SearchOptions(
                    query = q,
                    isRegex = regex,
                    caseSensitive = caseSensitive,
                    maxResults = MAX_DISPLAY_RESULTS+1,
                )
                val matches = when (scope) {
                    SearchScope.Content -> SearchModule.engine.search(rootFile, options)
                    SearchScope.Names -> SearchModule.engine.searchFileNames(rootFile, options)
                    SearchScope.CurrentDoc -> {
                        val snap = activeDocTab!!.editorState!!.snapshot.value
                        SearchModule.engine.searchLines(
                            lineCount = snap.lineCount,
                            lineText = snap::lineText,
                            options = options,
                            filePath = activeDocTab.filePath.relativeToOrNull(rootFile)?.path
                                ?: activeDocTab.filePath.name,
                        )
                    }
                }
                try {
                    matches.collect { match ->
                        if (results.size < MAX_DISPLAY_RESULTS) {
                            results.add(match)
                        } else {
                            truncated = true
                        }
                    }
                } catch(e: CancellationException) { throw e } catch(e: Exception) { searchError=strings.text("find.failed", mapOf("reason" to (e.message ?: "").toString())) } finally {
                    searching = false
                }
            }
    }

    // Current Doc results live outside the project-relative token scheme the other scopes use.
    val openMatch: (SearchMatch) -> Unit = { match ->
        val base = if (scope == SearchScope.CurrentDoc) {
            activeDocTab?.filePath?.absolutePath
        } else {
            File(rootFile, match.filePath).absolutePath
        }
        base?.let { if(match.isDirectory) onOpenDirectory(File(it)) else onOpenResult("$it:${match.lineNumber + 1}:${match.columnStart + 1}") }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max=260.dp).verticalScroll(rememberScrollState()).padding(horizontal = Space.md, vertical = Space.sm),
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Text(uiText("find.hint"),style=MaterialTheme.typography.bodySmall)
            if(internalRoot!=null && project?.id!=dev.blamspot.jcode.fs.InternalFiles.ROOT_NODE_ID) FlowRow {
                TextButton(onClick={searchInternal=true}) { Text((if(searchInternal) "✓ " else "")+uiText("files.internal")) }
                TextButton(onClick={searchInternal=false}) { Text((if(!searchInternal) "✓ " else "")+strings.text("ui.project")) }
            }
            Text(rootFile.path,style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis)
            CompactSearchField(
                query = query,
                onQueryChange = { query = it },
                searching = searching,
                placeholder = strings.text(when(scope) { SearchScope.Content -> "find.placeholderContent";SearchScope.Names -> "find.placeholderNames";SearchScope.CurrentDoc -> "find.placeholderCurrent" }),
            )
            SearchScopeSelector(selected=scope,onSelect={scope=it},modifier=Modifier.fillMaxWidth())
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                ManagerFilterChip(selected = caseSensitive, label = "Aa") { caseSensitive = !caseSensitive }
                ManagerFilterChip(selected = regex, label = ".*") { regex = !regex }

            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        val resultSnapshot=results.toList()
        val grouped = remember(resultSnapshot) {
            resultSnapshot.groupBy { it.filePath }.entries.toList()
        }
        val fileCount = grouped.size

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.md, vertical = Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val plus = if (truncated) "+" else ""
            val summary = when {
                scope == SearchScope.CurrentDoc && activeDocTab == null ->
                    strings.text("find.openCurrent")
                query.length < MIN_QUERY_LENGTH -> strings.text("find.minimum", mapOf("count" to (MIN_QUERY_LENGTH).toString()))
                results.isEmpty() && searching -> strings.text("find.searching")
                results.isEmpty() -> strings.text("find.none")
                scope == SearchScope.Names ->
                    strings.text("find.namesSummary", mapOf("count" to ("${results.size}$plus").toString()))
                else -> strings.text("find.contentSummary", mapOf("count" to ("${results.size}$plus").toString(), "files" to (fileCount).toString()))
            }
            Text(
                text = searchError ?: summary,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (scope == SearchScope.Names) {
                items(results.toList(), key = { it.filePath }) { match ->
                    FileNameRow(match) { openMatch(match) }
                }
            } else {
                grouped.forEach { (path, matches) ->
                    item(key = "file:$path") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Space.md, vertical = Space.xs),
                            horizontalArrangement = Arrangement.spacedBy(Space.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SearchFileIcon(path)
                            Text(
                                text = path,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    items(matches, key = { "${path}:${it.lineNumber}:${it.columnStart}" }) { match ->
                        MatchRow(match) { openMatch(match) }
                    }
                }
            }
        }
    }
}

/**
 * The file type badge on a search row.
 *
 * Drawn only when a file icon set is chosen: with none, every row would carry the same generic glyph
 * and the list would be narrower for no information. [path] may be a full path or a bare name.
 */
@Composable
private fun SearchFileIcon(path: String) {
    if (LocalFileIconSet.current == null) return
    FileTypeIcon(
        name = path.replace('\\', '/').substringAfterLast('/'),
        isDirectory = false,
        size = IconSize.sm,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FileNameRow(match: SearchMatch, onClick: () -> Unit) {
    val parent = match.filePath.replace('\\', '/').substringBeforeLast('/', "")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Space.md, vertical = Space.xs),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if(match.isDirectory) Icon(dev.blamspot.jcode.design.jcIcon(JCodeIcon.Folder),"Folder") else SearchFileIcon(match.lineText)
        Text(
            text = match.lineText,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (parent.isNotEmpty()) {
            Text(
                text = parent,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier=Modifier.weight(1f),
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MatchRow(match: SearchMatch, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = Space.xxl, end = Space.md, top = Space.xs, bottom = Space.xs),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Text(
            text = "${match.lineNumber + 1}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = match.lineText.trim(),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SearchHint(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Space.xxl),
        )
    }
}
