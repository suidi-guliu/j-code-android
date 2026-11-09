package dev.blamspot.jcode.feature.editor.pane

import androidx.compose.runtime.Composable
import dev.blamspot.jcode.design.uiText

/** Only host-owned page titles; real filenames, project names and extension-provided titles stay literal. */
@Composable
fun EditorTab.localizedTitle(): String = when(pageKind) {
    EditorPageKind.Settings -> uiText("nav.settings")
    EditorPageKind.Environment -> uiText("ui.environment")
    EditorPageKind.Trash -> uiText("ui.trash")
    EditorPageKind.RunConfig -> uiText("project.tasks")
    EditorPageKind.BuildConfig -> uiText("project.build")
    else -> title
}
