package dev.blamspot.jcode

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.suicode.nativehost.*
import dev.suicode.packages.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

/** Real production composables, host, catalog, archive verification and Core storage on Android16.
 * Fixtures may only be installed in an explicitly owned emulator, never a user's tablet. */
@RunWith(AndroidJUnit4::class)
class PackageManagementUiTest {
    @get:Rule val compose=createComposeRule()
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val host get()=NativePackageHost.get(instrumentation.targetContext)
    @Before fun ownedEmulatorOnly() {
        val process=Runtime.getRuntime().exec(arrayOf("getprop","ro.kernel.qemu"))
        check(process.inputStream.bufferedReader().readText().trim()=="1") { "Refuse physical-device fixture mutation" }
        runBlocking { host.restore() };host.selectUiLanguage(null)
    }
    private fun content(block: @androidx.compose.runtime.Composable ()->Unit) { compose.setContent { MaterialTheme { NativeLocalizationProvider(host) { block() } } } }
    private fun fixture(name: String): PackagePreview {
        val bytes=instrumentation.context.assets.open("v054/$name").use { it.readBytes() }
        val pack=PackageVerifier().verify(bytes,name.substringAfterLast('.'),checkDependencies=false)
        return PackagePreview(bytes,pack.kind.suffix,pack,"owned Android16 test fixture")
    }
    @Test fun languagesSearchFiltersRuntimesWithoutAnyKitManagement() {
        content { NativeLanguageCenter(Modifier.fillMaxSize()) {} }
        compose.onNodeWithTag("languages-search").performTextInput("pYtHoN")
        compose.onNodeWithTag("runtime-python").assertIsDisplayed()
        compose.onNodeWithTag("runtime-java").assertDoesNotExist()
        compose.onNodeWithTag("languages-search").performTextReplacement("GCC")
        compose.onNodeWithTag("runtime-c").assertIsDisplayed()
        compose.onNodeWithTag("runtime-python").assertDoesNotExist()
        compose.onNodeWithTag("languages-search").performTextReplacement("不存在的运行环境")
        compose.onNodeWithTag("languages-empty").assertIsDisplayed()
        compose.onNodeWithText("Clear").performClick()
        compose.onNodeWithTag("runtime-java").assertExists()
        compose.onNodeWithText("Select components / repair").assertDoesNotExist()
    }
    @Test fun packagesSearchIncludesOfficialAndThirdPartyAndKeepsQueryAcrossTabs() {
        val fixture=fixture("third-party-utility-card.suik")
        check(host.rows.value.none { it.record.id==fixture.pack.id })
        runBlocking { host.install(fixture,fixture.pack.permissions,true) }
        try {
            content { NativePackageCenter(Modifier.fillMaxSize(),1) }
            compose.onNodeWithTag("packages-search").performTextInput("Example third-party")
            compose.onNodeWithTag("package-${fixture.pack.id}").assertIsDisplayed()
            compose.onNodeWithText("Installed").performClick()
            compose.onNodeWithTag("package-${fixture.pack.id}").assertIsDisplayed()
            compose.onNodeWithTag("packages-search").performTextReplacement("does-not-exist-054")
            compose.onNodeWithTag("packages-empty").assertIsDisplayed()
            compose.onNodeWithText("IDE Kit Library").performClick()
            compose.onNodeWithTag("packages-search").assertTextContains("does-not-exist-054")
            compose.onNodeWithTag("packages-search").performTextReplacement("简体中文")
            compose.onNodeWithTag("package-dev.suicode.localization.zh-hans").assertIsDisplayed()
            compose.onNodeWithText("Clear").performClick()
            compose.onNodeWithTag("packages-search").assertTextContains("")
        } finally { runBlocking { host.uninstall(fixture.pack.id) } }
    }
    @Test fun thirdPartyLanguageCardHasAllHostSlotsAndRealMissingComponents() {
        val basic=fixture("Python.suic");val fixture=fixture("third-party-language-kit.suik")
        val added=host.rows.value.none { it.record.id==basic.pack.id }
        if(added) runBlocking { host.install(basic,basic.pack.permissions,true) }
        try {
            runBlocking { host.install(fixture,fixture.pack.permissions,true) }
            content { NativePackageCenter(Modifier.fillMaxSize(),1) }
            compose.onNodeWithTag("packages-search").performTextInput("org.example.python.ide")
            compose.onNodeWithTag("package-org.example.python.ide").assertExists()
            compose.onNodeWithText("DAP: Not provided").assertExists()
            compose.onNodeWithText("BUILD: Not provided").assertExists()
            compose.onNodeWithText("TEST: Not provided").assertExists()
            compose.onNodeWithText("FORMAT: Not provided").assertExists()
            compose.onNodeWithText("Unverified publisher").assertExists()
        } finally {
            runBlocking { if(host.rows.value.any { it.record.id==fixture.pack.id }) host.uninstall(fixture.pack.id);if(added) host.uninstall(basic.pack.id) }
        }
    }
    @Test fun locallyImportedBasicPluginUsesTheSameHostCard() {
        val fixture=fixture("minimal.suic")
        check(host.rows.value.none { it.record.id==fixture.pack.id })
        runBlocking { host.install(fixture,fixture.pack.permissions,true) }
        try {
            content { NativePackageCenter(Modifier.fillMaxSize(),0) }
            compose.onNodeWithTag("packages-search").performTextInput(fixture.pack.id)
            compose.onNodeWithTag("package-${fixture.pack.id}").assertIsDisplayed()
            compose.onNodeWithText("Unverified publisher").assertExists()
        } finally { runBlocking { host.uninstall(fixture.pack.id) } }
    }
    @Test fun localizationPlanNeedsActualSourceAndPermissionConsentButNoUbuntu() {
        val fixture=fixture("localization-card.suik")
        content { NativePackageReview(host,fixture,onDismiss={}) }
        compose.waitUntil(30000) { compose.onAllNodesWithTag("confirm-package-plan").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("confirm-package-plan").assertIsNotEnabled()
        compose.onNodeWithTag("trust-publisher").performClick()
        compose.onNodeWithTag("confirm-package-plan").assertIsNotEnabled()
        compose.onNodeWithTag("approve-permissions").performClick()
        compose.waitUntil(30000) { !compose.onNodeWithTag("confirm-package-plan").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) }
        compose.onNodeWithTag("confirm-package-plan").assertIsEnabled()
        // Review only: installation lifecycle is exercised by the real SAF end-to-end suite.
    }
}
