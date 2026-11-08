package dev.blamspot.jcode.core.editor

/** Snapshot identity is changed only by text edits, never by cursor navigation or rendering. */
class CompletionTriggerPolicy {
    private var observed: Any?=null
    fun reset(snapshot: Any?) { observed=snapshot }
    fun textChanged(snapshot: Any?): Boolean = (snapshot !== observed).also { observed=snapshot }
}
