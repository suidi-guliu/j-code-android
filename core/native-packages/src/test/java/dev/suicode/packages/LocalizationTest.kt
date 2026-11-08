package dev.suicode.packages
import java.io.File
import java.nio.file.Files
import kotlinx.serialization.json.*
import kotlin.test.*
class LocalizationTest {
    private val example=File("../../docs/specifications/examples/packages/zh-hans-kit.suik")
    private fun parse(s: String)=LocalizationResources.parse((canonical(Json.parseToJsonElement(s))+"\n").toByteArray())
    @Test fun realSuikHasNoEnvironmentDependencyAndLoadsTranslations() {
        val pack=PackageVerifier().verify(example.readBytes(),"suik")
        assertEquals(setOf(Capability.LOCALIZATION),pack.permissions)
        assertTrue(pack.manifest.getValue("dependencies").jsonArray.isEmpty())
        val b=LocalizationResources.forPack(pack).single();val t=UiTranslations(listOf(b),b.selectionId)
        assertEquals("文件",t.text("nav.files"));assertEquals("Stop file",t.text("run.stop"))
        assertEquals("3 个当前匹配项",t.text("files.matches",mapOf("count" to "3")))
    }
    @Test fun unknownKeysAndProtectedSecurityTextRejected() {
        assertEquals("E_LOCALIZATION",assertFailsWith<PackageFailure> { parse("{\"security.approve\":\"approved\"}") }.code)
        assertFailsWith<PackageFailure> { parse("{\"nav.files\":2}") }
        assertFailsWith<PackageFailure> { parse("{\"nav.files\":\"\\u202eevil\"}") }
    }
    @Test fun placeholderSetsValidatedAndArgumentsAreLiteral() {
        assertFailsWith<PackageFailure> { parse("{\"files.matches\":\"无占位符\"}") }
        assertFailsWith<PackageFailure> { parse("{\"nav.files\":\"{unexpected}\"}") }
        val b=LocalizationBundle("dev.test.locale","zh-Hans","中文",parse("{\"files.matches\":\"{count} 项\"}"))
        assertEquals("{scope} 项",UiTranslations(listOf(b),b.selectionId).text("files.matches",mapOf("count" to "{scope}")))
        assertFailsWith<IllegalArgumentException> { UiTranslations().text("files.matches") }
    }
    @Test fun localePathFallbackAndLimitsRejected() {
        fun d(locale:String="zh-Hans",resource:String="resources/i18n/zh.json",fallback:String="en")=buildJsonObject { put("locale",locale);put("displayName","中文");put("fallback",fallback);put("resource",resource) }
        assertEquals("zh-Hans",LocalizationDescriptor.parse(d()).locale)
        for(tag in listOf("en","zh-hans","../zh","zh_Hans")) assertFailsWith<PackageFailure> { LocalizationDescriptor.parse(d(locale=tag)) }
        assertFailsWith<PackageFailure> { LocalizationDescriptor.parse(d(resource="resources/i18n/../host.json")) }
        assertFailsWith<PackageFailure> { LocalizationDescriptor.parse(d(fallback="zh")) }
        assertFailsWith<PackageFailure> { LocalizationResources.parse(ByteArray(65537)) }
    }
    @Test fun explicitProviderChoiceNeverLetsAnotherPackageOverride() {
        val a=LocalizationBundle("dev.test.a","zh-Hans","A",mapOf("nav.files" to "A"));val b=a.copy(packageId="dev.test.b",entries=mapOf("nav.files" to "B"))
        assertEquals("A",UiTranslations(listOf(b,a),a.selectionId).text("nav.files"))
        assertEquals("Files",UiTranslations(listOf(b),a.selectionId).text("nav.files"))
        assertEquals("Files",UiTranslations(listOf(a,a),a.selectionId).text("nav.files"))
    }
    @Test fun productionStoreInstallEnableDisableUninstallPreservesEnglishFallback() {
        val root=Files.createTempDirectory("locale-store")
        val store=PackageStore(root,PackageVerifier())
        try {
            val bytes=example.readBytes();val p=store.inspect(bytes,"suik");val consent=Consent(p.digest,p.permissions,true)
            store.install(bytes,"suik",consent);store.enable(p.id,emptyMap())
            fun translated(): String {
                val bundles=store.records().filter { it.enabled && Capability.LOCALIZATION in it.grants }.flatMap { LocalizationResources.forPack(store.load(it.id)) }
                return UiTranslations(bundles,"${p.id}/zh-Hans").text("nav.files")
            }
            assertEquals("文件",translated());store.disable(p.id);assertEquals("Files",translated())
            store.enable(p.id,emptyMap());assertEquals("文件",translated());store.uninstall(p.id);assertEquals("Files",translated())
        } finally { store.close();root.toFile().deleteRecursively() }
    }
    @Test fun integrityTamperingAndMissingConsentRejected() {
        val bytes=example.readBytes();val p=PackageVerifier().verify(bytes,"suik")
        assertEquals("E_UNVERIFIED",assertFailsWith<PackageFailure> { IdeKitLoader().activate(p,Consent(p.digest,p.permissions,false),emptyMap()) }.code)
        bytes[bytes.size/2]=(bytes[bytes.size/2].toInt() xor 1).toByte()
        assertFailsWith<PackageFailure> { PackageVerifier().verify(bytes,"suik") }
    }
}
