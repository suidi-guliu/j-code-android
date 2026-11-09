package dev.blamspot.jcode.core.distro

import java.io.File
import java.util.Properties
import java.util.UUID
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Host-owned metadata only. Runtime and IDE logs have disjoint task types; log bytes stay in Ubuntu. */
class InstallationLogArchive(private val root: File) {
    data class Entry(val id: String,val kind: String,val component: String,val source: String,val distro: String,val time: Long,val outcome: String,val file: String)
    @Synchronized fun record(kind: String,component: String,source: String,distro: String,outcome: String,log: File) {
        require(kind in setOf("environment","ide-component"))
        root.mkdirs()
        val entry=Entry(UUID.randomUUID().toString(),kind,component,source,distro,System.currentTimeMillis(),outcome.take(2048),log.absolutePath)
        val data=Properties().apply { setProperty("id",entry.id);setProperty("kind",kind);setProperty("component",component);setProperty("source",source);setProperty("distro",distro);setProperty("time",entry.time.toString());setProperty("outcome",entry.outcome);setProperty("file",entry.file) }
        val temp=File.createTempFile("log-",".tmp",root)
        try { temp.outputStream().use { data.store(it,"sui-code installation log metadata v1") };Files.move(temp.toPath(),File(root,entry.id+".properties").toPath(),StandardCopyOption.ATOMIC_MOVE) }
        finally { temp.delete() }
    }
    @Synchronized fun entries(kind: String): List<Entry> = root.listFiles().orEmpty().filter { it.extension=="properties" }.mapNotNull { file ->
        runCatching { val p=Properties().apply { file.inputStream().use(::load) };Entry(p.getProperty("id"),p.getProperty("kind"),p.getProperty("component"),p.getProperty("source"),p.getProperty("distro"),p.getProperty("time").toLong(),p.getProperty("outcome"),p.getProperty("file")) }.getOrNull()
    }.filter { it.kind==kind }.sortedByDescending { it.time }
    fun tail(entry: Entry): String {
        val file=File(entry.file)
        return java.io.RandomAccessFile(file,"r").use { input -> input.seek((input.length()-65536).coerceAtLeast(0));ByteArray((input.length()-input.filePointer).toInt()).also { input.readFully(it) }.toString(Charsets.UTF_8) }
    }
}
