package dev.suicode.packages

/** Read-only public design contract. Actual Compose screens are migrated separately. */
object DesignSystem {
    const val VERSION = "0.1.0"
    val spacing = listOf(0,4,8,12,16,20,24,32,40,48)
    val radius = mapOf("small" to 4,"medium" to 8)
    val typographySp = mapOf("title" to 22,"section" to 18,"body" to 14,"label" to 12,"editor" to 14,"terminal" to 14)
    fun colors(dark: Boolean): Map<String,String> = if(dark) mapOf(
        "background" to "#11151C","surface" to "#191F29","surfaceRaised" to "#232C39",
        "text" to "#E8EDF5","textSecondary" to "#AAB8CB","accent" to "#85B4FF",
        "onAccent" to "#0A2547","border" to "#526176","error" to "#FFB4AB",
        "success" to "#8BD5A4","warning" to "#F2CA78","focus" to "#ABC9FF"
    ) else mapOf(
        "background" to "#F4F6FA","surface" to "#FFFFFF","surfaceRaised" to "#E9EEF6",
        "text" to "#182231","textSecondary" to "#4D5D73","accent" to "#205CA8",
        "onAccent" to "#FFFFFF","border" to "#75849A","error" to "#AF2525",
        "success" to "#176A36","warning" to "#795100","focus" to "#174C91"
    )
    enum class WidthClass { COMPACT, MEDIUM, EXPANDED }
    fun widthClass(widthDp: Int): WidthClass {
        require(widthDp>0)
        return when { widthDp<600 -> WidthClass.COMPACT; widthDp<840 -> WidthClass.MEDIUM; else -> WidthClass.EXPANDED }
    }
    fun terminalHeight(availableDp: Int, requestedDp: Int): Int {
        require(availableDp>0)
        val maximum=(availableDp*0.55).toInt().coerceAtLeast(1)
        return requestedDp.coerceIn(minOf(160,maximum),maximum)
    }
}
