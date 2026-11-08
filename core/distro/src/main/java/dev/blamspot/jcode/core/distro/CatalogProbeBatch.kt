package dev.blamspot.jcode.core.distro

/** Viewing a catalog only probes installed tools. Network update/install never runs implicitly. */
object CatalogProbeBatch {
    data class Probe(val id: String,val verify: String)
    data class Result(val installed: Set<String>,val checked: Set<String>,val timedOut: Set<String>)
    fun script(probes: List<Probe>,seconds: Int=8): String {
        require(seconds in 1..30)
        return buildString {
            append("set +e\n")
            probes.forEach { p ->
                require(p.id.matches(Regex("[A-Za-z0-9_.:+-]{1,128}")))
                append("timeout --foreground --kill-after=1s ${seconds}s /bin/bash --noprofile --norc -c ${shellQuote(p.verify)} >/dev/null 2>&1\n")
                append("s=\$?; printf 'SUI_PROBE\\t%s\\t%s\\n' ${shellQuote(p.id)} \"\$s\"\n")
            }
        }
    }
    fun parse(output: String): Result {
        val yes=mutableSetOf<String>();val checked=mutableSetOf<String>();val timeout=mutableSetOf<String>()
        output.lineSequence().forEach { line ->
            val row=line.split('\t');if(row.size!=3 || row[0]!="SUI_PROBE") return@forEach
            val status=row[2].toIntOrNull() ?: return@forEach
            if(status==124 || status==137) timeout+=row[1] else { checked+=row[1];if(status==0) yes+=row[1] }
        }
        return Result(yes,checked,timeout)
    }
}
