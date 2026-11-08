package dev.blamspot.jcode.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

private val JCodeTypography = Typography(
    titleMedium=TextStyle(fontSize=18.sp,lineHeight=27.sp),
    bodyLarge=TextStyle(fontSize=14.sp,lineHeight=21.sp),
    labelLarge=TextStyle(fontSize=14.sp,lineHeight=21.sp),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 15.6.sp,
    ),
)

@Composable
fun M3Theme(
    themeMode: ThemeMode = ThemeMode.System,
    densityMode: DensityMode = DensityMode.Comfortable,
    themeBundle: ThemeBundle = ThemeBundleRegistry.default,
    uiIconSet: UiIconSet = UiIconSetRegistry.default,
    fileIconSet: FileIconSet? = null,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(
        LocalDensityMode provides densityMode,
        LocalSemanticColors provides themeBundle.semanticColors(darkTheme),
        LocalUiIconSet provides uiIconSet,
        LocalFileIconSet provides fileIconSet,
    ) {
        MaterialTheme(
            colorScheme = themeBundle.colorScheme(darkTheme),
            typography = JCodeTypography,
            shapes = androidx.compose.material3.Shapes(
                extraSmall=androidx.compose.foundation.shape.RoundedCornerShape(Radius.sm),
                small=androidx.compose.foundation.shape.RoundedCornerShape(Radius.sm),
                medium=androidx.compose.foundation.shape.RoundedCornerShape(Radius.lg),
                large=androidx.compose.foundation.shape.RoundedCornerShape(Radius.lg),
                extraLarge=androidx.compose.foundation.shape.RoundedCornerShape(Radius.lg)),
            content = content,
        )
    }
}
