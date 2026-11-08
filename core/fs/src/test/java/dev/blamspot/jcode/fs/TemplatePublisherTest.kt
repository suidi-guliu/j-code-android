package dev.blamspot.jcode.fs

import java.nio.file.Files
import kotlin.test.*
import org.junit.Test

class TemplatePublisherTest {
    private fun fixture(block: (java.io.File)->Unit) {
        val root=Files.createTempDirectory("native-template-").toFile()
        try { block(root) } finally { root.deleteRecursively() }
    }
    @Test fun createsNestedSourceAndRetainsExistingProjectMetadata()=fixture { root ->
        val metadata=java.io.File(root,".jcode/project.yaml").apply { parentFile!!.mkdirs();writeText("existing") }
        TemplatePublisher.apply(root,mapOf("中文/src/main.txt" to "source".toByteArray(),"build.txt" to "build".toByteArray()))
        assertEquals("source",java.io.File(root,"中文/src/main.txt").readText());assertEquals("existing",metadata.readText())
    }
    @Test fun collisionRejectsBeforePublishingAnyFile()=fixture { root ->
        java.io.File(root,"b").writeText("keep")
        assertFails { TemplatePublisher.apply(root,mapOf("a" to byteArrayOf(1),"b" to byteArrayOf(2))) }
        assertFalse(java.io.File(root,"a").exists());assertEquals("keep",java.io.File(root,"b").readText())
    }
    @Test fun traversalAndSymlinkParentsAreRejected()=fixture { root ->
        assertFails { TemplatePublisher.apply(root,mapOf("../outside" to byteArrayOf(1))) }
        Files.createSymbolicLink(root.resolve("link").toPath(),root.toPath())
        assertFails { TemplatePublisher.apply(root,mapOf("link/a" to byteArrayOf(1))) }
    }
    @Test fun failureRollsBackPublishedFilesButPreservesUserEdits()=fixture { root ->
        assertFails { TemplatePublisher.applyChecked(root,mapOf("a" to "generated".toByteArray(),"b" to byteArrayOf(2))) { name -> if(name=="b") { root.resolve("a").writeText("user changed it");throw java.io.IOException("interrupted") } } }
        assertEquals("user changed it",root.resolve("a").readText());assertFalse(root.resolve("b").exists())
        root.resolve("a").delete()
        assertFails { TemplatePublisher.applyChecked(root,mapOf("a" to byteArrayOf(1),"b" to byteArrayOf(2))) { if(it=="b") throw java.io.IOException("interrupted") } }
        assertFalse(root.resolve("a").exists())
    }
}
