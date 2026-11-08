package dev.suicode.nativehost

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.blamspot.jcode.design.LocalUiTranslations
import dev.blamspot.jcode.design.uiText
import dev.suicode.packages.UiTranslations

@Composable
fun NativeLocalizationProvider(content: @Composable ()->Unit) {
    val context=LocalContext.current
    val host=remember(context) { NativePackageHost.get(context) }
    val bundles by host.localizations.collectAsState()
    val selection by host.selectedUiLanguage.collectAsState()
    val strings=remember(bundles,selection) { UiTranslations(bundles,selection) }
    CompositionLocalProvider(LocalUiTranslations provides strings,content=content)
}

@Composable
fun NativeLanguageSelection(host: NativePackageHost) {
    val bundles by host.localizations.collectAsState()
    val selection by host.selectedUiLanguage.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    Text(uiText("ui.language"),style=MaterialTheme.typography.titleSmall)
    Box {
        OutlinedButton(onClick={expanded=true}) { Text(bundles.singleOrNull { it.selectionId==selection }?.let { "${it.displayName} (${it.locale})" } ?: uiText("ui.english")) }
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
            DropdownMenuItem(text={Text("English (default)")},onClick={host.selectUiLanguage(null);expanded=false})
            bundles.forEach { bundle -> DropdownMenuItem(text={Text("${bundle.displayName} (${bundle.locale})\n${bundle.packageId}")},onClick={host.selectUiLanguage(bundle.selectionId);expanded=false}) }
        }
    }
    Text(uiText("ui.fallback"),style=MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(8.dp))
}
