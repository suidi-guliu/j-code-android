package dev.blamspot.jcode.design

/** Widths are content dp, independent of the 68dp navigation rail. Never consumes the editor. */
object WorkbenchPaneSizing {
    const val RAIL = 68f
    const val HANDLE = 12f
    data class Bounds(val modal: Boolean, val minimum: Float, val maximum: Float, val width: Float)
    fun bounds(available: Float, fontScale: Float, requested: Float?): Bounds {
        val width=available.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val scale=fontScale.takeIf { it.isFinite() }?.coerceIn(1f,1.5f) ?: 1f
        val minimum=260f*scale
        val editorMinimum=320f*scale
        val modal=width < RAIL+HANDLE+minimum+editorMinimum
        val maximum=if(modal) (width-RAIL-24f).coerceIn(0f,520f) else minOf(560f,width-RAIL-HANDLE-editorMinimum)
        val lower=minOf(minimum,maximum)
        val initial=(width*.30f).coerceIn(300f,420f)
        val preferred=requested?.takeIf { it.isFinite() && it>0f } ?: initial
        return Bounds(modal,lower,maximum,preferred.coerceIn(lower,maximum))
    }
}
