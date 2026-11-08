package dev.blamspot.jcode.fs

import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

object LocalFileSearch {
    data class Result(val files: List<File>,val truncated: Boolean)
    fun search(root: File,query: String,limit: Int=1000,cancel: ()->Unit={}): Result {
        require(limit in 1..10000 && query.length<=256)
        val hits=mutableListOf<File>();var visited=0;var truncated=false
        Files.walkFileTree(root.toPath(),object: SimpleFileVisitor<Path>() {
            fun examine(path: Path): FileVisitResult {
                cancel();visited++
                if(visited>50000 || hits.size>=limit) { truncated=true;return FileVisitResult.TERMINATE }
                if(path!=root.toPath() && path.fileName.toString().contains(query,ignoreCase=true)) hits+=path.toFile()
                return FileVisitResult.CONTINUE
            }
            override fun preVisitDirectory(dir: Path,attrs: BasicFileAttributes): FileVisitResult = examine(dir)
            override fun visitFile(file: Path,attrs: BasicFileAttributes): FileVisitResult = examine(file)
            override fun visitFileFailed(file: Path,exc: java.io.IOException): FileVisitResult { cancel();return FileVisitResult.CONTINUE }
        })
        return Result(hits,truncated)
    }
}
