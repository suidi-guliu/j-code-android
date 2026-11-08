package dev.blamspot.jcode.core.distro
import kotlin.test.*
class InstallationOutcomeTest {
    @Test fun exit100DoesNotContradictUsablePostconditions() { val o=InstallationOutcome.assess(100,true);assertTrue(o.ready);assertNotNull(o.warning);assertTrue(o.message.contains("warnings")) }
    @Test fun exitZeroDoesNotClaimUnverifiedInstallationSucceeded() { assertFalse(InstallationOutcome.assess(0,false).ready) }
    @Test fun cancellationKeepsCompletedToolsAndIsRetryable() { val o=InstallationOutcome.assess(130,false,true);assertFalse(o.ready);assertTrue(o.message.contains("retained")) }
}
