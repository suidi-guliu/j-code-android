#!/bin/bash
set -u
fail_env() { echo "[QuickRun environment error] $*" >&2; exit 78; }
MANAGED_ENV=/opt/sui-code/java.env
[ -r "$MANAGED_ENV" ] || fail_env 'Missing /opt/sui-code/java.env. Install Java in Toolchains; no fallback JDK will be used.'
unset JAVA_HOME
. "$MANAGED_ENV" || fail_env 'Cannot load managed Java environment.'
[ -n "${JAVA_HOME:-}" ] || fail_env 'JAVA_HOME is not configured.'
[ -x "$JAVA_HOME/bin/java" ] && [ -x "$JAVA_HOME/bin/javac" ] || fail_env 'Managed java or javac is missing.'
"$JAVA_HOME/bin/java" -version || fail_env 'Managed Java cannot run.'
cd -- "$(dirname -- "$1")" || { echo '[QuickRun file error] Cannot open source directory.' >&2; exit 66; }
[ -f "$1" ] && [ -r "$1" ] || { echo '[QuickRun file error] Source is not readable.' >&2; exit 66; }
BUILD=$(mktemp -d "${TMPDIR:-/tmp}/sui-quickrun-XXXXXX") || exit 73
trap 'rm -rf -- "$BUILD"' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
mkdir "$BUILD/input" "$BUILD/classes" "$BUILD/source" || exit 73
INPUT="$BUILD/input/""$(basename -- "$1")"
cp -- "$1" "$INPUT" || exit 66
echo '[QuickRun] Checking Java source (temporary output only)'
if ! "$JAVA_HOME/bin/javac" -proc:none -d "$BUILD/classes" "$INPUT"; then
    echo '[QuickRun compilation failed]' >&2
    exit 65
fi
# Java 27 requires a source path matching its package. Use the JDK parser, not a
# regex over Java comments/strings. Both the snapshot and generated classes stay private.
cat > "$BUILD/SuiQuickRunSource.java" <<'SUI_SOURCE_INFO'
import java.nio.file.*;
import java.util.*;
import javax.tools.*;
import com.sun.source.util.JavacTask;
public class SuiQuickRunSource {
    public static void main(String[] args) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK compiler unavailable");
        try (var files = compiler.getStandardFileManager(null, null, null)) {
            var input = Path.of(args[0]);
            var task = (JavacTask) compiler.getTask(null, files, null, List.of("-proc:none"), null, files.getJavaFileObjects(input));
            var unit = task.parse().iterator().next();
            var base = Path.of(args[1]).toAbsolutePath().normalize();
            var folder = unit.getPackageName() == null ? base : base.resolve(unit.getPackageName().toString().replace('.', '/')).normalize();
            if (!folder.startsWith(base)) throw new IllegalArgumentException("Invalid package path");
            Files.createDirectories(folder);
            var source = folder.resolve(input.getFileName());
            Files.copy(input, source);
            System.out.print(source);
        }
    }
}
SUI_SOURCE_INFO
RUN_SOURCE=$("$JAVA_HOME/bin/java" "$BUILD/SuiQuickRunSource.java" "$INPUT" "$BUILD/source") || {
    echo '[QuickRun preparation failed] Cannot prepare Java source package.' >&2; exit 73;
}
echo '[QuickRun] Running Java source; terminal input is connected'
"$JAVA_HOME/bin/java" --class-path "$BUILD/classes" "$RUN_SOURCE"
RESULT=$?
if [ "$RESULT" -ne 0 ]; then
    echo "[QuickRun runtime failed] Java exit code: $RESULT" >&2
    exit 70
fi
echo '[QuickRun completed] exit 0'
