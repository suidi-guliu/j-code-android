package dev.blamspot.jcode.core.distro
import kotlin.test.*
class CatalogProbeBatchTest {
    @Test fun probesAreBoundedAndDoNotUpdateApt() {
        val text=CatalogProbeBatch.script(listOf(CatalogProbeBatch.Probe("gcc","command -v gcc")))
        assertTrue("timeout --foreground --kill-after=1s 8s" in text);assertFalse("apt-get update" in text)
    }
    @Test fun realShellBatchBoundsHungProbeAndContinuesWithoutNetwork() {
        val script=CatalogProbeBatch.script(listOf(CatalogProbeBatch.Probe("ok","true"),CatalogProbeBatch.Probe("slow","trap '' TERM; while :; do sleep 1; done"),CatalogProbeBatch.Probe("after","true")),1)
        val started=System.nanoTime()
        val process=ProcessBuilder("bash","-c",script).start()
        val result=CatalogProbeBatch.parse(process.inputStream.bufferedReader().readText())
        assertTrue(process.waitFor(6,java.util.concurrent.TimeUnit.SECONDS))
        assertTrue((System.nanoTime()-started)/1e9<6)
        assertEquals(setOf("ok","after"),result.installed);assertEquals(setOf("slow"),result.timedOut)
    }
    @Test fun timeoutIsUnknownAndDoesNotPretendUninstalled() {
        val r=CatalogProbeBatch.parse("SUI_PROBE\ta\t0\nSUI_PROBE\tb\t1\nSUI_PROBE\tc\t124\n")
        assertEquals(setOf("a"),r.installed);assertEquals(setOf("a","b"),r.checked);assertEquals(setOf("c"),r.timedOut)
    }
}
