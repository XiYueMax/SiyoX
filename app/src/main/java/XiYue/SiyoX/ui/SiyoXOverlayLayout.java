// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import XiYue.SiyoX.SiyoXConfig;
import XiYue.SiyoX.data.AppSettings;
import XiYue.SiyoX.data.SiyoXDirManager;
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
    private RippleWaveView rippleWaveView;
    private FrameLayout panelContainer;

    // Full-screen verify components
    private TextView fullNoticeTitle;
    private TextView fullNoticeContent;
    private EditText fullCardInput;
    private Button fullBtnVerify;
    private Button fullBtnExit;
    private ProgressBar fullLoadingBar;
    private TextView fullStatusTip;

    // In-game Panel components (Left Category + Right Feature List)
    private LinearLayout categoryListLayout;
    private LinearLayout featureListContent;
    private int currentCategoryIndex = 0;
    private final List<TextView> categoryTabViews = new ArrayList<>();

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
        setClipChildren(false);
        setClipToPadding(false);
        setFocusable(false);
        setFocusableInTouchMode(false);

        // 确保目录和图标文件已初始化
        SiyoXDirManager.initDirectories(activity.getApplicationContext());

        initUI();
        setupListeners();
        loadNotice();
        checkInitialState();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int screenW = dm.widthPixels;
        int screenH = dm.heightPixels;
        setMeasuredDimension(screenW, screenH);
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(screenW, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(screenH, MeasureSpec.EXACTLY)
        );
    }

    private void initUI() {
        // 1. 全屏横屏验证窗口 (未验证时展示)
        buildFullScreenVerifyWindow();

        // 2. 游戏内悬浮功能面板 (左侧分类 + 右侧功能列表 + MiuiX 开关)
        buildInGamePanel();

        // 3. 全屏幕可自由拖拽悬浮球 (验证通过后展示，无蓝边，支持全屏移动)
        buildFloatingBall();
    }

    // ==========================================
    // 1. 全屏横屏验证窗口 (未验证时展示)
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

        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int screenW = dm.widthPixels;
        int screenH = dm.heightPixels;

        // 主居中卡片容器
        FrameLayout cardWrapper = new FrameLayout(getContext());
        LayoutParams wrapParams = new LayoutParams(
                (int)(screenW * 0.94f),
                (int)(screenH * 0.92f)
        );
        wrapParams.gravity = Gravity.CENTER;
        cardWrapper.setLayoutParams(wrapParams);
        cardWrapper.setBackground(createCardBg(Color.parseColor("#F9FAFC"), Color.parseColor("#E5E9F0"), dp(20)));
        cardWrapper.setPadding(dp18, dp16, dp18, dp16);
        cardWrapper.setClickable(true);

        LinearLayout mainHorizontalLayout = new LinearLayout(getContext());
        mainHorizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
        mainHorizontalLayout.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // ======================== 左半部分 ========================
        LinearLayout leftColumn = new LinearLayout(getContext());
        leftColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.1f);
        leftParams.setMargins(0, 0, dp14, 0);
        leftColumn.setLayoutParams(leftParams);

        // 左上方: 显示图标 + 软件名 SiyoX
        LinearLayout topLeftHeader = new LinearLayout(getContext());
        topLeftHeader.setOrientation(LinearLayout.HORIZONTAL);
        topLeftHeader.setGravity(Gravity.CENTER_VERTICAL);
        topLeftHeader.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        topLeftHeader.setPadding(0, 0, 0, dp8);

        ImageView logoView = new ImageView(getContext());
        int logoSize = dp(46);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(logoSize, logoSize);
        logoView.setLayoutParams(logoParams);
        Bitmap logoBmp = LogoLoader.getLogo(getContext());
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

        // 左下方: 公告栏
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
    // 2. 游戏内悬浮功能面板 (左侧功能分类 + 右侧功能列表 + MiuiX 开关)
    // 分类包含: 基础辅助, 视觉增强, 游戏微调, 个人中心(退出登录), 关于软件
    // ==========================================
    private void buildInGamePanel() {
        int dp10 = dp(10);
        int dp12 = dp(12);
        int dp14 = dp(14);
        int dp16 = dp(16);
        int dp18 = dp(18);
        int dp8 = dp(8);

        inGamePanelScrim = new FrameLayout(getContext());
        inGamePanelScrim.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        inGamePanelScrim.setVisibility(View.GONE);

        // 范围波背景层 (点击悬浮球时从中心向外扩展)
        rippleWaveView = new RippleWaveView(getContext());
        rippleWaveView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        rippleWaveView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                closePanel();
            }
        });
        inGamePanelScrim.addView(rippleWaveView);

        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int screenW = dm.widthPixels;
        int screenH = dm.heightPixels;

        // 功能面板容器 (位于半透明黑色波纹背景上方，不受背景遮盖)
        panelContainer = new FrameLayout(getContext());
        int panelWidth = (int)(screenW * 0.88f);
        int panelHeight = (int)(screenH * 0.86f);
        LayoutParams panelParams = new LayoutParams(panelWidth, panelHeight);
        panelParams.gravity = Gravity.CENTER;
        panelContainer.setLayoutParams(panelParams);
        panelContainer.setBackground(createCardBg(Color.parseColor("#F9FAFC"), Color.parseColor("#E5E9F0"), dp(20)));
        panelContainer.setPadding(dp18, dp14, dp18, dp16);
        panelContainer.setElevation(dp(12));
        panelContainer.setClickable(true);

        LinearLayout panelRoot = new LinearLayout(getContext());
        panelRoot.setOrientation(LinearLayout.VERTICAL);
        panelRoot.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // --- 顶部栏 (Logo + SiyoX 功能面板 + 收起按钮) ---
        LinearLayout topBar = new LinearLayout(getContext());
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        topBar.setPadding(0, 0, 0, dp8);

        ImageView topLogo = new ImageView(getContext());
        int topLogoSize = dp(32);
        topLogo.setLayoutParams(new LinearLayout.LayoutParams(topLogoSize, topLogoSize));
        Bitmap logoBmp = LogoLoader.getLogo(getContext());
        if (logoBmp != null) {
            topLogo.setImageBitmap(logoBmp);
        } else {
            topLogo.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        topLogo.setBackground(createCardBg(Color.WHITE, Color.parseColor("#E5E9F0"), dp(8)));
        topLogo.setClipToOutline(true);
        topBar.addView(topLogo);

        LinearLayout titleContainer = new LinearLayout(getContext());
        titleContainer.setOrientation(LinearLayout.HORIZONTAL);
        titleContainer.setGravity(Gravity.CENTER_VERTICAL);
        titleContainer.setPadding(dp10, 0, 0, 0);
        titleContainer.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        TextView titleSiyoX = new TextView(getContext());
        titleSiyoX.setText(SiyoXConfig.APP_NAME + " ");
        titleSiyoX.setTextSize(18f);
        titleSiyoX.setTypeface(Typeface.DEFAULT_BOLD);
        titleSiyoX.setTextColor(Color.parseColor("#0A84FF"));

        TextView titleSub = new TextView(getContext());
        titleSub.setText("功能面板");
        titleSub.setTextSize(15f);
        titleSub.setTypeface(Typeface.DEFAULT_BOLD);
        titleSub.setTextColor(Color.parseColor("#1C1C1E"));

        titleContainer.addView(titleSiyoX);
        titleContainer.addView(titleSub);
        topBar.addView(titleContainer);

        // 状态徽章
        TextView tvBadge = new TextView(getContext());
        tvBadge.setText("已授权");
        tvBadge.setTextSize(11f);
        tvBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvBadge.setTextColor(Color.WHITE);
        tvBadge.setPadding(dp8, dp(3), dp8, dp(3));
        tvBadge.setBackground(createCardBg(Color.parseColor("#34C759"), Color.TRANSPARENT, dp(8)));
        topBar.addView(tvBadge);

        View spacerTop = new View(getContext());
        spacerTop.setLayoutParams(new LinearLayout.LayoutParams(dp10, 1));
        topBar.addView(spacerTop);

        TextView btnMinimize = new TextView(getContext());
        btnMinimize.setText("收起");
        btnMinimize.setTextSize(12f);
        btnMinimize.setTextColor(Color.parseColor("#8E8E93"));
        btnMinimize.setPadding(dp10, dp(5), dp10, dp(5));
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

        // --- 主体部分: 左侧分类栏 + 右侧功能列表 ---
        LinearLayout mainContentRow = new LinearLayout(getContext());
        mainContentRow.setOrientation(LinearLayout.HORIZONTAL);
        mainContentRow.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        // 1. 左侧功能分类栏 (Width 130dp)
        LinearLayout leftSidebar = new LinearLayout(getContext());
        leftSidebar.setOrientation(LinearLayout.VERTICAL);
        leftSidebar.setLayoutParams(new LinearLayout.LayoutParams(dp(130), LayoutParams.MATCH_PARENT));
        leftSidebar.setBackground(createCardBg(Color.parseColor("#FFFFFF"), Color.parseColor("#ECEFF4"), dp(14)));
        leftSidebar.setPadding(dp8, dp8, dp8, dp8);

        categoryListLayout = new LinearLayout(getContext());
        categoryListLayout.setOrientation(LinearLayout.VERTICAL);
        categoryListLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // 分类: 基础辅助, 视觉增强, 游戏微调, 个人中心, 关于软件
        String[] categories = new String[]{"基础辅助", "视觉增强", "游戏微调", "个人中心", "关于软件"};
        categoryTabViews.clear();

        for (int i = 0; i < categories.length; i++) {
            final int index = i;
            TextView tabView = new TextView(getContext());
            tabView.setText(categories[i]);
            tabView.setTextSize(13f);
            tabView.setPadding(dp12, dp10, dp12, dp10);
            tabView.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            p.setMargins(0, dp(4), 0, dp(4));
            tabView.setLayoutParams(p);

            tabView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchCategory(index);
                }
            });

            categoryTabViews.add(tabView);
            categoryListLayout.addView(tabView);
        }
        leftSidebar.addView(categoryListLayout);
        mainContentRow.addView(leftSidebar);

        View colDivider = new View(getContext());
        colDivider.setLayoutParams(new LinearLayout.LayoutParams(dp10, 1));
        mainContentRow.addView(colDivider);

        // 2. 右侧功能列表容器
        ScrollView rightScrollView = new ScrollView(getContext());
        rightScrollView.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
        rightScrollView.setVerticalScrollBarEnabled(false);
        rightScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        featureListContent = new LinearLayout(getContext());
        featureListContent.setOrientation(LinearLayout.VERTICAL);
        featureListContent.setLayoutParams(new ScrollView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        rightScrollView.addView(featureListContent);
        mainContentRow.addView(rightScrollView);

        panelRoot.addView(mainContentRow);
        panelContainer.addView(panelRoot);
        inGamePanelScrim.addView(panelContainer);
        addView(inGamePanelScrim);

        switchCategory(0);
    }

    private void switchCategory(int categoryIndex) {
        this.currentCategoryIndex = categoryIndex;
        for (int i = 0; i < categoryTabViews.size(); i++) {
            TextView tv = categoryTabViews.get(i);
            if (i == categoryIndex) {
                tv.setTypeface(Typeface.DEFAULT_BOLD);
                tv.setTextColor(Color.parseColor("#0A84FF"));
                tv.setBackground(createCardBg(Color.parseColor("#EBF5FF"), Color.TRANSPARENT, dp(10)));
            } else {
                tv.setTypeface(Typeface.DEFAULT);
                tv.setTextColor(Color.parseColor("#666666"));
                tv.setBackground(createCardBg(Color.TRANSPARENT, Color.TRANSPARENT, dp(10)));
            }
        }

        renderFeatureList(categoryIndex);
    }

    private void renderFeatureList(int categoryIndex) {
        featureListContent.removeAllViews();

        int dp14 = dp(14);
        int dp12 = dp(12);
        int dp8 = dp(8);

        String catName = categoryTabViews.get(categoryIndex).getText().toString();
        TextView titleTv = new TextView(getContext());
        titleTv.setText(catName);
        titleTv.setTextSize(13f);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        titleTv.setTextColor(Color.parseColor("#8E8E93"));
        titleTv.setPadding(0, 0, 0, dp(6));
        featureListContent.addView(titleTv);

        if (categoryIndex == 0) {
            // 基础辅助
            featureListContent.addView(createMiuiXFeatureCard("功能模块 01 (占位)", "核心功能模块 01，可在源码中接入具体功能", false, new MiuiXSwitch.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(MiuiXSwitch switchView, boolean isChecked) {
                    Toast.makeText(getContext(), "功能 01: " + (isChecked ? "已启用" : "已停用"), Toast.LENGTH_SHORT).show();
                }
            }));

            featureListContent.addView(createMiuiXFeatureCard("功能模块 02 (占位)", "核心功能模块 02，可在源码中接入具体功能", false, new MiuiXSwitch.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(MiuiXSwitch switchView, boolean isChecked) {
                    Toast.makeText(getContext(), "功能 02: " + (isChecked ? "已启用" : "已停用"), Toast.LENGTH_SHORT).show();
                }
            }));
        } else if (categoryIndex == 1) {
            // 视觉增强
            featureListContent.addView(createMiuiXFeatureCard("功能模块 03 (占位)", "视觉增强占位模块，支持自适应开关切换", false, new MiuiXSwitch.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(MiuiXSwitch switchView, boolean isChecked) {
                    Toast.makeText(getContext(), "功能 03: " + (isChecked ? "已启用" : "已停用"), Toast.LENGTH_SHORT).show();
                }
            }));

            featureListContent.addView(createMiuiXFeatureCard("功能模块 04 (占位)", "视觉增强扩展占位模块，可在功能源码接入", false, new MiuiXSwitch.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(MiuiXSwitch switchView, boolean isChecked) {
                    Toast.makeText(getContext(), "功能 04: " + (isChecked ? "已启用" : "已停用"), Toast.LENGTH_SHORT).show();
                }
            }));
        } else if (categoryIndex == 2) {
            // 游戏微调
            featureListContent.addView(createMiuiXFeatureCard("功能模块 05 (占位)", "游戏微调占位模块，支持自定义参数配置", false, new MiuiXSwitch.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(MiuiXSwitch switchView, boolean isChecked) {
                    Toast.makeText(getContext(), "功能 05: " + (isChecked ? "已启用" : "已停用"), Toast.LENGTH_SHORT).show();
                }
            }));
        } else if (categoryIndex == 3) {
            // 个人中心 (User Center)
            LinearLayout profileCard = createInnerCard();
            profileCard.setPadding(dp16, dp14, dp16, dp14);

            profileCard.addView(createInfoRowItem("设备 Android ID", verifyManager.getAndroidId()));
            profileCard.addView(createDivider());
            profileCard.addView(createInfoRowItem("授权卡密", appSettings.getCard().isEmpty() ? "未绑定" : appSettings.getCard()));
            profileCard.addView(createDivider());
            profileCard.addView(createInfoRowItem("到期时间", VerifyManager.formatDate(verifyManager.getExpireTimestamp())));
            profileCard.addView(createDivider());
            profileCard.addView(createInfoRowItem("验证提供商", verifyManager.getActiveProviderName()));

            featureListContent.addView(profileCard);

            // 退出登录按钮
            Button btnLogout = new Button(getContext());
            btnLogout.setText("退出登录");
            btnLogout.setTextSize(14f);
            btnLogout.setTypeface(Typeface.DEFAULT_BOLD);
            btnLogout.setTextColor(Color.parseColor("#FF3B30"));
            btnLogout.setBackground(createRippleDrawable(Color.parseColor("#FDE8E8"), Color.parseColor("#FBD5D5"), dp(12)));
            LinearLayout.LayoutParams lpLogout = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(44));
            lpLogout.setMargins(0, dp14, 0, 0);
            btnLogout.setLayoutParams(lpLogout);

            btnLogout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    verifyManager.logout();
                    closePanel();
                    floatingBall.setVisibility(View.GONE);
                    fullScreenVerifyView.setVisibility(View.VISIBLE);
                    fullCardInput.setText("");
                    Toast.makeText(getContext(), "已退出登录", Toast.LENGTH_SHORT).show();
                }
            });
            featureListContent.addView(btnLogout);

        } else if (categoryIndex == 4) {
            // 关于软件 (About Software)
            LinearLayout aboutCard = createInnerCard();
            aboutCard.setPadding(dp16, dp14, dp16, dp14);

            aboutCard.addView(createInfoRowItem("软件名称", SiyoXConfig.APP_NAME));
            aboutCard.addView(createDivider());
            aboutCard.addView(createInfoRowItem("当前版本", SiyoXConfig.VERSION_NAME));
            aboutCard.addView(createDivider());
            aboutCard.addView(createInfoRowItem("注入作用域", SiyoXConfig.TARGET_PACKAGE));
            aboutCard.addView(createDivider());
            aboutCard.addView(createInfoRowItem("软件作者", SiyoXConfig.AUTHOR));
            aboutCard.addView(createDivider());
            aboutCard.addView(createInfoRowItem("数据目录", "/sdcard/Android/data/" + SiyoXConfig.TARGET_PACKAGE + "/SiyoX/"));

            featureListContent.addView(aboutCard);

            Button btnGithub = new Button(getContext());
            btnGithub.setText("前往 GitHub 查看源码");
            btnGithub.setTextSize(14f);
            btnGithub.setTypeface(Typeface.DEFAULT_BOLD);
            btnGithub.setTextColor(Color.WHITE);
            btnGithub.setBackground(createRippleDrawable(Color.parseColor("#0A84FF"), Color.parseColor("#0066CC"), dp(12)));
            LinearLayout.LayoutParams lpGh = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(44));
            lpGh.setMargins(0, dp14, 0, 0);
            btnGithub.setLayoutParams(lpGh);

            btnGithub.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(SiyoXConfig.GITHUB_URL));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getContext().startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "无法打开浏览器: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            featureListContent.addView(btnGithub);
        }
    }

    private View createInfoRowItem(String label, String value) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        row.setPadding(0, dp(4), 0, dp(4));

        TextView tvLabel = new TextView(getContext());
        tvLabel.setText(label);
        tvLabel.setTextSize(13f);
        tvLabel.setTextColor(Color.parseColor("#8E8E93"));
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tvLabel);

        TextView tvValue = new TextView(getContext());
        tvValue.setText(value);
        tvValue.setTextSize(13f);
        tvValue.setTypeface(Typeface.DEFAULT_BOLD);
        tvValue.setTextColor(Color.parseColor("#1C1C1E"));
        row.addView(tvValue);

        return row;
    }

    private View createMiuiXFeatureCard(String title, String desc, boolean initial, MiuiXSwitch.OnCheckedChangeListener listener) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(cardParams);
        card.setBackground(createCardBg(Color.WHITE, Color.parseColor("#ECEFF4"), dp(14)));
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setElevation(dp(1));

        LinearLayout textCol = new LinearLayout(getContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(title);
        tvTitle.setTextSize(14f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(Color.parseColor("#1C1C1E"));
        textCol.addView(tvTitle);

        TextView tvDesc = new TextView(getContext());
        tvDesc.setText(desc);
        tvDesc.setTextSize(11f);
        tvDesc.setTextColor(Color.parseColor("#8E8E93"));
        tvDesc.setPadding(0, dp(2), 0, 0);
        textCol.addView(tvDesc);

        card.addView(textCol);

        // MiuiX 风格开关
        MiuiXSwitch miuixSwitch = new MiuiXSwitch(getContext());
        miuixSwitch.setChecked(initial, false);
        miuixSwitch.setOnCheckedChangeListener(listener);
        card.addView(miuixSwitch);

        return card;
    }

    // ==========================================
    // 3. 悬浮球 (全屏幕自由拖拽移动，无蓝边，正常显示Logo图标)
    // ==========================================
    private void buildFloatingBall() {
        int ballSize = dp(54);
        floatingBall = new FrameLayout(getContext());
        LayoutParams ballParams = new LayoutParams(ballSize, ballSize);
        ballParams.gravity = Gravity.TOP | Gravity.START;
        ballParams.leftMargin = dp(20);
        ballParams.topMargin = dp(80);
        floatingBall.setLayoutParams(ballParams);
        floatingBall.setVisibility(View.GONE);
        floatingBall.setElevation(dp(8));

        // 无蓝色边框，圆角白色柔和阴影背景
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#FFFFFF"));
        bg.setCornerRadius(dp(16));
        floatingBall.setBackground(bg);

        ImageView logoImg = new ImageView(getContext());
        int pad = dp(6);
        logoImg.setPadding(pad, pad, pad, pad);
        logoImg.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        Bitmap bmp = LogoLoader.getLogo(getContext());
        if (bmp != null) {
            logoImg.setImageBitmap(bmp);
        } else {
            logoImg.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        logoImg.setClipToOutline(true);
        floatingBall.addView(logoImg);

        setupBallDragListener(floatingBall);
        addView(floatingBall);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupBallDragListener(final View ball) {
        ball.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                DisplayMetrics dm = activity.getResources().getDisplayMetrics();
                int screenW = dm.widthPixels;
                int screenH = dm.heightPixels;

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        FrameLayout.LayoutParams curLp = (FrameLayout.LayoutParams) v.getLayoutParams();
                        dX = curLp.leftMargin - event.getRawX();
                        dY = curLp.topMargin - event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // 全屏幕无阻碍自由拖拽移动
                        int newLeft = (int) Math.max(0, Math.min(event.getRawX() + dX, screenW - v.getWidth()));
                        int newTop = (int) Math.max(0, Math.min(event.getRawY() + dY, screenH - v.getHeight()));
                        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
                        lp.leftMargin = newLeft;
                        lp.topMargin = newTop;
                        lp.gravity = Gravity.TOP | Gravity.START;
                        v.setLayoutParams(lp);
                        return true;

                    case MotionEvent.ACTION_UP:
                        float diffX = Math.abs(event.getRawX() - downRawX);
                        float diffY = Math.abs(event.getRawY() - downRawY);
                        if (diffX < touchSlop && diffY < touchSlop) {
                            FrameLayout.LayoutParams curPos = (FrameLayout.LayoutParams) v.getLayoutParams();
                            openPanelWithRipple(curPos.leftMargin + v.getWidth() / 2f, curPos.topMargin + v.getHeight() / 2f);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    // 4. 点击悬浮球时，从悬浮球中心向外扩散半透明黑色波纹，随后弹出功能面板
    public void openPanelWithRipple(float originX, float originY) {
        if (!verifyManager.isVerified()) {
            fullScreenVerifyView.setVisibility(View.VISIBLE);
            inGamePanelScrim.setVisibility(View.GONE);
            floatingBall.setVisibility(View.GONE);
            return;
        }

        isPanelOpen = true;
        inGamePanelScrim.setVisibility(View.VISIBLE);
        floatingBall.setVisibility(View.GONE);

        // 启动波纹扩散
        rippleWaveView.startExpandAnimation(originX, originY);

        // 功能面板从中心弹出 (scale 0.85 -> 1.0, alpha 0 -> 1)
        panelContainer.setScaleX(0.85f);
        panelContainer.setScaleY(0.85f);
        panelContainer.setAlpha(0f);

        panelContainer.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(280)
                .setInterpolator(new OvershootInterpolator(1.1f))
                .start();
    }

    public void closePanel() {
        if (!isPanelOpen) return;
        isPanelOpen = false;

        panelContainer.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .alpha(0f)
                .setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        panelContainer.animate().setListener(null);
                        inGamePanelScrim.setVisibility(View.GONE);
                        if (verifyManager.isVerified()) {
                            floatingBall.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .start();
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
        floatingBall.setVisibility(View.VISIBLE);
    }

    // ==========================================
    // 自定义波纹扩展背景 View (从悬浮球中心向外扩展半透明黑色波纹)
    // ==========================================
    private static class RippleWaveView extends View {
        private float centerX = 0f;
        private float centerY = 0f;
        private float currentRadius = 0f;
        private float maxRadius = 0f;
        private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public RippleWaveView(Context context) {
            super(context);
            wavePaint.setColor(Color.parseColor("#80000000")); // 半透明黑色
        }

        public void startExpandAnimation(float cx, float cy) {
            this.centerX = cx;
            this.centerY = cy;

            int w = getWidth() > 0 ? getWidth() : 2560;
            int h = getHeight() > 0 ? getHeight() : 1600;
            this.maxRadius = (float) Math.hypot(w, h);

            ValueAnimator anim = ValueAnimator.ofFloat(0f, maxRadius);
            anim.setDuration(260);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    currentRadius = (float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            anim.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (currentRadius > 0) {
                canvas.drawCircle(centerX, centerY, currentRadius, wavePaint);
            }
        }
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
