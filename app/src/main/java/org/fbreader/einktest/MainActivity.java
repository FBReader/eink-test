/* Copyright (c) 2026 Codex <codex@fbreader.org>. SPDX-License-Identifier: MIT */
package org.fbreader.einktest;

import android.app.*;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** Native controls around software-rendered, device-pixel comparison samples. */
public final class MainActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout body;
    private Button ditherTab, fontTab;
    private int section, levels, method, reference, candidate, size, font, style, page;
    private boolean reverse, compare;
    private DitherView ditherView;
    private FontView fontView;
    private TextView sizeLabel;
    private static final String[] FONTS = {"System serif", "System sans", "System monospace"};
    private static final String[] STYLES = {"Regular", "Bold", "Italic"};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(android.os.Build.VERSION.SDK_INT >= 23 ? Color.WHITE : Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        if (android.os.Build.VERSION.SDK_INT >= 23)
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        prefs = getSharedPreferences("comparisons", MODE_PRIVATE);
        section = bounded("section", 0, 0, 1);
        levels = bounded("levels", 16, 4, 32);
        method = bounded("method", 0, 0, Dither.NAMES.length - 1);
        reference = bounded("reference", 0, 0, TextRenderer.MODES.length - 1);
        candidate = bounded("candidate", 4, 0, TextRenderer.MODES.length - 1);
        size = bounded("size", 26, 16, 60);
        font = bounded("font", 0, 0, 2); style = bounded("style", 0, 0, 2);
        page = bounded("page", 0, 0, 10000);
        reverse = prefs.getBoolean("reverse", false); compare = prefs.getBoolean("compare", true);

        LinearLayout root = column();
        root.setBackgroundColor(Color.WHITE);
        if (android.os.Build.VERSION.SDK_INT >= 35) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                v.setPadding(dp(8) + insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        dp(8) + insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
                return insets;
            });
        }
        LinearLayout header = row();
        TextView title = label("EinkTest", 23);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(button("Exit", this::finishAndRemoveTask));
        root.addView(header);
        LinearLayout tabs = row();
        ditherTab = button("Dithering", () -> showSection(0));
        fontTab = button("Font rendering", () -> showSection(1));
        addEqual(tabs, ditherTab); addEqual(tabs, fontTab);
        root.addView(tabs);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        body = column(); scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        showSection(section);
    }

    private int bounded(String key, int value, int min, int max) {
        return Math.max(min, Math.min(max, prefs.getInt(key, value)));
    }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(8), 0, dp(8), 0); return view;
    }
    private LinearLayout row() {
        LinearLayout view = new LinearLayout(this); view.setGravity(Gravity.CENTER_VERTICAL); return view;
    }
    private void addEqual(LinearLayout row, View view) {
        row.addView(view, new LinearLayout.LayoutParams(0, -2, 1));
    }
    private TextView label(String value, int sp) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(sp);
        view.setTextColor(Color.BLACK); view.setPadding(dp(4), dp(6), dp(4), dp(6));
        mono(view); return view;
    }
    private static void mono(TextView view) {
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        view.getPaint().setAntiAlias(false); view.getPaint().setSubpixelText(false);
        view.getPaint().setHinting(Paint.HINTING_ON);
    }
    private Button button(String title, Runnable action) {
        Button button = new Button(this); button.setText(title); button.setTextSize(14);
        button.setAllCaps(false); button.setTextColor(Color.BLACK); button.setMinHeight(dp(48));
        mono(button); button.setOnClickListener(v -> action.run()); return button;
    }
    private interface Choice { void accept(int value); }
    private void choose(String title, String[] values, int selected, Choice action) {
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(title)
                .setSingleChoiceItems(values, selected, (d, which) -> { d.dismiss(); action.accept(which); })
                .setNegativeButton("Cancel", null).create();
        dialog.setOnShowListener(d -> {
            // Newly scrolled list rows also receive legible mono UI text.
            View root = dialog.getWindow().getDecorView();
            root.getViewTreeObserver().addOnPreDrawListener(() -> { monoTree(root); return true; });
        });
        dialog.show();
    }
    private void monoTree(View view) {
        if (view instanceof TextView) mono((TextView)view);
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++) monoTree(group.getChildAt(i));
        }
    }
    private void save() {
        prefs.edit().putInt("section", section).putInt("levels", levels).putInt("method", method)
                .putInt("reference", reference).putInt("candidate", candidate).putInt("size", size)
                .putInt("font", font).putInt("style", style).putInt("page", page)
                .putBoolean("reverse", reverse).putBoolean("compare", compare).apply();
    }
    private void showSection(int selected) {
        if (ditherView != null) { ditherView.release(); ditherView = null; }
        if (fontView != null) { fontView.release(); fontView = null; }
        section = selected; save(); body.removeAllViews();
        ditherTab.setTypeface(null, section == 0 ? Typeface.BOLD : Typeface.NORMAL);
        fontTab.setTypeface(null, section == 1 ? Typeface.BOLD : Typeface.NORMAL);
        ditherTab.setSelected(section == 0); fontTab.setSelected(section == 1);
        if (section == 0) dithering(); else fonts();
        body.post(() -> ((ScrollView)body.getParent()).scrollTo(0, 0));
    }
    private void dithering() {
        body.addView(label("Compare on the physical screen. Screenshots may retain greys that the panel cannot show.", 14));
        body.addView(button("Method: " + Dither.NAMES[method], () ->
                choose("Dithering method", Dither.NAMES, method, v -> { method = v; showSection(0); })));
        LinearLayout controls = row();
        addEqual(controls, button(levels + " levels", () -> choose("Grey levels", new String[]{"4", "8", "16", "32"},
                levels == 4 ? 0 : levels == 8 ? 1 : levels == 16 ? 2 : 3,
                v -> { levels = 4 << v; showSection(0); })));
        addEqual(controls, button("Reverse", () -> { reverse = !reverse; save(); ditherView.redraw(); }));
        body.addView(controls);
        LinearLayout headings = row();
        headings.addView(label("RGB", 13), new LinearLayout.LayoutParams(dp(40), -2));
        addEqual(headings, label("Original grey", 13));
        addEqual(headings, label("Processed B/W", 13));
        body.addView(headings);
        ditherView = new DitherView();
        body.addView(ditherView, new LinearLayout.LayoutParams(-1, dp(levels * 25 + 250)));
        body.addView(label("The ramp and shaded sphere use the same input in both columns. Dots are drawn at one device pixel, without scaling.", 14));
        body.addView(button("Black / white redraw", this::redrawScreen));
        body.addView(label("Redraw paints black, then white. It does not change firmware or request a hardware refresh mode.", 13));
    }
    private void fonts() {
        body.addView(label("Identical text, font size and line breaks. Tap A or B to choose a rendering method.", 14));
        body.addView(button("A: " + TextRenderer.MODES[reference], () ->
                choose("Reference A", TextRenderer.MODES, reference, v -> { reference = v; showSection(1); })));
        body.addView(button("B: " + TextRenderer.MODES[candidate], () ->
                choose("Candidate B", TextRenderer.MODES, candidate, v -> { candidate = v; showSection(1); })));
        LinearLayout faces = row();
        addEqual(faces, button(FONTS[font], () -> choose("Font", FONTS, font, v -> { font = v; page = 0; showSection(1); })));
        addEqual(faces, button(STYLES[style], () -> choose("Style", STYLES, style, v -> { style = v; showSection(1); })));
        body.addView(faces);
        LinearLayout sizing = row();
        addEqual(sizing, button("Size −", () -> resize(-2)));
        sizeLabel = label(size + " px", 16); sizeLabel.setGravity(Gravity.CENTER); addEqual(sizing, sizeLabel);
        addEqual(sizing, button("Size +", () -> resize(2))); body.addView(sizing);
        LinearLayout actions = row();
        addEqual(actions, button(compare ? "Read B" : "Compare A / B", () -> { compare = !compare; page = 0; showSection(1); }));
        addEqual(actions, button("Next passage", () -> { page = (page + 1) % 10000; save(); fontView.redraw(); }));
        body.addView(actions);
        fontView = new FontView();
        body.addView(fontView, new LinearLayout.LayoutParams(-1, dp(compare ? 420 : 430)));
        body.addView(label("Samples use software Canvas text on transparent line bitmaps, composited at 1:1 pixels on white, like DinoReader’s reading screen. UI labels always use Mono + Hinting.", 14));
        body.addView(button("Black / white redraw", this::redrawScreen));
    }
    private void resize(int delta) {
        size = Math.max(16, Math.min(60, size + delta)); page = 0; save();
        sizeLabel.setText(size + " px"); fontView.redraw();
    }
    private void redrawScreen() {
        final View overlay = new View(this); overlay.setBackgroundColor(Color.BLACK);
        final ViewGroup root = findViewById(android.R.id.content);
        root.addView(overlay, new ViewGroup.LayoutParams(-1, -1));
        overlay.postDelayed(() -> overlay.setBackgroundColor(Color.WHITE), 500);
        overlay.postDelayed(() -> root.removeView(overlay), 1000);
    }
    @Override public void onBackPressed() { finishAndRemoveTask(); }
    @Override protected void onDestroy() {
        if (ditherView != null) ditherView.release();
        if (fontView != null) fontView.release();
        super.onDestroy();
    }

    private abstract class SampleView extends View {
        final Paint copy = new Paint(), ink = new Paint();
        SampleView() {
            super(MainActivity.this); setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            ink.setColor(Color.BLACK); ink.setTextSize(dp(13)); ink.setHinting(Paint.HINTING_ON);
            copy.setFilterBitmap(false); copy.setDither(false);
        }
        abstract void release();
        void redraw() { release(); invalidate(); }
        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) { release(); }
    }
    private final class DitherView extends SampleView {
        Bitmap original, processed;
        int left, width, patchHeight, rowHeight;
        @Override void release() {
            if (original != null) original.recycle(); if (processed != null) processed.recycle();
            original = processed = null;
        }
        void prepare() {
            left = dp(40); width = Math.max(1, (getWidth() - left - dp(12)) / 2);
            rowHeight = dp(25); patchHeight = levels * rowHeight + dp(220);
            int[] pixels = new int[width * patchHeight];
            for (int y = 0; y < patchHeight; y++) for (int x = 0; x < width; x++) {
                int value;
                if (y < levels * rowHeight) {
                    value = Math.round((y / rowHeight) * 255f / (levels - 1));
                } else if (y < levels * rowHeight + dp(50)) {
                    value = Math.round(x * 255f / Math.max(1, width - 1));
                } else {
                    double nx = (x - width / 2.0) / (width * .44);
                    double ny = (y - levels * rowHeight - dp(135)) / dp(70);
                    double r = nx * nx + ny * ny;
                    value = r > 1 ? 235 : (int)(35 + 210 * Math.max(0, -.4 * nx - .45 * ny + .7 * Math.sqrt(1 - r)));
                    value = Math.min(255, value);
                }
                if (reverse) value = 255 - value;
                pixels[y * width + x] = Color.rgb(value, value, value);
            }
            original = Bitmap.createBitmap(pixels, width, patchHeight, Bitmap.Config.ARGB_8888);
            original.setDensity(Bitmap.DENSITY_NONE);
            Dither.apply(pixels, width, method);
            processed = Bitmap.createBitmap(pixels, width, patchHeight, Bitmap.Config.ARGB_8888);
            processed.setDensity(Bitmap.DENSITY_NONE);
        }
        @Override protected void onDraw(Canvas c) {
            if (getWidth() < dp(60)) return;
            if (original == null) prepare();
            c.drawColor(Color.WHITE);
            c.drawBitmap(original, left, 0, copy); c.drawBitmap(processed, left + width + dp(12), 0, copy);
            for (int i = 0; i < levels; i++) {
                int value = Math.round(i * 255f / (levels - 1)); if (reverse) value = 255 - value;
                c.drawText(String.valueOf(value), 0, i * rowHeight + dp(17), ink);
                c.drawLine(left, i * rowHeight, getWidth(), i * rowHeight, ink);
            }
            c.drawLine(left, levels * rowHeight, getWidth(), levels * rowHeight, ink);
        }
    }
    private final class FontView extends SampleView {
        Bitmap a, b;
        int heading, sampleHeight;
        @Override void release() {
            if (a != null) a.recycle(); if (b != null) b.recycle(); a = b = null;
        }
        @Override protected void onDraw(Canvas c) {
            if (getWidth() == 0 || getHeight() == 0) return;
            if (b == null) {
                heading = dp(29); sampleHeight = Math.max(1, getHeight() / (compare ? 2 : 1) - heading - dp(8));
                Typeface face = Typeface.create(font == 0 ? "serif" : font == 1 ? "sans-serif" : "monospace", style);
                if (compare) a = TextRenderer.render(face, size, reference, getWidth(), sampleHeight, page);
                b = TextRenderer.render(face, size, candidate, getWidth(), sampleHeight, page);
            }
            c.drawColor(Color.WHITE);
            int top = 0;
            if (compare) {
                c.drawText("A: " + TextRenderer.MODES[reference], 0, dp(20), ink);
                c.drawBitmap(a, 0, heading, copy);
                top = getHeight() / 2; c.drawLine(0, top, getWidth(), top, ink);
            }
            c.drawText("B: " + TextRenderer.MODES[candidate], 0, top + dp(20), ink);
            c.drawBitmap(b, 0, top + heading, copy);
        }
    }
}
