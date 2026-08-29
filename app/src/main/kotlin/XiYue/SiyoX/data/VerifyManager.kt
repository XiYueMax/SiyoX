// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import XiYue.SiyoX.SiyoXConfig
import XiYue.SiyoX.data.verify.EpicVerifyProvider
import XiYue.SiyoX.data.verify.IVerifyProvider
import XiYue.SiyoX.data.verify.T3VerifyProvider
import XiYue.SiyoX.data.verify.WeiYanVerifyProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VerifyManager private constructor(private val appContext: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isVerified = MutableStateFlow(false)
    val isVerified: StateFlow<Boolean> = _isVerified.asStateFlow()

    private val _expireTimestamp = MutableStateFlow(0L)
    val expireTimestamp: StateFlow<Long> = _expireTimestamp.asStateFlow()

    private val _statusMessage = MutableStateFlow("未验证")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _noticeTitle = MutableStateFlow("官方公告")
    val noticeTitle: StateFlow<String> = _noticeTitle.asStateFlow()

    private val _noticeContent = MutableStateFlow("欢迎使用 SiyoX 模块！请输入授权卡密激活后开始体验。")
    val noticeContent: StateFlow<String> = _noticeContent.asStateFlow()

    private var activeProvider: IVerifyProvider = createProvider()

    init {
        val androidId = getAndroidId()
        activeProvider.initProvider(androidId)
    }

    private fun createProvider(): IVerifyProvider {
        return when (SiyoXConfig.CURRENT_VERIFY_TYPE) {
            SiyoXConfig.VerifyType.EPIC -> EpicVerifyProvider()
            SiyoXConfig.VerifyType.T3 -> T3VerifyProvider()
            SiyoXConfig.VerifyType.WEIYAN -> WeiYanVerifyProvider()
        }
    }

    fun getActiveProviderName(): String {
        return activeProvider.providerName
    }

    @SuppressLint("HardwareIds")
    fun getAndroidId(): String {
        return try {
            Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "unknown_android_id"
        } catch (e: Exception) {
            "unknown_android_id"
        }
    }

    fun loadSoftwareNotice(onComplete: ((Boolean, String) -> Unit)? = null) {
        activeProvider.fetchNotice { result ->
            mainHandler.post {
                if (result.success) {
                    _noticeTitle.value = result.title
                    _noticeContent.value = result.content
                    onComplete?.invoke(true, "公告获取成功")
                } else {
                    onComplete?.invoke(false, "公告获取失败")
                }
            }
        }
    }

    fun verifyCard(cardKey: String, onResult: (Boolean, String) -> Unit) {
        if (cardKey.isBlank()) {
            onResult(false, "卡密不能为空")
            return
        }

        _isLoading.value = true
        val androidId = getAndroidId()

        activeProvider.verifyCard(cardKey.trim(), androidId) { result ->
            mainHandler.post {
                _isLoading.value = false
                if (result.success) {
                    _isVerified.value = true
                    _expireTimestamp.value = result.expireTime
                    AppSettings.get().card = cardKey.trim()
                    AppSettings.get().expireTime = result.expireTime
                    _statusMessage.value = "已激活 (到期: ${formatDate(result.expireTime)})"
                    onResult(true, "验证成功\n到期时间: ${formatDate(result.expireTime)}")
                } else {
                    _isVerified.value = false
                    _statusMessage.value = "验证失败: ${result.message}"
                    onResult(false, result.message)
                }
            }
        }
    }

    fun handleEvent(context: Context, eventType: Int, value: String?) {
        if (value.isNullOrBlank()) return
        try {
            when (eventType) {
                1 -> {
                    // Open URL
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(value)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                2 -> {
                    // Open QQ contact
                    val url = "mqqwpa://im/chat?chat_type=wpa&uin=$value"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                3 -> {
                    // Open QQ Group
                    val url = "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=$value&card_type=group&source=qrcode"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                else -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(value)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "无法唤起应用: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        @Volatile
        private var instance: VerifyManager? = null

        fun init(context: Context): VerifyManager {
            return instance ?: synchronized(this) {
                instance ?: VerifyManager(context.applicationContext ?: context).also { instance = it }
            }
        }

        fun get(): VerifyManager {
            return instance ?: throw IllegalStateException("VerifyManager not initialized")
        }

        fun formatDate(ms: Long): String {
            if (ms <= 0) return "永不到期 / 未激活"
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(ms))
        }
    }
}
