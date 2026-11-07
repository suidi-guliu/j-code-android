package dev.blamspot.jcode.core.distro

object CLanguageAdapter : NativeLanguageAdapter {
    override val id = "c"
    override val extensions = setOf("c")
    override fun command(guestFile: String) = NativeQuickRunScript.build(guestFile, "c", "gcc", "SUI_C", "-std=c17")
}
object CppLanguageAdapter : NativeLanguageAdapter {
    override val id = "cpp"
    override val extensions = setOf("cpp", "cc", "cxx", "C")
    override fun command(guestFile: String) = NativeQuickRunScript.build(guestFile, "cpp", "g++", "SUI_CPP", "-std=c++20")
}
object PythonLanguageAdapter : NativeLanguageAdapter {
    override val id = "python"
    override val extensions = setOf("py")
    override fun command(guestFile: String) = NativeQuickRunScript.build(guestFile, "python", "python3", "SUI_PYTHON", "")
}

internal object NativeQuickRunScript {
    fun build(file: String, language: String, tool: String, variable: String, flags: String): String {
        require(file.startsWith('/') && '\u0000' !in file)
        val header = """
            #!/bin/bash
            set -u
            ENV_FILE=/opt/sui-code/$language.env
            if [ -r "${'$'}ENV_FILE" ]; then
                unset $variable
                . "${'$'}ENV_FILE" || { echo '[QuickRun environment error] Cannot load $language environment.' >&2; exit 78; }
                TOOL="${'$'}{$variable:-}"
            else
                TOOL=/usr/bin/$tool
                echo '[QuickRun] Using the existing Ubuntu /usr/bin/$tool; no managed override is configured.'
            fi
            [ -n "${'$'}TOOL" ] && [ -x "${'$'}TOOL" ] || { echo '[QuickRun environment error] Install $language through Toolchains; no automatic installation or fallback.' >&2; exit 78; }
            "${'$'}TOOL" --version || { echo '[QuickRun environment error] The selected tool cannot run.' >&2; exit 78; }
            cd -- ${shellQuote(file.substringBeforeLast('/').ifEmpty { "/" })} || exit 66
            BUILD=${'$'}(mktemp -d /tmp/sui-quickrun-XXXXXX) || exit 73
            trap 'rm -rf -- "${'$'}BUILD"' EXIT
            trap 'exit 130' INT
            trap 'exit 143' TERM
            SOURCE=${shellQuote(file)}
            cp -- "${'$'}SOURCE" "${'$'}BUILD/input" || { echo '[QuickRun file error] Cannot snapshot the saved file.' >&2; exit 66; }
        """.trimIndent() + "\n"
        val execution = if (language == "python") """
            export PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
            if ! "${'$'}TOOL" -c 'import sys,tokenize; compile(tokenize.open(sys.argv[1]).read(), sys.argv[2], "exec")' "${'$'}BUILD/input" "${'$'}SOURCE"; then
                echo '[QuickRun compilation failed] Python syntax error' >&2; exit 65;
            fi
            echo '[QuickRun] Running Python; terminal input is connected'
            "${'$'}TOOL" -u -c 'import sys,tokenize,os; original=sys.argv[2]; source=tokenize.open(sys.argv[1]).read(); sys.argv=[original]; sys.path[0]=os.path.dirname(original); namespace=sys.modules["__main__"].__dict__; namespace.update(__file__=original, __package__=None, __cached__=None, __spec__=None); exec(compile(source, original, "exec"), namespace)' "${'$'}BUILD/input" "${'$'}SOURCE"
        """.trimIndent() else """
            echo '[QuickRun] Compiling $language into a temporary directory'
            if ! "${'$'}TOOL" $flags -Wall -Wextra "-ffile-prefix-map=${'$'}BUILD/input=${'$'}SOURCE" -iquote "${'$'}PWD" -x ${if (language == "c") "c" else "c++"} "${'$'}BUILD/input" -o "${'$'}BUILD/program"; then
                echo '[QuickRun compilation failed]' >&2; exit 65;
            fi
            echo '[QuickRun] Running $language; terminal input is connected'
            "${'$'}BUILD/program"
        """.trimIndent()
        return header + execution + "\n" + """
            RESULT=${'$'}?
            if [ "${'$'}RESULT" -ne 0 ]; then echo "[QuickRun runtime failed] exit ${'$'}RESULT" >&2; exit 70; fi
            echo '[QuickRun completed] exit 0'
        """.trimIndent() + "\n"
    }
}
