package dev.blamspot.jcode.design

import androidx.compose.ui.unit.dp

/** Native 4 dp spacing scale; legacy slot names retain source compatibility. */
object Space {
    /** No gap — an explicit zero, so it reads as a decision rather than an omission. */
    val none = 0.dp

    /** Separators and 1px rules only. */
    val hairline = 1.dp

    val xxs = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val sm = 8.dp
    val ms = 12.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}
