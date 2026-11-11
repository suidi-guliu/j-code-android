package dev.blamspot.jcode

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import dev.suicode.packages.*
import dev.suicode.nativehost.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume
import org.junit.runner.RunWith
import java.io.File

/** Real production container/verifier/host on Android ICU. No runtime installers or user data reset. */
@RunWith(AndroidJUnit4::class)
class ZhHansRuntimeTest {
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    private fun bytes()=instrumentation.context.assets.open("sui-code-zh-hans-1.0.2.suik").use { it.readBytes() }
    private fun parameters(text:String):Map<String,String> = buildMap {
        var from=0
        while(true) {
            val start=text.indexOf('{',from)
            if(start<0) break
            val end=text.indexOf('}',start+1)
            check(end>start);put(text.substring(start+1,end),"含中文 {literal} $ \\ path");from=end+1
        }
    }
    @Test fun fullArchiveRendersAllKeysAndArgumentsOnAndroidWithoutRegex() {
        val pack=PackageVerifier().verify(bytes(),"suik")
        val bundle=LocalizationResources.forPack(pack).single()
        assertEquals(UiTextKeys.english.keys,bundle.entries.keys)
        assertTrue(bundle.entries.size>=700)
        assertFalse(pack.official)
        val chinese=UiTranslations(listOf(bundle),bundle.selectionId)
        for((key,english) in UiTextKeys.english) {
            val args=parameters(english)
            assertTrue(chinese.text(key,args).isNotBlank())
            assertTrue(UiTranslations().text(key,args).isNotBlank())
        }
        assertEquals("文件",chinese.text("nav.files"));assertEquals("运行文件",chinese.text("run.file"))
        assertEquals("设置",chinese.text("nav.settings"));assertEquals("查找",chinese.text("nav.find"))
    }
    @Test fun sourceConsentAndResourceGrantAreEnforcedByTheRealHost() = runBlocking {
        val host=NativePackageHost.get(context);host.restore()
        val pack=PackageVerifier().verify(bytes(),"suik")
        Assume.assumeFalse("Never replace an already installed user Kit",host.rows.value.any { it.record.id==pack.id })
        val preview=PackagePreview(bytes(),"suik",pack,"owned Android test")
        try {
            try { host.install(preview,pack.permissions,false);fail("Unverified source must need explicit consent") }
            catch(expected:PackageFailure) { assertEquals("E_UNVERIFIED",expected.code) }
            host.install(preview,emptySet(),true)
            assertFalse(host.localizations.value.any { it.packageId==pack.id })
            assertEquals("Files",UiTranslations(host.localizations.value,pack.id+"/zh-Hans").text("nav.files"))
        } finally { if(host.rows.value.any { it.record.id==pack.id }) host.uninstall(pack.id) }
        Unit
    }
    @Test fun zeroRuntimePlanSelectionRestoreDisableAndEnableKeepEnglishFallback() = runBlocking {
        val host=NativePackageHost.get(context);host.restore()
        val bytes=bytes();val pack=PackageVerifier().verify(bytes,"suik")
        Assume.assumeFalse("Never replace an already installed user Kit",host.rows.value.any { it.record.id==pack.id })
        val previous=host.rows.value.associate { it.record.id to it.record }
        try {
            val preview=PackagePreview(bytes,"suik",pack,"owned Android test")
            val plan=host.setup.prepare(preview)
            assertTrue(plan.status.isEmpty());assertTrue(plan.dependencies.isEmpty());assertTrue(plan.scripts.isEmpty())
            host.install(preview,pack.permissions,true)
            val selection=host.localizations.value.single { it.packageId==pack.id }.selectionId
            host.selectUiLanguage(selection);host.restore()
            assertEquals(selection,context.getSharedPreferences("sui-native-ui-language",0).getString("selection-v1",null))
            fun title()=UiTranslations(host.localizations.value,host.selectedUiLanguage.value).text("nav.files")
            assertEquals("文件",title());host.disable(pack.id);assertEquals("Files",title())
            host.enable(pack.id);assertEquals("文件",title());host.selectUiLanguage(null);assertEquals("Files",title())
        } finally { host.selectUiLanguage(null);if(host.rows.value.any { it.record.id==pack.id }) host.uninstall(pack.id) }
        for((id,record) in previous) assertEquals(record,host.rows.value.single { it.record.id==id }.record)
        Unit
    }
    @Test fun damagedAndMissingCompleteArchiveFallBackAndActualActivityStillStarts() = runBlocking {
        val host=NativePackageHost.get(context);host.restore()
        val bytes=bytes();val pack=PackageVerifier().verify(bytes,"suik")
        Assume.assumeFalse("Never replace an already installed user Kit",host.rows.value.any { it.record.id==pack.id })
        val archive=File(context.filesDir,"sui-code/native-packages/${pack.digest}.suik")
        var installed=false
        try {
            host.install(PackagePreview(bytes,"suik",pack,"owned Android test"),pack.permissions,true);installed=true
            host.selectUiLanguage(pack.id+"/zh-Hans")
            val damaged=bytes.clone().also { it[it.size/2]=(it[it.size/2].toInt() xor 1).toByte() }
            archive.writeBytes(damaged);host.restore()
            assertEquals("Files",UiTranslations(host.localizations.value,host.selectedUiLanguage.value).text("nav.files"))
            assertTrue(archive.readBytes().contentEquals(damaged));assertNotNull(host.error.value)
            assertTrue(archive.delete());host.restore()
            assertEquals("Files",UiTranslations(host.localizations.value,host.selectedUiLanguage.value).text("nav.files"))
            ActivityScenario.launch(MainActivity::class.java).use { scene ->
                instrumentation.waitForIdleSync()
                scene.onActivity { assertFalse(it.isFinishing);assertTrue(it.window.decorView.width>0) }
            }
        } finally {
            host.selectUiLanguage(null)
            if(installed) { archive.parentFile?.mkdirs();archive.writeBytes(bytes);host.uninstall(pack.id) }
        }
        Unit
    }
}
