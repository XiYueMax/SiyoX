#include <jni.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <unistd.h>
#include <ctime>
#include <string>
#include <vector>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <android/log.h>

#include "SiyoX_Config.h"

#define LOG_TAG "SiyoX_NativeVerify"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static void rc4_crypt(const unsigned char *key, int key_len, const unsigned char *input, unsigned char *output, int length) {
    unsigned char s[256];
    for (int i = 0; i < 256; i++) {
        s[i] = (unsigned char)i;
    }
    int j = 0;
    for (int i = 0; i < 256; i++) {
        j = (j + s[i] + key[i % key_len]) % 256;
        unsigned char tmp = s[i];
        s[i] = s[j];
        s[j] = tmp;
    }
    int i = 0;
    j = 0;
    for (int k = 0; k < length; k++) {
        i = (i + 1) % 256;
        j = (j + s[i]) % 256;
        unsigned char tmp = s[i];
        s[i] = s[j];
        s[j] = tmp;
        output[k] = input[k] ^ s[(s[i] + s[j]) % 256];
    }
}

static std::string rc4_encrypt_to_hex(const std::string &key, const std::string &input) {
    if (key.empty() || input.empty()) return "";
    int len = (int)input.length();
    auto *out_bytes = (unsigned char*)malloc((size_t)len);
    rc4_crypt((const unsigned char*)key.c_str(), (int)key.length(), (const unsigned char*)input.c_str(), out_bytes, len);
    char *hex_str = (char*)malloc((size_t)(len * 2 + 1));
    for (int i = 0; i < len; i++) {
        sprintf(&hex_str[i * 2], "%02x", out_bytes[i]);
    }
    hex_str[len * 2] = '\0';
    std::string result(hex_str);
    free(out_bytes);
    free(hex_str);
    return result;
}

static std::string rc4_decrypt_bytes(const std::string &key, const unsigned char *input_bytes, int length) {
    if (key.empty() || input_bytes == nullptr || length <= 0) return "";
    auto *decrypted = (unsigned char*)malloc((size_t)(length + 1));
    rc4_crypt((const unsigned char*)key.c_str(), (int)key.length(), input_bytes, decrypted, length);
    decrypted[length] = '\0';
    std::string result((char*)decrypted);
    free(decrypted);
    return result;
}

static std::string rc4_decrypt_from_hex(const std::string &key, const std::string &hex_input) {
    if (key.empty() || hex_input.empty() || (hex_input.length() % 2 != 0)) return "";
    size_t len = hex_input.length() / 2;
    auto *cipher = (unsigned char*)malloc(len);
    for (size_t i = 0; i < len; i++) {
        char buf[3] = { hex_input[i * 2], hex_input[i * 2 + 1], 0 };
        cipher[i] = (unsigned char)strtol(buf, nullptr, 16);
    }
    auto *decrypted = (unsigned char*)malloc(len + 1);
    rc4_crypt((const unsigned char*)key.c_str(), (int)key.length(), cipher, decrypted, (int)len);
    decrypted[len] = '\0';
    std::string result((char*)decrypted);
    free(cipher);
    free(decrypted);
    return result;
}

struct MD5Context {
    uint32_t state[4];
    uint32_t count[2];
    uint8_t buffer[64];
};

static void MD5Transform(uint32_t state[4], const uint8_t block[64]) {
    uint32_t a = state[0], b = state[1], c = state[2], d = state[3], x[16];
    for (int i = 0, j = 0; i < 16; i++, j += 4)
        x[i] = ((uint32_t)block[j]) | (((uint32_t)block[j+1]) << 8) | (((uint32_t)block[j+2]) << 16) | (((uint32_t)block[j+3]) << 24);

    #define F(x, y, z) (((x) & (y)) | ((~x) & (z)))
    #define G(x, y, z) (((x) & (z)) | ((y) & (~z)))
    #define H(x, y, z) ((x) ^ (y) ^ (z))
    #define I(x, y, z) ((y) ^ ((x) | (~z)))
    #define ROTL(x, n) (((x) << (n)) | ((x) >> (32 - (n))))
    #define FF(a, b, c, d, x, s, ac) { (a) += F((b), (c), (d)) + (x) + (uint32_t)(ac); (a) = ROTL((a), (s)); (a) += (b); }
    #define GG(a, b, c, d, x, s, ac) { (a) += G((b), (c), (d)) + (x) + (uint32_t)(ac); (a) = ROTL((a), (s)); (a) += (b); }
    #define HH(a, b, c, d, x, s, ac) { (a) += H((b), (c), (d)) + (x) + (uint32_t)(ac); (a) = ROTL((a), (s)); (a) += (b); }
    #define II(a, b, c, d, x, s, ac) { (a) += I((b), (c), (d)) + (x) + (uint32_t)(ac); (a) = ROTL((a), (s)); (a) += (b); }

    FF(a, b, c, d, x[ 0],  7, 0xd76aa478); FF(d, a, b, c, x[ 1], 12, 0xe8c7b756);
    FF(c, d, a, b, x[ 2], 17, 0x242070db); FF(b, c, d, a, x[ 3], 22, 0xc1bdceee);
    FF(a, b, c, d, x[ 4],  7, 0xf57c0faf); FF(d, a, b, c, x[ 5], 12, 0x4787c62a);
    FF(c, d, a, b, x[ 6], 17, 0xa8304613); FF(b, c, d, a, x[ 7], 22, 0xfd469501);
    FF(a, b, c, d, x[ 8],  7, 0x698098d8); FF(d, a, b, c, x[ 9], 12, 0x8b44f7af);
    FF(c, d, a, b, x[10], 17, 0xffff5bb1); FF(b, c, d, a, x[11], 22, 0x895cd7be);
    FF(a, b, c, d, x[12],  7, 0x6b901122); FF(d, a, b, c, x[13], 12, 0xfd987193);
    FF(c, d, a, b, x[14], 17, 0xa679438e); FF(b, c, d, a, x[15], 22, 0x49b40821);

    GG(a, b, c, d, x[ 1],  5, 0xf61e2562); GG(d, a, b, c, x[ 6],  9, 0xc040b340);
    GG(c, d, a, b, x[11], 14, 0x265e5a51); GG(b, c, d, a, x[ 0], 20, 0xe9b6c7aa);
    GG(a, b, c, d, x[ 5],  5, 0xd62f105d); GG(d, a, b, c, x[10],  9, 0x02441453);
    GG(c, d, a, b, x[15], 14, 0xd8a1e681); GG(b, c, d, a, x[ 4], 20, 0xe7d3fbc8);
    GG(a, b, c, d, x[ 9],  5, 0x21e1cde6); GG(d, a, b, c, x[14],  9, 0xc33707d6);
    GG(c, d, a, b, x[ 3], 14, 0xf4d50d87); GG(b, c, d, a, x[ 8], 20, 0x455a14ed);
    GG(a, b, c, d, x[13],  5, 0xa9e3e905); GG(d, a, b, c, x[ 2],  9, 0xfcefa3f8);
    GG(c, d, a, b, x[ 7], 14, 0x676f02d9); GG(b, c, d, a, x[12], 20, 0x8d2a4c8a);

    HH(a, b, c, d, x[ 5],  4, 0xfffa3942); HH(d, a, b, c, x[ 8], 11, 0x8771f681);
    HH(c, d, a, b, x[11], 16, 0x6d9d6122); HH(b, c, d, a, x[14], 23, 0xfde5380c);
    HH(a, b, c, d, x[ 1],  4, 0xa4beea44); HH(d, a, b, c, x[ 4], 11, 0x4bdecfa9);
    HH(c, d, a, b, x[ 7], 16, 0xf6bb4b60); HH(b, c, d, a, x[10], 23, 0xbebfbc70);
    HH(a, b, c, d, x[13],  4, 0x289b7ec6); HH(d, a, b, c, x[ 0], 11, 0xeaa127fa);
    HH(c, d, a, b, x[ 3], 16, 0xd4ef3085); HH(b, c, d, a, x[ 6], 23, 0x04881d05);
    HH(a, b, c, d, x[ 9],  4, 0xd9d4d039); HH(d, a, b, c, x[12], 11, 0xe6db99e5);
    HH(c, d, a, b, x[15], 16, 0x1fa27cf8); HH(b, c, d, a, x[ 2], 23, 0xc4ac5665);

    II(a, b, c, d, x[ 0],  6, 0xf4292244); II(d, a, b, c, x[ 7], 10, 0x432aff97);
    II(c, d, a, b, x[14], 15, 0xab9423a7); II(b, c, d, a, x[ 5], 21, 0xfc93a039);
    II(a, b, c, d, x[12],  6, 0x655b59c3); II(d, a, b, c, x[ 3], 10, 0x8f0ccc92);
    II(c, d, a, b, x[10], 15, 0xffeff47d); II(b, c, d, a, x[ 1], 21, 0x85845dd1);
    II(a, b, c, d, x[ 8],  6, 0x6fa87e4f); II(d, a, b, c, x[15], 10, 0xfe2ce6e0);
    II(c, d, a, b, x[ 6], 15, 0xa3014314); II(b, c, d, a, x[13], 21, 0x4e0811a1);
    II(a, b, c, d, x[ 4],  6, 0xf7537e82); II(d, a, b, c, x[11], 10, 0xbd3af235);
    II(c, d, a, b, x[ 2], 15, 0x2ad7d2bb); II(b, c, d, a, x[ 9], 21, 0xeb86d391);

    #undef F
    #undef G
    #undef H
    #undef I
    #undef ROTL
    #undef FF
    #undef GG
    #undef HH
    #undef II

    state[0] += a;
    state[1] += b;
    state[2] += c;
    state[3] += d;
}

static std::string md5_string(const std::string &str) {
    MD5Context ctx;
    ctx.count[0] = ctx.count[1] = 0;
    ctx.state[0] = 0x67452301;
    ctx.state[1] = 0xefcdab89;
    ctx.state[2] = 0x98badcfe;
    ctx.state[3] = 0x10325476;

    const uint8_t *input = (const uint8_t*)str.c_str();
    uint32_t inputLen = (uint32_t)str.length();
    uint32_t i = 0, index = (ctx.count[0] >> 3) & 63, partLen = 64 - index;

    ctx.count[0] += (inputLen << 3);
    if (ctx.count[0] < (inputLen << 3)) ctx.count[1]++;
    ctx.count[1] += (inputLen >> 29);

    if (inputLen >= partLen) {
        memcpy(&ctx.buffer[index], input, partLen);
        MD5Transform(ctx.state, ctx.buffer);
        for (i = partLen; i + 63 < inputLen; i += 64)
            MD5Transform(ctx.state, &input[i]);
        index = 0;
    }
    memcpy(&ctx.buffer[index], &input[i], inputLen - i);

    uint8_t bits[8];
    for (int k = 0; k < 4; k++) bits[k] = (uint8_t)((ctx.count[0] >> (k * 8)) & 0xFF);
    for (int k = 0; k < 4; k++) bits[k + 4] = (uint8_t)((ctx.count[1] >> (k * 8)) & 0xFF);

    index = (ctx.count[0] >> 3) & 63;
    uint32_t padLen = (index < 56) ? (56 - index) : (120 - index);
    static const uint8_t PADDING[64] = { 0x80 };
    
    index = (ctx.count[0] >> 3) & 63;
    partLen = 64 - index;
    if (padLen >= partLen) {
        memcpy(&ctx.buffer[index], PADDING, partLen);
        MD5Transform(ctx.state, ctx.buffer);
        for (i = partLen; i + 63 < padLen; i += 64)
            MD5Transform(ctx.state, &PADDING[i]);
        index = 0;
    } else i = 0;
    memcpy(&ctx.buffer[index], &PADDING[i], padLen - i);

    index = (ctx.count[0] >> 3) & 63;
    partLen = 64 - index;
    if (8 >= partLen) {
        memcpy(&ctx.buffer[index], bits, partLen);
        MD5Transform(ctx.state, ctx.buffer);
        for (i = partLen; i + 63 < 8; i += 64)
            MD5Transform(ctx.state, &bits[i]);
        index = 0;
    } else i = 0;
    memcpy(&ctx.buffer[index], &bits[i], 8 - i);

    uint8_t digest[16];
    for (int k = 0; k < 4; k++) {
        digest[k * 4]     = (uint8_t)((ctx.state[k]      ) & 0xFF);
        digest[k * 4 + 1] = (uint8_t)((ctx.state[k] >>  8) & 0xFF);
        digest[k * 4 + 2] = (uint8_t)((ctx.state[k] >> 16) & 0xFF);
        digest[k * 4 + 3] = (uint8_t)((ctx.state[k] >> 24) & 0xFF);
    }

    char hex[33];
    for (int k = 0; k < 16; k++) sprintf(&hex[k * 2], "%02x", digest[k]);
    hex[32] = '\0';
    return std::string(hex);
}

static int native_http_post_binary(const char *host, int port, const char *path, const char *body, unsigned char *resp_buffer, size_t max_resp_len) {
    if (host == nullptr || path == nullptr || body == nullptr || resp_buffer == nullptr || max_resp_len == 0) {
        return -1;
    }
    std::string clean_host = host;
    if (clean_host.rfind("http://", 0) == 0) clean_host = clean_host.substr(7);
    if (clean_host.rfind("https://", 0) == 0) clean_host = clean_host.substr(8);
    while (!clean_host.empty() && clean_host.back() == '/') clean_host.pop_back();

    struct addrinfo hints, *res = nullptr;
    memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;

    char port_str[16];
    snprintf(port_str, sizeof(port_str), "%d", port);

    if (getaddrinfo(clean_host.c_str(), port_str, &hints, &res) != 0 || res == nullptr) {
        return -2;
    }

    int sockfd = socket(res->ai_family, res->ai_socktype, res->ai_protocol);
    if (sockfd < 0) {
        freeaddrinfo(res);
        return -1;
    }

    struct timeval timeout;
    timeout.tv_sec = 8;
    timeout.tv_usec = 0;
    setsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
    setsockopt(sockfd, SOL_SOCKET, SO_SNDTIMEO, &timeout, sizeof(timeout));

    if (connect(sockfd, res->ai_addr, res->ai_addrlen) < 0) {
        close(sockfd);
        freeaddrinfo(res);
        return -3;
    }

    freeaddrinfo(res);

    char request[2048];
    int req_len = snprintf(request, sizeof(request),
        "POST %s HTTP/1.1\r\n"
        "Host: %s\r\n"
        "Content-Type: application/x-www-form-urlencoded\r\n"
        "Content-Length: %zu\r\n"
        "User-Agent: SiyoX_Native/1.0\r\n"
        "Connection: close\r\n\r\n"
        "%s",
        path, host, strlen(body), body);

    if (send(sockfd, request, (size_t)req_len, 0) < 0) {
        close(sockfd);
        return -4;
    }

    std::vector<unsigned char> full_resp;
    unsigned char chunk[1024];
    ssize_t bytes_read = 0;
    while ((bytes_read = recv(sockfd, chunk, sizeof(chunk), 0)) > 0) {
        full_resp.insert(full_resp.end(), chunk, chunk + bytes_read);
        if (full_resp.size() >= 65536) break;
    }

    close(sockfd);

    if (full_resp.empty()) return 0;

    const char *header_end_pattern = "\r\n\r\n";
    size_t pattern_len = 4;
    size_t body_start_idx = 0;
    for (size_t i = 0; i + pattern_len <= full_resp.size(); i++) {
        if (memcmp(&full_resp[i], header_end_pattern, pattern_len) == 0) {
            body_start_idx = i + pattern_len;
            break;
        }
    }

    if (body_start_idx == 0) {
        size_t copy_len = full_resp.size() < max_resp_len ? full_resp.size() : max_resp_len - 1;
        memcpy(resp_buffer, full_resp.data(), copy_len);
        resp_buffer[copy_len] = '\0';
        return (int)copy_len;
    }

    size_t body_len = full_resp.size() - body_start_idx;
    size_t copy_len = body_len < max_resp_len ? body_len : max_resp_len - 1;
    memcpy(resp_buffer, &full_resp[body_start_idx], copy_len);
    resp_buffer[copy_len] = '\0';
    return (int)copy_len;
}

static void parse_t3_host_and_path(const char* host_cfg, const char* code_cfg, std::string &out_host, std::string &out_path) {
    std::string code_str = code_cfg ? code_cfg : "";
    if (code_str.rfind("http://", 0) == 0) {
        std::string without_proto = code_str.substr(7);
        auto slash_pos = without_proto.find('/');
        if (slash_pos != std::string::npos) {
            out_host = without_proto.substr(0, slash_pos);
            out_path = without_proto.substr(slash_pos);
        } else {
            out_host = without_proto;
            out_path = "/";
        }
        return;
    }
    if (code_str.rfind("https://", 0) == 0) {
        std::string without_proto = code_str.substr(8);
        auto slash_pos = without_proto.find('/');
        if (slash_pos != std::string::npos) {
            out_host = without_proto.substr(0, slash_pos);
            out_path = without_proto.substr(slash_pos);
        } else {
            out_host = without_proto;
            out_path = "/";
        }
        return;
    }
    std::string h = host_cfg ? host_cfg : "w2.t3yanzheng.com";
    if (h.rfind("http://", 0) == 0) h = h.substr(7);
    if (h.rfind("https://", 0) == 0) h = h.substr(8);
    while (!h.empty() && h.back() == '/') h.pop_back();
    out_host = h;
    out_path = "/" + code_str;
}

extern "C" {

JNIEXPORT jint JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetActiveVerifyType(JNIEnv *env, jclass clazz) {
    return SIYOX_ACTIVE_VERIFY_TYPE;
}

JNIEXPORT jint JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetVersionCode(JNIEnv *env, jclass clazz) {
    return SIYOX_VERSION_CODE;
}

JNIEXPORT jstring JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetClientName(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(SIYOX_CLIENT_NAME);
}

JNIEXPORT jstring JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetClientAuthor(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(SIYOX_CLIENT_AUTHOR);
}

JNIEXPORT jstring JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetDefaultNoticeTitle(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(SIYOX_DEFAULT_NOTICE_TITLE);
}

JNIEXPORT jstring JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetDefaultNoticeContent(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(SIYOX_DEFAULT_NOTICE_CONTENT);
}

JNIEXPORT jstring JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetDefaultUpdateTitle(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(SIYOX_DEFAULT_UPDATE_TITLE);
}

JNIEXPORT jstring JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetDefaultUpdateLog(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(SIYOX_DEFAULT_UPDATE_LOG);
}

JNIEXPORT jboolean JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetEnableMd5Verify(JNIEnv *env, jclass clazz) {
    return SIYOX_ENABLE_MD5_VERIFY ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetEnableLoginVideoReplace(JNIEnv *env, jclass clazz) {
    return SIYOX_ENABLE_LOGIN_VIDEO_REPLACE ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetLoginVideoUrl(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(SIYOX_LOGIN_VIDEO_URL);
}

JNIEXPORT jboolean JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetEnableWatermark(JNIEnv *env, jclass clazz) {
    return SIYOX_ENABLE_WATERMARK ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetWatermarkText(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(SIYOX_WATERMARK_TEXT);
}

JNIEXPORT jboolean JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetAllowPanelToggleWatermark(JNIEnv *env, jclass clazz) {
    return SIYOX_ALLOW_PANEL_TOGGLE_WATERMARK ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetEpicAppKey(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(SIYOX_EPIC_APP_KEY);
}

JNIEXPORT jint JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetEpicPort(JNIEnv *env, jclass clazz) {
    return SIYOX_EPIC_PORT;
}

JNIEXPORT jobjectArray JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetEpicHosts(JNIEnv *env, jclass clazz) {
    size_t count = SIYOX_EPIC_HOSTS_COUNT;
    jclass string_class = env->FindClass("java/lang/String");
    jobjectArray array = env->NewObjectArray((jsize)count, string_class, nullptr);
    for (size_t i = 0; i < count; i++) {
        jstring str = env->NewStringUTF(SIYOX_EPIC_HOSTS[i]);
        env->SetObjectArrayElement(array, (jsize)i, str);
        env->DeleteLocalRef(str);
    }
    return array;
}

JNIEXPORT jstring JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetT3ConfigJson(JNIEnv *env, jclass clazz) {
    char json[4096];
    snprintf(json, sizeof(json),
        "{"
        "\"apiHost\":\"%s\","
        "\"appKey\":\"%s\","
        "\"rc4Key\":\"%s\","
        "\"loginCode\":\"%s\","
        "\"noticeCode\":\"%s\","
        "\"versionCode\":\"%s\","
        "\"heartbeatCode\":\"%s\""
        "}",
        SIYOX_T3_API_HOST,
        SIYOX_T3_APP_KEY,
        SIYOX_T3_RC4_KEY,
        SIYOX_T3_LOGIN_CODE,
        SIYOX_T3_NOTICE_CODE,
        SIYOX_T3_VERSION_CODE,
        SIYOX_T3_HEARTBEAT_CODE);
    return env->NewStringUTF(json);
}

JNIEXPORT jstring JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetWeiYanConfigJson(JNIEnv *env, jclass clazz) {
    char json[4096];
    snprintf(json, sizeof(json),
        "{"
        "\"apiHost\":\"%s\","
        "\"appId\":\"%s\","
        "\"appKey\":\"%s\","
        "\"rc4Key\":\"%s\","
        "\"apiToken\":\"%s\","
        "\"loginCode\":\"%s\","
        "\"noticeCode\":\"%s\","
        "\"updateCode\":\"%s\""
        "}",
        SIYOX_WEIYAN_API_HOST,
        SIYOX_WEIYAN_APP_ID,
        SIYOX_WEIYAN_APP_KEY,
        SIYOX_WEIYAN_RC4_KEY,
        SIYOX_WEIYAN_API_TOKEN,
        SIYOX_WEIYAN_LOGIN_CODE,
        SIYOX_WEIYAN_NOTICE_CODE,
        SIYOX_WEIYAN_UPDATE_CODE);
    return env->NewStringUTF(json);
}

JNIEXPORT jstring JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeGetDefaultResourcesJson(JNIEnv *env, jclass clazz) {
    std::string json = "[";
    size_t count = SIYOX_DEFAULT_RESOURCES_COUNT;
    for (size_t i = 0; i < count; i++) {
        char item[2048];
        snprintf(item, sizeof(item),
            "{\"name\":\"%s\",\"url\":\"%s\",\"md5\":\"%s\",\"description\":\"%s\"}%s",
            SIYOX_DEFAULT_RESOURCES[i].name,
            SIYOX_DEFAULT_RESOURCES[i].url,
            SIYOX_DEFAULT_RESOURCES[i].md5,
            SIYOX_DEFAULT_RESOURCES[i].description,
            (i < count - 1) ? "," : "");
        json += item;
    }
    json += "]";
    return env->NewStringUTF(json.c_str());
}

JNIEXPORT jstring JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeT3VerifyCard(
        JNIEnv *env,
        jclass clazz,
        jstring card_str,
        jstring imei_str) {

    if (card_str == nullptr || imei_str == nullptr) {
        return env->NewStringUTF("{\"code\":-1,\"msg\":\"参数不能为空\"}");
    }

    const char *card = env->GetStringUTFChars(card_str, nullptr);
    const char *imei = env->GetStringUTFChars(imei_str, nullptr);

    std::string host, path;
    parse_t3_host_and_path(SIYOX_T3_API_HOST, SIYOX_T3_LOGIN_CODE, host, path);

    std::string rc4_key = SIYOX_T3_RC4_KEY;
    std::string app_key = SIYOX_T3_APP_KEY;
    std::string post_body;

    time_t now = time(nullptr);
    std::string t_str = std::to_string((long)now);

    if (!rc4_key.empty() && rc4_key != "your_t3_rc4_key") {
        std::string k_enc = rc4_encrypt_to_hex(rc4_key, card);
        std::string i_enc = rc4_encrypt_to_hex(rc4_key, imei);
        std::string t_enc = rc4_encrypt_to_hex(rc4_key, t_str);
        std::string s_src = "kami=" + k_enc + "&imei=" + i_enc + "&t=" + t_enc + "&" + app_key;
        std::string s_val = md5_string(s_src);
        std::string s_enc = rc4_encrypt_to_hex(rc4_key, s_val);

        post_body = "kami=" + k_enc + "&imei=" + i_enc + "&t=" + t_enc + "&s=" + s_enc;
    } else {
        post_body = "kami=" + std::string(card) + "&imei=" + std::string(imei);
    }

    unsigned char raw_resp[8192] = { 0 };
    int ret = native_http_post_binary(host.c_str(), 80, path.c_str(), post_body.c_str(), raw_resp, sizeof(raw_resp));

    std::string final_resp;
    if (ret > 0) {
        if (!rc4_key.empty() && rc4_key != "your_t3_rc4_key") {
            std::string dec = rc4_decrypt_bytes(rc4_key, raw_resp, ret);
            if (!dec.empty()) final_resp = dec;
            else final_resp = std::string((char*)raw_resp, (size_t)ret);
        } else {
            final_resp = std::string((char*)raw_resp, (size_t)ret);
        }
    } else {
        final_resp = "{\"code\":-1,\"msg\":\"Native C 连接 T3 网络失败\"}";
    }

    env->ReleaseStringUTFChars(card_str, card);
    env->ReleaseStringUTFChars(imei_str, imei);

    return env->NewStringUTF(final_resp.c_str());
}

JNIEXPORT jstring JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeT3FetchNotice(
        JNIEnv *env,
        jclass clazz) {

    std::string host, path;
    parse_t3_host_and_path(SIYOX_T3_API_HOST, SIYOX_T3_NOTICE_CODE, host, path);

    std::string rc4_key = SIYOX_T3_RC4_KEY;
    std::string app_key = SIYOX_T3_APP_KEY;
    std::string post_body;

    time_t now = time(nullptr);
    std::string t_str = std::to_string((long)now);

    if (!rc4_key.empty() && rc4_key != "your_t3_rc4_key") {
        std::string t_enc = rc4_encrypt_to_hex(rc4_key, t_str);
        std::string s_src = "t=" + t_enc + "&" + app_key;
        std::string s_val = md5_string(s_src);
        std::string s_enc = rc4_encrypt_to_hex(rc4_key, s_val);
        post_body = "t=" + t_enc + "&s=" + s_enc;
    }

    unsigned char raw_resp[4096] = { 0 };
    int ret = native_http_post_binary(host.c_str(), 80, path.c_str(), post_body.c_str(), raw_resp, sizeof(raw_resp));

    std::string final_resp;
    if (ret > 0) {
        if (!rc4_key.empty() && rc4_key != "your_t3_rc4_key") {
            std::string dec = rc4_decrypt_bytes(rc4_key, raw_resp, ret);
            if (!dec.empty()) final_resp = dec;
            else final_resp = std::string((char*)raw_resp, (size_t)ret);
        } else {
            final_resp = std::string((char*)raw_resp, (size_t)ret);
        }
    }

    return env->NewStringUTF(final_resp.c_str());
}

JNIEXPORT jstring JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeT3Heartbeat(
        JNIEnv *env,
        jclass clazz,
        jstring card_str,
        jstring statecode_str) {

    if (card_str == nullptr || statecode_str == nullptr) {
        return env->NewStringUTF("{\"code\":-1,\"msg\":\"参数不能为空\"}");
    }

    const char *card = env->GetStringUTFChars(card_str, nullptr);
    const char *statecode = env->GetStringUTFChars(statecode_str, nullptr);

    std::string host, path;
    parse_t3_host_and_path(SIYOX_T3_API_HOST, SIYOX_T3_HEARTBEAT_CODE, host, path);

    std::string rc4_key = SIYOX_T3_RC4_KEY;
    std::string app_key = SIYOX_T3_APP_KEY;
    std::string post_body;

    time_t now = time(nullptr);
    std::string t_str = std::to_string((long)now);

    if (!rc4_key.empty() && rc4_key != "your_t3_rc4_key") {
        std::string k_enc = rc4_encrypt_to_hex(rc4_key, card);
        std::string st_enc = rc4_encrypt_to_hex(rc4_key, statecode);
        std::string t_enc = rc4_encrypt_to_hex(rc4_key, t_str);
        std::string s_src = "kami=" + k_enc + "&statecode=" + st_enc + "&t=" + t_enc + "&" + app_key;
        std::string s_val = md5_string(s_src);
        std::string s_enc = rc4_encrypt_to_hex(rc4_key, s_val);
        post_body = "kami=" + k_enc + "&statecode=" + st_enc + "&t=" + t_enc + "&s=" + s_enc;
    } else {
        post_body = "kami=" + std::string(card) + "&statecode=" + std::string(statecode);
    }

    unsigned char raw_resp[4096] = { 0 };
    int ret = native_http_post_binary(host.c_str(), 80, path.c_str(), post_body.c_str(), raw_resp, sizeof(raw_resp));

    std::string result_json = (ret > 0) ? "{\"code\":200,\"msg\":\"心跳成功\"}" : "{\"code\":-1,\"msg\":\"心跳失败\"}";

    env->ReleaseStringUTFChars(card_str, card);
    env->ReleaseStringUTFChars(statecode_str, statecode);

    return env->NewStringUTF(result_json.c_str());
}

JNIEXPORT jstring JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeVerifyCard(
        JNIEnv *env,
        jclass clazz,
        jint verify_type,
        jstring card_str,
        jstring imei_str) {

    if (card_str == nullptr || imei_str == nullptr) {
        return env->NewStringUTF("{\"code\":-1,\"msg\":\"参数不能为空\"}");
    }

    const char *card = env->GetStringUTFChars(card_str, nullptr);
    const char *imei = env->GetStringUTFChars(imei_str, nullptr);
    char result_json[2048] = { 0 };

    if (verify_type == 3) {
        const char *host = SIYOX_WEIYAN_API_HOST;
        const char *app_id = SIYOX_WEIYAN_APP_ID;
        const char *app_key = SIYOX_WEIYAN_APP_KEY;
        const char *rc4_key = SIYOX_WEIYAN_RC4_KEY;
        const char *login_code = SIYOX_WEIYAN_LOGIN_CODE;

        time_t now = time(nullptr);
        int rand_val = rand();

        std::string sign_src = "kami=" + std::string(card) + "&markcode=" + std::string(imei) + "&t=" + std::to_string((long)now) + "&" + app_key;
        std::string sign_md5 = md5_string(sign_src);

        std::string plain_data = "kami=" + std::string(card) + "&markcode=" + std::string(imei) + "&t=" + std::to_string((long)now) + "&sign=" + sign_md5 + "&value=" + std::to_string((long)now) + std::to_string(rand_val);

        std::string data_hex = rc4_encrypt_to_hex(rc4_key, plain_data);

        char path[256];
        snprintf(path, sizeof(path), "/api/?id=%s", login_code);
        char post_body[4096];
        snprintf(post_body, sizeof(post_body), "app=%s&data=%s", app_id, data_hex.c_str());

        unsigned char raw_resp[4096] = { 0 };
        int ret = native_http_post_binary(host, 80, path, post_body, raw_resp, sizeof(raw_resp));

        if (ret > 0 && strstr((char*)raw_resp, "\"code\":-1") != nullptr && strcmp(login_code, "kmlogon") != 0) {
            snprintf(path, sizeof(path), "/api/?id=kmlogon");
            memset(raw_resp, 0, sizeof(raw_resp));
            ret = native_http_post_binary(host, 80, path, post_body, raw_resp, sizeof(raw_resp));
        }

        if (ret > 0) {
            std::string raw_str((char*)raw_resp, (size_t)ret);
            if (raw_str.find("{") != std::string::npos && raw_str.find("\"code\"") != std::string::npos) {
                snprintf(result_json, sizeof(result_json), "%s", raw_str.c_str());
            } else {
                std::string dec = rc4_decrypt_from_hex(rc4_key, (const char*)raw_resp);
                if (!dec.empty() && dec.find("{") != std::string::npos) {
                    snprintf(result_json, sizeof(result_json), "%s", dec.c_str());
                } else {
                    snprintf(result_json, sizeof(result_json), "%s", raw_str.c_str());
                }
            }
        } else {
            snprintf(result_json, sizeof(result_json), "{\"code\":-1,\"msg\":\"Native C 连接微验网络失败\"}");
        }
    } else {
        snprintf(result_json, sizeof(result_json), "{\"code\":0,\"msg\":\"Native C Provider Ready\"}");
    }

    env->ReleaseStringUTFChars(card_str, card);
    env->ReleaseStringUTFChars(imei_str, imei);

    return env->NewStringUTF(result_json);
}

JNIEXPORT jstring JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeFetchNotice(
        JNIEnv *env,
        jclass clazz,
        jint verify_type) {

    char result_json[2048] = { 0 };

    if (verify_type == 3) {
        const char *host = SIYOX_WEIYAN_API_HOST;
        const char *app_id = SIYOX_WEIYAN_APP_ID;
        const char *rc4_key = SIYOX_WEIYAN_RC4_KEY;
        const char *notice_code = SIYOX_WEIYAN_NOTICE_CODE;

        char path[256];
        snprintf(path, sizeof(path), "/api/?id=%s", notice_code);
        char post_body[512];
        snprintf(post_body, sizeof(post_body), "app=%s", app_id);

        unsigned char raw_resp[4096] = { 0 };
        int ret = native_http_post_binary(host, 80, path, post_body, raw_resp, sizeof(raw_resp));

        if (ret > 0 && strstr((char*)raw_resp, "\"code\":-1") != nullptr && strcmp(notice_code, "notice") != 0) {
            snprintf(path, sizeof(path), "/api/?id=notice");
            memset(raw_resp, 0, sizeof(raw_resp));
            ret = native_http_post_binary(host, 80, path, post_body, raw_resp, sizeof(raw_resp));
        }

        if (ret > 0) {
            std::string raw_str((char*)raw_resp, (size_t)ret);
            if (raw_str.find("{") != std::string::npos && raw_str.find("\"code\"") != std::string::npos) {
                snprintf(result_json, sizeof(result_json), "%s", raw_str.c_str());
            } else {
                std::string dec = rc4_decrypt_from_hex(rc4_key, (const char*)raw_resp);
                if (!dec.empty() && dec.find("{") != std::string::npos) {
                    snprintf(result_json, sizeof(result_json), "%s", dec.c_str());
                } else {
                    snprintf(result_json, sizeof(result_json), "%s", raw_str.c_str());
                }
            }
        } else {
            snprintf(result_json, sizeof(result_json), "{\"code\":-1,\"msg\":\"Native C 获取微验公告失败\"}");
        }
    } else {
        snprintf(result_json, sizeof(result_json), "{\"code\":200,\"msg\":\"欢迎使用 SiyoX 模块！\"}");
    }

    return env->NewStringUTF(result_json);
}

}