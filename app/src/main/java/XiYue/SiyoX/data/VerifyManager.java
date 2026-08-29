// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
    private String noticeTitle = "官方公告";
    private String noticeContent = "欢迎使用 SiyoX 模块！请输入授权卡密激活后开始体验。";

    private static volatile VerifyManager instance;

    private VerifyManager(Context context) {
        this.appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
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
        isVerified = false;
        expireTimestamp = 0L;
        statusMessage = "未验证";
        AppSettings.get().setCard("");
        AppSettings.get().setExpireTime(0L);
    }


    public String getActiveProviderName() {
        switch (SiyoXConfig.CURRENT_VERIFY_TYPE) {
            case T3:
                return "T3 网络验证 (C/Java)";
            case WEIYAN:
                return "微验验证 (Native C)";
            case EPIC:
            default:
                return "摇光云 (EPIC)";
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
                                String nativeResp = NativeVerify.nativeFetchNotice(2);
                                if (nativeResp != null && !nativeResp.isEmpty()) {
                                    JSONObject json = new JSONObject(nativeResp);
                                    int code = json.optInt("code", -1);
                                    if (code == 200) {
                                        JSONObject msgObj = json.optJSONObject("msg");
                                        if (msgObj != null) {
                                            noticeTitle = "微验官方公告";
                                            noticeContent = msgObj.optString("app_gg", "欢迎使用 SiyoX 模块！");
                                        }
                                        notifyNoticeResult(callback, true, noticeTitle, noticeContent);
                                        return;
                                    }
                                }
                            }
                            notifyNoticeResult(callback, true, "微验官方公告", "欢迎使用 SiyoX 模块！请输入授权卡密激活后开始体验。");
                            break;
                        }

                        case T3: {
                            notifyNoticeResult(callback, true, "T3 官方公告", "欢迎使用 SiyoX 模块！请输入授权卡密激活后开始体验。");
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

    public void verifyCard(final String cardKey, final VerifyCallback callback) {
        if (cardKey == null || cardKey.trim().isEmpty()) {
            if (callback != null) callback.onResult(false, "卡密不能为空");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String androidId = getAndroidId();

                    switch (SiyoXConfig.CURRENT_VERIFY_TYPE) {
                        case EPIC: {
                            // 摇光云 (EPIC) 验证
                            EpicVerifySDK sdk = new EpicVerifySDK(
                                    SiyoXConfig.EpicConfig.HOSTS,
                                    SiyoXConfig.EpicConfig.PORT,
                                    SiyoXConfig.EpicConfig.APP_KEY
                            );
                            sdk.setDeviceId(androidId);
                            sdk.setPackageName(SiyoXConfig.TARGET_PACKAGE);
                            sdk.setCard(cardKey.trim());

                            Resp resp = sdk.cardVerify();
                            if (resp.isSuccess()) {
                                long expire = 0L;
                                try {
                                    if (resp.data != null) {
                                        expire = resp.data.getLong("expire");
                                    }
                                } catch (Exception ignored) {}
                                onVerifySuccess(cardKey.trim(), expire, callback);
                            } else {
                                onVerifyFailed(resp.msg != null ? resp.msg : "验证失败", callback);
                            }
                            break;
                        }

                        case WEIYAN: {
                            // 微验 (Native C) 验证
                            if (NativeVerify.isNativeLoaded()) {
                                String nativeResp = NativeVerify.nativeVerifyCard(2, cardKey.trim(), androidId);
                                if (nativeResp != null && !nativeResp.isEmpty()) {
                                    JSONObject json = new JSONObject(nativeResp);
                                    int code = json.optInt("code", -1);
                                    if (code == 200) {
                                        JSONObject msgObj = json.optJSONObject("msg");
                                        long vip = msgObj != null ? msgObj.optLong("vip", 0L) : 0L;
                                        long expireMs = vip > 0 ? vip * 1000L : System.currentTimeMillis() + 86400000L * 30;
                                        onVerifySuccess(cardKey.trim(), expireMs, callback);
                                        return;
                                    } else {
                                        String msg = json.optString("msg", "微验卡密验证失败");
                                        onVerifyFailed(msg, callback);
                                        return;
                                    }
                                }
                            }
                            onVerifyFailed("Native C 微验模块加载失败", callback);
                            break;
                        }

                        case T3: {
                            // T3 网络验证
                            String host = SiyoXConfig.T3Config.API_HOST;
                            String appKey = SiyoXConfig.T3Config.APP_KEY;
                            String loginCode = SiyoXConfig.T3Config.LOGIN_CODE;
                            String time = String.valueOf(System.currentTimeMillis() / 1000);
                            String sign = md5("appkey=" + appKey + "&card=" + cardKey.trim() + "&imei=" + androidId + "&t=" + time).toLowerCase();

                            String url = host.endsWith("/") ? host + "api/login" : host + "/api/login";
                            String body = "appkey=" + appKey + "&code=" + loginCode + "&card=" + cardKey.trim() + "&imei=" + androidId + "&t=" + time + "&sign=" + sign;

                            String responseStr = httpPost(url, body);
                            JSONObject json = new JSONObject(responseStr);
                            int code = json.optInt("code", json.optInt("status", -1));
                            String msg = json.optString("msg", json.optString("message", ""));

                            if (code == 200 || code == 1 || json.optBoolean("success", false)) {
                                JSONObject data = json.optJSONObject("data");
                                long endTime = data != null ? data.optLong("end_time", 0L) : 0L;
                                onVerifySuccess(cardKey.trim(), endTime * 1000L, callback);
                            } else {
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

    private void onVerifySuccess(final String card, final long expireMs, final VerifyCallback callback) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                isVerified = true;
                expireTimestamp = expireMs;
                AppSettings.get().setCard(card);
                AppSettings.get().setExpireTime(expireMs);
                statusMessage = "已激活 (到期: " + formatDate(expireMs) + ")";
                if (callback != null) {
                    callback.onResult(true, "验证成功\n到期时间: " + formatDate(expireMs));
                }
            }
        });
    }

    private void onVerifyFailed(final String msg, final VerifyCallback callback) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                isVerified = false;
                statusMessage = "验证失败: " + msg;
                if (callback != null) {
                    callback.onResult(false, msg);
                }
            }
        });
    }

    private String httpPost(String urlStr, String body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("User-Agent", "SiyoX/1.0");

        OutputStream os = conn.getOutputStream();
        os.write(body.getBytes("UTF-8"));
        os.flush();
        os.close();

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString().trim();
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
        try {
            Intent intent;
            if (eventType == 3) {
                // QQ Group
                String url = "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=" + value + "&card_type=group&source=qrcode";
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            } else {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(value));
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "无法唤起应用: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static String formatDate(long ms) {
        if (ms <= 0) return "永不到期 / 未激活";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(ms));
    }
}
