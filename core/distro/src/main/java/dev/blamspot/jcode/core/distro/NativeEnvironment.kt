package dev.blamspot.jcode.core.distro

/** Fixed Ubuntu packages, never marketplace extensions or arbitrary shell entered by a user. */
enum class NativeEnvironment(val key: String, val title: String, val packages: List<String>, val tool: String, val variable: String) {
    C("c", "C (GCC)", listOf("gcc", "libc6-dev"), "gcc", "SUI_C"),
    Cpp("cpp", "C/C++ (GCC, G++)", listOf("gcc", "g++", "libc6-dev"), "g++", "SUI_CPP"),
    Python("python", "Python 3", listOf("python3"), "python3", "SUI_PYTHON");

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
        Python -> "${shellQuote(toolPath)} -c 'import sys; assert sys.version_info.major == 3; print(\"SUI_NATIVE_READY\")'"
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
        val existingSmoke = lang.smoke("/usr/bin/${lang.tool}").replace(shellQuote("/usr/bin/${lang.tool}"), "\"\$SELECTED\"")
        return """
            set -eu
            umask 022
            ENV_FILE=/opt/sui-code/${lang.key}.env
            CANCEL=${shellQuote(cancelFile)}
            check_cancel() { [ ! -f "${'$'}CANCEL" ] || { echo 'Cancelled at a safe boundary; existing selections are retained.'; exit 130; }; }
            mkdir -p /opt/sui-code
            TMP=${'$'}(mktemp -d /opt/sui-code/.${lang.key}-install-XXXXXX)
            ENV_STAGE=''
            cleanup() {
                status=${'$'}?
                if [ "${'$'}status" -ne 0 ] && [ -f "${'$'}TMP/committed" ]; then
                    while IFS=' ' read -r target backup; do
                        if [ "${'$'}backup" = missing ]; then rm -f -- "${'$'}target";
                        else cp -P -- "${'$'}backup" "${'$'}target.rollback-${'$'}${'$'}" && mv -fT -- "${'$'}target.rollback-${'$'}${'$'}" "${'$'}target"; fi
                    done < "${'$'}TMP/committed"
                    echo '[Environment rollback] Previous configuration restored; verified installed packages retained.' >&2
                fi
                [ -z "${'$'}ENV_STAGE" ] || rm -f -- "${'$'}ENV_STAGE"
                rm -rf -- "${'$'}TMP"
                exit "${'$'}status"
            }
            trap cleanup EXIT
            configuration_kind() {
                if [ -L "${'$'}1" ]; then if [ -f "${'$'}1" ] && [ -r "${'$'}1" ]; then echo readable-symlink; else echo broken-or-unreadable-symlink; fi
                elif [ -d "${'$'}1" ]; then echo directory;
                elif [ -f "${'$'}1" ]; then if [ -r "${'$'}1" ]; then echo readable-file; else echo unreadable-file; fi
                elif [ -e "${'$'}1" ]; then echo invalid-file-type; else echo missing; fi
            }
            validate_target() {
                case "${'$'}(configuration_kind "${'$'}1")" in
                    directory|invalid-file-type) echo "[E_ENV_CONFIG_TYPE] Refusing to replace configuration directory or special file: ${'$'}1" >&2; return 73 ;;
                    unreadable-file) echo "[E_ENV_CONFIG_PERMISSION] Cannot safely back up unreadable configuration: ${'$'}1; restore read permission and retry." >&2; return 77 ;;
                esac
            }
            commit_environment() {
                target=${'$'}1; content=${'$'}2
                validate_target "${'$'}target"
                check_cancel
                ENV_STAGE=${'$'}(mktemp "${'$'}target.new-XXXXXX")
                printf '%s\n' "${'$'}content" > "${'$'}ENV_STAGE"
                chmod 644 "${'$'}ENV_STAGE"
                backup=missing
                if [ -e "${'$'}target" ] || [ -L "${'$'}target" ]; then
                    mkdir -p /opt/sui-code/.environment-backups
                    backup="/opt/sui-code/.environment-backups/${'$'}(basename "${'$'}target").${'$'}(basename "${'$'}TMP")"
                    cp -P -- "${'$'}target" "${'$'}backup"
                    echo "[Environment backup] ${'$'}backup"
                fi
                # Same-parent rename replaces the symlink itself. Never hard-link a temporary file:
                # PRoot --link2symlink would turn that link into a dangling selection on cleanup.
                mv -fT -- "${'$'}ENV_STAGE" "${'$'}target"
                ENV_STAGE=''
                printf '%s %s\n' "${'$'}target" "${'$'}backup" >> "${'$'}TMP/committed"
                test -f "${'$'}target" && test -r "${'$'}target"
            }
            validate_target "${'$'}ENV_FILE"
            echo "[Environment configuration] ${'$'}ENV_FILE: ${'$'}(configuration_kind "${'$'}ENV_FILE")"
            check_cancel
            ${if(lang==NativeEnvironment.Cpp) """
            validate_target /opt/sui-code/c.env
            ensure_c_environment() {
                c_tool=''
                if [ -f /opt/sui-code/c.env ] && [ -r /opt/sui-code/c.env ]; then
                    c_tool=${'$'}( . /opt/sui-code/c.env && printf '%s' "${'$'}{SUI_C:-}" ) || c_tool=''
                fi
                if [ -n "${'$'}c_tool" ] && [ -x "${'$'}c_tool" ] && "${'$'}c_tool" --version; then
                    SELECTED_C="${'$'}c_tool"
                else SELECTED_C=/usr/bin/gcc; fi
                if ! ( ${NativeEnvironment.C.smoke("/usr/bin/gcc").replace(shellQuote("/usr/bin/gcc"), "\"${'$'}SELECTED_C\"")} ); then return 1; fi
                if [ "${'$'}{1:-commit}" = commit ] && [ "${'$'}SELECTED_C" != "${'$'}c_tool" ]; then commit_environment /opt/sui-code/c.env 'export SUI_C=/usr/bin/gcc'; fi
                return 0
            }
            """.trimIndent() else "ensure_c_environment() { :; }"}
            PRESERVED_TOOL=''
            if [ -f "${'$'}ENV_FILE" ] && [ -r "${'$'}ENV_FILE" ]; then
                PRESERVED_TOOL=${'$'}( . "${'$'}ENV_FILE" && printf '%s' "${'$'}{${lang.variable}:-}" ) || PRESERVED_TOOL=''
            fi
            SELECTED="${'$'}PRESERVED_TOOL"
            usable_tool() { [ -x "${'$'}SELECTED" ] && "${'$'}SELECTED" --version && ( $existingSmoke ); }
            if ! usable_tool; then PRESERVED_TOOL=''; SELECTED=/usr/bin/${lang.tool}; fi
            if usable_tool && ensure_c_environment check && ${if(upgrade) "false" else "true"}; then
                ensure_c_environment commit
                if [ -z "${'$'}PRESERVED_TOOL" ]; then commit_environment "${'$'}ENV_FILE" 'export ${lang.variable}=/usr/bin/${lang.tool}'; fi
                echo 'SUI_NATIVE_READY — existing ${lang.title} verified; no packages downloaded; valid custom selections preserved.'
                exit 0
            fi
            echo '1/4 Repairing pending package configuration'
            sudo -n dpkg --configure -a
            check_cancel
            echo '2/4 Downloading reviewed Ubuntu packages using authenticated metadata'
            sudo -n apt-get -y -o APT::Get::AllowUnauthenticated=false -o Acquire::AllowInsecureRepositories=false -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 --download-only ${if(upgrade) "" else "--no-upgrade"} install $specifications
            check_cancel
            echo '3/4 Installing downloaded packages; cancellation waits for dpkg'
            sudo -n apt-get -y -o APT::Get::AllowUnauthenticated=false --no-download ${if(upgrade) "" else "--no-upgrade"} install $specifications
            check_cancel
            echo '4/4 Compiling and running the verification program before activation'
            /usr/bin/${lang.tool} --version
            ${lang.smoke("/usr/bin/${lang.tool}")}
            check_cancel
            ensure_c_environment
            if [ -z "${'$'}PRESERVED_TOOL" ]; then commit_environment "${'$'}ENV_FILE" 'export ${lang.variable}=/usr/bin/${lang.tool}'; fi
            echo 'SUI_NATIVE_READY — ${lang.title}; existing tool selections and unrelated environments retained.'
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
