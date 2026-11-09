package dev.suicode.packages

import java.io.File
import java.nio.file.Files
import kotlinx.serialization.json.*
import kotlin.test.*

class ChineseLanguageKitTest {
    private val archive=File("../../localization/releases/sui-code-zh-hans-1.0.1.suik")
    @Test fun shippedCompletePackIsIndependentAndEveryPublishedKeyHasValidChinese() {
        val bytes=archive.readBytes()
        assertEquals("SUIKPKG\u0000",bytes.copyOfRange(0,8).toString(Charsets.UTF_8))
        val pack=PackageVerifier().verify(bytes,"suik")
        assertEquals("dev.suicode.localization.zh-hans",pack.id)
        assertEquals("1.0.1",pack.version)
        assertFalse(pack.official)
        assertEquals(setOf(Capability.LOCALIZATION),pack.permissions)
        assertTrue(pack.manifest.getValue("dependencies").jsonArray.isEmpty())
        assertEquals(listOf("localization"),pack.declarations().map { it.type })
        val bundle=LocalizationResources.forPack(pack).single()
        assertEquals(UiTextKeys.english.keys,bundle.entries.keys)
        assertTrue(bundle.entries.size>=700,"Must not regress to the partial SDK example")
        val text=UiTranslations(listOf(bundle),bundle.selectionId)
        for((key,english) in UiTextKeys.english) {
            val parameters=UiPlaceholders.parameters(english).associateWith { "中文 {literal} $ \\ path" }
            assertEquals(UiPlaceholders.render(bundle.entries.getValue(key),parameters),text.text(key,parameters),key)
        }
        assertEquals("文件",text.text("nav.files"))
        assertEquals("运行文件",text.text("run.file"))
        assertEquals("扩展包",text.text("packages.title"))
        assertTrue(pack.content("resources/i18n/zh-Hans.json").size<=65536)
    }
    @Test fun sourceAndPermissionConsentCannotBeBypassedByACompleteResourceKit() {
        val pack=PackageVerifier().verify(archive.readBytes(),"suik")
        assertEquals("E_UNVERIFIED",assertFailsWith<PackageFailure> { IdeKitLoader().activate(pack,Consent(pack.digest,pack.permissions,false),emptyMap()) }.code)
        IdeKitLoader().activate(pack,Consent(pack.digest,emptySet(),true),emptyMap()).use { session ->
            assertFalse(session.hasCapability(Capability.LOCALIZATION),"An enabled package without resource consent must never supply localization")
        }
    }
    @Test fun fullKitStoreReopenDisableEnableAndUninstallFallBackWithoutRuntime() {
        val dir=Files.createTempDirectory("full-zh-kit")
        var store=PackageStore(dir)
        try {
            val pack=store.inspect(archive.readBytes(),"suik")
            store.install(archive.readBytes(),"suik",Consent(pack.digest,pack.permissions,true))
            store.enable(pack.id,emptyMap());store.close();store=PackageStore(dir)
            fun title():String {
                val bundles=store.records().filter { it.enabled }.flatMap { LocalizationResources.forPack(store.load(it.id)) }
                return UiTranslations(bundles,pack.id+"/zh-Hans").text("nav.settings")
            }
            assertEquals("设置",title())
            store.disable(pack.id);assertEquals("Settings",title())
            store.enable(pack.id,emptyMap());assertEquals("设置",title())
            store.uninstall(pack.id);assertEquals("Settings",title())
            assertTrue(Files.exists(dir.resolve(pack.digest+".suik")),"Recovery archive is retained; do not delete user files")
        } finally { store.close();dir.toFile().deleteRecursively() }
    }
    @Test fun expandedCatalogStillRejectsSecurityOverridesBadSlotsOversizeAndTampering() {
        for(key in listOf("security.approve","permissions.trust","ui.signature","ui.approveAllPermissions")) {
            val payload=(canonical(buildJsonObject { put(key,"trusted") })+"\n").toByteArray()
            assertEquals("E_LOCALIZATION",assertFailsWith<PackageFailure> { LocalizationResources.parse(payload) }.code)
        }
        assertFailsWith<PackageFailure> { LocalizationResources.parse((canonical(buildJsonObject { put("common.version","{other}") })+"\n").toByteArray()) }
        assertFailsWith<PackageFailure> { LocalizationResources.parse(ByteArray(65537)) }
        val bytes=archive.readBytes();bytes[bytes.size/2]=(bytes[bytes.size/2].toInt() xor 1).toByte()
        assertFailsWith<PackageFailure> { PackageVerifier().verify(bytes,"suik") }
        // Existing 12-key SDK Kit stays intentionally partial and compatible with the expanded host.
        val old=PackageVerifier().verify(File("../../docs/specifications/examples/packages/zh-hans-kit.suik").readBytes(),"suik")
        val bundle=LocalizationResources.forPack(old).single()
        assertEquals("Stop file",UiTranslations(listOf(bundle),bundle.selectionId).text("run.stop"))
    }
}
