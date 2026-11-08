package dev.blamspot.jcode.core.debug

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.*
import org.json.JSONObject
import kotlin.test.*

class DapCloseTest {
    private class Peer : DapTransport {
        val commands=CopyOnWriteArrayList<String>()
        val input=LinkedBlockingQueue<ByteArray>()
        @Volatile var closed=false
        var remaining=byteArrayOf()
        fun receive(json: JSONObject) { val b=json.toString().toByteArray();input.put("Content-Length: ${b.size}\r\n\r\n".toByteArray()+b) }
        override fun read(buffer: ByteArray): Int {
            if(remaining.isEmpty()) remaining=input.take()
            if(remaining.isEmpty()) return -1
            val count=minOf(buffer.size,remaining.size);remaining.copyInto(buffer,0,0,count);remaining=remaining.copyOfRange(count,remaining.size);return count
        }
        override fun write(bytes: ByteArray) {
            check(!closed) { "Write after closing adapter" }
            val request=JSONObject(bytes.toString(Charsets.UTF_8).substringAfter("\r\n\r\n"))
            val command=request.getString("command");commands+=command
            if(command=="disconnect") check(request.getJSONObject("arguments").getBoolean("terminateDebuggee"))
            receive(JSONObject().put("seq",100+commands.size).put("type","response").put("request_seq",request.getInt("seq")).put("command",command).put("success",true).put("body",JSONObject()))
            if(command=="launch") receive(JSONObject().put("seq",200).put("type","event").put("event","initialized"))
        }
        override fun close() { closed=true;input.offer(byteArrayOf()) }
    }
    @Test fun disconnectTerminatesDebuggeeBeforeTransportIsClosed()=runBlocking {
        val peer=Peer();val session=DebugSession("java","/project") { peer }
        session.start("adapter","launch",JSONObject())
        withTimeout(3000) { while("configurationDone" !in peer.commands) delay(10) }
        session.close();session.close()
        withTimeout(3000) { while(!peer.closed) delay(10) }
        assertEquals(1,peer.commands.count { it=="disconnect" });assertEquals("disconnect",peer.commands.last())
    }
}
