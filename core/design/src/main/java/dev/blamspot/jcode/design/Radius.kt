package dev.blamspot.jcode.design

import androidx.compose.ui.unit.dp

/** Corner radii for surfaces, cards, chips and pills. */
object Radius {
    /** Square — an explicit zero for a surface that deliberately has no rounding. */
    val none = 0.dp

    val xs = 2.dp
    val sm = 4.dp
    val md = 8.dp
    val lg = 8.dp
    val xl = 8.dp
    val xxl = 8.dp
    val xxxl = 8.dp
    val sheet = 8.dp

    /** Legacy slot retained for source compatibility; native controls use the medium radius. */
    val pill = 8.dp
}
