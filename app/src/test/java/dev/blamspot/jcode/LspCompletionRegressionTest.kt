package dev.blamspot.jcode
import dev.blamspot.jcode.core.lsp.CompletionResult
import dev.blamspot.jcode.editor.toEditorItem
import org.junit.Test
import org.junit.Assert.*
class LspCompletionRegressionTest {
    @Test fun javaDisplaySignatureDoesNotReplaceServerFilterText() {
        val item=CompletionResult("println(String value) : void",2,null,null,"println(\${1:value})",2,"00001","println").toEditorItem()
        assertTrue(item.filterText.startsWith("pr"));assertEquals("println",item.filterText);assertNotNull(item.snippetText)
    }
    @Test fun plainCompletionWithoutExplicitFilterStillMatchesIdentifier() {
        val item=CompletionResult("add(int a, int b)",3,null,null,"add",1,null).toEditorItem()
        assertEquals("add",item.filterText);assertEquals("add",item.insertText)
    }
}
