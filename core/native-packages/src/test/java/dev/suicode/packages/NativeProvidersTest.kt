package dev.suicode.packages

import java.io.File
import java.nio.file.Files
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import kotlin.test.*
import org.junit.Test

class NativeProvidersTest {
    private val library=File("../../native-library")
    private val keyBytes=File(library,"official-p256.der").readBytes()
    private val key=KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(keyBytes))
    private val verifier=PackageVerifier(mapOf(sha(keyBytes) to key))
    private fun basic(name: String)=verifier.verify(File(library,"v1/$name.suic").readBytes(),"suic")
    private fun session(p: VerifiedPack)=PluginLoader().activate(p,Consent(p.digest,p.permissions,false),mapOf(Operation.SIMPLE_RUN to CoreBackend { Reply(true,"AUTHORIZED") }))
    @Test fun signedFirstPartyPackagesAndKitsVerify() {
        val dependencies=mapOf("dev.suicode.java.basic" to "1.0.0","dev.suicode.ccpp.basic" to "1.0.0","dev.suicode.python.basic" to "1.0.0")
        for(name in listOf("Java","C_CPP","Python")) {
            assertTrue(basic(name).official)
            assertTrue(verifier.verify(File(library,"v1/$name.suik").readBytes(),"suik",dependencies).official)
        }
    }
    @Test fun projectRecipesStageTheirAuthenticatedSiblingResources() {
        val dependencies=mapOf("dev.suicode.java.basic" to "1.0.0","dev.suicode.ccpp.basic" to "1.0.0","dev.suicode.python.basic" to "1.0.0")
        for(name in listOf("Java","C_CPP","Python")) {
            val pack=verifier.verify(File(library,"v1/$name.suik").readBytes(),"suik",dependencies)
            val session=IdeKitLoader().activate(pack,Consent(pack.digest,pack.permissions,false),mapOf(Operation.BUILD to CoreBackend { Reply(true,"AUTHORIZED") }))
            val recipe=NativeRecipe.forComponent(pack,session,"build")
            assertEquals(pack.resourcePaths().toSet(),recipe.resources().keys)
            if(name=="C_CPP") assertTrue("resources/migrate-build.sh" in recipe.resources())
            assertTrue(recipe.script("/workspace/中文 project", "/tmp/sui-package-test", "/workspace/中文 project").contains("E_TASK_PATH"))
            session.close()
        }
    }
    @Test fun cAndCppShareOnePackageWithCaseSensitiveExtensions() {
        val p=basic("C_CPP");val registry=NativeProviders();registry.register(p,session(p))
        assertEquals("c",registry.resolve("hello.c")!!.languageId)
        assertEquals("cpp",registry.resolve("hello.C")!!.languageId)
        assertEquals("cpp",registry.resolve("hello.cpp")!!.languageId)
        assertNull(registry.resolve("hello.java"))
    }
    @Test fun stopDisableAndNoKitRequirement() {
        val p=basic("Python");val s=session(p);val r=NativeProviders();r.register(p,s)
        assertTrue(r.resolve("hello.py")!!.authorize().accepted)
        s.close();assertEquals("E_DISABLED",r.resolve("hello.py")!!.authorize().code)
        r.unregister(p.id);assertNull(r.resolve("hello.py"))
    }
    @Test fun allFourPackagedScriptsExecuteSavedFiles() {
        val jdk=System.getenv("SUI_QUICKRUN_TEST_JAVA_HOME") ?: error("Set SUI_QUICKRUN_TEST_JAVA_HOME to the verified test JDK")
        val temp=Files.createTempDirectory("sui-package-").toFile()
        try {
            val config=File(temp,"managed").apply { mkdirs() }
            File(config,"java.env").writeText("export JAVA_HOME='${jdk.replace("'","'\\''")}'\n")
            File(config,"c.env").writeText("export SUI_C=/usr/bin/gcc\n")
            File(config,"cpp.env").writeText("export SUI_CPP=/usr/bin/g++\n")
            File(config,"python.env").writeText("export SUI_PYTHON=/usr/bin/python3\n")
            val files=listOf(
                Triple("Java","Hello.java","public class Hello { public static void main(String[] args) { System.out.println(\"SUI_JAVA\"); } }"),
                Triple("C_CPP","hello.c","#include <stdio.h>\nint main(void) { puts(\"SUI_C\"); }\n"),
                Triple("C_CPP","hello.cpp","#include <iostream>\nint main() { std::cout << \"SUI_CPP\" << std::endl; }\n"),
                Triple("Python","hello.py","print('SUI_PYTHON')\n")
            )
            files.forEach { (name,fileName,source) ->
                val folder=File(temp,"中文 空格 ' ; test").apply { mkdirs() }
                val file=File(folder,fileName).apply { writeText(source) }
                val p=basic(name);val r=NativeProviders();r.register(p,session(p));val provider=r.resolve(file.name)!!
                provider.resources().forEach { (path,bytes) -> File(temp,path).apply { parentFile.mkdirs();writeText(bytes.toString(Charsets.UTF_8).replace("/opt/sui-code",config.path)) } }
                val runner=File(temp,"invoke.sh").apply { writeText(provider.script(file.path,temp.path)) }
                val output=File(temp,"out-$fileName.log")
                val process=ProcessBuilder("/bin/bash",runner.path).redirectErrorStream(true).redirectOutput(output).start()
                val finished=process.waitFor(60,java.util.concurrent.TimeUnit.SECONDS)
                if(!finished) process.destroyForcibly()
                assertTrue(finished,"Timed out: $fileName")
                assertEquals(0,process.exitValue(),output.readText())
                assertTrue("SUI_" in output.readText(),output.readText())
                assertTrue(File(temp,"tmp").isDirectory)
                assertTrue(File(temp,"tmp").listFiles().orEmpty().isEmpty(),"Basic task build output survived completion")
            }
            assertFalse(File(temp,"中文 空格 ' ; test/Hello.class").exists())
        } finally { temp.deleteRecursively() }
    }
}
