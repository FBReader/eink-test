/* Copyright (c) 2026 Codex <codex@fbreader.org>. SPDX-License-Identifier: MIT */
package org.fbreader.einktest;

import android.graphics.*;
import java.util.ArrayList;
import java.util.List;

final class TextRenderer {
    static final String[] MODES = {
        "Reader defaults", "AA + hinting", "AA + hinting + subpixel",
        "Mono, no hinting", "Mono + hinting", "Threshold 96", "Threshold 128",
        "Threshold 160", "Threshold 192", "Dither 2 x 2", "Dither 4 x 4",
        "Error diffusion", "Dither 8 x 8"
    };
    static final String[] DETAILS = {
        "AA on / hint off / subpixel off",
        "AA on / hint on / subpixel off",
        "AA on / hint on / subpixel on",
        "AA off / hint off / subpixel off",
        "AA off / hint on / subpixel off",
        "AA first, then black/white cutoff 96",
        "AA first, then black/white cutoff 128",
        "AA first, then black/white cutoff 160",
        "AA first, then black/white cutoff 192",
        "AA first, then 2 x 2 ordered dots",
        "AA first, then 4 x 4 ordered dots",
        "AA first, then Floyd-Steinberg dots",
        "AA first, then 8 x 8 ordered dots"
    };
    // Original test prose: identical in every rendering method.
    static final String PASSAGE =
        "At first light, the reader opened a book beside the window. Thin strokes, " +
        "small curves, and the quiet spaces inside each letter made the page easy to read. " +
        "The words should feel clear without looking heavy.\n\n" +
        "Look closely at the letters a, e, s, m, and rn. Compare fine serifs, rounded bowls, " +
        "and the dots above i and j. Can you distinguish 1, I, l and 0, O? " +
        "Does a long paragraph remain comfortable, or do its edges look rough?\n\n" +
        "Beyond the garden, a narrow path followed the river. Leaves moved in the wind, " +
        "and sunlight touched the water. Nothing hurried the morning. " +
        "She turned another page and continued reading.";
    static Paint paint(Typeface face, int size, int mode) {
        Paint p = new Paint();
        p.setLinearText(false);
        p.setAntiAlias(mode != 3 && mode != 4);
        p.setDither(false);
        p.setHinting(mode == 1 || mode == 2 || mode == 4 ? Paint.HINTING_ON : Paint.HINTING_OFF);
        p.setSubpixelText(mode == 2);
        p.setTypeface(face);
        p.setTextSize(size);
        p.setColor(Color.BLACK);
        return p;
    }

    static List<String> lines(Typeface face, int size, int width) {
        // Common line breaks keep the compared passages and positions identical.
        Paint measure = paint(face, size, 0);
        List<String> result = new ArrayList<String>();
        for (String paragraph : PASSAGE.split("\n", -1)) {
            if (paragraph.isEmpty()) { result.add(""); continue; }
            String line = "";
            for (String word : paragraph.split(" ")) {
                String next = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && Math.round(measure.measureText(next)) > width - size) {
                    result.add(line);
                    line = word;
                } else line = next;
            }
            result.add(line);
        }
        return result;
    }

    static Bitmap render(Typeface face, int size, int mode, int width, int height, int page) {
        // Reader-style text: software Canvas, ARGB line bitmaps and integer baselines.
        Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.setDensity(Bitmap.DENSITY_NONE);
        out.eraseColor(Color.WHITE);
        Canvas canvas = new Canvas(out);
        Paint glyph = paint(face, size, mode);
        Paint copy = new Paint();
        copy.setFilterBitmap(false);
        int lineHeight = Math.round(size * 1.45f);
        int baseline = (int)Math.ceil(-glyph.getFontMetrics().top);
        int lineBitmapHeight = Math.max(lineHeight, baseline + (int)Math.ceil(glyph.getFontMetrics().bottom) + 1);
        Bitmap line = Bitmap.createBitmap(width, lineBitmapHeight, Bitmap.Config.ARGB_8888);
        line.setDensity(Bitmap.DENSITY_NONE);
        Canvas lineCanvas = new Canvas(line);
        List<String> lines = lines(face, size, width);
        int count = Math.max(1, (height - lineBitmapHeight) / lineHeight + 1);
        int start = (page * count) % lines.size();
        for (int i = 0; i < count && start + i < lines.size(); i++) {
            line.eraseColor(Color.TRANSPARENT);
            lineCanvas.drawText(lines.get(start+i), 0, baseline, glyph);
            canvas.drawBitmap(line, 0, i * lineHeight, copy);
        }
        line.recycle();
        if (mode >= 5) process(out, mode);
        return out;
    }

    static void process(Bitmap image, int mode) {
        int w = image.getWidth(), h = image.getHeight();
        int[] pixels = new int[w*h];
        image.getPixels(pixels, 0, w, 0, 0, w, h);
        if (mode <= 8) Dither.threshold(pixels, new int[] {96,128,160,192}[mode-5]);
        else if (mode == 9 || mode == 10 || mode == 12)
            Dither.ordered(pixels, w, mode == 9 ? 2 : mode == 10 ? 4 : 8);
        else Dither.diffuse(pixels, w);
        image.setPixels(pixels, 0, w, 0, 0, w, h);
    }

    static void selfTest() {
        for (int mode = 0; mode < MODES.length; mode++) {
            Bitmap b = render(Typeface.SERIF, 26, mode, 628, 270, 0);
            int black = 0, white = 0, gray = 0;
            for (int y = 0; y < b.getHeight(); y++) for (int x = 0; x < b.getWidth(); x++) {
                int p = b.getPixel(x,y);
                if (p == Color.BLACK) black++;
                else if (p == Color.WHITE) white++;
                else gray++;
            }
            if (black == 0 || white == 0 || ((mode == 3 || mode == 4 || mode >= 5) && gray != 0)
                || (mode <= 2 && gray == 0)) throw new AssertionError("Incorrect pixels for " + MODES[mode]);
            b.recycle();
        }
        for (int mode = 5; mode < MODES.length; mode++) {
            Bitmap b = Bitmap.createBitmap(32,16,Bitmap.Config.ARGB_8888);
            for (int y=0;y<16;y++) for(int x=0;x<32;x++) b.setPixel(x,y,x<16?Color.BLACK:Color.WHITE);
            process(b,mode);
            for (int y=0;y<16;y++) for(int x=0;x<32;x++)
                if(b.getPixel(x,y)!=(x<16?Color.BLACK:Color.WHITE)) throw new AssertionError("Endpoints " + mode);
            b.recycle();
        }
        android.util.Log.i("EinkTest", "PASS: 13 rendering modes, binary outputs and preserved endpoints");
    }
}
