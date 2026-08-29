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

    public static native String nativeVerifyCard(int verifyType, String card, String imei);

    public static native String nativeFetchNotice(int verifyType);
}
