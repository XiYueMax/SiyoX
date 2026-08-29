// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX;

/**
 * SiyoX 全局自定义配置文件
 * 支持自定义软件信息、各家网络验证参数与快速切换验证方式
 */
public class SiyoXConfig {

    // ==================== 客户端自定义配置 ====================
    public static final String CLIENT_NAME = "SiyoX Client";
    public static final String CLIENT_AUTHOR = "XiYue.";

    // ==================== 软件基本信息 ====================
    public static final String APP_NAME = "SiyoX";
    public static final String PACKAGE_NAME = "XiYue.SiyoX";
    public static final String VERSION_NAME = "v1.0.0";
    public static final int VERSION_CODE = 1;
    public static final String AUTHOR = "@XiYueMax";
    public static final String GITHUB_URL = "https://github.com/XiYueMax/SiyoX";
    public static final String TARGET_PACKAGE = "com.netease.x19";



    // ==================== 网络验证类型 ====================
    public enum VerifyType {
        EPIC,    // 摇光云验证
        T3,      // T3网络验证
        WEIYAN   // 微验网络验证
    }

    /**
     * ★ 当前使用的网络验证提供商（在此切换）：
     * - VerifyType.EPIC   : 摇光云验证
     * - VerifyType.T3     : T3网络验证
     * - VerifyType.WEIYAN : 微验网络验证
     */
    public static VerifyType CURRENT_VERIFY_TYPE = VerifyType.EPIC;

    // ==================== 1. EPIC (摇光云) 配置 ====================
    public static class EpicConfig {
        public static final String APP_KEY = "iJQfzsjaI5IHW7W6VKjDXmF7gMxpSy0s";
        public static final String[] HOSTS = new String[]{
                "epic.z74d.top",
                "gl.t60.top",
                "test.t60.top",
                "epic.t5x.cc"
        };
        public static final int PORT = 5000;
    }

    // ==================== 2. T3 网络验证配置 ====================
    public static class T3Config {
        public static final String API_HOST = "https://api.t3yanzheng.com";
        public static final String APP_KEY = "your_t3_app_key";
        public static final String LOGIN_CODE = "your_t3_login_code";
        public static final String NOTICE_CODE = "your_t3_notice_code";
        public static final String VERSION_CODE_STR = "your_t3_version_code";
        public static final String HEARTBEAT_CODE = "your_t3_heartbeat_code";
        public static final String RSA_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----";
    }

    // ==================== 3. 微验 (WeiYan) 网络验证配置 ====================
    public static class WeiYanConfig {
        public static final String API_HOST = "wy.llua.cn";
        public static final String APP_ID = "10000";
        public static final String APP_KEY = "8LjdoLopmH9LyLVh";
        public static final String RC4_KEY = "ElFlF870vDk88gef";
    }
}
