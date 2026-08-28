// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.data

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import epic.verify.api.EpicVerifyException
import epic.verify.api.EpicVerifySDK
import epic.verify.api.Resp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VerifyManager private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var sdk: EpicVerifySDK? = null

    private val _isVerified = MutableStateFlow(false)
    val isVerified: StateFlow<Boolean> = _isVerified.asStateFlow()

    private val _expireTimestamp = MutableStateFlow(0L)
    val expireTimestamp: StateFlow<Long> = _expireTimestamp.asStateFlow()

    private val _statusMessage = MutableStateFlow("未验证")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    var verifyConfig: EpicVerifySDK.VerifyConfig? = null
        private set
    var noticeConfig: EpicVerifySDK.Notice? = null
        private set
    var versionConfig: EpicVerifySDK.Version? = null
        private set
    var passConfig: EpicVerifySDK.Pass? = null
        private set

    init {
        setupSdk()
    }

    private fun setupSdk() {
        try {
            sdk = EpicVerifySDK(HOSTS, PORT, APP_KEY).apply {
                setDeviceId(getAndroidId())
                setPackageName(appContext.packageName)
                setAppVersion(getAppVersionCode())
                setOnHeartbeatListener { resp ->
                    mainHandler.post {
                        if (resp.isSuccess) {
                            val remain = timeRemaining
                            _statusMessage.value = "心跳正常 (剩余 ${formatRemaining(remain)})"
                        } else {
                            _statusMessage.value = "心跳失败: ${resp.msg}"
                            _isVerified.value = false
                        }
                    }
                }
            }
        } catch (e: EpicVerifyException) {
            _statusMessage.value = "SDK 初始化异常: ${e.message}"
        }
    }

    @SuppressLint("HardwareIds")
    private fun getAndroidId(): String {
        return try {
            Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "unknown_device_${System.currentTimeMillis()}"
        } catch (e: Exception) {
            "unknown_device_${System.currentTimeMillis()}"
        }
    }

    private fun getAppVersionCode(): Int {
        return try {
            val pInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    fun loadSoftwareConfig(onComplete: ((Boolean, String) -> Unit)? = null) {
        scope.launch {
            try {
                val currentSdk = sdk ?: return@launch
                val resp = currentSdk.softwareConfig
                if (resp.isSuccess) {
                    verifyConfig = currentSdk.getVerifyConfig(resp)
                    noticeConfig = currentSdk.getSoftwareNotice(resp)
                    versionConfig = currentSdk.getSoftwareLatestVersion(getAppVersionCode().toString(), resp)
                    passConfig = currentSdk.getSoftwarePass(resp)

                    mainHandler.post {
                        onComplete?.invoke(true, "配置获取成功")
                    }
                } else {
                    mainHandler.post {
                        onComplete?.invoke(false, resp.msg ?: "获取配置失败")
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    onComplete?.invoke(false, e.message ?: "网络请求异常")
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
        scope.launch {
            try {
                val currentSdk = sdk ?: throw EpicVerifyException("SDK 未就绪")
                currentSdk.setCard(cardKey.trim())
                val resp: Resp = currentSdk.cardVerify()

                mainHandler.post {
                    _isLoading.value = false
                    if (resp.isSuccess) {
                        val expireMs = try {
                            resp.data?.getLong("expire") ?: 0L
                        } catch (e: Exception) {
                            0L
                        }
                        _isVerified.value = true
                        _expireTimestamp.value = expireMs
                        AppSettings.get().card = cardKey.trim()
                        AppSettings.get().expireTime = expireMs
                        _statusMessage.value = "已激活 (到期: ${formatDate(expireMs)})"
                        onResult(true, "验证成功\n到期时间: ${formatDate(expireMs)}")
                    } else {
                        _isVerified.value = false
                        _statusMessage.value = "验证失败: ${resp.msg}"
                        onResult(false, resp.msg ?: "验证失败")
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    _isLoading.value = false
                    _statusMessage.value = "验证异常: ${e.message}"
                    onResult(false, e.message ?: "验证出现异常")
                }
            }
        }
    }

    fun verifyPassword(password: String, onResult: (Boolean, String) -> Unit) {
        if (password.isBlank()) {
            onResult(false, "密码不能为空")
            return
        }

        val pass = passConfig
        if (pass != null && pass.passType == 0) {
            // 本地比对
            val ok = pass.pass != null && pass.pass == password
            if (ok) {
                _isVerified.value = true
                _statusMessage.value = "密码通道验证成功 (本地)"
                onResult(true, "密码验证成功")
            } else {
                onResult(false, "密码错误")
            }
            return
        }

        _isLoading.value = true
        scope.launch {
            try {
                val currentSdk = sdk ?: throw EpicVerifyException("SDK 未就绪")
                currentSdk.setCard(password.trim())
                val resp: Resp = currentSdk.cardPass()

                mainHandler.post {
                    _isLoading.value = false
                    if (resp.isSuccess) {
                        val expireMs = resp.data?.getLong("expire") ?: 0L
                        _isVerified.value = true
                        _expireTimestamp.value = expireMs
                        _statusMessage.value = "已通过密码通道进入"
                        onResult(true, "密码验证成功")
                    } else {
                        onResult(false, resp.msg ?: "密码错误")
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    _isLoading.value = false
                    onResult(false, e.message ?: "密码校验异常")
                }
            }
        }
    }

    fun queryCard(cardKey: String, onResult: (Boolean, String) -> Unit) {
        if (cardKey.isBlank()) {
            onResult(false, "请输入要查询的卡密")
            return
        }

        scope.launch {
            try {
                val currentSdk = sdk ?: throw EpicVerifyException("SDK 未就绪")
                currentSdk.setCard(cardKey.trim())
                val resp: Resp = currentSdk.cardQuery()

                mainHandler.post {
                    if (resp.isSuccess) {
                        val usable = resp.data?.getBoolean("usable") ?: true
                        val value = resp.data?.getInt("value") ?: 0
                        val mac = resp.data?.optString("mac", "无") ?: "无"
                        val expireTime = resp.data?.getLong("expireTime") ?: 0L
                        val info = "卡密状态: ${if (usable) "正常" else "冻结"}\n面值: $value\n绑定机器码: $mac\n到期时间: ${formatDate(expireTime)}"
                        onResult(true, info)
                    } else {
                        onResult(false, resp.msg ?: "查询失败")
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    onResult(false, e.message ?: "查询异常")
                }
            }
        }
    }

    fun unbindCard(cardKey: String, onResult: (Boolean, String) -> Unit) {
        if (cardKey.isBlank()) {
            onResult(false, "请输入要解绑的卡密")
            return
        }

        scope.launch {
            try {
                val currentSdk = sdk ?: throw EpicVerifyException("SDK 未就绪")
                currentSdk.setCard(cardKey.trim())
                val resp: Resp = currentSdk.cardUnbind()


                mainHandler.post {
                    if (resp.isSuccess) {
                        onResult(true, "解绑成功")
                    } else {
                        onResult(false, resp.msg ?: "解绑失败")
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    onResult(false, e.message ?: "解绑异常")
                }
            }
        }
    }

    fun handleEvent(context: Context, event: Int, value: String?) {
        val v = value ?: ""
        when (event) {
            0 -> { // 退出程序
                android.os.Process.killProcess(android.os.Process.myPid())
            }
            1 -> { // 打开网页
                openBrowser(context, v)
            }
            2 -> { // 跳转 QQ
                openBrowser(context, "mqqwpa://im/chat?chat_type=wpa&uin=$v&version=1")
            }
            3 -> { // 跳转 QQ 群
                openBrowser(context, "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=$v&card_type=group&source=qrcode")
            }
            6 -> { // 打开应用
                if (v.isNotBlank()) {
                    val intent = context.packageManager.getLaunchIntentForPackage(v)
                    if (intent != null) {
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "未找到目标应用: $v", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            7, 8, 9 -> { // 分享
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, v)
                    if (event == 8) setPackage("com.tencent.mobileqq")
                    if (event == 9) setPackage("com.tencent.mm")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(Intent.createChooser(shareIntent, "分享").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (e: Exception) {
                    Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            10 -> { // 复制
                copyToClipboard(context, v)
            }
        }
    }

    private fun openBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "打开链接失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(context: Context, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("SiyoX", text))
        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    fun isNoticeChanged(notice: EpicVerifySDK.Notice?): Boolean {
        if (notice == null) return false
        val currentMd5 = md5(notice.raw ?: "")
        val lastMd5 = AppSettings.get().announcementMd5
        if (lastMd5.isNotBlank() && lastMd5 == currentMd5) return false
        AppSettings.get().announcementMd5 = currentMd5
        return true
    }

    private fun md5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            input
        }
    }

    companion object {
        const val APP_KEY = "iJQfzsjaI5IHW7W6VKjDXmF7gMxpSy0s"
        val HOSTS = arrayOf("epic.z74d.top", "gl.t60.top", "test.t60.top", "epic.t5x.cc")
        const val PORT = 5000

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

        fun formatRemaining(seconds: Long): String {
            if (seconds <= 0) return "已到期"
            val days = seconds / 86400
            val hours = (seconds % 86400) / 3600
            val minutes = (seconds % 3600) / 60
            val sec = seconds % 60
            return when {
                days > 0 -> "${days}天 ${hours}小时"
                hours > 0 -> "${hours}小时 ${minutes}分"
                else -> "${minutes}分 ${sec}秒"
            }
        }
    }
}
