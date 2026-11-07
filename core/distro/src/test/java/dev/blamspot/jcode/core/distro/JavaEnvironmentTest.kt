package dev.blamspot.jcode.core.distro

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JavaEnvironmentTest {
    private val checksum = "a".repeat(64)
    private fun assets(type: String = "ga", arch: String = "aarch64", checksum: String = this.checksum) = """
        [{"release_type":"$type","version_data":{"major":25,"openjdk_version":"25.0.1+8"},
          "binaries":[{"os":"linux","architecture":"$arch","image_type":"jdk","jvm_impl":"hotspot",
          "package":{"name":"OpenJDK25U-jdk_aarch64_linux_hotspot_25.0.1_8.tar.gz",
          "link":"https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.1%2B8/jdk.tar.gz",
          "checksum":"$checksum","size":123456789}}]}]
    """.trimIndent()

    private fun release(checksum: String = this.checksum) = JavaReleaseProvider.parseRelease(assets(checksum = checksum), "aarch64", 25)

    @Test fun selectsLatestGaRatherThanLatestLtsOrTip() {
        assertEquals(26, JavaReleaseProvider.latestFeature("""{"most_recent_feature_release":26,"most_recent_lts":25,"tip_version":27}"""))
        assertEquals("aarch64", JavaReleaseProvider.architecture("arm64"))
        assertFailsWith<IllegalStateException> { JavaReleaseProvider.architecture("riscv64") }
        assertEquals("25.0.1+8", release().version)
    }

    @Test fun rejectsEaWrongArchitectureAndWrongMajor() {
        assertFailsWith<IllegalStateException> { JavaReleaseProvider.parseRelease(assets("ea"), "aarch64", 25) }
        assertFailsWith<IllegalStateException> { JavaReleaseProvider.parseRelease(assets(arch = "x64"), "aarch64", 25) }
        assertFailsWith<IllegalStateException> { JavaReleaseProvider.parseRelease(assets(), "aarch64", 26) }
    }

    @Test fun failsClosedOnMissingChecksumAndUntrustedSource() {
        assertFailsWith<IllegalArgumentException> { release("invalid") }
        assertFailsWith<IllegalArgumentException> { release().copy(downloadUrl = "https://example.com/jdk.tar.gz") }
        assertFailsWith<IllegalArgumentException> { release().copy(downloadUrl = "http://github.com/adoptium/temurin25-binaries/releases/download/x/x.tar.gz") }
        assertFailsWith<IllegalArgumentException> { release().copy(fileName = "../jdk.tar.gz") }
        assertFailsWith<IllegalArgumentException> { release().copy(version = "25'; touch /tmp/injected") }
    }

    @Test fun quickRunUsesManagedEnvironmentAndQuotesTheFile() {
        assertEquals(setOf("java"), JavaLanguageAdapter.extensions)
        val command = JavaLanguageAdapter.command("/workspace/a b/it's.java")
        assertTrue(command.contains("MANAGED_ENV=/opt/sui-code/java.env"))
        assertTrue(command.contains("'/workspace/a b/it'\\''s.java'"))
        assertFailsWith<IllegalArgumentException> { JavaLanguageAdapter.command("relative.java") }
    }

    /** Exercises the production shell's download, checksum, staging, commit, log and failure paths.
     * Fake JDK tools isolate installer semantics; the real JDK smoke is validated separately. */
    private class Fixture : AutoCloseable {
        val root = Files.createTempDirectory("sui-java-test").toFile()
        val base = File(root, "managed")
        val tools = File(root, "tools").apply { mkdirs() }
        val archive = File(root, "jdk.tar.gz")
        init {
            val jdk = File(root, "archive/jdk/bin").apply { mkdirs() }
            executable(File(jdk, "java"), "[ \"\${1:-}\" = '-version' ] && exit 0; echo 'HelloWorld from sui-code'")
            executable(File(jdk, "javac"), "exit 0")
            executable(File(jdk, "jshell"), "[ \"\${1:-}\" = '--version' ] && exit 0; cat >/dev/null; echo SUI_JSHELL_OK")
            check(ProcessBuilder("tar", "-czf", archive.path, "-C", File(root, "archive").path, "jdk").start().waitFor() == 0)
            executable(File(tools, "uname"), "echo aarch64")
            executable(File(tools, "curl"), "while [ \"\$1\" != '-o' ]; do shift; done; cp '${archive.path}' \"\$2\"")
        }
        fun checksum(): String = MessageDigest.getInstance("SHA-256").digest(archive.readBytes()).joinToString("") { "%02x".format(it) }
        fun script(release: JavaRelease): String = JavaInstallScript.build(release)
            .replace("/opt/sui-code", base.path)
            .replace("/etc/profile.d/sui-code-java.sh", File(root, "profile.sh").path)
        fun start(release: JavaRelease): Process {
            val script = File(root, "install.sh").apply { writeText(script(release)) }
            return ProcessBuilder("bash", script.path).apply {
                environment()["PATH"] = tools.path + ":" + System.getenv("PATH")
                redirectErrorStream(true)
                redirectOutput(File(root, "output.log"))
            }.start()
        }
        fun output() = File(root, "output.log").readText()
        override fun close() { root.deleteRecursively() }
        companion object {
            fun executable(file: File, body: String) { file.writeText("#!/bin/sh\n$body\n"); file.setExecutable(true) }
        }
    }

    @Test fun installsAfterChecksumAndSmokeWithoutOverwritingExistingJdk() = Fixture().use { f ->
        val r = release(f.checksum())
        assertEquals(0, f.start(r).waitFor(), f.output())
        val home = File(f.base, "jdks/temurin-${r.version}-aarch64")
        assertTrue(File(home, "bin/java").isFile)
        assertTrue(File(f.base, "java.env").readText().contains(home.path))
        assertTrue(f.output().contains("HelloWorld from sui-code"))
        File(home, "keep").writeText("original")
        assertEquals(1, f.start(r).waitFor())
        assertEquals("original", File(home, "keep").readText())
    }

    @Test fun checksumFailureLeavesNoInstalledJdkAndCanRetry() = Fixture().use { f ->
        assertEquals(1, f.start(release()).waitFor())
        assertFalse(File(f.base, "java.env").exists())
        assertTrue(File(f.base, "jdks").listFiles().orEmpty().isEmpty())
        assertEquals(0, f.start(release(f.checksum())).waitFor(), f.output())
    }

    @Test fun failedJavaValidationNeverActivatesOrLeavesPartialInstall() = Fixture().use { f ->
        val source = File(f.root, "archive/jdk/bin/java")
        Fixture.executable(source, "exit 42")
        check(ProcessBuilder("tar", "-czf", f.archive.path, "-C", File(f.root, "archive").path, "jdk").start().waitFor() == 0)
        assertEquals(42, f.start(release(f.checksum())).waitFor(), f.output())
        assertFalse(File(f.base, "java.env").exists())
        assertTrue(File(f.base, "jdks").listFiles().orEmpty().isEmpty())
    }

    @Test fun architectureMismatchFailsBeforeDownload() = Fixture().use { f ->
        Fixture.executable(File(f.tools, "uname"), "echo x86_64")
        assertEquals(1, f.start(release()).waitFor())
        assertFalse(f.base.exists())
        assertTrue(f.output().contains("architecture changed"))
    }

    @Test fun downloadFailurePreservesThePreviouslySelectedEnvironment() = Fixture().use { f ->
        f.base.mkdirs()
        val env = File(f.base, "java.env").apply { writeText("export JAVA_HOME=/existing/jdk\n") }
        Fixture.executable(File(f.tools, "curl"), "exit 22")
        assertEquals(22, f.start(release()).waitFor())
        assertEquals("export JAVA_HOME=/existing/jdk\n", env.readText())
        assertTrue(File(f.base, "jdks").listFiles().orEmpty().isEmpty())
    }

    @Test fun terminationCleansStagingAndReleasesLockForRetry() = Fixture().use { f ->
        Fixture.executable(File(f.tools, "curl"), "kill -TERM \"\$PPID\"; exit 1")
        assertEquals(143, f.start(release()).waitFor(), f.output())
        assertFalse(File(f.base, "java.env").exists())
        assertTrue(File(f.base, "jdks").listFiles().orEmpty().isEmpty())
        Fixture.executable(File(f.tools, "curl"), "while [ \"\$1\" != '-o' ]; do shift; done; cp '${f.archive.path}' \"\$2\"")
        assertEquals(0, f.start(release(f.checksum())).waitFor(), f.output())
    }
}
