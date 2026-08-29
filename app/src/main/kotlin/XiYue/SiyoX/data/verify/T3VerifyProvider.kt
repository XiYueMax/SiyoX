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

class T3VerifyProvider : IVerifyProvider {

    override val providerName: String = "T3 网络验证 (T3)"

    override fun initProvider(androidId: String) {
        // T3 HTTP API initialization
    }

    override fun verifyCard(cardKey: String, androidId: String, callback: (VerifyResult) -> Unit) {
        thread {
            try {
                val host = SiyoXConfig.T3Config.API_HOST
                val appKey = SiyoXConfig.T3Config.APP_KEY
                val loginCode = SiyoXConfig.T3Config.LOGIN_CODE

                val time = (System.currentTimeMillis() / 1000).toString()
                val sign = md5("appkey=$appKey&card=$cardKey&imei=$androidId&t=$time").lowercase()

                val url = if (host.endsWith("/")) "${host}api/login" else "$host/api/login"
                val body = "appkey=$appKey&code=$loginCode&card=$cardKey&imei=$androidId&t=$time&sign=$sign"

                val responseStr = httpPost(url, body)
                val json = JSONObject(responseStr)

                val code = json.optInt("code", json.optInt("status", -1))
                val msg = json.optString("msg", json.optString("message", ""))

                if (code == 200 || code == 1 || json.optBoolean("success", false)) {
                    val data = json.optJSONObject("data")
                    val endTime = data?.optLong("end_time", 0L) ?: 0L
                    callback(VerifyResult(true, "T3验证成功", endTime * 1000L))
                } else {
                    callback(VerifyResult(false, if (msg.isNotBlank()) msg else "T3验证失败", 0L))
                }

            } catch (e: Exception) {
                callback(VerifyResult(false, "T3网络异常: ${e.message}", 0L))
            }
        }
    }

    override fun fetchNotice(callback: (NoticeResult) -> Unit) {
        thread {
            try {
                val host = SiyoXConfig.T3Config.API_HOST
                val appKey = SiyoXConfig.T3Config.APP_KEY
                val noticeCode = SiyoXConfig.T3Config.NOTICE_CODE

                val url = if (host.endsWith("/")) "${host}api/notice" else "$host/api/notice"
                val body = "appkey=$appKey&code=$noticeCode"

                val responseStr = httpPost(url, body)
                val json = JSONObject(responseStr)
                val noticeText = json.optString("notice", json.optString("data", ""))

                if (noticeText.isNotBlank()) {
                    callback(NoticeResult(true, "T3官方公告", noticeText))
                } else {
                    callback(NoticeResult(false, "官方公告", "欢迎使用 SiyoX 模块！请输入授权卡密激活后开始体验。"))
                }
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
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")

        conn.outputStream.use { os: OutputStream ->
            os.write(body.toByteArray(Charsets.UTF_8))
            os.flush()
        }

        val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
        val response = reader.readText().trim()
        reader.close()
        return response
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
