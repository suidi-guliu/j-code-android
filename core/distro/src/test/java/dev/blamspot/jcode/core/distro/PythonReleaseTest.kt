package dev.blamspot.jcode.core.distro

import kotlin.test.*
class PythonReleaseTest {
    @Test fun stableVersionsUseSemanticOrderAndExcludePrereleases() {
        assertEquals("3.15.2",PythonReleaseProvider.parse("<title>v3.16.0a1</title><title>v3.15.2</title><title>v3.9.99</title><title>v3.14.10</title>"))
        assertFailsWith<IllegalStateException> { PythonReleaseProvider.parse("<title>v3.16.0rc1</title>") }
    }
}
