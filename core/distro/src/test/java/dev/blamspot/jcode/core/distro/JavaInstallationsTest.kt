package dev.blamspot.jcode.core.distro

import java.nio.file.Files
import java.io.File
import kotlin.test.*

class JavaInstallationsTest {
    @Test fun onlyManagedDirectChildrenCanBeSelected() {
        for(home in listOf("/usr/lib/jvm/default-java","/opt/sui-code/jdks/../Ubuntu","/opt/sui-code/jdks/temurin-27-aarch64;echo bad")) assertFailsWith<IllegalArgumentException> { JavaInstallationScript.select(home) }
    }
    @Test fun symlinkManagementRootsCannotDeleteAnotherDirectory() {
        val root=Files.createTempDirectory("jdk-root-").toFile()
        val other=Files.createTempDirectory("user-data-").toFile()
        try {
            File(other,"keep.txt").writeText("keep")
            File(root,"opt/sui-code").mkdirs()
            Files.createSymbolicLink(File(root,"opt/sui-code/jdks").toPath(),other.toPath())
            assertFailsWith<IllegalStateException> { JavaInstallationScript.managedDirectory(root) }
            assertEquals("keep",File(other,"keep.txt").readText())
        } finally { Files.deleteIfExists(File(root,"opt/sui-code/jdks").toPath());root.deleteRecursively();other.deleteRecursively() }
    }
    @Test fun verificationPrecedesAtomicSelectionAndRetainsOldJdk() {
        val root=Files.createTempDirectory("sui-jdk-selection-").toFile()
        try {
            val home="/opt/sui-code/jdks/temurin-27-aarch64"
            val prefix=File(root,"opt/sui-code").apply { mkdirs() }
            val old=File(prefix,"jdks/temurin-21-aarch64/keep.txt").apply { parentFile!!.mkdirs();writeText("keep") }
            val selected=File(root,home.removePrefix("/"))
            for(tool in listOf("java","javac","jshell")) File(selected,"bin/$tool").apply { parentFile!!.mkdirs();writeText("#!/bin/sh\necho verified-$tool\n");setExecutable(true) }
            File(prefix,"java.env").writeText("old config")
            fun run(): Int { val script=File(root,"select.sh").apply { writeText(JavaInstallationScript.select(home).replace("/opt/sui-code",prefix.path)) };return ProcessBuilder("bash",script.path).redirectErrorStream(true).redirectOutput(File(root,"output.log")).start().waitFor() }
            File(selected,"bin/javac").writeText("#!/bin/sh\nexit 1\n")
            assertNotEquals(0,run());assertEquals("old config",File(prefix,"java.env").readText())
            File(selected,"bin/javac").writeText("#!/bin/sh\necho javac\n")
            assertEquals(0,run());assertTrue(File(prefix,"java.env").readText().contains("export JAVA_HOME="));assertEquals("keep",old.readText())
        } finally { root.deleteRecursively() }
    }
}
