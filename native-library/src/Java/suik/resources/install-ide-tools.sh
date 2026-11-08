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
cleanup() {
    status=$?
    if [ "$status" -ne 0 ] && [ "$SWITCHED" = true ] && [ -n "$PREVIOUS" ]; then
        ln -s "$PREVIOUS" "$STAGE/restore-current"
        mv -Tf "$STAGE/restore-current" "$ROOT/current"
        echo 'Restored previous Java IDE tool selection; validated downloads and backups retained.' >&2
    fi
    rm -rf -- "$STAGE"
}
trap cleanup EXIT
check_cancel() { [ -z "${SUI_KIT_CANCEL:-}" ] || [ ! -f "$SUI_KIT_CANCEL" ] || exit 130; }
check_cancel
if [ ! -r "$ROOT/$VERSION/config_linux/config.ini" ] || [ ! -r "$ROOT/$VERSION/java-debug.jar" ]; then
    echo 'Download verified Eclipse JDT LS and Microsoft Java Debug'
    curl -fL --proto '=https' --tlsv1.2 --retry 2 --connect-timeout 20 --max-time 600 https://download.eclipse.org/jdtls/milestones/1.63.0/jdt-language-server-1.63.0-202610191322.tar.gz -o "$STAGE/server.tar.gz"
    printf '%s  %s\n' b1f2abe0b3cccb462cf64fb4a5587c7d09df60cf8be7748b152cbd89d018bdc4 "$STAGE/server.tar.gz" | sha256sum -c -
    check_cancel
    curl -fL --proto '=https' --tlsv1.2 --retry 2 --connect-timeout 20 --max-time 300 https://repo.maven.apache.org/maven2/com/microsoft/java/com.microsoft.java.debug.plugin/0.53.1/com.microsoft.java.debug.plugin-0.53.1.jar -o "$STAGE/java-debug.jar"
    printf '%s  %s\n' 4f4778d452a6a0665536f43ce4e32403a24be6593336b80dc85a322912859e24 "$STAGE/java-debug.jar" | sha256sum -c -
    check_cancel
    mkdir "$STAGE/unpacked"
    tar --no-same-owner --no-same-permissions -xzf "$STAGE/server.tar.gz" -C "$STAGE/unpacked"
    cp "$STAGE/java-debug.jar" "$STAGE/unpacked/java-debug.jar"
    if [ -e "$ROOT/$VERSION" ]; then mv "$ROOT/$VERSION" "$ROOT/$VERSION.backup-$(date +%s)-$$"; fi
    mv "$STAGE/unpacked" "$ROOT/$VERSION"
fi
if [ ! -x "$ROOT/maven/$MAVEN/bin/mvn" ]; then
    echo 'Download verified Apache Maven'
    curl -fL --proto '=https' --tlsv1.2 --retry 2 --connect-timeout 20 --max-time 300 https://archive.apache.org/dist/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.tar.gz -o "$STAGE/maven.tar.gz"
    printf '%s  %s\n' bcfe4fe305c962ace56ac7b5fc7a08b87d5abd8b7e89027ab251069faebee516b0ded8961445d6d91ec1985dfe30f8153268843c89aa392733d1a3ec956c9978 "$STAGE/maven.tar.gz" | sha512sum -c -
    check_cancel
    mkdir "$STAGE/maven"
    tar --no-same-owner --no-same-permissions -xzf "$STAGE/maven.tar.gz" -C "$STAGE/maven"
    if [ -e "$ROOT/maven/$MAVEN" ]; then mv "$ROOT/maven/$MAVEN" "$ROOT/maven/$MAVEN.backup-$(date +%s)-$$"; fi
    mv "$STAGE/maven/$MAVEN" "$ROOT/maven/$MAVEN"
fi
check_cancel
ln -s "$ROOT/$VERSION" "$STAGE/new-current"
mv -Tf "$STAGE/new-current" "$ROOT/current"
SWITCHED=true
"$ROOT/maven/$MAVEN/bin/mvn" --version
[ -r "$ROOT/current/java-debug.jar" ] && [ -r "$ROOT/current/config_linux_arm/config.ini" ]
echo 'Java IDE tools verified; selected JDK, java.env and user project configuration retained.'
