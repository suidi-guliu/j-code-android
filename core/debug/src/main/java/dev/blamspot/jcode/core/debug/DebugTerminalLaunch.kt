package dev.blamspot.jcode.core.debug

import dev.blamspot.jcode.core.distro.GuestProcessTree
import dev.blamspot.jcode.core.distro.shellQuote

/** Pure scripts reused by Android and the real owned Ubuntu integration runner. No execution API. */
data class DebugTerminalPaths(val token: String) {
    init { require(token.matches(Regex("[a-f0-9-]{36}"))) }
    val directory="/tmp/sui-debug-terminal-$token"
    val script="$directory/launch.sh"
    val ready="$directory/ready"
    val cancel="$directory/cancel"
    val go="$directory/go"
    val pid="$directory/pid"
    val owner="sui-quickrun:debug-$token/debuggee"
}
object DebugTerminalLaunch {
    fun script(request: RunInTerminal,paths: DebugTerminalPaths): String {
        val env=request.env.filterValues { it==null }.keys.flatMap { listOf("-u",it) }+listOf("--")+request.env.filterValues { it!=null }.map { "${it.key}=${it.value}" }
        val command=(listOf("/usr/bin/env","--default-signal=INT,QUIT")+env+request.args).joinToString(" ",transform=::shellQuote)
        val child="""
            printf '%s %s\n' "${'$'}${'$'}" "${'$'}0" > ${shellQuote(paths.pid)}
            for attempt in {1..750}; do [ ! -r ${shellQuote(paths.go)} ] && [ ! -r ${shellQuote(paths.cancel)} ] || break; sleep .02; done
            [ ! -r ${shellQuote(paths.cancel)} ] || exit 143
            [ -r ${shellQuote(paths.go)} ] || exit 70
            exec $command
        """.trimIndent()
        return """
            #!/bin/bash
            set +e
            trap ':' INT
            for attempt in {1..750}; do [ ! -r ${shellQuote(paths.ready)} ] && [ ! -r ${shellQuote(paths.cancel)} ] || break; sleep .02; done
            if [ -r ${shellQuote(paths.cancel)} ]; then result=143
            elif [ -r ${shellQuote(paths.ready)} ]; then
                ${GuestProcessTree.OWNER_ENV}=${shellQuote(paths.owner)} /bin/bash --noprofile --norc -c ${shellQuote(child)} "${'$'}${'$'}" <&0 &
                child=${'$'}!
                wait "${'$'}child";result=${'$'}?
                while kill -0 "${'$'}child" 2>/dev/null; do wait "${'$'}child";result=${'$'}?;[ "${'$'}result" -ne 127 ] || break;done
            else result=70
            fi
            printf '\033]7713;debug-${paths.token};%s\007' "${'$'}result"
            printf '\n[Terminal command exit %s]\n' "${'$'}result"
            rm -f -- ${listOf(paths.ready,paths.cancel,paths.go,paths.pid,paths.script).joinToString(" ",transform=::shellQuote)}
            rmdir -- ${shellQuote(paths.directory)} 2>/dev/null
            exec /bin/bash --noprofile --norc -i
        """.trimIndent()
    }
}
