package dev.blamspot.jcode.core.term

/** Clipboard is text, never an already-framed terminal input stream. */
object TerminalPaste {
    fun normalize(text: String): String = text.replace("\u001b[200~", "").replace("\u001b[201~", "")
        .replace("\u009b200~", "").replace("\u009b201~", "")
        .filter { it=='\t' || it=='\r' || it=='\n' || (it.code>=32 && it.code!=127 && it.code !in 128..159) }
        .replace("\r\n", "\r").replace("\n", "\r")
    fun encode(text: String,bracketed: Boolean): String = normalize(text).let { if(bracketed) "\u001b[200~"+it+"\u001b[201~" else it }
}
