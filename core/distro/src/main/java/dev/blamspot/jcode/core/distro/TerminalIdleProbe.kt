package dev.blamspot.jcode.core.distro

import java.io.File
import java.nio.file.Files
import java.nio.file.NoSuchFileException

/** Conservative kernel evidence. Unknown/restricted /proc data means busy, never permission to kill. */
object TerminalIdleProbe {
    data class Proof(val idle: Boolean,val detail: String)
    fun idle(shellPid: Int,expectedHostDirectory: String?=null,proc: File=File("/proc")): Boolean = inspect(shellPid,expectedHostDirectory,proc).idle
    fun inspect(shellPid: Int,expectedHostDirectory: String?=null,proc: File=File("/proc")): Proof = runCatching {
        require(shellPid>0)
        val uid=(Files.getAttribute(File(proc,"self").toPath(),"unix:uid") as Number).toLong()
        check((Files.getAttribute(File(proc,shellPid.toString()).toPath(),"unix:uid") as Number).toLong()==uid)
        var fallback: Map<Int,List<Int>>?=null
        fun children(pid: Int): List<Int> {
            val direct=File(proc,"$pid/task/$pid/children")
            if(direct.isFile && direct.canRead()) return direct.readText().trim().split(Regex("\\s+")).filter(String::isNotEmpty).map(String::toInt)
            // Android/Linux kernels may omit CONFIG_CHECKPOINT_RESTORE's children pseudo-file.
            // Build the same relationships from stat, inspecting only this app UID.
            if(fallback==null) {
                val parents=HashMap<Int,MutableList<Int>>()
                for(directory in proc.listFiles() ?: error("Cannot enumerate owned processes")) {
                    val child=directory.name.toIntOrNull() ?: continue
                    try {
                        if((Files.getAttribute(directory.toPath(),"unix:uid") as Number).toLong()!=uid) continue
                        val stat=File(directory,"stat").readText()
                        val parent=stat.substring(stat.lastIndexOf(')')+2).split(' ')[1].toInt()
                        parents.getOrPut(parent) { mutableListOf() }.add(child)
                    } catch(_: NoSuchFileException) { /* exited during the snapshot */ }
                    catch(e: java.io.FileNotFoundException) { if(directory.exists()) throw e }
                }
                fallback=parents
            }
            return fallback!![pid].orEmpty()
        }
        val pending=java.util.ArrayDeque<Int>().apply { add(shellPid) };val seen=HashSet<Int>();var leaves=0
        while(pending.isNotEmpty()) {
            val pid=pending.removeFirst();check(seen.add(pid) && seen.size<=16)
            val directory=File(proc,pid.toString());val comm=File(directory,"comm").readText().trim()
            check(comm in setOf("bash","sh","dash","su","env","proot","libproot.so")) { "Active or unknown process: $pid $comm" }
            val children=children(pid)
            if(children.isEmpty()) {
                check(comm in setOf("bash","sh","dash"));leaves++
                if(expectedHostDirectory!=null) check(File(directory,"cwd").canonicalFile==File(expectedHostDirectory).canonicalFile)
            } else pending.addAll(children)
        }
        Proof(leaves==1,"$leaves idle shell leaves")
    }.getOrElse { Proof(false,it.message ?: it.javaClass.simpleName) }
}
