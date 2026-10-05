/* Copyright (c) 2026 Codex <codex@fbreader.org>. SPDX-License-Identifier: MIT */
package org.fbreader.einktest;

import java.util.Arrays;

public final class DitherTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(Dither.grey(0) == 255, "Transparent input composites on white");
        check(Math.abs(Dither.grey(0x80000000) - 127) <= 1, "Half-opacity black");
        for (int n : new int[]{2, 4, 8}) {
            boolean[] seen = new boolean[n*n];
            for (int[] row : Dither.bayer(n)) for (int v : row) {
                check(v >= 0 && v < n*n && !seen[v], "Bayer ranks are a permutation"); seen[v] = true;
            }
            int previous = -1;
            for (int gray = 0; gray <= 255; gray++) {
                int[] tile = new int[n*n]; Arrays.fill(tile, 0xff000000 | gray * 0x010101);
                Dither.ordered(tile, n, n);
                int white = 0;
                for (int p : tile) { check(p == Dither.BLACK || p == Dither.WHITE, "Binary Bayer output"); if (p == Dither.WHITE) white++; }
                check(white >= previous, "Increasing gray never reduces Bayer white coverage");
                check(Math.abs(white / (double)(n*n) - gray / 255.0) <= .5 / (n*n) + .0001, "Bayer tone error bounded");
                previous = white;
            }
        }
        for (int mode = 0; mode < Dither.NAMES.length; mode++) {
            for (int gray : new int[]{0, 255}) {
                int[] image = new int[99]; Arrays.fill(image, 0xff000000 | gray * 0x010101);
                int[] expected = image.clone(); Dither.apply(image, 9, mode);
                check(Arrays.equals(image, expected), "Endpoints preserved for " + mode);
            }
            int[] narrow = {Dither.BLACK, 0xff888888, Dither.WHITE}; Dither.apply(narrow, 1, mode);
            for (int p : narrow) check(p == Dither.BLACK || p == Dither.WHITE, "Narrow images");
        }
        for (int gray : new int[]{32, 64, 128, 192, 224}) {
            int[] image = new int[128*128]; Arrays.fill(image, 0xff000000 | gray * 0x010101);
            Dither.diffuse(image, 128); int white = 0;
            for (int p : image) { check(p == Dither.BLACK || p == Dither.WHITE, "Binary diffusion output"); if (p == Dither.WHITE) white++; }
            check(Math.abs(white / (double)image.length - gray / 255.0) < .012, "Diffusion preserves average tone");
        }
        int[] boundary = {0xff808080, 0xff818181}; Dither.threshold(boundary, 128);
        check(boundary[0] == Dither.BLACK && boundary[1] == Dither.WHITE, "Inclusive threshold cutoff");
        System.out.println("PASS: Bayer rank/coverage, all six methods, endpoints, alpha, narrow images, diffusion tone and cutoff");
    }
}
