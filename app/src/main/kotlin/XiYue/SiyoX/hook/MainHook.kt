// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.hook

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.concurrent.atomic.AtomicBoolean

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        XposedBridge.log("[$TAG] SiyoX Xposed module initialized in Zygote")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET_PACKAGE) {
            return
        }

        XposedBridge.log("[$TAG] Injected into $TARGET_PACKAGE (process: ${lpparam.processName})")

        hookActivityLifecycle(lpparam)
    }

    private fun hookActivityLifecycle(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook all Activity onCreate in com.netease.x19
            XposedHelpers.findAndHookMethod(
                Activity::class.java,
                "onCreate",
                Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as? Activity ?: return
                        val currentPackage = activity.packageName
                        if (currentPackage != TARGET_PACKAGE) return

                        XposedBridge.log("[$TAG] Target Activity created: ${activity.javaClass.name}")

                        // When first activity launches, check verification status
                        if (!isSessionVerified.get()) {
                            onInterceptActivity(activity)
                        }
                    }
                }
            )

            // Also hook onResume to prevent bypassing via back stack or task switching
            XposedHelpers.findAndHookMethod(
                Activity::class.java,
                "onResume",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as? Activity ?: return
                        if (activity.packageName != TARGET_PACKAGE) return

                        if (!isSessionVerified.get()) {
                            onInterceptActivity(activity)
                        }
                    }
                }
            )

        } catch (t: Throwable) {
            XposedBridge.log("[$TAG] Error hooking Activity lifecycle: ${t.message}")
        }
    }

    private fun onInterceptActivity(activity: Activity) {
        if (isSessionVerified.get()) return

        mainHandler.post {
            try {
                // Method 1: Launch SiyoX full screen VerifyActivity
                val intent = Intent().apply {
                    setClassName("XiYue.SiyoX", "XiYue.SiyoX.ui.VerifyActivity")
                    putExtra("target_package", TARGET_PACKAGE)
                    putExtra("is_injected", true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                // Check if VerifyActivity can be started
                val resolveInfo = activity.packageManager.resolveActivity(intent, 0)
                if (resolveInfo != null) {
                    XposedBridge.log("[$TAG] Launching SiyoX VerifyActivity overlay...")
                    activity.startActivity(intent)
                } else {
                    XposedBridge.log("[$TAG] Standalone VerifyActivity not found, displaying in-process overlay")
                    GameOverlay.showOverlay(activity) {
                        isSessionVerified.set(true)
                    }
                }
            } catch (e: Exception) {
                XposedBridge.log("[$TAG] Fallback to in-process GameOverlay: ${e.message}")
                GameOverlay.showOverlay(activity) {
                    isSessionVerified.set(true)
                }
            }
        }
    }

    companion object {
        const val TAG = "SiyoX"
        const val TARGET_PACKAGE = "com.netease.x19"

        val isSessionVerified = AtomicBoolean(false)
        private val mainHandler = Handler(Looper.getMainLooper())

        fun notifyVerified() {
            isSessionVerified.set(true)
            XposedBridge.log("[$TAG] SiyoX authorization verified! Releasing target app.")
        }
    }
}
