package dev.blamspot.jcode.core.debug
import org.json.JSONObject
import kotlin.test.*
import org.junit.Test
class RunInTerminalTest {
    private fun request()=JSONObject("""{"kind":"integrated","cwd":"/workspace/中文 space","args":["/usr/bin/python3","a; '中文.py"],"env":{"KEEP":"$","REMOVE":null}}""")
    @Test fun preservesArgvUnicodeAndNullEnvironment() { val r=RunInTerminal.parse(request());assertEquals("a; '中文.py",r.args[1]);assertNull(r.env["REMOVE"]);assertEquals("$",r.env["KEEP"]) }
    @Test fun rejectsExternalShellAndTraversal() { for(r in listOf(request().put("kind","external"),request().put("argsCanBeInterpretedByShell",true),request().put("cwd","/workspace/../root"))) assertFails { RunInTerminal.parse(r) } }
    @Test fun rejectsOwnerSpoofingAndInvalidTypes() { for(r in listOf(request().put("env",JSONObject().put("JCODE_EXT_ID","other")),request().put("args",org.json.JSONArray().put(17)),request().put("env",JSONObject().put("N",17)),request().put("env","ignored"),request().put("argsCanBeInterpretedByShell","false"),request().put("unexpected","ignored"))) assertFails { RunInTerminal.parse(r) } }
}
