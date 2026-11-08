package dev.suicode.packages

import kotlinx.serialization.json.*

data class ResolvedDebugPlan(val launch: JsonObject,val cwd: String,val port: Int?)

/** Bounded data-flow interpreter. Only the host-supplied RPC executor can perform side effects. */
object DebugWorkflow {
    suspend fun resolve(workflow: JsonObject,context: Map<String,JsonElement>,rpc: suspend (String,JsonArray)->JsonElement): ResolvedDebugPlan {
        workflow.keysExactly(setOf("steps","launch","port"))
        val values=context.toMutableMap()
        fun reference(path: String): JsonElement {
            val parts=path.split('.')
            var current=values[parts.first()] ?: fail("E_WORKFLOW","Missing value ${parts.first()}")
            parts.drop(1).forEach { part -> current=when(val parent=current) {
                is JsonObject -> parent[part] ?: fail("E_WORKFLOW","Missing field $path")
                is JsonArray -> parent.getOrNull(part.toIntOrNull() ?: -1) ?: fail("E_WORKFLOW","Invalid array reference $path")
                else -> fail("E_WORKFLOW","Invalid reference $path")
            } }
            return current
        }
        fun render(value: JsonElement): JsonElement = when(value) {
            is JsonPrimitive -> if(value.isString && value.content.startsWith("\${") && value.content.endsWith("}")) reference(value.content.substring(2,value.content.length-1)) else value
            is JsonArray -> JsonArray(value.map(::render))
            is JsonObject -> JsonObject(value.mapValues { render(it.value) })
        }
        val steps=workflow.array("steps")
        if(steps.size>32) fail("E_SIZE","Too many debug preparation steps")
        steps.forEach { element ->
            val step=element.jsonObject
            step.keysExactly(setOf("id","command","arguments"),setOf("select","jsonArguments","expect","retryEmpty"))
            val id=step.string("id")
            if(!id.matches(Regex("[a-z][a-zA-Z0-9]{0,31}")) || id in values) fail("E_WORKFLOW","Duplicate/reserved step ID")
            var args=render(step.getValue("arguments")).jsonArray
            if(step["jsonArguments"]?.jsonPrimitive?.booleanOrNull==true) args=JsonArray(args.map { if(it is JsonObject || it is JsonArray) JsonPrimitive(it.toString()) else it })
            val attempts=step["retryEmpty"]?.jsonPrimitive?.intOrNull ?: 1
            if(attempts !in 1..30) fail("E_WORKFLOW","retryEmpty must be 1..30")
            var result=rpc(step.string("command"),args)
            var attempt=1
            while(result is JsonArray && result.isEmpty() && attempt++<attempts) {
                kotlinx.coroutines.delay(1000)
                result=rpc(step.string("command"),args)
            }
            step["select"]?.jsonObject?.let { select ->
                select.keysExactly(setOf("field","equals"),setOf("path","fallbackSingle"))
                val expected=render(select.getValue("equals"))
                val candidates=result as? JsonArray ?: fail("E_WORKFLOW","Launch discovery did not return an array")
                fun normalized(value: JsonElement?): String? = (value as? JsonPrimitive)?.content?.let {
                    runCatching { val uri=java.net.URI(it); java.nio.file.Paths.get(if(uri.scheme=="file") uri.path else it).normalize().toString() }.getOrNull()
                }
                val matches=candidates.filter { item ->
                    val actual=item.jsonObject[select.string("field")]
                    if(select["path"]?.jsonPrimitive?.booleanOrNull==true) normalized(actual)!=null && normalized(actual)==normalized(expected) else actual==expected
                }
                result=matches.singleOrNull()
                    ?: candidates.singleOrNull()?.takeIf { select["fallbackSingle"]?.jsonPrimitive?.booleanOrNull==true }
                    ?: fail("E_WORKFLOW",if(candidates.isEmpty()) "No launch target found. Wait for project indexing, check the source root and build errors, then retry." else "Multiple launch targets (${candidates.size}); open the desired entry-point source file. Candidates: ${candidates.toString().take(1500)}")
            }
            step["expect"]?.let { if(result!=it) fail("E_BUILD","IDE build failed: $result") }
            values[id]=result
        }
        val launch=render(workflow.getValue("launch")).jsonObject
        val cwd=launch.string("cwd")
        val port=render(workflow.getValue("port")).let { if(it==JsonNull) null else it.jsonPrimitive.int }
        if(port!=null && port !in 1..65535) fail("E_WORKFLOW","Invalid adapter port")
        return ResolvedDebugPlan(launch,cwd,port)
    }
}
