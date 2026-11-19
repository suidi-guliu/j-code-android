#!/usr/bin/env bash
set -euo pipefail
: "${RUNNER_TEMP:?Owned Ubuntu runner required}" "${JAVA_HOME:?JDK 25 required}"
source /etc/os-release
test "$VERSION_ID" = '24.04'
export SUI_OWNED_INTEGRATION=1
export MAVEN_OPTS="-Dmaven.repo.local=$RUNNER_TEMP/maven-repository"
root="$RUNNER_TEMP/v055-ubuntu"
mkdir -p "$root/runtime" "$root/evidence" "$root/bin"
export PATH="$root/bin:/opt/sui-code/ide/java/maven/apache-maven-3.9.11/bin:$PATH"
export MAVEN_ARGS="-ntp -B -Dstyle.color=never"
unzip -q .delivery/sui-code-v0.5.5-candidate/integration-runtime.zip -d "$root/runtime"
# Bind the Android process APIs used by the packaged production code to the Linux kernel.
mapfile -t shim_sources < <(find tools/ide_reliability/linux-shim -name '*.java' -print)
javac --release 21 -d "$root/runtime/cli" "${shim_sources[@]}"
sudo apt-get update
sudo apt-get install -y --no-install-recommends gcc g++ cmake ninja-build jq clangd-18 lldb-18 python3-debugpy libtalloc-dev
ln -s "$(command -v clangd-18)" "$root/bin/clangd"
proot_source="$root/proot"
git init -q "$proot_source"
git -C "$proot_source" fetch --depth=1 https://github.com/termux/proot.git 52d65a4dc140ca554d241a1438ddd4f6b00c9813
git -C "$proot_source" checkout --detach FETCH_HEAD
make -C "$proot_source/src" -j2
sudo install -d -o "$(id -u)" -g "$(id -g)" /opt/sui-code
install -m 755 "$proot_source/src/proot" "$root/bin/proot"
sudo ln -s "$JAVA_HOME" /jdk
sudo ln -s "$JAVA_HOME/lib/security/cacerts" /run/java-cacerts
printf 'export JAVA_HOME=/jdk\nexport PATH="$JAVA_HOME/bin:$PATH"\n' > /opt/sui-code/java.env
printf 'export SUI_C=/usr/bin/gcc\n' > /opt/sui-code/c.env
printf 'export SUI_CPP=/usr/bin/g++\n' > /opt/sui-code/cpp.env
printf 'export SUI_PYTHON=/usr/bin/python3\n' > /opt/sui-code/python.env
bash native-library/src/Java/suik/resources/install-ide-tools.sh
{
  cat /etc/os-release
  uname -m
  java -version
  proot --version
  clangd-18 --version
  lldb-18 --version
  python3 -c 'import debugpy; print(debugpy.__version__)'
} > "$root/evidence/tool-versions.txt" 2>&1
integration=tools/ide_reliability
library=(--library "$PWD/native-library/src" --classpath "$root/runtime/cli:$root/runtime/lib/*" --proot "$root/bin/proot")
broker=(--broker "$PWD/$integration/pty_broker.py")
status=0
for language in Python Java C C_CPP; do
  python3 "$integration/ubuntu_debug_input.py" "${library[@]}" "${broker[@]}" \
    --only "$language" --protocol "$PWD/tools/sui_packages/integration/java_lsp_dap.py" \
    --output "$root/evidence/debug-$language" || status=1
done
python3 "$integration/ubuntu_cpp_definition.py" "${library[@]}" --output "$root/evidence/clangd" || status=1
python3 "$integration/ubuntu_task_results.py" "${library[@]}" "${broker[@]}" --output "$root/evidence/tasks" || status=1
exit "$status"
