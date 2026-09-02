package XiYue.SiyoX.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import XiYue.SiyoX.SiyoXConfig;
import epic.verify.api.EpicVerifySDK;
import epic.verify.api.Resp;

public class VerifyManager {

    public interface VerifyCallback {
        void onResult(boolean success, String message);
    }

    public interface NoticeCallback {
        void onResult(boolean success, String title, String content);
    }

    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean isVerified = false;
    private long expireTimestamp = 0L;
    private String statusMessage = "未验证";
    private String noticeTitle = SiyoXConfig.DEFAULT_NOTICE_TITLE;
    private String noticeContent = SiyoXConfig.DEFAULT_NOTICE_CONTENT;

    private static volatile VerifyManager instance;

    private VerifyManager(Context context) {
        this.appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        SiyoXConfig.initContext(this.appContext);
        if (SiyoXConfig.CURRENT_VERIFY_TYPE == SiyoXConfig.VerifyType.NONE) {
            this.isVerified = true;
            this.expireTimestamp = Long.MAX_VALUE;
            this.statusMessage = "已关闭网络验证";
        }
    }

    public static VerifyManager init(Context context) {
        if (instance == null) {
            synchronized (VerifyManager.class) {
                if (instance == null) {
                    instance = new VerifyManager(context);
                }
            }
        }
        return instance;
    }

    public static VerifyManager get() {
        if (instance == null) {
            throw new IllegalStateException("VerifyManager must be initialized first");
        }
        return instance;
    }

    public boolean isVerified() {
        if (SiyoXConfig.CURRENT_VERIFY_TYPE == SiyoXConfig.VerifyType.NONE) {
            return true;
        }
        return isVerified;
    }

    public long getExpireTimestamp() {
        return expireTimestamp;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getNoticeTitle() {
        return noticeTitle;
    }

    public String getNoticeContent() {
        return noticeContent;
    }

    public void logout() {
        if (SiyoXConfig.CURRENT_VERIFY_TYPE == SiyoXConfig.VerifyType.NONE) {
            return;
        }
        isVerified = false;
        expireTimestamp = 0L;
        statusMessage = "未验证";
        AppSettings.get().setCard("");
        AppSettings.get().setExpireTime(0L);
    }

    public String getActiveProviderName() {
        switch (SiyoXConfig.CURRENT_VERIFY_TYPE) {
            case NONE:
                return "关闭验证 (免卡密模式)";
            case EPIC:
                return "摇光云 (EPIC)";
            case T3:
                return "T3 网络验证 (Native C/C++)";
            case WEIYAN:
                return "微验验证 (Native C)";
            default:
                return "关闭验证";
        }
    }

    @SuppressLint("HardwareIds")
    public String getHWID() {
        try {
            String id = Settings.Secure.getString(appContext.getContentResolver(), Settings.Secure.ANDROID_ID);
            return id != null ? id : "unknown_hwid";
        } catch (Exception e) {
            return "unknown_hwid";
        }
    }

    public String getAndroidId() {
        return getHWID();
    }

    public void loadSoftwareNotice(final NoticeCallback callback) {
        if (SiyoXConfig.CURRENT_VERIFY_TYPE == SiyoXConfig.VerifyType.NONE) {
            notifyNoticeResult(callback, true, SiyoXConfig.DEFAULT_NOTICE_TITLE, SiyoXConfig.DEFAULT_NOTICE_CONTENT);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    switch (SiyoXConfig.CURRENT_VERIFY_TYPE) {
                        case EPIC: {
                            EpicVerifySDK sdk = new EpicVerifySDK(
                                    SiyoXConfig.EpicConfig.HOSTS,
                                    SiyoXConfig.EpicConfig.PORT,
                                    SiyoXConfig.EpicConfig.APP_KEY
                            );
                            sdk.setDeviceId(getAndroidId());
                            sdk.setPackageName(SiyoXConfig.TARGET_PACKAGE);
                            Resp resp = sdk.getSoftwareConfig();
                            if (resp.isSuccess()) {
                                EpicVerifySDK.Notice notice = sdk.getSoftwareNotice(resp);
                                if (notice != null && notice.hasNotice()) {
                                    noticeTitle = notice.title != null ? notice.title : "官方公告";
                                    noticeContent = notice.content != null ? notice.content : "欢迎使用 SiyoX 模块！";
                                    notifyNoticeResult(callback, true, noticeTitle, noticeContent);
                                    return;
                                }
                            }
                            notifyNoticeResult(callback, true, noticeTitle, noticeContent);
                            break;
                        }

                        case WEIYAN: {
                            if (NativeVerify.isNativeLoaded()) {
                                try {
                                    String nativeResp = NativeVerify.nativeFetchNotice(3);
                                    if (nativeResp != null && !nativeResp.isEmpty()) {
                                        JSONObject json = new JSONObject(nativeResp);
                                        int code = json.optInt("code", -1);
                                        if (code == 200) {
                                            JSONObject msgObj = json.optJSONObject("msg");
                                            String gg = msgObj != null ? msgObj.optString("app_gg", "") : json.optString("msg", "");
                                            if (!gg.isEmpty()) {
                                                noticeTitle = SiyoXConfig.DEFAULT_NOTICE_TITLE;
                                                noticeContent = gg;
                                                notifyNoticeResult(callback, true, noticeTitle, noticeContent);
                                                return;
                                            }
                                        }
                                    }
                                } catch (Throwable ignored) {}
                            }
                            try {
                                String host = SiyoXConfig.WeiYanConfig.API_HOST;
                                if (host == null || host.isEmpty()) host = "wy.llua.cn";
                                if (!host.startsWith("http://") && !host.startsWith("https://")) host = "http://" + host;
                                String appId = SiyoXConfig.WeiYanConfig.APP_ID;
                                String rc4Key = SiyoXConfig.WeiYanConfig.RC4_KEY;
                                String apiToken = SiyoXConfig.WeiYanConfig.API_TOKEN;
                                String noticeCode = SiyoXConfig.WeiYanConfig.NOTICE_CODE;
                                if (noticeCode == null || noticeCode.isEmpty()) noticeCode = "notice";

                                String url = host + "/api/?id=" + noticeCode;
                                String postBody = "app=" + appId;

                                byte[] respBytes = httpPostBytes(url, postBody.getBytes("UTF-8"));
                                String respStr = (respBytes != null && respBytes.length > 0) ? new String(respBytes, "UTF-8").trim() : "";
                                if (respStr.contains("\"code\":-1") && !noticeCode.equals("notice")) {
                                    url = host + "/api/?id=notice";
                                    respBytes = httpPostBytes(url, postBody.getBytes("UTF-8"));
                                    respStr = (respBytes != null && respBytes.length > 0) ? new String(respBytes, "UTF-8").trim() : "";
                                }
                                if (!respStr.isEmpty()) {
                                    String decStr = respStr;
                                    if (!respStr.startsWith("{")) {
                                        String d = rc4DecryptHexToString(rc4Key, respStr);
                                        if (d != null && !d.isEmpty() && d.startsWith("{")) {
                                            decStr = d;
                                        }
                                    }

                                    JSONObject json = new JSONObject(decStr);
                                    int code = json.optInt("code", -1);
                                    if (code == 200) {
                                        JSONObject msgObj = json.optJSONObject("msg");
                                        String gg = msgObj != null ? msgObj.optString("app_gg", "") : json.optString("msg", "");
                                        if (!gg.isEmpty()) {
                                            noticeTitle = SiyoXConfig.DEFAULT_NOTICE_TITLE;
                                            noticeContent = gg;
                                            notifyNoticeResult(callback, true, noticeTitle, noticeContent);
                                            return;
                                        }
                                    }
                                }
                            } catch (Throwable ignored) {}
                            notifyNoticeResult(callback, true, SiyoXConfig.DEFAULT_NOTICE_TITLE, SiyoXConfig.DEFAULT_NOTICE_CONTENT);
                            break;
                        }

                        case T3: {
                            if (NativeVerify.isNativeLoaded()) {
                                try {
                                    String nativeResp = NativeVerify.nativeT3FetchNotice();
                                    if (nativeResp != null && !nativeResp.isEmpty()) {
                                        JSONObject json = new JSONObject(nativeResp);
                                        int code = json.optInt("code", -1);
                                        if (code == 200) {
                                            String notice = json.optString("msg", "");
                                            if (!notice.isEmpty() && !notice.endsWith("=")) {
                                                noticeTitle = SiyoXConfig.DEFAULT_NOTICE_TITLE;
                                                noticeContent = notice;
                                                notifyNoticeResult(callback, true, noticeTitle, noticeContent);
                                                return;
                                            }
                                        }
                                    }
                                } catch (Throwable ignored) {}
                            }
                            String rc4Key = SiyoXConfig.T3Config.RC4_KEY;
                            String appKey = SiyoXConfig.T3Config.APP_KEY;
                            String url = buildT3Url(SiyoXConfig.T3Config.API_HOST, SiyoXConfig.T3Config.NOTICE_CODE);
                            if (url != null && !url.isEmpty()) {
                                try {
                                    byte[] respBytes;
                                    if (rc4Key != null && !rc4Key.isEmpty() && !rc4Key.equals("your_t3_rc4_key")) {
                                        long now = System.currentTimeMillis() / 1000L;
                                        String tEnc = hexRc4Encrypt(rc4Key, String.valueOf(now));
                                        String sSrc = "t=" + tEnc + "&" + appKey;
                                        String sVal = md5(sSrc);
                                        String sEnc = hexRc4Encrypt(rc4Key, sVal);
                                        String postBody = "t=" + tEnc + "&s=" + sEnc;
                                        respBytes = httpPostBytes(url, postBody.getBytes("UTF-8"));
                                    } else {
                                        respBytes = httpGetBytes(url);
                                    }
                                    if (respBytes != null && respBytes.length > 0) {
                                        String text;
                                        if (rc4Key != null && !rc4Key.isEmpty() && !rc4Key.equals("your_t3_rc4_key")) {
                                            text = rc4DecryptBytesToString(rc4Key, respBytes);
                                        } else {
                                            text = new String(respBytes, "UTF-8");
                                            if (text.contains("\uFFFD")) text = new String(respBytes, "GBK");
                                        }
                                        text = text.trim();
                                        if (!text.isEmpty()) {
                                            try {
                                                JSONObject json = new JSONObject(text);
                                                int code = json.optInt("code", -1);
                                                if (code == 200) {
                                                    String notice = json.optString("msg", "");
                                                    if (!notice.isEmpty() && !notice.endsWith("=")) {
                                                        noticeTitle = SiyoXConfig.DEFAULT_NOTICE_TITLE;
                                                        noticeContent = notice;
                                                        notifyNoticeResult(callback, true, noticeTitle, noticeContent);
                                                        return;
                                                    }
                                                }
                                            } catch (Exception ignored) {
                                                if (!text.startsWith("{") && !text.endsWith("=") && !text.contains("404")) {
                                                    noticeTitle = SiyoXConfig.DEFAULT_NOTICE_TITLE;
                                                    noticeContent = text;
                                                    notifyNoticeResult(callback, true, noticeTitle, noticeContent);
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                } catch (Throwable ignored) {}
                            }
                            notifyNoticeResult(callback, true, SiyoXConfig.DEFAULT_NOTICE_TITLE, SiyoXConfig.DEFAULT_NOTICE_CONTENT);
                            break;
                        }
                    }
                } catch (Exception e) {
                    notifyNoticeResult(callback, false, noticeTitle, noticeContent);
                }
            }
        }).start();
    }

    private void notifyNoticeResult(final NoticeCallback callback, final boolean success, final String title, final String content) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onResult(success, title, content);
            }
        });
    }

    public static class SoftwareUpdate {
        public boolean hasUpdate = false;
        public int latestVersionCode = 0;
        public String latestVersionName = "";
        public String title = "";
        public String log = "";
        public String downloadUrl = "";
        public boolean isForce = false;
    }

    private static SoftwareUpdate cachedSoftwareUpdate = null;
    private static boolean updateDismissed = false;

    public static SoftwareUpdate getCachedSoftwareUpdate() {
        return cachedSoftwareUpdate;
    }

    public static boolean isUpdateDismissed() {
        return updateDismissed;
    }

    public static void setUpdateDismissed(boolean dismissed) {
        updateDismissed = dismissed;
    }

    public interface UpdateCallback {
        void onUpdateResult(boolean hasUpdate, SoftwareUpdate update);
    }

    private void notifyUpdateResult(final UpdateCallback callback, final boolean hasUpdate, final SoftwareUpdate update) {
        if (hasUpdate && update != null) {
            cachedSoftwareUpdate = update;
        }
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onUpdateResult(hasUpdate, update);
                }
            }
        });
    }

    public void checkSoftwareUpdate(final UpdateCallback callback) {
        if (cachedSoftwareUpdate != null && cachedSoftwareUpdate.hasUpdate) {
            notifyUpdateResult(callback, true, cachedSoftwareUpdate);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int verifyType = SiyoXConfig.CURRENT_VERIFY_TYPE.ordinal();
                    if (SiyoXConfig.CURRENT_VERIFY_TYPE == SiyoXConfig.VerifyType.NONE) {
                        if (SiyoXConfig.EpicConfig.APP_KEY != null && !SiyoXConfig.EpicConfig.APP_KEY.isEmpty() && !SiyoXConfig.EpicConfig.APP_KEY.equals("your_epic_app_key")) {
                            verifyType = 1;
                        } else if (SiyoXConfig.T3Config.APP_KEY != null && !SiyoXConfig.T3Config.APP_KEY.isEmpty() && !SiyoXConfig.T3Config.APP_KEY.equals("your_t3_app_key")) {
                            verifyType = 2;
                        } else if (SiyoXConfig.WeiYanConfig.APP_KEY != null && !SiyoXConfig.WeiYanConfig.APP_KEY.isEmpty() && !SiyoXConfig.WeiYanConfig.APP_KEY.equals("your_weiyan_app_key")) {
                            verifyType = 3;
                        }
                    }

                    if (verifyType == 1) {
                        EpicVerifySDK sdk = new EpicVerifySDK(
                                SiyoXConfig.EpicConfig.HOSTS,
                                SiyoXConfig.EpicConfig.PORT,
                                SiyoXConfig.EpicConfig.APP_KEY
                        );
                        sdk.setDeviceId(getAndroidId());
                        sdk.setPackageName(SiyoXConfig.TARGET_PACKAGE);
                        sdk.setAppVersion(SiyoXConfig.VERSION_CODE);
                        Resp resp = sdk.getSoftwareConfig();
                        if (resp.isSuccess()) {
                            EpicVerifySDK.Version ver = sdk.getSoftwareLatestVersion(String.valueOf(SiyoXConfig.VERSION_CODE), resp);
                            if (ver != null && ver.hasUpdate) {
                                SoftwareUpdate update = new SoftwareUpdate();
                                update.hasUpdate = true;
                                update.latestVersionCode = ver.version;
                                update.latestVersionName = String.valueOf(ver.version);
                                update.title = ver.title != null && !ver.title.isEmpty() ? ver.title : SiyoXConfig.DEFAULT_UPDATE_TITLE;
                                update.log = ver.updateLog != null && !ver.updateLog.isEmpty() ? ver.updateLog : (ver.content != null && !ver.content.isEmpty() ? ver.content : SiyoXConfig.DEFAULT_UPDATE_LOG);
                                update.downloadUrl = ver.downloadUrl != null ? ver.downloadUrl : "";
                                update.isForce = (ver.updateType == 1 || !ver.cancelEnable);
                                notifyUpdateResult(callback, true, update);
                                return;
                            }
                        }
                    } else if (verifyType == 2) {
                        String host = SiyoXConfig.T3Config.API_HOST;
                        String vCode = SiyoXConfig.T3Config.VERSION_CODE_STR;
                        if (vCode != null && !vCode.isEmpty() && !vCode.equals("your_t3_version_code")) {
                            String url = buildT3Url(host, vCode);
                            byte[] respBytes = httpPostBytes(url, ("v=" + SiyoXConfig.VERSION_CODE).getBytes("UTF-8"));
                            if (respBytes != null && respBytes.length > 0) {
                                String raw = new String(respBytes, "UTF-8").trim();
                                if (!raw.startsWith("{")) {
                                    raw = rc4DecryptBytesToString(SiyoXConfig.T3Config.RC4_KEY, respBytes);
                                }
                                if (raw.contains("{")) {
                                    JSONObject json = new JSONObject(raw);
                                    int latestVer = json.optInt("version", json.optInt("code", 0));
                                    if (latestVer > SiyoXConfig.VERSION_CODE) {
                                        SoftwareUpdate update = new SoftwareUpdate();
                                        update.hasUpdate = true;
                                        update.latestVersionCode = latestVer;
                                        update.latestVersionName = String.valueOf(latestVer);
                                        update.title = json.optString("title", SiyoXConfig.DEFAULT_UPDATE_TITLE);
                                        update.log = json.optString("log", json.optString("msg", SiyoXConfig.DEFAULT_UPDATE_LOG));
                                        update.downloadUrl = json.optString("url", json.optString("download", ""));
                                        update.isForce = json.optInt("force", 0) == 1;
                                        notifyUpdateResult(callback, true, update);
                                        return;
                                    }
                                }
                            }
                        }
                    } else if (verifyType == 3) {
                        String host = SiyoXConfig.WeiYanConfig.API_HOST;
                        if (host == null || host.isEmpty()) host = "wy.llua.cn";
                        if (!host.startsWith("http://") && !host.startsWith("https://")) host = "http://" + host;
                        String appId = SiyoXConfig.WeiYanConfig.APP_ID;
                        String updateCode = SiyoXConfig.WeiYanConfig.UPDATE_CODE;
                        if (updateCode == null || updateCode.isEmpty()) updateCode = "checkupdate";
                        String url = host + "/api/?id=" + updateCode;
                        byte[] respBytes = httpPostBytes(url, ("app=" + appId + "&v=" + SiyoXConfig.VERSION_CODE).getBytes("UTF-8"));
                        if (respBytes != null && respBytes.length > 0) {
                            String raw = new String(respBytes, "UTF-8").trim();
                            if (!raw.startsWith("{")) {
                                String d = rc4DecryptHexToString(SiyoXConfig.WeiYanConfig.RC4_KEY, raw);
                                if (d != null && d.startsWith("{")) raw = d;
                            }
                            if (raw.startsWith("{")) {
                                JSONObject json = new JSONObject(raw);
                                if (json.optInt("code", -1) == 200) {
                                    JSONObject msgObj = json.optJSONObject("msg");
                                    int latestVer = msgObj != null ? msgObj.optInt("version", 0) : json.optInt("version", 0);
                                    if (latestVer > SiyoXConfig.VERSION_CODE) {
                                        SoftwareUpdate update = new SoftwareUpdate();
                                        update.hasUpdate = true;
                                        update.latestVersionCode = latestVer;
                                        update.latestVersionName = String.valueOf(latestVer);
                                        update.title = msgObj != null ? msgObj.optString("title", SiyoXConfig.DEFAULT_UPDATE_TITLE) : SiyoXConfig.DEFAULT_UPDATE_TITLE;
                                        update.log = msgObj != null ? msgObj.optString("log", msgObj.optString("msg", SiyoXConfig.DEFAULT_UPDATE_LOG)) : SiyoXConfig.DEFAULT_UPDATE_LOG;
                                        update.downloadUrl = msgObj != null ? msgObj.optString("url", msgObj.optString("download_url", "")) : "";
                                        update.isForce = (msgObj != null ? msgObj.optInt("force", 0) : 0) == 1;
                                        notifyUpdateResult(callback, true, update);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) {}
                notifyUpdateResult(callback, false, null);
            }
        }).start();
    }

    public void verifyCard(final String cardKey, final VerifyCallback callback) {
        if (SiyoXConfig.CURRENT_VERIFY_TYPE == SiyoXConfig.VerifyType.NONE) {
            onVerifySuccess("免卡密", Long.MAX_VALUE, callback);
            return;
        }
        if (cardKey == null || cardKey.trim().isEmpty()) {
            if (callback != null) callback.onResult(false, "卡密不能为空");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String androidId = getAndroidId();
                    SiyoXLogger.i("SiyoX_VerifyManager", "Verifying card via provider: " + getActiveProviderName());

                    switch (SiyoXConfig.CURRENT_VERIFY_TYPE) {
                        case EPIC: {
                            EpicVerifySDK sdk = new EpicVerifySDK(
                                    SiyoXConfig.EpicConfig.HOSTS,
                                    SiyoXConfig.EpicConfig.PORT,
                                    SiyoXConfig.EpicConfig.APP_KEY
                            );
                            sdk.setDeviceId(androidId);
                            sdk.setCard(cardKey.trim());
                            sdk.setPackageName(SiyoXConfig.TARGET_PACKAGE);

                            Resp resp = sdk.cardVerify();
                            if (resp.isSuccess()) {
                                EpicVerifySDK.LoginResult result = sdk.getLoginResult();
                                if (result != null) {
                                    onVerifySuccess(cardKey.trim(), result.expire, callback);
                                } else {
                                    onVerifyFailed("解析登录数据失败", callback);
                                }
                            } else {
                                onVerifyFailed(resp.msg, callback);
                            }
                            break;
                        }

                        case WEIYAN: {
                            if (NativeVerify.isNativeLoaded()) {
                                try {
                                    String nativeResp = NativeVerify.nativeVerifyCard(3, cardKey.trim(), androidId);
                                    if (nativeResp != null && !nativeResp.trim().isEmpty()) {
                                        JSONObject json = new JSONObject(nativeResp);
                                        int code = json.optInt("code", -1);
                                        if (code == 200) {
                                            JSONObject dataObj = json.optJSONObject("data");
                                            JSONObject msgObj = json.optJSONObject("msg");
                                            long expireTime = 0L;
                                            if (msgObj != null) {
                                                expireTime = msgObj.optLong("vip", msgObj.optLong("vip_time", msgObj.optLong("end_time", 0L)));
                                            }
                                            if (expireTime == 0L && dataObj != null) {
                                                expireTime = dataObj.optLong("vip", dataObj.optLong("vip_time", dataObj.optLong("end_time", 0L)));
                                            }
                                            if (expireTime == 0L) {
                                                expireTime = json.optLong("vip", json.optLong("vip_time", json.optLong("end_time", 0L)));
                                            }
                                            long expireMs = expireTime > 0 ? (expireTime > 100000000000L ? expireTime : expireTime * 1000L) : (System.currentTimeMillis() + 86400000L);
                                            onVerifySuccess(cardKey.trim(), expireMs, callback);
                                            return;
                                        } else {
                                            String msg = extractErrorMessage(json, "微验卡密验证失败");
                                            onVerifyFailed(msg, callback);
                                            return;
                                        }
                                    }
                                } catch (Throwable ignored) {}
                            }
                            try {
                                String host = SiyoXConfig.WeiYanConfig.API_HOST;
                                if (host == null || host.isEmpty()) host = "wy.llua.cn";
                                if (!host.startsWith("http://") && !host.startsWith("https://")) host = "http://" + host;
                                String appId = SiyoXConfig.WeiYanConfig.APP_ID;
                                String appKey = SiyoXConfig.WeiYanConfig.APP_KEY;
                                String rc4Key = SiyoXConfig.WeiYanConfig.RC4_KEY;
                                String apiToken = SiyoXConfig.WeiYanConfig.API_TOKEN;
                                String loginCode = SiyoXConfig.WeiYanConfig.LOGIN_CODE;
                                if (loginCode == null || loginCode.isEmpty()) loginCode = "kmlogon";

                                long now = System.currentTimeMillis() / 1000L;
                                String signSrc = "kami=" + cardKey.trim() + "&markcode=" + androidId + "&t=" + now + "&" + appKey;
                                String signMd5 = md5(signSrc);
                                String plainData = "kami=" + cardKey.trim() + "&markcode=" + androidId + "&t=" + now + "&sign=" + signMd5 + "&v=" + SiyoXConfig.VERSION_CODE + "&value=" + now + (int)(Math.random() * 10000);
                                String dataHex = hexRc4Encrypt(rc4Key, plainData);

                                String url = host + "/api/?id=" + loginCode;
                                String postBody = "app=" + appId + "&data=" + dataHex;

                                byte[] respBytes = httpPostBytes(url, postBody.getBytes("UTF-8"));
                                String respStr = (respBytes != null && respBytes.length > 0) ? new String(respBytes, "UTF-8").trim() : "";
                                if (respStr.contains("\"code\":-1") && !loginCode.equals("kmlogon")) {
                                    url = host + "/api/?id=kmlogon";
                                    respBytes = httpPostBytes(url, postBody.getBytes("UTF-8"));
                                    respStr = (respBytes != null && respBytes.length > 0) ? new String(respBytes, "UTF-8").trim() : "";
                                }
                                if (!respStr.isEmpty()) {
                                    String decStr = respStr;
                                    if (!respStr.startsWith("{")) {
                                        String d = rc4DecryptHexToString(rc4Key, respStr);
                                        if (d != null && !d.isEmpty() && d.startsWith("{")) {
                                            decStr = d;
                                        }
                                    }

                                    JSONObject json = new JSONObject(decStr);
                                    int code = json.optInt("code", -1);
                                    if (code == 200) {
                                        JSONObject dataObj = json.optJSONObject("data");
                                        JSONObject msgObj = json.optJSONObject("msg");
                                        long expireTime = 0L;
                                        if (msgObj != null) {
                                            expireTime = msgObj.optLong("vip", msgObj.optLong("vip_time", msgObj.optLong("end_time", 0L)));
                                        }
                                        if (expireTime == 0L && dataObj != null) {
                                            expireTime = dataObj.optLong("vip", dataObj.optLong("vip_time", dataObj.optLong("end_time", 0L)));
                                        }
                                        if (expireTime == 0L) {
                                            expireTime = json.optLong("vip", json.optLong("vip_time", json.optLong("end_time", 0L)));
                                        }
                                        long expireMs = expireTime > 0 ? (expireTime > 100000000000L ? expireTime : expireTime * 1000L) : (System.currentTimeMillis() + 86400000L);
                                        onVerifySuccess(cardKey.trim(), expireMs, callback);
                                        return;
                                    } else {
                                        String msg = extractErrorMessage(json, "微验卡密验证失败");
                                        onVerifyFailed(msg, callback);
                                        return;
                                    }
                                }
                            } catch (Throwable t) {
                                SiyoXLogger.w("SiyoX_VerifyManager", "WeiYan verification exception: " + t.getMessage());
                            }
                            onVerifyFailed("连接微验服务器失败", callback);
                            break;
                        }

                        case T3: {
                            if (NativeVerify.isNativeLoaded()) {
                                try {
                                    String nativeResp = NativeVerify.nativeT3VerifyCard(cardKey.trim(), androidId);
                                    if (nativeResp != null && !nativeResp.isEmpty()) {
                                        JSONObject json = new JSONObject(nativeResp);
                                        int code = json.optInt("code", -1);
                                        if (code == 200) {
                                            JSONObject data = json.optJSONObject("data");
                                            long expireMs = 0L;
                                            String endTimeStr = json.optString("end_time", "");
                                            if (endTimeStr.isEmpty() && data != null) {
                                                endTimeStr = data.optString("end_time", "");
                                            }
                                            if (!endTimeStr.isEmpty()) {
                                                try {
                                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                                    Date d = sdf.parse(endTimeStr);
                                                    if (d != null) expireMs = d.getTime();
                                                } catch (Throwable ignored) {}
                                            }
                                            if (expireMs == 0L) {
                                                long availSec = json.optLong("available", data != null ? data.optLong("available", 0L) : 0L);
                                                if (availSec > 0) {
                                                    expireMs = System.currentTimeMillis() + availSec * 1000L;
                                                }
                                            }
                                            if (expireMs == 0L) {
                                                expireMs = System.currentTimeMillis() + 86400000L;
                                            }
                                            onVerifySuccess(cardKey.trim(), expireMs, callback);
                                            return;
                                        } else {
                                            String msg = json.optString("msg", "");
                                            if (!msg.isEmpty()) {
                                                onVerifyFailed(msg, callback);
                                                return;
                                            }
                                        }
                                    }
                                } catch (Throwable ignored) {}
                            }

                            String rc4Key = SiyoXConfig.T3Config.RC4_KEY;
                            String appKey = SiyoXConfig.T3Config.APP_KEY;
                            String url = buildT3Url(SiyoXConfig.T3Config.API_HOST, SiyoXConfig.T3Config.LOGIN_CODE);
                            byte[] respBytes;
                            if (rc4Key != null && !rc4Key.isEmpty() && !rc4Key.equals("your_t3_rc4_key")) {
                                long now = System.currentTimeMillis() / 1000L;
                                String kEnc = hexRc4Encrypt(rc4Key, cardKey.trim());
                                String iEnc = hexRc4Encrypt(rc4Key, androidId);
                                String tEnc = hexRc4Encrypt(rc4Key, String.valueOf(now));
                                String sSrc = "kami=" + kEnc + "&imei=" + iEnc + "&t=" + tEnc + "&" + appKey;
                                String sVal = md5(sSrc);
                                String sEnc = hexRc4Encrypt(rc4Key, sVal);
                                String postBody = "kami=" + kEnc + "&imei=" + iEnc + "&t=" + tEnc + "&s=" + sEnc;
                                respBytes = httpPostBytes(url, postBody.getBytes("UTF-8"));
                            } else {
                                String body = "kami=" + URLEncoder.encode(cardKey.trim(), "GBK") + "&imei=" + URLEncoder.encode(androidId, "GBK");
                                respBytes = httpPostBytes(url, body.getBytes("GBK"));
                            }

                            String responseStr;
                            if (rc4Key != null && !rc4Key.isEmpty() && !rc4Key.equals("your_t3_rc4_key")) {
                                responseStr = rc4DecryptBytesToString(rc4Key, respBytes);
                            } else {
                                responseStr = new String(respBytes, "UTF-8");
                                if (responseStr.contains("\uFFFD")) {
                                    responseStr = new String(respBytes, "GBK");
                                }
                            }
                            responseStr = responseStr.trim();
                            SiyoXLogger.i("SiyoX_VerifyManager", "T3 login response: " + responseStr);

                            if (responseStr.startsWith("{")) {
                                JSONObject json = new JSONObject(responseStr);
                                int code = json.optInt("code", -1);
                                if (code == 200) {
                                    JSONObject data = json.optJSONObject("data");
                                    long expireMs = 0L;
                                    String endTimeStr = json.optString("end_time", "");
                                    if (endTimeStr.isEmpty() && data != null) {
                                        endTimeStr = data.optString("end_time", "");
                                    }
                                    if (!endTimeStr.isEmpty()) {
                                        try {
                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                            Date d = sdf.parse(endTimeStr);
                                            if (d != null) expireMs = d.getTime();
                                        } catch (Throwable ignored) {}
                                    }
                                    if (expireMs == 0L) {
                                        long availSec = json.optLong("available", data != null ? data.optLong("available", 0L) : 0L);
                                        if (availSec > 0) {
                                            expireMs = System.currentTimeMillis() + availSec * 1000L;
                                        }
                                    }
                                    if (expireMs == 0L) {
                                        expireMs = System.currentTimeMillis() + 86400000L;
                                    }
                                    onVerifySuccess(cardKey.trim(), expireMs, callback);
                                    return;
                                } else {
                                    String msg = json.optString("msg", "");
                                    onVerifyFailed(msg.isEmpty() ? "T3 验证失败" : msg, callback);
                                    return;
                                }
                            } else if (responseStr.contains("登录成功:200") || responseStr.contains("登录成功")) {
                                long expireMs = 0L;
                                String[] lines = responseStr.split(";");
                                for (String line : lines) {
                                    line = line.trim();
                                    if (line.startsWith("到期时间:")) {
                                        String timeStr = line.substring("到期时间:".length()).trim();
                                        try {
                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                            Date d = sdf.parse(timeStr);
                                            if (d != null) expireMs = d.getTime();
                                        } catch (Throwable ignored) {}
                                    } else if (line.startsWith("剩余时间:")) {
                                        String secStr = line.substring("剩余时间:".length()).trim();
                                        try {
                                            long sec = Long.parseLong(secStr);
                                            if (expireMs == 0L && sec > 0) {
                                                expireMs = System.currentTimeMillis() + sec * 1000L;
                                            }
                                        } catch (Throwable ignored) {}
                                    }
                                }
                                if (expireMs == 0L) {
                                    expireMs = System.currentTimeMillis() + 86400000L;
                                }
                                onVerifySuccess(cardKey.trim(), expireMs, callback);
                            } else {
                                String msg = responseStr.replace(";", "").replace("\r", "").replace("\n", " ").trim();
                                onVerifyFailed(msg.isEmpty() ? "T3 验证失败" : msg, callback);
                            }
                            break;
                        }
                    }

                } catch (Exception e) {
                    onVerifyFailed("连接异常: " + e.getMessage(), callback);
                }
            }
        }).start();
    }

    private String extractErrorMessage(JSONObject json, String defaultMsg) {
        if (json == null) return defaultMsg;
        String msg = json.optString("msg", "");
        if (msg.isEmpty() || msg.equalsIgnoreCase("null")) {
            msg = json.optString("message", "");
        }
        if (msg.isEmpty() || msg.equalsIgnoreCase("null")) {
            msg = json.optString("text", "");
        }
        if (msg.isEmpty() || msg.equalsIgnoreCase("null")) {
            msg = json.optString("info", "");
        }
        if (msg.isEmpty() || msg.equalsIgnoreCase("null")) {
            JSONObject msgObj = json.optJSONObject("msg");
            if (msgObj != null) {
                msg = msgObj.optString("text", msgObj.optString("msg", msgObj.optString("info", "")));
            }
        }
        if (msg.isEmpty() || msg.equalsIgnoreCase("null")) {
            int code = json.optInt("code", -1);
            return defaultMsg + (code != -1 ? " (错误码: " + code + ")" : "");
        }
        return msg.trim();
    }

    private void onVerifySuccess(final String card, final long expireMs, final VerifyCallback callback) {
        final String safeCard = (card == null || card.trim().equalsIgnoreCase("null")) ? "" : card.trim();
        SiyoXLogger.i("SiyoX_VerifyManager", "Card verified successfully, expire: " + formatDate(expireMs));
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                isVerified = true;
                expireTimestamp = expireMs;
                AppSettings.get().setCard(safeCard);
                AppSettings.get().setExpireTime(expireMs);
                statusMessage = "已激活 (到期: " + formatDate(expireMs) + ")";
                if (callback != null) {
                    callback.onResult(true, "验证成功\n到期时间: " + formatDate(expireMs));
                }
            }
        });
    }

    private void onVerifyFailed(final String msg, final VerifyCallback callback) {
        final String displayMsg;
        if (msg == null || msg.trim().isEmpty() || msg.trim().equalsIgnoreCase("null")) {
            displayMsg = "验证失败，请检查卡密或网络状态";
        } else {
            displayMsg = msg.trim();
        }
        SiyoXLogger.w("SiyoX_VerifyManager", "Card verification failed: " + displayMsg);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                isVerified = false;
                statusMessage = "验证失败: " + displayMsg;
                if (callback != null) {
                    callback.onResult(false, displayMsg);
                }
            }
        });
    }

    private static String buildT3Url(String host, String code) {
        if (code == null || code.trim().isEmpty()) return "";
        code = code.trim();
        if (code.startsWith("http://") || code.startsWith("https://")) {
            return code;
        }
        if (host == null || host.trim().isEmpty()) {
            host = "http://w2.t3yanzheng.com";
        }
        return host.endsWith("/") ? host + code : host + "/" + code;
    }

    private byte[] httpPostBytes(String urlStr, byte[] bodyBytes) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10)");

        OutputStream os = conn.getOutputStream();
        os.write(bodyBytes);
        os.flush();
        os.close();

        InputStream is = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
        is.close();
        return baos.toByteArray();
    }

    private byte[] httpGetBytes(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setDoInput(true);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10)");

        InputStream is = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
        is.close();
        return baos.toByteArray();
    }

    private static byte[] rc4Crypt(byte[] key, byte[] data) {
        if (key == null || key.length == 0 || data == null || data.length == 0) return new byte[0];
        int[] s = new int[256];
        for (int i = 0; i < 256; i++) s[i] = i;
        int j = 0;
        for (int i = 0; i < 256; i++) {
            j = (j + s[i] + (key[i % key.length] & 0xFF)) & 0xFF;
            int t = s[i]; s[i] = s[j]; s[j] = t;
        }
        int i = 0; j = 0;
        byte[] out = new byte[data.length];
        for (int k = 0; k < data.length; k++) {
            i = (i + 1) & 0xFF;
            j = (j + s[i]) & 0xFF;
            int t = s[i]; s[i] = s[j]; s[j] = t;
            out[k] = (byte) (data[k] ^ s[(s[i] + s[j]) & 0xFF]);
        }
        return out;
    }

    private static String hexRc4Encrypt(String key, String text) {
        if (key == null || key.isEmpty() || text == null || text.isEmpty()) return "";
        try {
            byte[] keyBytes = key.getBytes("UTF-8");
            byte[] textBytes = text.getBytes("UTF-8");
            byte[] enc = rc4Crypt(keyBytes, textBytes);
            StringBuilder sb = new StringBuilder(enc.length * 2);
            for (byte b : enc) {
                sb.append(String.format("%02x", b & 0xFF));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String rc4DecryptBytesToString(String key, byte[] cipherBytes) {
        if (key == null || key.isEmpty() || cipherBytes == null || cipherBytes.length == 0) return "";
        try {
            byte[] keyBytes = key.getBytes("UTF-8");
            byte[] dec = rc4Crypt(keyBytes, cipherBytes);
            String utf8 = new String(dec, "UTF-8");
            if (!utf8.contains("\uFFFD")) {
                return utf8;
            }
            return new String(dec, "GBK");
        } catch (Exception e) {
            return "";
        }
    }

    private static String rc4DecryptHexToString(String key, String hexStr) {
        if (key == null || key.isEmpty() || hexStr == null || hexStr.length() < 2) return "";
        try {
            hexStr = hexStr.trim();
            int len = hexStr.length();
            if (len % 2 != 0) return "";
            byte[] cipherBytes = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                cipherBytes[i / 2] = (byte) ((Character.digit(hexStr.charAt(i), 16) << 4)
                        + Character.digit(hexStr.charAt(i + 1), 16));
            }
            return rc4DecryptBytesToString(key, cipherBytes);
        } catch (Exception e) {
            return "";
        }
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public void handleEvent(Context context, int eventType, String value) {
        if (value == null || value.trim().isEmpty()) return;
        Context targetCtx = context != null ? context : appContext;
        if (targetCtx == null) return;
        try {
            Intent intent;
            if (eventType == 3) {
                String url = "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=" + value + "&card_type=group&source=qrcode";
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            } else {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(value));
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            targetCtx.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(targetCtx, "无法唤起应用: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static String formatDate(long ms) {
        if (SiyoXConfig.CURRENT_VERIFY_TYPE == SiyoXConfig.VerifyType.NONE || ms == Long.MAX_VALUE) {
            return "永久";
        }
        if (ms <= 0) return "永不到期 / 未激活";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(ms));
    }
}
