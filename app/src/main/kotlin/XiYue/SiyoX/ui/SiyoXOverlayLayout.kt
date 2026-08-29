// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import XiYue.SiyoX.SiyoXConfig
import XiYue.SiyoX.data.AppSettings
import XiYue.SiyoX.data.VerifyManager
import kotlin.math.abs

@SuppressLint("ViewConstructor")
class SiyoXOverlayLayout(private val activity: Activity) : FrameLayout(activity) {

    private val appSettings = AppSettings.get()
    private val verifyManager = VerifyManager.get()

    private var isPanelOpen = false

    // Views
    private lateinit var fullScreenVerifyView: FrameLayout
    private lateinit var floatingBall: FrameLayout
    private lateinit var inGamePanelScrim: FrameLayout

    // Full screen verify elements
    private lateinit var fullNoticeTitle: TextView
    private lateinit var fullNoticeContent: TextView
    private lateinit var fullCardInput: EditText
    private lateinit var fullBtnVerify: Button
    private lateinit var fullBtnExit: Button
    private lateinit var fullLoadingBar: ProgressBar
    private lateinit var fullStatusTip: TextView

    // In-game Panel elements
    private lateinit var panelStatusDetail: TextView
    private lateinit var panelStatusBadge: TextView
    private lateinit var switchFeature1: Switch
    private lateinit var switchFeature2: Switch
    private lateinit var switchFeature3: Switch

    // Floating Ball drag coordinates (Full-screen free movement)
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
        loadNotice()

        // Check verification state
        checkInitialState()
    }

    private fun initUI() {
        // 1. Build Full-Screen Verification Window (Shown when NOT verified)
        buildFullScreenVerifyWindow()

        // 2. Build In-Game Function Panel (Shown when verified and ball clicked)
        buildInGamePanel()

        // 3. Build Floating Ball (Shown when verified)
        buildFloatingBall()
    }

    // ==========================================
    // 1. 全屏验证窗口 (未验证时展示)
    // 只需要显示: Logo, 软件名和版本号, 公告栏, 卡密输入框, 验证按钮, 退出按钮
    // ==========================================
    private fun buildFullScreenVerifyWindow() {
        val dp12 = dp(12)
        val dp14 = dp(14)
        val dp16 = dp(16)
        val dp8 = dp(8)

        fullScreenVerifyView = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#E6000000")) // Semi-transparent dark immersive mask
            isClickable = true
        }

        val panelWidth = dp(420).coerceAtMost((activity.resources.displayMetrics.widthPixels * 0.92f).toInt())

        val scrollView = ScrollView(context).apply {
            layoutParams = LayoutParams(panelWidth, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val cardRoot = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dp18(), dp16, dp18(), dp18())
            background = createCardBg(Color.parseColor("#F9FAFC"), Color.parseColor("#E5E9F0"), dp(20))
            isClickable = true
        }

        // Header: Logo, 软件名, 版本号
        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(0, dp8, 0, dp12)
        }

        // Logo Image
        val logoView = ImageView(context).apply {
            val logoSize = dp(64)
            layoutParams = LinearLayout.LayoutParams(logoSize, logoSize).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            val logoBmp = getLogoBitmap()
            if (logoBmp != null) {
                setImageBitmap(logoBmp)
            } else {
                setImageResource(android.R.drawable.sym_def_app_icon)
            }
            background = createCardBg(Color.WHITE, Color.parseColor("#E5E9F0"), dp(16))
            clipToOutline = true
        }
        headerLayout.addView(logoView)

        // 软件名
        val tvAppName = TextView(context).apply {
            text = SiyoXConfig.APP_NAME
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0A84FF"))
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp8, 0, 0)
        }
        headerLayout.addView(tvAppName)

        // 版本号
        val tvVersion = TextView(context).apply {
            text = "${SiyoXConfig.VERSION_NAME} (${verifyManager.getActiveProviderName()})"
            textSize = 12f
            setTextColor(Color.parseColor("#8E8E93"))
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(2), 0, 0)
        }
        headerLayout.addView(tvVersion)
        cardRoot.addView(headerLayout)

        cardRoot.addView(createDivider())

        // --- 公告栏 (Notice Board) ---
        cardRoot.addView(createSectionTitle("公告栏")) // Flush left

        val noticeCard = createInnerCard()
        val noticeLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp14, dp12, dp14, dp12)
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        fullNoticeTitle = TextView(context).apply {
            text = "官方公告"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0A84FF"))
        }
        fullNoticeContent = TextView(context).apply {
            text = "欢迎使用 SiyoX 模块！请输入授权卡密激活后开始体验。"
            textSize = 12f
            setLineSpacing(dp(2).toFloat(), 1.1f)
            setTextColor(Color.parseColor("#3A3A3C"))
            setPadding(0, dp(4), 0, 0)
        }
        noticeLayout.addView(fullNoticeTitle)
        noticeLayout.addView(fullNoticeContent)
        noticeCard.addView(noticeLayout)
        cardRoot.addView(noticeCard)

        // --- 卡密输入框 (Card Key Input) ---
        cardRoot.addView(createSectionTitle("卡密授权")) // Flush left

        val cardKeyCard = createInnerCard()
        val cardKeyLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp14, dp14, dp14, dp14)
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        fullCardInput = EditText(context).apply {
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
        cardKeyLayout.addView(fullCardInput)

        // Action Buttons Row (Paste / Clear)
        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp8, 0, dp8)
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        val btnPaste = createSecondaryButton("粘贴卡密") {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = cm?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString() ?: ""
                if (text.isNotBlank()) {
                    fullCardInput.setText(text.trim())
                    Toast.makeText(context, "已粘贴", Toast.LENGTH_SHORT).show()
                }
            }
        }
        btnRow.addView(btnPaste)

        val spacer = View(context).apply { layoutParams = LinearLayout.LayoutParams(dp8, 1) }
        btnRow.addView(spacer)

        val btnClear = createSecondaryButton("清空") {
            fullCardInput.setText("")
        }
        btnRow.addView(btnClear)
        cardKeyLayout.addView(btnRow)

        // Loading bar
        fullLoadingBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(4)).apply {
                setMargins(0, 0, 0, dp(6))
            }
        }
        cardKeyLayout.addView(fullLoadingBar)

        fullStatusTip = TextView(context).apply {
            text = "设备 ID: ${verifyManager.getAndroidId()}"
            textSize = 11f
            setTextColor(Color.parseColor("#8E8E93"))
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, dp8)
        }
        cardKeyLayout.addView(fullStatusTip)

        // Bottom Action Buttons: 验证按钮 + 退出按钮
        val bottomActions = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(44))
        }

        // 验证按钮 (Blue Background with White Text)
        fullBtnVerify = Button(context).apply {
            text = "立即验证"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE) // Requirement: White text on blue background
            background = createRippleDrawable(Color.parseColor("#0A84FF"), Color.parseColor("#0066CC"), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 2f)
        }
        bottomActions.addView(fullBtnVerify)

        val spacerExit = View(context).apply { layoutParams = LinearLayout.LayoutParams(dp8, 1) }
        bottomActions.addView(spacerExit)

        // 退出按钮
        fullBtnExit = Button(context).apply {
            text = "退出游戏"
            textSize = 14f
            setTextColor(Color.parseColor("#FF3B30"))
            background = createRippleDrawable(Color.parseColor("#FDE8E8"), Color.parseColor("#FBD5D5"), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            setOnClickListener {
                activity.finishAffinity()
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
        bottomActions.addView(fullBtnExit)

        cardKeyLayout.addView(bottomActions)
        cardKeyCard.addView(cardKeyLayout)
        cardRoot.addView(cardKeyCard)

        scrollView.addView(cardRoot)
        fullScreenVerifyView.addView(scrollView)
        addView(fullScreenVerifyView)
    }

    // ==========================================
    // 2. 游戏内悬浮功能面板 (验证通过后由悬浮球唤起)
    // 包含: 顶部栏, 状态卡片, 仨占位功能(文字占位), 云端服务
    // ==========================================
    private fun buildInGamePanel() {
        val dp12 = dp(12)
        val dp14 = dp(14)
        val dp16 = dp(16)
        val dp8 = dp(8)

        inGamePanelScrim = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#80000000"))
            visibility = View.GONE
            setOnClickListener {
                closePanel()
            }
        }

        val panelWidth = dp(400).coerceAtMost((activity.resources.displayMetrics.widthPixels * 0.92f).toInt())

        val scrollView = ScrollView(context).apply {
            layoutParams = LayoutParams(panelWidth, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val panelRoot = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(dp18(), dp16, dp18(), dp18())
            background = createCardBg(Color.parseColor("#F9FAFC"), Color.parseColor("#E5E9F0"), dp(20))
            isClickable = true
        }

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
            text = "${SiyoXConfig.APP_NAME} "
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

        val btnMinimize = TextView(context).apply {
            text = "收起"
            textSize = 12f
            setTextColor(Color.parseColor("#8E8E93"))
            setPadding(dp8, dp(4), dp8, dp(4))
            background = createCardBg(Color.parseColor("#EBEBF0"), Color.TRANSPARENT, dp(8))
            setOnClickListener { closePanel() }
        }
        topBar.addView(btnMinimize)
        panelRoot.addView(topBar)

        panelRoot.addView(createDivider())

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
            text = "作用域: ${SiyoXConfig.TARGET_PACKAGE}"
            textSize = 12f
            setTextColor(Color.parseColor("#8E8E93"))
        }
        panelStatusDetail = TextView(context).apply {
            text = "已激活"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1C1C1E"))
            setPadding(0, dp(2), 0, 0)
        }
        statusInfoLayout.addView(textScope)
        statusInfoLayout.addView(panelStatusDetail)
        statusLayout.addView(statusInfoLayout)

        panelStatusBadge = TextView(context).apply {
            text = "已授权"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = createCardBg(Color.parseColor("#34C759"), Color.TRANSPARENT, dp(10))
        }
        statusLayout.addView(panelStatusBadge)
        statusCard.addView(statusLayout)
        panelRoot.addView(statusCard)

        // --- 仨功能占位 (Three Placeholder Features) ---
        panelRoot.addView(createSectionTitle("模块功能")) // Flush left

        val featureCard = createInnerCard()
        val featureLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp14, dp12, dp14, dp12)
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        // 功能一 (占位)
        val f1Row = createSwitchRow(
            "功能模块 01 (待接入)",
            "核心功能占位 01，可在功能源码中接入 Hook 逻辑",
            false
        ) { isChecked ->
            Toast.makeText(context, "功能 01: ${if (isChecked) "已启用" else "已关闭"}", Toast.LENGTH_SHORT).show()
        }
        switchFeature1 = f1Row.second
        featureLayout.addView(f1Row.first)

        featureLayout.addView(createDivider())

        // 功能二 (占位)
        val f2Row = createSwitchRow(
            "功能模块 02 (待接入)",
            "核心功能占位 02，可在功能源码中接入 Hook 逻辑",
            false
        ) { isChecked ->
            Toast.makeText(context, "功能 02: ${if (isChecked) "已启用" else "已关闭"}", Toast.LENGTH_SHORT).show()
        }
        switchFeature2 = f2Row.second
        featureLayout.addView(f2Row.first)

        featureLayout.addView(createDivider())

        // 功能三 (占位)
        val f3Row = createSwitchRow(
            "功能模块 03 (待接入)",
            "核心功能占位 03，可在功能源码中接入 Hook 逻辑",
            false
        ) { isChecked ->
            Toast.makeText(context, "功能 03: ${if (isChecked) "已启用" else "已关闭"}", Toast.LENGTH_SHORT).show()
        }
        switchFeature3 = f3Row.second
        featureLayout.addView(f3Row.first)

        featureCard.addView(featureLayout)
        panelRoot.addView(featureCard)

        // --- 云端服务 ---
        panelRoot.addView(createSectionTitle("云端服务")) // Flush left

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
        panelRoot.addView(cloudCard)

        scrollView.addView(panelRoot)
        inGamePanelScrim.addView(scrollView)
        addView(inGamePanelScrim)
    }

    // ==========================================
    // 3. 悬浮球 (全屏幕自由拖拽移动，验证通过后展示)
    // ==========================================
    private fun buildFloatingBall() {
        val ballSize = dp(56)
        floatingBall = FrameLayout(context).apply {
            layoutParams = LayoutParams(ballSize, ballSize).apply {
                setMargins(dp(20), dp(160), 0, 0)
            }
            visibility = View.GONE
            elevation = dp(10).toFloat()

            // Icon background
            val bg = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#0A84FF"), Color.parseColor("#0056B3"))
            ).apply {
                shape = GradientDrawable.OVAL
            }
            background = bg

            val logoImg = ImageView(context).apply {
                val pad = dp(10)
                setPadding(pad, pad, pad, pad)
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                val bmp = getLogoBitmap()
                if (bmp != null) {
                    setImageBitmap(bmp)
                } else {
                    setImageResource(android.R.drawable.sym_def_app_icon)
                }
            }
            addView(logoImg)
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
                    // Support full-screen free movement
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
                        // Click event: toggle function panel
                        togglePanel()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun setupListeners() {
        fullBtnVerify.setOnClickListener {
            val key = fullCardInput.text.toString().trim()
            if (key.isEmpty()) {
                Toast.makeText(context, "请输入卡密", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            fullLoadingBar.visibility = View.VISIBLE
            fullBtnVerify.isEnabled = false
            fullStatusTip.text = "正在连接云端验证..."

            verifyManager.verifyCard(key) { success, msg ->
                post {
                    fullLoadingBar.visibility = View.GONE
                    fullBtnVerify.isEnabled = true
                    fullStatusTip.text = msg

                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

                    if (success) {
                        onVerifySuccess()
                    }
                }
            }
        }
    }

    private fun loadNotice() {
        verifyManager.loadSoftwareNotice { success, _ ->
            post {
                fullNoticeTitle.text = verifyManager.noticeTitle.value
                fullNoticeContent.text = verifyManager.noticeContent.value
            }
        }

        // Auto verify if card already exists
        val savedCard = appSettings.card
        if (appSettings.autoVerify && savedCard.isNotBlank() && !verifyManager.isVerified.value) {
            verifyManager.verifyCard(savedCard) { success, _ ->
                post {
                    if (success) {
                        onVerifySuccess()
                    }
                }
            }
        }
    }

    private fun checkInitialState() {
        if (verifyManager.isVerified.value) {
            onVerifySuccess()
        } else {
            fullScreenVerifyView.visibility = View.VISIBLE
            floatingBall.visibility = View.GONE
            inGamePanelScrim.visibility = View.GONE
        }
    }

    private fun onVerifySuccess() {
        // Dismiss full-screen verification window
        fullScreenVerifyView.visibility = View.GONE

        // Update panel status
        panelStatusBadge.text = "已授权"
        panelStatusBadge.background = createCardBg(Color.parseColor("#34C759"), Color.TRANSPARENT, dp(10))
        panelStatusDetail.text = "已激活 (到期: ${VerifyManager.formatDate(verifyManager.expireTimestamp.value)})"

        // Show floating ball
        floatingBall.visibility = View.VISIBLE
    }

    fun openPanel() {
        if (!verifyManager.isVerified.value) {
            fullScreenVerifyView.visibility = View.VISIBLE
            inGamePanelScrim.visibility = View.GONE
            floatingBall.visibility = View.GONE
            return
        }
        isPanelOpen = true
        inGamePanelScrim.visibility = View.VISIBLE
        floatingBall.visibility = View.GONE
    }

    fun closePanel() {
        isPanelOpen = false
        inGamePanelScrim.visibility = View.GONE
        if (verifyManager.isVerified.value) {
            floatingBall.visibility = View.VISIBLE
        }
    }

    fun togglePanel() {
        if (isPanelOpen) closePanel() else openPanel()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (fullScreenVerifyView.visibility == View.VISIBLE || isPanelOpen) {
            return false
        }
        return false
    }

    private fun getLogoBitmap(): android.graphics.Bitmap? {
        return try {
            val resId = context.resources.getIdentifier("logo", "drawable", context.packageName)
            if (resId != 0) {
                BitmapFactory.decodeResource(context.resources, resId)
            } else {
                BitmapFactory.decodeFile("/sdcard/Logo.png")
            }
        } catch (e: Exception) {
            null
        }
    }

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

    private fun dp18(): Int = dp(18)
}
