package dev.suicode.packages
import java.io.File
import kotlinx.serialization.json.*
import kotlin.test.*
class KitRequirementsTest {
    private fun fixture()=Json.parseToJsonElement(File("../../docs/specifications/examples/dependencies-kit/ide/prerequisites.json").readText()).jsonObject
    @Test fun realDependencyExampleHasUsableAptPlan() { val v=KitRequirements.parse(fixture()).single();assertEquals(listOf("cmake","ninja-build"),v.packages);assertNull(v.bytes) }
    @Test fun shellInjectionInPackageNameIsRejected() { val d=fixture().toString().replace("ninja-build","ninja-build;touch bad");assertEquals("E_COMPONENT",assertFailsWith<PackageFailure> { KitRequirements.parse(Json.parseToJsonElement(d).jsonObject) }.code) }
    @Test fun unknownProviderCannotBypassCore() { val d=fixture().toString().replace("ubuntu-apt","arbitrary-host-exec");assertFailsWith<PackageFailure> { KitRequirements.parse(Json.parseToJsonElement(d).jsonObject) } }
    @Test fun unsignedDoesNotMeanInvalidIntegrityAndStillNeedsConsent() {
        val p=PackageVerifier().verify(File("../../docs/specifications/examples/packages/dependencies-kit.suik").readBytes(),"suik")
        assertFalse(p.official);assertEquals(1,KitRequirements.forPack(p).size)
        assertEquals("E_UNVERIFIED",assertFailsWith<PackageFailure> { IdeKitLoader().activate(p,Consent(p.digest,p.permissions,false),emptyMap()) }.code)
    }
    @Test fun allThreeOfficialLanguageDefinitionsValidate() { for(name in listOf("Java","C_CPP","Python")) assertTrue(KitRequirements.parse(Json.parseToJsonElement(File("../../native-library/src/$name/suik/adapters/setup.json").readText()).jsonObject).size>=3) }
    @Test fun setupRemainsOutsidePluginBoundary() {
        assertEquals("E_BOUNDARY",assertFailsWith<PackageFailure> { PackageVerifier().verify(File("../../docs/specifications/examples/packages/illegal-plugin.suic").readBytes(),"suic") }.code)
    }
}
