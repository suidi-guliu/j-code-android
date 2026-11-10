package dev.blamspot.jcode.core.debug

import org.json.JSONObject

/** Data only. The Android host must review the exact request and recheck the active Kit grant. */
data class RunInTerminal(val cwd: String,val args: List<String>,val env: Map<String,String?>,val title: String) {
    companion object {
        fun parse(json: JSONObject): RunInTerminal {
            require(json.keys().asSequence().all { it in setOf("cwd","args","env","kind","title","argsCanBeInterpretedByShell") }) { "Unsupported runInTerminal parameter" }
            require((if(json.has("kind")) json.opt("kind") as? String else "integrated")=="integrated") { "Only integrated terminals are supported" }
            require(!json.has("argsCanBeInterpretedByShell") || json.opt("argsCanBeInterpretedByShell") is Boolean) { "Shell interpretation flag must be boolean" }
            require(!json.optBoolean("argsCanBeInterpretedByShell",false)) { "Shell interpretation of DAP arguments is unsupported" }
            val cwd=json.opt("cwd") as? String ?: throw IllegalArgumentException("Terminal cwd must be a string")
            require(cwd.length<=4096 && cwd.startsWith('/') && cwd.split('/').none { it==".." } && '\u0000' !in cwd) { "Invalid terminal working directory" }
            val array=json.getJSONArray("args")
            require(array.length() in 1..256) { "Invalid terminal argument count" }
            val args=(0 until array.length()).map { (array.opt(it) as? String ?: throw IllegalArgumentException("Terminal arguments must be strings")).also { s -> require('\u0000' !in s && s.length<=65536) } }
            require(args.first().isNotBlank() && args.sumOf { it.length }<=131072)
            require(!json.has("env") || json.opt("env") is JSONObject) { "Terminal env must be an object" }
            val envJson=json.optJSONObject("env") ?: JSONObject()
            require(envJson.length()<=256)
            val env=envJson.keys().asSequence().associateWith { key ->
                require(key.matches(Regex("[A-Za-z_][A-Za-z0-9_]*")) && key !in setOf("JCODE_EXT_ID","JCODE_PROC_ID") && !key.startsWith("PROOT_")) { "Reserved or invalid environment key: $key" }
                if(envJson.isNull(key)) null else (envJson.opt(key) as? String ?: throw IllegalArgumentException("Environment values must be strings or null")).also { require('\u0000' !in it && it.length<=65536) }
            }
            require(env.entries.sumOf { it.key.length+(it.value?.length ?: 0) }<=131072) { "Terminal environment exceeds size limit" }
            val title=if(json.has("title")) json.opt("title") as? String ?: throw IllegalArgumentException("Terminal title must be a string") else "Debug"
            return RunInTerminal(cwd,args,env,title.take(160))
        }
    }
}

data class TerminalProcess(val processId: Int?,val shellProcessId: Int?) {
    fun json()=JSONObject().apply { processId?.let { put("processId",it) };shellProcessId?.let { put("shellProcessId",it) } }
}
