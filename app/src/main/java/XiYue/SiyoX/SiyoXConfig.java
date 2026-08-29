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
    /**
     * 是否开启默认资源包 MD5 完整性校验
     * - true: 开启校验（下载/注入时自动校验文件 MD5，防止文件损坏或被篡改）
     * - false: 关闭校验（不比对 MD5，直接注入）
     */
    public static boolean ENABLE_RESOURCE_MD5_VERIFY = true;

    public static class DefaultResource {
        public final String name;
        public final String url;
        public final String md5; // 资源包 MD5 校验码 (32位小写Hex，填空字符串 "" 或在上面关闭开关则不校验)
        public final String description;

        public DefaultResource(String name, String url, String md5, String description) {
            this.name = name;
            this.url = url;
            this.md5 = md5;
            this.description = description;
        }

        /**
         * 自动从 URL 提取或通过 MD5/名称生成本地缓存文件名
         */
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
     * 开发者预置默认资源包列表 (在面板“资源列表 -> 默认资源”中展示)
     * 参数格式：new DefaultResource("资源包名称", "直链下载地址", "资源包MD5值", "资源包描述")
     */
    public static final DefaultResource[] DEFAULT_RESOURCES = new DefaultResource[]{
            new DefaultResource(
                    "SiyoX 官方专属优化材质包",
                    "https://example.com/res/siyox_default_texture.zip",
                    "a1b2c3d4e5f67890123456789abcdef0",
                    "官方定制超清纹理优化包，深度优化游戏加载与材质表现"
            ),
            new DefaultResource(
                    "PVP 极速流畅材质包",
                    "https://example.com/res/siyox_pvp_texture.zip",
                    "0fedcba9876543210987654f3e2d1cba",
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
