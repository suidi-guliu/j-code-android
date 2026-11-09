package dev.blamspot.jcode

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

/** Renders the APK's actual Android resources through the framework's adaptive mask. */
@RunWith(AndroidJUnit4::class)
class LauncherIconRuntimeTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun render(name: String,size: Int): Bitmap {
        val id=context.resources.getIdentifier(name,"mipmap",context.packageName)
        assertTrue("Missing launcher channel: $name",id!=0)
        val icon=context.getDrawable(id)
        assertTrue("Launcher must use actual adaptive layers",icon is AdaptiveIconDrawable)
        val bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888)
        icon!!.setBounds(0,0,size,size);icon.draw(Canvas(bitmap));return bitmap
    }
    @Test fun releaseDebugBetaAndRoundIconsShareExactUserArtworkAtSmallAndLargeSizes() {
        val folder=File(context.filesDir,"sui-code/test-evidence/icons").apply { mkdirs() }
        for(size in listOf(48,96,216)) {
            val original=render("ic_launcher",size)
            for(name in listOf("ic_launcher_round","ic_launcher_debug","ic_launcher_debug_round","ic_launcher_beta","ic_launcher_beta_round")) assertTrue("$name at ${size}px differs",original.sameAs(render(name,size)))
            val pixels=IntArray(size*size);original.getPixels(pixels,0,size,0,0,size,size)
            val colored=pixels.count { Color.alpha(it)>240 && Color.blue(it)>100 && Color.blue(it)>Color.red(it)+60 }
            assertTrue("User S., brackets and gradient must remain visible at $size",colored>size*size/20)
            File(folder,"launcher-${size}px.png").outputStream().use { original.compress(Bitmap.CompressFormat.PNG,100,it) }
        }
    }
    @Test fun adaptiveBackgroundIsOriginalColorAndForegroundDoesNotPaintItsOwnBackground() {
        val background=context.resources.getIdentifier("ic_launcher_adaptive_background","color",context.packageName)
        assertEquals(Color.rgb(0x15,0x19,0x22),context.getColor(background))
        val id=context.resources.getIdentifier("ic_launcher_foreground","drawable",context.packageName)
        val foreground=context.getDrawable(id)!!
        val bitmap=Bitmap.createBitmap(216,216,Bitmap.Config.ARGB_8888)
        foreground.setBounds(0,0,216,216);foreground.draw(Canvas(bitmap))
        assertEquals(0,Color.alpha(bitmap.getPixel(0,0)))
        assertEquals(0,Color.alpha(bitmap.getPixel(108,8)))
        val pixels=IntArray(216*216);bitmap.getPixels(pixels,0,216,0,0,216,216)
        assertTrue(pixels.count { Color.alpha(it)==0 }>pixels.size*0.65)
    }
}
