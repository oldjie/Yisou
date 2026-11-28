// SPDX-License-Identifier: AGPL-3.0-only
package probe;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

/** Synthetic slow destination. It never reads, displays or logs URI contents. */
public final class SlowTargetActivity extends Activity {
    private static final String TAG = "YisouHandoffProbe";
    private static final long DELAY_MS = 1500;
    private int run;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        run = state == null ? 0 : state.getInt("run", 0);
        delayedFrame("onCreate");
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Do not inspect the incoming URI. A changed frame number identifies
        // the new draw even if Android briefly reuses the previous blue frame.
        delayedFrame("onNewIntent");
    }

    private void stamp(String phase, int frameRun) {
        Log.i(TAG, phase + " run=" + frameRun
                + " uptime_ms=" + SystemClock.uptimeMillis()
                + " wall_ms=" + System.currentTimeMillis());
    }

    private void delayedFrame(String phase) {
        final int frameRun = ++run;
        stamp(phase, frameRun);
        // Intentionally block only this disposable test application's main
        // thread. No result frame can be produced during the simulated startup.
        SystemClock.sleep(DELAY_MS);

        final FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLUE);
        TextView marker = new TextView(this);
        marker.setText("HANDOFF PROBE\nFRAME " + frameRun);
        marker.setTextColor(Color.WHITE);
        marker.setTextSize(24);
        marker.setGravity(Gravity.CENTER);
        root.addView(marker, new FrameLayout.LayoutParams(-1, -1));
        root.getViewTreeObserver().addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
            private boolean recorded;
            @Override public void onDraw() {
                if (recorded || frameRun != run) return;
                recorded = true;
                stamp("firstDraw", frameRun);
                // Removing an OnDrawListener inside its callback is forbidden.
                root.post(() -> {
                    ViewTreeObserver observer = root.getViewTreeObserver();
                    if (observer.isAlive()) observer.removeOnDrawListener(this);
                });
            }
        });
        setContentView(root);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("run", run);
    }
}
