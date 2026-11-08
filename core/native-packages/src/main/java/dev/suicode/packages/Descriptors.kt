package dev.suicode.packages

import kotlinx.serialization.json.*

/** Only these bounded declarative shapes enter Core; none is executable host code. */
internal fun validateDescriptor(type: String, d: JsonObject): Operation? {
    fun text(key: String) { if(d.string(key).length !in 1..4096) fail("E_COMPONENT",key) }
    when(type) {
        "localization" -> { LocalizationDescriptor.parse(d); return null }
        "setup" -> { KitRequirements.parse(d); return null }
        "language" -> {
            d.keysExactly(setOf("languageId","extensions","priority"))
            if(!d.string("languageId").matches(Regex("[a-z][a-z0-9+-]{0,31}"))) fail("E_COMPONENT","languageId")
            val exts=d.array("extensions").map { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content ?: fail("E_COMPONENT","extensions") }
            if(exts.isEmpty() || exts.distinct().size!=exts.size || exts.any { !it.matches(Regex("[A-Za-z0-9+_-]{1,16}")) }) fail("E_COMPONENT","extensions")
            val p=(d["priority"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull ?: fail("E_COMPONENT","priority")
            if(p !in 0..100) fail("E_COMPONENT","priority")
            return null
        }
        "environment" -> {
            d.keysExactly(setOf("environmentId","provider","tools","configuration"))
            text("environmentId")
            if(d.string("provider") !in setOf("temurin","ubuntu-apt")) fail("E_COMPONENT","Unapproved environment provider")
            if(!d.string("configuration").matches(Regex("/opt/sui-code/[a-z-]+\\.env"))) fail("E_COMPONENT","configuration")
            if(d.array("tools").isEmpty() || d.array("tools").any { it !is JsonPrimitive || !it.isString || !it.content.matches(Regex("[A-Za-z0-9+_-]{1,40}")) }) fail("E_COMPONENT","tools")
            return null
        }
        "simpleRun", "basicBuild" -> {
            d.keysExactly(setOf("languageId","environmentId","prepare","run"))
            text("languageId"); text("environmentId")
            fun argv(v: JsonElement) {
                val a=v as? JsonArray ?: fail("E_COMPONENT","argv array required")
                if(a.isEmpty() || a.size>128 || a.any { it !is JsonPrimitive || !it.isString || it.content.length>4096 || '\u0000' in it.content }) fail("E_COMPONENT","Invalid argv")
            }
            val steps=d.array("prepare")
            if(steps.size>16) fail("E_COMPONENT","Too many preparation steps")
            steps.forEach(::argv); argv(d.getValue("run"))
            return Operation.SIMPLE_RUN
        }
        "panel" -> {
            d.keysExactly(setOf("title","text","status"),setOf("nodes"))
            PanelState("panel",d.string("title"),d.string("text"),d.string("status"),(d["nodes"] as? JsonArray) ?: JsonArray(emptyList()))
            return Operation.PANEL_OPEN
        }
        else -> {
            d.keysExactly(setOf("operation","arguments"))
            val op=try { Operation.valueOf(d.string("operation")) } catch(_: Exception) { fail("E_COMPONENT","Unknown operation") }
            val required=mapOf("lsp" to Operation.LSP_START,"dap" to Operation.DAP_START,"build" to Operation.BUILD,"test" to Operation.TEST,"format" to Operation.FORMAT,"lint" to Operation.LINT,"project" to Operation.PROJECT_READ)
            if(type in required && required[type]!=op) fail("E_BOUNDARY","Operation/type mismatch")
            if(type !in required && type !in setOf("workflow","command")) fail("E_COMPONENT",type)
            val args=d["arguments"] as? JsonObject ?: fail("E_COMPONENT","arguments")
            if(args.size>128 || args.values.any { it !is JsonPrimitive || !it.isString || it.content.length>4096 }) fail("E_COMPONENT","arguments")
            return op
        }
    }
}
