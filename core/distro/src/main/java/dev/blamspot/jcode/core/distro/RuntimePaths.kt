package dev.blamspot.jcode.core.distro

import java.io.File

/** Shared mount ordering: broad mounts precede nested overrides; the last explicit target wins. */
object RuntimeMounts {
    /** PRoot splits host:guest at the first colon. Stable owned aliases preserve legal host names. */
    fun addressable(bind: DistroBind, aliasRoot: File): DistroBind {
        if (':' !in bind.host) return bind
        check(':' !in aliasRoot.path && (aliasRoot.isDirectory || aliasRoot.mkdirs())) { "E_BIND_PATH: cannot prepare mount aliases" }
        val hash = java.security.MessageDigest.getInstance("SHA-256").digest(bind.host.toByteArray()).joinToString("") { "%02x".format(it) }
        val alias = File(aliasRoot, hash).toPath()
        val target = File(bind.host).toPath()
        try { java.nio.file.Files.createSymbolicLink(alias,target) }
        catch (_: java.nio.file.FileAlreadyExistsException) {
            check(java.nio.file.Files.isSymbolicLink(alias) && java.nio.file.Files.readSymbolicLink(alias)==target) { "E_BIND_PATH: invalid owned mount alias: $alias" }
        }
        return DistroBind(alias.toString(),bind.target)
    }
    fun ordered(binds: List<DistroBind>, projectsRoot: String = WorkspaceHostPaths.projectsRoot): List<DistroBind> {
        val base = File(projectsRoot).canonicalFile
        val defaults = if (base.isDirectory) listOf(DistroBind(base.path, WorkspaceHostPaths.WORKSPACE_GUEST)) else emptyList()
        return (defaults + binds + ProjectPathRegistry.binds()).associateBy { it.target.trimEnd('/').ifEmpty { "/" } }.values
            .sortedBy { it.target.count { c -> c == '/' } }
    }
}

data class NativeTaskPaths(val source: File, val directory: File, val guestSource: String, val guestDirectory: String, val binds: List<DistroBind>) {
    companion object {
        /** No task aliases: files have the same identity as LSP/DAP and persistent build databases. */
        fun resolve(file: File, project: File?, runtimeBinds: List<DistroBind>, projectsRoot: String = WorkspaceHostPaths.projectsRoot): NativeTaskPaths {
            val source = file.canonicalFile
            val directory = (project ?: source.parentFile ?: error("E_TASK_PATH: source has no parent: $source")).canonicalFile
            check(source.exists() && source.canRead()) { "E_TASK_PATH: saved source is absent or unreadable: $source" }
            check(directory.isDirectory && directory.canRead() && directory.canExecute()) { "E_TASK_PATH: working directory is absent or inaccessible: $directory" }
            check(source.isFile || (project != null && source == directory)) { "E_TASK_PATH: source is not a regular file: $source" }
            check(source.toPath().startsWith(directory.toPath())) { "E_TASK_PATH: source is outside the selected project: $source; project=$directory" }
            val root = File(projectsRoot).canonicalFile
            ProjectPathRegistry.register(directory,projectsRoot)
            fun guest(path: File): String = ProjectPathRegistry.hostToGuest(path.path) ?: if (path.toPath().startsWith(root.toPath()))
                WorkspaceHostPaths.WORKSPACE_GUEST + path.path.removePrefix(root.path) else path.path
            // Bind a colon-free ancestor; colons in descendant filenames remain literal path bytes.
            var ancestor = directory
            while (':' in ancestor.path) ancestor = ancestor.parentFile ?: error("E_TASK_PATH: no unambiguous bind ancestor")
            val bindings = RuntimeMounts.ordered(runtimeBinds + DistroBind(ancestor.path, guest(ancestor)), projectsRoot)
            return NativeTaskPaths(source, directory, guest(source), guest(directory), bindings)
        }
    }
}
