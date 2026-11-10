package dev.blamspot.jcode.tasks
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test
class TaskHistoryTest {
 @Test fun silentSuccessFailureAndCancellationHaveRealSeparateResults() {
  val root=Files.createTempDirectory("tasks").toFile()
  try {
   val store=TaskHistoryStore(root)
   val a=store.start("Python Build","kit","build","/中文/a.py","/中文");store.bind(a.id,"one");store.finish(a.id,0)
   val b=store.start("C++ Build","kit","build","/main.cpp","/");store.bind(b.id,"two");store.finish(b.id,2)
   val c=store.start("Run","kit","run","/main.py",null);store.bind(c.id,"three");store.finish(c.id,null,true);store.finish(c.id,143)
   assertEquals(listOf(TaskOutcome.SUCCEEDED,TaskOutcome.FAILED,TaskOutcome.CANCELLED),store.records.value.map { it.outcome })
   assertEquals(listOf(0,2,143),store.records.value.map { it.exitCode })
   assertTrue(store.records.value.all { it.ended!=null })
   val reopened=TaskHistoryStore(root);reopened.load();assertEquals(store.records.value,reopened.records.value)
  } finally { root.deleteRecursively() }
 }
 @Test fun chunkedUnicodeAndConcurrentSessionOwnershipArePreserved() {
  val root=Files.createTempDirectory("logs").toFile()
  try {
   val store=TaskHistoryStore(root);val a=store.start("A","kit","run","/a",null);val b=store.start("B","kit","run","/b",null);store.bind(a.id,"a");store.bind(b.id,"b")
   val bytes="你好\n".toByteArray();store.append("a",bytes.copyOfRange(0,2),2);store.append("b","other\n".toByteArray(),6);store.append("a",bytes.copyOfRange(2,bytes.size),bytes.size-2)
   assertEquals("你好\n",store.logFile(a.id).readText());assertEquals("other\n",store.logFile(b.id).readText())
   val reopened=TaskHistoryStore(root);reopened.load();assertTrue(reopened.records.value.all { it.outcome==TaskOutcome.INTERRUPTED && it.exitCode==null })
  } finally { root.deleteRecursively() }
 }
 @Test fun archiveStorageFailureKeepsRealTaskOutcomeAndReportsIncompleteLog() {
  val directory=Files.createTempDirectory("blocked-history").toFile()
  try {
   val root=java.io.File(directory,"not-a-directory").apply { writeText("preserved") }
   val store=TaskHistoryStore(root);val task=store.start("Build","kit","build","/file",null)
   store.bind(task.id,"one");store.append("one","output".toByteArray(),6);store.finish(task.id,23)
   assertEquals(TaskOutcome.FAILED,store.records.value.single().outcome)
   assertEquals(23,store.records.value.single().exitCode)
   assertTrue(store.records.value.single().truncated);assertNotNull(store.warning.value)
   assertEquals("preserved",root.readText())
  } finally { directory.deleteRecursively() }
 }

}
