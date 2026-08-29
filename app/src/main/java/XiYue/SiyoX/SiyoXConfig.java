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

    // ==================== 默认资源配置 ====================
    public static class DefaultResource {
        public final String name;
        public final String url;
        public final String fileName;
        public final String description;

        public DefaultResource(String name, String url, String fileName, String description) {
            this.name = name;
            this.url = url;
            this.fileName = fileName;
            this.description = description;
        }
    }

    /**
     * 开发者预置默认资源包列表 (在面板“资源列表 -> 默认资源”中展示)
     * 可在下方自由添加或修改预置材质包名称与直链下载地址
     */
    public static final DefaultResource[] DEFAULT_RESOURCES = new DefaultResource[]{
            new DefaultResource(
                    "SiyoX 官方专属优化材质包",
                    "https://example.com/res/siyox_default_texture.zip",
                    "siyox_default_texture.zip",
                    "官方定制超清纹理优化包，深度优化游戏加载与材质表现"
            ),
            new DefaultResource(
                    "PVP 极速流畅材质包",
                    "https://example.com/res/siyox_pvp_texture.zip",
                    "siyox_pvp_texture.zip",
                    "极致低延迟低粒子渲染，专为竞技与对战定制"
            )
    };

    public enum VerifyType {
        EPIC,    // 摇光云验证
        T3,      // T3网络验证
        WEIYAN   // 微验网络验证
    }

    /**
     *   网络验证提供商列表
     * - VerifyType.EPIC   : 摇光云验证
     * - VerifyType.T3     : T3网络验证
     * - VerifyType.WEIYAN : 微验网络验证
     */
    public static VerifyType CURRENT_VERIFY_TYPE = VerifyType.EPIC; //在等号后修改网络验证提供商 [例如你要用T3，那就把等号后面的VerifyType.EPIC换成VerifyType.T3]

    // ==================== 1. EPIC (摇光云) 配置 ====================
    public static class EpicConfig {
        public static final String APP_KEY = "your_epic_app_key";
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
        public static final String APP_ID = "your_weiyan_app_id";
        public static final String APP_KEY = "your_weiyan_app_key";
        public static final String RC4_KEY = "your_weiyan_rc4_key";
    }
}
