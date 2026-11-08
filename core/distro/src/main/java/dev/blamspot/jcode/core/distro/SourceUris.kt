package dev.blamspot.jcode.core.distro

import java.net.URI
import java.nio.file.Paths

/** File URIs use URI escaping, never form URL decoding (+ is a legal filename character). */
object SourceUris {
    fun fileUri(path: String): String = URI("file", "", path, null).toASCIIString()
    fun path(value: String): String = if(value.startsWith("file:")) {
        val uri=URI(value)
        require(uri.authority.isNullOrEmpty() || uri.authority=="localhost") { "Remote file authority is unsupported" }
        uri.path
    } else value
    fun normalize(value: String): String = Paths.get(path(value)).normalize().toString()
    fun samePath(a: String,b: String): Boolean = runCatching { normalize(a)==normalize(b) }.getOrDefault(false)
}
