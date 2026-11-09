package dev.suicode.packages

import kotlinx.serialization.json.*

/** API4 host-owned cards. Metadata is inert; category never changes trust, grants or loaders. */
enum class CardCategory(val wire: String) {
    LANGUAGE_IDE_KIT("language-ide-kit"), LOCALIZATION("localization"), DEVELOPMENT_TOOL("development-tool"), UTILITY("utility"), GENERAL("general");
}
enum class LanguageKitSlot(val wire: String, val componentTypes: Set<String>) {
    RUNTIME("runtime",emptySet()), LSP("lsp",setOf("lsp")), DAP("dap",setOf("dap")),
    BUILD("build",setOf("build")), TEST("test",setOf("test")), RUN("run",setOf("command")),
    FORMAT("format",setOf("format")), LINT("lint",setOf("lint"));
}
data class CardField(val id: String, val label: String, val type: String, val value: String)
data class PackageCard(val category: CardCategory=CardCategory.GENERAL, val basicPlugin: String?=null, val languageIds: List<String> = emptyList(), val slots: Map<LanguageKitSlot,String> = emptyMap(), val fields: List<CardField> = emptyList(), val actions: List<String> = emptyList(), val formatVersion: Int=1)

object PackageCards {
    const val VERSION=1
    fun forPack(pack: VerifiedPack): PackageCard {
        val raw=pack.manifest["card"] ?: return PackageCard()
        if(pack.manifest.getValue("api").jsonObject.getValue("min").jsonPrimitive.int<4) fail("E_API","Card metadata requires minimum Core API 4")
        val card=raw as? JsonObject ?: fail("E_CARD","card must be an object")
        card.keysExactly(setOf("version","category"),setOf("basicPlugin","languageIds","slots","fields","actions"))
        if((card["version"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull!=VERSION) fail("E_CARD","Unsupported card version")
        val category=CardCategory.entries.firstOrNull { it.wire==card.string("category") } ?: fail("E_CARD","Unknown card category")
        if(pack.kind==PackKind.PLUGIN && (category!=CardCategory.GENERAL || "slots" in card || "basicPlugin" in card || "languageIds" in card || "actions" in card)) fail("E_BOUNDARY","Basic plugins cannot declare IDE cards or commands")
        val declarations=pack.declarations().associateBy { it.id }
        val basic=card["basicPlugin"]?.let { (it as? JsonPrimitive)?.takeIf { it.isString }?.content ?: fail("E_CARD","basicPlugin must be a string") }
        if(basic!=null && pack.manifest.array("dependencies").none { it.jsonObject.string("id")==basic }) fail("E_CARD","basicPlugin must reference a declared dependency")
        val languages=(card["languageIds"]?.let { it as? JsonArray ?: fail("E_CARD","languageIds must be an array") } ?: JsonArray(emptyList())).map {
            (it as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { id -> id.matches(Regex("[a-z][a-z0-9+_.-]{0,63}")) } ?: fail("E_CARD","Invalid language ID")
        }
        if(languages.size>16 || languages.distinct().size!=languages.size) fail("E_CARD","Duplicate/too many languages")
        val slots=linkedMapOf<LanguageKitSlot,String>()
        val rawSlots=card["slots"]?.let { it as? JsonObject ?: fail("E_CARD","slots must be an object") } ?: JsonObject(emptyMap())
        for((name,value) in rawSlots) {
            val slot=LanguageKitSlot.entries.firstOrNull { it.wire==name } ?: fail("E_CARD","Unknown language slot: $name")
            val id=(value as? JsonPrimitive)?.takeIf { it.isString }?.content ?: fail("E_CARD","Slot references must be strings")
            if(slot==LanguageKitSlot.RUNTIME) {
                if(KitRequirements.forPack(pack).none { it.id==id && it.provider=="environment" }) fail("E_CARD","runtime must reference a read-only environment requirement")
            } else if(declarations[id]?.type !in slot.componentTypes) fail("E_CARD","Wrong or missing component for $name")
            if(slot==LanguageKitSlot.RUN && declarations.getValue(id).descriptor.string("operation")!="SIMPLE_RUN") fail("E_CARD","run slot must reference a SimpleRun project command")
            slots[slot]=id
        }
        if(category!=CardCategory.LANGUAGE_IDE_KIT && (basic!=null || languages.isNotEmpty() || slots.isNotEmpty())) fail("E_CARD","Language fields require language-ide-kit category")
        if(category==CardCategory.LOCALIZATION && declarations.values.none { it.type=="localization" }) fail("E_CARD","localization card requires actual localization resources")
        val rawFields=card["fields"]?.let { it as? JsonArray ?: fail("E_CARD","fields must be an array") } ?: JsonArray(emptyList())
        if(rawFields.size>8 || (category==CardCategory.LANGUAGE_IDE_KIT && rawFields.isNotEmpty())) fail("E_CARD","Language cards use the fixed host structure; other cards allow at most eight information fields")
        val ids=mutableSetOf<String>()
        val fields=rawFields.map { element ->
            val f=element as? JsonObject ?: fail("E_CARD","Field must be an object")
            f.keysExactly(setOf("id","label","type","value"))
            val id=f.string("id");val label=f.string("label");val type=f.string("type");val value=f.string("value")
            if(!id.matches(Regex("[a-z][a-z0-9.-]{0,63}")) || !ids.add(id)) fail("E_CARD","Duplicate/invalid field ID")
            if(label.length !in 1..80 || value.length !in 1..1024 || (label+value).any { it=='\u0000' || it in '\u202a'..'\u202e' || it in '\u2066'..'\u2069' }) fail("E_CARD","Invalid display text")
            if(type !in setOf("text","link")) fail("E_CARD","Only inert text and HTTPS link fields are supported")
            if(type=="link") {
                val uri=runCatching { java.net.URI(value) }.getOrNull()
                if(uri?.scheme!="https" || uri.host.isNullOrBlank() || uri.userInfo!=null) fail("E_CARD","Card links must be HTTPS without embedded credentials")
            }
            CardField(id,label,type,value)
        }
        val rawActions=card["actions"]?.let { it as? JsonArray ?: fail("E_CARD","actions must be an array") } ?: JsonArray(emptyList())
        val actions=rawActions.map { (it as? JsonPrimitive)?.takeIf { it.isString }?.content ?: fail("E_CARD","Invalid action") }
        if(actions.size>8 || actions.distinct().size!=actions.size || actions.any { declarations[it]?.type !in setOf("command","workflow","panel","build","test","format","lint") }) fail("E_CARD","Actions must reference supported declared components")
        return PackageCard(category,basic,languages,slots,fields,actions)
    }
}
