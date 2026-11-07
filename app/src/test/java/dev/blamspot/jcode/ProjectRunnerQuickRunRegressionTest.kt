package dev.blamspot.jcode

import dev.blamspot.jcode.core.config.RunConfig
import dev.blamspot.jcode.core.config.RunConfigTerminal
import dev.blamspot.jcode.fs.Project
import dev.blamspot.jcode.fs.ProjectKind
import dev.blamspot.jcode.run.ProjectRunner
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** An existing non-Java run config still stages and executes without the managed Java environment. */
class ProjectRunnerQuickRunRegressionTest {
    @Test fun projectRecipeExecutesUnchangedWithoutJavaEnvironment() {
        val root = Files.createTempDirectory("project-run-regression").toFile()
        try {
            val project = Project(1, ProjectKind.Local, root.path, "Existing project", root.path, 0)
            val config = RunConfig("Existing run", 0, terminals = listOf(RunConfigTerminal("Console", "printf 'PROJECT_RUN_OK\\n'\n")))
            val terminal = ProjectRunner.runConfigToPlan(config).terminals.single()
            val invocation = ProjectRunner.runInvocation(project, terminal)
            val process = ProcessBuilder("bash", "-c", invocation).apply {
                environment().remove("JAVA_HOME")
                redirectErrorStream(true)
            }.start()
            val output = process.inputStream.bufferedReader().readText()
            assertEquals(0, process.waitFor())
            assertEquals("PROJECT_RUN_OK\n", output)
            assertFalse(File(root, ".jcode/run-console.sh").readText().contains("/opt/sui-code/java.env"))
        } finally { root.deleteRecursively() }
    }
}
