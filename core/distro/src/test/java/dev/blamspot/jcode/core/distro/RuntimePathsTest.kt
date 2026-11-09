package dev.blamspot.jcode.core.distro

import java.io.File
import java.nio.file.Files
import kotlin.test.*

class RuntimePathsTest {
    private fun sandbox(test: (File) -> Unit) {
        val root = Files.createTempDirectory("runtime paths 中文 '").toFile()
        try { test(root) } finally { root.deleteRecursively() }
    }
    @Test fun projectTaskAlwaysCarriesTheWorkspaceMountAndUsesItsStableIdentity() = sandbox { root ->
        val projects = File(root,"projects").apply { mkdir() }
        val project = File(projects,"多层/项目 空格:one").apply { mkdirs() }
        val source = File(project,"Main.java").apply { writeText("class Main {}") }
        val task = NativeTaskPaths.resolve(source,project,emptyList(),projects.path)
        assertEquals("/workspace/多层/项目 空格:one",task.guestDirectory)
        assertEquals(task.guestDirectory+"/Main.java",task.guestSource)
        assertTrue(task.binds.any { it.host==projects.canonicalPath && it.target=="/workspace" })
        assertTrue(task.binds.none { ':' in it.host })
    }
    @Test fun internalFilesWithoutAProjectKeepTheSamePathAsEditorAndProtocolClients() = sandbox { root ->
        val source = File(root,"internal/中文:folder/hello ' $;.py").apply { parentFile!!.mkdirs();writeText("print(1)") }
        val task = NativeTaskPaths.resolve(source,null,emptyList(),File(root,"projects").path)
        assertEquals(source.canonicalPath,task.guestSource)
        assertEquals(source.parentFile!!.canonicalPath,task.guestDirectory)
        assertTrue(task.binds.none { ':' in it.host })
    }
    @Test fun broadBindsCannotMaskNestedProjectsAndExplicitOverridesAreRetained() = sandbox { root ->
        val bindings = RuntimeMounts.ordered(listOf(DistroBind("/a","/workspace/one/sub"),DistroBind("/b","/workspace"),DistroBind("/c","/workspace/one")),root.path)
        assertEquals(listOf("/workspace","/workspace/one","/workspace/one/sub"),bindings.map { it.target })
        assertEquals("/b",bindings.first().host)
    }
    @Test fun switchingProjectsDoesNotReuseThePreviousProjectMount() = sandbox { root ->
        val first=File(root,"one").apply { mkdir() };val second=File(root,"two").apply { mkdir() }
        for(project in listOf(first,second,first)) {
            val source=File(project,"test.c").apply { writeText("int main() {return 0;}") }
            val task=NativeTaskPaths.resolve(source,project,emptyList(),root.path)
            assertEquals("/workspace/${project.name}",task.guestDirectory)
            assertTrue(task.binds.any { it.host==project.canonicalPath && it.target==task.guestDirectory })
        }
    }
    @Test fun missingDirectoryAndOutsideSourceFailBeforeAProcessCanBeCreated() = sandbox { root ->
        val source=File(root,"source.java").apply { writeText("class Source {}") }
        assertTrue(assertFails { NativeTaskPaths.resolve(source,File(root,"missing"),emptyList(),root.path) }.message!!.contains("E_TASK_PATH"))
        val elsewhere=File(root,"elsewhere").apply { mkdir() }
        assertFails { NativeTaskPaths.resolve(source,elsewhere,emptyList(),root.path) }
        assertFails { NativeTaskPaths.resolve(File(root,"missing.c"),null,emptyList(),root.path) }
    }
}
