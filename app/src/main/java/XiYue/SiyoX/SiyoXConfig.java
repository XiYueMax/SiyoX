

package XiYue.SiyoX;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import XiYue.SiyoX.data.NativeVerify;

public class SiyoXConfig {

    public static final String APP_NAME = "SiyoX";
    public static final String PACKAGE_NAME = "XiYue.SiyoX";
    public static final String VERSION_NAME = "v1.0.0";
    public static final int VERSION_CODE = 1;
    public static final String AUTHOR = "@XiYueMax";
    public static final String GITHUB_URL = "https://github.com/XiYueMax/SiyoX";
    public static final String TARGET_PACKAGE = "com.netease.x19";

    public static String CLIENT_NAME = "";
    public static String CLIENT_AUTHOR = "";
    public static String DEFAULT_NOTICE_TITLE = "官方公告";
    public static String DEFAULT_NOTICE_CONTENT = "欢迎使用 SiyoX 模块！请输入授权卡密激活后开始体验。";
    public static boolean ENABLE_RESOURCE_MD5_VERIFY = true;

    public static class DefaultResource {
        public final String name;
        public final String url;
        public final String md5;
        public final String description;

        public DefaultResource(String name, String url, String md5, String description) {
            this.name = name;
            this.url = url;
            this.md5 = md5;
            this.description = description;
        }

        public String getFileName() {
            try {
                if (url != null && url.contains("/")) {
                    String sub = url.substring(url.lastIndexOf('/') + 1);
                    if (sub.contains("?")) {
                        sub = sub.substring(0, sub.indexOf('?'));
                    }
                    if (sub.toLowerCase().endsWith(".zip")) {
                        return sub;
                    }
                }
            } catch (Throwable ignored) {}
            return (md5 != null && !md5.trim().isEmpty() ? md5.trim().toLowerCase() : "res_" + Math.abs(name.hashCode())) + ".zip";
        }
    }

    public static DefaultResource[] DEFAULT_RESOURCES = new DefaultResource[0];

    public enum VerifyType {
        EPIC,
        T3,
        WEIYAN
    }

    public static VerifyType CURRENT_VERIFY_TYPE = VerifyType.EPIC;

    public static class EpicConfig {
        public static String APP_KEY = "";
        public static String[] HOSTS = new String[0];
        public static int PORT = 5000;
    }

    public static class T3Config {
        public static String API_HOST = "";
        public static String APP_KEY = "";
        public static String RC4_KEY = "";
        public static String LOGIN_CODE = "";
        public static String NOTICE_CODE = "";
        public static String VERSION_CODE_STR = "";
        public static String HEARTBEAT_CODE = "";
    }

    public static class WeiYanConfig {
        public static String API_HOST = "wy.llua.cn";
        public static String APP_ID = "";
        public static String APP_KEY = "";
        public static String RC4_KEY = "";
        public static String API_TOKEN = "";
        public static String LOGIN_CODE = "kmlogon";
        public static String NOTICE_CODE = "notice";
        public static String UPDATE_CODE = "checkupdate";
    }

    static {
        loadNativeConfig();
    }

    public static void loadNativeConfig() {
        if (!NativeVerify.isNativeLoaded()) {
            return;
        }
        try {
            int nativeType = NativeVerify.nativeGetActiveVerifyType();
            if (nativeType == 1) {
                CURRENT_VERIFY_TYPE = VerifyType.T3;
            } else if (nativeType == 2) {
                CURRENT_VERIFY_TYPE = VerifyType.WEIYAN;
            } else {
                CURRENT_VERIFY_TYPE = VerifyType.EPIC;
            }

            String nativeClientName = NativeVerify.nativeGetClientName();
            if (nativeClientName != null && !nativeClientName.isEmpty()) {
                CLIENT_NAME = nativeClientName;
            }
            String nativeClientAuthor = NativeVerify.nativeGetClientAuthor();
            if (nativeClientAuthor != null && !nativeClientAuthor.isEmpty()) {
                CLIENT_AUTHOR = nativeClientAuthor;
            }

            String nativeNoticeTitle = NativeVerify.nativeGetDefaultNoticeTitle();
            if (nativeNoticeTitle != null && !nativeNoticeTitle.isEmpty()) {
                DEFAULT_NOTICE_TITLE = nativeNoticeTitle;
            }
            String nativeNoticeContent = NativeVerify.nativeGetDefaultNoticeContent();
            if (nativeNoticeContent != null && !nativeNoticeContent.isEmpty()) {
                DEFAULT_NOTICE_CONTENT = nativeNoticeContent;
            }

            ENABLE_RESOURCE_MD5_VERIFY = NativeVerify.nativeGetEnableMd5Verify();

            String nativeEpicAppKey = NativeVerify.nativeGetEpicAppKey();
            if (nativeEpicAppKey != null && !nativeEpicAppKey.isEmpty()) {
                EpicConfig.APP_KEY = nativeEpicAppKey;
            }
            int nativeEpicPort = NativeVerify.nativeGetEpicPort();
            if (nativeEpicPort > 0) {
                EpicConfig.PORT = nativeEpicPort;
            }
            String[] nativeEpicHosts = NativeVerify.nativeGetEpicHosts();
            if (nativeEpicHosts != null && nativeEpicHosts.length > 0) {
                EpicConfig.HOSTS = nativeEpicHosts;
            }

            String t3Json = NativeVerify.nativeGetT3ConfigJson();
            if (t3Json != null && !t3Json.isEmpty()) {
                JSONObject obj = new JSONObject(t3Json);
                T3Config.API_HOST = obj.optString("apiHost", T3Config.API_HOST);
                T3Config.APP_KEY = obj.optString("appKey", T3Config.APP_KEY);
                T3Config.RC4_KEY = obj.optString("rc4Key", T3Config.RC4_KEY);
                T3Config.LOGIN_CODE = obj.optString("loginCode", T3Config.LOGIN_CODE);
                T3Config.NOTICE_CODE = obj.optString("noticeCode", T3Config.NOTICE_CODE);
                T3Config.VERSION_CODE_STR = obj.optString("versionCode", T3Config.VERSION_CODE_STR);
                T3Config.HEARTBEAT_CODE = obj.optString("heartbeatCode", T3Config.HEARTBEAT_CODE);
            }

            String wyJson = NativeVerify.nativeGetWeiYanConfigJson();
            if (wyJson != null && !wyJson.isEmpty()) {
                JSONObject obj = new JSONObject(wyJson);
                WeiYanConfig.API_HOST = obj.optString("apiHost", WeiYanConfig.API_HOST);
                WeiYanConfig.APP_ID = obj.optString("appId", WeiYanConfig.APP_ID);
                WeiYanConfig.APP_KEY = obj.optString("appKey", WeiYanConfig.APP_KEY);
                WeiYanConfig.RC4_KEY = obj.optString("rc4Key", WeiYanConfig.RC4_KEY);
                WeiYanConfig.API_TOKEN = obj.optString("apiToken", WeiYanConfig.API_TOKEN);
                WeiYanConfig.LOGIN_CODE = obj.optString("loginCode", WeiYanConfig.LOGIN_CODE);
                WeiYanConfig.NOTICE_CODE = obj.optString("noticeCode", WeiYanConfig.NOTICE_CODE);
                WeiYanConfig.UPDATE_CODE = obj.optString("updateCode", WeiYanConfig.UPDATE_CODE);
            }

            String resJson = NativeVerify.nativeGetDefaultResourcesJson();
            if (resJson != null && !resJson.isEmpty()) {
                JSONArray arr = new JSONArray(resJson);
                List<DefaultResource> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.getJSONObject(i);
                    list.add(new DefaultResource(
                            item.optString("name", ""),
                            item.optString("url", ""),
                            item.optString("md5", ""),
                            item.optString("description", "")
                    ));
                }
                DEFAULT_RESOURCES = list.toArray(new DefaultResource[0]);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
