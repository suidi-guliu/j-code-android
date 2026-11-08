package dev.blamspot.jcode.core.term
import kotlin.test.*
class TerminalPasteTest {
    @Test fun unicodeAndLineEndingsArePreserved() { assertEquals("中文 +\rnext\r",TerminalPaste.encode("中文 +\r\nnext\n",false)) }
    @Test fun alreadyFramedClipboardDoesNotNestMarkers() { assertEquals("\u001b[200~abc\u001b[201~",TerminalPaste.encode("\u001b[200~abc\u001b[201~",true)) }
    @Test fun unrequestedBracketsNeverBecomeCommandPrefixes() { assertEquals("printf ok",TerminalPaste.encode("\u001b[200~printf ok\u001b[201~",false)) }
    @Test fun csiAndControlBytesAreNotClipboardCommands() { assertEquals("ab\tc",TerminalPaste.normalize("\u009b200~a\u0003b\t\u009b201~c")) }
}
