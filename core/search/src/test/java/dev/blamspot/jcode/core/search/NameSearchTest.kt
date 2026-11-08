package dev.blamspot.jcode.core.search
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.nio.file.Files
import kotlin.test.*
class NameSearchTest {
    @Test fun includesDirectoriesAndUnicodeWithoutReadingContents()=runBlocking {
        val root=Files.createTempDirectory("find-test").toFile()
        try {
            val folder=root.resolve("中文 Space").apply { mkdir() };folder.resolve("中文.java").writeText("Hello")
            val matches=SearchEngine().searchFileNames(root,SearchOptions("中文")).toList()
            assertEquals(2,matches.size);assertTrue(matches.any { it.isDirectory });assertTrue(matches.any { !it.isDirectory })
        } finally { root.deleteRecursively() }
    }
    @Test fun regexCaseLimitsAndSymlinkBoundary()=runBlocking {
        val root=Files.createTempDirectory("find-test").toFile();val outside=Files.createTempDirectory("outside").toFile()
        try {
            root.resolve("Alpha").mkdir();root.resolve("alpha.kt").writeText("unused");outside.resolve("Alpha-secret").writeText("secret")
            Files.createSymbolicLink(root.resolve("link").toPath(),outside.toPath())
            assertEquals(1,SearchEngine().searchFileNames(root,SearchOptions("^Alpha",isRegex=true,caseSensitive=true)).toList().size)
            assertEquals(1,SearchEngine().searchFileNames(root,SearchOptions("alpha",maxResults=1)).toList().size)
            assertTrue(SearchEngine().searchFileNames(root,SearchOptions("secret")).toList().isEmpty())
        } finally { root.deleteRecursively();outside.deleteRecursively() }
    }
    @Test fun cancelledNameWalkStopsAndInvalidRegexFails()=runBlocking {
        val root=Files.createTempDirectory("find-test").toFile()
        try {
            repeat(100) { root.resolve("file$it").writeText("") }
            var count=0
            SearchEngine().searchFileNames(root,SearchOptions("file")).take(1).collect { count++ }
            assertEquals(1,count)
            assertFailsWith<java.util.regex.PatternSyntaxException> { SearchEngine().searchFileNames(root,SearchOptions("[",isRegex=true)).toList() }
            Unit
        } finally { root.deleteRecursively() }
    }
}
