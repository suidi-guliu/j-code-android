package dev.blamspot.jcode.fs

import java.io.File
import java.io.InputStream
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

/** Shared export traversal: never follows links out of a user-selected source tree. */
object ExportTree {
    fun walk(source: File,onDirectory: (String)->Unit,onFile: (String,InputStream)->Unit): Int {
        require(!Files.isSymbolicLink(source.toPath())) { "Export source must be a real folder" }
        val root=source.canonicalFile.toPath()
        require(Files.isDirectory(root)) { "Export source is unavailable" }
        var count=0
        Files.walkFileTree(root,object: SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path,attrs: BasicFileAttributes): FileVisitResult {
                if(dir!=root && dir.fileName.toString()=="node_modules") return FileVisitResult.SKIP_SUBTREE
                if(dir!=root) onDirectory(root.relativize(dir).toString())
                return FileVisitResult.CONTINUE
            }
            override fun visitFile(file: Path,attrs: BasicFileAttributes): FileVisitResult {
                if(attrs.isRegularFile) {
                    Files.newInputStream(file,LinkOption.NOFOLLOW_LINKS).use { onFile(root.relativize(file).toString(),it) }
                    count++
                }
                return FileVisitResult.CONTINUE
            }
        })
        return count
    }
}
