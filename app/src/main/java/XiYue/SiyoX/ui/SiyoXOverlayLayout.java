// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
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
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
    private FrameLayout cardWrapper;

    // Full-screen verify components
    private TextView fullNoticeTitle;
    private TextView fullNoticeContent;
    private EditText fullCardInput;
    private Button fullBtnVerify;
    private Button fullBtnExit;
    private ProgressBar fullLoadingBar;
    private TextView fullStatusTip;
    private MiuiXCheckBox cbRememberCard;
    private MiuiXCheckBox cbAutoLogin;

    // In-game Panel components
    private LinearLayout categoryListLayout;
    private LinearLayout featureListContent;
    private TextView tvTopExpireBadge;
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

    private int[] getRealScreenSize() {
        int w = activity.getResources().getDisplayMetrics().widthPixels;
        int h = activity.getResources().getDisplayMetrics().heightPixels;
        try {
            WindowManager wm = activity.getWindowManager();
            if (wm != null) {
                DisplayMetrics dm = new DisplayMetrics();
                wm.getDefaultDisplay().getRealMetrics(dm);
                w = dm.widthPixels;
                h = dm.heightPixels;
            }
        } catch (Throwable ignored) {}
        return new int[]{w, h};
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int[] size = getRealScreenSize();
        int screenW = size[0];
        int screenH = size[1];

        setMeasuredDimension(screenW, screenH);
        measureChildren(
                MeasureSpec.makeMeasureSpec(screenW, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(screenH, MeasureSpec.EXACTLY)
        );
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;

        if (fullScreenVerifyView != null && fullScreenVerifyView.getVisibility() != GONE) {
            fullScreenVerifyView.layout(0, 0, width, height);
            if (cardWrapper != null) {
                int cw = cardWrapper.getMeasuredWidth();
                int ch = cardWrapper.getMeasuredHeight();
                int cl = (width - cw) / 2;
                int ct = (height - ch) / 2;
                cardWrapper.layout(cl, ct, cl + cw, ct + ch);
            }
        }

        if (inGamePanelScrim != null && inGamePanelScrim.getVisibility() != GONE) {
            inGamePanelScrim.layout(0, 0, width, height);
            if (rippleWaveView != null) {
                rippleWaveView.layout(0, 0, width, height);
            }
            if (panelContainer != null) {
                int pw = panelContainer.getMeasuredWidth();
                int ph = panelContainer.getMeasuredHeight();
                int pl = (width - pw) / 2;
                int pt = (height - ph) / 2;
                panelContainer.layout(pl, pt, pl + pw, pt + ph);
            }
        }

        if (floatingBall != null && floatingBall.getVisibility() != GONE) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) floatingBall.getLayoutParams();
            int bw = floatingBall.getMeasuredWidth();
            int bh = floatingBall.getMeasuredHeight();
            floatingBall.layout(lp.leftMargin, lp.topMargin, lp.leftMargin + bw, lp.topMargin + bh);
        }
    }

    private void initUI() {
        // 1. 全屏居中横屏验证窗口 (暗黑/浅色自适应)
        buildFullScreenVerifyWindow();

        // 2. 游戏内悬浮功能面板 (暗黑/浅色自适应，精致紧凑尺寸)
        buildInGamePanel();

        // 3. 全屏幕可自由拖拽悬浮球 (无蓝边，无黑边，支持全屏自由移动)
        buildFloatingBall();
    }

    // ==========================================
    // 1. 全屏居中横屏验证窗口
    // 自动检测暗黑模式，隐藏网络验证提供商，Siyo黑/白+X蓝，CheckBox勾选框，退出按钮白色波纹
    // ==========================================
    private void buildFullScreenVerifyWindow() {
        boolean isDark = SiyoXTheme.isDarkMode(getContext());

        int dp10 = dp(10);
        int dp12 = dp(12);
        int dp14 = dp(14);
        int dp16 = dp(16);
        int dp18 = dp(18);
        int dp8 = dp(8);

        fullScreenVerifyView = new FrameLayout(getContext());
        fullScreenVerifyView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        fullScreenVerifyView.setBackgroundColor(SiyoXTheme.getWindowBg(isDark));
        fullScreenVerifyView.setClickable(true);

        int[] size = getRealScreenSize();
        int screenW = size[0];
        int screenH = size[1];

        // 主居中卡片容器 (屏幕绝对正中心)
        cardWrapper = new FrameLayout(getContext());
        int wrapWidth = (int) (screenW * 0.82f);
        int wrapHeight = (int) (screenH * 0.82f);
        FrameLayout.LayoutParams wrapParams = new FrameLayout.LayoutParams(wrapWidth, wrapHeight, Gravity.CENTER);
        cardWrapper.setLayoutParams(wrapParams);
        cardWrapper.setBackground(createCardBg(SiyoXTheme.getCardBg(isDark), Color.TRANSPARENT, dp(20)));
        cardWrapper.setPadding(dp18, dp16, dp18, dp16);
        cardWrapper.setClickable(true);

        LinearLayout mainHorizontalLayout = new LinearLayout(getContext());
        mainHorizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
        mainHorizontalLayout.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // ======================== 左半部分 ========================
        LinearLayout leftColumn = new LinearLayout(getContext());
        leftColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.05f);
        leftParams.setMargins(0, 0, dp14, 0);
        leftColumn.setLayoutParams(leftParams);

        // 左上方: 显示图标 + 软件名 Siyo(黑/白) X(蓝)
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
        logoView.setBackground(createCardBg(isDark ? Color.parseColor("#2A2A2E") : Color.WHITE, Color.TRANSPARENT, dp(12)));
        logoView.setClipToOutline(true);
        topLeftHeader.addView(logoView);

        LinearLayout titleTextCol = new LinearLayout(getContext());
        titleTextCol.setOrientation(LinearLayout.VERTICAL);
        titleTextCol.setPadding(dp10, 0, 0, 0);

        // 软件名 (自适应暗黑模式: Siyo 黑/白 + X 蓝)
        titleTextCol.addView(createSiyoXTitle(20f, isDark));

        // 版本号 (不展示任何验证提供商)
        TextView tvVersion = new TextView(getContext());
        tvVersion.setText(SiyoXConfig.VERSION_NAME);
        tvVersion.setTextSize(11f);
        tvVersion.setTextColor(SiyoXTheme.getTextSecondary(isDark));
        titleTextCol.addView(tvVersion);

        topLeftHeader.addView(titleTextCol);
        leftColumn.addView(topLeftHeader);

        leftColumn.addView(createDivider(isDark));

        // 左下方: 公告栏 (无emoji)
        TextView tvNoticeLabel = createSectionTitle("公告栏", isDark);
        leftColumn.addView(tvNoticeLabel);

        LinearLayout noticeCard = createInnerCard(isDark);
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
        fullNoticeTitle.setTextColor(SiyoXTheme.getAccentBlue());
        noticeInner.addView(fullNoticeTitle);

        fullNoticeContent = new TextView(getContext());
        fullNoticeContent.setText("欢迎使用 SiyoX 模块！正在连接云端获取最新公告...");
        fullNoticeContent.setTextSize(11.5f);
        fullNoticeContent.setLineSpacing(dp(2), 1.15f);
        fullNoticeContent.setTextColor(SiyoXTheme.getTextPrimary(isDark));
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
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.25f);
        rightParams.setMargins(dp14, 0, 0, 0);
        rightColumn.setLayoutParams(rightParams);

        TextView tvAuthLabel = createSectionTitle("卡密授权", isDark);
        rightColumn.addView(tvAuthLabel);

        LinearLayout cardKeyCard = createInnerCard(isDark);
        LinearLayout cardKeyLayout = new LinearLayout(getContext());
        cardKeyLayout.setOrientation(LinearLayout.VERTICAL);
        cardKeyLayout.setPadding(dp16, dp14, dp16, dp14);
        cardKeyLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        fullCardInput = new EditText(getContext());
        fullCardInput.setHint("请输入授权卡密");
        fullCardInput.setText(appSettings.isRememberCard() ? appSettings.getCard() : "");
        fullCardInput.setTextSize(14f);
        fullCardInput.setSingleLine(true);
        fullCardInput.setInputType(InputType.TYPE_CLASS_TEXT);
        fullCardInput.setPadding(dp12, dp12, dp12, dp12);
        fullCardInput.setBackground(createCardBg(SiyoXTheme.getInputBg(isDark), SiyoXTheme.getInputBorder(isDark), dp(10)));
        fullCardInput.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        fullCardInput.setHintTextColor(SiyoXTheme.getInputHint(isDark));
        cardKeyLayout.addView(fullCardInput);

        // 选项勾选框行：记住卡密 & 自动登录
        LinearLayout optionsRow = new LinearLayout(getContext());
        optionsRow.setOrientation(LinearLayout.HORIZONTAL);
        optionsRow.setGravity(Gravity.CENTER_VERTICAL);
        optionsRow.setPadding(0, dp10, 0, dp8);
        optionsRow.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // 记住卡密
        LinearLayout optRemember = new LinearLayout(getContext());
        optRemember.setOrientation(LinearLayout.HORIZONTAL);
        optRemember.setGravity(Gravity.CENTER_VERTICAL);
        optRemember.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        optRemember.setClickable(true);

        cbRememberCard = new MiuiXCheckBox(getContext());
        cbRememberCard.setChecked(appSettings.isRememberCard(), false);
        cbRememberCard.setOnCheckedChangeListener(new MiuiXCheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(MiuiXCheckBox checkBox, boolean isChecked) {
                appSettings.setRememberCard(isChecked);
                if (!isChecked) {
                    appSettings.setCard("");
                }
            }
        });
        optRemember.addView(cbRememberCard);

        View spacerR = new View(getContext());
        spacerR.setLayoutParams(new LinearLayout.LayoutParams(dp(6), 1));
        optRemember.addView(spacerR);

        TextView tvRemember = new TextView(getContext());
        tvRemember.setText("记住卡密");
        tvRemember.setTextSize(13f);
        tvRemember.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        optRemember.addView(tvRemember);

        optRemember.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cbRememberCard.toggle();
            }
        });
        optionsRow.addView(optRemember);

        // 自动登录
        LinearLayout optAuto = new LinearLayout(getContext());
        optAuto.setOrientation(LinearLayout.HORIZONTAL);
        optAuto.setGravity(Gravity.CENTER_VERTICAL);
        optAuto.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        optAuto.setClickable(true);

        cbAutoLogin = new MiuiXCheckBox(getContext());
        cbAutoLogin.setChecked(appSettings.isAutoVerify(), false);
        cbAutoLogin.setOnCheckedChangeListener(new MiuiXCheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(MiuiXCheckBox checkBox, boolean isChecked) {
                appSettings.setAutoVerify(isChecked);
            }
        });
        optAuto.addView(cbAutoLogin);

        View spacerA = new View(getContext());
        spacerA.setLayoutParams(new LinearLayout.LayoutParams(dp(6), 1));
        optAuto.addView(spacerA);

        TextView tvAuto = new TextView(getContext());
        tvAuto.setText("自动登录");
        tvAuto.setTextSize(13f);
        tvAuto.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        optAuto.addView(tvAuto);

        optAuto.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cbAutoLogin.toggle();
            }
        });
        optionsRow.addView(optAuto);

        cardKeyLayout.addView(optionsRow);

        // 加载指示器
        fullLoadingBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        fullLoadingBar.setIndeterminate(true);
        fullLoadingBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(4));
        barParams.setMargins(0, 0, 0, dp(6));
        fullLoadingBar.setLayoutParams(barParams);
        cardKeyLayout.addView(fullLoadingBar);

        // HWID 点击复制提示
        fullStatusTip = new TextView(getContext());
        final String hwid = verifyManager.getHWID();
        fullStatusTip.setText("HWID: " + hwid + " (点击复制)");
        fullStatusTip.setTextSize(11f);
        fullStatusTip.setTextColor(SiyoXTheme.getTextSecondary(isDark));
        fullStatusTip.setGravity(Gravity.CENTER_HORIZONTAL);
        fullStatusTip.setPadding(0, 0, 0, dp8);
        fullStatusTip.setClickable(true);
        fullStatusTip.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    ClipData clip = ClipData.newPlainText("HWID", hwid);
                    cm.setPrimaryClip(clip);
                    Toast.makeText(getContext(), "已复制 HWID", Toast.LENGTH_SHORT).show();
                }
            }
        });
        cardKeyLayout.addView(fullStatusTip);

        // 底部按钮栏: 【退出按钮在左侧】 【验证按钮在右侧】
        LinearLayout bottomActions = new LinearLayout(getContext());
        bottomActions.setOrientation(LinearLayout.HORIZONTAL);
        bottomActions.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(44)));

        // 3. 退出按钮 (左侧，白色波纹点击特效)
        fullBtnExit = new Button(getContext());
        fullBtnExit.setText("退出游戏");
        fullBtnExit.setTextSize(14f);
        fullBtnExit.setTypeface(Typeface.DEFAULT_BOLD);
        fullBtnExit.setTextColor(Color.parseColor("#FF3B30"));
        fullBtnExit.setBackground(createExitRippleDrawable(SiyoXTheme.getExitBtnBg(isDark), dp(12)));
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
    // 2. 游戏内悬浮功能面板 (精致紧凑尺寸 0.78f)
    // 分类：资源管理、辅助功能、脚本列表、个人中心、关于软件
    // ==========================================
    private void buildInGamePanel() {
        boolean isDark = SiyoXTheme.isDarkMode(getContext());

        int dp10 = dp(10);
        int dp12 = dp(12);
        int dp14 = dp(14);
        int dp16 = dp(16);
        int dp18 = dp(18);
        int dp8 = dp(8);

        inGamePanelScrim = new FrameLayout(getContext());
        inGamePanelScrim.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        inGamePanelScrim.setVisibility(View.GONE);

        // 范围波背景层 (点击悬浮球时从中心向外扩展，点击背景关闭面板)
        rippleWaveView = new RippleWaveView(getContext());
        rippleWaveView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        rippleWaveView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                closePanel();
            }
        });
        inGamePanelScrim.addView(rippleWaveView);

        int[] size = getRealScreenSize();
        int screenW = size[0];
        int screenH = size[1];

        // 5. 功能面板容器 (缩小为 0.78f 更加精致)
        panelContainer = new FrameLayout(getContext());
        int panelWidth = (int) (screenW * 0.78f);
        int panelHeight = (int) (screenH * 0.78f);
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(panelWidth, panelHeight, Gravity.CENTER);
        panelContainer.setLayoutParams(panelParams);
        panelContainer.setBackground(createCardBg(SiyoXTheme.getCardBg(isDark), Color.TRANSPARENT, dp(20)));
        panelContainer.setPadding(dp18, dp14, dp18, dp16);
        panelContainer.setClickable(true);

        LinearLayout panelRoot = new LinearLayout(getContext());
        panelRoot.setOrientation(LinearLayout.VERTICAL);
        panelRoot.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // --- 顶部栏 (Logo + Siyo(黑/白)X(蓝) 功能面板 + 右上角仅保留到期时间徽章) ---
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
        topLogo.setBackground(createCardBg(isDark ? Color.parseColor("#2A2A2E") : Color.WHITE, Color.TRANSPARENT, dp(8)));
        topLogo.setClipToOutline(true);
        topBar.addView(topLogo);

        LinearLayout titleContainer = new LinearLayout(getContext());
        titleContainer.setOrientation(LinearLayout.HORIZONTAL);
        titleContainer.setGravity(Gravity.CENTER_VERTICAL);
        titleContainer.setPadding(dp10, 0, 0, 0);
        titleContainer.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        titleContainer.addView(createSiyoXTitle(17f, isDark));

        TextView titleSub = new TextView(getContext());
        titleSub.setText(" 功能面板");
        titleSub.setTextSize(14.5f);
        titleSub.setTypeface(Typeface.DEFAULT_BOLD);
        titleSub.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        titleContainer.addView(titleSub);
        topBar.addView(titleContainer);

        // 右上角：到期时间徽章
        tvTopExpireBadge = new TextView(getContext());
        tvTopExpireBadge.setText("到期时间: " + VerifyManager.formatDate(verifyManager.getExpireTimestamp()));
        tvTopExpireBadge.setTextSize(11f);
        tvTopExpireBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvTopExpireBadge.setTextColor(SiyoXTheme.getAccentBlue());
        tvTopExpireBadge.setPadding(dp10, dp(4), dp10, dp(4));
        tvTopExpireBadge.setBackground(createCardBg(SiyoXTheme.getExpireBadgeBg(isDark), Color.TRANSPARENT, dp(8)));
        topBar.addView(tvTopExpireBadge);

        panelRoot.addView(topBar);

        panelRoot.addView(createDivider(isDark));

        // --- 主体部分: 左侧分类栏 + 右侧功能列表 ---
        LinearLayout mainContentRow = new LinearLayout(getContext());
        mainContentRow.setOrientation(LinearLayout.HORIZONTAL);
        mainContentRow.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        // 1. 左侧功能分类栏 (Width 118dp)
        LinearLayout leftSidebar = new LinearLayout(getContext());
        leftSidebar.setOrientation(LinearLayout.VERTICAL);
        leftSidebar.setLayoutParams(new LinearLayout.LayoutParams(dp(118), LayoutParams.MATCH_PARENT));
        leftSidebar.setBackground(createCardBg(SiyoXTheme.getSidebarBg(isDark), Color.TRANSPARENT, dp(14)));
        leftSidebar.setPadding(dp8, dp8, dp8, dp8);

        categoryListLayout = new LinearLayout(getContext());
        categoryListLayout.setOrientation(LinearLayout.VERTICAL);
        categoryListLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        String[] categories = new String[]{"资源管理", "辅助功能", "脚本列表", "个人中心", "关于软件"};
        categoryTabViews.clear();

        for (int i = 0; i < categories.length; i++) {
            final int index = i;
            TextView tabView = new TextView(getContext());
            tabView.setText(categories[i]);
            tabView.setTextSize(12.5f);
            tabView.setPadding(dp10, dp8, dp10, dp8);
            tabView.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            p.setMargins(0, dp(3), 0, dp(3));
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

    // 切换分类带有平滑过渡动画
    private void switchCategory(int categoryIndex) {
        boolean isDark = SiyoXTheme.isDarkMode(getContext());
        this.currentCategoryIndex = categoryIndex;

        for (int i = 0; i < categoryTabViews.size(); i++) {
            TextView tv = categoryTabViews.get(i);
            if (i == categoryIndex) {
                tv.setTypeface(Typeface.DEFAULT_BOLD);
                tv.setTextColor(SiyoXTheme.getAccentBlue());
                tv.setBackground(createCardBg(SiyoXTheme.getActiveTabBg(isDark), Color.TRANSPARENT, dp(10)));
            } else {
                tv.setTypeface(Typeface.DEFAULT);
                tv.setTextColor(SiyoXTheme.getTextSecondary(isDark));
                tv.setBackground(createCardBg(Color.TRANSPARENT, Color.TRANSPARENT, dp(10)));
            }
        }

        // 渲染右侧内容并执行平滑上升渐显动画
        featureListContent.setAlpha(0f);
        featureListContent.setTranslationY(dp(10));
        renderFeatureList(categoryIndex);
        featureListContent.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void renderFeatureList(int categoryIndex) {
        featureListContent.removeAllViews();
        boolean isDark = SiyoXTheme.isDarkMode(getContext());

        int dp16 = dp(16);
        int dp14 = dp(14);
        int dp12 = dp(12);
        int dp8 = dp(8);

        String catName = categoryTabViews.get(categoryIndex).getText().toString();
        TextView titleTv = new TextView(getContext());
        titleTv.setText(catName);
        titleTv.setTextSize(13f);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        titleTv.setTextColor(SiyoXTheme.getTextSecondary(isDark));
        titleTv.setPadding(0, 0, 0, dp(6));
        featureListContent.addView(titleTv);

        if (categoryIndex == 0) {
            // 1. 资源管理 (单行布局：标题、路径与复制按钮)
            String resPath = "/sdcard/Android/data/" + SiyoXConfig.TARGET_PACKAGE + "/SiyoX/Resources/";
            featureListContent.addView(createDirectoryCard("资源目录", resPath, "已复制资源目录路径", isDark));

        } else if (categoryIndex == 1) {
            // 2. 辅助功能 (Auxiliary Features)
            featureListContent.addView(createMiuiXFeatureCard("辅助功能模块 01", "核心辅助功能模块，可在源码中接入具体功能", false, isDark, new MiuiXSwitch.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(MiuiXSwitch switchView, boolean isChecked) {
                    Toast.makeText(getContext(), "功能 01: " + (isChecked ? "已启用" : "已停用"), Toast.LENGTH_SHORT).show();
                }
            }));

            featureListContent.addView(createMiuiXFeatureCard("辅助功能模块 02", "扩展辅助功能模块，可在源码中接入具体功能", false, isDark, new MiuiXSwitch.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(MiuiXSwitch switchView, boolean isChecked) {
                    Toast.makeText(getContext(), "功能 02: " + (isChecked ? "已启用" : "已停用"), Toast.LENGTH_SHORT).show();
                }
            }));

            featureListContent.addView(createMiuiXFeatureCard("辅助功能模块 03", "自适应视觉微调模块，支持独立开关控制", false, isDark, new MiuiXSwitch.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(MiuiXSwitch switchView, boolean isChecked) {
                    Toast.makeText(getContext(), "功能 03: " + (isChecked ? "已启用" : "已停用"), Toast.LENGTH_SHORT).show();
                }
            }));

        } else if (categoryIndex == 2) {
            // 3. 脚本列表 (单行布局：标题、路径与复制按钮)
            String scriptPath = "/sdcard/Android/data/" + SiyoXConfig.TARGET_PACKAGE + "/SiyoX/Script/";
            featureListContent.addView(createDirectoryCard("脚本目录", scriptPath, "已复制脚本目录路径", isDark));

        } else if (categoryIndex == 3) {
            // 4. 个人中心 (HWID，卡密，到期时间，退出登录)
            LinearLayout profileCard = createInnerCard(isDark);
            profileCard.setPadding(dp16, dp14, dp16, dp14);

            profileCard.addView(createInfoRowItem("HWID", verifyManager.getHWID(), isDark));
            profileCard.addView(createDivider(isDark));
            profileCard.addView(createInfoRowItem("授权卡密", appSettings.getCard().isEmpty() ? "未绑定" : appSettings.getCard(), isDark));
            profileCard.addView(createDivider(isDark));
            profileCard.addView(createInfoRowItem("到期时间", VerifyManager.formatDate(verifyManager.getExpireTimestamp()), isDark));

            featureListContent.addView(profileCard);

            // 退出登录按钮 (白色波纹点击特效)
            Button btnLogout = new Button(getContext());
            btnLogout.setText("退出登录");
            btnLogout.setTextSize(14f);
            btnLogout.setTypeface(Typeface.DEFAULT_BOLD);
            btnLogout.setTextColor(Color.parseColor("#FF3B30"));
            btnLogout.setBackground(createExitRippleDrawable(SiyoXTheme.getExitBtnBg(isDark), dp(12)));
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
            // 5. 关于软件 (客户端名称, 客户端作者, 软件名称, 软件版本, 软件作者, 当前作用域)
            LinearLayout aboutCard = createInnerCard(isDark);
            aboutCard.setPadding(dp16, dp14, dp16, dp14);

            aboutCard.addView(createInfoRowItem("客户端名称", SiyoXConfig.CLIENT_NAME, isDark));
            aboutCard.addView(createDivider(isDark));
            aboutCard.addView(createInfoRowItem("客户端作者", SiyoXConfig.CLIENT_AUTHOR, isDark));
            aboutCard.addView(createDivider(isDark));
            aboutCard.addView(createCustomInfoRow("软件名称", createSiyoXTitle(14f, isDark), isDark));
            aboutCard.addView(createDivider(isDark));
            aboutCard.addView(createInfoRowItem("软件版本", SiyoXConfig.VERSION_NAME, isDark));
            aboutCard.addView(createDivider(isDark));
            aboutCard.addView(createInfoRowItem("软件作者", SiyoXConfig.AUTHOR, isDark)); // @XiYueMax
            aboutCard.addView(createDivider(isDark));
            aboutCard.addView(createInfoRowItem("当前作用域", SiyoXConfig.TARGET_PACKAGE, isDark));

            featureListContent.addView(aboutCard);
        }
    }

    // 4. 目录的标题、目录路径和复制按钮在同一行里
    private View createDirectoryCard(final String title, final String path, final String toastMsg, boolean isDark) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardParams);
        card.setBackground(createCardBg(SiyoXTheme.getInnerCardBg(isDark), Color.TRANSPARENT, dp(14)));
        card.setPadding(dp(16), dp(12), dp(16), dp(12));

        // 1. 标题 (Fixed wrap)
        TextView tvLabel = new TextView(getContext());
        tvLabel.setText(title + ": ");
        tvLabel.setTextSize(13f);
        tvLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvLabel.setTextColor(SiyoXTheme.getTextSecondary(isDark));
        card.addView(tvLabel);

        // 2. 路径 (占满中间，单行显示，省略中间)
        TextView tvPath = new TextView(getContext());
        tvPath.setText(path);
        tvPath.setTextSize(12f);
        tvPath.setSingleLine(true);
        tvPath.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        tvPath.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        LinearLayout.LayoutParams pathParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        pathParams.setMargins(0, 0, dp(8), 0);
        tvPath.setLayoutParams(pathParams);
        card.addView(tvPath);

        // 3. 复制按钮 (图标 + "复制")
        LinearLayout btnCopy = new LinearLayout(getContext());
        btnCopy.setOrientation(LinearLayout.HORIZONTAL);
        btnCopy.setGravity(Gravity.CENTER_VERTICAL);
        btnCopy.setPadding(dp(8), dp(4), dp(8), dp(4));
        btnCopy.setBackground(createRippleDrawable(SiyoXTheme.getActiveTabBg(isDark), Color.parseColor("#0066CC"), dp(8)));
        btnCopy.setClickable(true);

        CopyIconView copyIcon = new CopyIconView(getContext());
        copyIcon.setIconColor(SiyoXTheme.getAccentBlue());
        btnCopy.addView(copyIcon);

        View spacerIcon = new View(getContext());
        spacerIcon.setLayoutParams(new LinearLayout.LayoutParams(dp(4), 1));
        btnCopy.addView(spacerIcon);

        TextView tvCopy = new TextView(getContext());
        tvCopy.setText("复制");
        tvCopy.setTextSize(11.5f);
        tvCopy.setTypeface(Typeface.DEFAULT_BOLD);
        tvCopy.setTextColor(SiyoXTheme.getAccentBlue());
        btnCopy.addView(tvCopy);

        btnCopy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    ClipData clip = ClipData.newPlainText(title, path);
                    cm.setPrimaryClip(clip);
                    Toast.makeText(getContext(), toastMsg, Toast.LENGTH_SHORT).show();
                }
            }
        });

        card.addView(btnCopy);
        return card;
    }

    private View createInfoRowItem(String label, String value, boolean isDark) {
        TextView tvVal = new TextView(getContext());
        tvVal.setText(value);
        tvVal.setTextSize(13f);
        tvVal.setTypeface(Typeface.DEFAULT_BOLD);
        tvVal.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        return createCustomInfoRow(label, tvVal, isDark);
    }

    private View createCustomInfoRow(String label, View rightView, boolean isDark) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        row.setPadding(0, dp(4), 0, dp(4));

        TextView tvLabel = new TextView(getContext());
        tvLabel.setText(label);
        tvLabel.setTextSize(13f);
        tvLabel.setTextColor(SiyoXTheme.getTextSecondary(isDark));
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tvLabel);

        row.addView(rightView);
        return row;
    }

    private View createMiuiXFeatureCard(String title, String desc, boolean initial, boolean isDark, MiuiXSwitch.OnCheckedChangeListener listener) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(cardParams);
        card.setBackground(createCardBg(SiyoXTheme.getInnerCardBg(isDark), Color.TRANSPARENT, dp(14)));
        card.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout textCol = new LinearLayout(getContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(title);
        tvTitle.setTextSize(14f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        textCol.addView(tvTitle);

        TextView tvDesc = new TextView(getContext());
        tvDesc.setText(desc);
        tvDesc.setTextSize(11f);
        tvDesc.setTextColor(SiyoXTheme.getTextSecondary(isDark));
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
    // 3. 悬浮球 (全屏幕自由拖拽移动，无蓝边，无黑边，正常显示Logo图标)
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
        floatingBall.setClipChildren(false);
        floatingBall.setClipToPadding(false);

        // 无蓝色边框，圆角白色背景，无黑边
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
        floatingBall.addView(logoImg);

        setupBallDragListener(floatingBall);
        addView(floatingBall);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupBallDragListener(final View ball) {
        ball.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int[] size = getRealScreenSize();
                int screenW = size[0];
                int screenH = size[1];

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

                        // 1. 即时同步布局坐标，彻底消除 GPU RenderNode 脏矩形裁剪滞后与边缘残缺
                        v.layout(newLeft, newTop, newLeft + v.getWidth(), newTop + v.getHeight());

                        // 2. 更新 LayoutParams 保存当前坐标
                        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
                        lp.leftMargin = newLeft;
                        lp.topMargin = newTop;
                        lp.gravity = Gravity.TOP | Gravity.START;
                        v.setLayoutParams(lp);

                        // 3. 触发父容器即时重绘
                        invalidate();
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

    // 点击悬浮球时，从悬浮球中心向外扩散半透明黑色波纹，随后弹出功能面板
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

        // 刷新右上角到期时间
        if (tvTopExpireBadge != null) {
            tvTopExpireBadge.setText("到期时间: " + VerifyManager.formatDate(verifyManager.getExpireTimestamp()));
        }

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
                final String key = fullCardInput.getText().toString().trim();
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
                                fullStatusTip.setText("HWID: " + verifyManager.getHWID() + " (点击复制)");

                                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

                                if (success) {
                                    if (cbRememberCard != null && cbRememberCard.isChecked()) {
                                        appSettings.setCard(key);
                                    } else {
                                        appSettings.setCard("");
                                    }
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

        // 自动登录
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
        if (tvTopExpireBadge != null) {
            tvTopExpireBadge.setText("到期时间: " + VerifyManager.formatDate(verifyManager.getExpireTimestamp()));
        }
    }

    // Siyo 黑/白 + X 蓝色标题组件 (自适应暗黑模式)
    public static View createSiyoXTitleView(Context context, float textSize) {
        boolean isDark = SiyoXTheme.isDarkMode(context);
        return createSiyoXTitleView(context, textSize, isDark);
    }

    public static View createSiyoXTitleView(Context context, float textSize, boolean isDark) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvSiyo = new TextView(context);
        tvSiyo.setText("Siyo");
        tvSiyo.setTextSize(textSize);
        tvSiyo.setTypeface(Typeface.DEFAULT_BOLD);
        tvSiyo.setTextColor(SiyoXTheme.getTextSiyo(isDark)); // Siyo 浅色黑 / 暗色白
        layout.addView(tvSiyo);

        TextView tvX = new TextView(context);
        tvX.setText("X");
        tvX.setTextSize(textSize);
        tvX.setTypeface(Typeface.DEFAULT_BOLD);
        tvX.setTextColor(SiyoXTheme.getAccentBlue()); // X 亮蓝
        layout.addView(tvX);

        return layout;
    }

    private View createSiyoXTitle(float textSize, boolean isDark) {
        return createSiyoXTitleView(getContext(), textSize, isDark);
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

    private TextView createSectionTitle(String title, boolean isDark) {
        TextView tv = new TextView(getContext());
        tv.setText(title);
        tv.setTextSize(13f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(SiyoXTheme.getTextSecondary(isDark));
        tv.setPadding(0, dp(10), 0, dp(4)); // 0 start padding, flush left
        return tv;
    }

    private LinearLayout createInnerCard(boolean isDark) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        card.setBackground(createCardBg(SiyoXTheme.getInnerCardBg(isDark), Color.TRANSPARENT, dp(14)));
        return card;
    }

    private View createDivider(boolean isDark) {
        View div = new View(getContext());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1));
        p.setMargins(0, dp(6), 0, dp(6));
        div.setLayoutParams(p);
        div.setBackgroundColor(SiyoXTheme.getDivider(isDark));
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
        GradientDrawable mask = createCardBg(Color.WHITE, Color.TRANSPARENT, radius);
        return new RippleDrawable(ColorStateList.valueOf(pressedColor), content, mask);
    }

    // 3. 退出按钮纯白柔和波纹特效
    private RippleDrawable createExitRippleDrawable(int normalColor, int radius) {
        GradientDrawable content = createCardBg(normalColor, Color.TRANSPARENT, radius);
        GradientDrawable mask = createCardBg(Color.WHITE, Color.TRANSPARENT, radius);
        return new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#40FFFFFF")), content, mask);
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                v,
                getContext().getResources().getDisplayMetrics()
        );
    }
}
