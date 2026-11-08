package dev.suicode.packages

import java.io.File
import kotlin.test.*
class HostAuthorityTest {
    private fun pack()=PackageVerifier().verify(File("../../docs/specifications/examples/packages/java.suic").readBytes(),"suic")
    @Test fun callerCannotSpoofAuthenticatedPackageIdentity() {
        val p=pack();var seen: Request?=null
        val session=PluginLoader().activate(p,Consent(p.digest,p.permissions,true),mapOf(Operation.SIMPLE_RUN to CoreBackend { seen=it;Reply(true,"STARTING") }))
        val component=p.declarations().single { it.type=="simpleRun" }.id
        assertTrue(session.request(Request(Operation.SIMPLE_RUN,component,emptyMap(),"foreign","forged")).accepted)
        assertEquals(p.id,seen!!.packageId);assertEquals(p.digest,seen!!.digest)
        session.close();assertEquals("E_DISABLED",session.request(Request(Operation.SIMPLE_RUN,component,emptyMap())).code)
    }
    @Test fun processConsentDoesNotImplyReadConsent() {
        val p=pack();val session=PluginLoader().activate(p,Consent(p.digest,setOf(Capability.RUN),true),emptyMap())
        val component=p.declarations().single { it.type=="simpleRun" }.id
        assertEquals("E_PERMISSION",session.authorize(Request(Operation.SIMPLE_RUN,component,emptyMap())).code)
    }
}
