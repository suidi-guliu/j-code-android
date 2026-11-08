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
            step.keysExactly(setOf("id","command","arguments"),setOf("select","jsonArguments","expect"))
            val id=step.string("id")
            if(!id.matches(Regex("[a-z][a-zA-Z0-9]{0,31}")) || id in values) fail("E_WORKFLOW","Duplicate/reserved step ID")
            var args=render(step.getValue("arguments")).jsonArray
            if(step["jsonArguments"]?.jsonPrimitive?.booleanOrNull==true) args=JsonArray(args.map { if(it is JsonObject || it is JsonArray) JsonPrimitive(it.toString()) else it })
            var result=rpc(step.string("command"),args)
            step["select"]?.jsonObject?.let { select ->
                select.keysExactly(setOf("field","equals"))
                val expected=render(select.getValue("equals"))
                result=result.jsonArray.singleOrNull { item -> item.jsonObject[select.string("field")]==expected } ?: fail("E_WORKFLOW","No unique launch target for this file")
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
