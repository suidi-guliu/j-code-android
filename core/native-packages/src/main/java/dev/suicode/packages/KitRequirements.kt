package dev.suicode.packages

import kotlinx.serialization.json.*

/** API 3. Reviewed, declarative prerequisites; inspection must never execute package code. */
data class KitRequirement(
    val id: String, val name: String, val version: String, val source: String,
    val provider: String, val files: List<String>, val commands: List<String>, val packages: List<String>,
    val environmentId: String?, val resource: String?, val bytes: Long?, val anyCommands: List<String> = emptyList(),
    val dependsOn: List<String> = emptyList(), val pythonModules: List<String> = emptyList(), val explicitDependencies: Boolean = false,
)
object KitRequirements {
    fun parse(d: JsonObject): List<KitRequirement> {
        d.keysExactly(setOf("requirements"))
        val entries=d.array("requirements")
        if(entries.isEmpty() || entries.size>32) fail("E_COMPONENT","Require 1..32 prerequisites")
        val ids=mutableSetOf<String>()
        val parsed=entries.map { element ->
            val v=element as? JsonObject ?: fail("E_COMPONENT","Prerequisite must be an object")
            v.keysExactly(setOf("id","name","version","source","provider","files","commands","packages","bytes"),setOf("environmentId","resource","anyCommands","dependsOn","pythonModules"))
            val id=v.string("id")
            if(!id.matches(Regex("[a-z][a-z0-9.-]{0,63}")) || !ids.add(id)) fail("E_COMPONENT","Duplicate/invalid prerequisite")
            for(key in listOf("name","version","source")) if(v.string(key).length !in 1..1024) fail("E_COMPONENT",key)
            val uri=runCatching { java.net.URI(v.string("source")) }.getOrNull()
            if(uri?.scheme!="https" || uri.host.isNullOrBlank() || uri.userInfo!=null) fail("E_COMPONENT","HTTPS prerequisite source required")
            fun list(key: String,pattern: Regex): List<String> = (v[key]?.jsonArray ?: JsonArray(emptyList())).map { value ->
                val s=(value as? JsonPrimitive)?.takeIf { it.isString }?.content ?: fail("E_COMPONENT",key)
                if(!s.matches(pattern)) fail("E_COMPONENT","Invalid $key: $s")
                s
            }.also { if(it.size>64 || it.distinct().size!=it.size) fail("E_COMPONENT",key) }
            val files=list("files",Regex("/(opt/sui-code|usr|bin|lib)/[A-Za-z0-9_./+*-]+"))
            if(files.any { it.split('/').any { p -> p==".." || p=="." } || '*' in it }) fail("E_PATH","Prerequisite file path")
            val commands=list("commands",Regex("[A-Za-z0-9][A-Za-z0-9+_.-]{0,79}"))
            val anyCommands=list("anyCommands",Regex("[A-Za-z0-9][A-Za-z0-9+_.-]{0,79}"))
            val packages=list("packages",Regex("[a-z0-9][a-z0-9+.-]{0,79}"))
            val dependsOn=list("dependsOn",Regex("[a-z][a-z0-9.-]{0,63}"))
            val pythonModules=list("pythonModules",Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*"))
            if(files.isEmpty() && commands.isEmpty() && packages.isEmpty() && anyCommands.isEmpty() && pythonModules.isEmpty()) fail("E_COMPONENT","A prerequisite needs a verifiable condition")
            val provider=v.string("provider")
            val env=v["environmentId"]?.jsonPrimitive?.content
            val resource=v["resource"]?.jsonPrimitive?.content
            when(provider) {
                "environment" -> if(env !in setOf("java","cpp","c","python") || resource!=null || packages.isNotEmpty()) fail("E_COMPONENT","Unsupported environment provider")
                "ubuntu-apt" -> if(packages.isEmpty() || env!=null || resource!=null) fail("E_COMPONENT","APT prerequisites require package names")
                "kit-resource" -> if(resource==null || !resource.matches(Regex("resources/[A-Za-z0-9_.-]+(/[A-Za-z0-9_.-]+)*")) || resource.split('/').any { it==".." || it=="." } || env!=null) fail("E_PATH","Setup resource")
                else -> fail("E_COMPONENT","Unknown prerequisite provider")
            }
            val bytes=v["bytes"].let { if(it==JsonNull) null else (it as? JsonPrimitive)?.takeUnless { it.isString }?.longOrNull ?: fail("E_COMPONENT","bytes") }
            if(bytes!=null && bytes<0) fail("E_COMPONENT","bytes")
            KitRequirement(id,v.string("name"),v.string("version"),v.string("source"),provider,files,commands,packages,env,resource,bytes,anyCommands,dependsOn,pythonModules,"dependsOn" in v)
        }
        return checked(parsed)
    }
    private fun checked(requirements: List<KitRequirement>): List<KitRequirement> {
        val byId=requirements.associateBy { it.id }
        if(byId.size!=requirements.size) fail("E_COMPONENT","Duplicate prerequisite ID across setup components")
        val visited=mutableSetOf<String>();val visiting=mutableSetOf<String>()
        fun visit(id: String) {
            if(id in visited) return
            if(!visiting.add(id)) fail("E_COMPONENT","Prerequisite dependency cycle at $id")
            val r=byId[id] ?: fail("E_COMPONENT","Unknown prerequisite dependency: $id")
            r.dependsOn.forEach(::visit)
            visiting.remove(id);visited.add(id)
        }
        requirements.forEach { visit(it.id) }
        return requirements
    }
    /** Topological closure; environment requirements participate in review but are never installers. */
    fun selection(requirements: List<KitRequirement>, requested: Set<String>): List<KitRequirement> {
        checked(requirements)
        val byId=requirements.associateBy { it.id };val chosen=linkedSetOf<String>();val visiting=mutableSetOf<String>();val grouping=mutableSetOf<String>()
        fun choose(id: String,grouped: Boolean=false) {
            if(id in chosen) return
            val r=byId[id] ?: fail("E_COMPONENT","Unknown selected prerequisite: $id")
            if(!grouped && !r.explicitDependencies && r.provider=="kit-resource") {
                val resource=r.resource ?: fail("E_COMPONENT","Legacy grouped installer has no resource")
                if(!grouping.add(resource)) fail("E_COMPONENT","Legacy installer dependency cycle")
                requirements.filter { it.provider=="kit-resource" && it.resource==resource }.forEach { choose(it.id,true) }
                grouping.remove(resource)
                return
            }
            if(!visiting.add(id)) fail("E_COMPONENT","Implicit prerequisite cycle at $id")
            val dependencies=if(r.explicitDependencies || r.provider=="environment") r.dependsOn else requirements.filter { it.provider=="environment" }.map { it.id }
            dependencies.forEach { if(it !in chosen) choose(it) }
            visiting.remove(id);chosen.add(id)
        }
        requested.forEach { choose(it) }
        return chosen.map(byId::getValue)
    }
    fun forPack(pack: VerifiedPack): List<KitRequirement> = checked(pack.declarations().filter { it.type=="setup" }.flatMap { parse(it.descriptor) })
}
