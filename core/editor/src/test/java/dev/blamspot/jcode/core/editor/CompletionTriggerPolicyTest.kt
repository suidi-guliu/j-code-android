package dev.blamspot.jcode.core.editor
import kotlin.test.*
class CompletionTriggerPolicyTest {
    @Test fun openingOrNavigatingDoesNotStartCompletion() { val s=Any();val p=CompletionTriggerPolicy();p.reset(s);repeat(20) { assertFalse(p.textChanged(s)) } }
    @Test fun typingTriggersAndMovingAfterTypingDoesNot() { val p=CompletionTriggerPolicy();p.reset(Any());val edited=Any();assertTrue(p.textChanged(edited));assertFalse(p.textChanged(edited)) }
    @Test fun switchingTabsSeedsWithoutPopup() { val p=CompletionTriggerPolicy();p.reset(Any());val other=Any();p.reset(other);assertFalse(p.textChanged(other)) }
}
