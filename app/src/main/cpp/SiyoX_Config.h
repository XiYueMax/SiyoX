// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

#ifndef SIYOX_CONFIG_H
#define SIYOX_CONFIG_H

#include <stdbool.h>

/**
 * ============================================================================
 *                    SiyoX 核心自定义与安全配置文件
 * ============================================================================
 */

// ==================== 客户端展示信息 ====================
#define SIYOX_CLIENT_NAME        "SiyoX Client"
#define SIYOX_CLIENT_AUTHOR      "XiYue."

// ==================== 材质包 MD5 完整性校验开关 ====================
// true: 开启校验（下载/注入时自动校验 32 位 MD5，防止材质包被篡改或损坏）
// false: 关闭校验（直接注入）
// 提示：如果您的直链资源包需要经常在线更新或替换内容，不建议开启 MD5 校验（避免频繁修改 MD5 配置）
#define SIYOX_ENABLE_MD5_VERIFY  true

// ==================== 默认资源包配置 ====================
typedef struct {
    const char *name;
    const char *url;
    const char *md5;
    const char *description;
} SiyoXDefaultResource;

/**
 * 预置默认资源包列表 (可在下方按格式增删材质包)
 * 格式：{ "资源包名称", "直链下载地址", "资源包MD5值[32位小写字母]", "资源包简介" }
 */
static const SiyoXDefaultResource SIYOX_DEFAULT_RESOURCES[] = {
    {
        "默认资源包1",
        "https://example.com/example2.zip",
        "597776459862b5c52a2a7db89b933b0d",
        "资源包介绍1"
    },
    {
        "默认资源包2",
        "https://example.com/example2.zip",
        "ec51c3940f73dccd7464cfe462d9046d",
        "资源包介绍2"
    }
};

#define SIYOX_DEFAULT_RESOURCES_COUNT (sizeof(SIYOX_DEFAULT_RESOURCES) / sizeof(SIYOX_DEFAULT_RESOURCES[0]))

// ==================== EPIC (摇光云) 网络验证配置 ====================
#define SIYOX_EPIC_APP_KEY       "your_epic_app_key"
#define SIYOX_EPIC_PORT          5000

static const char* SIYOX_EPIC_HOSTS[] = {
    "epic.z74d.top",
    "gl.t60.top",
    "test.t60.top",
    "epic.t5x.cc"
};
#define SIYOX_EPIC_HOSTS_COUNT (sizeof(SIYOX_EPIC_HOSTS) / sizeof(SIYOX_EPIC_HOSTS[0]))

// ==================== T3 网络验证配置 ====================
#define SIYOX_T3_API_HOST        "https://api.t3yanzheng.com"
#define SIYOX_T3_APP_KEY         "your_t3_app_key"
#define SIYOX_T3_LOGIN_CODE      "your_t3_login_code"
#define SIYOX_T3_NOTICE_CODE     "your_t3_notice_code"
#define SIYOX_T3_VERSION_CODE    "your_t3_version_code"
#define SIYOX_T3_HEARTBEAT_CODE  "your_t3_heartbeat_code"
#define SIYOX_T3_RSA_PUBLIC_KEY  "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----"

// ==================== 微验 (WeiYan) 网络验证配置 ====================
#define SIYOX_WEIYAN_API_HOST    "wy.llua.cn"
#define SIYOX_WEIYAN_APP_ID      "your_weiyan_app_id"
#define SIYOX_WEIYAN_APP_KEY     "your_weiyan_app_key"
#define SIYOX_WEIYAN_RC4_KEY     "your_weiyan_rc4_key"

#endif // SIYOX_CONFIG_H
