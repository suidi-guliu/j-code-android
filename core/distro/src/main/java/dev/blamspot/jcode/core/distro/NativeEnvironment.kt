package dev.blamspot.jcode.core.distro

/** Fixed Ubuntu packages, never marketplace extensions or arbitrary shell entered by a user. */
enum class NativeEnvironment(val key: String, val title: String, val packages: List<String>, val tool: String, val variable: String) {
    C("c", "C (GCC)", listOf("gcc", "libc6-dev"), "gcc", "SUI_C"),
    Cpp("cpp", "C/C++ (GCC, G++, GDB, CMake, Ninja)", listOf("gcc", "g++", "libc6-dev", "gdb", "cmake", "ninja-build"), "g++", "SUI_CPP"),
    Python("python", "Python 3", listOf("python3", "python3-venv", "python3-pip"), "python3", "SUI_PYTHON");

    val catalogId get() = "sui-$key"
    fun queryScript(): String = """
        set -eu
        . /etc/os-release
        [ "${'$'}ID" = ubuntu ] || { echo 'Only the Ubuntu distribution package channel is supported.' >&2; exit 1; }
        export LC_ALL=C
        sudo -n apt-get -o Acquire::AllowInsecureRepositories=false -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 update >&2
        for p in ${packages.joinToString(" ")}; do
            installed=${'$'}(dpkg-query -W -f='${'$'}{db:Status-Abbrev}${'$'}{Version}' "${'$'}p" 2>/dev/null || true)
            case "${'$'}installed" in 'ii '*|'hi '*) installed=${'$'}{installed#* } ;; *) installed=- ;; esac
            candidate=${'$'}(apt-cache policy "${'$'}p" | sed -n 's/^[[:space:]]*Candidate: //p' | head -1)
            printf 'PKG\t%s\t%s\t%s\n' "${'$'}p" "${'$'}installed" "${'$'}candidate"
        done
        apt-cache policy ${packages.joinToString(" ")} >&2
        dpkg --print-architecture
    """.trimIndent()

    fun smoke(toolPath: String): String = when (this) {
        C, Cpp -> """
            printf '%s\n' '#include <stdio.h>' 'int main(void) { puts("SUI_NATIVE_READY"); return 0; }' > "${'$'}TMP/main.${if (this == C) "c" else "cpp"}"
            ${shellQuote(toolPath)} "${'$'}TMP/main.${if (this == C) "c" else "cpp"}" -o "${'$'}TMP/program"
            [ "${'$'}("${'$'}TMP/program")" = SUI_NATIVE_READY ]
        """.trimIndent()
        Python -> "${shellQuote(toolPath)} -c 'import sys; assert sys.version_info.major == 3; print(\"SUI_NATIVE_READY\")'\n${shellQuote(toolPath)} -m venv \"\$TMP/venv\""
    }
}

data class NativePackage(val name: String, val installed: String?, val candidate: String) {
    init {
        require(name.matches(Regex("[a-z0-9][a-z0-9+.-]*")))
        require(candidate.matches(Regex("[A-Za-z0-9:.+~_-]+")))
        require(installed == null || installed.matches(Regex("[A-Za-z0-9:.+~_-]+")))
    }
    val selected get() = installed ?: candidate
}

data class NativeInstallPlan(val language: NativeEnvironment, val packages: List<NativePackage>, val distroId: String, val architecture: String, val upgrade: Boolean = false) {
    init {
        require(architecture in setOf("arm64", "amd64"))
        require(packages.map { it.name } == language.packages)
    }
    val specifications get() = packages.joinToString(" ") { shellQuote("${it.name}=${if(upgrade) it.candidate else it.selected}") }

    fun script(cancelFile: String): String {
        require(cancelFile.matches(Regex("/tmp/sui-env-cancel-[a-zA-Z0-9-]+")))
        val lang = language
        val completeToolchain = if(lang == NativeEnvironment.Cpp) "test -x /usr/bin/gcc && test -x /usr/bin/gdb && test -x /usr/bin/cmake && test -x /usr/bin/ninja" else "true"
        val existingSmoke = lang.smoke("/usr/bin/${lang.tool}").replace(shellQuote("/usr/bin/${lang.tool}"), "\"\$SELECTED\"")
        return """
            set -eu
            umask 022
            ENV_FILE=/opt/sui-code/${lang.key}.env
            CANCEL=${shellQuote(cancelFile)}
            check_cancel() { [ ! -f "${'$'}CANCEL" ] || { echo 'Cancelled at a safe boundary. Package operations already completed are retained; previous environment selection is unchanged.'; exit 130; }; }
            mkdir -p /opt/sui-code
            TMP=${'$'}(mktemp -d /opt/sui-code/.${lang.key}-install-XXXXXX)
            trap 'rm -rf -- "${'$'}TMP"' EXIT
            check_cancel
            ${if(lang==NativeEnvironment.Cpp) """
            ensure_c_environment() {
                if [ ! -e /opt/sui-code/c.env ]; then
                    /usr/bin/gcc --version
                    printf '%s\n' 'export SUI_C=/usr/bin/gcc' > "${'$'}TMP/c.env"
                    chmod 644 "${'$'}TMP/c.env"
                    ln "${'$'}TMP/c.env" /opt/sui-code/c.env || test -e /opt/sui-code/c.env
                fi
            }
            """.trimIndent() else "ensure_c_environment() { :; }"}
            PRESERVED_TOOL=''
            if [ -r "${'$'}ENV_FILE" ]; then
                . "${'$'}ENV_FILE"
                SELECTED="${'$'}{${lang.variable}:-}"
                if [ -n "${'$'}SELECTED" ] && [ -x "${'$'}SELECTED" ] && "${'$'}SELECTED" --version; then
                    $existingSmoke
                    check_cancel
                    if ${if(upgrade) "false" else "true"} && $completeToolchain; then
                        ensure_c_environment
                        echo 'Existing managed ${lang.title} is usable and preserved. No packages or environment files changed.'
                        exit 0
                    fi
                    PRESERVED_TOOL="${'$'}SELECTED"
                fi
            fi
            echo '1/4 Repairing only pending package configuration, if any'
            sudo -n dpkg --configure -a
            check_cancel
            echo '2/4 Downloading reviewed Ubuntu packages; apt verifies signed metadata and package hashes'
            sudo -n apt-get -y -o APT::Get::AllowUnauthenticated=false -o Acquire::AllowInsecureRepositories=false -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 --download-only ${if(upgrade) "" else "--no-upgrade"} install $specifications
            check_cancel
            echo '3/4 Installing downloaded packages; cancellation waits for dpkg to finish safely'
            sudo -n apt-get -y -o APT::Get::AllowUnauthenticated=false --no-download ${if(upgrade) "" else "--no-upgrade"} install $specifications
            check_cancel
            echo '4/4 Verifying tool and compiling/running a smoke program before activation'
            /usr/bin/${lang.tool} --version
            ${lang.smoke("/usr/bin/${lang.tool}")}
            ${if(lang==NativeEnvironment.Cpp) "/usr/bin/gcc --version; /usr/bin/gdb --version; /usr/bin/cmake --version; /usr/bin/ninja --version" else "true"}
            check_cancel
            ensure_c_environment
            if [ -n "${'$'}PRESERVED_TOOL" ]; then
                echo 'Existing compiler selection retained; missing toolchain components were added.'
                exit 0
            fi
            printf '%s\n' 'export ${lang.variable}=/usr/bin/${lang.tool}' > "${'$'}TMP/environment"
            chmod 644 "${'$'}TMP/environment"
            mv -f "${'$'}TMP/environment" "${'$'}ENV_FILE"
            echo 'SUI_NATIVE_READY — ${lang.title}. Existing JDKs and system defaults were not replaced.'
        """.trimIndent()
    }

    companion object {
        fun parse(language: NativeEnvironment, distro: String, output: String): NativeInstallPlan {
            val lines = output.lines()
            val pkgs = lines.filter { it.startsWith("PKG\t") }.map { line ->
                val values = line.split('\t')
                require(values.size == 4) { "Invalid Ubuntu package metadata" }
                NativePackage(values[1], values[2].takeUnless { it == "-" }, values[3])
            }
            return NativeInstallPlan(language, pkgs, distro, lines.lastOrNull { it in setOf("arm64", "amd64") } ?: error("Ubuntu architecture was not reported"))
        }
    }
}
