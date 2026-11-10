package dev.blamspot.jcode.workbench

import androidx.compose.runtime.compositionLocalOf

data class WorkbenchFocusControls(val mode: String="NORMAL",val setMode: (String)->Unit={})
val LocalWorkbenchFocus=compositionLocalOf { WorkbenchFocusControls() }
class TerminalManagementActions(val reuseIdle: ()->Unit={})
val LocalTerminalManagement=compositionLocalOf { TerminalManagementActions() }
