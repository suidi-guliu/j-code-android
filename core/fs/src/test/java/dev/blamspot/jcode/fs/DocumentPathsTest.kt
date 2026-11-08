package dev.blamspot.jcode.fs
import java.nio.file.Files
import java.io.File
import kotlin.test.*
class DocumentPathsTest {
    private fun fixture(body: (File,DocumentPaths)->Unit) { val r=Files.createTempDirectory("sui-docs").toFile();try { body(r,DocumentPaths(r)) } finally { SafeLocalFiles.remove(r) } }
    @Test fun unicodeSpacesAndSpecialCharactersStayInsideGrant()=fixture { r,p -> val f=File(r,"中文 + # % ;.java");f.writeText("keep");assertEquals(f,p.resolve(f.path));assertEquals(f.name,DocumentPaths.name(f.name)) }
    @Test fun parentTraversalAndSiblingPrefixAreRejected()=fixture { r,p -> assertFails { p.resolve(r.path+"/../secret") };assertFails { p.resolve(r.path+"-sibling/x") };assertFails { p.resolve("relative") } }
    @Test fun mutationNamesCannotEscapeAParentGrant() { for(n in listOf("../secret","a/b",".","..","/x","\u0000")) assertFails { DocumentPaths.name(n) } }
    @Test fun symlinksAreNotTraversedEvenWithinGrant()=fixture { r,p -> val target=File(r,"target").apply { mkdir() };Files.createSymbolicLink(File(r,"link").toPath(),target.toPath());assertFails { p.resolve(File(r,"link/new.txt").path) };assertTrue(target.isDirectory) }
}
