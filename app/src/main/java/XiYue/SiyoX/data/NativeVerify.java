package XiYue.SiyoX.data;

import android.util.Log;

public class NativeVerify {

    private static final String TAG = "SiyoX_NativeVerify";
    private static boolean isNativeLoaded = false;

    static {
        try {
            System.loadLibrary("siyox_verify");
            isNativeLoaded = true;
            SiyoXLogger.i(TAG, "Native C verification library loaded successfully");
        } catch (Throwable t) {
            SiyoXLogger.w(TAG, "Native C library not loaded, using Java implementation: " + t.getMessage());
            isNativeLoaded = false;
        }
    }

    public static boolean isNativeLoaded() {
        return isNativeLoaded;
    }

    public static native int nativeGetActiveVerifyType();
    public static native int nativeGetVersionCode();
    public static native String nativeGetClientName();
    public static native String nativeGetClientAuthor();
    public static native String nativeGetDefaultNoticeTitle();
    public static native String nativeGetDefaultNoticeContent();
    public static native String nativeGetDefaultUpdateTitle();
    public static native String nativeGetDefaultUpdateLog();
    public static native boolean nativeGetEnableMd5Verify();
    public static native String nativeGetEpicAppKey();
    public static native int nativeGetEpicPort();
    public static native String[] nativeGetEpicHosts();
    public static native String nativeGetT3ConfigJson();
    public static native String nativeGetWeiYanConfigJson();
    public static native String nativeGetDefaultResourcesJson();

    public static native String nativeT3VerifyCard(String card, String imei);
    public static native String nativeT3FetchNotice();
    public static native String nativeT3Heartbeat(String card, String statecode);

    public static native String nativeVerifyCard(int verifyType, String card, String imei);
    public static native String nativeFetchNotice(int verifyType);
}
