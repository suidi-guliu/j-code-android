package dev.blamspot.jcode.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import dev.suicode.packages.UiTranslations

val LocalUiTranslations=compositionLocalOf { UiTranslations() }
@Composable fun uiText(key: String,arguments: Map<String,String> = emptyMap()): String = LocalUiTranslations.current.text(key,arguments)
