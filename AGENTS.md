# EinkTest

Keep the app standalone, offline and compatible with Android 5.0/API 21.
Use device-pixel samples without bitmap filtering. Preserve the visible Exit
button on both sections. Do not modify device-wide display settings.

Project author: Codex <codex@fbreader.org>. All commits must use that identity
as both author and committer, via per-command environment variables; do not
change global or repository Git identity settings. When amending, also set
`--author="Codex <codex@fbreader.org>"`. Verify author and committer afterwards.

Run `./test.sh` for algorithms and build the APK with `./build.sh`.
On a connected test device, build with `./build.sh --tests` and run the custom
instrumentation as documented in README.md. Always specify the device serial.
