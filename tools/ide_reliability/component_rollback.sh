#!/bin/bash
set -euo pipefail
[ "${SUI_OWNED_INTEGRATION:-}" = v054 ] || exit 90
root=/opt/sui-code/ide/java
before=$(sha256sum "$root/maven/apache-maven-3.9.11/bin/mvn" /opt/sui-code/java.env "$root/current/java-debug.jar")
# Fail the commit AFTER the old Maven directory was preserved, not a simulated download response.
set +e
SUI_KIT_COMPONENTS=maven SUI_KIT_REPAIR=1 /bin/bash -c '
 mv() {
   if [ "${1:-}" = "${STAGE:-}/maven/${MAVEN:-}" ]; then echo "[test] injected Maven commit failure" >&2;return 91;fi
   /bin/mv "$@"
 }
 . /library/Java/suik/resources/install-ide-tools.sh
'
status=$?
set -e
[ "$status" = 91 ]
after=$(sha256sum "$root/maven/apache-maven-3.9.11/bin/mvn" /opt/sui-code/java.env "$root/current/java-debug.jar")
[ "$before" = "$after" ]
"$root/maven/apache-maven-3.9.11/bin/mvn" --version
echo 'PASS actual failed Maven commit rolls back selection; JDK environment and Java Debug byte-identical'
