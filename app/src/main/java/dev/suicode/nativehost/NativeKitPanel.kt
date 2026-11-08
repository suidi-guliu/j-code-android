package dev.suicode.nativehost

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.suicode.packages.PanelState
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

/** Kit widgets inherit native tokens and stay inside one owned dialog surface. */
@Composable
fun NativeKitPanel(host: NativePackageHost,panel: PanelState) {
    val inputs=remember(panel) { mutableStateMapOf<String,String>() }
    val results=remember(panel) { mutableStateMapOf<String,String>() }
    val scope=rememberCoroutineScope()
    var busy by remember(panel) { mutableStateOf(false) }
    var message by remember(panel) { mutableStateOf<String?>(null) }
    fun bind(text: String): String = Regex("\\$\\{result:([a-z][a-z0-9.-]*)\\}").replace(text) { results[it.groupValues[1]] ?: "—" }
    fun invoke(command: String) {
        if(busy) return
        val owner=host.panelOwner ?: return
        busy=true;message=null
        scope.launch {
            try {
                val reply=host.invoke(owner,command,inputs.toMap())
                message=reply.code
                reply.values.forEach { (key,value) -> results["$command.$key"]=value }
            } catch(e: Exception) { message=e.message ?: "Native command failed" }
            finally { busy=false }
        }
    }
    Dialog(onDismissRequest=host::dismissPanel,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.padding(24.dp).widthIn(max=840.dp).fillMaxWidth(),shape=MaterialTheme.shapes.medium) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(panel.title,style=MaterialTheme.typography.titleLarge)
                if(panel.text.isNotEmpty()) Text(panel.text)
                if(panel.status=="loading" || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if(panel.status=="empty") Text("No results")
                panel.nodes.forEach { element -> NativeNode(element.jsonObject,inputs,::bind,!busy,::invoke) }
                message?.let { Text(it,color=MaterialTheme.colorScheme.primary) }
                TextButton(onClick=host::dismissPanel) { Text("Close") }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun NativeNode(n: JsonObject,inputs: MutableMap<String,String>,bind: (String)->String,enabled: Boolean,invoke: (String)->Unit) {
    fun text(key: String)=n.getValue(key).jsonPrimitive.content
    val id=text("id")
    when(text("type")) {
        "column" -> Column(verticalArrangement=Arrangement.spacedBy(8.dp)) { n.getValue("children").jsonArray.forEach { NativeNode(it.jsonObject,inputs,bind,enabled,invoke) } }
        "row" -> androidx.compose.foundation.layout.FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { n.getValue("children").jsonArray.forEach { NativeNode(it.jsonObject,inputs,bind,enabled,invoke) } }
        "text" -> Text(bind(text("text")))
        "metric" -> Text(bind(text("text")),style=MaterialTheme.typography.headlineSmall)
        "input" -> {
            LaunchedEffect(id) { inputs.putIfAbsent(id,text("value")) }
            OutlinedTextField(value=inputs[id] ?: text("value"),onValueChange={ if(it.length<=4096) inputs[id]=it },label={ Text(text("label")) },enabled=enabled,modifier=Modifier.fillMaxWidth())
        }
        "toggle" -> {
            LaunchedEffect(id) { inputs.putIfAbsent(id,text("value")) }
            Row { Switch(checked=(inputs[id] ?: text("value"))=="true",onCheckedChange={inputs[id]=it.toString()},enabled=enabled);Text(text("label"),Modifier.padding(12.dp)) }
        }
        "button" -> Button(onClick={invoke(text("command"))},enabled=enabled) { Text(text("label")) }
        "table" -> Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth()) { n.getValue("columns").jsonArray.forEach { Text(it.jsonPrimitive.content,Modifier.weight(1f),style=MaterialTheme.typography.titleSmall) } }
            n.getValue("rows").jsonArray.forEach { r -> Row(Modifier.fillMaxWidth()) { r.jsonArray.forEach { Text(bind(it.jsonPrimitive.content),Modifier.weight(1f)) } } }
        }
    }
}
