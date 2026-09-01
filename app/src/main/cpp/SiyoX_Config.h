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

// ==================== 验证模式选择 ====================
// 0: 关闭验证 (免验证模式), 1: EPIC (摇光云), 2: T3 验证, 3: 微验
#define SIYOX_ACTIVE_VERIFY_TYPE  0

// ==================== 客户端展示信息 ====================
#define SIYOX_CLIENT_NAME        "SiyoX Client"  //客户端名称
#define SIYOX_CLIENT_AUTHOR      "XiYue."        //客户端作者

// ==================== 版本信息配置 ====================
// 内部版本号（必须为整数，网络验证版本比对与更新检测时使用）
#define SIYOX_VERSION_CODE       2

// ==================== 默认官方公告配置 ====================
// 当网络验证未配置公告或离线/加载前展示的默认标题与内容
#define SIYOX_DEFAULT_NOTICE_TITLE   "官方公告"
#define SIYOX_DEFAULT_NOTICE_CONTENT "欢迎使用SiyoX！请输入授权卡密激活后开始体验。"

// ==================== 默认更新弹窗配置 ====================
// 当网络验证未配置更新标题或更新日志时展示的默认内容
#define SIYOX_DEFAULT_UPDATE_TITLE   "SiyoX 更新公告"
#define SIYOX_DEFAULT_UPDATE_LOG     "有新版本可用，请及时更新以获得最佳体验！"

// ==================== 材质包 MD5 完整性校验开关 ====================
// true: 开启校验（下载/注入时自动校验 32 位 MD5，防止材质包被篡改或损坏）
// false: 关闭校验（直接注入）
// 提示：如果您的直链资源包需要经常在线更新或替换内容，不建议开启 MD5 校验（避免频繁修改 MD5 配置）
// 提示：若此处填写false则不需要填写下方资源包列表里的资源包MD5值
#define SIYOX_ENABLE_MD5_VERIFY  false

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
#define SIYOX_EPIC_APP_KEY       "your_epic_app_key"        // EPIC 应用 AppKey
#define SIYOX_EPIC_PORT          5000

static const char* SIYOX_EPIC_HOSTS[] = {
    "epic.z74d.top",
    "gl.t60.top",
    "test.t60.top",
    "epic.t5x.cc"
};
#define SIYOX_EPIC_HOSTS_COUNT (sizeof(SIYOX_EPIC_HOSTS) / sizeof(SIYOX_EPIC_HOSTS[0]))

// ==================== T3 网络验证配置====================
// 提示：支持填写T3后台接口列表中的16位调用码或完整调用地址
#define SIYOX_T3_API_HOST        "http://w2.t3yanzheng.com"  // T3 接口域名
#define SIYOX_T3_APP_KEY         "your_t3_app_key"           // T3 应用 AppKey
#define SIYOX_T3_RC4_KEY         "your_t3_rc4_key"           // T3 RC4 传输加密密钥
#define SIYOX_T3_LOGIN_CODE      "your_t3_login_code"        // 单码卡密登录 调用码 或 完整调用地址
#define SIYOX_T3_NOTICE_CODE     "your_t3_notice_code"       // 获取程序公告 调用码 或 完整调用地址
#define SIYOX_T3_VERSION_CODE    "your_t3_version_code"      // 获取最新版本 调用码 或 完整调用地址
#define SIYOX_T3_HEARTBEAT_CODE  "your_t3_heartbeat_code"    // 单码心跳验证 调用码 或 完整调用地址

// ==================== 微验网络验证配置 ====================
#define SIYOX_WEIYAN_API_HOST    "wy.llua.cn"                // 微验 接口域名
#define SIYOX_WEIYAN_APP_ID      "your_weiyan_app_id"        // 微验 应用 ID
#define SIYOX_WEIYAN_APP_KEY     "your_weiyan_app_key"       // 微验 程序密钥
#define SIYOX_WEIYAN_RC4_KEY     "your_weiyan_rc4_key"       // 微验 RC4 密钥
#define SIYOX_WEIYAN_API_TOKEN   "your_weiyan_api_token"     // 微验 API 令牌
#define SIYOX_WEIYAN_LOGIN_CODE  "your_weiyan_login_code"    // 单码登录接口 API调用ID
#define SIYOX_WEIYAN_NOTICE_CODE "your_weiyan_notice_code"   // 公告接口 API调用ID
#define SIYOX_WEIYAN_UPDATE_CODE "your_weiyan_update_code"   // 更新接口 API调用ID

#endif // SIYOX_CONFIG_H
