// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

#ifndef SIYOX_CONFIG_H
#define SIYOX_CONFIG_H

#include <stdbool.h>

/**
 * ============================================================================
 *                    SiyoX 核心自定义与安全配置文件 (C/C++)
 * ============================================================================
 * 所有可自定义的配置（客户端信息、各类网络验证密钥、默认材质资源列表等）
 * 均在此单个独立文件中集中管理，开发者可在此自由修改与添加。
 * 编译后自动沉淀至动态链接库 (.so) 中，防止被反编译直接提取明文字符串。
 * ============================================================================
 */

// ==================== 1. 客户端展示信息 ====================
#define SIYOX_CLIENT_NAME        "SiyoX Client"
#define SIYOX_CLIENT_AUTHOR      "XiYue."

// ==================== 2. 材质包 MD5 完整性校验开关 ====================
// true: 开启校验（下载/注入时自动校验 32 位 MD5，防止材质包被篡改或损坏）
// false: 关闭校验（直接注入）
#define SIYOX_ENABLE_MD5_VERIFY  true

// ==================== 3. 预置默认资源包配置 ====================
typedef struct {
    const char *name;        // 资源包名称
    const char *url;         // 直链下载地址 (必须是直接可下载的直链)
    const char *md5;         // 32 位小写 MD5 校验和 (若未开启校验可填 "")
    const char *description; // 资源包简介与描述
} SiyoXDefaultResource;

/**
 * 预置默认资源包列表 (可在下方按格式增删材质包)
 * 格式：{ "资源包名称", "直链下载地址", "资源包MD5值", "资源包简介" }
 */
static const SiyoXDefaultResource SIYOX_DEFAULT_RESOURCES[] = {
    {
        "XingYueClient PVP 材质包",
        "https://1833946539.cdn.123clouddisk.com/1833946539/Resource/XingYueClientPVP.zip",
        "4fc67cf64c5affce96309c2035a62af5",
        "星月专属 PVP 定制材质包，已配置 MD5 完整性校验"
    },
    {
        "SiyoX 官方专属优化材质包",
        "https://example.com/res/siyox_default_texture.zip",
        "597776459862b5c52a2a7db89b933b0d",
        "官方定制超清纹理优化包，深度优化游戏加载与材质表现"
    }
};

#define SIYOX_DEFAULT_RESOURCES_COUNT (sizeof(SIYOX_DEFAULT_RESOURCES) / sizeof(SIYOX_DEFAULT_RESOURCES[0]))

// ==================== 4. EPIC (摇光云) 网络验证配置 ====================
#define SIYOX_EPIC_APP_KEY       "iJQfzsjaI5IHW7W6VKjDXmF7gMxpSy0s"
#define SIYOX_EPIC_PORT          5000

static const char* SIYOX_EPIC_HOSTS[] = {
    "epic.z74d.top",
    "gl.t60.top",
    "test.t60.top",
    "epic.t5x.cc"
};
#define SIYOX_EPIC_HOSTS_COUNT (sizeof(SIYOX_EPIC_HOSTS) / sizeof(SIYOX_EPIC_HOSTS[0]))

// ==================== 5. T3 网络验证配置 ====================
#define SIYOX_T3_API_HOST        "https://api.t3yanzheng.com"
#define SIYOX_T3_APP_KEY         "your_t3_app_key"
#define SIYOX_T3_LOGIN_CODE      "your_t3_login_code"
#define SIYOX_T3_NOTICE_CODE     "your_t3_notice_code"
#define SIYOX_T3_VERSION_CODE    "your_t3_version_code"
#define SIYOX_T3_HEARTBEAT_CODE  "your_t3_heartbeat_code"
#define SIYOX_T3_RSA_PUBLIC_KEY  "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----"

// ==================== 6. 微验 (WeiYan) 网络验证配置 ====================
#define SIYOX_WEIYAN_API_HOST    "wy.llua.cn"
#define SIYOX_WEIYAN_APP_ID      "your_weiyan_app_id"
#define SIYOX_WEIYAN_APP_KEY     "your_weiyan_app_key"
#define SIYOX_WEIYAN_RC4_KEY     "your_weiyan_rc4_key"

#endif // SIYOX_CONFIG_H