package dev.blamspot.jcode.fs

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest

/** Publishes only new files. Existing project metadata and concurrently edited files are retained. */
object TemplatePublisher {
    fun apply(root: File,files: Map<String,ByteArray>) = applyChecked(root,files) { }
    internal fun applyChecked(root: File,files: Map<String,ByteArray>,beforePublish: (String)->Unit) {
        require(files.size in 1..256 && files.values.sumOf { it.size.toLong() }<=32L*1024*1024)
        val destinations=files.mapValues { (name,_) -> SafeLocalFiles.child(root,name,true).also { check(!Files.exists(it.toPath(),LinkOption.NOFOLLOW_LINKS)) { "Template would overwrite $name" } } }
        val created=mutableListOf<Pair<File,ByteArray>>()
        try {
            for((name,data) in files.toSortedMap()) {
                beforePublish(name)
                val target=SafeLocalFiles.child(root,name,true)
                check(target==destinations[name])
                Files.createDirectories(target.parentFile!!.toPath())
                val published=SafeLocalFiles.importNew(target.parentFile!!,target.name) { it.write(data) }
                created+=published to digest(data)
            }
        } catch(e: Exception) {
            created.asReversed().forEach { (file,expected) ->
                runCatching { if(Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS) && digest(file.readBytes()).contentEquals(expected)) Files.delete(file.toPath()) }
            }
            throw e
        }
    }
    private fun digest(data: ByteArray)=MessageDigest.getInstance("SHA-256").digest(data)
}
