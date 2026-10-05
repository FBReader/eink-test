#!/bin/sh
# Copyright (c) 2026 Codex <codex@fbreader.org>. SPDX-License-Identifier: MIT
set -eu
cd "$(dirname "$0")"
sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$sdk" ]; then
    if [ -d "$HOME/Library/Android/sdk" ]; then sdk="$HOME/Library/Android/sdk";
    elif [ -d "$HOME/Android/Sdk" ]; then sdk="$HOME/Android/Sdk";
    else echo 'Set ANDROID_SDK_ROOT to your Android SDK.' >&2; exit 1; fi
fi
bt="$sdk/build-tools/${BUILD_TOOLS_VERSION:-35.0.0}"
android="$sdk/platforms/android-${COMPILE_SDK:-35}/android.jar"
if [ ! -f "$android" ] || [ ! -x "$bt/d8" ]; then
    echo 'Install SDK platform 35 and build-tools 35.0.0 (or set COMPILE_SDK/BUILD_TOOLS_VERSION).' >&2
    exit 1
fi
rm -rf build/classes build/dex build/test-classes build/test-dex
mkdir -p build/classes build/dex
find app/src/main/java -name '*.java' > build/sources.txt
javac -encoding UTF-8 --release 8 -cp "$android" -d build/classes @build/sources.txt
find build/classes -name '*.class' > build/classes.txt
"$bt/d8" --min-api 21 --lib "$android" --output build/dex @build/classes.txt
"$bt/aapt" package -f -M app/src/main/AndroidManifest.xml -S app/src/main/res -I "$android" -F build/unsigned.apk
(cd build/dex && zip -q ../unsigned.apk classes.dex)
if [ ! -f build/debug.keystore ]; then
    keytool -genkeypair -keystore build/debug.keystore -storepass android -keypass android \
        -alias eink-test -keyalg RSA -keysize 2048 -validity 10000 \
        -dname 'CN=Codex, EMAILADDRESS=codex@fbreader.org' >/dev/null 2>&1
fi
sign_apk() {
    "$bt/zipalign" -f 4 "$1" build/aligned.apk
    "$bt/apksigner" sign --ks build/debug.keystore --ks-key-alias eink-test \
        --ks-pass pass:android --key-pass pass:android --out "$2" build/aligned.apk
    "$bt/apksigner" verify "$2"
}
sign_apk build/unsigned.apk build/EinkTest.apk
echo 'Built build/EinkTest.apk (development signature)'
if [ "${1:-}" = '--tests' ]; then
    mkdir -p build/test-classes build/test-dex
    find app/src/androidTest/java -name '*.java' > build/test-sources.txt
    javac -encoding UTF-8 --release 8 -cp "$android:build/classes" \
        -d build/test-classes @build/test-sources.txt
    find build/test-classes -name '*.class' > build/test-classes.txt
    "$bt/d8" --min-api 21 --lib "$android" --classpath build/classes --output build/test-dex @build/test-classes.txt
    "$bt/aapt" package -f -M tests/AndroidManifest.xml -I "$android" -F build/test-unsigned.apk
    (cd build/test-dex && zip -q ../test-unsigned.apk classes.dex)
    sign_apk build/test-unsigned.apk build/EinkTest-tests.apk
    echo 'Built build/EinkTest-tests.apk'
fi
