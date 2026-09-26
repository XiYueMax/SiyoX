package XiYue.SiyoX.data;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class UniFixBypass {

    private static final String TAG = "UniFixBypass";

    private static final String TARGET_CLASS = "com.netease.ntunisdk.unifix.UniFixBase";

    private static final String PREFERRED_METHOD = "a";

    private static final String ENGINE_SO = "libminecraftpe.so";

    private static final String ORIGINAL_SUFFIX = ".com";
    private static final String INVALID_SUFFIX = ".dev";

    private static final AtomicBoolean sInstalled = new AtomicBoolean(false);

    private UniFixBypass() {
    }

    public static void apply(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            if (lpparam == null) {
                return;
            }

            if (lpparam.appInfo == null) {
                return;
            }
            final String nativeLibraryDir = lpparam.appInfo.nativeLibraryDir;
            if (TextUtils.isEmpty(nativeLibraryDir)) {
                return;
            }

            if (!TextUtils.equals(lpparam.packageName, lpparam.processName)) {
                return;
            }

            try {
                if (!new File(nativeLibraryDir, ENGINE_SO).exists()) {
                    return;
                }
            } catch (Throwable t) {

                return;
            }

            if (!isFeatureEnabled()) {
                log("Feature disabled by user, skip.");
                return;
            }

            if (!sInstalled.compareAndSet(false, true)) {
                log("Already installed, skip duplicate.");
                return;
            }

            ClassLoader classLoader = lpparam.classLoader;
            if (classLoader == null) {
                classLoader = UniFixBypass.class.getClassLoader();
            }

            Class<?> clazz = classLoader.loadClass(TARGET_CLASS);
            int hookedCount = hookCandidateMethods(clazz);

            if (hookedCount > 0) {
                log("Hooked " + hookedCount + " method(s) on " + TARGET_CLASS);
            } else {
                log("No matching method found on " + TARGET_CLASS + " (possibly renamed)");
                sInstalled.set(false);
            }
        } catch (Throwable t) {

            log("Apply failed: " + t);
            sInstalled.set(false);
        }
    }

    private static int hookCandidateMethods(Class<?> clazz) {
        int count = 0;

        try {
            Method preferred = clazz.getDeclaredMethod(PREFERRED_METHOD, Context.class);
            count += installHook(preferred);
        } catch (Throwable ignored) {

        }

        if (count > 0) {
            return count;
        }

        try {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                try {
                    Class<?>[] params = method.getParameterTypes();
                    if (params == null || params.length != 1) {
                        continue;
                    }
                    if (!Context.class.isAssignableFrom(params[0])) {
                        continue;
                    }
                    if (method.getReturnType() != String.class) {
                        continue;
                    }
                    count += installHook(method);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            log("Scan declared methods failed: " + t);
        }

        return count;
    }

    private static int installHook(final Method method) {
        try {
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Object result = param.getResult();
                        if (!(result instanceof String)) {
                            return;
                        }
                        String originalUrl = (String) result;
                        if (originalUrl.isEmpty()) {
                            return;
                        }
                        String hijacked = hijackUrl(originalUrl);
                        if (hijacked != null && !hijacked.equals(originalUrl)) {
                            param.setResult(hijacked);
                            log("Hijacked " + method.getName() + ": "
                                    + originalUrl + " -> " + hijacked);
                        }
                    } catch (Throwable t) {
                        log("afterHookedMethod error: " + t);
                    }
                }
            });
            log("Installed hook on " + method.getName() + "(Context):String");
            return 1;
        } catch (Throwable t) {
            log("hookMethod failed for " + method.getName() + ": " + t);
            return 0;
        }
    }

    private static String hijackUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        if (url.contains(INVALID_SUFFIX)) {
            return null;
        }
        if (url.contains(ORIGINAL_SUFFIX)) {
            return url.replace(ORIGINAL_SUFFIX, INVALID_SUFFIX);
        }

        return url + INVALID_SUFFIX;
    }

    private static boolean isFeatureEnabled() {
        try {
            AppSettings settings = AppSettings.get();
            if (settings != null) {
                return settings.isUniFixBypassEnabled();
            }
        } catch (Throwable t) {

            log("Read settings failed, default to enabled: " + t);
        }
        return true;
    }

    private static void log(String message) {
        try {
            XposedBridge.log("[" + TAG + "] " + message);
        } catch (Throwable ignored) {
        }
        try {
            android.util.Log.i(TAG, message);
        } catch (Throwable ignored) {
        }
    }
}
