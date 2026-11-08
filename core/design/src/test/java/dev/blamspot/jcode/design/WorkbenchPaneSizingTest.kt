package dev.blamspot.jcode.design
import org.junit.Test
import org.junit.Assert.*
class WorkbenchPaneSizingTest {
    @Test fun tabletSeparatesRailAndReservesEditor() {
        val b=WorkbenchPaneSizing.bounds(1200f,1f,null)
        assertFalse(b.modal);assertEquals(360f,b.width,0f);assertTrue(1200-b.width-68-12>=320)
    }
    @Test fun rotationSplitAndLargeFontUseDrawerWhenNeeded() {
        assertTrue(WorkbenchPaneSizing.bounds(600f,1f,480f).modal)
        assertTrue(WorkbenchPaneSizing.bounds(900f,1.5f,480f).modal)
        assertFalse(WorkbenchPaneSizing.bounds(1600f,1.5f,480f).modal)
    }
    @Test fun restoredWidthClampsWithoutDestroyingPreference() {
        val saved=500f
        val narrow=WorkbenchPaneSizing.bounds(800f,1f,saved)
        assertEquals(400f,narrow.width,0f)
        assertEquals(saved,WorkbenchPaneSizing.bounds(1200f,1f,saved).width,0f)
    }
    @Test fun malformedAndTinyMeasurementsNeverProduceNegativeWidths() {
        for(w in listOf(0f,40f,Float.NaN,Float.POSITIVE_INFINITY,800f)) {
            val b=WorkbenchPaneSizing.bounds(w,Float.NaN,Float.NaN)
            assertTrue(b.width.isFinite());assertTrue(b.width>=0f);assertTrue(b.maximum>=b.minimum)
        }
    }
}
