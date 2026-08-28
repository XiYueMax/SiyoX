// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.ui.pages

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import epic.verify.api.EpicUpdater
import epic.verify.api.EpicVerifySDK
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import XiYue.SiyoX.data.AppSettings
import XiYue.SiyoX.data.VerifyManager

@Composable
fun NoticeDialog(
    show: Boolean,
    notice: EpicVerifySDK.Notice?,
    onDismiss: () -> Unit
) {
    if (notice == null) return
    val context = LocalContext.current

    WindowDialog(
        show = show,
        title = if (!notice.title.isNullOrBlank()) notice.title else "公告通知",
        summary = if (!notice.content.isNullOrBlank()) notice.content else "暂无公告详情",
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (notice.cancel != null && notice.cancel.enabled()) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            color = MiuixTheme.colorScheme.surfaceContainer
                        ),
                        onClick = {
                            onDismiss()
                            VerifyManager.get().handleEvent(context, notice.cancel.event, notice.cancel.value)
                        }
                    ) {
                        Text(text = notice.cancel.text ?: "关闭")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (notice.extra != null && notice.extra.enabled()) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            color = MiuixTheme.colorScheme.surfaceContainer
                        ),
                        onClick = {
                            VerifyManager.get().handleEvent(context, notice.extra.event, notice.extra.value)
                        }
                    ) {
                        Text(text = notice.extra.text ?: "更多")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    colors = ButtonDefaults.buttonColors(
                        color = MiuixTheme.colorScheme.primary
                    ),

                    onClick = {
                        onDismiss()
                        if (notice.confirm != null && notice.confirm.enabled()) {
                            VerifyManager.get().handleEvent(context, notice.confirm.event, notice.confirm.value)
                        }
                    }
                ) {
                    Text(text = if (notice.confirm != null && notice.confirm.enabled()) notice.confirm.text else "我知道了")
                }
            }
        }
    }
}

@Composable
fun UpgradeDialog(
    show: Boolean,
    version: EpicVerifySDK.Version?,
    onDismiss: () -> Unit
) {
    if (version == null || !version.hasUpdate) return
    val context = LocalContext.current
    val force = !version.cancelEnable && !version.ignoreEnable

    val summaryText = buildString {
        append(version.content ?: "发现新版本可用")
        append("\n最新版本号: ${version.version}")
        if (force) append("\n\n（强制更新，更新后方可使用）")
    }

    WindowDialog(
        show = show,
        title = if (!version.title.isNullOrBlank()) version.title else "发现新版本",
        summary = summaryText,
        onDismissRequest = { if (!force) onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (version.ignoreEnable && !version.ignoreBtn.isNullOrBlank()) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            color = MiuixTheme.colorScheme.surfaceContainer
                        ),
                        onClick = {
                            AppSettings.get().ignoredVersion = version.version.toString()
                            onDismiss()
                        }

                    ) {
                        Text(text = version.ignoreBtn)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (!force && version.cancelEnable && !version.cancelBtn.isNullOrBlank()) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            color = MiuixTheme.colorScheme.surfaceContainer
                        ),
                        onClick = onDismiss
                    ) {
                        Text(text = version.cancelBtn)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    colors = ButtonDefaults.buttonColors(
                        color = MiuixTheme.colorScheme.primary
                    ),

                    onClick = {
                        if (version.updateType == 0 && context is Activity) {
                            EpicUpdater.downloadAndInstall(context, version.downloadUrl, version.version) { ok, msg ->
                                // Handle update result
                            }
                        } else {
                            VerifyManager.get().handleEvent(context, 1, version.downloadUrl)
                        }
                        if (!force) onDismiss()
                    }
                ) {
                    Text(text = if (!version.upgradeBtn.isNullOrBlank()) version.upgradeBtn else "立即更新")
                }
            }
        }
    }
}

@Composable
fun CardQueryDialog(
    show: Boolean,
    onDismiss: () -> Unit
) {
    var cardInput by remember { mutableStateOf(AppSettings.get().card) }
    var resultText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    WindowDialog(
        show = show,
        title = "卡密信息查询",
        summary = "输入卡密可查询到期时间、绑定机器码等详细状态",
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = cardInput,
                onValueChange = { cardInput = it },
                label = "请输入要查询的卡密",
                useLabelAsPlaceholder = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (resultText.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = resultText,
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        color = MiuixTheme.colorScheme.surfaceContainer
                    ),
                    onClick = onDismiss
                ) {
                    Text(text = "取消")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    enabled = !isLoading && cardInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        color = MiuixTheme.colorScheme.primary
                    ),

                    onClick = {
                        isLoading = true
                        VerifyManager.get().queryCard(cardInput) { success, info ->
                            isLoading = false
                            resultText = info
                        }
                    }
                ) {
                    Text(text = if (isLoading) "查询中…" else "查询")
                }
            }
        }
    }
}

@Composable
fun CardUnbindDialog(
    show: Boolean,
    onDismiss: () -> Unit
) {
    var cardInput by remember { mutableStateOf(AppSettings.get().card) }
    var resultText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    WindowDialog(
        show = show,
        title = "解绑设备",
        summary = "解绑后该卡密可重新在其他设备或新客户端上绑定使用",
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = cardInput,
                onValueChange = { cardInput = it },
                label = "请输入要解绑的卡密",
                useLabelAsPlaceholder = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (resultText.isNotBlank()) {
                Text(
                    text = resultText,
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        color = MiuixTheme.colorScheme.surfaceContainer
                    ),
                    onClick = onDismiss
                ) {
                    Text(text = "取消")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    enabled = !isLoading && cardInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        color = MiuixTheme.colorScheme.primary
                    ),

                    onClick = {
                        isLoading = true
                        VerifyManager.get().unbindCard(cardInput) { success, msg ->
                            isLoading = false
                            resultText = msg
                        }
                    }
                ) {
                    Text(text = if (isLoading) "解绑中…" else "确认解绑")
                }
            }
        }
    }
}

@Composable
fun PassDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var passInput by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    WindowDialog(
        show = show,
        title = "密码验证通道",
        summary = "输入内部访问密码或紧急密码直接授权",
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = passInput,
                onValueChange = { passInput = it },
                label = "请输入密码",
                useLabelAsPlaceholder = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (resultText.isNotBlank()) {
                Text(
                    text = resultText,
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        color = MiuixTheme.colorScheme.surfaceContainer
                    ),
                    onClick = onDismiss
                ) {
                    Text(text = "取消")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    enabled = !isLoading && passInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        color = MiuixTheme.colorScheme.primary
                    ),

                    onClick = {
                        isLoading = true
                        VerifyManager.get().verifyPassword(passInput) { success, msg ->
                            isLoading = false
                            resultText = msg
                            if (success) {
                                onSuccess()
                                onDismiss()
                            }
                        }
                    }
                ) {
                    Text(text = if (isLoading) "验证中…" else "验证进入")
                }
            }
        }
    }
}
