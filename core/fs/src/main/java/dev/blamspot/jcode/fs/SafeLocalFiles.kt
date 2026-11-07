package dev.blamspot.jcode.fs

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption

/** Local mutations never truncate an existing document before its replacement is safely written. */
internal object SafeLocalFiles {
    private fun child(parent: File, relative: String, nested: Boolean): File {
        require(relative.isNotBlank() && '\u0000' !in relative && !File(relative).isAbsolute) { "Invalid relative name" }
        val parts = relative.split('/')
        require(parts.all { it.isNotEmpty() && it != "." && it != ".." }) { "Names cannot escape the selected folder" }
        require(nested || parts.size == 1) { "Rename accepts one name, not a path" }
        val base = parent.canonicalFile.toPath()
        check(Files.isDirectory(base)) { "Parent folder does not exist" }
        var next = base
        for (part in parts) {
            next = next.resolve(part)
            require(!Files.isSymbolicLink(next)) { "Cannot create or replace a symbolic link" }
            require(next.normalize().startsWith(base)) { "Path escapes the selected folder" }
        }
        return next.toFile()
    }

    fun create(parent: File, relative: String, directory: Boolean): File {
        val target = child(parent, relative, nested = true).toPath()
        check(!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) { "An entry with that name already exists" }
        Files.createDirectories(target.parent)
        if (directory) Files.createDirectory(target) else Files.createFile(target)
        return target.toFile()
    }

    fun rename(source: File, name: String): File {
        val target = child(source.parentFile ?: error("No parent folder"), name, nested = false)
        check(!Files.exists(target.toPath(), LinkOption.NOFOLLOW_LINKS)) { "An entry with that name already exists" }
        Files.move(source.toPath(), target.toPath())
        return target
    }

    fun remove(file: File) {
        if (!Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)) return
        Files.walkFileTree(file.toPath(), object : java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
            override fun visitFile(path: java.nio.file.Path, attrs: java.nio.file.attribute.BasicFileAttributes): java.nio.file.FileVisitResult {
                Files.delete(path); return java.nio.file.FileVisitResult.CONTINUE
            }
            override fun postVisitDirectory(path: java.nio.file.Path, error: java.io.IOException?): java.nio.file.FileVisitResult {
                if (error != null) throw error
                Files.delete(path); return java.nio.file.FileVisitResult.CONTINUE
            }
        })
    }

    fun copy(source: File, parent: File, name: String): File {
        val target = child(parent, name, nested = false).toPath()
        check(!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) { "An entry with that name already exists" }
        val src = source.toPath()
        if (Files.isDirectory(src, LinkOption.NOFOLLOW_LINKS)) {
            require(!target.normalize().startsWith(source.canonicalFile.toPath())) { "Cannot copy a folder into itself" }
        }
        val stage = Files.createTempDirectory(target.parent, ".sui-copy-")
        val staged = stage.resolve("payload")
        try {
            if (Files.isDirectory(src, LinkOption.NOFOLLOW_LINKS)) {
                Files.walkFileTree(src, object : java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
                    override fun preVisitDirectory(dir: java.nio.file.Path, attrs: java.nio.file.attribute.BasicFileAttributes): java.nio.file.FileVisitResult {
                        Files.createDirectory(staged.resolve(src.relativize(dir)))
                        return java.nio.file.FileVisitResult.CONTINUE
                    }
                    override fun visitFile(file: java.nio.file.Path, attrs: java.nio.file.attribute.BasicFileAttributes): java.nio.file.FileVisitResult {
                        Files.copy(file, staged.resolve(src.relativize(file)), LinkOption.NOFOLLOW_LINKS)
                        return java.nio.file.FileVisitResult.CONTINUE
                    }
                })
            } else Files.copy(src, staged, LinkOption.NOFOLLOW_LINKS)
            Files.move(staged, target)
            return target.toFile()
        } finally {
            Files.walkFileTree(stage, object : java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
                override fun visitFile(file: java.nio.file.Path, attrs: java.nio.file.attribute.BasicFileAttributes): java.nio.file.FileVisitResult {
                    Files.deleteIfExists(file); return java.nio.file.FileVisitResult.CONTINUE
                }
                override fun postVisitDirectory(dir: java.nio.file.Path, error: java.io.IOException?): java.nio.file.FileVisitResult {
                    Files.deleteIfExists(dir); return java.nio.file.FileVisitResult.CONTINUE
                }
            })
        }
    }

    fun importNew(parent: File, name: String, writer: (FileOutputStream) -> Unit): File {
        val target = child(parent, name, nested = false).toPath()
        check(!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) { "An entry with that name already exists" }
        val temp = Files.createTempFile(target.parent, ".sui-import-", ".tmp")
        try {
            FileOutputStream(temp.toFile()).use { stream -> writer(stream); stream.fd.sync() }
            Files.move(temp, target)
            return target.toFile()
        } finally { Files.deleteIfExists(temp) }
    }

    fun write(file: File, bytes: ByteArray) = replace(file) { it.write(bytes) }

    /** The writer seam also exercises interrupted I/O without corrupting the prior document. */
    fun replace(file: File, writer: (FileOutputStream) -> Unit) {
        val target = file.canonicalFile.toPath()
        Files.createDirectories(target.parent)
        if (Files.exists(target)) check(Files.isWritable(target)) { "File is read-only" }
        val temp = Files.createTempFile(target.parent, ".sui-save-", ".tmp")
        try {
            FileOutputStream(temp.toFile()).use { stream -> writer(stream); stream.fd.sync() }
            if (Files.exists(target)) {
                Files.setPosixFilePermissions(temp, Files.getPosixFilePermissions(target))
            }
            // The temp is on the same internal filesystem. Refuse unsupported atomic replacement
            // rather than falling back to truncating the user's only copy.
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally { Files.deleteIfExists(temp) }
    }
}
