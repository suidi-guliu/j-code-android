package dev.blamspot.jcode.core.distro

import kotlinx.coroutines.async
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JavaQuickRunTest {
    private fun <T> sync(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(value: Result<T>) { result = value }
        })
        return result!!.getOrThrow()
    }

    @Test fun dispatchUsesNativeAdaptersAndDoesNotNeedAProject() {
        assertEquals(JavaLanguageAdapter, QuickRunLanguages.forFile("HelloWorld.java"))
        assertEquals(PythonLanguageAdapter, QuickRunLanguages.forFile("hello.py"))
        assertNull(QuickRunLanguages.forFile("page"))
    }

    @Test fun repeatedAndConcurrentKeysCannotStartAnotherTask() {
        val gate = QuickRunGate()
        assertFalse(gate.begin(1))
        assertTrue(gate.begin(0))
        assertFalse(gate.begin(0))
        assertFalse(gate.begin(9))
        gate.finish()
        assertTrue(gate.begin(0))
    }

    @Test fun failedSaveNeverResolvesOrRunsStaleContent() {
        var resolved = false
        assertFailsWith<IllegalStateException> {
            sync { prepareQuickRun({ false }) { resolved = true; "stale.java" } }
        }
        assertFalse(resolved)
    }

    @Test fun executionWaitsForAnActuallySuspendedSave() = kotlinx.coroutines.runBlocking {
        val saved = kotlinx.coroutines.CompletableDeferred<Unit>()
        var resolved = false
        val task = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            prepareQuickRun({ saved.await(); true }) { resolved = true; "/saved.java" }
        }
        assertFalse(resolved)
        saved.complete(Unit)
        assertEquals("/saved.java", task.await())
        assertTrue(resolved)
    }

    @Test fun dirtyFileIsSavedBeforeItsPathIsUsed() {
        val events = mutableListOf<String>()
        assertEquals("/saved.java", sync { prepareQuickRun({ events += "saved"; true }) { events += "resolved"; "/saved.java" } })
        assertEquals(listOf("saved", "resolved"), events)
    }

    @Test fun stopKillsTheTraceeFirstAndReapsStubbornAndDetachedChildren() {
        val live = mutableSetOf(10, 11, 12)
        val signals = mutableListOf<String>()
        var waits = 0
        QuickRunCleanup.stop(
            owned = { live.toSet() }, parents = { mapOf(10 to 1, 11 to 10, 12 to 1, 99 to 1) },
            terminate = { signals += "term:$it" },
            kill = { signals += "kill:$it"; live.remove(it) },
            pause = { waits++ },
        )
        assertEquals(listOf("term:10", "term:12", "kill:10", "kill:11", "kill:12"), signals)
        assertTrue(live.isEmpty())
        assertEquals(25, waits)
        assertFalse(signals.any { it.endsWith(":99") }) // unrelated ProjectRunner process
    }

    @Test fun gracefulStopDoesNotEscalateAndCloseUsesTheSamePolicy() {
        val live = mutableSetOf(10, 11)
        val signals = mutableListOf<Int>()
        QuickRunCleanup.stop(
            owned = { live.toSet() }, parents = { mapOf(10 to 1, 11 to 10) },
            terminate = { signals += it; live.clear() },
            kill = { error("No escalation should be needed") }, pause = { error("Already stopped") },
        )
        assertEquals(listOf(10), signals)
    }

    private class Sandbox : AutoCloseable {
        val root = Files.createTempDirectory("quickrun 中文 空格 '$;").toFile()
        val env = File(root, "java.env")
        init { env.writeText("export JAVA_HOME=${shellQuote(System.getenv("SUI_QUICKRUN_TEST_JAVA_HOME") ?: System.getProperty("java.home"))}\n") }
        fun source(text: String, name: String = "HelloWorld.java"): File = File(root, name).apply { writeText(text) }
        fun start(file: File, stdin: String = "", pty: Boolean = false): Pair<Int, String> {
            val script = File(root, "runner.sh").apply {
                writeText(JavaLanguageAdapter.command(file.path).replace("MANAGED_ENV=/opt/sui-code/java.env", "MANAGED_ENV=${shellQuote(env.path)}"))
            }
            val output = File(root, "output.log")
            val command = if (pty) listOf("script", "-qefc", "bash ${shellQuote(script.path)}", "/dev/null") else listOf("bash", script.path)
            val p = ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output).start()
            p.outputStream.use { it.write(stdin.toByteArray()) }
            check(p.waitFor(45, TimeUnit.SECONDS)) { p.destroyForcibly(); "QuickRun test timed out" }
            return p.exitValue() to output.readText()
        }
        override fun close() { root.deleteRecursively() }
    }

    @Test fun helloWorldFromUnicodeQuotedDirectoryHasNoClassGarbage() = Sandbox().use { s ->
        val file = s.source("public class HelloWorld { public static void main(String[] a) { System.out.println(\"Hello from sui-code!\"); } }")
        val (exit, out) = s.start(file)
        assertEquals(0, exit, out)
        assertTrue(out.contains("Hello from sui-code!"))
        assertTrue(out.contains("[QuickRun completed]"))
        assertFalse(s.root.walk().any { it.extension == "class" })
    }

    @Test fun packageAndRelativeResourcesWorkFromTheSourceDirectory() = Sandbox().use { s ->
        File(s.root, "message.txt").writeText("source directory OK")
        val f = s.source("package demo; public class HelloWorld { public static void main(String[] a) throws Exception { System.out.println(java.nio.file.Files.readString(java.nio.file.Path.of(\"message.txt\"))); } }")
        val (exit, out) = s.start(f)
        assertEquals(0, exit, out)
        assertTrue(out.contains("source directory OK"))
    }

    @Test fun specialFileNameIsNeverShellCode() = Sandbox().use { s ->
        val f = s.source("class HelloWorld { public static void main(String[] a) { System.out.println(\"safe filename\"); } }", "中文 ' ; \$ (合法).java")
        val (exit, out) = s.start(f)
        assertEquals(0, exit, out)
        assertTrue(out.contains("safe filename"))
    }

    @Test fun missingManagedEnvironmentDoesNotFallBackToHostJava() = Sandbox().use { s ->
        s.env.delete()
        val (exit, out) = s.start(s.source("class HelloWorld {}"))
        assertEquals(78, exit)
        assertTrue(out.contains("Missing"))
        assertFalse(out.contains("Checking Java source"))
    }

    @Test fun brokenManagedJavaDoesNotFallBack() = Sandbox().use { s ->
        s.env.writeText("JAVA_HOME=/missing/jdk\n")
        val (exit, out) = s.start(s.source("class HelloWorld {}"))
        assertEquals(78, exit)
        assertTrue(out.contains("Managed java or javac is missing"))
    }

    @Test fun compilationFailureIsDifferentFromRuntimeFailure() = Sandbox().use { s ->
        val (compileExit, compileOut) = s.start(s.source("public class HelloWorld { invalid syntax }"))
        assertEquals(65, compileExit, compileOut)
        assertTrue(compileOut.contains("compilation failed"))
        val (runExit, runOut) = s.start(s.source("public class HelloWorld { public static void main(String[] a) { throw new IllegalStateException(\"boom\"); } }"))
        assertEquals(70, runExit, runOut)
        assertTrue(runOut.contains("IllegalStateException: boom"))
        assertTrue(runOut.contains("runtime failed"))
    }

    @Test fun interactiveStdinUsesARealHostPty() = Sandbox().use { s ->
        val f = s.source("public class HelloWorld { public static void main(String[] a) { var scanner = new java.util.Scanner(System.in); System.out.println(\"INPUT=\" + scanner.nextLine()); } }")
        val (exit, out) = s.start(f, "平板 input\n", pty = true)
        assertEquals(0, exit, out)
        assertTrue(out.contains("INPUT=平板 input"), out)
    }

    @Test fun stoppingALongRunningHostPtyReapsJavaAndItsChildWithoutTouchingOtherTasks() = Sandbox().use { s ->
        val file = s.source("""public class HelloWorld {
            public static void main(String[] a) throws Exception {
                new ProcessBuilder("sleep", "120").start();
                Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { Thread.sleep(60000); } catch (Exception ignored) {} }));
                System.out.println("SUI_LONG_READY");
                Thread.sleep(120000);
            }
        }""")
        val runner = File(s.root, "long-runner.sh").apply {
            writeText(JavaLanguageAdapter.command(file.path).replace("MANAGED_ENV=/opt/sui-code/java.env", "MANAGED_ENV=${shellQuote(s.env.path)}"))
        }
        val token = "sui-quickrun:test-" + java.util.UUID.randomUUID()
        val output = File(s.root, "long-output.log")
        val process = ProcessBuilder("script", "-qefc", "bash ${shellQuote(runner.path)}", "/dev/null").apply {
            environment()["JCODE_EXT_ID"] = token
            redirectErrorStream(true); redirectOutput(output)
        }.start()
        val unrelated = ProcessBuilder("sleep", "120").start()
        fun owned(): Set<Int> = File("/proc").listFiles().orEmpty().mapNotNull { entry ->
            val pid = entry.name.toIntOrNull() ?: return@mapNotNull null
            if (runCatching { File(entry, "environ").readText().split('\u0000').contains("JCODE_EXT_ID=$token") }.getOrDefault(false)) pid else null
        }.toSet()
        fun signal(pid: Int, name: String) {
            ProcessBuilder("kill", "-$name", pid.toString()).start().waitFor()
        }
        fun parents(): Map<Int, Int> = File("/proc").listFiles().orEmpty().mapNotNull { entry ->
            val pid = entry.name.toIntOrNull() ?: return@mapNotNull null
            val parent = runCatching { File(entry, "stat").readText().substringAfterLast(") ").split(' ')[1].toInt() }.getOrNull() ?: return@mapNotNull null
            pid to parent
        }.toMap()
        try {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(40)
            while (!output.readText().contains("SUI_LONG_READY") && System.nanoTime() < deadline) Thread.sleep(50)
            assertTrue(output.readText().contains("SUI_LONG_READY"), output.readText())
            assertTrue(owned().size >= 3)
            QuickRunCleanup.stop(
                owned = ::owned,
                parents = ::parents,
                terminate = { pid -> signal(pid, "TERM") },
                kill = { pid -> signal(pid, "KILL") },
                pause = { Thread.sleep(20) },
            )
            process.waitFor(5, TimeUnit.SECONDS)
            repeat(50) { if (owned().isNotEmpty()) Thread.sleep(20) }
            assertTrue(owned().isEmpty(), "Tagged Java/children survived stop")
            assertTrue(unrelated.isAlive, "An unrelated task was stopped")
        } finally {
            owned().forEach { pid -> signal(pid, "KILL") }
            process.destroyForcibly(); unrelated.destroyForcibly()
        }
    }

    @Test fun consecutiveDifferentFilesRunIndependently() = Sandbox().use { s ->
        for (name in listOf("One", "Two")) {
            val (exit, out) = s.start(s.source("class $name { public static void main(String[] a) { System.out.println(\"$name OK\"); } }", "$name.java"))
            assertEquals(0, exit, out)
            assertTrue(out.contains("$name OK"))
        }
        assertFalse(s.root.walk().any { it.extension == "class" })
    }
}
