package dev.blamspot.jcode.core.distro

/** Versioned, staged install. Never uses apt alternatives or removes an existing JDK. */
object JavaInstallScript {
    fun build(release: JavaRelease): String = """
        set -eu
        umask 022
        BASE=/opt/sui-code
        DEST=${shellQuote(release.home)}
        EXPECTED_ARCH=${shellQuote(release.architecture)}
        case "${'$'}(uname -m)" in aarch64|arm64) ARCH=aarch64 ;; x86_64|amd64) ARCH=x64 ;; *) echo 'Unsupported Linux architecture'; exit 1 ;; esac
        [ "${'$'}ARCH" = "${'$'}EXPECTED_ARCH" ] || { echo 'Environment architecture changed; query again.'; exit 1; }
        for tool in curl sha256sum tar mktemp flock; do
          command -v "${'$'}tool" >/dev/null || { echo "Missing prerequisite: ${'$'}tool"; exit 1; }
        done
        mkdir -p "${'$'}BASE/jdks" "${'$'}BASE/logs"
        # A failed or cancelled prior install never owns the final destination.
        [ ! -e "${'$'}DEST" ] || { echo "Preserving existing JDK at ${'$'}DEST; nothing overwritten."; exit 1; }
        exec 9>"${'$'}BASE/.java-install.lock"
        flock -n 9 || { echo 'Another Java install is running. Retry after it stops.'; exit 1; }
        STAGE=''
        cleanup() { [ -z "${'$'}STAGE" ] || rm -rf "${'$'}STAGE"; }
        trap cleanup EXIT
        trap 'exit 130' INT
        trap 'exit 143' TERM
        STAGE=${'$'}(mktemp -d "${'$'}BASE/jdks/.install-XXXXXX")
        echo '1/5 Download ${release.fileName} from Eclipse Adoptium'
        curl --fail --location --retry 2 --connect-timeout 20 --max-time 1800 --proto '=https' --proto-redir '=https' ${shellQuote(release.downloadUrl)} -o "${'$'}STAGE/archive.tar.gz"
        echo '2/5 Verify SHA-256 before extraction'
        printf '%s  %s\n' ${shellQuote(release.sha256.lowercase())} "${'$'}STAGE/archive.tar.gz" | sha256sum -c -
        # Reject paths that could escape the private extraction directory.
        tar -tzf "${'$'}STAGE/archive.tar.gz" > "${'$'}STAGE/members"
        if grep -E '(^/|(^|/)\.\.(/|${'$'}))' "${'$'}STAGE/members"; then echo 'Unsafe archive path'; exit 1; fi
        mkdir "${'$'}STAGE/jdk"
        echo '3/5 Extract into a private staging directory'
        tar -xzf "${'$'}STAGE/archive.tar.gz" --strip-components=1 --no-same-owner -C "${'$'}STAGE/jdk"
        echo '4/5 Validate java, javac, jshell and HelloWorld'
        JAVA_HOME="${'$'}STAGE/jdk"
        export JAVA_HOME
        export PATH="${'$'}JAVA_HOME/bin:${'$'}PATH"
        "${'$'}JAVA_HOME/bin/java" -version
        "${'$'}JAVA_HOME/bin/javac" -version
        "${'$'}JAVA_HOME/bin/jshell" --version
        mkdir "${'$'}STAGE/smoke"
        printf '%s\n' 'public class HelloWorld { public static void main(String[] args) { System.out.println("HelloWorld from sui-code"); } }' > "${'$'}STAGE/smoke/HelloWorld.java"
        "${'$'}JAVA_HOME/bin/javac" -d "${'$'}STAGE/smoke" "${'$'}STAGE/smoke/HelloWorld.java"
        OUTPUT=${'$'}("${'$'}JAVA_HOME/bin/java" -cp "${'$'}STAGE/smoke" HelloWorld)
        [ "${'$'}OUTPUT" = 'HelloWorld from sui-code' ] || { echo 'HelloWorld output mismatch'; exit 1; }
        printf '%s\n' "${'$'}OUTPUT"
        printf 'System.out.println("SUI_JSHELL_OK");\n/exit\n' | "${'$'}JAVA_HOME/bin/jshell" - > "${'$'}STAGE/jshell.log" 2>&1
        cat "${'$'}STAGE/jshell.log"
        grep -qx 'SUI_JSHELL_OK' "${'$'}STAGE/jshell.log"
        # Restore PATH before deciding whether an existing usable default needs preserving.
        PATH=${'$'}{PATH#*:}
        unset JAVA_HOME
        PRESERVE_DEFAULT=false
        if command -v java >/dev/null && command -v javac >/dev/null && java -version >/dev/null 2>&1 && javac -version >/dev/null 2>&1; then PRESERVE_DEFAULT=true; fi
        echo '5/5 Commit the validated JDK and configure the managed Java environment'
        mv -T -n "${'$'}STAGE/jdk" "${'$'}DEST"
        [ ! -d "${'$'}STAGE/jdk" ] || { echo 'Destination appeared during install; preserved it.'; exit 1; }
        printf 'export JAVA_HOME=%s\nexport PATH="${'$'}JAVA_HOME/bin:${'$'}PATH"\n' "${'$'}DEST" > "${'$'}STAGE/java.env"
        chmod 644 "${'$'}STAGE/java.env"
        mv -f "${'$'}STAGE/java.env" "${'$'}BASE/java.env"
        if [ "${'$'}PRESERVE_DEFAULT" = false ]; then
          # Only fill an absent default; never replace an existing profile file.
          if [ ! -e /etc/profile.d/sui-code-java.sh ]; then
            printf '%s\n' 'if ! command -v java >/dev/null 2>&1 || ! command -v javac >/dev/null 2>&1; then' '  . /opt/sui-code/java.env' 'fi' > /etc/profile.d/sui-code-java.sh
          fi
        else
          echo 'Existing system JDK and default preserved. Use the managed Java environment for QuickRun.'
        fi
        echo "JAVA_HOME=${'$'}DEST"
        echo 'Java verified. New terminals can source /opt/sui-code/java.env explicitly.'
    """.trimIndent()
}
