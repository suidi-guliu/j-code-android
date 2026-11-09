# sui-code UI catalog and Simplified Chinese Kit

The production host contains **English only** defaults generated from `en.json`. Simplified Chinese lives solely in the independent package source `zh-Hans/`; it is never copied into main Android resources/assets. `releases/sui-code-zh-hans-1.0.0.suik` is a real format-1.0 `SUIKPKG\0` container built with the production authoring tool.

- Package ID: `dev.suicode.localization.zh-hans`, version `1.0.0`, locale `zh-Hans`.
- Requires sui-code **0.5.3+** with the expanded API4 UI catalog. Older hosts reject unknown keys safely. Existing 12-key SDK examples remain compatible and intentionally partial.
- Capability: **only `ui.localization`**. Dependencies: none. No setup scripts, processes, Ubuntu, JDK, Python or GCC are bundled or installed.
- **Unsigned / unverified publisher.** No official private signing key is available in the configured environment. No substitute key is added to the host trust store. Integrity uses the normal per-file SHA-256 checks; installation still requires explicit source and capability consent.
- Resource size limit remains **65536 bytes** and values remain plain text with validated named placeholders. Security, permission and signature consent text remains protected host English. Source code, real filenames, paths, terminal output and extension-authored text are not automatically translated.

Author/check/build:

```sh
python3 tools/localization/catalog.py --write  # after reviewing changes to en.json
python3 tools/localization/catalog.py
python3 tools/localization/audit.py            # fails on new unaudited display literals
python3 -m unittest discover -s tools/localization -p 'test_*.py'
python3 tools/sui_packages/check_specifications.py
python3 tools/sui_packages/pack.py localization/zh-Hans /tmp/sui-code-zh-hans-1.0.0.suik
./gradlew :core:native-packages:validatePackage -PsuiPackage=/tmp/sui-code-zh-hans-1.0.0.suik
```

Update Chinese values in `zh-Hans/resources/i18n/zh-Hans.json` whenever publishing a Core key. Generated Core Kotlin and schema must match `en.json`. To intentionally review a new unbound/technical/protected literal, run `audit.py --update-baseline` and inspect the complete diff; do not update it just to make a failing check green. [Coverage and gaps](../docs/localization/locale-key-audit.md) include source locations, keys and translations. Catalog completeness is **not** full application translation.

Installation: first upgrade the APK without uninstalling/clearing data. Packages → Import .suic / .suik → select the independent Kit → review **Unverified publisher**, check **I trust this unverified publisher**, approve **ui.localization**, then **Confirm plan and install**. Packages → Interface language → 简体中文. Enable/disable/uninstall use the existing package lifecycle; disabled/missing/uninstalled resources fall back to English. English can always be explicitly selected. Recovery archives follow the existing store policy and may remain after uninstall, but are no longer active. A new installation can use **Continue without environment** while setup is idle; this only defers the existing wizard and never marks Ubuntu ready. Code execution still requires a real configured runtime.
