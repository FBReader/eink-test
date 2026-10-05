#!/bin/sh
# Copyright (c) 2026 Codex <codex@fbreader.org>. SPDX-License-Identifier: MIT
set -eu
cd "$(dirname "$0")"
mkdir -p build/unit-tests
javac -encoding UTF-8 -d build/unit-tests app/src/main/java/org/fbreader/einktest/Dither.java tests/org/fbreader/einktest/DitherTest.java
java -cp build/unit-tests org.fbreader.einktest.DitherTest
