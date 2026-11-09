package dev.blamspot.jcode.startup

import android.content.Context
import android.content.SharedPreferences

/** Only optional UI state gets a default; shared files are retained for diagnosis/user recovery. */
object StartupPreferences {
    fun language(context: Context): String? = try {
        UiPreferenceValues.language(context.getSharedPreferences("sui-native-ui-language",Context.MODE_PRIVATE).all["selection-v1"]) { StartupDiagnostics.report(context,"localization.preference",it) }
    } catch(e: Exception) { StartupDiagnostics.report(context,"localization.preference.read",e);null }
    fun paneWidth(context: Context,prefs: SharedPreferences): Float = try {
        UiPreferenceValues.paneWidth(prefs.all["content-width-v1"]) { StartupDiagnostics.report(context,"layout.preference",it) }
    } catch(e: Exception) { StartupDiagnostics.report(context,"layout.preference.read",e);Float.NaN }
    fun saveLanguage(context: Context,selection: String?) {
        try { context.getSharedPreferences("sui-native-ui-language",Context.MODE_PRIVATE).edit().putString("selection-v1",selection).apply() }
        catch(e: Exception) { StartupDiagnostics.report(context,"localization.preference.write",e) }
    }
    fun savePaneWidth(context: Context,prefs: SharedPreferences,value: Float) {
        if(!value.isFinite() || value<=0f) return
        try { prefs.edit().putFloat("content-width-v1",value).apply() }
        catch(e: Exception) { StartupDiagnostics.report(context,"layout.preference.write",e) }
    }
}
