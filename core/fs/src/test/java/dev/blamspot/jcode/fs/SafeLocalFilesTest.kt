package dev.blamspot.jcode.fs

import java.io.IOException
import java.nio.file.Files
import kotlin.test.*

class SafeLocalFilesTest {
    private fun fixture(test: (java.io.File) -> Unit) {
        val root = Files.createTempDirectory("sui-files-").toFile()
        try { test(root) } finally { SafeLocalFiles.remove(root) }
    }
    @Test fun internalRootIsStableWithoutAnyWorkspaceDatabaseOrProject() = fixture { filesDir ->
        val root = InternalFiles.root(filesDir)
        SafeLocalFiles.create(root, "loose/hello.py", false).writeText("print('saved')")
        val reopened = InternalFiles.root(java.io.File(filesDir.path))
        assertEquals(root.canonicalPath, reopened.canonicalPath)
        assertEquals("print('saved')", java.io.File(reopened,"loose/hello.py").readText())
        assertFalse(java.io.File(root,".jcode").exists())
    }
    @Test fun registeredProjectRootsAndTheirAncestorsCannotBeMovedByTheLooseFileBrowser() = fixture { root ->
        val project = SafeLocalFiles.create(root,"group/project",true)
        val registered = setOf(project.path)
        assertTrue(InternalFiles.protectsProject(root,registered))
        assertTrue(InternalFiles.protectsProject(project.parentFile,registered))
        assertTrue(InternalFiles.protectsProject(project,registered))
        assertFalse(InternalFiles.protectsProject(java.io.File(project,"source.java"),registered))
        assertFalse(InternalFiles.protectsProject(java.io.File(root,"loose"),registered))
    }
    @Test fun nestedUnicodeFilesPersistAcrossReopening() = fixture { root ->
        val f = SafeLocalFiles.create(root, "中文 空格/deep/nested/Hello.java", false)
        SafeLocalFiles.write(f, "hello".toByteArray())
        assertEquals("hello", java.io.File(root.path, "中文 空格/deep/nested/Hello.java").readText())
    }
    @Test fun creatingOrRenamingNeverOverwritesAnExistingEntry() = fixture { root ->
        val a = SafeLocalFiles.create(root, "a", false).apply { writeText("original") }
        val b = SafeLocalFiles.create(root, "b", false).apply { writeText("second") }
        assertFails { SafeLocalFiles.create(root, "a", false) }
        assertFails { SafeLocalFiles.rename(b, "a") }
        assertEquals("original", a.readText()); assertEquals("second", b.readText())
    }
    @Test fun traversalAndSymlinkCreationAreRejected() = fixture { root ->
        for (path in listOf("../escape", "/tmp/escape", "a/../../escape", "./x", "a//b")) assertFails { SafeLocalFiles.create(root, path, false) }
        Files.createSymbolicLink(root.toPath().resolve("outside"), root.parentFile.toPath())
        assertFails { SafeLocalFiles.create(root, "outside/escape", false) }
    }
    @Test fun failedSaveRetainsOriginalContentsAndRemovesStaging() = fixture { root ->
        val f = SafeLocalFiles.create(root, "document", false).apply { writeText("precious data") }
        assertFailsWith<IOException> { SafeLocalFiles.replace(f) { it.write("partial".toByteArray()); throw IOException("simulated disk failure") } }
        assertEquals("precious data", f.readText())
        assertEquals(listOf("document"), root.listFiles()!!.map { it.name })
    }
    @Test fun savesPreserveExecutablePermissionsAndSymlinkTargets() = fixture { root ->
        val original = SafeLocalFiles.create(root, "script", false).apply { writeText("old"); setExecutable(true) }
        val link = root.toPath().resolve("link"); Files.createSymbolicLink(link, original.toPath())
        SafeLocalFiles.write(link.toFile(), "new".toByteArray())
        assertTrue(Files.isSymbolicLink(link)); assertTrue(original.canExecute()); assertEquals("new", original.readText())
    }
    @Test fun copyingFoldersDoesNotRecurseIntoItselfOrOverwrite() = fixture { root ->
        val dir = SafeLocalFiles.create(root, "source", true)
        SafeLocalFiles.create(dir, "data", false).writeText("original")
        assertFails { SafeLocalFiles.copy(dir, dir, "recursive") }
        val copy = SafeLocalFiles.copy(dir, root, "copy")
        assertEquals("original", java.io.File(copy, "data").readText())
        assertFails { SafeLocalFiles.copy(dir, root, "copy") }
    }
    @Test fun copyAndDeleteDoNotFollowSymlinks() = fixture { root ->
        val source = SafeLocalFiles.create(root, "source", true)
        val other = SafeLocalFiles.create(root, "other", true)
        java.io.File(other, "keep").writeText("safe")
        Files.createSymbolicLink(source.toPath().resolve("link"), other.toPath())
        val copy = SafeLocalFiles.copy(source, root, "copy")
        assertTrue(Files.isSymbolicLink(copy.toPath().resolve("link")))
        SafeLocalFiles.remove(copy); SafeLocalFiles.remove(source)
        assertEquals("safe", java.io.File(other, "keep").readText())
    }
    @Test fun importFailureAndCollisionPreserveUserData() = fixture { root ->
        assertFailsWith<IOException> { SafeLocalFiles.importNew(root, "new") { it.write(1); throw IOException("interrupted") } }
        assertFalse(java.io.File(root, "new").exists())
        java.io.File(root, "existing").writeText("safe")
        assertFails { SafeLocalFiles.importNew(root, "existing") { it.write(2) } }
        assertEquals("safe", java.io.File(root, "existing").readText())
    }
}
