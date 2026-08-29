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
import XiYue.SiyoX.data.SiyoXDirManager;
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

                    // 初始化配置、目录与网络验证
                    AppSettings.init(activity.getApplicationContext());
                    VerifyManager.init(activity.getApplicationContext());
                    SiyoXDirManager.initDirectories(activity.getApplicationContext());

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

                    // 禁用裁剪，防止悬浮球在全屏移动时被系统父容器裁剪
                    decorView.setClipChildren(false);
                    decorView.setClipToPadding(false);
                    ViewGroup contentParent = decorView.findViewById(android.R.id.content);
                    if (contentParent != null) {
                        contentParent.setClipChildren(false);
                        contentParent.setClipToPadding(false);
                    }

                    View existing = decorView.findViewWithTag(OVERLAY_VIEW_TAG);
                    if (existing != null) {
                        existing.bringToFront();
                        return;
                    }

                    XposedBridge.log("[" + TAG + "] Mounting SiyoX Java Overlay to DecorView in " + activity.getClass().getName());

                    SiyoXOverlayLayout overlay = new SiyoXOverlayLayout(activity);
                    overlay.setTag(OVERLAY_VIEW_TAG);
                    activeOverlay = overlay;

                    FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    );

                    decorView.addView(overlay, rootParams);
                    overlay.bringToFront();
                    decorView.requestLayout();

                    XposedBridge.log("[" + TAG + "] SiyoX Java Overlay mounted successfully to root DecorView!");

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
