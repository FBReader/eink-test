/* Copyright (c) 2026 Codex <codex@fbreader.org>. SPDX-License-Identifier: MIT */
package org.fbreader.einktest;

/** Pixel algorithms with no Android dependency. All outputs are opaque black or white. */
final class Dither {
    static final String[] NAMES = {"Bayer 8 × 8", "Bayer 4 × 4", "Bayer 2 × 2",
            "Floyd–Steinberg", "Threshold 128", "Threshold 192"};
    static final int BLACK = 0xff000000, WHITE = 0xffffffff;

    private Dither() {}

    /** Integer luminance after compositing transparent input on white. */
    static int grey(int argb) {
        int a = argb >>> 24;
        int y = (77 * ((argb >> 16) & 255) + 150 * ((argb >> 8) & 255)
                + 29 * (argb & 255) + 128) >> 8;
        return (y * a + 255 * (255 - a) + 127) / 255;
    }

    static int[][] bayer(int n) {
        if (n != 2 && n != 4 && n != 8) throw new IllegalArgumentException("Bayer size");
        int[][] matrix = {{0}};
        for (int size = 1; size < n; size *= 2) {
            int[][] next = new int[size * 2][size * 2];
            for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
                int value = matrix[y][x] * 4;
                next[y][x] = value;
                next[y][x + size] = value + 2;
                next[y + size][x] = value + 3;
                next[y + size][x + size] = value + 1;
            }
            matrix = next;
        }
        return matrix;
    }

    static void apply(int[] pixels, int width, int method) {
        if (width <= 0 || pixels.length % width != 0 || method < 0 || method >= NAMES.length)
            throw new IllegalArgumentException("Invalid image or method");
        if (method < 3) ordered(pixels, width, method == 0 ? 8 : method == 1 ? 4 : 2);
        else if (method == 3) diffuse(pixels, width);
        else threshold(pixels, method == 4 ? 128 : 192);
    }

    static void threshold(int[] pixels, int cutoff) {
        for (int i = 0; i < pixels.length; i++) pixels[i] = grey(pixels[i]) <= cutoff ? BLACK : WHITE;
    }

    static void ordered(int[] pixels, int width, int n) {
        int[][] matrix = bayer(n);
        for (int y = 0; y < pixels.length / width; y++) for (int x = 0; x < width; x++) {
            int i = y * width + x;
            pixels[i] = grey(pixels[i]) * 2 * n * n > (2 * matrix[y % n][x % n] + 1) * 255
                    ? WHITE : BLACK;
        }
    }

    /** Serpentine Floyd–Steinberg: alternate traversal direction on successive rows. */
    static void diffuse(int[] pixels, int width) {
        float[] current = new float[width + 2], next = new float[width + 2];
        for (int y = 0; y < pixels.length / width; y++) {
            boolean reverse = (y & 1) != 0;
            int direction = reverse ? -1 : 1;
            for (int j = 0; j < width; j++) {
                int x = reverse ? width - 1 - j : j, i = y * width + x, e = x + 1;
                float value = grey(pixels[i]) + current[e];
                int output = value < 128 ? 0 : 255;
                pixels[i] = output == 0 ? BLACK : WHITE;
                float error = value - output;
                current[e + direction] += error * 7 / 16;
                next[e - direction] += error * 3 / 16;
                next[e] += error * 5 / 16;
                next[e + direction] += error / 16;
            }
            float[] swap = current;
            current = next;
            next = swap;
            java.util.Arrays.fill(next, 0);
        }
    }
}
