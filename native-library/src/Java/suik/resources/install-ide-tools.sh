#!/bin/bash
set -euo pipefail
. /opt/sui-code/java.env
[ -x "$JAVA_HOME/bin/java" ] || { echo 'The selected managed JDK is unavailable' >&2; exit 78; }
ROOT=/opt/sui-code/ide/java
VERSION=1.63.0
MAVEN=apache-maven-3.9.11
mkdir -p "$ROOT/maven"
STAGE=$(mktemp -d "$ROOT/.download-XXXXXX")
PREVIOUS=$(readlink "$ROOT/current" || true)
SWITCHED=false
SERVER_BACKUP=''
MAVEN_BACKUP=''
cleanup() {
    status=$?
    if [ "$status" -ne 0 ] && [ "$SWITCHED" = true ] && [ -n "$PREVIOUS" ]; then
        ln -s "$PREVIOUS" "$STAGE/restore-current"
        mv -Tf "$STAGE/restore-current" "$ROOT/current"
        echo 'Restored previous Java IDE tool selection; validated downloads and backups retained.' >&2
    fi
    if [ "$status" -ne 0 ]; then
        if [ -n "$SERVER_BACKUP" ] && [ -d "$SERVER_BACKUP" ]; then
            if [ -e "$ROOT/$VERSION" ]; then mv -T "$ROOT/$VERSION" "$ROOT/$VERSION.failed-$(date +%s)-$$"; fi
            mv -T "$SERVER_BACKUP" "$ROOT/$VERSION"
        fi
        if [ -n "$MAVEN_BACKUP" ] && [ -d "$MAVEN_BACKUP" ]; then
            if [ -e "$ROOT/maven/$MAVEN" ]; then mv -T "$ROOT/maven/$MAVEN" "$ROOT/maven/$MAVEN.failed-$(date +%s)-$$"; fi
            mv -T "$MAVEN_BACKUP" "$ROOT/maven/$MAVEN"
        fi
    fi
    rm -rf -- "$STAGE"
}
trap cleanup EXIT
check_cancel() { [ -z "${SUI_KIT_CANCEL:-}" ] || [ ! -f "$SUI_KIT_CANCEL" ] || exit 130; }
selected=" ${SUI_KIT_COMPONENTS:-jdtls java-debug maven} "
want() { case "$selected" in *" $1 "*) return 0;; *) return 1;; esac; }
check_cancel
if want jdtls && { [ ! -r "$ROOT/$VERSION/config_linux/config.ini" ] || [ "${SUI_KIT_REPAIR:-0}" = 1 ]; }; then
    echo 'Download verified Eclipse JDT LS'
    curl -fL --proto '=https' --tlsv1.2 --retry 2 --connect-timeout 20 --max-time 600 https://download.eclipse.org/jdtls/milestones/1.63.0/jdt-language-server-1.63.0-202610191322.tar.gz -o "$STAGE/server.tar.gz"
    printf '%s  %s\n' b1f2abe0b3cccb462cf64fb4a5587c7d09df60cf8be7748b152cbd89d018bdc4 "$STAGE/server.tar.gz" | sha256sum -c -
    check_cancel
    mkdir "$STAGE/unpacked"
    tar --no-same-owner --no-same-permissions -xzf "$STAGE/server.tar.gz" -C "$STAGE/unpacked"
    if [ -r "$ROOT/$VERSION/java-debug.jar" ]; then cp "$ROOT/$VERSION/java-debug.jar" "$STAGE/unpacked/java-debug.jar"; fi
    if [ -e "$ROOT/$VERSION" ]; then SERVER_BACKUP="$ROOT/$VERSION.backup-$(date +%s)-$$"; mv "$ROOT/$VERSION" "$SERVER_BACKUP"; fi
    mv "$STAGE/unpacked" "$ROOT/$VERSION"
fi
if want java-debug && { [ ! -r "$ROOT/$VERSION/java-debug.jar" ] || [ "${SUI_KIT_REPAIR:-0}" = 1 ]; }; then
    [ -r "$ROOT/$VERSION/config_linux/config.ini" ] || { echo '[E_DEPENDENCY] Install JDT LS before Java Debug' >&2; exit 78; }
    curl -fL --proto '=https' --tlsv1.2 --retry 2 --connect-timeout 20 --max-time 300 https://repo.maven.apache.org/maven2/com/microsoft/java/com.microsoft.java.debug.plugin/0.53.1/com.microsoft.java.debug.plugin-0.53.1.jar -o "$STAGE/java-debug.jar"
    printf '%s  %s\n' 4f4778d452a6a0665536f43ce4e32403a24be6593336b80dc85a322912859e24 "$STAGE/java-debug.jar" | sha256sum -c -
    check_cancel
    mv -fT "$STAGE/java-debug.jar" "$ROOT/$VERSION/java-debug.jar"
fi
if want maven && { [ ! -x "$ROOT/maven/$MAVEN/bin/mvn" ] || [ "${SUI_KIT_REPAIR:-0}" = 1 ]; }; then
    echo 'Download verified Apache Maven'
    curl -fL --proto '=https' --tlsv1.2 --retry 2 --connect-timeout 20 --max-time 300 https://archive.apache.org/dist/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.tar.gz -o "$STAGE/maven.tar.gz"
    printf '%s  %s\n' bcfe4fe305c962ace56ac7b5fc7a08b87d5abd8b7e89027ab251069faebee516b0ded8961445d6d91ec1985dfe30f8153268843c89aa392733d1a3ec956c9978 "$STAGE/maven.tar.gz" | sha512sum -c -
    check_cancel
    mkdir "$STAGE/maven"
    tar --no-same-owner --no-same-permissions -xzf "$STAGE/maven.tar.gz" -C "$STAGE/maven"
    if [ -e "$ROOT/maven/$MAVEN" ]; then MAVEN_BACKUP="$ROOT/maven/$MAVEN.backup-$(date +%s)-$$"; mv "$ROOT/maven/$MAVEN" "$MAVEN_BACKUP"; fi
    mv "$STAGE/maven/$MAVEN" "$ROOT/maven/$MAVEN"
fi
check_cancel
if want jdtls || want java-debug; then
    ln -s "$ROOT/$VERSION" "$STAGE/new-current"
    mv -Tf "$STAGE/new-current" "$ROOT/current"
    SWITCHED=true
    [ -r "$ROOT/current/config_linux_arm/config.ini" ]
fi
if want java-debug; then [ -r "$ROOT/current/java-debug.jar" ]; fi
if want maven; then "$ROOT/maven/$MAVEN/bin/mvn" --version; fi
echo 'Selected Java IDE tools verified; JDK, environment and project configuration retained.'
