package dev.blamspot.jcode.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.blamspot.jcode.workbench.WorkbenchDetailDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NativeTaskActions(val stop: (String)->Unit={},val rerun: (NativeTaskRecord)->Unit={},val reveal: (String)->Unit={})
val LocalNativeTaskActions=compositionLocalOf { NativeTaskActions() }

@Composable fun NativeTaskResults() {
    val context=LocalContext.current
    val store=remember(context) { NativeTaskHistory.get(context) }
    val records by store.records.collectAsState()
    val archiveWarning by store.warning.collectAsState()
    val manager=dev.blamspot.jcode.TerminalSessionHost.existingManager()
    val metadata by (manager?.metadataVersion ?: remember { MutableStateFlow(0L) }).collectAsState()
    val actions=LocalNativeTaskActions.current
    val strings=dev.blamspot.jcode.design.LocalUiTranslations.current
    LaunchedEffect(store) { withContext(Dispatchers.IO) { store.load() } }
    var selected by remember { mutableStateOf<String?>(null) }
    Text(strings.text("tasks.results"),style=MaterialTheme.typography.titleSmall)
    archiveWarning?.let { Text(it,color=MaterialTheme.colorScheme.error) }
    if(records.isEmpty()) Text(strings.text("tasks.empty"),style=MaterialTheme.typography.bodySmall)
    records.asReversed().take(30).forEach { record ->
        OutlinedCard(Modifier.fillMaxWidth().testTag("task-${record.id}")) {
            Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(record.name,style=MaterialTheme.typography.titleSmall)
                Text(strings.text("tasks.state."+record.outcome.name)+" · "+strings.text("tasks.exit",mapOf("code" to (record.exitCode?.toString() ?: "—"))),style=MaterialTheme.typography.bodySmall)
                Text(record.project ?: record.source,style=MaterialTheme.typography.bodySmall,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick={selected=record.id}) { Text(strings.text("tasks.details")) }
                    record.sessionId?.let { id -> TextButton(enabled=metadata>=0 && manager?.getSession(id)!=null,onClick={actions.reveal(id)}) { Text(strings.text("ui.terminal")) } }
                    if(record.outcome in setOf(TaskOutcome.STARTING,TaskOutcome.RUNNING)) TextButton(onClick={actions.stop(record.id)}) { Text(strings.text("run.stop")) }
                    else TextButton(onClick={actions.rerun(record)}) { Text(strings.text("tasks.rerun")) }
                }
            }
        }
    }
    selected?.let { id ->
        records.firstOrNull { it.id==id }?.let { record ->
            var log by remember(id) { mutableStateOf("") }
            LaunchedEffect(record) { log=withContext(Dispatchers.IO) { val file=store.logFile(id);if(file.isFile) java.io.RandomAccessFile(file,"r").use { input -> input.seek((input.length()-65536).coerceAtLeast(0));val b=ByteArray((input.length()-input.filePointer).toInt());input.readFully(b);String(b,Charsets.UTF_8) } else "" } }
            WorkbenchDetailDialog(record.name,{selected=null}) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(strings.text("tasks.state."+record.outcome.name)+" · "+strings.text("tasks.exit",mapOf("code" to (record.exitCode?.toString() ?: "—"))))
                    Text("${record.packageId}/${record.component} ${record.packageVersion}\n${record.source}\n${record.project.orEmpty()}")
                    Text(strings.text("tasks.times",mapOf("start" to java.util.Date(record.started).toString(),"end" to (record.ended?.let { java.util.Date(it).toString() } ?: "—"),"duration" to record.durationMs.toString())))
                    if(record.detail.isNotBlank()) Text(record.detail)
                    Text(store.logFile(id).path,style=MaterialTheme.typography.bodySmall)
                    Text(strings.text("tasks.logBound"),style=MaterialTheme.typography.bodySmall)
                    if(record.truncated) Text(strings.text("tasks.logTruncated"),color=MaterialTheme.colorScheme.error)
                    Text(log,fontFamily=FontFamily.Monospace,style=MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
