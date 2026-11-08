package dev.suicode.packages

import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Test
import kotlin.test.*

class DebugWorkflowTest {
    private fun workflow(name: String): JsonObject = Json.parseToJsonElement(
        Json.parseToJsonElement(File("../../native-library/src/$name/suik/adapters/dap.json").readText()).jsonObject.getValue("workflow").jsonPrimitive.content
    ).jsonObject
    private val context=mapOf("source" to JsonPrimitive("/project/中文 space/Hello.java"),"project" to JsonPrimitive("/project"),"projectUri" to JsonPrimitive("file:/project/"))
    @Test fun realJavaKitWorkflowResolvesMainBuildPathsAndPort() = runBlocking {
        val commands=mutableListOf<String>()
        val result=DebugWorkflow.resolve(workflow("Java"),context) { command,args ->
            commands+=command
            when(command) {
                "vscode.java.resolveMainClass" -> buildJsonArray { add(buildJsonObject { put("filePath",context.getValue("source"));put("mainClass","app.Hello");put("projectName","demo") }) }
                "vscode.java.buildWorkspace" -> { assertEquals("demo",Json.parseToJsonElement(args[0].jsonPrimitive.content).jsonObject["projectName"]?.jsonPrimitive?.content);JsonPrimitive(1) }
                "vscode.java.resolveClasspath" -> Json.parseToJsonElement("[[],[\"/project/target/classes\",\"/lib/dependency.jar\"]]")
                "vscode.java.startDebugSession" -> JsonPrimitive(43210)
                else -> error(command)
            }
        }
        assertEquals(4,commands.size);assertEquals(43210,result.port)
        assertEquals("app.Hello",result.launch["mainClass"]?.jsonPrimitive?.content)
        assertEquals(2,result.launch.getValue("classPaths").jsonArray.size)
    }
    @Test fun buildFailurePreventsLaunchingAdapter() = runBlocking {
        val failure=assertFailsWith<PackageFailure> { DebugWorkflow.resolve(workflow("Java"),context) { command,_ ->
            when(command) {
                "vscode.java.resolveMainClass" -> buildJsonArray { add(buildJsonObject { put("filePath",context.getValue("source"));put("mainClass","Hello");put("projectName","demo") }) }
                "vscode.java.buildWorkspace" -> JsonPrimitive(0)
                else -> error("No later operation may run")
            }
        } }
        assertEquals("E_BUILD",failure.code)
    }
    @Test fun pythonLaunchPreservesSourceAsDataWithoutRpc()=runBlocking {
        val value="/project/中文 ' space; touch injected.py"
        val result=DebugWorkflow.resolve(workflow("Python"),context+("source" to JsonPrimitive(value))) { _,_->error("No RPC required") }
        assertEquals(value,result.launch.getValue("program").jsonPrimitive.content);assertNull(result.port)
    }
    @Test fun sourceSelectionRejectsWrongMainClass()=runBlocking {
        assertEquals("E_WORKFLOW",assertFailsWith<PackageFailure> { DebugWorkflow.resolve(workflow("Java"),context) { _,_->JsonArray(emptyList()) } }.code)
    }
}
