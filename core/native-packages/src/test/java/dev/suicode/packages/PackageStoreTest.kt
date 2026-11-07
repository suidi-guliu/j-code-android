package dev.suicode.packages

import java.io.File
import java.nio.file.Files
import kotlin.test.*
import org.junit.Test

class PackageStoreTest {
    private fun data(name: String)=File("../../docs/specifications/examples/packages/$name").readBytes()
    private fun storeTest(block: (PackageStore)->Unit) {
        val dir=Files.createTempDirectory("sui-pack-test-")
        try { val s=PackageStore(dir);try { block(s) } finally { s.close() } } finally { dir.toFile().deleteRecursively() }
    }
    @Test fun installDisableAndUninstallNeverRunCode()=storeTest { s ->
        val b=data("minimal.suic");val p=s.inspect(b,"suic")
        s.install(b,"suic",Consent(p.digest,emptySet(),true))
        assertEquals("0.1.0",s.installed()[p.id])
        val session=s.enable(p.id,emptyMap())
        s.disable(p.id)
        assertEquals("E_DISABLED",session.request(Request(Operation.SIMPLE_RUN,"language",emptyMap())).code)
        s.uninstall(p.id);assertTrue(s.installed().isEmpty())
    }
    @Test fun failedInstallPreservesState()=storeTest { s ->
        val b=data("minimal.suic");val p=s.inspect(b,"suic");s.install(b,"suic",Consent(p.digest,emptySet(),true))
        assertFailsWith<PackageFailure> { s.install(data("illegal-plugin.suic"),"suic",Consent("bad",emptySet(),true)) }
        assertEquals(mapOf(p.id to "0.1.0"),s.installed())
    }
    @Test fun dependenciesBlockRemoval()=storeTest { s ->
        for(name in listOf("java.suic","java-kit.suik")) {
            val b=data(name);val p=s.inspect(b,name.substringAfter('.'));s.install(b,name.substringAfter('.'),Consent(p.digest,p.permissions,true))
        }
        assertEquals("E_DEPENDENCY",assertFailsWith<PackageFailure> { s.uninstall("dev.suicode.example.java") }.code)
        s.uninstall("dev.suicode.example.java-kit");s.uninstall("dev.suicode.example.java")
    }
    @Test fun updateAndExplicitRollback()=storeTest { s ->
        val b=data("minimal.suic");val p=s.inspect(b,"suic");val consent=Consent(p.digest,emptySet(),true)
        s.install(b,"suic",consent);s.install(b,"suic",consent)
        assertEquals(p.digest,s.rollback(p.id,consent).digest)
    }
    @Test fun designBoundsAndTokens() {
        assertEquals(DesignSystem.WidthClass.COMPACT,DesignSystem.widthClass(599))
        assertEquals(DesignSystem.WidthClass.MEDIUM,DesignSystem.widthClass(600))
        assertEquals(DesignSystem.WidthClass.EXPANDED,DesignSystem.widthClass(840))
        assertEquals(440,DesignSystem.terminalHeight(800,900))
        assertEquals(DesignSystem.colors(false).keys,DesignSystem.colors(true).keys)
        assertTrue(DesignSystem.radius.values.all { it<=8 })
    }
}
