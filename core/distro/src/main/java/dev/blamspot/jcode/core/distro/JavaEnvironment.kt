package dev.blamspot.jcode.core.distro

import java.net.HttpURLConnection
import java.net.URI
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings

/** A resolved GA release is immutable: confirmation installs exactly these bytes, never "latest". */
data class JavaRelease(
    val version: String,
    val architecture: String,
    val fileName: String,
    val downloadUrl: String,
    val sha256: String,
    val size: Long,
) {
    init {
        require(version.matches(Regex("[0-9][0-9A-Za-z.+_-]*")))
        require(architecture in setOf("aarch64", "x64"))
        require(fileName.matches(Regex("[A-Za-z0-9._+-]+\\.tar\\.gz")))
        val uri = URI(downloadUrl)
        require(uri.scheme == "https" && uri.host == "github.com" && uri.userInfo == null &&
            uri.rawQuery == null && uri.fragment == null &&
            uri.path.matches(Regex("/adoptium/temurin[0-9]+-binaries/releases/download/.+"))) {
            "The JDK archive must come from an official Adoptium GitHub release."
        }
        require(sha256.matches(Regex("[a-fA-F0-9]{64}")))
        require(size > 0)
    }

    val source: String get() = "Eclipse Adoptium / Temurin OpenJDK (GA, HotSpot, Linux $architecture)"
    val home: String get() = "/opt/sui-code/jdks/temurin-$version-$architecture"
}

object JavaReleaseProvider {
    const val API = "https://api.adoptium.net/v3"
    private fun document(text: String): Any? = Load(
        LoadSettings.builder().setAllowDuplicateKeys(false).build(),
    ).loadFromString(text)

    fun architecture(rootfsKey: String): String = when (rootfsKey) {
        "arm64" -> "aarch64"
        "amd64" -> "x64"
        else -> error("Unsupported Linux architecture: $rootfsKey")
    }

    fun latestFeature(json: String): Int {
        val releases = document(json) as? Map<*, *> ?: error("Invalid release information")
        val feature = (releases["most_recent_feature_release"] as? Number)?.toInt()
            ?: error("Missing latest GA release")
        require(feature >= 21)
        return feature
    }

    fun parseRelease(json: String, architecture: String, feature: Int): JavaRelease {
        val assets = document(json) as? List<*> ?: error("Invalid JDK assets")
        return assets.mapNotNull { it as? Map<*, *> }.mapNotNull { asset ->
            val version = asset["version_data"] as? Map<*, *> ?: return@mapNotNull null
            val binary = (asset["binaries"] as? List<*>)?.mapNotNull { it as? Map<*, *> }
                ?.firstOrNull { it["os"] == "linux" && it["architecture"] == architecture &&
                    it["image_type"] == "jdk" && it["jvm_impl"] == "hotspot" } ?: return@mapNotNull null
            if (asset["release_type"] != "ga" || (version["major"] as? Number)?.toInt() != feature ||
                binary["os"] != "linux" || binary["architecture"] != architecture ||
                binary["image_type"] != "jdk" || binary["jvm_impl"] != "hotspot") return@mapNotNull null
            val pkg = binary["package"] as? Map<*, *> ?: return@mapNotNull null
            JavaRelease(
                version["openjdk_version"] as? String ?: error("Missing JDK version"), architecture,
                pkg["name"] as? String ?: error("Missing archive name"),
                pkg["link"] as? String ?: error("Missing archive URL"),
                pkg["checksum"] as? String ?: error("Missing SHA-256"),
                (pkg["size"] as? Number)?.toLong() ?: error("Missing archive size"),
            )
        }.firstOrNull() ?: error("No compatible GA JDK is available; no third-party fallback is used.")
    }

    /** Runs on Dispatchers.IO. Timeouts bound discovery; redirects are rejected for metadata. */
    fun latest(rootfsKey: String): JavaRelease {
        val arch = architecture(rootfsKey)
        val feature = latestFeature(get("$API/info/available_releases"))
        return parseRelease(get("$API/assets/feature_releases/$feature/ga?architecture=$arch&image_type=jdk&jvm_impl=hotspot&os=linux&vendor=eclipse&page_size=1&sort_order=DESC"), arch, feature)
    }

    private fun get(url: String): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            check(connection.responseCode == 200) { "Adoptium query failed (HTTP ${connection.responseCode}); retry when connected." }
            connection.inputStream.bufferedReader().use { reader ->
                val text = reader.readText()
                require(text.length <= 2_000_000) { "Release response is too large" }
                text
            }
        } finally { connection.disconnect() }
    }
}

/** Contract shared with the future native QuickRun dispatcher; no extension marketplace needed. */
interface NativeLanguageAdapter {
    val id: String
    val extensions: Set<String>
    fun command(guestFile: String): String
}

object JavaLanguageAdapter : NativeLanguageAdapter {
    override val id = "java"
    override val extensions = setOf("java")
    override fun command(guestFile: String): String {
        require(guestFile.startsWith('/') && guestFile.endsWith(".java") && '\u0000' !in guestFile)
        // Source-file launch handles packages and keeps generated classes off shared storage.
        return "set -e; . /opt/sui-code/java.env; exec \"\$JAVA_HOME/bin/java\" ${shellQuote(guestFile)}"
    }
}

internal fun shellQuote(value: String) = "'" + value.replace("'", "'\\''") + "'"
