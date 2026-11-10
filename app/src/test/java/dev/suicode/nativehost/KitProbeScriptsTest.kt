package dev.suicode.nativehost

import dev.suicode.packages.KitRequirement
import org.junit.Test
import org.junit.Assert.*
import java.nio.file.Files

class KitProbeScriptsTest {
    @Test fun dottedModulePresenceNeverExecutesTheParentPackage() {
        val root=Files.createTempDirectory("passive-module").toFile()
        try {
            val marker=root.resolve("executed")
            val pkg=root.resolve("probe_evil").apply { mkdir() }
            pkg.resolve("__init__.py").writeText("from pathlib import Path\nPath(${quotePython(marker.path)}).write_text('executed')\n")
            pkg.resolve("child.py").writeText("raise RuntimeError('must never import')\n")
            val process=ProcessBuilder("/bin/bash","-c",KitProbeScripts.modulePresence("probe_evil.child",listOf(root.path))).redirectErrorStream(true).start()
            val output=process.inputStream.bufferedReader().readText()
            assertEquals(output,0,process.waitFor());assertFalse(marker.exists())
        } finally { root.deleteRecursively() }
    }
    @Test fun versionDiscoveryNeverRunsTheDeclaredExecutable() {
        val root=Files.createTempDirectory("passive-version").toFile()
        try {
            val marker=root.resolve("executed");val binary=root.resolve("untrusted_probe_tool")
            binary.writeText("#!/bin/sh\ntouch '${marker.path}'\n");binary.setExecutable(true)
            val r=KitRequirement("tool","Tool","unknown","https://example.org","ubuntu-apt",emptyList(),listOf(binary.name),listOf("missing-package-for-probe"),null,null,null)
            val builder=ProcessBuilder("/bin/bash","-c",KitProbeScripts.versionMetadata(r)).redirectErrorStream(true)
            builder.environment()["PATH"]=root.path+":"+System.getenv("PATH")
            val process=builder.start();val output=process.inputStream.bufferedReader().readText()
            assertEquals(output,0,process.waitFor());assertFalse(marker.exists())
        } finally { root.deleteRecursively() }
    }
    private fun quotePython(value: String)="'"+value.replace("\\","\\\\").replace("'","\\'")+"'"
}
