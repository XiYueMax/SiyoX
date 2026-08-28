// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.ui

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

import de.robv.android.xposed.XposedBridge
import XiYue.SiyoX.data.AppSettings
import XiYue.SiyoX.data.VerifyManager
import java.lang.ref.WeakReference

object FloatingOverlayManager {

    private const val TAG = "SiyoX"
    private const val OVERLAY_VIEW_TAG = "SIYOX_IN_GAME_OVERLAY"

    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeOverlay: SiyoXOverlayLayout? = null
    private var currentActivityRef: WeakReference<Activity>? = null

    fun attach(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return

        currentActivityRef = WeakReference(activity)

        mainHandler.post {
            try {
                if (activity.isFinishing || activity.isDestroyed) return@post

                // Initialize AppSettings and VerifyManager with host context
                AppSettings.init(activity.applicationContext)
                VerifyManager.init(activity.applicationContext)

                val decorView = activity.window?.decorView as? ViewGroup
                if (decorView == null) {
                    XposedBridge.log("[$TAG] decorView is null, scheduling retry...")
                    mainHandler.postDelayed({ attach(activity) }, 300)
                    return@post
                }

                // Check if already present on this decorView
                val existing = decorView.findViewWithTag<View>(OVERLAY_VIEW_TAG)
                if (existing != null) {
                    XposedBridge.log("[$TAG] Overlay already mounted on ${activity.javaClass.name}")
                    existing.bringToFront()
                    return@post
                }

                XposedBridge.log("[$TAG] Mounting SiyoX Floating Ball & UI to ${activity.javaClass.name}")

                val overlay = SiyoXOverlayLayout(activity).apply {
                    tag = OVERLAY_VIEW_TAG
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                activeOverlay = overlay

                decorView.addView(overlay)
                overlay.bringToFront()
                decorView.requestLayout()

                XposedBridge.log("[$TAG] SiyoX In-Game Floating Ball & UI mounted successfully!")

            } catch (t: Throwable) {
                XposedBridge.log("[$TAG] Error attaching SiyoX overlay: ${t.message}")
                t.printStackTrace()
            }
        }
    }

    fun detach(activity: Activity) {
        mainHandler.post {
            try {
                val decorView = activity.window?.decorView as? ViewGroup ?: return@post
                val existing = decorView.findViewWithTag<View>(OVERLAY_VIEW_TAG)
                if (existing != null) {
                    decorView.removeView(existing)
                    XposedBridge.log("[$TAG] SiyoX overlay detached from ${activity.javaClass.name}")
                }
                if (activeOverlay === existing) {
                    activeOverlay = null
                }
            } catch (t: Throwable) {
                XposedBridge.log("[$TAG] Error detaching overlay: ${t.message}")
            }
        }
    }
}
