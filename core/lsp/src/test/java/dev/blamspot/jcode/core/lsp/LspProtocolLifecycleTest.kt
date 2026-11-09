package dev.blamspot.jcode.core.lsp

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.util.concurrent.LinkedBlockingQueue
import kotlin.test.*

/** Protocol fault tests complement the real Ubuntu server and Android editor acceptance tests. */
class LspProtocolLifecycleTest {
    private class Transport: LspTransport {
        val messages=java.util.concurrent.CopyOnWriteArrayList<JSONObject>()
        val input=LinkedBlockingQueue<ByteArray>()
        private var remaining=byteArrayOf()
        private var position=0
        fun frame(value: JSONObject) {
            val body=value.toString().toByteArray(Charsets.UTF_8)
            val bytes="Content-Length: ${body.size}\r\n\r\n".toByteArray()+body
            // Split UTF-8 and headers across arbitrary pipe reads.
            bytes.toList().chunked(7).forEach { input.put(it.toByteArray()) }
        }
        override fun write(bytes: ByteArray) {
            val text=bytes.toString(Charsets.UTF_8).substringAfter("\r\n\r\n")
            val message=JSONObject(text);messages+=message
            if(message.optString("method")=="initialize") frame(JSONObject().put("jsonrpc","2.0").put("id",message.getInt("id")).put("result",JSONObject().put("capabilities",JSONObject().put("completionProvider",JSONObject()).put("definitionProvider",true))))
            if(message.optString("method")=="textDocument/completion") frame(JSONObject().put("jsonrpc","2.0").put("id",message.getInt("id")).put("result",org.json.JSONArray().put(JSONObject().put("label","println(String value)").put("filterText","println").put("insertTextFormat",2).put("textEdit",JSONObject().put("newText","println(\${1:中文})")))))
        }
        override fun read(buffer: ByteArray): Int {
            if(position>=remaining.size) { remaining=input.take();position=0;if(remaining.isEmpty()) return -1 }
            val n=minOf(buffer.size,remaining.size-position);remaining.copyInto(buffer,0,position,position+n);position+=n;return n
        }
        override fun close() { input.offer(byteArrayOf()) }
    }
    @Test fun eofAfterInitializeCannotRemainFalselyReady()=runBlocking {
        val transport=Transport();val session=LspSession(LspServerDescriptor("test",listOf("test"),"true","true","server"),"/project") { transport }
        try {
            session.start("file:///project");assertEquals(LspState.READY,session.state.value)
            transport.input.put(byteArrayOf())
            withTimeout(3000) { session.state.first { it==LspState.ERROR } }
            assertTrue(session.errorMessage!!.contains("closed its protocol stream"))
        } finally { session.close() }
    }
    @Test fun unicodeFramesCompletionAndContextOptionsUseLiteralProjectPaths()=runBlocking {
        val root="/tmp/中文 space ' : project"
        val transport=Transport();val descriptor=LspServerDescriptor("test",listOf("test"),"true","true","server",initializationOptions="""{"fallbackFlags":["-I${'$'}{workspaceRoot}/include"]}""")
        val session=LspSession(descriptor,root) { transport }
        try {
            session.start(java.io.File(root).toURI().toASCIIString())
            assertEquals(LspState.READY,session.state.value)
            val flags=transport.messages.first().getJSONObject("params").getJSONObject("initializationOptions").getJSONArray("fallbackFlags")
            assertEquals("-I$root/include",flags.getString(0))
            val uri=session.hostToDistroUri("$root/main.cpp")
            session.didOpen(uri,"cpp",1,"中文");session.didChange(uri,2,"中文 changed")
            val item=session.completion(uri,0,2).single()
            assertEquals("println",item.filterText);assertEquals("println(\${1:中文})",item.insertText)
            val methods=transport.messages.map { it.optString("method") }
            assertTrue(methods.indexOf("initialized")<methods.indexOf("textDocument/didOpen"))
            assertTrue(methods.indexOf("textDocument/didChange")<methods.indexOf("textDocument/completion"))
        } finally { session.close() }
    }
}
