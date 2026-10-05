# EinkTest

A small, offline Android app for comparing image dithering and reading-text
rendering on e-ink screens. Android 5.0 (API 21) or later. No permissions,
network access, analytics, native libraries, or bundled fonts.

Written by **Codex <codex@fbreader.org>**, released under the [MIT licence](LICENSE).

## Two sections

**Dithering** compares untouched grey pixels with processed black-and-white
pixels. Choose 4, 8, 16 or 32 grey strips, reverse their order, and examine a
continuous gradient and an original shaded-sphere test image. Methods:

- Ordered Bayer dithering: 2 × 2, 4 × 4 and 8 × 8.
- Serpentine Floyd–Steinberg error diffusion.
- Thresholds 128 and 192 (values at or below the cutoff become black).

**Font rendering** compares the same passage with identical font size and line
breaks in A and B. Choose system serif, sans or monospace, regular/bold/italic,
and sizes from 16 to 60 actual pixels. `Read B` shows a longer sample; `Next
passage` advances through the original test prose. Thirteen methods:

- Reader defaults: antialiasing on, hinting and subpixel positioning off.
- Antialiasing with hinting, with or without subpixel positioning.
- Monochrome with and without hinting.
- Antialiasing followed by thresholds 96, 128, 160 or 192.
- Antialiasing followed by Bayer 2 × 2, 4 × 4 or 8 × 8.
- Antialiasing followed by Floyd–Steinberg error diffusion.

The text path uses `Canvas.drawText` on transparent ARGB line bitmaps, then
composites at integer positions on white with filtering disabled. It mirrors
the technique used by DinoReader's reading screen, not its entire layout engine.
The two projects have no build-time or runtime dependency on each other.

Controls use Mono + Hinting so they remain readable on thresholded screens.
Settings persist across launches. Scroll for additional controls and samples.
**Exit is always at the top of both sections** and closes/removes the task;
Android Back does the same. The screen stays awake only while the app is open.

`Black / white redraw` briefly paints the app black and then white. It does
not invoke a vendor refresh API or change device-wide display settings.

## Interpreting results

Judge on the physical display. An Android screenshot captures pixels before
the e-ink controller processes them, so it can contain grey even when the
physical screen shows only black and white. Photograph the screen for before/
after comparisons. A threshold-like result describes the active rendering
path; it does not prove the panel hardware cannot display greyscale.

Patterns are generated at the final view size and drawn without scaling.
Bayer uses a repeated threshold matrix; Floyd–Steinberg propagates quantisation
error to neighbouring pixels. More dots can improve tonal appearance while
making small text less comfortable. Font rasterisation varies by Android
version and installed system fonts. The UI uses dp/sp; sample text uses px.

## Build

The dependency-free SDK build is the simplest path on macOS/Linux:

1. Install JDK 17, Android SDK platform 35 and build-tools 35.0.0.
2. Set `ANDROID_SDK_ROOT` (or `ANDROID_HOME`) to the SDK directory. The script
   also recognises the usual macOS and Linux SDK locations.
3. Run `./test.sh` then `./build.sh`.

Output: `build/EinkTest.apk`. The script needs `javac`, `java`, `keytool`, and
`zip` on PATH. It creates an ignored local development key in
`build/debug.keystore`; keep it to install subsequent builds over the first.
`BUILD_TOOLS_VERSION` and `COMPILE_SDK` can override the SDK defaults.

Alternatively, open this directory in Android Studio, or use an installed
Gradle compatible with Android Gradle Plugin 8.10.1 (Gradle 8.11.1 or later):

```sh
gradle :app:assembleDebug
```

There is no Gradle wrapper or downloaded application dependency. The SDK
script and Gradle use different development signing keys; uninstall before
switching between their APKs (this clears the app's test settings). Neither
build uses DinoReader signing keys or configuration. Release signing and
GitHub publishing are deliberately left to the project's release process.

The app compiles and targets API 35 while supporting Android 5.0/API 21.
Android 15 and later receive system-bar insets around the fixed header and
scrolling content. Builds here are intended for sideloading.

## Install and test

Always choose the intended device when several are attached:

```sh
adb devices
adb -s SERIAL install -r build/EinkTest.apk
adb -s SERIAL shell am start -n org.fbreader.einktest/.MainActivity
```

Package ID: `org.fbreader.einktest`. It coexists with the original `Grey Levels`
and `Reading Text Test` apps and DinoReader.

`./test.sh` checks Bayer rank permutations and tone coverage, binary output,
alpha compositing, endpoints, narrow images, threshold boundaries and average
tone preservation with error diffusion. Device-side tests exercise all text
methods, font families/sizes, both sections, and the visible Exit button:

```sh
./build.sh --tests
adb -s SERIAL install -r build/EinkTest.apk
adb -s SERIAL install -r build/EinkTest-tests.apk
adb -s SERIAL shell am instrument -w org.fbreader.einktest.test/org.fbreader.einktest.RendererInstrumentation
adb -s SERIAL uninstall org.fbreader.einktest.test
```

Device tests and visual checks have been run on a Veidoo E7 with Android 8.1.
Other Android versions have not yet been tested on hardware. Android lint
reports no errors; it retains advisory warnings about the target SDK, the
SDK-script/Gradle manifest duplication, backup configuration and localisation.

Development APKs, local SDK paths, signing keys and build products are excluded
from Git. See [AUTHORS](AUTHORS) for authorship and [AGENTS.md](AGENTS.md) for
project maintenance instructions.
