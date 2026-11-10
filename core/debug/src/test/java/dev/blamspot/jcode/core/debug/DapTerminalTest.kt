package dev.blamspot.jcode.core.debug

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.*
import org.json.JSONObject
import kotlin.test.*

class DapTerminalTest {
    private class Peer : DapTransport {
        val written=CopyOnWriteArrayList<JSONObject>()
        val input=LinkedBlockingQueue<ByteArray>();var remaining=byteArrayOf()
        fun receive(j: JSONObject) { val bytes=j.toString().toByteArray();input.put("Content-Length: ${bytes.size}\r\n\r\n".toByteArray()+bytes) }
        override fun read(buffer: ByteArray): Int { if(remaining.isEmpty()) remaining=input.take();if(remaining.isEmpty()) return -1;val n=minOf(buffer.size,remaining.size);remaining.copyInto(buffer,0,0,n);remaining=remaining.copyOfRange(n,remaining.size);return n }
        override fun write(bytes: ByteArray) {
            val m=JSONObject(bytes.toString(Charsets.UTF_8).substringAfter("\r\n\r\n"));written+=m
            if(m.optString("command")=="next") receive(JSONObject("""{"seq":901,"type":"event","event":"stopped","body":{"reason":"step","threadId":1}}"""))
            if(m.getString("type")=="request") receive(JSONObject().put("seq",900).put("type","response").put("request_seq",m.getInt("seq")).put("command",m.getString("command")).put("success",true).put("body",JSONObject()))
        }
        override fun close() { input.offer(byteArrayOf()) }
    }
    @Test fun terminalCapabilityAndReverseResponseUseTheProtocolWithoutBlockingItsReader()=runBlocking {
        val peer=Peer();val session=DebugSession("python","/project") { peer }
        val approved=CompletableDeferred<Unit>();val called=CompletableDeferred<Unit>()
        session.onRunInTerminal={ r -> assertEquals(listOf("python3","中文.py"),r.args);called.complete(Unit);approved.await();TerminalProcess(123,122) }
        try {
            session.start("adapter","launch",JSONObject())
            assertTrue(peer.written.first().getJSONObject("arguments").getBoolean("supportsRunInTerminalRequest"))
            peer.receive(JSONObject("""{"seq":42,"type":"request","command":"runInTerminal","arguments":{"cwd":"/project","args":["python3","中文.py"]}}"""))
            withTimeout(2000) { called.await() }
            // While the user reviews the command, ordinary DAP requests must still receive replies.
            withTimeout(2000) { session.evaluate("value",null) }
            approved.complete(Unit)
            withTimeout(2000) { while(peer.written.none { it.optInt("request_seq")==42 }) delay(10) }
            val response=peer.written.single { it.optInt("request_seq")==42 }
            assertTrue(response.getBoolean("success"));assertEquals(123,response.getJSONObject("body").getInt("processId"))
        } finally { session.close() }
    }
    @Test fun rejectsUnsupportedParametersAndKeepsExitedCodeDistinctFromTerminated()=runBlocking {
        val peer=Peer();val session=DebugSession("lldb","/project") { peer }
        var exit: Int?=null;var terminated=false
        session.onRunInTerminal={ error("Must not launch an external terminal") };session.onExit={exit=it};session.onTerminated={terminated=true}
        try {
            session.start("adapter","launch",JSONObject())
            peer.receive(JSONObject("""{"seq":43,"type":"request","command":"runInTerminal","arguments":{"kind":"external","cwd":"/project","args":["unsafe"]}}"""))
            withTimeout(2000) { while(peer.written.none { it.optInt("request_seq")==43 }) delay(10) }
            assertFalse(peer.written.single { it.optInt("request_seq")==43 }.getBoolean("success"))
            peer.receive(JSONObject("""{"seq":44,"type":"event","event":"exited","body":{"exitCode":7}}"""))
            withTimeout(2000) { while(exit==null) delay(10) };assertEquals(7,exit);assertFalse(terminated)
            peer.receive(JSONObject("""{"seq":45,"type":"event","event":"terminated"}"""))
            withTimeout(2000) { while(!terminated) delay(10) }
        } finally { session.close() }
    }
    @Test fun fastStoppedEventBeforeStepResponseIsRetained()=runBlocking {
        val peer=Peer();val session=DebugSession("lldb","/project") { peer }
        try {
            session.start("adapter","launch",JSONObject())
            session.next(1)
            assertEquals(DebugState.STOPPED,session.state.value)
            assertEquals("step",session.stopped.value?.reason)
        } finally { session.close() }
    }

}
