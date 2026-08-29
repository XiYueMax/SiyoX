// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <time.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <android/log.h>

#define LOG_TAG "SiyoX_NativeVerify"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ==========================================
// 1. MD5 算法纯 C 实现
// ==========================================
typedef struct {
    uint32_t state[4];
    uint32_t count[2];
    unsigned char buffer[64];
} MD5_CTX;

#define S11 7
#define S12 12
#define S13 17
#define S14 22
#define S21 5
#define S22 9
#define S23 14
#define S24 20
#define S31 4
#define S32 11
#define S33 16
#define S34 23
#define S41 6
#define S42 10
#define S43 15
#define S44 21

#define F(x, y, z) (((x) & (y)) | ((~x) & (z)))
#define G(x, y, z) (((x) & (z)) | ((y) & (~z)))
#define H(x, y, z) ((x) ^ (y) ^ (z))
#define I(x, y, z) ((y) ^ ((x) | (~z)))

#define ROTATE_LEFT(x, n) (((x) << (n)) | ((x) >> (32-(n))))

#define FF(a, b, c, d, x, s, ac) { \
 (a) += F ((b), (c), (d)) + (x) + (uint32_t)(ac); \
 (a) = ROTATE_LEFT ((a), (s)); \
 (a) += (b); \
  }
#define GG(a, b, c, d, x, s, ac) { \
 (a) += G ((b), (c), (d)) + (x) + (uint32_t)(ac); \
 (a) = ROTATE_LEFT ((a), (s)); \
 (a) += (b); \
  }
#define HH(a, b, c, d, x, s, ac) { \
 (a) += H ((b), (c), (d)) + (x) + (uint32_t)(ac); \
 (a) = ROTATE_LEFT ((a), (s)); \
 (a) += (b); \
  }
#define II(a, b, c, d, x, s, ac) { \
 (a) += I ((b), (c), (d)) + (x) + (uint32_t)(ac); \
 (a) = ROTATE_LEFT ((a), (s)); \
 (a) += (b); \
  }

static void MD5Transform(uint32_t state[4], const unsigned char block[64]) {
    uint32_t a = state[0], b = state[1], c = state[2], d = state[3], x[16];
    for (int i = 0, j = 0; j < 64; i++, j += 4)
        x[i] = ((uint32_t)block[j]) | (((uint32_t)block[j+1]) << 8) |
               (((uint32_t)block[j+2]) << 16) | (((uint32_t)block[j+3]) << 24);

    FF (a, b, c, d, x[ 0], S11, 0xd76aa478);
    FF (d, a, b, c, x[ 1], S12, 0xe8c7b756);
    FF (c, d, a, b, x[ 2], S13, 0x242070db);
    FF (b, c, d, a, x[ 3], S14, 0xc1bdceee);
    FF (a, b, c, d, x[ 4], S11, 0xf57c0faf);
    FF (d, a, b, c, x[ 5], S12, 0x4787c62a);
    FF (c, d, a, b, x[ 6], S13, 0xa8304613);
    FF (b, c, d, a, x[ 7], S14, 0xfd469501);
    FF (a, b, c, d, x[ 8], S11, 0x698098d8);
    FF (d, a, b, c, x[ 9], S12, 0x8b44f7af);
    FF (c, d, a, b, x[10], S13, 0xffff5bb1);
    FF (b, c, d, a, x[11], S14, 0x895cd7be);
    FF (a, b, c, d, x[12], S11, 0x6b901122);
    FF (d, a, b, c, x[13], S12, 0xfd987193);
    FF (c, d, a, b, x[14], S13, 0xa679438e);
    FF (b, c, d, a, x[15], S14, 0x49b40821);

    GG (a, b, c, d, x[ 1], S21, 0xf61e2562);
    GG (d, a, b, c, x[ 6], S22, 0xc040b340);
    GG (c, d, a, b, x[11], S23, 0x265e5a51);
    GG (b, c, d, a, x[ 0], S24, 0xe9b6c7aa);
    GG (a, b, c, d, x[ 5], S21, 0xd62f105d);
    GG (d, a, b, c, x[10], S22,  0x2441453);
    GG (c, d, a, b, x[15], S23, 0xd8a1e681);
    GG (b, c, d, a, x[ 4], S24, 0xe7d3fbc8);
    GG (a, b, c, d, x[ 9], S21, 0x21e1cde6);
    GG (d, a, b, c, x[14], S22, 0xc33707d6);
    GG (c, d, a, b, x[ 3], S23, 0xf4d50d87);
    GG (b, c, d, a, x[ 8], S24, 0x455a14ed);
    GG (a, b, c, d, x[13], S21, 0xa9e3e905);
    GG (d, a, b, c, x[ 2], S22, 0xfcefa3f8);
    GG (c, d, a, b, x[ 7], S23, 0x676f02d9);
    GG (b, c, d, a, x[12], S24, 0x8d2a4c8a);

    HH (a, b, c, d, x[ 5], S31, 0xfffa3942);
    HH (d, a, b, c, x[ 8], S32, 0x8771f681);
    HH (c, d, a, b, x[11], S33, 0x6d9d6122);
    HH (b, c, d, a, x[14], S34, 0xfde5380c);
    HH (a, b, c, d, x[ 1], S31, 0xa4beea44);
    HH (d, a, b, c, x[ 4], S32, 0x4bdecfa9);
    HH (c, d, a, b, x[ 7], S33, 0xf6bb4b60);
    HH (b, c, d, a, x[10], S34, 0xbebfbc70);
    HH (a, b, c, d, x[13], S31, 0x289b7ec6);
    HH (d, a, b, c, x[ 0], S32, 0xeaa127fa);
    HH (c, d, a, b, x[ 3], S33, 0xd4ef3085);
    HH (b, c, d, a, x[ 6], S34,  0x4881d05);
    HH (a, b, c, d, x[ 9], S31, 0xd9d4d039);
    HH (d, a, b, c, x[12], S32, 0xe6db99e5);
    HH (c, d, a, b, x[15], S33, 0x1fa27cf8);
    HH (b, c, d, a, x[ 2], S34, 0xc4ac5665);

    II (a, b, c, d, x[ 0], S41, 0xf4292244);
    II (d, a, b, c, x[ 7], S42, 0x432aff97);
    II (c, d, a, b, x[14], S43, 0xab9423a7);
    II (b, c, d, a, x[ 5], S44, 0xfc93a039);
    II (a, b, c, d, x[12], S41, 0x655b59c3);
    II (d, a, b, c, x[ 3], S42, 0x8f0ccc92);
    II (c, d, a, b, x[10], S43, 0xffeff47d);
    II (b, c, d, a, x[ 1], S44, 0x85845dd1);
    II (a, b, c, d, x[ 8], S41, 0x6fa87e4f);
    II (d, a, b, c, x[15], S42, 0xfe2ce6e0);
    II (c, d, a, b, x[ 6], S43, 0xa3014314);
    II (b, c, d, a, x[13], S44, 0x4e0811a1);
    II (a, b, c, d, x[ 4], S41, 0xf7537e82);
    II (d, a, b, c, x[11], S42, 0xbd3af235);
    II (c, d, a, b, x[ 2], S43, 0x2ad7d2bb);
    II (b, c, d, a, x[ 9], S44, 0xeb86d391);

    state[0] += a;
    state[1] += b;
    state[2] += c;
    state[3] += d;
}

static void md5_calculate(const unsigned char *input, size_t length, unsigned char output[16]) {
    MD5_CTX context;
    context.count[0] = context.count[1] = 0;
    context.state[0] = 0x67452301;
    context.state[1] = 0xefcdab89;
    context.state[2] = 0x98badcfe;
    context.state[3] = 0x10325476;

    size_t index = (size_t)((context.count[0] >> 3) & 0x3F);
    if ((context.count[0] += ((uint32_t)length << 3)) < ((uint32_t)length << 3))
        context.count[1]++;
    context.count[1] += ((uint32_t)length >> 29);

    size_t partLen = 64 - index;
    size_t i = 0;

    if (length >= partLen) {
        memcpy(&context.buffer[index], input, partLen);
        MD5Transform(context.state, context.buffer);
        for (i = partLen; i + 63 < length; i += 64)
            MD5Transform(context.state, &input[i]);
        index = 0;
    }
    memcpy(&context.buffer[index], &input[i], length - i);

    unsigned char bits[8];
    for (int k = 0; k < 8; k++)
        bits[k] = (unsigned char)((context.count[k >= 4 ? 1 : 0] >> ((k % 4) * 8)) & 0xFF);

    unsigned char PADDING[64] = { 0x80 };
    index = (size_t)((context.count[0] >> 3) & 0x3f);
    size_t padLen = (index < 56) ? (56 - index) : (120 - index);

    index = (size_t)((context.count[0] >> 3) & 0x3F);
    if ((context.count[0] += ((uint32_t)padLen << 3)) < ((uint32_t)padLen << 3))
        context.count[1]++;
    context.count[1] += ((uint32_t)padLen >> 29);

    partLen = 64 - index;
    if (padLen >= partLen) {
        memcpy(&context.buffer[index], PADDING, partLen);
        MD5Transform(context.state, context.buffer);
        for (i = partLen; i + 63 < padLen; i += 64)
            MD5Transform(context.state, &PADDING[i]);
        index = 0;
    }
    memcpy(&context.buffer[index], &PADDING[i], padLen - i);

    index = (size_t)((context.count[0] >> 3) & 0x3F);
    memcpy(&context.buffer[index], bits, 8);
    MD5Transform(context.state, context.buffer);

    for (int k = 0; k < 16; k++)
        output[k] = (unsigned char)((context.state[k >> 2] >> ((k & 3) * 8)) & 0xFF);
}

static void md5_hex(const char* input, char* output33) {
    unsigned char digest[16];
    md5_calculate((const unsigned char*)input, strlen(input), digest);
    for (int i = 0; i < 16; i++) {
        sprintf(&output33[i * 2], "%02x", digest[i]);
    }
    output33[32] = '\0';
}

// ==========================================
// 2. RC4 算法纯 C 实现
// ==========================================
static void rc4_crypt(const unsigned char *key, int key_len, const unsigned char *in, unsigned char *out, int len) {
    unsigned char s[256];
    for (int i = 0; i < 256; i++) s[i] = (unsigned char)i;
    int j = 0;
    for (int i = 0; i < 256; i++) {
        j = (j + s[i] + key[i % key_len]) % 256;
        unsigned char t = s[i]; s[i] = s[j]; s[j] = t;
    }
    int i = 0; j = 0;
    for (int k = 0; k < len; k++) {
        i = (i + 1) % 256;
        j = (j + s[i]) % 256;
        unsigned char t = s[i]; s[i] = s[j]; s[j] = t;
        out[k] = in[k] ^ s[(s[i] + s[j]) % 256];
    }
}

// ==========================================
// 3. 原生 C 网络请求 (Socket HTTP POST)
// ==========================================
static int native_http_post(const char *host, int port, const char *path, const char *post_data, char *resp_buffer, int max_len) {
    struct hostent *server = gethostbyname(host);
    if (!server) {
        LOGE("Cannot resolve host: %s", host);
        return -1;
    }

    int sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) {
        LOGE("Cannot open socket");
        return -2;
    }

    struct sockaddr_in serv_addr;
    memset(&serv_addr, 0, sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(port);
    memcpy(&serv_addr.sin_addr.s_addr, server->h_addr, server->h_length);

    struct timeval tv;
    tv.tv_sec = 8;
    tv.tv_usec = 0;
    setsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, (const char*)&tv, sizeof tv);
    setsockopt(sockfd, SOL_SOCKET, SO_SNDTIMEO, (const char*)&tv, sizeof tv);

    if (connect(sockfd, (struct sockaddr*)&serv_addr, sizeof(serv_addr)) < 0) {
        LOGE("Connect failed to %s:%d", host, port);
        close(sockfd);
        return -3;
    }

    char request[4096];
    int req_len = snprintf(request, sizeof(request),
        "POST %s HTTP/1.1\r\n"
        "Host: %s\r\n"
        "Content-Type: application/x-www-form-urlencoded\r\n"
        "User-Agent: SiyoX-Native/1.0\r\n"
        "Content-Length: %zu\r\n"
        "Connection: close\r\n\r\n"
        "%s",
        path, host, strlen(post_data), post_data);

    if (write(sockfd, request, req_len) < 0) {
        LOGE("Write socket failed");
        close(sockfd);
        return -4;
    }

    int total_read = 0;
    int n;
    while ((n = read(sockfd, resp_buffer + total_read, max_len - total_read - 1)) > 0) {
        total_read += n;
        if (total_read >= max_len - 1) break;
    }
    resp_buffer[total_read] = '\0';
    close(sockfd);

    // Extract body after \r\n\r\n
    char *body = strstr(resp_buffer, "\r\n\r\n");
    if (body) {
        body += 4;
        memmove(resp_buffer, body, strlen(body) + 1);
    }

    return total_read;
}

// ==========================================
// 4. JNI 接口导出实现
// ==========================================

JNIEXPORT jstring JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeVerifyCard(
        JNIEnv *env,
        jclass clazz,
        jint verify_type,
        jstring card_str,
        jstring imei_str) {

    const char *card = (*env)->GetStringUTFChars(env, card_str, 0);
    const char *imei = (*env)->GetStringUTFChars(env, imei_str, 0);

    char result_json[2048] = { 0 };

    if (verify_type == 2) {
        // 微验 (WeiYan)
        const char *host = "wy.llua.cn";
        const char *app_id = "10000";
        const char *app_key = "8LjdoLopmH9LyLVh";
        const char *rc4_key = "ElFlF870vDk88gef";

        time_t now = time(NULL);
        int t = (int)now;
        char value[64];
        snprintf(value, sizeof(value), "%d%d", t, rand() % 10000);

        char sign_raw[512];
        snprintf(sign_raw, sizeof(sign_raw), "kami=%s&markcode=%s&t=%d&%s", card, imei, t, app_key);
        char sign_md5[33];
        md5_hex(sign_raw, sign_md5);

        char data_raw[1024];
        snprintf(data_raw, sizeof(data_raw), "kami=%s&markcode=%s&t=%d&sign=%s&value=%s", card, imei, t, sign_md5, value);

        int raw_len = (int)strlen(data_raw);
        unsigned char *encrypted = (unsigned char*)malloc(raw_len);
        rc4_crypt((const unsigned char*)rc4_key, (int)strlen(rc4_key), (const unsigned char*)data_raw, encrypted, raw_len);

        char *encrypted_hex = (char*)malloc(raw_len * 2 + 1);
        for (int i = 0; i < raw_len; i++) {
            sprintf(&encrypted_hex[i * 2], "%02x", encrypted[i]);
        }
        encrypted_hex[raw_len * 2] = '\0';

        char path[256];
        snprintf(path, sizeof(path), "/api/?id=kmlogon&app=%s", app_id);
        char post_body[2048];
        snprintf(post_body, sizeof(post_body), "&data=%s", encrypted_hex);

        char raw_resp[4096] = { 0 };
        int ret = native_http_post(host, 80, path, post_body, raw_resp, sizeof(raw_resp));

        if (ret > 0) {
            // Decrypt response hex with RC4
            int resp_len = (int)strlen(raw_resp);
            int hex_bytes_len = resp_len / 2;
            unsigned char *hex_bytes = (unsigned char*)malloc(hex_bytes_len);
            for (int i = 0; i < hex_bytes_len; i++) {
                unsigned int byte_val;
                sscanf(&raw_resp[i * 2], "%02x", &byte_val);
                hex_bytes[i] = (unsigned char)byte_val;
            }
            unsigned char *decrypted_resp = (unsigned char*)malloc(hex_bytes_len + 1);
            rc4_crypt((const unsigned char*)rc4_key, (int)strlen(rc4_key), hex_bytes, decrypted_resp, hex_bytes_len);
            decrypted_resp[hex_bytes_len] = '\0';

            snprintf(result_json, sizeof(result_json), "%s", (char*)decrypted_resp);

            free(hex_bytes);
            free(decrypted_resp);
        } else {
            snprintf(result_json, sizeof(result_json), "{\"code\":-1,\"msg\":\"Native C 连接微验网络失败\"}");
        }

        free(encrypted);
        free(encrypted_hex);
    } else {
        snprintf(result_json, sizeof(result_json), "{\"code\":0,\"msg\":\"Native C Provider Ready\"}");
    }

    (*env)->ReleaseStringUTFChars(env, card_str, card);
    (*env)->ReleaseStringUTFChars(env, imei_str, imei);

    return (*env)->NewStringUTF(env, result_json);
}

JNIEXPORT jstring JNICALL
Java_XiYue_SiyoX_data_NativeVerify_nativeFetchNotice(
        JNIEnv *env,
        jclass clazz,
        jint verify_type) {

    char result_json[2048] = { 0 };

    if (verify_type == 2) {
        // 微验公告
        const char *host = "wy.llua.cn";
        const char *app_id = "10000";
        const char *rc4_key = "ElFlF870vDk88gef";

        char path[256];
        snprintf(path, sizeof(path), "/api/?id=notice");
        char post_body[256];
        snprintf(post_body, sizeof(post_body), "app=%s", app_id);

        char raw_resp[4096] = { 0 };
        int ret = native_http_post(host, 80, path, post_body, raw_resp, sizeof(raw_resp));

        if (ret > 0) {
            int resp_len = (int)strlen(raw_resp);
            int hex_bytes_len = resp_len / 2;
            unsigned char *hex_bytes = (unsigned char*)malloc(hex_bytes_len);
            for (int i = 0; i < hex_bytes_len; i++) {
                unsigned int byte_val;
                sscanf(&raw_resp[i * 2], "%02x", &byte_val);
                hex_bytes[i] = (unsigned char)byte_val;
            }
            unsigned char *decrypted_resp = (unsigned char*)malloc(hex_bytes_len + 1);
            rc4_crypt((const unsigned char*)rc4_key, (int)strlen(rc4_key), hex_bytes, decrypted_resp, hex_bytes_len);
            decrypted_resp[hex_bytes_len] = '\0';

            snprintf(result_json, sizeof(result_json), "%s", (char*)decrypted_resp);

            free(hex_bytes);
            free(decrypted_resp);
        } else {
            snprintf(result_json, sizeof(result_json), "{\"code\":-1,\"msg\":\"获取公告失败\"}");
        }
    } else {
        snprintf(result_json, sizeof(result_json), "{\"code\":200,\"msg\":{\"title\":\"SiyoX 官方公告\",\"content\":\"欢迎使用 SiyoX 模块！请输入授权卡密激活后开始体验。\"}}");
    }

    return (*env)->NewStringUTF(env, result_json);
}
