// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.hook

import android.app.Activity
import android.os.Bundle
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import XiYue.SiyoX.ui.FloatingOverlayManager

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        XposedBridge.log("[$TAG] SiyoX Xposed module initialized in Zygote")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET_PACKAGE) {
            return
        }

        XposedBridge.log("[$TAG] Successfully injected into $TARGET_PACKAGE (process: ${lpparam.processName})")

        hookActivityLifecycle(lpparam)
    }

    private fun hookActivityLifecycle(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook Activity.onCreate
            XposedHelpers.findAndHookMethod(
                Activity::class.java,
                "onCreate",
                Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as? Activity ?: return
                        if (activity.packageName != TARGET_PACKAGE) return

                        XposedBridge.log("[$TAG] Activity onCreate: ${activity.javaClass.name}")
                        FloatingOverlayManager.attach(activity)
                    }
                }
            )

            // Hook Activity.onPostCreate
            XposedHelpers.findAndHookMethod(
                Activity::class.java,
                "onPostCreate",
                Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as? Activity ?: return
                        if (activity.packageName != TARGET_PACKAGE) return

                        XposedBridge.log("[$TAG] Activity onPostCreate: ${activity.javaClass.name}")
                        FloatingOverlayManager.attach(activity)
                    }
                }
            )

            // Hook Activity.onResume
            XposedHelpers.findAndHookMethod(
                Activity::class.java,
                "onResume",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as? Activity ?: return
                        if (activity.packageName != TARGET_PACKAGE) return

                        FloatingOverlayManager.attach(activity)
                    }
                }
            )

            // Hook Activity.onWindowFocusChanged
            XposedHelpers.findAndHookMethod(
                Activity::class.java,
                "onWindowFocusChanged",
                Boolean::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val hasFocus = param.args[0] as? Boolean ?: false
                        val activity = param.thisObject as? Activity ?: return
                        if (activity.packageName != TARGET_PACKAGE) return

                        if (hasFocus) {
                            FloatingOverlayManager.attach(activity)
                        }
                    }
                }
            )

        } catch (t: Throwable) {
            XposedBridge.log("[$TAG] Error hooking Activity lifecycle: ${t.message}")
            t.printStackTrace()
        }
    }

    companion object {
        const val TAG = "SiyoX"
        const val TARGET_PACKAGE = "com.netease.x19"
    }
}
