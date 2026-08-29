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

    public String getActiveProviderName() {
        switch (SiyoXConfig.CURRENT_VERIFY_TYPE) {
            case T3:
                return "T3 网络验证 (C/Java)";
            case WEIYAN:
                return "微验验证 (C/Java)";
            case EPIC:
            default:
                return "摇光云 (EPIC)";
        }
    }

    @SuppressLint("HardwareIds")
    public String getAndroidId() {
        try {
            String id = Settings.Secure.getString(appContext.getContentResolver(), Settings.Secure.ANDROID_ID);
            return id != null ? id : "unknown_android_id";
        } catch (Exception e) {
            return "unknown_android_id";
        }
    }

    public void loadSoftwareNotice(final NoticeCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int typeCode = SiyoXConfig.CURRENT_VERIFY_TYPE.ordinal();

                    // Try Native C first
                    if (NativeVerify.isNativeLoaded()) {
                        try {
                            String nativeResp = NativeVerify.nativeFetchNotice(typeCode);
                            if (nativeResp != null && !nativeResp.isEmpty()) {
                                JSONObject json = new JSONObject(nativeResp);
                                int code = json.optInt("code", -1);
                                if (code == 200) {
                                    JSONObject msgObj = json.optJSONObject("msg");
                                    if (msgObj != null) {
                                        noticeTitle = msgObj.optString("title", "官方公告");
                                        noticeContent = msgObj.optString("content", msgObj.optString("app_gg", "欢迎使用 SiyoX 模块！"));
                                    } else {
                                        noticeContent = json.optString("msg", "欢迎使用 SiyoX 模块！");
                                    }
                                    notifyNoticeResult(callback, true, noticeTitle, noticeContent);
                                    return;
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    // Java Fallback for EPIC
                    if (SiyoXConfig.CURRENT_VERIFY_TYPE == SiyoXConfig.VerifyType.EPIC) {
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
                                noticeTitle = notice.getTitle() != null ? notice.getTitle() : "官方公告";
                                noticeContent = notice.getContent() != null ? notice.getContent() : "欢迎使用 SiyoX 模块！";
                                notifyNoticeResult(callback, true, noticeTitle, noticeContent);
                                return;
                            }
                        }
                    }

                    notifyNoticeResult(callback, true, noticeTitle, noticeContent);

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
                    int typeCode = SiyoXConfig.CURRENT_VERIFY_TYPE.ordinal();

                    // 1. Try Native C Verification
                    if (NativeVerify.isNativeLoaded()) {
                        try {
                            String nativeResp = NativeVerify.nativeVerifyCard(typeCode, cardKey.trim(), androidId);
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
                                    String msg = json.optString("msg", "验证失败");
                                    onVerifyFailed(msg, callback);
                                    return;
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    // 2. Java Fallback for EPIC
                    if (SiyoXConfig.CURRENT_VERIFY_TYPE == SiyoXConfig.VerifyType.EPIC) {
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
                                if (resp.getData() != null) {
                                    expire = resp.getData().getLong("expire");
                                }
                            } catch (Exception ignored) {}
                            onVerifySuccess(cardKey.trim(), expire, callback);
                        } else {
                            onVerifyFailed(resp.getMsg() != null ? resp.getMsg() : "验证失败", callback);
                        }
                        return;
                    }

                    onVerifyFailed("网络验证服务未就绪", callback);

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
