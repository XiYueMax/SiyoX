// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.ui

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import de.robv.android.xposed.XposedBridge
import XiYue.SiyoX.data.AppSettings
import XiYue.SiyoX.data.VerifyManager
import XiYue.SiyoX.ui.pages.SiyoXOverlayView
import XiYue.SiyoX.ui.theme.SiyoXTheme
import java.lang.ref.WeakReference

object FloatingOverlayManager {

    private const val TAG = "SiyoX"
    private const val OVERLAY_TAG = "SIYOX_FLOATING_OVERLAY"

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentActivityRef: WeakReference<Activity>? = null

    fun attach(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        currentActivityRef = WeakReference(activity)

        mainHandler.post {
            try {
                // Initialize settings and verify manager inside target process
                AppSettings.init(activity.applicationContext)
                VerifyManager.init(activity.applicationContext)

                val decorView = activity.window?.decorView as? ViewGroup
                if (decorView == null) {
                    XposedBridge.log("[$TAG] decorView is null, retry later")
                    return@post
                }

                // Check if already attached
                val existingView = decorView.findViewWithTag<ComposeView>(OVERLAY_TAG)
                if (existingView != null) {
                    XposedBridge.log("[$TAG] Overlay already attached to Activity ${activity.javaClass.name}")
                    return@post
                }

                XposedBridge.log("[$TAG] Attaching SiyoX Compose Overlay to ${activity.javaClass.name}")

                val composeView = ComposeView(activity).apply {
                    tag = OVERLAY_TAG
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Setup Lifecycle & SavedState owners
                    ViewTreeHelper.setupViewTree(this, activity)

                    setContent {
                        SiyoXTheme {
                            SiyoXOverlayView(
                                onDismissRequest = {
                                    // Minimize or dismiss
                                }
                            )
                        }
                    }
                }

                decorView.addView(composeView)
                XposedBridge.log("[$TAG] SiyoX In-Game Floating Ball & UI successfully mounted!")

            } catch (t: Throwable) {
                XposedBridge.log("[$TAG] Failed to attach Floating Overlay: ${t.message}")
                t.printStackTrace()
            }
        }
    }

    fun detach(activity: Activity) {
        mainHandler.post {
            try {
                val decorView = activity.window?.decorView as? ViewGroup ?: return@post
                val overlay = decorView.findViewWithTag<ComposeView>(OVERLAY_TAG)
                if (overlay != null) {
                    decorView.removeView(overlay)
                    XposedBridge.log("[$TAG] Detached overlay from ${activity.javaClass.name}")
                }
            } catch (t: Throwable) {
                XposedBridge.log("[$TAG] Error detaching overlay: ${t.message}")
            }
        }
    }
}
