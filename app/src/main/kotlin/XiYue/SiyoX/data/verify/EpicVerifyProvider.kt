// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.data.verify

import XiYue.SiyoX.SiyoXConfig
import epic.verify.api.EpicVerifySDK
import epic.verify.api.Resp
import kotlin.concurrent.thread

class EpicVerifyProvider : IVerifyProvider {

    override val providerName: String = "摇光云 (EPIC)"
    private var sdk: EpicVerifySDK? = null

    override fun initProvider(androidId: String) {
        try {
            sdk = EpicVerifySDK(
                SiyoXConfig.EpicConfig.HOSTS,
                SiyoXConfig.EpicConfig.PORT,
                SiyoXConfig.EpicConfig.APP_KEY
            ).apply {
                setDeviceId(androidId)
                setPackageName(SiyoXConfig.TARGET_PACKAGE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun verifyCard(cardKey: String, androidId: String, callback: (VerifyResult) -> Unit) {
        thread {
            try {
                val currentSdk = sdk ?: EpicVerifySDK(
                    SiyoXConfig.EpicConfig.HOSTS,
                    SiyoXConfig.EpicConfig.PORT,
                    SiyoXConfig.EpicConfig.APP_KEY
                ).apply {
                    setDeviceId(androidId)
                    setPackageName(SiyoXConfig.TARGET_PACKAGE)
                    sdk = this
                }

                currentSdk.setCard(cardKey)
                val resp: Resp = currentSdk.cardVerify()

                if (resp.isSuccess) {
                    val expire = try {
                        resp.data?.getLong("expire") ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                    callback(VerifyResult(true, "验证成功", expire))
                } else {
                    callback(VerifyResult(false, resp.msg ?: "验证失败", 0L))
                }
            } catch (e: Exception) {
                callback(VerifyResult(false, "连接异常: ${e.message}", 0L))
            }
        }
    }

    override fun fetchNotice(callback: (NoticeResult) -> Unit) {
        thread {
            try {
                val currentSdk = sdk ?: return@thread
                val resp = currentSdk.softwareConfig
                if (resp.isSuccess) {
                    val notice = currentSdk.getSoftwareNotice(resp)
                    if (notice != null && notice.hasNotice()) {
                        val title = notice.title ?: "官方公告"
                        val content = notice.content ?: "欢迎使用 SiyoX 模块"
                        callback(NoticeResult(true, title, content))
                        return@thread
                    }
                }
                callback(NoticeResult(false, "官方公告", "欢迎使用 SiyoX 模块！请输入授权卡密激活后开始体验。"))
            } catch (e: Exception) {
                callback(NoticeResult(false, "官方公告", "欢迎使用 SiyoX 模块！请输入授权卡密激活后开始体验。"))
            }
        }
    }
}
