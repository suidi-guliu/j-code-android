package dev.blamspot.jcode

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.blamspot.jcode.core.debug.DebugState
import dev.blamspot.jcode.design.IconSize
import dev.blamspot.jcode.design.JCodeIcon
import dev.blamspot.jcode.design.JcTooltip
import dev.blamspot.jcode.design.Radius
import dev.blamspot.jcode.design.Space
import dev.blamspot.jcode.design.jcIcon
import dev.blamspot.jcode.workbench.DebugSessionUi

/** Whether a session is up: there is something to step, inspect and stop. Three places asked this
 *  question and each spelled the answer out again. */
internal val DebugSessionUi.active: Boolean
    get() = state != DebugState.DISCONNECTED && state != DebugState.TERMINATED

/**
 * A running debug session: transport toolbar, call stack and variables.
 *
 * Only shown while a session is up. That is the one state where this is something no other control
 * does, and where a heading, a surface and a state chip are carrying their weight. The idle
 * fallback is [DebugLaunchRow], which the caller places on its own.
 *
 * Reads its state from [dev.blamspot.jcode.workbench.LocalDebugSession]; the caller passes the
 * resolved [ui].
 */
@Composable
internal fun DebugSessionPanel(ui: DebugSessionUi, modifier: Modifier = Modifier) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        Row(
            modifier = Modifier.padding(start = Space.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            Icon(jcIcon(JCodeIcon.Debug), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            // "Debug session", not "Debug": beside a row of run configs each carrying their own Debug
            // button, a section called Debug reads as a second, different Debug command.
            Text(strings.text("ui.debugSession"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            DebugStateChip(ui.state)
        }
        DebugToolbar(ui)
        if (ui.callStack.isNotEmpty()) CallStackList(ui)
        if (ui.variables.isNotEmpty()) VariablesList(ui)
        // Console output lives in the right-drawer "Debug" tab (alongside Terminal/Output), not here.
    }
}

/**
 * Start a session on the file in the editor — the way in for a project with no run config to start
 * one from.
 *
 * The caller places this only when there is a target, so there is no "open a source file" state: a
 * project with nothing to debug has already been told it has no run config, and a second line
 * saying there is nothing to debug either was the panel repeating itself.
 */
@Composable
internal fun DebugLaunchRow(ui: DebugSessionUi) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    val target = ui.debugTargetName
    when {
        target != null && ui.canDebug -> {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                shape = RoundedCornerShape(Radius.lg),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = Space.ms, end = Space.xxs, top = Space.xxs, bottom = Space.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.xs),
                ) {
                    Text(
                        text = strings.text("debug.target", mapOf("target" to (target).toString())),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    JcTooltip(strings.text("ui.startDebugging")) {
                        IconButton(onClick = ui.onDebug) {
                            Icon(
                                painter = jcIcon(JCodeIcon.Debug),
                                contentDescription = strings.text("ui.startDebugging"),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }
        // Kept, because it is a diagnosis rather than an empty state: it names the engine that is
        // missing and where to install it.
        target != null -> Text(
            strings.text("debug.missing", mapOf("target" to (target).toString())),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun DebugToolbar(ui: DebugSessionUi) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    val stopped = ui.state == DebugState.STOPPED
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically) {
            DebugAction(JCodeIcon.Continue, strings.text("run.continue"), enabled = stopped, tint = MaterialTheme.colorScheme.primary, onClick = ui.onContinue)
            DebugAction(JCodeIcon.StepOver, strings.text("ui.stepOver"), enabled = stopped, onClick = ui.onStepOver)
            DebugAction(JCodeIcon.StepInto, strings.text("ui.stepInto"), enabled = stopped, onClick = ui.onStepInto)
            DebugAction(JCodeIcon.StepOut, strings.text("ui.stepOut"), enabled = stopped, onClick = ui.onStepOut)
        }
        DebugAction(JCodeIcon.Stop, strings.text("ui.stop"), enabled = true, tint = MaterialTheme.colorScheme.error, onClick = ui.onStop)
    }
}

@Composable
private fun DebugAction(icon: JCodeIcon, label: String, enabled: Boolean, tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant, onClick: () -> Unit) {
    JcTooltip(label) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                painter = jcIcon(icon),
                contentDescription = label,
                tint = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.size(IconSize.lg),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Space.xs, top = Space.xxs),
    )
}

@Composable
private fun CallStackList(ui: DebugSessionUi) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    SectionLabel(strings.text("ui.callStack"))
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp).verticalScroll(rememberScrollState()),
    ) {
        ui.callStack.forEach { frame ->
            val where = frame.sourcePath?.substringAfterLast('/')?.let { "$it:${frame.line}" } ?: ""
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Space.xs, vertical = Space.xxs),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = frame.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (where.isNotEmpty()) {
                    Text(
                        text = where,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun VariablesList(ui: DebugSessionUi) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    SectionLabel(strings.text("ui.variables"))
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState()),
    ) {
        ui.variables.forEach { row ->
            if (row.depth == 0) {
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = Space.xs, top = Space.xs, bottom = Space.hairline),
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = Space.lg, end = Space.xs, top = Space.hairline, bottom = Space.hairline),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    Text(
                        text = row.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.4f, fill = false),
                    )
                    Text(
                        text = row.value,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Scrolling monospace console lines — shared by the Debug side-panel and the right-drawer tab. */
@Composable
internal fun DebugConsoleLines(
    lines: List<String>,
    maxHeight: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified,
    modifier: Modifier = Modifier,
) {
    val heightModifier = if (maxHeight.isSpecified) Modifier.heightIn(max = maxHeight) else Modifier
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(heightModifier)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.sm, vertical = Space.s),
    ) {
        lines.takeLast(200).forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
        }
    }
}

/**
 * The right-drawer "Debug" tab: live session state + transport controls + the full-height console.
 * Reads [dev.blamspot.jcode.workbench.LocalDebugSession] so it mirrors the Run/Debug side panel's session.
 */
@Composable
internal fun DebugConsoleSidebarContent(modifier: Modifier = Modifier) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    val ui = dev.blamspot.jcode.workbench.LocalDebugSession.current
    val active = ui.active
    Column(modifier = modifier.fillMaxSize().padding(horizontal = Space.sm, vertical = Space.s)) {
        if (!active && ui.output.isEmpty()) {
            Text(
                strings.text("ui.startADebugSessionToSeeItsConsoleOutput"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Space.s),
            )
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(
                text = ui.debugTargetName?.let { strings.text("debug.console", mapOf("target" to (it).toString())) } ?: strings.text("run.debug"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            DebugStateChip(ui.state)
        }
        if (active) DebugToolbar(ui)
        DebugConsoleLines(ui.output, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DebugStateChip(state: DebugState) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current
    val (label, active) = when (state) {
        DebugState.STARTING, DebugState.INITIALIZING -> strings.text("option.Starting") to false
        DebugState.RUNNING -> strings.text("option.Running") to true
        DebugState.STOPPED -> strings.text("option.Paused") to true
        DebugState.ERROR -> strings.text("option.Error") to false
        else -> strings.text(if(state==DebugState.TERMINATED) "option.terminated" else "option.disconnected") to false
    }
    Surface(
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        shape = RoundedCornerShape(Radius.md),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = Space.sm, vertical = Space.xs),
            style = MaterialTheme.typography.labelSmall,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
