package dev.blamspot.jcode.core.distro

import org.junit.Test
import org.junit.Assert.*
import java.nio.file.Files

class InstallationLogArchiveTest {
    @Test fun runtimeAndIdeHistoriesRemainSeparateAfterReopeningAndKeepActualLogBytes() {
        val root=Files.createTempDirectory("sui-log-archive").toFile()
        try {
            val log=root.resolve("actual.log").apply { writeText("compiler output\n") }
            val archive=InstallationLogArchive(root.resolve("index"))
            archive.record("environment","cpp","signed Ubuntu","ubuntu","verified",log)
            archive.record("ide-component","example.kit/clangd","signed Ubuntu","ubuntu","verified",log)
            val reopened=InstallationLogArchive(root.resolve("index"))
            assertEquals(listOf("cpp"),reopened.entries("environment").map { it.component })
            assertEquals(listOf("example.kit/clangd"),reopened.entries("ide-component").map { it.component })
            assertEquals("compiler output\n",reopened.tail(reopened.entries("ide-component").single()))
            assertTrue(log.isFile)
        } finally { root.deleteRecursively() }
    }
    @Test fun persistentLogViewBoundsMemoryWithoutTruncatingTheOriginalFile() {
        val root=Files.createTempDirectory("sui-log-tail").toFile()
        try {
            val log=root.resolve("large.log").apply { writeText("a".repeat(100000)+"LAST_LINE") }
            val archive=InstallationLogArchive(root.resolve("index"))
            archive.record("ide-component","tool","https://example.org","ubuntu","failed, retry",log)
            val text=archive.tail(archive.entries("ide-component").single())
            assertEquals(65536,text.length);assertTrue(text.endsWith("LAST_LINE"));assertEquals(100009,log.length())
        } finally { root.deleteRecursively() }
    }
}
