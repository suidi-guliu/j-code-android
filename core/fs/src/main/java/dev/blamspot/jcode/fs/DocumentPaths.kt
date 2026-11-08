package dev.blamspot.jcode.fs

import java.io.File
import java.nio.file.Files

/** DocumentsProvider's URI grant is a subtree grant, never access to the rest of app storage. */
class DocumentPaths(private val root: File) {
    fun resolve(id: String): File {
        require(File(id).isAbsolute) { "Document id must be absolute" }
        require(!Files.isSymbolicLink(root.toPath())) { "Internal root cannot be a symbolic link" }
        val path=File(id).absoluteFile.toPath().normalize()
        val lexical=root.absoluteFile.toPath().normalize()
        val canonical=root.canonicalFile.toPath()
        val base=when { path.startsWith(lexical) -> lexical;path.startsWith(canonical) -> canonical;else -> error("Document outside Internal Files") }
        var current=base
        for(part in base.relativize(path)) {
            current=current.resolve(part)
            require(!Files.isSymbolicLink(current)) { "Symbolic links are not exposed through document grants" }
        }
        require(File(id).canonicalFile.toPath().startsWith(canonical)) { "Document escapes Internal Files" }
        return File(id)
    }
    companion object {
        fun name(value: String): String {
            require(value.isNotBlank() && value !in setOf(".","..") && '/' !in value && '\u0000' !in value) { "Use a single file/folder name without traversal" }
            return value
        }
    }
}
