package dev.blamspot.jcode.core.distro
import kotlin.test.*
class RunShortcutGateTest {
    @Test fun bothAndroidDispatchPathsOnlyFireOnce() { val g=RunShortcutGate();assertTrue(g.accept(123,135,0));assertFalse(g.accept(123,135,0)) }
    @Test fun longPressDoesNotStartMultipleRunsAndNewPressWorks() { val g=RunShortcutGate();assertTrue(g.accept(1,135,0));(1..20).forEach { assertFalse(g.accept(1,135,it)) };assertTrue(g.accept(2,135,0)) }
    @Test fun compatibilityKeyHasIndependentIdentity() { val g=RunShortcutGate();assertTrue(g.accept(1,135,0));assertTrue(g.accept(1,66,0)) }
}
