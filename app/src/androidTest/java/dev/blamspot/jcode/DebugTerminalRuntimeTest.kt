package dev.blamspot.jcode

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.UiController
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.blamspot.jcode.core.debug.*
import dev.blamspot.jcode.core.distro.TerminalIdleProbe
import dev.blamspot.jcode.core.term.*
import dev.blamspot.jcode.debug.VariableRow
import dev.blamspot.jcode.workbench.*
import dev.suicode.nativehost.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Real Android native PTY/TerminalView + production DAP/UI, real runner-host debugpy.
 * This deliberately does not claim the runner's debuggee is Android/ARM64 Ubuntu. */
@RunWith(AndroidJUnit4::class)
class DebugTerminalRuntimeTest {
    @get:Rule val compose=createComposeRule()
    private val instrument get()=InstrumentationRegistry.getInstrumentation()
    private fun rpc(port: Int,value: JSONObject): JSONObject = Socket("127.0.0.1",port).use { socket ->
        socket.soTimeout=15000
        socket.getOutputStream().write((value.toString()+"\n").toByteArray())
        JSONObject(socket.getInputStream().bufferedReader().readLine()).also { check(!it.has("error")) { it.toString() } }
    }
    private fun waitFor(message: String,condition: ()->Boolean) {
        val end=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(40)
        while(!condition()) { assertTrue(message,System.nanoTime()<end);Thread.sleep(30) }
    }
    @Test fun liveDebugUiUnicodeInputHideRenameStepAndStopKeepTheOwnedPty() {
        val encoded=InstrumentationRegistry.getArguments().getString("suiDebugFixture")
        Assume.assumeTrue("CI must supply the actual debugpy/PTY fixture",encoded!=null)
        assertEquals("1",Runtime.getRuntime().exec(arrayOf("getprop","ro.kernel.qemu")).inputStream.bufferedReader().readText().trim())
        val cfg=JSONObject(String(android.util.Base64.decode(encoded,android.util.Base64.NO_WRAP)))
        val control=cfg.getInt("controlPort");val ttyPort=cfg.getInt("ttyPort");val project=cfg.getString("project");val source=cfg.getString("source")
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        val terminals=CopyOnWriteArrayList<TerminalSessionManager.Session>()
        val transcript=ByteArrayOutputStream();val sent=ByteArrayOutputStream();val readers=CopyOnWriteArrayList<Job>()
        val reading=AtomicBoolean(true);var shown by mutableStateOf(true);var roster by mutableIntStateOf(0)
        val host=NativePackageHost.get(instrument.targetContext);runBlocking { host.restore() };host.selectUiLanguage(null)
        val session=DebugSession("python",project) {
            val socket=Socket("127.0.0.1",cfg.getInt("dapPort"))
            object: DapTransport {
                override fun read(buffer: ByteArray): Int=try { socket.getInputStream().read(buffer) } catch(e: Exception) { -1 }
                override fun write(bytes: ByteArray) { synchronized(sent) { sent.write(bytes) };socket.getOutputStream().write(bytes);socket.getOutputStream().flush() }
                override fun close() { socket.close() }
            }
        }
        session.onRunInTerminal={ request ->
            withContext(Dispatchers.IO) {
                val result=rpc(control,JSONObject().put("operation","launch").put("arguments",JSONObject().put("cwd",request.cwd).put("kind","integrated").put("args",org.json.JSONArray(request.args)).put("env",JSONObject(request.env))))
                val pty=PtyProcess.create("/system/bin/sh",listOf("sh","-c","exec /system/bin/toybox nc 127.0.0.1 $ttyPort"),listOf("PATH=/system/bin","TERM=xterm-256color"),instrument.targetContext.cacheDir.path)
                val terminal=TerminalSessionManager.Session("owned-live-debug","Python Debug",pty).apply { taskState="RUNNING";managedLabel=true }
                terminals.add(terminal)
                readers.add(scope.launch {
                    val bytes=ByteArray(32768)
                    while(reading.get()) {
                        if(!pty.awaitReadable(100)) continue
                        val count=pty.read(bytes);if(count<0) break;if(count==0) continue
                        synchronized(transcript) { transcript.write(bytes,0,count) }
                        synchronized(terminal) { terminal.parser.feed(bytes,count);terminal.inputModesSnapshot=terminal.parser.inputModes() }
                        terminal.onUpdate?.invoke()
                    }
                })
                withContext(Dispatchers.Main) { roster++ }
                TerminalProcess(result.getInt("processId"),result.getInt("shellProcessId"))
            }
        }
        fun text()=synchronized(transcript) { transcript.toString("UTF-8") }
        try {
            compose.setContent {
                val state by session.state.collectAsState()
                var frames by remember { mutableStateOf(emptyList<DapStackFrame>()) }
                var variables by remember { mutableStateOf(emptyList<VariableRow>()) }
                LaunchedEffect(state) {
                    if(state==DebugState.STOPPED) {
                        val thread=session.stopped.value?.threadId ?: return@LaunchedEffect
                        frames=session.stackTrace(thread)
                        variables=frames.firstOrNull()?.let { frame -> session.scopes(frame.id).filter { !it.expensive }.flatMap { sc -> session.variables(sc.variablesReference).map { VariableRow(it.name,it.value,it.type,1) } } }.orEmpty()
                    }
                }
                val ui=DebugSessionUi(state=state,callStack=frames,variables=variables,
                    onContinue={scope.launch { session.stopped.value?.threadId?.let { session.continueThread(it) } }},
                    onStepOver={scope.launch { session.stopped.value?.threadId?.let { session.next(it) } }},
                    onStop={scope.launch { session.close() }})
                MaterialTheme { NativeLocalizationProvider(host) {
                    CompositionLocalProvider(LocalDebugSession provides ui) {
                        roster
                        if(shown) TerminalSidebarContent(null,terminals.map { it.id },terminals.firstOrNull()?.id.orEmpty(),terminals.firstOrNull()?.pty,{ id -> terminals.firstOrNull { it.id==id } },{ id -> terminals.firstOrNull { it.id==id }?.label },true,emptyList(),{_,_->},{},{},{},{},Modifier.fillMaxSize())
                        else Text("Owned test: terminal view hidden; program and reader remain live")
                    }
                } }
            }
            val started=scope.launch { session.start("","launch",JSONObject().put("type","python").put("request","launch").put("program",source).put("cwd",project).put("console","integratedTerminal").put("redirectOutput",false),mapOf(source to listOf(cfg.getInt("before"),cfg.getInt("after")))) }
            waitFor("Input-before breakpoint not reached") { session.state.value==DebugState.STOPPED }
            assertTrue(terminals.single().pty.childPid>0)
            assertTrue(File("/proc/${terminals.single().pty.childPid}/stat").isFile)
            assertFalse("A running nc transport cannot be reaped as an idle shell",TerminalIdleProbe.idle(terminals.single().pty.childPid))
            compose.onNodeWithContentDescription("Continue").performClick()
            waitFor("No real input prompt") { text().contains("INPUT:") }
            compose.runOnIdle { shown=false }
            Thread.sleep(300)
            assertTrue(rpc(control,JSONObject().put("operation","status")).getBoolean("alive"))
            compose.runOnIdle { shown=true }
            compose.onNodeWithText("Python Debug",substring=true).performTouchInput { longClick() }
            compose.onNodeWithText("Rename terminal").performClick()
            compose.onNode(hasSetTextAction()).performTextReplacement("用户调试终端")
            compose.onNodeWithText("Save").performClick()
            compose.onNodeWithText("用户调试终端",substring=true).assertExists()
            input("用户输入张三")
            waitFor("Post-input breakpoint not reached") { session.state.value==DebugState.STOPPED && runBlocking { session.stackTrace(session.stopped.value!!.threadId) }.firstOrNull()?.line==cfg.getInt("after") }
            compose.onNodeWithContentDescription("Debug variables and stack").performClick()
            compose.onNodeWithContentDescription("Continue").assertIsEnabled()
            val frame=runBlocking { session.stackTrace(session.stopped.value!!.threadId) }.first()
            assertEquals(cfg.getInt("after"),frame.line)
            val values=runBlocking { session.scopes(frame.id).filter { !it.expensive }.flatMap { session.variables(it.variablesReference) } }
            assertTrue(values.toString(),values.any { it.value.contains("用户输入张三") })
            compose.onNodeWithContentDescription("Step over").performClick()
            waitFor("Single step failed") { session.state.value==DebugState.STOPPED && runBlocking { session.stackTrace(session.stopped.value!!.threadId) }.firstOrNull()?.line!=cfg.getInt("after") }
            input("暂停时输入中文")
            compose.onNodeWithContentDescription("Continue").performClick()
            waitFor("Queued terminal input was lost") { text().contains("SECOND:暂停时输入中文") && text().contains("RUNNING:") }
            compose.onNodeWithContentDescription("Stop").performClick()
            waitFor("Stop failed to disconnect") { session.state.value==DebugState.DISCONNECTED }
            waitFor("Actual debuggee process leaked") { !rpc(control,JSONObject().put("operation","status")).getBoolean("alive") }
            assertTrue(rpc(control,JSONObject().put("operation","status")).getBoolean("sentinelAlive"))
            assertTrue("Hiding or Stop closed the user's terminal",terminals.single().pty.isOpen)
            assertTrue(text().contains("RESULT:你好 用户输入张三"))
            assertFalse("stdin leaked into DAP JSON",synchronized(sent) { sent.toString("UTF-8") }.contains("用户输入张三"))
            started.joinForTest()
            android.util.Log.i("SuiDebugUiTest","PASS real host debugpy + Android16 native PTY/production UI: Unicode input, before/after breakpoint, variables, step, hide/rename, Stop, transcript and unrelated process retention. Not Android ARM64 Ubuntu.")
        } finally {
            session.close();reading.set(false);runBlocking { readers.forEach { it.cancelAndJoin() } };scope.cancel()
            terminals.forEach { it.pty.close();it.parser.close() }
        }
    }
    private fun Job.joinForTest()=runBlocking { withTimeout(5000) { join() } }
    private fun input(value: String) {
        onView(isAssignableFrom(TerminalView::class.java)).perform(object: ViewAction {
            override fun getConstraints()=isAssignableFrom(TerminalView::class.java)
            override fun getDescription()="Send actual Unicode IME text and physical Enter into native PTY"
            override fun perform(controller: UiController,view: android.view.View) {
                val terminal=view as TerminalView;terminal.requestFocus()
                check(terminal.onCreateInputConnection(EditorInfo()).commitText(value,1))
                terminal.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER))
                terminal.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_ENTER));controller.loopMainThreadUntilIdle()
            }
        })
    }
}
