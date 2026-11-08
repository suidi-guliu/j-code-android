# sui-code official native library

The `v1/` directory contains the six real signed packages for Java, combined C/C++, and Python. `index.json` is authenticated by `index.sig`; the APK pins `official-p256.der`. The catalog refers only to this Fork's independent branch. There is no JCode or VS Code marketplace fallback.

- `.suic`: language registration and single-file execution. Works without an IDE Kit.
- `.suik`: real LSP/DAP metadata, project templates, Build/Test/Run workflows and native panels. Disabling/removing a Kit preserves the Basic plugin, toolchains and user files.
- `src/`: reviewable package manifests, adapters, scripts and project templates.

No JDK, compiler, interpreter, Maven, language server or debugger binary is embedded. Installation uses reviewed official Temurin, Eclipse, Maven Central, Apache or signed Ubuntu downloads. Runtime installation always requires separate user confirmation in Language Center. PRoot is not a sandbox for hostile packages; process.run grants access to the application's guest files and network.

Maintainers rebuild with:

```sh
python3 tools/sui_packages/build_official.py --key /path/outside/repo/official-p256.pem
```

The key must match the pinned public key. Never commit a private key. ECDSA signing is nondeterministic: a rebuild changes package hashes and the signed catalog, so rebuild only when releasing changed reviewed content, then run tests and update the APK catalog together.

The [development specification](../docs/specifications/SUIC-SUIK-Development-Specification.md), [UI specification](../docs/specifications/SuiCode-UI-Design-Specification.md), SDK templates and production verifier define compatibility. Development examples remain unsigned and are not official library entries.
