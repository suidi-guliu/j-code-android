package dev.blamspot.jcode
import dev.blamspot.jcode.startup.UiPreferenceValues
import dev.blamspot.jcode.design.WorkbenchPaneSizing
import org.junit.Test
import org.junit.Assert.*
class StartupPreferenceTest {
    @Test fun missingV05PreferencesNeedNoMigration() {
        assertNull(UiPreferenceValues.language(null) { throw it })
        val width=UiPreferenceValues.paneWidth(null) { throw it }
        assertTrue(width.isNaN());assertEquals(360f,WorkbenchPaneSizing.bounds(1200f,1f,width).width,0f)
    }
    @Test fun wrongPreferenceTypesFallBackAndReportWithoutMutatingInput() {
        val saved=mapOf<String,Any>("selection-v1" to 42,"content-width-v1" to "broken")
        val errors=mutableListOf<Exception>()
        assertNull(UiPreferenceValues.language(saved["selection-v1"],errors::add))
        assertTrue(UiPreferenceValues.paneWidth(saved["content-width-v1"],errors::add).isNaN())
        assertEquals(2,errors.size);assertEquals(42,saved["selection-v1"]);assertEquals("broken",saved["content-width-v1"])
    }
    @Test fun infiniteNegativeAndExtremePanelPreferencesCannotCreateInvalidBounds() {
        for(value in listOf(Float.NaN,Float.POSITIVE_INFINITY,-1f,0f,Float.MAX_VALUE)) {
            val width=UiPreferenceValues.paneWidth(value) {}
            for(screen in listOf(0f,40f,600f,1200f)) {
                val b=WorkbenchPaneSizing.bounds(screen,2f,width)
                assertTrue(b.width.isFinite());assertTrue(b.width>=0 && b.minimum<=b.maximum)
            }
        }
    }
}
