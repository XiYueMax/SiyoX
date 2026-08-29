// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.ui

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import XiYue.SiyoX.SiyoXConfig;
import XiYue.SiyoX.data.AppSettings;
import XiYue.SiyoX.data.VerifyManager;

@SuppressLint("ViewConstructor")
public class SiyoXOverlayLayout extends FrameLayout {

    private final Activity activity;
    private final AppSettings appSettings;
    private final VerifyManager verifyManager;

    private boolean isPanelOpen = false;

    // View Components
    private FrameLayout fullScreenVerifyView;
    private FrameLayout floatingBall;
    private FrameLayout inGamePanelScrim;

    // Full-screen verify components
    private TextView fullNoticeTitle;
    private TextView fullNoticeContent;
    private EditText fullCardInput;
    private Button fullBtnVerify;
    private Button fullBtnExit;
    private ProgressBar fullLoadingBar;
    private TextView fullStatusTip;

    // In-game Panel components
    private TextView panelStatusDetail;
    private TextView panelStatusBadge;
    private Switch switchFeature1;
    private Switch switchFeature2;
    private Switch switchFeature3;

    // Floating Ball drag coordinates (Full-screen free movement)
    private float dX = 0f;
    private float dY = 0f;
    private float downRawX = 0f;
    private float downRawY = 0f;
    private final int touchSlop;

    public SiyoXOverlayLayout(Activity activity) {
        super(activity);
        this.activity = activity;
        this.appSettings = AppSettings.get();
        this.verifyManager = VerifyManager.get();
        this.touchSlop = dp(6);

        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setFocusable(false);
        setFocusableInTouchMode(false);

        initUI();
        setupListeners();
        loadNotice();
        checkInitialState();
    }

    private void initUI() {
        // 1. 全屏横屏验证窗口 (未验证时全屏锁定展示)
        buildFullScreenVerifyWindow();

        // 2. 游戏内悬浮功能面板 (验证通过后由悬浮球唤起)
        buildInGamePanel();

        // 3. 全屏幕可自由拖拽悬浮球 (验证通过后展示)
        buildFloatingBall();
    }

    // ==========================================
    // 1. 全屏横屏验证窗口 (未验证时展示)
    // 布局结构 (横屏设计):
    // 左上方: 图标 + 软件名 SiyoX
    // 左下方: 公告栏 (对接 T3 / EPIC / 微验)
    // 页面右侧: 卡密输入框, 退出按钮(在左), 验证按钮(在右)
    // ==========================================
    private void buildFullScreenVerifyWindow() {
        int dp10 = dp(10);
        int dp12 = dp(12);
        int dp14 = dp(14);
        int dp16 = dp(16);
        int dp18 = dp(18);
        int dp8 = dp(8);

        fullScreenVerifyView = new FrameLayout(getContext());
        fullScreenVerifyView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        fullScreenVerifyView.setBackgroundColor(Color.parseColor("#E6000000")); // 全屏半透明沉浸遮罩
        fullScreenVerifyView.setClickable(true);

        // 主居中卡片容器 (自适应横屏宽度与高度)
        FrameLayout cardWrapper = new FrameLayout(getContext());
        LayoutParams wrapParams = new LayoutParams(
                (int)(activity.getResources().getDisplayMetrics().widthPixels * 0.94f),
                (int)(activity.getResources().getDisplayMetrics().heightPixels * 0.92f)
        );
        wrapParams.gravity = Gravity.CENTER;
        cardWrapper.setLayoutParams(wrapParams);
        cardWrapper.setBackground(createCardBg(Color.parseColor("#F9FAFC"), Color.parseColor("#E5E9F0"), dp(20)));
        cardWrapper.setPadding(dp18, dp16, dp18, dp16);
        cardWrapper.setClickable(true);

        // 横向左右分栏
        LinearLayout mainHorizontalLayout = new LinearLayout(getContext());
        mainHorizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
        mainHorizontalLayout.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // ======================== 左半部分 ========================
        LinearLayout leftColumn = new LinearLayout(getContext());
        leftColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.1f);
        leftParams.setMargins(0, 0, dp14, 0);
        leftColumn.setLayoutParams(leftParams);

        // 2. 左上方: 显示图标 + 软件名 SiyoX
        LinearLayout topLeftHeader = new LinearLayout(getContext());
        topLeftHeader.setOrientation(LinearLayout.HORIZONTAL);
        topLeftHeader.setGravity(Gravity.CENTER_VERTICAL);
        topLeftHeader.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        topLeftHeader.setPadding(0, 0, 0, dp8);

        ImageView logoView = new ImageView(getContext());
        int logoSize = dp(46);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(logoSize, logoSize);
        logoView.setLayoutParams(logoParams);
        Bitmap logoBmp = getLogoBitmap();
        if (logoBmp != null) {
            logoView.setImageBitmap(logoBmp);
        } else {
            logoView.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        logoView.setBackground(createCardBg(Color.WHITE, Color.parseColor("#E5E9F0"), dp(12)));
        logoView.setClipToOutline(true);
        topLeftHeader.addView(logoView);

        LinearLayout titleTextCol = new LinearLayout(getContext());
        titleTextCol.setOrientation(LinearLayout.VERTICAL);
        titleTextCol.setPadding(dp10, 0, 0, 0);

        TextView tvAppName = new TextView(getContext());
        tvAppName.setText(SiyoXConfig.APP_NAME);
        tvAppName.setTextSize(20f);
        tvAppName.setTypeface(Typeface.DEFAULT_BOLD);
        tvAppName.setTextColor(Color.parseColor("#0A84FF"));
        titleTextCol.addView(tvAppName);

        TextView tvVersion = new TextView(getContext());
        tvVersion.setText(SiyoXConfig.VERSION_NAME + " • " + verifyManager.getActiveProviderName());
        tvVersion.setTextSize(11f);
        tvVersion.setTextColor(Color.parseColor("#8E8E93"));
        titleTextCol.addView(tvVersion);

        topLeftHeader.addView(titleTextCol);
        leftColumn.addView(topLeftHeader);

        leftColumn.addView(createDivider());

        // 3. 左下方: 公告栏 (对接 T3 / EPIC / 微验)
        TextView tvNoticeLabel = createSectionTitle("📢 公告栏");
        leftColumn.addView(tvNoticeLabel);

        LinearLayout noticeCard = createInnerCard();
        noticeCard.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        noticeCard.setPadding(dp14, dp10, dp14, dp10);

        ScrollView noticeScrollView = new ScrollView(getContext());
        noticeScrollView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        noticeScrollView.setVerticalScrollBarEnabled(true);
        noticeScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout noticeInner = new LinearLayout(getContext());
        noticeInner.setOrientation(LinearLayout.VERTICAL);
        noticeInner.setLayoutParams(new ScrollView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        fullNoticeTitle = new TextView(getContext());
        fullNoticeTitle.setText("官方公告");
        fullNoticeTitle.setTextSize(13f);
        fullNoticeTitle.setTypeface(Typeface.DEFAULT_BOLD);
        fullNoticeTitle.setTextColor(Color.parseColor("#0A84FF"));
        noticeInner.addView(fullNoticeTitle);

        fullNoticeContent = new TextView(getContext());
        fullNoticeContent.setText("欢迎使用 SiyoX 模块！正在连接云端获取最新公告...");
        fullNoticeContent.setTextSize(11.5f);
        fullNoticeContent.setLineSpacing(dp(2), 1.15f);
        fullNoticeContent.setTextColor(Color.parseColor("#3A3A3C"));
        fullNoticeContent.setPadding(0, dp(4), 0, 0);
        noticeInner.addView(fullNoticeContent);

        noticeScrollView.addView(noticeInner);
        noticeCard.addView(noticeScrollView);
        leftColumn.addView(noticeCard);

        mainHorizontalLayout.addView(leftColumn);

        // ======================== 右半部分 ========================
        // 4. 页面右侧是卡密输入框和验证按钮和退出按钮，退出在左侧，验证在右侧
        LinearLayout rightColumn = new LinearLayout(getContext());
        rightColumn.setOrientation(LinearLayout.VERTICAL);
        rightColumn.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.2f);
        rightParams.setMargins(dp14, 0, 0, 0);
        rightColumn.setLayoutParams(rightParams);

        TextView tvAuthLabel = createSectionTitle("🔑 卡密授权");
        rightColumn.addView(tvAuthLabel);

        LinearLayout cardKeyCard = createInnerCard();
        LinearLayout cardKeyLayout = new LinearLayout(getContext());
        cardKeyLayout.setOrientation(LinearLayout.VERTICAL);
        cardKeyLayout.setPadding(dp16, dp14, dp16, dp14);
        cardKeyLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        fullCardInput = new EditText(getContext());
        fullCardInput.setHint("请输入授权卡密");
        fullCardInput.setText(appSettings.getCard());
        fullCardInput.setTextSize(14f);
        fullCardInput.setSingleLine(true);
        fullCardInput.setInputType(InputType.TYPE_CLASS_TEXT);
        fullCardInput.setPadding(dp12, dp12, dp12, dp12);
        fullCardInput.setBackground(createCardBg(Color.WHITE, Color.parseColor("#D1D1D6"), dp(10)));
        fullCardInput.setTextColor(Color.parseColor("#1C1C1E"));
        fullCardInput.setHintTextColor(Color.parseColor("#AEAEB2"));
        cardKeyLayout.addView(fullCardInput);

        // 辅助操作 (粘贴 / 清空)
        LinearLayout btnRow = new LinearLayout(getContext());
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp8, 0, dp8);
        btnRow.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        Button btnPaste = createSecondaryButton("粘贴卡密", new OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && cm.getPrimaryClip() != null && cm.getPrimaryClip().getItemCount() > 0) {
                    CharSequence text = cm.getPrimaryClip().getItemAt(0).getText();
                    if (text != null && text.length() > 0) {
                        fullCardInput.setText(text.toString().trim());
                        Toast.makeText(getContext(), "已粘贴", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        btnRow.addView(btnPaste);

        View spacer = new View(getContext());
        spacer.setLayoutParams(new LinearLayout.LayoutParams(dp8, 1));
        btnRow.addView(spacer);

        Button btnClear = createSecondaryButton("清空", new OnClickListener() {
            @Override
            public void onClick(View v) {
                fullCardInput.setText("");
            }
        });
        btnRow.addView(btnClear);
        cardKeyLayout.addView(btnRow);

        // 加载指示器
        fullLoadingBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        fullLoadingBar.setIndeterminate(true);
        fullLoadingBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(4));
        barParams.setMargins(0, 0, 0, dp(6));
        fullLoadingBar.setLayoutParams(barParams);
        cardKeyLayout.addView(fullLoadingBar);

        fullStatusTip = new TextView(getContext());
        fullStatusTip.setText("设备 ID: " + verifyManager.getAndroidId());
        fullStatusTip.setTextSize(11f);
        fullStatusTip.setTextColor(Color.parseColor("#8E8E93"));
        fullStatusTip.setGravity(Gravity.CENTER_HORIZONTAL);
        fullStatusTip.setPadding(0, 0, 0, dp8);
        cardKeyLayout.addView(fullStatusTip);

        // 底部按钮栏: 【退出按钮在左侧】 【验证按钮在右侧】
        LinearLayout bottomActions = new LinearLayout(getContext());
        bottomActions.setOrientation(LinearLayout.HORIZONTAL);
        bottomActions.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(44)));

        // 退出按钮 (左侧)
        fullBtnExit = new Button(getContext());
        fullBtnExit.setText("退出游戏");
        fullBtnExit.setTextSize(14f);
        fullBtnExit.setTypeface(Typeface.DEFAULT_BOLD);
        fullBtnExit.setTextColor(Color.parseColor("#FF3B30"));
        fullBtnExit.setBackground(createRippleDrawable(Color.parseColor("#FDE8E8"), Color.parseColor("#FBD5D5"), dp(12)));
        fullBtnExit.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.1f));
        fullBtnExit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.finishAffinity();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
        bottomActions.addView(fullBtnExit);

        View spacerExit = new View(getContext());
        spacerExit.setLayoutParams(new LinearLayout.LayoutParams(dp10, 1));
        bottomActions.addView(spacerExit);

        // 验证按钮 (右侧，蓝底白字)
        fullBtnVerify = new Button(getContext());
        fullBtnVerify.setText("立即验证");
        fullBtnVerify.setTextSize(15f);
        fullBtnVerify.setTypeface(Typeface.DEFAULT_BOLD);
        fullBtnVerify.setTextColor(Color.WHITE); // 白字
        fullBtnVerify.setBackground(createRippleDrawable(Color.parseColor("#0A84FF"), Color.parseColor("#0066CC"), dp(12))); // 蓝底
        fullBtnVerify.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 2f));
        bottomActions.addView(fullBtnVerify);

        cardKeyLayout.addView(bottomActions);
        cardKeyCard.addView(cardKeyLayout);
        rightColumn.addView(cardKeyCard);

        mainHorizontalLayout.addView(rightColumn);

        cardWrapper.addView(mainHorizontalLayout);
        fullScreenVerifyView.addView(cardWrapper);
        addView(fullScreenVerifyView);
    }

    // ==========================================
    // 2. 游戏内悬浮功能面板 (验证通过后由悬浮球唤起)
    // 包含: 顶部栏, 状态卡片, 仨占位功能(文字占位), 云端服务
    // ==========================================
    private void buildInGamePanel() {
        int dp12 = dp(12);
        int dp14 = dp(14);
        int dp16 = dp(16);
        int dp18 = dp(18);
        int dp8 = dp(8);

        inGamePanelScrim = new FrameLayout(getContext());
        inGamePanelScrim.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        inGamePanelScrim.setBackgroundColor(Color.parseColor("#80000000"));
        inGamePanelScrim.setVisibility(View.GONE);
        inGamePanelScrim.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                closePanel();
            }
        });

        int panelWidth = Math.min(dp(440), (int)(activity.getResources().getDisplayMetrics().widthPixels * 0.88f));

        ScrollView scrollView = new ScrollView(getContext());
        LayoutParams scrollParams = new LayoutParams(panelWidth, LayoutParams.WRAP_CONTENT);
        scrollParams.gravity = Gravity.CENTER;
        scrollView.setLayoutParams(scrollParams);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout panelRoot = new LinearLayout(getContext());
        panelRoot.setOrientation(LinearLayout.VERTICAL);
        panelRoot.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        panelRoot.setPadding(dp18, dp16, dp18, dp18);
        panelRoot.setBackground(createCardBg(Color.parseColor("#F9FAFC"), Color.parseColor("#E5E9F0"), dp(20)));
        panelRoot.setClickable(true);

        // --- Top Bar ---
        LinearLayout topBar = new LinearLayout(getContext());
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout titleContainer = new LinearLayout(getContext());
        titleContainer.setOrientation(LinearLayout.HORIZONTAL);
        titleContainer.setGravity(Gravity.CENTER_VERTICAL);
        titleContainer.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        TextView titleSiyoX = new TextView(getContext());
        titleSiyoX.setText(SiyoXConfig.APP_NAME + " ");
        titleSiyoX.setTextSize(20f);
        titleSiyoX.setTypeface(Typeface.DEFAULT_BOLD);
        titleSiyoX.setTextColor(Color.parseColor("#0A84FF"));

        TextView titleSub = new TextView(getContext());
        titleSub.setText("功能面板");
        titleSub.setTextSize(16f);
        titleSub.setTypeface(Typeface.DEFAULT_BOLD);
        titleSub.setTextColor(Color.parseColor("#1C1C1E"));

        titleContainer.addView(titleSiyoX);
        titleContainer.addView(titleSub);
        topBar.addView(titleContainer);

        TextView btnMinimize = new TextView(getContext());
        btnMinimize.setText("收起");
        btnMinimize.setTextSize(12f);
        btnMinimize.setTextColor(Color.parseColor("#8E8E93"));
        btnMinimize.setPadding(dp8, dp(4), dp8, dp(4));
        btnMinimize.setBackground(createCardBg(Color.parseColor("#EBEBF0"), Color.TRANSPARENT, dp(8)));
        btnMinimize.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                closePanel();
            }
        });
        topBar.addView(btnMinimize);
        panelRoot.addView(topBar);

        panelRoot.addView(createDivider());

        // --- Status Card ---
        LinearLayout statusCard = createInnerCard();
        LinearLayout statusLayout = new LinearLayout(getContext());
        statusLayout.setOrientation(LinearLayout.HORIZONTAL);
        statusLayout.setGravity(Gravity.CENTER_VERTICAL);
        statusLayout.setPadding(dp14, dp12, dp14, dp12);
        statusLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout statusInfoLayout = new LinearLayout(getContext());
        statusInfoLayout.setOrientation(LinearLayout.VERTICAL);
        statusInfoLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        TextView textScope = new TextView(getContext());
        textScope.setText("作用域: " + SiyoXConfig.TARGET_PACKAGE);
        textScope.setTextSize(12f);
        textScope.setTextColor(Color.parseColor("#8E8E93"));

        panelStatusDetail = new TextView(getContext());
        panelStatusDetail.setText("已激活");
        panelStatusDetail.setTextSize(13f);
        panelStatusDetail.setTypeface(Typeface.DEFAULT_BOLD);
        panelStatusDetail.setTextColor(Color.parseColor("#1C1C1E"));
        panelStatusDetail.setPadding(0, dp(2), 0, 0);

        statusInfoLayout.addView(textScope);
        statusInfoLayout.addView(panelStatusDetail);
        statusLayout.addView(statusInfoLayout);

        panelStatusBadge = new TextView(getContext());
        panelStatusBadge.setText("已授权");
        panelStatusBadge.setTextSize(12f);
        panelStatusBadge.setTypeface(Typeface.DEFAULT_BOLD);
        panelStatusBadge.setTextColor(Color.WHITE);
        panelStatusBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        panelStatusBadge.setBackground(createCardBg(Color.parseColor("#34C759"), Color.TRANSPARENT, dp(10)));

        statusLayout.addView(panelStatusBadge);
        statusCard.addView(statusLayout);
        panelRoot.addView(statusCard);

        // --- 仨功能占位 (Three Placeholder Features) ---
        panelRoot.addView(createSectionTitle("模块功能")); // Flush left

        LinearLayout featureCard = createInnerCard();
        LinearLayout featureLayout = new LinearLayout(getContext());
        featureLayout.setOrientation(LinearLayout.VERTICAL);
        featureLayout.setPadding(dp14, dp12, dp14, dp12);
        featureLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // 功能一 (占位)
        switchFeature1 = new Switch(getContext());
        featureLayout.addView(createSwitchRowLayout("功能模块 01 (待接入)", "核心功能占位 01，可在功能源码中接入 Hook 逻辑", switchFeature1, new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Toast.makeText(getContext(), "功能 01: " + (isChecked ? "已开启" : "已关闭"), Toast.LENGTH_SHORT).show();
            }
        }));

        featureLayout.addView(createDivider());

        // 功能二 (占位)
        switchFeature2 = new Switch(getContext());
        featureLayout.addView(createSwitchRowLayout("功能模块 02 (待接入)", "核心功能占位 02，可在功能源码中接入 Hook 逻辑", switchFeature2, new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Toast.makeText(getContext(), "功能 02: " + (isChecked ? "已开启" : "已关闭"), Toast.LENGTH_SHORT).show();
            }
        }));

        featureLayout.addView(createDivider());

        // 功能三 (占位)
        switchFeature3 = new Switch(getContext());
        featureLayout.addView(createSwitchRowLayout("功能模块 03 (待接入)", "核心功能占位 03，可在功能源码中接入 Hook 逻辑", switchFeature3, new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Toast.makeText(getContext(), "功能 03: " + (isChecked ? "已开启" : "已关闭"), Toast.LENGTH_SHORT).show();
            }
        }));

        featureCard.addView(featureLayout);
        panelRoot.addView(featureCard);

        // --- 云端服务 ---
        panelRoot.addView(createSectionTitle("云端服务")); // Flush left

        LinearLayout cloudCard = createInnerCard();
        LinearLayout cloudRow = new LinearLayout(getContext());
        cloudRow.setOrientation(LinearLayout.HORIZONTAL);
        cloudRow.setPadding(dp14, dp12, dp14, dp12);
        cloudRow.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        Button btnGroup = createSecondaryButton("官方群聊", new OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyManager.handleEvent(getContext(), 3, "1031891543");
            }
        });
        cloudRow.addView(btnGroup);

        View spacerCloud = new View(getContext());
        spacerCloud.setLayoutParams(new LinearLayout.LayoutParams(dp8, 1));
        cloudRow.addView(spacerCloud);

        Button btnWeb = createSecondaryButton("官方网站", new OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyManager.handleEvent(getContext(), 1, "https://epic.t60.top/");
            }
        });
        cloudRow.addView(btnWeb);
        cloudCard.addView(cloudRow);
        panelRoot.addView(cloudCard);

        scrollView.addView(panelRoot);
        inGamePanelScrim.addView(scrollView);
        addView(inGamePanelScrim);
    }

    // ==========================================
    // 3. 悬浮球 (全屏幕自由拖拽移动，验证通过后展示)
    // ==========================================
    private void buildFloatingBall() {
        int ballSize = dp(56);
        floatingBall = new FrameLayout(getContext());
        LayoutParams ballParams = new LayoutParams(ballSize, ballSize);
        ballParams.setMargins(dp(20), dp(80), 0, 0);
        floatingBall.setLayoutParams(ballParams);
        floatingBall.setVisibility(View.GONE);
        floatingBall.setElevation(dp(10));

        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#0A84FF"), Color.parseColor("#0056B3")}
        );
        bg.setShape(GradientDrawable.OVAL);
        floatingBall.setBackground(bg);

        ImageView logoImg = new ImageView(getContext());
        int pad = dp(10);
        logoImg.setPadding(pad, pad, pad, pad);
        logoImg.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        Bitmap bmp = getLogoBitmap();
        if (bmp != null) {
            logoImg.setImageBitmap(bmp);
        } else {
            logoImg.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        floatingBall.addView(logoImg);

        setupBallDragListener(floatingBall);
        addView(floatingBall);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupBallDragListener(final View ball) {
        ball.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // 全屏幕自由拖拽移动
                        float newX = Math.max(0, Math.min(event.getRawX() + dX, getWidth() - v.getWidth()));
                        float newY = Math.max(0, Math.min(event.getRawY() + dY, getHeight() - v.getHeight()));
                        v.setX(newX);
                        v.setY(newY);
                        return true;

                    case MotionEvent.ACTION_UP:
                        float diffX = Math.abs(event.getRawX() - downRawX);
                        float diffY = Math.abs(event.getRawY() - downRawY);
                        if (diffX < touchSlop && diffY < touchSlop) {
                            togglePanel();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void setupListeners() {
        fullBtnVerify.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String key = fullCardInput.getText().toString().trim();
                if (key.isEmpty()) {
                    Toast.makeText(getContext(), "请输入卡密", Toast.LENGTH_SHORT).show();
                    return;
                }

                fullLoadingBar.setVisibility(View.VISIBLE);
                fullBtnVerify.setEnabled(false);
                fullStatusTip.setText("正在连接云端验证...");

                verifyManager.verifyCard(key, new VerifyManager.VerifyCallback() {
                    @Override
                    public void onResult(final boolean success, final String message) {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                fullLoadingBar.setVisibility(View.GONE);
                                fullBtnVerify.setEnabled(true);
                                fullStatusTip.setText(message);

                                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

                                if (success) {
                                    onVerifySuccess();
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private void loadNotice() {
        verifyManager.loadSoftwareNotice(new VerifyManager.NoticeCallback() {
            @Override
            public void onResult(boolean success, final String title, final String content) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        fullNoticeTitle.setText(title);
                        fullNoticeContent.setText(content);
                    }
                });
            }
        });

        // 自动验证已保存卡密
        String savedCard = appSettings.getCard();
        if (appSettings.isAutoVerify() && !savedCard.trim().isEmpty() && !verifyManager.isVerified()) {
            verifyManager.verifyCard(savedCard, new VerifyManager.VerifyCallback() {
                @Override
                public void onResult(final boolean success, String message) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                onVerifySuccess();
                            }
                        }
                    });
                }
            });
        }
    }

    private void checkInitialState() {
        if (verifyManager.isVerified()) {
            onVerifySuccess();
        } else {
            fullScreenVerifyView.setVisibility(View.VISIBLE);
            floatingBall.setVisibility(View.GONE);
            inGamePanelScrim.setVisibility(View.GONE);
        }
    }

    private void onVerifySuccess() {
        fullScreenVerifyView.setVisibility(View.GONE);
        panelStatusBadge.setText("已授权");
        panelStatusBadge.setBackground(createCardBg(Color.parseColor("#34C759"), Color.TRANSPARENT, dp(10)));
        panelStatusDetail.setText("已激活 (到期: " + VerifyManager.formatDate(verifyManager.getExpireTimestamp()) + ")");
        floatingBall.setVisibility(View.VISIBLE);
    }

    public void openPanel() {
        if (!verifyManager.isVerified()) {
            fullScreenVerifyView.setVisibility(View.VISIBLE);
            inGamePanelScrim.setVisibility(View.GONE);
            floatingBall.setVisibility(View.GONE);
            return;
        }
        isPanelOpen = true;
        inGamePanelScrim.setVisibility(View.VISIBLE);
        floatingBall.setVisibility(View.GONE);
    }

    public void closePanel() {
        isPanelOpen = false;
        inGamePanelScrim.setVisibility(View.GONE);
        if (verifyManager.isVerified()) {
            floatingBall.setVisibility(View.VISIBLE);
        }
    }

    public void togglePanel() {
        if (isPanelOpen) closePanel(); else openPanel();
    }

    private Bitmap getLogoBitmap() {
        try {
            int resId = getContext().getResources().getIdentifier("logo", "drawable", getPackageName());
            if (resId != 0) {
                return BitmapFactory.decodeResource(getContext().getResources(), resId);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getPackageName() {
        return getContext().getPackageName();
    }

    private TextView createSectionTitle(String title) {
        TextView tv = new TextView(getContext());
        tv.setText(title);
        tv.setTextSize(13f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(Color.parseColor("#8E8E93"));
        tv.setPadding(0, dp(10), 0, dp(4)); // 0 start padding, flush left
        return tv;
    }

    private LinearLayout createInnerCard() {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        card.setBackground(createCardBg(Color.parseColor("#FFFFFF"), Color.parseColor("#ECEFF4"), dp(14)));
        card.setElevation(dp(2));
        return card;
    }

    private Button createSecondaryButton(String text, OnClickListener onClick) {
        Button btn = new Button(getContext());
        btn.setText(text);
        btn.setTextSize(13f);
        btn.setTextColor(Color.parseColor("#1C1C1E"));
        btn.setBackground(createRippleDrawable(Color.parseColor("#F2F3F7"), Color.parseColor("#E2E4EB"), dp(10)));
        btn.setLayoutParams(new LinearLayout.LayoutParams(0, dp(38), 1f));
        btn.setOnClickListener(onClick);
        return btn;
    }

    private View createSwitchRowLayout(String title, String desc, Switch sw, CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout textCol = new LinearLayout(getContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        TextView titleTv = new TextView(getContext());
        titleTv.setText(title);
        titleTv.setTextSize(14f);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        titleTv.setTextColor(Color.parseColor("#1C1C1E"));

        TextView descTv = new TextView(getContext());
        descTv.setText(desc);
        descTv.setTextSize(11f);
        descTv.setTextColor(Color.parseColor("#8E8E93"));

        textCol.addView(titleTv);
        textCol.addView(descTv);
        row.addView(textCol);

        sw.setOnCheckedChangeListener(listener);
        row.addView(sw);

        return row;
    }

    private View createDivider() {
        View div = new View(getContext());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1));
        p.setMargins(0, dp(6), 0, dp(6));
        div.setLayoutParams(p);
        div.setBackgroundColor(Color.parseColor("#E5E9F0"));
        return div;
    }

    private GradientDrawable createCardBg(int bgColor, int strokeColor, int radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(radius);
        if (strokeColor != Color.TRANSPARENT) {
            gd.setStroke(dp(1), strokeColor);
        }
        return gd;
    }

    private RippleDrawable createRippleDrawable(int normalColor, int pressedColor, int radius) {
        GradientDrawable content = createCardBg(normalColor, Color.TRANSPARENT, radius);
        GradientDrawable mask = createCardBg(Color.BLACK, Color.TRANSPARENT, radius);
        return new RippleDrawable(ColorStateList.valueOf(pressedColor), content, mask);
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                v,
                getContext().getResources().getDisplayMetrics()
        );
    }
}
