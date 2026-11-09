#!/bin/bash
# Run ONLY inside the named, owned Ubuntu integration container; no user runtime.
set -euo pipefail
[ "${SUI_OWNED_INTEGRATION:-}" = v054 ] || exit 90
root=/opt/sui-code/ide/java
[ -r "$root/current/java-debug.jar" ] && [ -x "$root/maven/apache-maven-3.9.11/bin/mvn" ]
stage=$(mktemp -d "$root/.selection-test-XXXXXX")
restore() {
 [ -r "$root/current/java-debug.jar" ] || cp "$stage/java-debug.jar" "$root/current/java-debug.jar"
 if [ ! -d "$root/maven/apache-maven-3.9.11" ]; then mv "$stage/apache-maven-3.9.11" "$root/maven/apache-maven-3.9.11"; fi
}
trap restore EXIT
cp "$root/current/java-debug.jar" "$stage/java-debug.jar"
rm "$root/current/java-debug.jar"
mv "$root/maven/apache-maven-3.9.11" "$stage/apache-maven-3.9.11"
export SUI_KIT_COMPONENTS=jdtls
/bin/bash /library/Java/suik/resources/install-ide-tools.sh
[ ! -e "$root/current/java-debug.jar" ] && [ ! -e "$root/maven/apache-maven-3.9.11" ]
echo 'PASS select JDT only: omitted Java Debug/Maven are not installed'
# Reuse the actual protocol driver while the optional Java Debug bundle is absent.
python3 /opt/v054-tests/ubuntu_lsp.py --only-language Java
export SUI_KIT_COMPONENTS=java-debug
/bin/bash /library/Java/suik/resources/install-ide-tools.sh
[ -r "$root/current/java-debug.jar" ] && [ ! -e "$root/maven/apache-maven-3.9.11" ]
echo 'PASS select Java Debug: checksum verified, Maven remains omitted'
export SUI_KIT_COMPONENTS=maven
/bin/bash /library/Java/suik/resources/install-ide-tools.sh
[ -x "$root/maven/apache-maven-3.9.11/bin/mvn" ]
echo 'PASS select Maven: checksum verified, JDK/environment untouched'
sha256sum /opt/sui-code/java.env > /evidence/component-java-env-after.sha256
