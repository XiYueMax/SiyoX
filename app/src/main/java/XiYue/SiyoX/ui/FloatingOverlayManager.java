// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.ui;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import de.robv.android.xposed.XposedBridge;
import XiYue.SiyoX.data.AppSettings;
import XiYue.SiyoX.data.VerifyManager;

public class FloatingOverlayManager {

    private static final String TAG = "SiyoX";
    private static final String OVERLAY_VIEW_TAG = "SIYOX_IN_GAME_OVERLAY";

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static SiyoXOverlayLayout activeOverlay = null;

    public static void attach(final Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (activity.isFinishing() || activity.isDestroyed()) return;

                    AppSettings.init(activity.getApplicationContext());
                    VerifyManager.init(activity.getApplicationContext());

                    ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
                    if (decorView == null) {
                        mainHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                attach(activity);
                            }
                        }, 300);
                        return;
                    }

                    View existing = decorView.findViewWithTag(OVERLAY_VIEW_TAG);
                    if (existing != null) {
                        existing.bringToFront();
                        return;
                    }

                    XposedBridge.log("[" + TAG + "] Mounting SiyoX Java Overlay to " + activity.getClass().getName());

                    SiyoXOverlayLayout overlay = new SiyoXOverlayLayout(activity);
                    overlay.setTag(OVERLAY_VIEW_TAG);
                    overlay.setLayoutParams(new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    ));
                    activeOverlay = overlay;

                    decorView.addView(overlay);
                    overlay.bringToFront();
                    decorView.requestLayout();

                    XposedBridge.log("[" + TAG + "] SiyoX Java Overlay mounted successfully!");

                } catch (Throwable t) {
                    XposedBridge.log("[" + TAG + "] Error attaching SiyoX overlay: " + t.getMessage());
                    t.printStackTrace();
                }
            }
        });
    }

    public static void detach(final Activity activity) {
        if (activity == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
                    if (decorView != null) {
                        View existing = decorView.findViewWithTag(OVERLAY_VIEW_TAG);
                        if (existing != null) {
                            decorView.removeView(existing);
                        }
                    }
                    activeOverlay = null;
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
    }
}
