package dev.blamspot.jcode.core.distro

import kotlin.test.*

class NativeEnvironmentTest {
    private fun plan(language: NativeEnvironment) = NativeInstallPlan.parse(language, "ubuntu-24.04",
        language.packages.joinToString("\n") { "PKG\t$it\t-\t1:2.3-4ubuntu1" } + "\narm64\n")
    @Test fun plansUseTheReviewedPackageVersionsAndArchitecture() {
        val plan = plan(NativeEnvironment.Cpp)
        assertEquals("arm64",plan.architecture)
        assertTrue(plan.specifications.contains("g++=1:2.3-4ubuntu1"))
    }
    @Test fun existingVersionsAreSelectedInsteadOfSilentlyUpgraded() {
        val plan = NativeInstallPlan.parse(NativeEnvironment.C,"ubuntu-24.04","PKG\tgcc\t12.0\t13.0\nPKG\tlibc6-dev\t2.39\t2.40\narm64\n")
        assertEquals("12.0",plan.packages.first().selected)
        assertTrue(plan.script("/tmp/sui-env-cancel-test").contains("--no-upgrade"))
    }
    @Test fun untrustedVersionsAndIncompleteMetadataAreRejected() {
        assertFails { NativePackage("gcc","1; touch /tmp/x","2") }
        assertFails { NativePackage("gcc",null,"(none)") }
        assertFails { NativeInstallPlan.parse(NativeEnvironment.Python,"ubuntu-24.04","arm64") }
        assertFails { NativeInstallPlan.parse(NativeEnvironment.C,"ubuntu-24.04","PKG\tgcc\t-\t2\nPKG\tlibc6-dev\t-\t2\nriscv64") }
    }
    @Test fun installationIsAuthenticatedCooperativelyCancellableAndDoesNotTouchJava() {
        for (language in NativeEnvironment.entries) {
            val script = plan(language).script("/tmp/sui-env-cancel-test")
            assertTrue(script.contains("AllowUnauthenticated=false"))
            assertTrue(script.indexOf("--download-only") < script.indexOf("--no-download"))
            assertTrue(script.indexOf("check_cancel",script.indexOf("--no-download")) < script.indexOf("mv -f"))
            assertFalse(script.contains("java.env")); assertFalse(script.contains("apt-get upgrade")); assertFalse(script.contains("apt-get remove"))
        }
    }
    private class Fixture : AutoCloseable {
        val root = java.nio.file.Files.createTempDirectory("native-manager-").toFile()
        val managed = java.io.File(root, "managed")
        val tools = java.io.File(root, "tools").apply { mkdirs() }
        val calls = java.io.File(root, "apt.calls")
        val cancel = java.io.File("/tmp/sui-env-cancel-" + java.util.UUID.randomUUID())
        init {
            tool("sudo", "[ \"\$1\" = '-n' ] && shift; exec \"\$@\"")
            tool("dpkg", "if [ \"\${1:-}\" = '--print-architecture' ]; then echo arm64; fi; exit 0")
            tool("dpkg-query", "printf 'ii 1:2.3-4ubuntu1'")
            tool("apt-cache", "printf '  Candidate: 2:3.4-5ubuntu1\\n'")
            tool("apt-get", "printf '%s\\n' \"\$*\" >> '${calls.path}'; case \"\$*\" in *--download-only*) [ \"\${FAIL_DOWNLOAD:-0}\" = 0 ] || exit 100; [ \"\${CANCEL_DOWNLOAD:-0}\" = 0 ] || touch '${cancel.path}' ;; *--no-download*) touch '${root.path}/packages-completed'; [ \"\${CANCEL_INSTALL:-0}\" = 0 ] || touch '${cancel.path}' ;; esac; exit 0")
        }
        fun tool(name: String, body: String) { java.io.File(tools, name).apply { writeText("#!/bin/sh\n$body\n"); setExecutable(true) } }
        fun execute(script: String, env: Map<String,String> = emptyMap()): Pair<Int,String> {
            val file = java.io.File(root,"task.sh").apply { writeText(script.replace("/opt/sui-code", managed.path)) }
            val p = ProcessBuilder("bash", file.path).apply {
                environment()["PATH"] = tools.path + ":" + System.getenv("PATH")
                environment().putAll(env); redirectErrorStream(true)
            }.start()
            val output = p.inputStream.bufferedReader().readText()
            return p.waitFor() to output
        }
        override fun close() { cancel.delete(); root.deleteRecursively() }
    }

    @Test fun queryReadsRealScriptMetadataWithoutTouchingHostApt() = Fixture().use { f ->
        val os = java.io.File(f.root,"os-release").apply { writeText("ID=ubuntu\n") }
        val (exit,out) = f.execute(NativeEnvironment.C.queryScript().replace(". /etc/os-release", ". ${shellQuote(os.path)}"))
        assertEquals(0,exit,out)
        val reviewed = NativeInstallPlan.parse(NativeEnvironment.C,"ubuntu-24.04",out)
        assertEquals("1:2.3-4ubuntu1",reviewed.packages.first().installed)
        assertEquals("2:3.4-5ubuntu1",reviewed.packages.first().candidate)
    }
    @Test fun installerCompilesRealGccSmokeBeforeActivatingAndLeavesJavaAlone() = Fixture().use { f ->
        f.managed.mkdirs()
        val javaEnv = java.io.File(f.managed,"java.env").apply { writeText("export JAVA_HOME=/existing/jdk\n") }
        val (exit,out) = f.execute(plan(NativeEnvironment.C).script(f.cancel.path))
        assertEquals(0,exit,out)
        assertTrue(java.io.File(f.managed,"c.env").readText().contains("SUI_C=/usr/bin/gcc"))
        assertEquals("export JAVA_HOME=/existing/jdk\n",javaEnv.readText())
        assertTrue(f.calls.readText().contains("--download-only"))
        assertTrue(f.calls.readText().contains("--no-download"))
    }
    @Test fun cancelledDownloadNeverRunsDpkgInstallOrPublishesEnvironment() = Fixture().use { f ->
        val (exit,out) = f.execute(plan(NativeEnvironment.C).script(f.cancel.path),mapOf("CANCEL_DOWNLOAD" to "1"))
        assertEquals(130,exit,out)
        assertFalse(f.calls.readText().contains("--no-download"))
        assertFalse(java.io.File(f.managed,"c.env").exists())
    }
    @Test fun cancellationDuringInstallWaitsForItsSafeCompletion() = Fixture().use { f ->
        val (exit,out) = f.execute(plan(NativeEnvironment.C).script(f.cancel.path),mapOf("CANCEL_INSTALL" to "1"))
        assertEquals(130,exit,out)
        assertTrue(java.io.File(f.root,"packages-completed").isFile)
        assertFalse(java.io.File(f.managed,"c.env").exists())
    }
    @Test fun failedAuthenticatedDownloadDoesNotReplaceThePreviousEnvironment() = Fixture().use { f ->
        f.managed.mkdirs()
        val previous = java.io.File(f.managed,"c.env").apply { writeText("export SUI_C=/missing/old/compiler\n") }
        val (exit,out) = f.execute(plan(NativeEnvironment.C).script(f.cancel.path),mapOf("FAIL_DOWNLOAD" to "1"))
        assertEquals(100,exit,out)
        assertEquals("export SUI_C=/missing/old/compiler\n",previous.readText())
    }
    @Test fun existingUsableManagedCompilerIsVerifiedAndPreservedWithoutApt() = Fixture().use { f ->
        f.managed.mkdirs()
        val previous = java.io.File(f.managed,"c.env").apply { writeText("export SUI_C=/usr/bin/gcc\n") }
        val (exit,out) = f.execute(plan(NativeEnvironment.C).script(f.cancel.path))
        assertEquals(0,exit,out)
        assertFalse(f.calls.exists())
        assertEquals("export SUI_C=/usr/bin/gcc\n",previous.readText())
    }

}
