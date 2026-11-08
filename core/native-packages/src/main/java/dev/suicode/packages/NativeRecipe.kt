package dev.suicode.packages

import kotlinx.serialization.json.*

/** One argv-based execution contract for single files, builds, tests and project workflows. */
open class NativeRecipe internal constructor(
    val pack: VerifiedPack,private val session: CoreSession,val componentId: String,val operation: Operation,
    private val prepare: JsonArray,private val run: JsonArray,private val inputs: Map<String,String> = emptyMap()
) {
    fun authorize(): Reply = session.authorize(Request(operation,componentId,emptyMap()))
    fun resources(): Map<String,ByteArray> = (prepare.flatMap { it.jsonArray }+run).map { it.jsonPrimitive.content }
        .filter { it.startsWith("\${resource:") && it.endsWith("}") }
        .map { it.removePrefix("\${resource:").removeSuffix("}") }.distinct().associateWith { path ->
            if(!path.startsWith("resources/") || path.split('/').any { it in setOf(".","..") }) fail("E_PATH",path)
            pack.content(path)
        }
    fun script(source: String,resourceDirectory: String,project: String=source.substringBeforeLast('/').ifEmpty { "/" }): String {
        require(source.startsWith('/') && project.startsWith('/') && '\u0000' !in source && '\u0000' !in project)
        require(resourceDirectory.matches(Regex("/tmp/sui-package-[a-zA-Z0-9-]+")))
        val reply=authorize();if(!reply.accepted) fail(reply.code,"Native task permission denied or package disabled")
        val resourcePaths=resources().keys
        fun expand(value: String): String=when {
            value=="\${source}" -> source
            value=="\${directory}" -> source.substringBeforeLast('/').ifEmpty { "/" }
            value=="\${project}" -> project
            value.startsWith("\${resource:") && value.endsWith("}") -> {
                val path=value.removePrefix("\${resource:").removeSuffix("}")
                if(path !in resourcePaths) fail("E_COMPONENT",path)
                "$resourceDirectory/$path"
            }
            value.startsWith("\${input:") && value.endsWith("}") -> inputs[value.removePrefix("\${input:").removeSuffix("}")] ?: fail("E_CONTEXT","Missing workflow input")
            "\${" in value -> fail("E_COMPONENT","Unsupported placeholder: $value")
            else -> value
        }
        fun command(value: JsonElement)=value.jsonArray.joinToString(" ") { quote(expand(it.jsonPrimitive.content)) }
        return buildString {
            append("#!/bin/bash\nset -u\nexport TMPDIR=${quote(resourceDirectory+"/tmp")}\nmkdir -p -- \"\$TMPDIR\" || exit 73\ncd -- ${quote(project)} || exit 66\n")
            prepare.forEach { append(command(it)+" || { echo '[Native compilation/preparation failed]' >&2; exit 65; }\n") }
            append(command(run)+"\nresult=\$?\nexit \"\$result\"\n")
        }
    }
    companion object {
        fun forComponent(pack: VerifiedPack,session: CoreSession,id: String,inputs: Map<String,String> = emptyMap()): NativeRecipe {
            require(pack.id==session.packageId && pack.digest==session.digest)
            val c=pack.declarations().singleOrNull { it.id==id } ?: fail("E_COMPONENT",id)
            if(c.type in setOf("simpleRun","basicBuild")) return NativeRecipe(pack,session,id,Operation.SIMPLE_RUN,c.descriptor.array("prepare"),c.descriptor.array("run"))
            val operation=Operation.valueOf(c.descriptor.string("operation"))
            if(operation !in setOf(Operation.SIMPLE_RUN,Operation.BUILD,Operation.TEST)) fail("E_BOUNDARY","Only process operations can supply execution recipes")
            val args=c.descriptor.getValue("arguments").jsonObject
            val argv=Json.parseToJsonElement(args.string("argv")).jsonArray
            if(argv.isEmpty() || argv.size>128 || argv.any { it !is JsonPrimitive || !it.isString || it.content.length>4096 || '\u0000' in it.content }) fail("E_COMPONENT","Invalid recipe argv")
            return NativeRecipe(pack,session,id,operation,JsonArray(emptyList()),argv,inputs)
        }
    }
}
private fun quote(value: String)="'"+value.replace("'","'\\''")+"'"
