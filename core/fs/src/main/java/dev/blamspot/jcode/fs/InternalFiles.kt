package dev.blamspot.jcode.fs

import java.io.File

/** Persistent app-private storage, independent of Workspace/Project database records. The existing
 * projects location is retained so Project creation, backup and export keep seeing the same files. */
object InternalFiles {
    fun protectsProject(file: File, registeredRoots: Set<String>): Boolean {
        if (registeredRoots.isEmpty()) return false
        val path = runCatching { file.canonicalPath.trimEnd(File.separatorChar) }.getOrElse { return true }
        return registeredRoots.any { root ->
            val protected = runCatching { File(root).canonicalPath }.getOrElse { return true }
            protected == path || protected.startsWith(path + File.separator)
        }
    }

    const val ROOT_NODE_ID: Long = Long.MIN_VALUE
    fun root(filesDir: File): File = File(filesDir, "workspace/projects").apply { if (!isDirectory) mkdirs() }
}
