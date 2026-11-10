package dev.blamspot.jcode.core.distro

import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.*

/** Runs the production wrapper; assertions use its actual child and exit acknowledgement. */
class NativeTaskLauncherTest {
    private fun run(exit: Int, cancelBeforeGo: Boolean = false, input: String? = null) {
        val token=UUID.randomUUID().toString()
        val recipe=File("/tmp/sui-native-task-$token.sh")
        val ready=File("/tmp/sui-task-launch-$token.sh.ready")
        val launch=File.createTempFile("sui-launch-test-", ".sh")
        val output=File.createTempFile("sui-launch-output-", ".log")
        val forbidden=File("/tmp/sui-task-forbidden-$token")
        val process: Process
        try {
            recipe.writeText("touch '${forbidden.path}'\n"+(if(input!=null) "IFS= read -r value; printf 'INPUT:%s\\n' \"\$value\"\n" else "")+"exit $exit\n")
            launch.writeText(NativeTaskLauncher.script(recipe.path,"sui-quickrun:$token/task",token,ready.path))
            process=ProcessBuilder("/bin/bash",launch.path).redirectErrorStream(true).redirectOutput(output).start()
            try {
                ready.writeText("ready")
                val pid=File(ready.path+".pid")
                val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8)
                while(!pid.isFile && process.isAlive && System.nanoTime()<deadline) Thread.sleep(10)
                assertTrue(pid.isFile,output.readText());assertTrue(pid.readText().trim().toInt()>0)
                assertFalse(forbidden.exists(),"User recipe ran before the host execution gate")
                File(ready.path+if(cancelBeforeGo) ".cancel" else ".go").writeText("ack")
                if(input!=null) { process.outputStream.write((input+"\n").toByteArray());process.outputStream.flush() }
                val code=if(cancelBeforeGo) 143 else exit
                val marker="\u001b]7713;native-$token;$code\u0007"
                while(!output.readText().contains(marker) && System.nanoTime()<deadline) Thread.sleep(10)
                assertTrue(output.readText().contains(marker),output.readText())
                assertEquals(!cancelBeforeGo,forbidden.exists())
                if(input!=null && !cancelBeforeGo) assertTrue(output.readText().contains("INPUT:$input"),output.readText())
                assertTrue(process.isAlive,"Finishing the task must retain its shell and transcript")
                process.outputStream.write("exit\n".toByteArray());process.outputStream.flush()
                assertTrue(process.waitFor(5,TimeUnit.SECONDS))
            } finally { process.destroyForcibly();process.waitFor() }
        } finally {
            (listOf(recipe,launch,output,forbidden)+listOf("",".pid",".go",".cancel").map { File(ready.path+it) }).forEach(File::delete)
        }
    }
    @Test fun silentFastSuccessRetainsShellAndReportsZero()=run(0)
    @Test fun actualFailureCodeIsPreserved()=run(37)
    @Test fun cancelledPendingChildNeverExecutesRecipe()=run(0,true)
    @Test fun backgroundTaskReceivesOriginalInteractiveInput()=run(0,input="中文 input ' literal")
}
