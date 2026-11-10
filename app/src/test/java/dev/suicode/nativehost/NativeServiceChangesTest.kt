package dev.suicode.nativehost

import dev.suicode.packages.InstalledPackage
import org.junit.Test
import org.junit.Assert.*

class NativeServiceChangesTest {
    private fun record(id: String,digest: String="old")=InstalledPackage(id,"1.0.0",digest,"suik",true,emptySet())
    @Test fun updatingPythonCannotInvalidateJavaDebugOrCppSessions() {
        val before=listOf(record("java"),record("cpp"),record("python")).associateBy { it.id }
        val after=before+("python" to record("python","new"))
        assertEquals(setOf("python"),affectedServicePackages(before,after,emptyMap()))
        assertTrue(affectedServicePackages(before,before,emptyMap()).isEmpty())
    }
    @Test fun disablingOrRemovingABasicPluginInvalidatesItsTransitiveKitsOnly() {
        val before=listOf(record("basic"),record("kit"),record("advanced"),record("other")).associateBy { it.id }
        val dependencies=mapOf("kit" to listOf("basic"),"advanced" to listOf("kit"))
        assertEquals(setOf("basic","kit","advanced"),affectedServicePackages(before,before-"basic",dependencies))
        assertEquals(setOf("basic","kit","advanced"),affectedServicePackages(before,before+("basic" to before.getValue("basic").copy(enabled=false)),dependencies))
    }
}
