package dev.blamspot.jcode.feature.sdkmanager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.blamspot.jcode.core.distro.DistroService
import dev.blamspot.jcode.core.distro.InstallationLogArchive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun InstallationLogHistory(distro: DistroService,kind: String) {
    val strings=dev.blamspot.jcode.design.LocalUiTranslations.current
    var show by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf<List<InstallationLogArchive.Entry>>(emptyList()) }
    var selected by remember { mutableStateOf<InstallationLogArchive.Entry?>(null) }
    var content by remember { mutableStateOf("") }
    LaunchedEffect(show) { if(show) entries=withContext(Dispatchers.IO) { distro.installationLogs.entries(kind) } }
    LaunchedEffect(selected) { selected?.let { entry -> content=withContext(Dispatchers.IO) { try { distro.installationLogs.tail(entry) } catch(e: Exception) { "${entry.file}\n${e.message}" } } } }
    TextButton(onClick={show=true}) { Text(strings.text("management.logHistory")) }
    if(show) AlertDialog(onDismissRequest={show=false;selected=null},title={Text(strings.text("management.logHistory"))},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())) {
        if(selected!=null) { TextButton(onClick={selected=null}) { Text(strings.text("ui.back")) };Text(content,style=MaterialTheme.typography.bodySmall) }
        else { if(entries.isEmpty()) Text(strings.text("management.noLogs"));entries.forEach { entry -> TextButton(onClick={selected=entry}) { Text("${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(entry.time))}\n${entry.component} · ${entry.distro}\n${entry.source}\n${entry.outcome}") } } }
    }},confirmButton={TextButton(onClick={show=false;selected=null}) { Text(strings.text("ui.close")) }})
}
