package dev.blamspot.jcode.fs
import java.nio.file.Files
import kotlin.test.*
class LocalFileSearchTest {
    @Test fun searchesNestedUnicodeWithoutAProjectOrWorkspace() {
        val root=Files.createTempDirectory("sui-search").toFile()
        try { SafeLocalFiles.create(root,"任意 层/深度/Hello.java",false);assertEquals("Hello.java",LocalFileSearch.search(root,"hello").files.single().name) } finally { SafeLocalFiles.remove(root) }
    }
    @Test fun externalSymlinkIsNotTraversed() {
        val root=Files.createTempDirectory("sui-root").toFile();val outside=Files.createTempDirectory("sui-outside").toFile()
        try { java.io.File(outside,"secret.py").writeText("private");Files.createSymbolicLink(java.io.File(root,"link").toPath(),outside.toPath());assertTrue(LocalFileSearch.search(root,"secret").files.isEmpty()) } finally { SafeLocalFiles.remove(root);SafeLocalFiles.remove(outside) }
    }
    @Test fun limitedSearchIsExplicitAndCancellationStopsTraversal() {
        val root=Files.createTempDirectory("sui-search").toFile()
        try { repeat(5) { java.io.File(root,"item$it.py").writeText("") };assertTrue(LocalFileSearch.search(root,"item",2).truncated);assertFailsWith<IllegalStateException> { LocalFileSearch.search(root,"item") { error("cancel") } } } finally { SafeLocalFiles.remove(root) }
    }
}
