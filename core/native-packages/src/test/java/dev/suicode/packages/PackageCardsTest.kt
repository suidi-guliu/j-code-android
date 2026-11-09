package dev.suicode.packages

import java.io.File
import kotlinx.serialization.json.*
import kotlin.test.*

class PackageCardsTest {
    private fun fixture(card: String?,example: String="minimal-kit",kind: String="suik",api: Int=4): VerifiedPack {
        val directory=File("../../docs/specifications/examples/$example")
        val original=Json.parseToJsonElement(File(directory,"manifest.json").readText()).jsonObject
        val manifest=JsonObject(original + mapOf("api" to buildJsonObject { put("min",api);put("max",api) },"kind" to JsonPrimitive(kind)) + (card?.let { mapOf("card" to Json.parseToJsonElement(it)) } ?: emptyMap()))
        val files=directory.walkTopDown().filter { it.isFile }.associate { it.relativeTo(directory).invariantSeparatorsPath to if(it.extension=="json") (canonical(Json.parseToJsonElement(it.readText()))+"\n").toByteArray() else it.readBytes() }.toMutableMap()
        files["manifest.json"]=(canonical(manifest)+"\n").toByteArray()
        return PackageVerifier().verify(PackageVerifierTest().pack(kind,files).first,kind)
    }
    @Test fun legacyKitUsesGenericCardAndKeepsItsActualPublisherTrust() {
        val pack=fixture(null)
        assertEquals(CardCategory.GENERAL,PackageCards.forPack(pack).category);assertFalse(pack.official)
        assertEquals("E_UNVERIFIED",assertFailsWith<PackageFailure> { IdeKitLoader().activate(pack,Consent(pack.digest,pack.permissions,false),emptyMap()) }.code)
    }
    @Test fun languageKitCanDeclareOnlySomeFeaturesWithoutInventingMissingOnes() {
        val card=PackageCards.forPack(fixture("""{"version":1,"category":"language-ide-kit","languageIds":["python"],"slots":{}}"""))
        assertEquals(CardCategory.LANGUAGE_IDE_KIT,card.category);assertEquals(listOf("python"),card.languageIds)
        assertTrue(LanguageKitSlot.entries.all { it !in card.slots })
    }
    @Test fun generalCardHasOnlyInertInformationAndActualDeclaredActions() {
        val card=PackageCards.forPack(fixture("""{"version":1,"category":"utility","fields":[{"id":"description","label":"Details","type":"text","value":"Literal <b>text</b>"},{"id":"documentation","label":"Documentation","type":"link","value":"https://example.com/docs"}],"actions":["overview"]}"""))
        assertEquals("Literal <b>text</b>",card.fields.first().value);assertEquals(listOf("overview"),card.actions)
    }
    @Test fun localizationCategoryRequiresRealAuthenticatedLocalizationResources() {
        assertFailsWith<PackageFailure> { fixture("""{"version":1,"category":"localization"}""") }
        val pack=fixture("""{"version":1,"category":"localization"}""","zh-hans-kit")
        assertEquals(CardCategory.LOCALIZATION,PackageCards.forPack(pack).category)
        assertEquals(12,LocalizationResources.forPack(pack).single().entries.size)
    }
    @Test fun unknownCategoriesDynamicUiAndInventedActionsAreRejected() {
        for(card in listOf(
            """{"version":1,"category":"official-trusted"}""",
            """{"version":2,"category":"general"}""",
            """{"version":1,"category":"general","fields":[{"id":"evil","label":"Evil","type":"html","value":"<script>run()</script>"}]}""",
            """{"version":1,"category":"general","fields":[{"id":"evil","label":"Evil","type":"link","value":"javascript:run()"}]}""",
            """{"version":1,"category":"general","actions":["undeclared"]}""",
            """{"version":1,"category":"language-ide-kit","slots":{"lsp":"overview"}}""",
            """{"version":1,"category":"general","languageIds":"python"}"""
        )) assertFailsWith<PackageFailure> { fixture(card) }
    }
    @Test fun cardsRequireApi4AndCannotTurnBasicPluginsIntoIdeKits() {
        assertEquals("E_API",assertFailsWith<PackageFailure> { fixture("""{"version":1,"category":"general"}""",api=3) }.code)
        assertEquals("E_BOUNDARY",assertFailsWith<PackageFailure> { fixture("""{"version":1,"category":"language-ide-kit"}""","minimal","suic") }.code)
    }
}
