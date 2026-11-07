package dev.blamspot.jcode.core.distro

import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.*

class NativeQuickRunTest {
    private fun sandbox(test: (File) -> Unit) {
        val root = Files.createTempDirectory("native 中文 空格 '$;").toFile()
        try { test(root) } finally { root.deleteRecursively() }
    }
    private fun run(root: File, adapter: NativeLanguageAdapter, source: String, name: String, input: String = "", pty: Boolean = false): Pair<Int,String> {
        val file = File(root, name).apply { writeText(source) }
        val script = File(root, "runner.sh").apply {
            writeText(adapter.command(file.path).replace("ENV_FILE=/opt/sui-code/${adapter.id}.env", "ENV_FILE=${shellQuote(File(root, "missing.env").path)}"))
        }
        val output = File(root, "output.log")
        val cmd = if (pty) listOf("script", "-qefc", "bash ${shellQuote(script.path)}", "/dev/null") else listOf("bash", script.path)
        val process = ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(output).start()
        process.outputStream.use { it.write(input.toByteArray()) }
        check(process.waitFor(30,TimeUnit.SECONDS)) { process.destroyForcibly(); "Native test timed out" }
        return process.exitValue() to output.readText()
    }
    @Test fun dispatchSupportsOnlyTheImplementedLanguages() {
        assertEquals(CLanguageAdapter, QuickRunLanguages.forFile("a.c"))
        assertEquals(CppLanguageAdapter, QuickRunLanguages.forFile("a.cpp"))
        assertEquals(CppLanguageAdapter, QuickRunLanguages.forFile("a.C"))
        assertEquals(PythonLanguageAdapter, QuickRunLanguages.forFile("a.py"))
        assertNull(QuickRunLanguages.forFile("a.rs"))
    }
    @Test fun colonContainingParentsHaveAnUnambiguousProotBindAndCorrectWorkingDirectory() = sandbox { root ->
        val file = File(root,"colon:folder/中文 子目录/hello.py").apply { parentFile!!.mkdirs(); writeText("print('OK')") }
        val mount = QuickRunFileMount.forFile(file)
        assertFalse(mount.hostDirectory.path.contains(':'))
        assertTrue(mount.guestFile.endsWith("/colon:folder/中文 子目录/hello.py"))
        assertEquals(file.canonicalPath, File(mount.hostDirectory, mount.guestFile.removePrefix("/sui-quickrun/")).canonicalPath)
        assertTrue(mount.guestDirectory.endsWith("colon:folder/中文 子目录"))
    }
    @Test fun cCompilesReadsPtyInputAndLeavesNoSourceArtifacts() = sandbox { root ->
        val (exit,out) = run(root,CLanguageAdapter,"#include <stdio.h>\nint main(void) { int n; if(scanf(\"%d\",&n)!=1) return 3; printf(\"C=%d\\n\", n+1); }", "中文 ' ;.c", "41\n",true)
        assertEquals(0,exit,out); assertTrue(out.contains("C=42"),out)
        assertFalse(root.listFiles()!!.any { it.name in setOf("a.out","program") || it.extension=="o" })
    }
    @Test fun cppCompilesAndHandlesInteractiveInput() = sandbox { root ->
        val (exit,out) = run(root,CppLanguageAdapter,"#include <iostream>\nint main() { std::string s; std::getline(std::cin,s); std::cout << \"CPP=\" << s << std::endl; }", "hello world.cpp", "input\n",true)
        assertEquals(0,exit,out); assertTrue(out.contains("CPP=input"),out)
    }
    @Test fun pythonReadsPtyInputAndPreservesFileAndRelativeImportSemantics() = sandbox { root ->
        File(root,"helper.py").writeText("value = 'module OK'\n")
        val source = "import helper, pathlib\nprint(helper.value)\nprint('FILE=' + pathlib.Path(__file__).name)\nprint('PY=' + input())\n"
        val (exit,out) = run(root,PythonLanguageAdapter,source,"中文 '$;.py","你好\n",true)
        assertEquals(0,exit,out); assertTrue(out.contains("module OK"),out); assertTrue(out.contains("PY=你好"),out)
        assertTrue(out.contains("FILE=中文 '$;.py"),out); assertFalse(File(root,"__pycache__").exists())
    }
    @Test fun cAndCppCompileFailuresAreNotRuntimeFailures() = sandbox { root ->
        for ((adapter,name) in listOf(CLanguageAdapter to "bad.c",CppLanguageAdapter to "bad.cpp")) {
            val (exit,out) = run(root,adapter,"not valid syntax",name)
            assertEquals(65,exit,out); assertTrue(out.contains("compilation failed"))
        }
    }
    @Test fun nativeAndPythonRuntimeFailuresPreserveDiagnostics() = sandbox { root ->
        val (cExit,cOut) = run(root,CLanguageAdapter,"int main(void) { return 7; }","fail.c")
        assertEquals(70,cExit,cOut); assertTrue(cOut.contains("exit 7"))
        val (pExit,pOut) = run(root,PythonLanguageAdapter,"raise ValueError('boom')\n","fail.py")
        assertEquals(70,pExit,pOut); assertTrue(pOut.contains("ValueError: boom"))
    }
    @Test fun pythonSyntaxFailureIsClassifiedBeforeExecution() = sandbox { root ->
        val (exit,out) = run(root,PythonLanguageAdapter,"print('not executed')\nif :\n","bad.py")
        assertEquals(65,exit,out); assertFalse(out.contains("[QuickRun] Running Python"))
    }
    @Test fun pythonMainModuleAndDataclassAnnotationsBehaveLikeARealScript() = sandbox { root ->
        val source = "from __future__ import annotations\nfrom dataclasses import dataclass\nimport __main__\n@dataclass\nclass Item:\n    value: int\nassert __main__.Item is Item\nprint(Item(42).value)\n"
        val (exit,out) = run(root,PythonLanguageAdapter,source,"module.py")
        assertEquals(0,exit,out); assertTrue(out.contains("42"),out)
    }
    @Test fun compilerFileMacrosKeepTheOriginalSourceName() = sandbox { root ->
        val (exit,out) = run(root,CLanguageAdapter,"#include <stdio.h>\nint main(void) { puts(__FILE__); }","original name.c")
        assertEquals(0,exit,out); assertTrue(out.contains(java.io.File(root,"original name.c").path),out)
    }
    @Test fun brokenManagedToolNeverFallsBackToUbuntuDefault() = sandbox { root ->
        File(root,"missing.env").writeText("SUI_C=/does/not/exist\n")
        val (exit,out) = run(root,CLanguageAdapter,"int main(void) { return 0; }","good.c")
        assertEquals(78,exit,out); assertTrue(out.contains("environment error"))
    }
}
