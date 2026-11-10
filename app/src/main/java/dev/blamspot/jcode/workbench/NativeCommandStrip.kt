package dev.blamspot.jcode.workbench

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.blamspot.jcode.design.LocalEditorSaveActions
import dev.blamspot.jcode.feature.editor.pane.EditorTab
import dev.suicode.nativehost.*
import dev.suicode.packages.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

internal val LocalAdvancedTaskNavigation=compositionLocalOf<()->Unit> { {} }

/** Current-file execution and project workflows have distinct labelled controls, shared across layouts. */
@Composable internal fun NativeCommandStrip(activeTab: EditorTab?,project: dev.blamspot.jcode.fs.Project?) {
    val strings = dev.blamspot.jcode.design.LocalUiTranslations.current

    val host=NativePackageHost.get(LocalContext.current)
    val rows by host.rows.collectAsState()
    val panel by host.panel.collectAsState()
    val source by host.source.collectAsState()
    val save=LocalEditorSaveActions.current
    val debug=LocalDebugSession.current
    val tasks=LocalAdvancedTaskNavigation.current
    val scope=rememberCoroutineScope()
    var feedback by remember { mutableStateOf<String?>(null) }
    val hasProject=project!=null
    val file=activeTab?.takeIf { !it.isPage }?.filePath?.name
    val name=file ?: source?.substringAfterLast('/')
    val basic=name?.let { runCatching { host.resolve(it)?.pack?.id }.getOrNull() }
    val sourceKit=rows.firstOrNull { row -> row.record.enabled && row.pack.kind==PackKind.KIT && row.pack.manifest.getValue("dependencies").jsonArray.any { it.jsonObject["id"]?.jsonPrimitive?.content==basic } }
    var projectKitIds by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(project?.location,rows) {
        val root=(project?.fsPath as? dev.blamspot.jcode.fs.FsPath.Local)?.file
        projectKitIds=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if(root==null) emptyList() else rows.filter { row -> row.record.enabled && row.pack.kind==PackKind.KIT && row.pack.declarations().any { c ->
                if(c.type!="project") false else runCatching {
                    val markers=Json.parseToJsonElement(c.descriptor.getValue("arguments").jsonObject.getValue("markers").jsonPrimitive.content).jsonArray.map { it.jsonPrimitive.content }
                    markers.isNotEmpty() && markers.all { marker -> marker.matches(Regex("[A-Za-z0-9_.+-]+(/[A-Za-z0-9_.+-]+)*")) && marker.split('/').none { it==".." || it=="." } && java.io.File(root,marker).isFile }
                }.getOrDefault(false)
            } }.map { it.record.id }
        }
    }
    val kit=rows.firstOrNull { it.record.id==projectKitIds.singleOrNull() } ?: sourceKit

    fun invoke(id: String) {
        if(kit==null) { feedback=strings.text("run.kitMissing");return }
        scope.launch {
            try { val result=host.invoke(kit.record.id,id);feedback=if(result.accepted) null else result.code+": "+result.values.entries.joinToString { "${it.key}: ${it.value}" } }
            catch(e: Exception) { feedback=e.message }
        }
    }
    Column {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=8.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            FilledTonalButton(enabled=file!=null && basic!=null || save.quickRunActive,onClick={if(save.quickRunActive) save.onStopQuickRun() else save.onQuickRun(0)}) { Text(if(save.quickRunActive) dev.blamspot.jcode.design.uiText("run.stop") else dev.blamspot.jcode.design.uiText("run.file")) }
            FilledTonalButton(enabled=debug.canDebug || debug.state in setOf(dev.blamspot.jcode.core.debug.DebugState.STOPPED,dev.blamspot.jcode.core.debug.DebugState.RUNNING),onClick={
                when(debug.state) {
                    dev.blamspot.jcode.core.debug.DebugState.STOPPED -> debug.onContinue()
                    dev.blamspot.jcode.core.debug.DebugState.RUNNING -> debug.onStop()
                    else -> debug.onDebug()
                }
            }) { Text(when(debug.state) { dev.blamspot.jcode.core.debug.DebugState.STOPPED -> dev.blamspot.jcode.design.uiText("run.continue");dev.blamspot.jcode.core.debug.DebugState.RUNNING -> dev.blamspot.jcode.design.uiText("run.stopDebug");else -> dev.blamspot.jcode.design.uiText("run.debug") }) }
            OutlinedButton(onClick={invoke("overview")}) { Text(dev.blamspot.jcode.design.uiText("run.overview")) }
            VerticalDivider(Modifier.height(40.dp).padding(horizontal=4.dp))
            TextButton(enabled=hasProject && kit!=null && !save.quickRunActive,onClick={invoke("build")}) { Text(dev.blamspot.jcode.design.uiText("project.build")) }
            TextButton(enabled=hasProject && kit!=null && !save.quickRunActive,onClick={invoke("test")}) { Text(dev.blamspot.jcode.design.uiText("project.test")) }
            TextButton(enabled=hasProject && kit!=null && !save.quickRunActive,onClick={invoke("run-project")}) { Text(dev.blamspot.jcode.design.uiText("project.run")) }
            TextButton(onClick=tasks) { Text(dev.blamspot.jcode.design.uiText("project.tasks")) }
        }
        feedback?.let { text -> Row(Modifier.padding(horizontal=8.dp)) { Text(text,Modifier.weight(1f),color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall);TextButton(onClick={feedback=null}) { Text(strings.text("ui.dismiss")) } } }
    }
    panel?.let { NativeKitPanel(host,it) }
}
