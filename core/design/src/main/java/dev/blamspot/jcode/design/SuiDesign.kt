package dev.blamspot.jcode.design

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import dev.suicode.packages.DesignSystem

/** Shared public tokens drive every native screen, including settings and Kit panels. */
val suiThemeBundle: ThemeBundle = run {
    fun palette(dark: Boolean): androidx.compose.material3.ColorScheme {
        val tokens=DesignSystem.colors(dark)
        fun c(key: String)=Color(0xff000000L or tokens.getValue(key).removePrefix("#").toLong(16))
        val base=if(dark) darkColorScheme() else lightColorScheme()
        return base.copy(primary=c("accent"),onPrimary=c("onAccent"),primaryContainer=c("surfaceRaised"),onPrimaryContainer=c("text"),
            secondary=c("accent"),onSecondary=c("onAccent"),secondaryContainer=c("surfaceRaised"),onSecondaryContainer=c("text"),
            tertiary=c("success"),onTertiary=c("background"),background=c("background"),onBackground=c("text"),
            surface=c("surface"),onSurface=c("text"),surfaceVariant=c("surfaceRaised"),onSurfaceVariant=c("textSecondary"),
            surfaceContainer=c("surface"),surfaceContainerLow=c("background"),surfaceContainerHigh=c("surfaceRaised"),
            surfaceContainerHighest=c("surfaceRaised"),surfaceContainerLowest=c("background"),surfaceTint=c("accent"),
            tertiaryContainer=c("surfaceRaised"),onTertiaryContainer=c("success"),errorContainer=c("surfaceRaised"),onErrorContainer=c("error"),
            inverseSurface=c("text"),inverseOnSurface=c("background"),inversePrimary=c("accent"),surfaceDim=c("background"),surfaceBright=c("surfaceRaised"),
            outline=c("border"),outlineVariant=c("border").copy(alpha=0.35f),error=c("error"),onError=c("background"))
    }
    fun semantic(dark: Boolean): JCodeSemanticColors {
        val t=DesignSystem.colors(dark)
        fun c(k: String)=Color(0xff000000L or t.getValue(k).removePrefix("#").toLong(16))
        return JCodeSemanticColors(c("success"),c("background"),c("warning"),c("background"),c("accent"),c("onAccent"))
    }
    ThemeBundle("sui-code","sui-code","sui-code native Light / Dark design system","sui-code",palette(true),palette(false),semantic(true),semantic(false))
}
