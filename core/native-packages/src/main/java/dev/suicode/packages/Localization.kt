package dev.suicode.packages

import java.util.Locale
import kotlinx.serialization.json.*

/** Core-owned public key registry. Security/permission text is intentionally not localizable. */
object UiTextKeys {
    val english: Map<String,String> = mapOf(
        "nav.files" to "Files", "nav.find" to "Find", "nav.languages" to "Languages", "nav.packages" to "Packages", "nav.settings" to "Settings",
        "files.internal" to "Internal Files", "files.filter" to "Filter visible names", "files.filterHint" to "Filters loaded entries only. Use Find for nested names and contents.",
        "files.matches" to "{count} visible matches", "files.newFile" to "New File", "files.newFolder" to "New Folder", "files.refresh" to "Refresh", "files.more" to "More file actions",
        "find.content" to "File contents", "find.names" to "Files and folders", "find.current" to "Current document", "find.query" to "Search {scope}",
        "find.scope" to "Search scope", "find.hint" to "Search names or contents across the selected folder.",
        "run.file" to "Run file", "run.stop" to "Stop file", "run.debug" to "Debug", "run.continue" to "Continue", "run.stopDebug" to "Stop debug", "run.overview" to "Overview",
        "project.build" to "Build", "project.test" to "Test", "project.run" to "Run Project", "project.tasks" to "Advanced tasks",
        "packages.title" to "Packages", "languages.title" to "Languages", "ui.language" to "Interface language", "ui.english" to "English (default)",
        "ui.fallback" to "Missing translations use English. Language resources do not install development environments."
    )
}

data class LocalizationDescriptor(val locale: String,val displayName: String,val fallback: String,val resource: String) {
    companion object {
        fun parse(d: JsonObject): LocalizationDescriptor {
            d.keysExactly(setOf("locale","displayName","fallback","resource"))
            val tag=d.string("locale")
            if(!tag.matches(Regex("[a-z]{2,3}(-[A-Za-z0-9]{2,8}){0,3}")) || Locale.forLanguageTag(tag).toLanguageTag()!=tag || tag=="en") fail("E_LOCALIZATION","Use a canonical BCP47 locale other than reserved en")
            val name=d.string("displayName")
            if(name.length !in 1..80 || unsafe(name)) fail("E_LOCALIZATION","Invalid display name")
            if(d.string("fallback")!="en") fail("E_LOCALIZATION","Only Core English fallback is supported in API 4")
            val resource=d.string("resource")
            if(!resource.matches(Regex("resources/i18n/[A-Za-z0-9_-]+\\.json"))) fail("E_LOCALIZATION","Resource must be resources/i18n/<name>.json")
            return LocalizationDescriptor(tag,name,"en",resource)
        }
    }
}

data class LocalizationBundle(val packageId: String,val locale: String,val displayName: String,val entries: Map<String,String>) {
    val selectionId: String get()="$packageId/$locale"
}

/** Parsed only from authenticated payload bytes; no paths, classes, HTML or scripts are evaluated. */
object LocalizationResources {
    fun parse(bytes: ByteArray): Map<String,String> {
        if(bytes.size>65536) fail("E_LOCALIZATION","Translation resource exceeds 64 KiB")
        val d=jsonObject(bytes)
        if(d.isEmpty() || d.size>UiTextKeys.english.size) fail("E_LOCALIZATION","Invalid translation key count")
        return d.mapValues { (key,value) ->
            val fallback=UiTextKeys.english[key] ?: fail("E_LOCALIZATION","Unknown or protected key: $key")
            val text=(value as? JsonPrimitive)?.takeIf { it.isString }?.content ?: fail("E_LOCALIZATION","String required: $key")
            if(text.length !in 1..2048 || unsafe(text) || UiPlaceholders.parameters(text)!=UiPlaceholders.parameters(fallback)) fail("E_LOCALIZATION","Invalid text/placeholders: $key")
            text
        }
    }
    fun forPack(pack: VerifiedPack): List<LocalizationBundle> = pack.declarations().filter { it.type=="localization" }.map { c ->
        val d=LocalizationDescriptor.parse(c.descriptor)
        LocalizationBundle(pack.id,d.locale,d.displayName,parse(pack.content(d.resource)))
    }
}
private fun unsafe(s: String)=s.any { (it.code<32 && it!='\n' && it!='\t') || it in '\u202a'..'\u202e' || it in '\u2066'..'\u2069' || it=='\u007f' }

/** Host supplies only enabled, consented bundles. Selection is explicit, never first-provider-wins. */
class UiTranslations(bundles: List<LocalizationBundle> = emptyList(),selection: String?=null) {
    private val entries=bundles.singleOrNull { it.selectionId==selection }?.entries.orEmpty().toMap()
    fun text(key: String,arguments: Map<String,String> = emptyMap()): String {
        val english=UiTextKeys.english[key] ?: throw IllegalArgumentException("Unknown UI key $key")
        val expected=UiPlaceholders.parameters(english)
        require(arguments.keys==expected) { "Placeholder arguments differ for $key" }
        return UiPlaceholders.render(entries[key] ?: english,arguments)
    }
}

/** Literal scanner shared by verification and rendering. Android ICU and JVM regex differ on `}`. */
internal object UiPlaceholders {
    private data class Slot(val start: Int,val end: Int,val name: String)
    private fun slots(text: String): List<Slot> {
        val out=mutableListOf<Slot>()
        var index=0
        while(index<text.length) {
            when(text[index]) {
                '}' -> fail("E_LOCALIZATION","Unmatched closing placeholder brace")
                '{' -> {
                    val end=text.indexOf('}',index+1)
                    if(end<0) fail("E_LOCALIZATION","Unclosed placeholder")
                    val name=text.substring(index+1,end)
                    if(name.isEmpty() || name[0] !in 'a'..'z' || name.any { it !in 'a'..'z' && it !in 'A'..'Z' && it !in '0'..'9' }) fail("E_LOCALIZATION","Invalid placeholder name")
                    out+=Slot(index,end+1,name);index=end
                }
            }
            index++
        }
        return out
    }
    fun parameters(text: String): Set<String> = slots(text).map { it.name }.toSet()
    fun render(text: String,arguments: Map<String,String>): String = buildString {
        var previous=0
        for(slot in slots(text)) {
            append(text,previous,slot.start);append(arguments.getValue(slot.name));previous=slot.end
        }
        append(text,previous,text.length)
    }
}
