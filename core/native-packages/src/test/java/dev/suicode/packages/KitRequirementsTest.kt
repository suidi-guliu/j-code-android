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
    private fun graph(vararg nodes: Pair<String,List<String>>): JsonObject {
        val template=fixture().array("requirements").single().jsonObject
        return buildJsonObject { put("requirements",JsonArray(nodes.map { (id,deps) -> JsonObject(template + mapOf("id" to JsonPrimitive(id),"dependsOn" to JsonArray(deps.map(::JsonPrimitive)))) })) }
    }
    @Test fun selectedComponentsIncludeOnlyTheirTopologicalDependencyClosure() {
        val requirements=KitRequirements.parse(graph("ninja" to emptyList(),"cmake" to listOf("ninja"),"test" to listOf("cmake"),"clangd" to emptyList()))
        assertEquals(listOf("ninja","cmake","test"),KitRequirements.selection(requirements,setOf("test")).map { it.id })
        assertTrue(KitRequirements.selection(requirements,emptySet()).isEmpty())
        assertFailsWith<PackageFailure> { KitRequirements.selection(requirements,setOf("unknown")) }
    }
    @Test fun cyclicAndUnknownDependenciesAreRejectedBeforePlanningOrExecution() {
        assertFailsWith<PackageFailure> { KitRequirements.parse(graph("a" to listOf("b"),"b" to listOf("a"))) }
        assertFailsWith<PackageFailure> { KitRequirements.parse(graph("a" to listOf("missing"))) }
    }
    @Test fun legacySharedResourceIsOneExplicitlyReviewedInstallationGroup() {
        val base=KitRequirements.parse(fixture()).single()
        val runtime=base.copy(id="runtime",provider="environment",resource=null,environmentId="python",packages=emptyList(),commands=listOf("python3"))
        val a=base.copy(id="a",provider="kit-resource",resource="resources/common.sh",packages=emptyList(),commands=listOf("tool-a"))
        val b=a.copy(id="b",commands=listOf("tool-b"))
        assertEquals(listOf("runtime","a","b"),KitRequirements.selection(listOf(runtime,a,b),setOf("a")).map { it.id })
        assertEquals(listOf("a"),KitRequirements.selection(listOf(a.copy(explicitDependencies=true),b.copy(explicitDependencies=true)),setOf("a")).map { it.id })
    }
    @Test fun moduleChecksAcceptOnlyDottedPythonIdentifiers() {
        val base=fixture().array("requirements").single().jsonObject
        fun module(name:String)=buildJsonObject { put("requirements",JsonArray(listOf(JsonObject(base + mapOf("pythonModules" to JsonArray(listOf(JsonPrimitive(name)))))))) }
        assertEquals(listOf("unittest.mock"),KitRequirements.parse(module("unittest.mock")).single().pythonModules)
        assertFailsWith<PackageFailure> { KitRequirements.parse(module("os; system('bad')")) }
    }

}
