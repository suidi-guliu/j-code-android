package dev.blamspot.jcode.core.distro

/** Only installations owned by the native manager can be selected or removed. */
data class JavaInstallation(val home: String,val active: Boolean,val version: String)
object JavaInstallationScript {
    fun managedDirectory(root: java.io.File): java.io.File {
        var path=root.canonicalFile
        for(part in listOf("opt","sui-code","jdks")) {
            path=java.io.File(path,part)
            check(!java.nio.file.Files.isSymbolicLink(path.toPath())) { "Custom symlink JDK layout is preserved; automatic removal is unavailable" }
        }
        return path
    }

    fun checkedHome(home: String): String {
        require(home.matches(Regex("/opt/sui-code/jdks/temurin-[A-Za-z0-9.+_-]+-(aarch64|x64)"))) { "Not a managed JDK installation" }
        return shellQuote(home)
    }
    fun select(home: String): String {
        val path=checkedHome(home)
        return """
            set -eu
            HOME_DIR=$path
            [ -d "${'$'}HOME_DIR" ] && [ ! -L "${'$'}HOME_DIR" ] || exit 78
            for tool in java javac jshell; do "${'$'}HOME_DIR/bin/${'$'}tool" --version || exit 78; done
            STAGE=${'$'}(mktemp /opt/sui-code/.java-env-XXXXXX)
            trap 'rm -f -- "${'$'}STAGE"' EXIT
            printf 'export JAVA_HOME=%s\nexport PATH="${'$'}JAVA_HOME/bin:${'$'}PATH"\n' $path > "${'$'}STAGE"
            chmod 644 "${'$'}STAGE"
            mv -f -- "${'$'}STAGE" /opt/sui-code/java.env
            echo 'Managed Java selection updated; existing terminals and language servers must be restarted.'
        """.trimIndent()
    }
}
