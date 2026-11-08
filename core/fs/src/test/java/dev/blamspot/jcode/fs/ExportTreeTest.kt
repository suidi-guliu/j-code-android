package dev.blamspot.jcode.fs

import java.nio.file.Files
import java.io.File
import kotlin.test.*
class ExportTreeTest {
    @Test fun unicodeAndBinaryExportPreservesSourceAndPrunesOnlyRegenerableFolders() {
        val root=Files.createTempDirectory("sui-export-").toFile()
        try {
            val source=File(root,"source").apply { mkdirs() }
            val bytes=byteArrayOf(0,1,-1,65)
            File(source,"中文 space/a.bin").apply { parentFile!!.mkdirs();writeBytes(bytes) }
            File(source,"node_modules/skip.js").apply { parentFile!!.mkdirs();writeText("regenerate") }
            val dirs=mutableSetOf<String>();val files=mutableMapOf<String,ByteArray>()
            val count=ExportTree.walk(source,{dirs+=it},{name,input -> files[name]=input.readBytes()})
            assertEquals(1,count);assertTrue("中文 space" in dirs);assertContentEquals(bytes,files["中文 space/a.bin"])
            assertContentEquals(bytes,File(source,"中文 space/a.bin").readBytes())
            assertEquals("regenerate",File(source,"node_modules/skip.js").readText())
        } finally { root.deleteRecursively() }
    }
    @Test fun symlinkedDirectoriesAndFilesCannotLeakUnselectedData() {
        val root=Files.createTempDirectory("sui-export-links-").toFile()
        try {
            val source=File(root,"source").apply { mkdirs() };val outside=File(root,"private").apply { mkdirs() }
            File(outside,"secret").writeText("private")
            Files.createSymbolicLink(File(source,"folder-link").toPath(),outside.toPath())
            Files.createSymbolicLink(File(source,"file-link").toPath(),File(outside,"secret").toPath())
            assertEquals(0,ExportTree.walk(source,{error("Must not traverse a link")},{_,_->error("Must not read a link")}))
            assertEquals("private",File(outside,"secret").readText())
            Files.delete(File(source,"folder-link").toPath());Files.delete(File(source,"file-link").toPath())
        } finally { root.deleteRecursively() }
    }
}
