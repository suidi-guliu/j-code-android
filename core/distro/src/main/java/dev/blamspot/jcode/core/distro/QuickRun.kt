package dev.blamspot.jcode.core.distro

import java.util.concurrent.atomic.AtomicBoolean

class QuickRunGate {
    private val held = AtomicBoolean(false)
    fun begin(repeatCount: Int): Boolean = repeatCount == 0 && held.compareAndSet(false, true)
    fun finish() { held.set(false) }
}

/** Saving must succeed before resolving a path or creating any process. Captures one editor tab. */
suspend fun prepareQuickRun(save: suspend () -> Boolean, resolve: () -> String): String {
    check(save()) { "QuickRun cancelled: the current file could not be saved, or changed during saving." }
    return resolve()
}

/** Shared, bounded shutdown policy; inventory is refreshed to catch descendants and detached children. */
internal object QuickRunCleanup {
    fun stop(
        owned: () -> Set<Int>, parents: () -> Map<Int, Int>,
        terminate: (Int) -> Unit, kill: (Int) -> Unit, pause: () -> Unit,
    ) {
        val initial = owned()
        val ancestry = parents()
        initial.filter { ancestry[it] !in initial }.forEach(terminate)
        repeat(25) {
            if (owned().isEmpty()) return
            pause()
        }
        owned().forEach(kill)
    }
}

/** PRoot separates host:guest at the first colon. Bind an unambiguous ancestor, then address
 * colon-containing directories inside the guest rather than misparsing a legal source path. */
data class QuickRunFileMount(val hostDirectory: java.io.File, val guestFile: String) {
    val guestDirectory: String get() = guestFile.substringBeforeLast('/')
    companion object {
        fun forFile(file: java.io.File, rootDirectory: java.io.File? = null): QuickRunFileMount {
            val actual = file.canonicalFile
            var parent = rootDirectory?.canonicalFile ?: actual.parentFile ?: error("Source has no parent directory")
            require(actual.toPath().startsWith(parent.toPath())) { "The task source must be inside its work directory" }
            while (':' in parent.path) parent = parent.parentFile ?: error("Cannot map this source directory into Ubuntu")
            return QuickRunFileMount(parent, "/sui-quickrun/" + actual.relativeTo(parent).invariantSeparatorsPath)
        }
    }
}
