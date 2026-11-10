package dev.suicode.nativehost

import dev.blamspot.jcode.core.distro.shellQuote
import dev.suicode.packages.KitRequirement
import kotlinx.serialization.json.*

/** Pre-consent inspection must never invoke an extension-named executable or import its module. */
internal object KitProbeScripts {
    fun modulePresence(module: String,additionalPaths: List<String> = emptyList()): String {
        require(module.matches(Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*")))
        val paths=JsonArray(additionalPaths.map(::JsonPrimitive)).toString()
        val script="""
            import sys, sysconfig
            from importlib.machinery import PathFinder
            paths=list(sys.path)+[sysconfig.get_path('purelib'),sysconfig.get_path('platlib'),'/usr/lib/python3/dist-packages']+$paths
            for part in ${JsonPrimitive(module)}.split('.'):
                spec=PathFinder.find_spec(part,[p for p in paths if p])
                if spec is None: raise SystemExit(1)
                paths=list(spec.submodule_search_locations or [])
        """.trimIndent()
        // -I/-S ignore PYTHONPATH, startup site/.pth files and the current directory.
        return "/usr/bin/python3 -I -S -c "+shellQuote(script)
    }
    fun versionMetadata(requirement: KitRequirement): String = """
        set +e
        for package in ${requirement.packages.joinToString(" ",transform=::shellQuote)}; do
            value=${'$'}(dpkg-query -W -f='${'$'}{db:Status-Abbrev} ${'$'}{Version}' "${'$'}package" 2>/dev/null)
            case "${'$'}value" in 'ii '*) printf '%s' "${'$'}{value#ii }"; exit 0;; esac
        done
        for tool in ${(requirement.commands+requirement.anyCommands).distinct().joinToString(" ",transform=::shellQuote)}; do
            path=${'$'}(command -v "${'$'}tool" 2>/dev/null) || continue
            path=${'$'}(readlink -f -- "${'$'}path") || continue
            owner=${'$'}(dpkg-query -S -- "${'$'}path" 2>/dev/null | head -1 | sed 's/: .*//')
            [ -n "${'$'}owner" ] || continue
            value=${'$'}(dpkg-query -W -f='${'$'}{Version}' "${'$'}owner" 2>/dev/null) || continue
            printf '%s' "${'$'}value"; exit 0
        done
        printf '%s' ${shellQuote("Installed checks passed; required/source version: "+requirement.version)}
    """.trimIndent()
}
