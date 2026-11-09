package dev.blamspot.jcode

import dev.blamspot.jcode.startup.environmentStartupResolving
import org.junit.Test
import org.junit.Assert.*

class EnvironmentStartupStateTest {
    @Test fun absentRuntimeDoesNotTrapAnExplicitlyDeferredSetupBehindSplash() {
        // DistroService refreshStatus intentionally publishes false/null for an absent rootfs.
        assertFalse(environmentStartupResolving(false,null,false))
        assertFalse(environmentStartupResolving(false,false,false))
        assertFalse(environmentStartupResolving(true,false,false))
    }
    @Test fun realPendingProbesAndActiveInstallStillBlockUntilKnown() {
        assertTrue(environmentStartupResolving(null,null,false))
        assertTrue(environmentStartupResolving(true,null,false))
        assertTrue(environmentStartupResolving(false,null,true))
        assertTrue(environmentStartupResolving(true,true,true))
        assertFalse(environmentStartupResolving(true,true,false))
    }
}
