// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.data;

import android.util.Log;

public class NativeVerify {

    private static final String TAG = "SiyoX_NativeVerify";
    private static boolean isNativeLoaded = false;

    static {
        try {
            System.loadLibrary("siyox_verify");
            isNativeLoaded = true;
            Log.i(TAG, "Native C verification library loaded successfully");
        } catch (Throwable t) {
            Log.w(TAG, "Native C library not loaded, using Java implementation: " + t.getMessage());
            isNativeLoaded = false;
        }
    }

    public static boolean isNativeLoaded() {
        return isNativeLoaded;
    }

    // ==================== C/C++ 独立配置读取接口 ====================
    public static native String nativeGetClientName();
    public static native String nativeGetClientAuthor();
    public static native boolean nativeGetEnableMd5Verify();
    public static native String nativeGetEpicAppKey();
    public static native int nativeGetEpicPort();
    public static native String[] nativeGetEpicHosts();
    public static native String nativeGetT3ConfigJson();
    public static native String nativeGetWeiYanConfigJson();
    public static native String nativeGetDefaultResourcesJson();

    // ==================== C/C++ 网络验证接口 ====================
    public static native String nativeVerifyCard(int verifyType, String card, String imei);
    public static native String nativeFetchNotice(int verifyType);
}

