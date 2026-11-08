package dev.blamspot.jcode.workbench

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import dev.blamspot.jcode.design.WorkbenchPaneSizing

@Composable
internal fun WorkbenchPaneResize(bounds: WorkbenchPaneSizing.Bounds, onWidth: (Float)->Unit, onDelta: (Float)->Unit, onCommit: ()->Unit, modifier: Modifier=Modifier) {
    val density=LocalDensity.current
    Box(modifier.width(12.dp).fillMaxHeight()
        .pointerHoverIcon(PointerIcon(android.view.PointerIcon.getSystemIcon(androidx.compose.ui.platform.LocalContext.current,android.view.PointerIcon.TYPE_HORIZONTAL_DOUBLE_ARROW)))
        .semantics {
            contentDescription="Resize navigation panel. Arrow keys adjust width."
            stateDescription="${bounds.width.toInt()} dp"
            progressBarRangeInfo=ProgressBarRangeInfo(bounds.width,bounds.minimum..bounds.maximum)
            setProgress { value -> onWidth(value.coerceIn(bounds.minimum,bounds.maximum));onCommit();true }
            customActions=listOf(CustomAccessibilityAction("Reset panel width") { onWidth(340f.coerceIn(bounds.minimum,bounds.maximum));onCommit();true })
        }.onPreviewKeyEvent {
            if(it.type==KeyEventType.KeyDown && it.key in listOf(Key.DirectionLeft,Key.DirectionRight)) {
                onWidth((bounds.width+if(it.key==Key.DirectionRight) 16f else -16f).coerceIn(bounds.minimum,bounds.maximum));onCommit();true
            } else false
        }.focusable().draggable(rememberDraggableState { delta -> onDelta(delta/density.density) },Orientation.Horizontal,onDragStopped={onCommit()}),contentAlignment=Alignment.Center) {
        Box(Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
        Box(Modifier.width(4.dp).height(40.dp).background(MaterialTheme.colorScheme.primary))
    }
}
