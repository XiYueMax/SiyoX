// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.ui.pages

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import XiYue.SiyoX.data.AppSettings
import XiYue.SiyoX.data.VerifyManager
import kotlin.math.roundToInt

@Composable
fun SiyoXOverlayView(
    onDismissRequest: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val appSettings = remember { AppSettings.get() }
    val verifyManager = remember { VerifyManager.get() }

    val isVerified by verifyManager.isVerified.collectAsState()
    val statusMsg by verifyManager.statusMessage.collectAsState()
    val isLoading by verifyManager.isLoading.collectAsState()

    // Panel visibility: on launch, if not verified, show panel automatically
    var isPanelOpen by remember { mutableStateOf(!isVerified) }

    var cardInput by remember { mutableStateOf(appSettings.card) }
    var nightVision by remember { mutableStateOf(appSettings.isNightVisionEnabled) }
    var xray by remember { mutableStateOf(appSettings.isXrayEnabled) }

    // Floating Ball position
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    var offsetX by remember { mutableFloatStateOf(40f) }
    var offsetY by remember { mutableFloatStateOf(screenHeightPx * 0.25f) }

    LaunchedEffect(Unit) {
        verifyManager.loadSoftwareConfig { success, _ ->
            // Config loaded
        }

        // Auto verify if card key exists
        if (appSettings.autoVerify && cardInput.isNotBlank() && !isVerified) {
            verifyManager.verifyCard(cardInput) { ok, _ ->
                if (ok) {
                    // Auto verify success
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Floating Ball (visible when panel is closed)
        AnimatedVisibility(
            visible = !isPanelOpen,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .size(54.dp)
                    .shadow(10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0A84FF),
                                Color(0xFF0056B3)
                            )
                        )
                    )
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newX = (offsetX + dragAmount.x).coerceIn(0f, screenWidthPx - 54.dp.toPx())
                            val newY = (offsetY + dragAmount.y).coerceIn(0f, screenHeightPx - 54.dp.toPx())
                            offsetX = newX
                            offsetY = newY
                        }
                    }
                    .clickable {
                        isPanelOpen = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Siyo",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "X",
                        color = Color(0xFFFFCC00),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // 2. Full In-Game SiyoX Function Panel (visible when expanded)
        AnimatedVisibility(
            visible = isPanelOpen,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut() + scaleOut(targetScale = 0.92f)
        ) {
            // Semi-transparent scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable {
                        // Allow closing only if verified
                        if (isVerified) {
                            isPanelOpen = false
                        } else {
                            Toast.makeText(context, "请先输入卡密完成网络验证", Toast.LENGTH_SHORT).show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Main Dialog Container
                Card(
                    modifier = Modifier
                        .widthIn(min = 320.dp, max = 460.dp)
                        .fillMaxWidth(0.92f)
                        .heightIn(max = 620.dp)
                        .clickable(enabled = false) {} // Prevent click-through to scrim
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Top Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "SiyoX",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixTheme.colorScheme.primary
                                )
                                Text(
                                    text = "功能面板",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                            }

                            // Close / Minimize Button
                            if (isVerified) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MiuixTheme.colorScheme.surfaceContainer)
                                        .clickable { isPanelOpen = false }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "收起悬浮球",
                                        fontSize = 12.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        // Status Info Card
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "作用域: com.netease.x19",
                                        fontSize = 12.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = statusMsg,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MiuixTheme.colorScheme.onSurface
                                    )
                                }

                                // Status Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isVerified) Color(0xFF34C759)
                                            else Color(0xFFFF9500)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = if (isVerified) "已授权" else "未授权",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White // Requirement: White text on colored background
                                    )
                                }
                            }
                        }

                        // 6. 公告栏 (Notice Board) - Flush with card left edge
                        Text(
                            text = "公告栏",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            modifier = Modifier.padding(start = 0.dp) // Flush left
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val notice = verifyManager.noticeConfig
                                val noticeTitle = notice?.title?.takeIf { it.isNotBlank() } ?: "官方公告"
                                val noticeContent = notice?.content?.takeIf { it.isNotBlank() }
                                    ?: "欢迎使用 SiyoX 注入辅助模块！请输入授权卡密激活后开始体验。"

                                Text(
                                    text = noticeTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixTheme.colorScheme.primary
                                )

                                Text(
                                    text = noticeContent,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // 4. 卡密授权 (Card Key Input) - Subtitle flush left
                        Text(
                            text = "卡密授权",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            modifier = Modifier.padding(start = 0.dp) // Flush left
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                        Text(
                                            text = "粘贴卡密",
                                            color = MiuixTheme.colorScheme.onSurface
                                        )
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
                                        Text(
                                            text = "清空",
                                            color = MiuixTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // 7. 立即验证按钮 (蓝色背景下文字为白色)
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isLoading && cardInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        color = Color(0xFF0A84FF) // Blue background
                                    ),
                                    onClick = {
                                        verifyManager.verifyCard(cardInput) { ok, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            if (ok) {
                                                isPanelOpen = false
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        text = if (isLoading) "正在验证中…" else if (isVerified) "验证通过 (点击重新验证)" else "立即验证",
                                        color = Color.White, // Requirement: White text on blue background
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }

                        // 5. 模块增强功能 (Minecraft Enhancements) - Subtitle flush left
                        Text(
                            text = "模块增强功能",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            modifier = Modifier.padding(start = 0.dp) // Flush left
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "夜视增强 (NightVision)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MiuixTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "自动在游戏中保持高清夜视效果",
                                            fontSize = 11.sp,
                                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                                        )
                                    }

                                    Switch(
                                        enabled = isVerified,
                                        checked = nightVision && isVerified,
                                        onCheckedChange = {
                                            if (!isVerified) {
                                                Toast.makeText(context, "请先验证卡密激活模块", Toast.LENGTH_SHORT).show()
                                                return@Switch
                                            }
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
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MiuixTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "高亮矿石并过滤无效方块",
                                            fontSize = 11.sp,
                                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                                        )
                                    }

                                    Switch(
                                        enabled = isVerified,
                                        checked = xray && isVerified,
                                        onCheckedChange = {
                                            if (!isVerified) {
                                                Toast.makeText(context, "请先验证卡密激活模块", Toast.LENGTH_SHORT).show()
                                                return@Switch
                                            }
                                            xray = it
                                            appSettings.isXrayEnabled = it
                                        }
                                    )
                                }
                            }
                        }

                        // 云端服务
                        Text(
                            text = "云端服务",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            modifier = Modifier.padding(start = 0.dp) // Flush left
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
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
                                    Text(
                                        text = "官方群聊",
                                        color = MiuixTheme.colorScheme.onSurface,
                                        fontSize = 12.sp
                                    )
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
                                    Text(
                                        text = "官方网站",
                                        color = MiuixTheme.colorScheme.onSurface,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}
