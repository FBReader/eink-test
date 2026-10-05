/* Copyright (c) 2026 Codex <codex@fbreader.org>. SPDX-License-Identifier: MIT */
package org.fbreader.einktest;

import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.concurrent.atomic.AtomicReference;

public final class RendererInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    private Button find(View view, String text) {
        if (view instanceof Button && ((Button)view).getText().toString().equals(text)) return (Button)view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++) { Button found = find(group.getChildAt(i), text); if (found != null) return found; }
        }
        return null;
    }
    private void onMain(Runnable action) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        runOnMainSync(() -> { try { action.run(); } catch (Throwable t) { failure.set(t); } });
        if (failure.get() != null) throw new AssertionError(failure.get());
    }
    private void click(Activity activity, String text) {
        onMain(() -> {
            Button button = find(activity.getWindow().getDecorView(), text);
            if (button == null || !button.isShown()) throw new AssertionError("Missing control: " + text);
            button.performClick();
        }); waitForIdleSync();
    }
    @Override public void onStart() {
        Bundle result = new Bundle(); Activity activity = null; boolean pass = false;
        try {
            TextRenderer.selfTest();
            for (Typeface face : new Typeface[]{Typeface.SERIF, Typeface.SANS_SERIF, Typeface.MONOSPACE})
                for (int size : new int[]{16, 26, 60}) for (int mode = 0; mode < TextRenderer.MODES.length; mode++) {
                    Bitmap bitmap = TextRenderer.render(face, size, mode, 240, 180, 1);
                    bitmap.recycle();
                }
            activity = startActivitySync(new Intent(getTargetContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            click(activity, "Dithering"); click(activity, "Reverse"); click(activity, "Reverse");
            click(activity, "Font rendering"); click(activity, "Size +"); click(activity, "Size −");
            click(activity, "Exit");
            if (!activity.isFinishing()) throw new AssertionError("Exit must finish the activity");
            result.putString("stream", "PASS: 13 text modes, three fonts/sizes, binary outputs, endpoints, both sections and Exit\n"); pass = true;
        } catch (Throwable t) { result.putString("stream", android.util.Log.getStackTraceString(t)); }
        finally { if (activity != null && !activity.isFinishing()) { final Activity a = activity; onMain(a::finish); } }
        finish(pass ? -1 : 1, result);
    }
}
