package dev.blamspot.jcode.workbench

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Bounded by the actual available window, including IME/split-screen; body owns scrolling. */
@Composable fun WorkbenchDetailDialog(title: String,onDismiss: ()->Unit,content: @Composable ()->Unit) {
    val strings=dev.blamspot.jcode.design.LocalUiTranslations.current
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.widthIn(max=1100.dp).fillMaxWidth(.96f).fillMaxHeight(.94f).testTag("workbench-detail"),shape=MaterialTheme.shapes.medium) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    TextButton(onClick=onDismiss) { Text(strings.text("ui.close")) }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) { content() }
            }
        }
    }
}

@Composable fun WorkbenchReviewDialog(onDismissRequest: ()->Unit,title: @Composable ()->Unit,text: @Composable ()->Unit,confirmButton: @Composable ()->Unit,dismissButton: @Composable ()->Unit) {
    val strings=dev.blamspot.jcode.design.LocalUiTranslations.current
    Dialog(onDismissRequest=onDismissRequest,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.widthIn(max=1100.dp).fillMaxWidth(.96f).fillMaxHeight(.94f),shape=MaterialTheme.shapes.medium) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth()) { Box(Modifier.weight(1f)) { title() };TextButton(onClick=onDismissRequest) { Text(strings.text("ui.close")) } }
                Box(Modifier.weight(1f).fillMaxWidth()) { text() }
                FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) { dismissButton();confirmButton() }
            }
        }
    }
}

@Composable fun WorkbenchExpandableDetails(title: String,content: @Composable ()->Unit) {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        TextButton(onClick={open=true}) { Text(dev.blamspot.jcode.design.uiText("focus.details")) }
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
    }
    if(open) WorkbenchDetailDialog(title,{open=false}) { content() }
}
