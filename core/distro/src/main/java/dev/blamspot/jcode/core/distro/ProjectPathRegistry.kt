package dev.blamspot.jcode.core.distro

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Stable guest identities for paths which build tools cannot represent (CMake lists/JVM classpath).
 * No project/file is moved. Choose the first unsafe ancestor, so nested projects share one mapping
 * regardless of the order in which they are opened. Original /workspace paths remain accessible.
 */
object ProjectPathRegistry {
    const val GUEST_ROOT="/sui-projects"
    private val roots=ConcurrentHashMap<String,String>()
    private fun unsafe(name: String)=name.any { it in ";:$'\"`\\\r\n" }
    fun register(directory: File, projectsRoot: String = WorkspaceHostPaths.projectsRoot) {
        val path=directory.canonicalFile.toPath()
        if(!directory.isDirectory) return
        val projects=File(projectsRoot).canonicalFile.toPath()
        val base=if(path.startsWith(projects)) projects else path.root
        var ancestor=base
        for(segment in base.relativize(path)) {
            ancestor=ancestor.resolve(segment)
            if(unsafe(segment.toString())) {
                val source=ancestor.toString()
                roots.computeIfAbsent(source) {
                    check(roots.size<4096) { "E_TASK_PATH: too many active tool-safe directory mappings" }
                    GUEST_ROOT+"/"+java.security.MessageDigest.getInstance("SHA-256").digest(source.toByteArray()).joinToString("") { "%02x".format(it) }
                }
                return
            }
        }
    }
    fun hostToGuest(path: String): String? = roots.entries.filter { path==it.key || path.startsWith(it.key+"/") }.maxByOrNull { it.key.length }?.let { it.value+path.removePrefix(it.key) }
    fun guestToHost(path: String): String? = roots.entries.firstOrNull { path==it.value || path.startsWith(it.value+"/") }?.let {
        val source=File(it.key).toPath();val result=source.resolve(path.removePrefix(it.value).removePrefix("/")).normalize()
        require(result.startsWith(source)) { "E_TASK_PATH: guest path escaped its project mapping" }
        result.toString()
    }
    fun binds(): List<DistroBind> = roots.entries.filter { File(it.key).isDirectory }.sortedBy { it.key }.map { DistroBind(it.key,it.value) }
}
