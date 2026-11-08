package dev.blamspot.jcode.core.distro
import kotlin.test.*
class SourceUrisTest {
    @Test fun fileNamesRoundTripWithoutFormDecoding() {
        for(path in listOf("/workspace/中文 + space/a#b.java","/workspace/a%b/file.cpp","/workspace/quoted's/name.py")) assertEquals(path,SourceUris.path(SourceUris.fileUri(path)))
    }
    @Test fun equivalentUriAndPathMatchButPlusIsNotSpace() {
        assertTrue(SourceUris.samePath("file:///workspace/a%20b/Main.java","/workspace/a b/Main.java"))
        assertFalse(SourceUris.samePath("file:///workspace/a+b/Main.java","/workspace/a b/Main.java"))
    }
}
