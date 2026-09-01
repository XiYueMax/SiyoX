package XiYue.SiyoX.hook;

import android.app.Activity;
import android.os.Bundle;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import XiYue.SiyoX.SiyoXConfig;
import XiYue.SiyoX.data.SiyoXLogger;
import XiYue.SiyoX.ui.FloatingOverlayManager;

public class MainHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String TAG = "SiyoX";

    @Override
    public void initZygote(StartupParam startupParam) {
        XposedBridge.log("[" + TAG + "] SiyoX Java Xposed module initialized in Zygote");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!SiyoXConfig.TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        XposedBridge.log("[" + TAG + "] Successfully injected into " + SiyoXConfig.TARGET_PACKAGE + " (process: " + lpparam.processName + ")");
        hookActivityLifecycle(lpparam);
    }

    private void hookActivityLifecycle(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                Activity.class,
                "onCreate",
                Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Activity activity = (Activity) param.thisObject;
                        if (activity == null || !SiyoXConfig.TARGET_PACKAGE.equals(activity.getPackageName())) return;
                        SiyoXLogger.init(activity);
                        SiyoXLogger.i(TAG, "Activity onCreate: " + activity.getClass().getName());
                        FloatingOverlayManager.attach(activity);
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                Activity.class,
                "onPostCreate",
                Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Activity activity = (Activity) param.thisObject;
                        if (activity == null || !SiyoXConfig.TARGET_PACKAGE.equals(activity.getPackageName())) return;
                        FloatingOverlayManager.attach(activity);
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                Activity.class,
                "onResume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Activity activity = (Activity) param.thisObject;
                        if (activity == null || !SiyoXConfig.TARGET_PACKAGE.equals(activity.getPackageName())) return;
                        FloatingOverlayManager.attach(activity);
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                Activity.class,
                "onWindowFocusChanged",
                boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Boolean hasFocus = (Boolean) param.args[0];
                        Activity activity = (Activity) param.thisObject;
                        if (activity == null || !SiyoXConfig.TARGET_PACKAGE.equals(activity.getPackageName())) return;
                        if (hasFocus != null && hasFocus) {
                            FloatingOverlayManager.attach(activity);
                        }
                    }
                }
            );

        } catch (Throwable t) {
            XposedBridge.log("[" + TAG + "] Error hooking Activity lifecycle: " + t.getMessage());
        }
    }
}
