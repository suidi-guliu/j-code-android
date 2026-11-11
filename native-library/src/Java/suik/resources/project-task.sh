#!/bin/bash
set -euo pipefail
ACTION=$1
PROJECT=$2
. /opt/sui-code/java.env
[ -x "$JAVA_HOME/bin/java" ] && [ -x "$JAVA_HOME/bin/javac" ] || { echo 'Managed JDK is unavailable' >&2; exit 78; }
if [ -x /opt/sui-code/ide/java/maven/apache-maven-3.9.11/bin/mvn ]; then export PATH="/opt/sui-code/ide/java/maven/apache-maven-3.9.11/bin:$PATH"; fi
cd -- "$PROJECT"
if [ -f pom.xml ]; then
    command -v mvn >/dev/null || { echo 'Install the Java IDE tools (Maven) from Packages > IDE Kit Library' >&2; exit 78; }
    case "$ACTION" in build) exec mvn --strict-checksums compile ;; test) exec mvn --strict-checksums test ;; run) exec mvn --strict-checksums compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java ;; esac
elif [ -f gradlew ]; then
    case "$ACTION" in build) exec /bin/bash ./gradlew build ;; test) exec /bin/bash ./gradlew test ;; run) exec /bin/bash ./gradlew run ;; esac
else
    echo 'This Kit uses pom.xml or an existing Gradle wrapper for project Build/Test. Single files run through Java Basic Plugin.' >&2
    exit 64
fi
