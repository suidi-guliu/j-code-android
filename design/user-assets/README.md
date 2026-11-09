# Official user-supplied sui-code icon

Exact source: `sui-code/user-icon-asset`, commit `8561a4bc826469baf4491cefbceeff769c6100ab`, `sui-code-app-icon.svg`.

SVG SHA-256: `39003c5686cdeaf2e09d018c17787f883667815d43227a8dd38b9b54634a60c3`.

`python3 tools/ide_reliability/build_launcher.py` mechanically renders this SVG using Inkscape. It removes only the full background rectangle from the adaptive foreground, applies a uniform 0.8 safe-area inset to the unchanged artwork, and renders a transparent 864px PNG. Android supplies the single `#151922` background layer. No path, gradient, S., bracket, relative position or aspect ratio is redesigned. Release, Debug, Beta and round resources share the exact same foreground.

Actual Android resource rendering, adaptive masks and 48/96/216px screenshots are tested by `LauncherIconRuntimeTest`; emulator launcher screenshots and physical ARM64 review remain separate evidence levels. The source SVG is never overwritten by conversion.
