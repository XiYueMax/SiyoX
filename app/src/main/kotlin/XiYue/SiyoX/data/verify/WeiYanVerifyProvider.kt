// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.data.verify

import XiYue.SiyoX.SiyoXConfig
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.concurrent.thread

class WeiYanVerifyProvider : IVerifyProvider {

    override val providerName: String = "微验验证 (WeiYan)"

    override fun initProvider(androidId: String) {
        // No persistent socket needed for WeiYan
    }

    override fun verifyCard(cardKey: String, androidId: String, callback: (VerifyResult) -> Unit) {
        thread {
            try {
                val host = SiyoXConfig.WeiYanConfig.API_HOST
                val appId = SiyoXConfig.WeiYanConfig.APP_ID
                val appKey = SiyoXConfig.WeiYanConfig.APP_KEY
                val rc4Key = SiyoXConfig.WeiYanConfig.RC4_KEY

                val time = (System.currentTimeMillis() / 1000).toInt()
                val value = "$time${(1000..9999).random()}"
                val signStr = "kami=$cardKey&markcode=$androidId&t=$time&$appKey"
                val signMd5 = md5(signStr).lowercase()

                val rawData = "kami=$cardKey&markcode=$androidId&t=$time&sign=$signMd5&value=$value"
                val encryptedDataHex = rc4EncryptHex(rawData, rc4Key)

                val requestUrl = "http://$host/api/?id=kmlogon&app=$appId"
                val postBody = "&data=$encryptedDataHex"

                val responseRaw = httpPost(requestUrl, postBody)
                val decryptedJson = rc4DecryptHex(responseRaw, rc4Key)

                val json = JSONObject(decryptedJson)
                val code = json.optInt("code", -1)
                val msg = json.optString("msg", "")

                if (code == 200) {
                    val msgObj = json.optJSONObject("msg")
                    val vipTime = msgObj?.optLong("vip", 0L) ?: 0L
                    val expireMs = if (vipTime > 0) vipTime * 1000L else 0L
                    callback(VerifyResult(true, "微验登录成功", expireMs))
                } else {
                    callback(VerifyResult(false, if (msg.isNotBlank()) msg else "卡密验证失败 (Code: $code)", 0L))
                }

            } catch (e: Exception) {
                callback(VerifyResult(false, "微验连接异常: ${e.message}", 0L))
            }
        }
    }

    override fun fetchNotice(callback: (NoticeResult) -> Unit) {
        thread {
            try {
                val host = SiyoXConfig.WeiYanConfig.API_HOST
                val appId = SiyoXConfig.WeiYanConfig.APP_ID
                val rc4Key = SiyoXConfig.WeiYanConfig.RC4_KEY

                val requestUrl = "http://$host/api/?id=notice"
                val postBody = "app=$appId"

                val responseRaw = httpPost(requestUrl, postBody)
                val decryptedJson = rc4DecryptHex(responseRaw, rc4Key)

                val json = JSONObject(decryptedJson)
                val code = json.optInt("code", -1)
                if (code == 200) {
                    val msgObj = json.optJSONObject("msg")
                    val appGg = msgObj?.optString("app_gg", "") ?: ""
                    if (appGg.isNotBlank()) {
                        callback(NoticeResult(true, "微验官方公告", appGg))
                        return@thread
                    }
                }
                callback(NoticeResult(false, "官方公告", "欢迎使用 SiyoX 模块！请输入授权卡密激活后开始体验。"))
            } catch (e: Exception) {
                callback(NoticeResult(false, "官方公告", "欢迎使用 SiyoX 模块！请输入授权卡密激活后开始体验。"))
            }
        }
    }

    private fun httpPost(urlStr: String, body: String): String {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.doOutput = true
        conn.doInput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)")

        conn.outputStream.use { os: OutputStream ->
            os.write(body.toByteArray(Charsets.UTF_8))
            os.flush()
        }

        val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
        val response = reader.readText().trim()
        reader.close()
        return response
    }

    private fun rc4EncryptHex(data: String, key: String): String {
        val bytes = rc4(data.toByteArray(Charsets.UTF_8), key.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun rc4DecryptHex(hexData: String, key: String): String {
        val trimmed = hexData.trim()
        val bytes = ByteArray(trimmed.length / 2)
        for (i in bytes.indices) {
            bytes[i] = trimmed.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        val decrypted = rc4(bytes, key.toByteArray(Charsets.UTF_8))
        return String(decrypted, Charsets.UTF_8)
    }

    private fun rc4(input: ByteArray, key: ByteArray): ByteArray {
        val s = IntArray(256) { it }
        var j = 0
        for (i in 0 until 256) {
            j = (j + s[i] + (key[i % key.size].toInt() and 0xFF)) % 256
            val tmp = s[i]; s[i] = s[j]; s[j] = tmp
        }
        var i = 0
        j = 0
        val out = ByteArray(input.size)
        for (k in input.indices) {
            i = (i + 1) % 256
            j = (j + s[i]) % 256
            val tmp = s[i]; s[i] = s[j]; s[j] = tmp
            out[k] = (input[k].toInt() xor s[(s[i] + s[j]) % 256]).toByte()
        }
        return out
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
