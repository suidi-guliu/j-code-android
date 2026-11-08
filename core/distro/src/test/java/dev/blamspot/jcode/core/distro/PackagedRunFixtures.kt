package dev.blamspot.jcode.core.distro

import java.io.File

/** Test-only facades exercise the exact reviewed plugin resources, never ship in Core. */
interface NativeLanguageAdapter { val id: String; val extensions: Set<String>; fun command(guestFile: String): String }
private fun packaged(file: String,lang: String,script: String): String {
    require(file.startsWith('/') && '\u0000' !in file)
    return "set -- ${shellQuote(file)}\n"+File("../../native-library/src/$lang/suic/resources/$script").readText()
}
object JavaLanguageAdapter: NativeLanguageAdapter {
    override val id="java";override val extensions=setOf("java")
    override fun command(guestFile: String)=packaged(guestFile,"Java","run-java.sh")
}
object CLanguageAdapter: NativeLanguageAdapter {
    override val id="c";override val extensions=setOf("c")
    override fun command(guestFile: String)=packaged(guestFile,"C_CPP","run-c.sh")
}
object CppLanguageAdapter: NativeLanguageAdapter {
    override val id="cpp";override val extensions=setOf("cpp","cc","cxx","C")
    override fun command(guestFile: String)=packaged(guestFile,"C_CPP","run-cpp.sh")
}
object PythonLanguageAdapter: NativeLanguageAdapter {
    override val id="python";override val extensions=setOf("py")
    override fun command(guestFile: String)=packaged(guestFile,"Python","run-python.sh")
}
object QuickRunLanguages {
    fun forFile(name: String): NativeLanguageAdapter?=listOf(JavaLanguageAdapter,CLanguageAdapter,CppLanguageAdapter,PythonLanguageAdapter).firstOrNull { name.substringAfterLast('.') in it.extensions }
}
