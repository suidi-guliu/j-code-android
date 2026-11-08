package dev.suicode.packages

import kotlinx.serialization.json.*

/** Bounded native widget tree. No HTML, stylesheets, code loading or global layout mutation. */
object UiNodes {
    fun validate(nodes: JsonArray,commands: Set<String>?=null) {
        var count=0
        val ids=mutableSetOf<String>()
        fun visit(element: JsonElement,depth: Int) {
            if(++count>128 || depth>8) fail("E_SIZE","UI tree exceeds 128 nodes / 8 levels")
            val n=element as? JsonObject ?: fail("E_COMPONENT","UI node must be an object")
            val type=n.string("type");val id=n.string("id")
            if(!id.matches(Regex("[a-z][a-z0-9.-]{0,63}")) || !ids.add(id)) fail("E_COMPONENT","Duplicate/invalid UI node ID")
            val required=when(type) {
                "column","row" -> setOf("children")
                "text","metric" -> setOf("text")
                "input","toggle" -> setOf("label","value")
                "button" -> setOf("label","command")
                "table" -> setOf("columns","rows")
                else -> fail("E_COMPONENT","Unknown native UI node: $type")
            }
            n.keysExactly(setOf("type","id")+required)
            for(key in required-setOf("children","columns","rows")) if(n.string(key).length>4096) fail("E_SIZE","UI value too long")
            if(type=="toggle" && n.string("value") !in setOf("true","false")) fail("E_COMPONENT","Invalid toggle")
            if(type=="button" && commands!=null && n.string("command") !in commands) fail("E_COMPONENT","UI command must belong to the same Kit")
            if(type in setOf("column","row")) n.array("children").forEach { visit(it,depth+1) }
            if(type=="table") {
                val columns=n.array("columns")
                if(columns.size !in 1..8 || columns.any { it !is JsonPrimitive || !it.isString || it.content.length>80 }) fail("E_COMPONENT","Invalid table columns")
                val rows=n.array("rows")
                if(rows.size>100 || rows.any { r -> r !is JsonArray || r.size!=columns.size || r.any { it !is JsonPrimitive || !it.isString || it.content.length>4096 } }) fail("E_COMPONENT","Invalid table rows")
            }
        }
        nodes.forEach { visit(it,1) }
    }
}
