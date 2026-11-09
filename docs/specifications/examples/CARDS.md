# Declarative card SDK examples (specification 0.4.2 / Core API 4)

These are real packages parsed by the production verifier and independent IDE Kit loader. They contain no JavaScript, HTML, DEX or executable UI. `card.version` is 1 and does not grant permissions or confer publisher trust.

| Source template | Package | What actually works |
|---|---|---|
| `third-party-language-kit/` | `packages/third-party-language-kit.suik` | Declares the actual pylsp backend and reviewed Ubuntu component. Requires installed/enabled `dev.suicode.python.basic` 1.x and Python runtime. Host renders all language slots; undeclared DAP/Build/Test/Run/Format/Lint are unavailable. |
| `third-party-utility-card/` | `packages/third-party-utility-card.suik` | Statistics command routes to permission-gated `PROJECT_READ`; a nonproject context returns a diagnostic. No private panel code is loaded. |
| `localization-card/` | `packages/localization-card.suik` | A small resource-only Chinese Kit; no Ubuntu needed. This is an SDK example, not the full Chinese translation delivery. |

Full Chinese source is `localization/zh-Hans/`, shipped as `sui-code-zh-hans-1.0.1.suik`, independently installable with no runtime. Existing 1.0.0 packages remain readable. All examples are unsigned: source trust and capabilities must be explicitly reviewed in Packages. Being listed in a signed download directory does not sign an unsigned package.

Build with `python3 tools/sui_packages/pack.py SOURCE OUTPUT.suik`. Validate with `PackageTool FILE [DEPENDENCY_ID=VERSION ...]`; supply `dev.suicode.python.basic=1.1.0` for the language example. Run `python3 tools/sui_packages/check_specifications.py` and the production `PackageCardsTest`/`OfficialLibraryTest`. The existing `packages/illegal-plugin.suic` must still be rejected with `E_BOUNDARY`.
