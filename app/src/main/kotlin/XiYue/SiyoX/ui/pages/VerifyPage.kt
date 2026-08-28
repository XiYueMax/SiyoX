// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.ui.pages

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import XiYue.SiyoX.data.AppSettings
import XiYue.SiyoX.data.VerifyManager

@Composable
fun VerifyPage(
    isOverlayMode: Boolean = false,
    onVerifiedSuccess: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val appSettings = remember { AppSettings.get() }
    val verifyManager = remember { VerifyManager.get() }

    val isVerified by verifyManager.isVerified.collectAsState()
    val statusMsg by verifyManager.statusMessage.collectAsState()
    val isLoading by verifyManager.isLoading.collectAsState()

    var cardInput by remember { mutableStateOf(appSettings.card) }
    var nightVision by remember { mutableStateOf(appSettings.isNightVisionEnabled) }
    var xray by remember { mutableStateOf(appSettings.isXrayEnabled) }

    var showNoticeDialog by remember { mutableStateOf(false) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var showQueryDialog by remember { mutableStateOf(false) }
    var showUnbindDialog by remember { mutableStateOf(false) }
    var showPassDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        verifyManager.loadSoftwareConfig { success, _ ->
            if (success) {
                // Check if notice is updated
                if (verifyManager.isNoticeChanged(verifyManager.noticeConfig)) {
                    showNoticeDialog = true
                }
                // Check if update is available
                val ver = verifyManager.versionConfig
                if (ver != null && ver.hasUpdate && ver.version != appSettings.ignoredVersion) {
                    showUpgradeDialog = true
                }
            }
        }

        // Auto verify if card exists
        if (appSettings.autoVerify && cardInput.isNotBlank() && !isVerified) {
            verifyManager.verifyCard(cardInput) { ok, _ ->
                if (ok) {
                    onVerifiedSuccess?.invoke()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = if (isOverlayMode) "SiyoX 网络验证" else "SiyoX 管理器"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SiyoX",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "作用域: com.netease.x19",
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isVerified) Color(0xFF34C759).copy(alpha = 0.15f)
                                    else Color(0xFFFF9500).copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (isVerified) "已授权" else "未授权",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isVerified) Color(0xFF248A3D) else Color(0xFFC97500)
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = statusMsg,
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }
            }

            // Card Input and Verification Section
            SmallTitle(text = "卡密授权")

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val placeholder = verifyManager.verifyConfig?.cardPlaceholder ?: "请输入授权卡密"

                    TextField(
                        value = cardInput,
                        onValueChange = { cardInput = it },
                        label = placeholder,
                        useLabelAsPlaceholder = true,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                color = MiuixTheme.colorScheme.surfaceContainer
                            ),
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = cm?.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                    if (text.isNotBlank()) {
                                        cardInput = text.trim()
                                        Toast.makeText(context, "已粘贴", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Text(text = "粘贴卡密")
                        }

                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                color = MiuixTheme.colorScheme.surfaceContainer
                            ),
                            onClick = {
                                cardInput = ""
                            }
                        ) {
                            Text(text = "清空")
                        }
                    }

                    // Main Verification Button
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && cardInput.isNotBlank(),
                        colors = ButtonDefaults.primaryButtonColors(),
                        onClick = {
                            verifyManager.verifyCard(cardInput) { ok, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (ok) {
                                    onVerifiedSuccess?.invoke()
                                }
                            }
                        }
                    ) {
                        Text(text = if (isLoading) "正在验证中…" else if (isVerified) "验证通过 (点击重验)" else "立即验证")
                    }

                    // Secondary actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "卡密查询",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { showQueryDialog = true }
                                .padding(4.dp)
                        )

                        Text(
                            text = "解绑设备",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { showUnbindDialog = true }
                                .padding(4.dp)
                        )

                        Text(
                            text = "密码通道",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { showPassDialog = true }
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Cloud Services & Actions
            SmallTitle(text = "云端服务")

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                color = MiuixTheme.colorScheme.surfaceContainer
                            ),
                            onClick = {
                                if (verifyManager.noticeConfig != null) {
                                    showNoticeDialog = true
                                } else {
                                    verifyManager.loadSoftwareConfig { _, _ ->
                                        showNoticeDialog = true
                                    }
                                }
                            }
                        ) {
                            Text(text = "查看公告")
                        }

                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                color = MiuixTheme.colorScheme.surfaceContainer
                            ),
                            onClick = {
                                verifyManager.loadSoftwareConfig { success, _ ->
                                    val ver = verifyManager.versionConfig
                                    if (ver != null && ver.hasUpdate) {
                                        showUpgradeDialog = true
                                    } else {
                                        Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Text(text = "检查更新")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                color = MiuixTheme.colorScheme.surfaceContainer
                            ),
                            onClick = {
                                verifyManager.handleEvent(context, 3, "1031891543")
                            }
                        ) {
                            Text(text = "官方群聊")
                        }

                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                color = MiuixTheme.colorScheme.surfaceContainer
                            ),
                            onClick = {
                                verifyManager.handleEvent(context, 1, "https://epic.t60.top/")
                            }
                        ) {
                            Text(text = "官方网站")
                        }
                    }
                }
            }

            // Minecraft In-game Function Toggles
            SmallTitle(text = "模块增强功能")

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "夜视增强 (NightVision)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "自动在游戏中保持高清夜视效果",
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }

                        Switch(
                            checked = nightVision,
                            onCheckedChange = {
                                nightVision = it
                                appSettings.isNightVisionEnabled = it
                            }
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "矿物透视 (X-Ray)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "高亮矿石并过滤石头方块",
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }

                        Switch(
                            checked = xray,
                            onCheckedChange = {
                                xray = it
                                appSettings.isXrayEnabled = it
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Dialogs
        NoticeDialog(
            show = showNoticeDialog,
            notice = verifyManager.noticeConfig,
            onDismiss = { showNoticeDialog = false }
        )

        UpgradeDialog(
            show = showUpgradeDialog,
            version = verifyManager.versionConfig,
            onDismiss = { showUpgradeDialog = false }
        )

        CardQueryDialog(
            show = showQueryDialog,
            onDismiss = { showQueryDialog = false }
        )

        CardUnbindDialog(
            show = showUnbindDialog,
            onDismiss = { showUnbindDialog = false }
        )

        PassDialog(
            show = showPassDialog,
            onDismiss = { showPassDialog = false },
            onSuccess = {
                Toast.makeText(context, "密码验证成功，已解锁", Toast.LENGTH_SHORT).show()
                onVerifiedSuccess?.invoke()
            }
        )
    }
}
