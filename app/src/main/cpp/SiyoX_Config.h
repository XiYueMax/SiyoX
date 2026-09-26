#ifndef SIYOX_CONFIG_H
#define SIYOX_CONFIG_H
#include <stdbool.h>
#define SIYOX_ACTIVE_VERIFY_TYPE  0
#define SIYOX_CLIENT_NAME        "SiyoX Client"
#define SIYOX_CLIENT_AUTHOR      "XiYue."
#define SIYOX_VERSION_CODE       3
#define SIYOX_DEFAULT_NOTICE_TITLE   "官方公告"
#define SIYOX_DEFAULT_NOTICE_CONTENT "欢迎使用SiyoX！请输入授权卡密激活后开始体验。"
#define SIYOX_DEFAULT_UPDATE_TITLE   "SiyoX 更新公告"
#define SIYOX_DEFAULT_UPDATE_LOG     "有新版本可用，请及时更新以获得最佳体验！"
#define SIYOX_ENABLE_LOGIN_VIDEO_REPLACE  false
#define SIYOX_LOGIN_VIDEO_URL             "https://example.com/loginVideo.mp4"
#define SIYOX_ENABLE_WATERMARK           true
#define SIYOX_WATERMARK_TEXT             "SiyoX Client"
#define SIYOX_ALLOW_PANEL_TOGGLE_WATERMARK true
#define SIYOX_ENABLE_MD5_VERIFY  false

#define SIYOX_ENTITY_KILLER_ENABLE  true

#define SIYOX_ENTITY_KILLER_ENABLE_CUSTOM  false

#define SIYOX_ENTITY_KILLER_PATTERNS \
    ".*sword.*,.*jian.*,.*blade.*,.*dao.*,.*weapon.*,.*armor.*," \
    ".*attachables.*,.*attachable.*,.*animations.*,.*animation_controllers.*," \
    ".*render_controllers.*,.*player\\.entity.*," \
    ".*chatExtensionMainScreenMsgNew.*,.*netease_chat_screen_extension.*," \
    ".*neteaseVoiceTrans.*,.*speakSectionUI.*,.*gameChatMainUI.*,.*gameChatPlayerInfoTipsUI.*"

typedef struct {
    const char *name;
    const char *url;
    const char *md5;
    const char *description;
} SiyoXDefaultResource;
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
#define SIYOX_EPIC_APP_KEY       "your_epic_app_key"
#define SIYOX_EPIC_PORT          5000
static const char* SIYOX_EPIC_HOSTS[] = {
    "epic.z74d.top",
    "gl.t60.top",
    "test.t60.top",
    "epic.t5x.cc"
};
#define SIYOX_EPIC_HOSTS_COUNT (sizeof(SIYOX_EPIC_HOSTS) / sizeof(SIYOX_EPIC_HOSTS[0]))
#define SIYOX_T3_API_HOST        "http://w2.t3yanzheng.com"
#define SIYOX_T3_APP_KEY         "your_t3_app_key"
#define SIYOX_T3_RC4_KEY         "your_t3_rc4_key"
#define SIYOX_T3_LOGIN_CODE      "your_t3_login_code"
#define SIYOX_T3_NOTICE_CODE     "your_t3_notice_code"
#define SIYOX_T3_VERSION_CODE    "your_t3_version_code"
#define SIYOX_T3_HEARTBEAT_CODE  "your_t3_heartbeat_code"
#define SIYOX_WEIYAN_API_HOST    "wy.llua.cn"
#define SIYOX_WEIYAN_APP_ID      "your_weiyan_app_id"
#define SIYOX_WEIYAN_APP_KEY     "your_weiyan_app_key"
#define SIYOX_WEIYAN_RC4_KEY     "your_weiyan_rc4_key"
#define SIYOX_WEIYAN_API_TOKEN   "your_weiyan_api_token"
#define SIYOX_WEIYAN_LOGIN_CODE  "your_weiyan_login_code"
#define SIYOX_WEIYAN_NOTICE_CODE "your_weiyan_notice_code"
#define SIYOX_WEIYAN_UPDATE_CODE "your_weiyan_update_code"
#endif
