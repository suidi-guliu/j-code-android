package dev.blamspot.jcode.core.distro

import kotlin.test.*
class NativeOwnerScopeTest {
    @Test fun parentCleanupIncludesPayloadButCannotMatchAnotherTaskPrefix() {
        assertTrue(NativeOwnerScope.contains("sui-quickrun:abc","sui-quickrun:abc/task"))
        assertTrue(NativeOwnerScope.contains("sui-quickrun:abc","sui-quickrun:abc"))
        assertFalse(NativeOwnerScope.contains("sui-quickrun:abc","sui-quickrun:abcd/task"))
        assertFalse(NativeOwnerScope.contains("sui-quickrun:abc/task","sui-quickrun:abc"))
    }
}
