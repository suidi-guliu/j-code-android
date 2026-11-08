package dev.blamspot.jcode.core.distro

import java.net.HttpURLConnection
import java.net.URI

/** Official CPython tag feed, not a third-party version registry or a packaged environment. */
object PythonReleaseProvider {
    const val SOURCE="https://github.com/python/cpython/tags.atom"
    fun parse(feed: String): String {
        require(feed.length<=1024*1024)
        return Regex("<title>v(3\\.[0-9]{1,3}\\.[0-9]{1,3})</title>").findAll(feed).map { it.groupValues[1] }
            .maxWithOrNull { a,b -> a.split('.').map(String::toInt).zip(b.split('.').map(String::toInt)).firstOrNull { it.first!=it.second }?.let { it.first.compareTo(it.second) } ?: 0 }
            ?: error("No stable Python 3 release was found in the official CPython feed")
    }
    fun latest(): String {
        val c=URI(SOURCE).toURL().openConnection() as HttpURLConnection
        return try {
            c.connectTimeout=15000;c.readTimeout=20000;c.instanceFollowRedirects=false
            check(c.responseCode==200) { "Official CPython query HTTP ${c.responseCode}" }
            val bytes=c.inputStream.use { it.readNBytes(1024*1024+1) }
            check(bytes.size<=1024*1024) { "Release feed exceeds limit" }
            parse(bytes.toString(Charsets.UTF_8))
        } finally { c.disconnect() }
    }
}
