# User-supplied sui-code launcher mark

The user supplied `sui-code-app-icon.svg` as the final graphical source for a subsequent sui-code Android launcher icon.

- Graphic: preserve the SVG paths, dimensions, stroke, circle and gradients as provided; **no generated redesign**.
- Background: retain the exact original Android launcher adaptive background `#151922` from `app/src/main/res/values/colors.xml`. This staged SVG includes a `<rect>` solely to provide a self-contained preview.
- For Android adaptive icons: set `@color/ic_launcher_adaptive_background` to `#151922` (already present); render the user paths in the transparent foreground layer **without duplicating the SVG background rect in the foreground**, preserving launcher-mask behavior. Keep appropriate safe-zone and optical sizing; do not distort geometry.
- Update launch icon resources for debug and other shipped variants consistently and inspect the compiled APK plus launcher preview at small sizes. Do not alter app identity, signing, data or theme.
- This repository branch is only a **staging branch for user-provided artwork**, not a compiled or validated APK. Actual launcher conversion and building belongs in the next explicitly authorized application development run.
- This artwork supersedes the earlier provisional 'do not alter the icon' instruction **only when the user requests integrating this supplied artwork**; no other UI redesign is implied.
