package dev.blamspot.jcode

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import dev.suicode.packages.UiTranslations
import dev.suicode.packages.UiTextKeys
import dev.blamspot.jcode.startup.StartupPreferences
import dev.blamspot.jcode.startup.StartupDiagnostics
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/** Runs with Android libcore/ICU, unlike JVM regex tests. Never clears target app data or rootfs. */
@RunWith(AndroidJUnit4::class)
class StartupRuntimeTest {
    @Test fun androidRejectsFormerRegexAndNewEnglishRendererWorksForEveryKey() {
        try { Pattern.compile("\\{([a-z][a-zA-Z0-9]*)}");fail("Android must reproduce the former ICU syntax failure") }
        catch(expected: PatternSyntaxException) { android.util.Log.i("SuiStartupTest","Former v0.5.1 expression rejected by actual Android ICU",expected) }
        val ui=UiTranslations()
        for((key,english) in UiTextKeys.english) {
            val args=when { "{count}" in english -> mapOf("count" to "1");"{scope}" in english -> mapOf("scope" to "test");else -> emptyMap() }
            assertTrue(ui.text(key,args).isNotBlank())
        }
    }
    @Test fun actualActivityReachesContentOnRepeatedColdActivityStarts() {
        val instrument=InstrumentationRegistry.getInstrumentation()
        repeat(3) {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                instrument.waitForIdleSync()
                scenario.onActivity { activity ->
                    assertTrue(activity.window.decorView.width>0)
                    assertTrue(activity.window.decorView.height>0)
                    assertFalse(activity.isFinishing)
                }
            }
        }
    }
    @Test fun preferencesAndLocalizationFailureAreOptionalAndLogged() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val language=context.getSharedPreferences("sui-native-ui-language",0)
        val pane=context.getSharedPreferences("sui-workbench-layout",0)
        val oldLanguage=language.all["selection-v1"]
        val oldPane=pane.all["content-width-v1"]
        try {
            assertTrue(language.edit().putInt("selection-v1",7).commit())
            assertTrue(pane.edit().putString("content-width-v1","broken").commit())
            assertNull(StartupPreferences.language(context))
            assertTrue(StartupPreferences.paneWidth(context,pane).isNaN())
            assertEquals(7,language.all["selection-v1"])
            assertEquals("broken",pane.all["content-width-v1"])
        } finally {
            // Restore only the two owned test keys, preserving all unrelated preferences.
            fun restore(p:android.content.SharedPreferences,key:String,value:Any?) {
                val e=p.edit()
                when(value) { null -> e.remove(key);is String -> e.putString(key,value);is Float -> e.putFloat(key,value);is Int -> e.putInt(key,value);is Boolean -> e.putBoolean(key,value);is Long -> e.putLong(key,value);is Set<*> -> e.putStringSet(key,value.filterIsInstance<String>().toSet());else -> error("Unsupported fixture value") }
                check(e.commit())
            }
            restore(language,"selection-v1",oldLanguage);restore(pane,"content-width-v1",oldPane)
        }
    }
    @Test fun damagedOrMissingLanguageArchiveFallsBackWithoutRemovingOtherPackages() = kotlinx.coroutines.runBlocking {
        val instrument=InstrumentationRegistry.getInstrumentation()
        val context=instrument.targetContext
        val host=dev.suicode.nativehost.NativePackageHost.get(context)
        host.restore()
        val bytes=instrument.context.assets.open("zh-hans-kit.suik").use { it.readBytes() }
        val pack=dev.suicode.packages.PackageVerifier().verify(bytes,"suik")
        org.junit.Assume.assumeFalse("Do not replace an existing user-owned example",host.rows.value.any { it.record.id==pack.id })
        val previous=host.rows.value.associate { it.record.id to it.record.digest }
        val prefs=context.getSharedPreferences("sui-native-ui-language",0)
        val old=prefs.all["selection-v1"]
        val archive=java.io.File(context.filesDir,"sui-code/native-packages/${pack.digest}.suik")
        var installed=false
        try {
            host.install(dev.suicode.nativehost.PackagePreview(bytes,"suik",pack,"instrumentation-only fixture"),pack.permissions,true)
            installed=true
            val bundle=host.localizations.value.single { it.packageId==pack.id }
            host.selectUiLanguage(bundle.selectionId)
            assertEquals("文件",UiTranslations(host.localizations.value,host.selectedUiLanguage.value).text("nav.files"))
            val damaged=bytes.clone().also { it[it.size/2]=(it[it.size/2].toInt() xor 1).toByte() }
            archive.writeBytes(damaged);host.restore()
            assertEquals("Files",UiTranslations(host.localizations.value,host.selectedUiLanguage.value).text("nav.files"))
            assertTrue(archive.readBytes().contentEquals(damaged));assertNotNull(host.error.value)
            assertTrue(archive.delete());host.restore()
            assertEquals("Files",UiTranslations(host.localizations.value,host.selectedUiLanguage.value).text("nav.files"))
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                instrument.waitForIdleSync()
                scenario.onActivity { assertFalse(it.isFinishing);assertTrue(it.window.decorView.width>0) }
            }
            for((id,digest) in previous) assertEquals(digest,host.rows.value.single { it.record.id==id }.record.digest)
        } finally {
            if(installed) { archive.parentFile?.mkdirs();archive.writeBytes(bytes);host.uninstall(pack.id) }
            host.selectUiLanguage(null)
            val e=prefs.edit()
            when(old) { null -> e.remove("selection-v1");is String -> e.putString("selection-v1",old);is Int -> e.putInt("selection-v1",old);is Float -> e.putFloat("selection-v1",old);is Long -> e.putLong("selection-v1",old);is Boolean -> e.putBoolean("selection-v1",old);is Set<*> -> e.putStringSet("selection-v1",old.filterIsInstance<String>().toSet()) }
            check(e.commit())
        }
        Unit
    }

}
