package dev.blamspot.jcode.core.distro
import kotlin.test.*
import java.nio.file.Files
class TerminalIdleProbeTest {
 @Test fun realShellWithAWaitingProgramIsNeverConsideredIdle() {
  val process=ProcessBuilder("/bin/bash","--noprofile","--norc").start()
  try {
   val pid=(java.lang.Process::class.java.getMethod("pid").invoke(process) as Long).toInt();val deadline=System.nanoTime()+3_000_000_000
   while(!TerminalIdleProbe.idle(pid) && System.nanoTime()<deadline) Thread.sleep(10)
   assertTrue(TerminalIdleProbe.idle(pid),TerminalIdleProbe.inspect(pid).detail)
   process.outputStream.write("sleep 30\n".toByteArray());process.outputStream.flush()
   val activeDeadline=System.nanoTime()+3_000_000_000
   while(TerminalIdleProbe.idle(pid) && System.nanoTime()<activeDeadline) Thread.sleep(10)
   assertFalse(TerminalIdleProbe.idle(pid))
   assertTrue(process.isAlive)
  } finally { runCatching {
      val pid=(java.lang.Process::class.java.getMethod("pid").invoke(process) as Long).toInt()
      java.io.File("/proc/$pid/task/$pid/children").readText().trim().split(Regex("\\s+")).filter { it.toIntOrNull()!=null }.forEach { child -> ProcessBuilder("/bin/kill","-KILL",child).start().waitFor() }
     };process.destroyForcibly();process.waitFor() }
 }
 @Test fun unknownPidAndRestrictedMetadataAreConservativelyBusy() {
  val root=Files.createTempDirectory("unknown-proc").toFile()
  try { assertFalse(TerminalIdleProbe.idle(-1));assertFalse(TerminalIdleProbe.idle(123,null,root)) }
  finally { root.deleteRecursively() }
 }
}
