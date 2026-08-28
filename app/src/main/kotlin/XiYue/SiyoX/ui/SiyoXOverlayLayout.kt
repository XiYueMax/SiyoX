// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import XiYue.SiyoX.data.AppSettings
import XiYue.SiyoX.data.VerifyManager
import kotlin.math.abs

@SuppressLint("ViewConstructor")
class SiyoXOverlayLayout(private val activity: Activity) : FrameLayout(activity) {

    private val appSettings = AppSettings.get()
    private val verifyManager = VerifyManager.get()

    private var isPanelOpen = false

    // Views
    private lateinit var floatingBall: FrameLayout
    private lateinit var scrimView: FrameLayout
    private lateinit var panelCard: LinearLayout
    private lateinit var statusBadge: TextView
    private lateinit var statusDetailText: TextView
    private lateinit var noticeTitleText: TextView
    private lateinit var noticeContentText: TextView
    private lateinit var cardEditText: EditText
    private lateinit var verifyButton: Button
    private lateinit var loadingBar: ProgressBar
    private lateinit var switchNightVision: Switch
    private lateinit var switchXray: Switch
    private lateinit var btnMinimize: TextView

    // Floating Ball drag coordinates
    private var dX = 0f
    private var dY = 0f
    private var downRawX = 0f
    private var downRawY = 0f
    private val touchSlop = dp(6)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        isFocusable = false
        isFocusableInTouchMode = false

        initUI()
        setupListeners()
        loadCloudData()

        // If not verified, open panel immediately
        if (!verifyManager.isVerified.value) {
            openPanel()
        } else {
            closePanel()
        }
    }

    private fun initUI() {
        // 1. Scrim (Dark translucent background when panel is open)
        scrimView = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#80000000"))
            visibility = View.GONE
            setOnClickListener {
                if (verifyManager.isVerified.value) {
                    closePanel()
                } else {
                    Toast.makeText(context, "请先输入卡密完成网络验证", Toast.LENGTH_SHORT).show()
                }
            }
        }
        addView(scrimView)

        // 2. Main Modal Panel
        val panelWidth = dp(400).coerceAtMost((activity.resources.displayMetrics.widthPixels * 0.92f).toInt())
        val panelMaxHeight = (activity.resources.displayMetrics.heightPixels * 0.88f).toInt()

        val scrollView = ScrollView(context).apply {
            layoutParams = LayoutParams(panelWidth, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        panelCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dp(18), dp(16), dp(18), dp(18))
            background = createCardBg(Color.parseColor("#F9FAFC"), Color.parseColor("#E5E9F0"), dp(20))
            isClickable = true // Prevent clicks from passing to scrim
        }

        buildPanelContent(panelCard)
        scrollView.addView(panelCard)
        scrimView.addView(scrollView)

        // 3. Floating Ball
        buildFloatingBall()
    }

    private fun buildPanelContent(root: LinearLayout) {
        val dp12 = dp(12)
        val dp14 = dp(14)
        val dp8 = dp(8)

        // --- Top Bar ---
        val topBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        val titleContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleSiyoX = TextView(context).apply {
            text = "SiyoX "
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0A84FF"))
        }
        val titleSub = TextView(context).apply {
            text = "功能面板"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1C1C1E"))
        }
        titleContainer.addView(titleSiyoX)
        titleContainer.addView(titleSub)
        topBar.addView(titleContainer)

        btnMinimize = TextView(context).apply {
            text = "收起"
            textSize = 12f
            setTextColor(Color.parseColor("#8E8E93"))
            setPadding(dp8, dp(4), dp8, dp(4))
            background = createCardBg(Color.parseColor("#EBEBF0"), Color.TRANSPARENT, dp(8))
            setOnClickListener {
                if (verifyManager.isVerified.value) {
                    closePanel()
                } else {
                    Toast.makeText(context, "请先完成网络验证", Toast.LENGTH_SHORT).show()
                }
            }
        }
        topBar.addView(btnMinimize)
        root.addView(topBar)

        // Divider
        root.addView(createDivider())

        // --- Status Card ---
        val statusCard = createInnerCard()
        val statusLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp14, dp12, dp14, dp12)
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        val statusInfoLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        val textScope = TextView(context).apply {
            text = "作用域: com.netease.x19"
            textSize = 12f
            setTextColor(Color.parseColor("#8E8E93"))
        }
        statusDetailText = TextView(context).apply {
            text = "未验证"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1C1C1E"))
            setPadding(0, dp(2), 0, 0)
        }
        statusInfoLayout.addView(textScope)
        statusInfoLayout.addView(statusDetailText)
        statusLayout.addView(statusInfoLayout)

        statusBadge = TextView(context).apply {
            text = "未授权"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE) // Requirement: White text on badge
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = createCardBg(Color.parseColor("#FF9500"), Color.TRANSPARENT, dp(10))
        }
        statusLayout.addView(statusBadge)
        statusCard.addView(statusLayout)
        root.addView(statusCard)

        // --- 6. 公告栏 (Notice Board) ---
        root.addView(createSectionTitle("公告栏")) // Flush left

        val noticeCard = createInnerCard()
        val noticeLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp14, dp12, dp14, dp12)
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        noticeTitleText = TextView(context).apply {
            text = "官方公告"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0A84FF"))
        }
        noticeContentText = TextView(context).apply {
            text = "欢迎使用 SiyoX 注入辅助模块！请输入授权卡密激活后开始体验。"
            textSize = 12f
            setLineSpacing(dp(2).toFloat(), 1.1f)
            setTextColor(Color.parseColor("#3A3A3C"))
            setPadding(0, dp(4), 0, 0)
        }
        noticeLayout.addView(noticeTitleText)
        noticeLayout.addView(noticeContentText)
        noticeCard.addView(noticeLayout)
        root.addView(noticeCard)

        // --- 4. 卡密授权 (Card Key Card) ---
        root.addView(createSectionTitle("卡密授权")) // Flush left

        val cardKeyCard = createInnerCard()
        val cardKeyLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp14, dp14, dp14, dp14)
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        cardEditText = EditText(context).apply {
            hint = "请输入授权卡密"
            setText(appSettings.card)
            textSize = 14f
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(dp12, dp12, dp12, dp12)
            background = createCardBg(Color.WHITE, Color.parseColor("#D1D1D6"), dp(10))
            setTextColor(Color.parseColor("#1C1C1E"))
            setHintTextColor(Color.parseColor("#AEAEB2"))
        }
        cardKeyLayout.addView(cardEditText)

        // Action Buttons Row (Paste / Clear)
        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(8))
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        val btnPaste = createSecondaryButton("粘贴卡密") {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = cm?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString() ?: ""
                if (text.isNotBlank()) {
                    cardEditText.setText(text.trim())
                    Toast.makeText(context, "已粘贴", Toast.LENGTH_SHORT).show()
                }
            }
        }
        btnRow.addView(btnPaste)

        val spacer = View(context).apply { layoutParams = LinearLayout.LayoutParams(dp8, 1) }
        btnRow.addView(spacer)

        val btnClear = createSecondaryButton("清空") {
            cardEditText.setText("")
        }
        btnRow.addView(btnClear)
        cardKeyLayout.addView(btnRow)

        // Loading bar
        loadingBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(4)).apply {
                setMargins(0, 0, 0, dp(6))
            }
        }
        cardKeyLayout.addView(loadingBar)

        // 7. 立即验证按钮 (Blue Background with White Text)
        verifyButton = Button(context).apply {
            text = "立即验证"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE) // Requirement: White text on blue background
            background = createRippleDrawable(Color.parseColor("#0A84FF"), Color.parseColor("#0066CC"), dp(12))
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(44))
        }
        cardKeyLayout.addView(verifyButton)
        cardKeyCard.addView(cardKeyLayout)
        root.addView(cardKeyCard)

        // --- 5. 模块增强功能 (Minecraft Enhancements) ---
        root.addView(createSectionTitle("模块增强功能")) // Flush left

        val featureCard = createInnerCard()
        val featureLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp14, dp12, dp14, dp12)
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        // NightVision Row
        val nvRow = createSwitchRow(
            "夜视增强 (NightVision)",
            "自动在游戏中保持高清夜视效果",
            appSettings.isNightVisionEnabled
        ) { isChecked ->
            if (!verifyManager.isVerified.value) {
                Toast.makeText(context, "请先验证卡密激活模块", Toast.LENGTH_SHORT).show()
                switchNightVision.isChecked = false
                return@createSwitchRow
            }
            appSettings.isNightVisionEnabled = isChecked
        }
        switchNightVision = nvRow.second
        featureLayout.addView(nvRow.first)

        featureLayout.addView(createDivider())

        // X-Ray Row
        val xrayRow = createSwitchRow(
            "矿物透视 (X-Ray)",
            "高亮矿石并过滤无效方块",
            appSettings.isXrayEnabled
        ) { isChecked ->
            if (!verifyManager.isVerified.value) {
                Toast.makeText(context, "请先验证卡密激活模块", Toast.LENGTH_SHORT).show()
                switchXray.isChecked = false
                return@createSwitchRow
            }
            appSettings.isXrayEnabled = isChecked
        }
        switchXray = xrayRow.second
        featureLayout.addView(xrayRow.first)

        featureCard.addView(featureLayout)
        root.addView(featureCard)

        // --- 云端服务 ---
        root.addView(createSectionTitle("云端服务")) // Flush left

        val cloudCard = createInnerCard()
        val cloudRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp14, dp12, dp14, dp12)
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        val btnGroup = createSecondaryButton("官方群聊") {
            verifyManager.handleEvent(context, 3, "1031891543")
        }
        cloudRow.addView(btnGroup)

        val spacerCloud = View(context).apply { layoutParams = LinearLayout.LayoutParams(dp8, 1) }
        cloudRow.addView(spacerCloud)

        val btnWeb = createSecondaryButton("官方网站") {
            verifyManager.handleEvent(context, 1, "https://epic.t60.top/")
        }
        cloudRow.addView(btnWeb)
        cloudCard.addView(cloudRow)
        root.addView(cloudCard)
    }

    private fun buildFloatingBall() {
        val ballSize = dp(54)
        floatingBall = FrameLayout(context).apply {
            layoutParams = LayoutParams(ballSize, ballSize).apply {
                setMargins(dp(20), dp(120), 0, 0)
            }
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#0A84FF"), Color.parseColor("#0056B3"))
            ).apply {
                shape = GradientDrawable.OVAL
            }
            elevation = dp(8).toFloat()

            val textLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            }

            val text1 = TextView(context).apply {
                text = "Siyo"
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            val text2 = TextView(context).apply {
                text = "X"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#FFCC00"))
                gravity = Gravity.CENTER
            }
            textLayout.addView(text1)
            textLayout.addView(text2)
            addView(textLayout)
        }

        setupBallDragListener(floatingBall)
        addView(floatingBall)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBallDragListener(ball: View) {
        ball.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = (event.rawX + dX).coerceIn(0f, (width - v.width).toFloat())
                    val newY = (event.rawY + dY).coerceIn(0f, (height - v.height).toFloat())
                    v.x = newX
                    v.y = newY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = abs(event.rawX - downRawX)
                    val diffY = abs(event.rawY - downRawY)
                    if (diffX < touchSlop && diffY < touchSlop) {
                        // Click event
                        togglePanel()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun setupListeners() {
        verifyButton.setOnClickListener {
            val key = cardEditText.text.toString().trim()
            if (key.isEmpty()) {
                Toast.makeText(context, "请输入卡密", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loadingBar.visibility = View.VISIBLE
            verifyButton.isEnabled = false
            statusDetailText.text = "正在连接云端服务器验证..."

            verifyManager.verifyCard(key) { success, msg ->
                post {
                    loadingBar.visibility = View.GONE
                    verifyButton.isEnabled = true
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    updateStateUI()
                    if (success) {
                        closePanel()
                    }
                }
            }
        }
    }

    private fun loadCloudData() {
        verifyManager.loadSoftwareConfig { success, _ ->
            post {
                val notice = verifyManager.noticeConfig
                if (notice != null && notice.hasNotice()) {
                    if (!notice.title.isNullOrBlank()) noticeTitleText.text = notice.title
                    if (!notice.content.isNullOrBlank()) noticeContentText.text = notice.content
                }
            }
        }

        // Auto verify if card exists
        val savedCard = appSettings.card
        if (appSettings.autoVerify && savedCard.isNotBlank() && !verifyManager.isVerified.value) {
            verifyManager.verifyCard(savedCard) { success, _ ->
                post {
                    updateStateUI()
                }
            }
        }
    }

    private fun updateStateUI() {
        val verified = verifyManager.isVerified.value
        if (verified) {
            statusBadge.text = "已授权"
            statusBadge.background = createCardBg(Color.parseColor("#34C759"), Color.TRANSPARENT, dp(10))
            statusDetailText.text = "已激活 (到期: ${verifyManager.formatDate(verifyManager.expireTimestamp.value)})"
            verifyButton.text = "验证通过 (点击重验)"
            btnMinimize.visibility = View.VISIBLE
        } else {
            statusBadge.text = "未授权"
            statusBadge.background = createCardBg(Color.parseColor("#FF9500"), Color.TRANSPARENT, dp(10))
            statusDetailText.text = "未激活"
            verifyButton.text = "立即验证"
            btnMinimize.visibility = View.GONE
        }
    }

    fun openPanel() {
        isPanelOpen = true
        scrimView.visibility = View.VISIBLE
        floatingBall.visibility = View.GONE
        updateStateUI()
    }

    fun closePanel() {
        isPanelOpen = false
        scrimView.visibility = View.GONE
        floatingBall.visibility = View.VISIBLE
    }

    fun togglePanel() {
        if (isPanelOpen) closePanel() else openPanel()
    }

    // Touch interceptor: when panel is open, intercept all touch events; when closed, only touches on floatingBall are consumed
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isPanelOpen) {
            return false // Let child views in panel process touches
        }
        // When panel is closed, only intercept if touch is within floatingBall
        val ballRect = IntArray(2)
        floatingBall.getLocationOnScreen(ballRect)
        val inBall = ev.rawX >= ballRect[0] && ev.rawX <= ballRect[0] + floatingBall.width &&
                ev.rawY >= ballRect[1] && ev.rawY <= ballRect[1] + floatingBall.height
        return false // Do not intercept, let floatingBall onTouch handle it
    }

    // Helper UI builders
    private fun createSectionTitle(title: String): TextView {
        return TextView(context).apply {
            text = title
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#8E8E93"))
            setPadding(0, dp(12), 0, dp(4)) // Requirement 4: 0 start padding, flush left
        }
    }

    private fun createInnerCard(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            background = createCardBg(Color.parseColor("#FFFFFF"), Color.parseColor("#ECEFF4"), dp(14))
            elevation = dp(2).toFloat()
        }
    }

    private fun createSecondaryButton(text: String, onClick: () -> Unit): Button {
        return Button(context).apply {
            this.text = text
            textSize = 13f
            setTextColor(Color.parseColor("#1C1C1E"))
            background = createRippleDrawable(Color.parseColor("#F2F3F7"), Color.parseColor("#E2E4EB"), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f)
            setOnClickListener { onClick() }
        }
    }

    private fun createSwitchRow(title: String, desc: String, initial: Boolean, onChecked: (Boolean) -> Unit): Pair<LinearLayout, Switch> {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleTv = TextView(context).apply {
            text = title
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1C1C1E"))
        }
        val descTv = TextView(context).apply {
            text = desc
            textSize = 11f
            setTextColor(Color.parseColor("#8E8E93"))
        }
        textCol.addView(titleTv)
        textCol.addView(descTv)
        row.addView(textCol)

        val sw = Switch(context).apply {
            isChecked = initial
            setOnCheckedChangeListener { _, isChecked -> onChecked(isChecked) }
        }
        row.addView(sw)

        return Pair(row, sw)
    }

    private fun createDivider(): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1)).apply {
                setMargins(0, dp(8), 0, dp(8))
            }
            setBackgroundColor(Color.parseColor("#E5E9F0"))
        }
    }

    private fun createCardBg(bgColor: Int, strokeColor: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = radius.toFloat()
            if (strokeColor != Color.TRANSPARENT) {
                setStroke(dp(1), strokeColor)
            }
        }
    }

    private fun createRippleDrawable(normalColor: Int, pressedColor: Int, radius: Int): RippleDrawable {
        val content = createCardBg(normalColor, Color.TRANSPARENT, radius)
        val mask = createCardBg(Color.BLACK, Color.TRANSPARENT, radius)
        return RippleDrawable(ColorStateList.valueOf(pressedColor), content, mask)
    }

    private fun dp(v: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            v.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
