// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import XiYue.SiyoX.data.NativeVerify;

/**
 * SiyoX 全局配置桥接类
 * 真实敏感配置全部已安全下沉至独立的 C/C++ 配置文件:
 * 【 app/src/main/cpp/SiyoX_Config.h 】
 * 编译后存入底层 .so 动态库中，防止反编译泄露明文。
 */
public class SiyoXConfig {

    // ==================== 软件框架基本信息 (Java层固定信息) ====================
    public static final String APP_NAME = "SiyoX";
    public static final String PACKAGE_NAME = "XiYue.SiyoX";
    public static final String VERSION_NAME = "v1.0.0";
    public static final int VERSION_CODE = 1;
    public static final String AUTHOR = "@XiYueMax";
    public static final String GITHUB_URL = "https://github.com/XiYueMax/SiyoX";
    public static final String TARGET_PACKAGE = "com.netease.x19";

    // ==================== 客户端自定义配置 (从 C/C++ SiyoX_Config.h 动态加载) ====================
    public static String CLIENT_NAME = "SiyoX Client";
    public static String CLIENT_AUTHOR = "XiYue.";

    // ==================== 材质包 MD5 校验开关 (从 C/C++ SiyoX_Config.h 动态加载) ====================
    public static boolean ENABLE_RESOURCE_MD5_VERIFY = true;

    // ==================== 默认材质资源实体定义 ====================
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

    /**
     * 预置默认资源包列表 (运行时从 C/C++ SiyoX_Config.h 动态构建)
     */
    public static DefaultResource[] DEFAULT_RESOURCES = new DefaultResource[0];

    // ==================== 网络验证类型 ====================
    public enum VerifyType {
        EPIC,    // 摇光云验证
        T3,      // T3网络验证
        WEIYAN   // 微验网络验证
    }

    public static VerifyType CURRENT_VERIFY_TYPE = VerifyType.EPIC;

    // ==================== 1. EPIC (摇光云) 配置 (从 C/C++ SiyoX_Config.h 动态加载) ====================
    public static class EpicConfig {
        public static String APP_KEY = "iJQfzsjaI5IHW7W6VKjDXmF7gMxpSy0s";
        public static String[] HOSTS = new String[]{
                "epic.z74d.top",
                "gl.t60.top",
                "test.t60.top",
                "epic.t5x.cc"
        };
        public static int PORT = 5000;
    }

    // ==================== 2. T3 网络验证配置 ====================
    public static class T3Config {
        public static String API_HOST = "https://api.t3yanzheng.com";
        public static String APP_KEY = "your_t3_app_key";
        public static String LOGIN_CODE = "your_t3_login_code";
        public static String NOTICE_CODE = "your_t3_notice_code";
        public static String VERSION_CODE_STR = "your_t3_version_code";
        public static String HEARTBEAT_CODE = "your_t3_heartbeat_code";
        public static String RSA_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----";
    }

    // ==================== 3. 微验 (WeiYan) 网络验证配置 ====================
    public static class WeiYanConfig {
        public static String API_HOST = "wy.llua.cn";
        public static String APP_ID = "your_weiyan_app_id";
        public static String APP_KEY = "your_weiyan_app_key";
        public static String RC4_KEY = "your_weiyan_rc4_key";
    }

    // ==================== 静态初始化：从 Native C/C++ 动态注入所有配置 ====================
    static {
        loadNativeConfig();
    }

    public static void loadNativeConfig() {
        if (!NativeVerify.isNativeLoaded()) {
            return;
        }
        try {
            // 1. 客户端展示信息
            String nativeClientName = NativeVerify.nativeGetClientName();
            if (nativeClientName != null && !nativeClientName.isEmpty()) {
                CLIENT_NAME = nativeClientName;
            }
            String nativeClientAuthor = NativeVerify.nativeGetClientAuthor();
            if (nativeClientAuthor != null && !nativeClientAuthor.isEmpty()) {
                CLIENT_AUTHOR = nativeClientAuthor;
            }

            // 2. MD5 校验开关
            ENABLE_RESOURCE_MD5_VERIFY = NativeVerify.nativeGetEnableMd5Verify();

            // 3. EPIC 验证配置
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

            // 4. T3 验证配置
            String t3Json = NativeVerify.nativeGetT3ConfigJson();
            if (t3Json != null && !t3Json.isEmpty()) {
                JSONObject obj = new JSONObject(t3Json);
                T3Config.API_HOST = obj.optString("apiHost", T3Config.API_HOST);
                T3Config.APP_KEY = obj.optString("appKey", T3Config.APP_KEY);
                T3Config.LOGIN_CODE = obj.optString("loginCode", T3Config.LOGIN_CODE);
                T3Config.NOTICE_CODE = obj.optString("noticeCode", T3Config.NOTICE_CODE);
                T3Config.VERSION_CODE_STR = obj.optString("versionCode", T3Config.VERSION_CODE_STR);
                T3Config.HEARTBEAT_CODE = obj.optString("heartbeatCode", T3Config.HEARTBEAT_CODE);
                T3Config.RSA_PUBLIC_KEY = obj.optString("rsaPublicKey", T3Config.RSA_PUBLIC_KEY);
            }

            // 5. 微验配置
            String wyJson = NativeVerify.nativeGetWeiYanConfigJson();
            if (wyJson != null && !wyJson.isEmpty()) {
                JSONObject obj = new JSONObject(wyJson);
                WeiYanConfig.API_HOST = obj.optString("apiHost", WeiYanConfig.API_HOST);
                WeiYanConfig.APP_ID = obj.optString("appId", WeiYanConfig.APP_ID);
                WeiYanConfig.APP_KEY = obj.optString("appKey", WeiYanConfig.APP_KEY);
                WeiYanConfig.RC4_KEY = obj.optString("rc4Key", WeiYanConfig.RC4_KEY);
            }

            // 6. 默认资源包列表 (从 C++ 读取并解析)
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
