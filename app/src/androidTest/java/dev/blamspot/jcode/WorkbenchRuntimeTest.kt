package dev.blamspot.jcode

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.blamspot.jcode.lsp.LspLocationEntry
import dev.suicode.nativehost.NativePackageHost
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Actual complete workbench, editor and activity. No runtime-readiness or LSP reply fixtures. */
@RunWith(AndroidJUnit4::class)
class WorkbenchRuntimeTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun ownedEmulatorOnly() {
        assertEquals("1",Runtime.getRuntime().exec(arrayOf("getprop","ro.kernel.qemu")).inputStream.bufferedReader().readText().trim())
        val host=NativePackageHost.get(compose.activity);runBlocking { host.restore() };host.selectUiLanguage(null)
    }
    @Test fun actualDefinitionTargetOpensAtByteAccurateCaretAndFocusModesKeepDirtyEditor() {
        lateinit var model: MainViewModel
        compose.runOnUiThread { model=ViewModelProvider(compose.activity)[MainViewModel::class.java] }
        val directory=File(compose.activity.cacheDir,"v055-owned-ui/中文 space").apply { mkdirs() }
        val target=File(directory,"math.cpp").apply { writeText("// 中文路径\nint add(int a, int b) { return a+b; }\n") }
        val prefs=compose.activity.getSharedPreferences("sui-workbench-layout",0);val old=prefs.all.toMap()
        compose.runOnUiThread { model.openLspLocationEntry(LspLocationEntry(target.path,1,4,"int add(int a, int b)")) }
        compose.waitUntil(30000) { model.editorGroup.value.activeTab?.filePath?.path==target.path }
        val expected="// 中文路径\nint ".toByteArray().size
        compose.waitUntil(15000) { model.editorGroup.value.activeTab?.editorState?.carets?.value?.firstOrNull()?.offset==expected }
        assertEquals(target.path,model.editorGroup.value.activeTab!!.filePath.path)
        val edited=target.readText()+"// 未保存编辑应保留\n"
        compose.runOnUiThread { model.replaceFileText(target.path,edited) }
        compose.waitUntil(10000) { model.readOpenFileText(target.path)==edited }
        compose.onNodeWithContentDescription("Maximize editor").performClick()
        compose.onNodeWithContentDescription("Restore workbench").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Maximize editor").assertIsDisplayed()
        assertEquals(edited,model.readOpenFileText(target.path))
        assertEquals("Maximize/restore must not rewrite saved pane widths",old,prefs.all.toMap())
        // The real dock can be expanded even with no Ubuntu; no fake installed runtime is needed.
        compose.onAllNodesWithContentDescription("Terminal").onFirst().performClick()
        compose.onNodeWithContentDescription("Maximize bottom panel").performClick()
        compose.onNodeWithContentDescription("Restore workbench").assertIsDisplayed().performClick()
        assertEquals(edited,model.readOpenFileText(target.path))
        assertEquals(old,prefs.all.toMap())
        android.util.Log.i("SuiWorkbenchTest","PASS actual target math.cpp at UTF-8 caret, dirty buffer and saved panes retained across editor/dock maximize/restore")
    }
}
