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
}
