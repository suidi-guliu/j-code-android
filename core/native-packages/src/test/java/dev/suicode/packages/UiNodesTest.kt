package dev.suicode.packages

import kotlinx.serialization.json.*
import kotlin.test.*

class UiNodesTest {
    private fun nodes(value: String)=Json.parseToJsonElement(value).jsonArray
    @Test fun nativeWidgetsAndSameKitCommandsValidate() {
        UiNodes.validate(nodes("""[{"id":"tools","type":"column","children":[{"id":"action","type":"button","label":"Count","command":"statistics"},{"id":"count","type":"metric","text":"Files: ${'$'}{result:statistics.files}"},{"id":"name","type":"input","label":"Name","value":""}]}]"""),setOf("statistics"))
    }
    @Test fun globalStyleOverridesAndUnknownWidgetsAreRejected() {
        for(value in listOf("""[{"id":"x","type":"text","text":"hello","globalTheme":"red"}]""","""[{"id":"x","type":"webview","html":"script"}]""")) assertFailsWith<PackageFailure> { UiNodes.validate(nodes(value)) }
    }
    @Test fun actionsCannotAddressAnotherKit() {
        val e=assertFailsWith<PackageFailure> { UiNodes.validate(nodes("""[{"id":"action","type":"button","label":"Run","command":"foreign"}]"""),setOf("mine")) }
        assertEquals("E_COMPONENT",e.code)
    }
    @Test fun duplicateIdsAndUnboundedTreesAreRejected() {
        assertFailsWith<PackageFailure> { UiNodes.validate(nodes("""[{"id":"x","type":"text","text":"a"},{"id":"x","type":"text","text":"b"}]""")) }
        var tree="""{"id":"leaf","type":"text","text":"hello"}"""
        repeat(9) { tree="""{"id":"n$it","type":"column","children":[$tree]}""" }
        assertEquals("E_SIZE",assertFailsWith<PackageFailure> { UiNodes.validate(nodes("[$tree]")) }.code)
    }
    @Test fun newCoreStillSupportsVersionOnePackages() {
        assertTrue(CoreApi.compatible(1,1));assertTrue(CoreApi.compatible(2,2));assertTrue(CoreApi.compatible(3,3));assertFalse(CoreApi.compatible(4,5));assertFalse(CoreApi.compatible(2,1))
    }
}
