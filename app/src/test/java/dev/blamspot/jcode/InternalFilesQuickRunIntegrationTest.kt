package dev.blamspot.jcode

import dev.blamspot.jcode.core.distro.*
import dev.blamspot.jcode.fs.*
import dev.suicode.packages.*
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Real local save -> native dispatcher -> real tool execution, without any Workspace or Project. */
class InternalFilesQuickRunIntegrationTest {
    @Test fun internalFilesSaveThenRunAllFourLanguagesWithoutAProject() = runBlocking {
        val files = Files.createTempDirectory("internal-integration-").toFile()
        try {
            val root = InternalFiles.root(files)
            val folder = File(root,"中文 ' ;/deep/subfolder").apply { mkdirs() }
            val preserved = File(root,"existing-project/data.txt").apply { parentFile!!.mkdirs(); writeText("preserve me") }
            val cases = listOf(
                "hello.c" to "#include <stdio.h>\nint main(void) { puts(\"C-SAVED\"); }",
                "hello.cpp" to "#include <iostream>\nint main() { std::cout << \"CPP-SAVED\" << std::endl; }",
                "hello.py" to "print('PYTHON-SAVED')\n",
                "HelloWorld.java" to "public class HelloWorld { public static void main(String[] a) { System.out.println(\"JAVA-SAVED\"); } }",
            )
            val javaEnv = File(files,"test-java.env").apply {
                val jdk = System.getenv("SUI_QUICKRUN_TEST_JAVA_HOME") ?: System.getProperty("java.home")
                writeText("export JAVA_HOME='${jdk.replace("'", "'\\''")}'\n")
            }
            val registry=NativeProviders()
            val library=File("../native-library/v1")
            for(language in listOf("Java","C_CPP","Python")) {
                val pack=PackageVerifier().verify(File(library,"$language.suic").readBytes(),"suic")
                registry.register(pack,PluginLoader().activate(pack,Consent(pack.digest,pack.permissions,true),emptyMap()))
            }
            val resourceDir=Files.createTempDirectory("sui-package-").toFile()
            for ((name, text) in cases) {
                val document = File(folder,name).apply { writeText("stale content must not run") }
                val path = prepareQuickRun({ PosixFs().write(FsPath.Local(document),text.toByteArray()); true }) { document.canonicalPath }
                val adapter = registry.resolve(name) ?: error("Missing adapter")
                adapter.resources().forEach { (entry,data) ->
                    File(resourceDir,entry).apply { parentFile.mkdirs();writeText(data.toString(Charsets.UTF_8).replace("MANAGED_ENV=/opt/sui-code/java.env","MANAGED_ENV='${javaEnv.path}'")) }
                }
                val command = adapter.script(path,resourceDir.path)
                val script = File(files,"runner.sh").apply { writeText(command) }
                val output = File(files,"output.txt")
                val process = ProcessBuilder("bash",script.path).redirectErrorStream(true).redirectOutput(output).start()
                process.outputStream.close()
                assertTrue("Timed out: $name",process.waitFor(45,TimeUnit.SECONDS))
                assertEquals(output.readText(),0,process.exitValue())
                assertTrue(output.readText().contains("-SAVED"))
                assertEquals(text,document.readText())
            }
            resourceDir.deleteRecursively()
            assertEquals("preserve me",preserved.readText())
            assertFalse(root.walk().any { it.extension in setOf("class","o","pyc") || it.name=="program" })
            assertEquals(root.canonicalPath,InternalFiles.root(File(files.path)).canonicalPath)
        } finally { files.deleteRecursively() }
    }
}
