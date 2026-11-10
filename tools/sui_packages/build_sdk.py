#!/usr/bin/env python3
"""Export a standalone JVM SDK using exact production API sources (no Android SDK required)."""
import argparse,hashlib,pathlib,shutil
ROOT=pathlib.Path(__file__).resolve().parents[2]
def main(destination):
 dst=pathlib.Path(destination).resolve()
 if dst.exists() and any(dst.iterdir()):raise ValueError('Destination must be empty')
 dst.mkdir(parents=True,exist_ok=True)
 for rel in ['core/native-packages/src/main/java','docs/specifications','native-library','localization','tools/localization']:
  shutil.copytree(ROOT/rel,dst/rel,dirs_exist_ok=True,ignore=shutil.ignore_patterns("__pycache__","*.pyc"))
 for rel in ['LICENSE','gradlew','gradlew.bat','gradle/wrapper/gradle-wrapper.jar','gradle/wrapper/gradle-wrapper.properties']:
  target=dst/rel;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(ROOT/rel,target)
 (dst/'tools/sui_packages').mkdir(parents=True,exist_ok=True)
 for name in ['pack.py','check_specifications.py','requirements-dev.txt','generate_examples.py','generate_card_examples.py']:
  shutil.copy2(ROOT/'tools/sui_packages'/name,dst/'tools/sui_packages'/name)
 (dst/'settings.gradle.kts').write_text('rootProject.name = "sui-code-native-sdk"\n')
 (dst/'build.gradle.kts').write_text('''plugins {
    kotlin("jvm") version "2.2.20"
    application
}
repositories { mavenCentral() }
kotlin { jvmToolchain(21) }
sourceSets.main { kotlin.srcDir("core/native-packages/src/main/java") }
dependencies { implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0"); implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2") }
application { mainClass.set("dev.suicode.packages.PackageTool") }
''')
 sums=[]
 for p in sorted((ROOT/'core/native-packages/src/main/java').rglob('*.kt')):
  rel=p.relative_to(ROOT);assert p.read_bytes()==(dst/rel).read_bytes()
  sums.append(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+rel.as_posix())
 (dst/'API-SOURCE-SHA256SUMS').write_text('\n'.join(sums)+'\n')
 (dst/'README.md').write_text('''# sui-code Native SDK 0.4.3 / Core API 4 · UI catalog 0.5.5

This standalone JVM SDK copies the exact production Kotlin verifier, independent loaders, authorization API, native recipe/dataflow interpreter and design model. API-SOURCE-SHA256SUMS records byte identity. It does not emulate Android services or embed runtimes.

With JDK21 (Android SDK is not required):

```sh
./gradlew classes
./gradlew run --args="docs/specifications/examples/packages/minimal.suic"
./gradlew run --args="docs/specifications/examples/packages/minimal-kit.suik"
# Must fail with E_BOUNDARY:
./gradlew run --args="docs/specifications/examples/packages/illegal-plugin.suic"
```

Build a local development package with `python3 tools/sui_packages/pack.py SOURCE OUTPUT.suic` or `.suik`. The provided examples include actual runnable Basic resources and a native interactive statistics Kit, language IDE cards, utility cards and independent localization cards. Read both formal specifications in docs/specifications before adding capabilities. Advanced/special features belong in `.suik`.

PackageTool verifies SHA-256 and any present signature; the standalone CLI does not pin a publisher, so its trust label is unverified. The Android app pins native-library/official-p256.der and separately verifies the official library. Do not add a key to a trust store merely because a package includes it. Official private keys, Android signing keys, installed Ubuntu and user data are excluded.

LSP/DAP/terminal/UI operation backends are implemented in the complete Android source distribution, not simulated in this SDK. Installing the exact same package in Package Manager still requires review and permissions. Linux protocol validity is not an ARM64 tablet acceptance result.
''')
 print(f'SDK exported: {len(sums)} exact production API files')
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('destination');main(p.parse_args().destination)
