package dev.blamspot.jcode.core.lsp
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import kotlin.test.*
class LspStartupCancellationTest {
    @Test fun closingDuringSpawnReapsTransportReturnedAfterClose()=runBlocking {
        val spawning=CountDownLatch(1);val finish=CountDownLatch(1);val closed=AtomicBoolean()
        val descriptor=LspServerDescriptor("test",listOf("test"),"true","true","server")
        val transport=object: LspTransport {
            override fun read(buffer: ByteArray): Int = -1
            override fun write(bytes: ByteArray) { error("Closed startup must not send initialize") }
            override fun close() { closed.set(true) }
        }
        val session=LspSession(descriptor,"/project") { spawning.countDown();check(finish.await(3,TimeUnit.SECONDS));transport }
        val job=launch(Dispatchers.IO) { session.start("file:///project") }
        assertTrue(withContext(Dispatchers.IO) { spawning.await(3,TimeUnit.SECONDS) })
        session.close();finish.countDown()
        withTimeout(3000) { job.join() }
        assertTrue(closed.get());assertEquals(LspState.DISCONNECTED,session.state.value)
    }
}
