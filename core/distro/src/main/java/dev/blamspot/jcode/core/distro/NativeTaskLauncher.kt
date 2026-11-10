package dev.blamspot.jcode.core.distro

/** Two phases make fast completion and cancellation during process creation deterministic. */
object NativeTaskLauncher {
    fun script(recipePath: String,owner: String,token: String,readyPath: String): String {
        require(token.matches(Regex("[a-f0-9-]{36}")) && owner=="sui-quickrun:$token/task")
        require(recipePath=="/tmp/sui-native-task-$token.sh" && readyPath=="/tmp/sui-task-launch-$token.sh.ready")
        val child="""
            printf '%s\n' "${'$'}${'$'}" > ${shellQuote(readyPath+".pid")}
            for attempt in {1..750}; do [ ! -r ${shellQuote(readyPath+".go")} ] && [ ! -r ${shellQuote(readyPath+".cancel")} ] || break; sleep .02; done
            [ ! -r ${shellQuote(readyPath+".cancel")} ] || exit 143
            [ -r ${shellQuote(readyPath+".go")} ] || exit 70
            exec /bin/bash ${shellQuote(recipePath)}
        """.trimIndent()
        return """
            #!/bin/bash
            set +e
            trap ':' INT
            for attempt in {1..750}; do [ ! -r ${shellQuote(readyPath)} ] && [ ! -r ${shellQuote(readyPath+".cancel")} ] || break; sleep .02;done
            if [ -r ${shellQuote(readyPath+".cancel")} ]; then result=143
            elif [ -r ${shellQuote(readyPath)} ]; then
                ${GuestProcessTree.OWNER_ENV}=${shellQuote(owner)} /bin/bash --noprofile --norc -c ${shellQuote(child)} <&0 &
                child=${'$'}!
                wait "${'$'}child";result=${'$'}?
                while kill -0 "${'$'}child" 2>/dev/null; do wait "${'$'}child";result=${'$'}?;[ "${'$'}result" -ne 127 ] || break;done
            else result=70; printf '%s\n' 'Task launch acknowledgement missing' >&2
            fi
            rm -f -- ${listOf(readyPath,readyPath+".cancel",readyPath+".go",readyPath+".pid").joinToString(" ",transform=::shellQuote)}
            printf '\033]7713;native-$token;%s\007' "${'$'}result"
            printf '\n[Task exit %s]\n' "${'$'}result"
            exec /bin/bash --noprofile --norc -i
        """.trimIndent()
    }
}
