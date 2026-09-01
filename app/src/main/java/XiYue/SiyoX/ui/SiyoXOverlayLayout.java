

package XiYue.SiyoX.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.animation.AccelerateDecelerateInterpolator;
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
import android.view.Window;
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

import android.os.Build;
import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import XiYue.SiyoX.SiyoXConfig;
import XiYue.SiyoX.data.AppSettings;
import XiYue.SiyoX.data.ResourceInjector;
import XiYue.SiyoX.data.SiyoXDirManager;
import XiYue.SiyoX.data.SiyoXLogger;
import XiYue.SiyoX.data.VerifyManager;

@SuppressLint("ViewConstructor")
public class SiyoXOverlayLayout extends FrameLayout {

    private final Activity activity;
    private final AppSettings appSettings;
    private final VerifyManager verifyManager;

    private boolean isPanelOpen = false;
    private int currentResSubTab = 0; 

private FrameLayout fullScreenVerifyView;
    private FrameLayout floatingBall;
    private FrameLayout inGamePanelScrim;
    private FrameLayout updateModalScrim;
    private RippleWaveView rippleWaveView;
    private FrameLayout panelContainer;
    private FrameLayout cardWrapper;

private TextView fullNoticeTitle;
    private TextView fullNoticeContent;
    private EditText fullCardInput;
    private Button fullBtnVerify;
    private Button fullBtnExit;
    private SiyoXLoadingBar fullLoadingBar;
    private TextView fullStatusTip;
    private MiuiXCheckBox cbRememberCard;
    private MiuiXCheckBox cbAutoLogin;

private LinearLayout categoryListLayout;
    private LinearLayout featureListContent;
    private TextView tvTopExpireBadge;
    private int currentCategoryIndex = 0;
    private final List<TextView> categoryTabViews = new ArrayList<>();

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

SiyoXDirManager.initDirectories(activity.getApplicationContext());

        initUI();
        setupListeners();
        loadNotice();
        checkInitialState();
        checkAndShowUpdateDialog();
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
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(screenW, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(screenH, MeasureSpec.EXACTLY)
        );
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    private void initUI() {
        
        buildFullScreenVerifyWindow();

buildInGamePanel();

buildFloatingBall();
    }

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

LinearLayout leftColumn = new LinearLayout(getContext());
        leftColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.05f);
        leftParams.setMargins(0, 0, dp14, 0);
        leftColumn.setLayoutParams(leftParams);

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

        titleTextCol.addView(createSiyoXTitle(20f, isDark));

        TextView tvVersion = new TextView(getContext());
        tvVersion.setText("v" + SiyoXConfig.VERSION_CODE);
        tvVersion.setTextSize(11f);
        tvVersion.setTextColor(SiyoXTheme.getTextSecondary(isDark));
        titleTextCol.addView(tvVersion);

        topLeftHeader.addView(titleTextCol);
        leftColumn.addView(topLeftHeader);

        leftColumn.addView(createDivider(isDark));

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
        fullNoticeTitle.setText(SiyoXConfig.DEFAULT_NOTICE_TITLE);
        fullNoticeTitle.setTextSize(13f);
        fullNoticeTitle.setTypeface(Typeface.DEFAULT_BOLD);
        fullNoticeTitle.setTextColor(SiyoXTheme.getAccentBlue());
        noticeInner.addView(fullNoticeTitle);

        fullNoticeContent = new TextView(getContext());
        fullNoticeContent.setText(SiyoXConfig.DEFAULT_NOTICE_CONTENT);
        fullNoticeContent.setTextSize(11.5f);
        fullNoticeContent.setLineSpacing(dp(2), 1.15f);
        fullNoticeContent.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        fullNoticeContent.setPadding(0, dp(4), 0, 0);
        noticeInner.addView(fullNoticeContent);

        noticeScrollView.addView(noticeInner);
        noticeCard.addView(noticeScrollView);
        leftColumn.addView(noticeCard);

        mainHorizontalLayout.addView(leftColumn);

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

LinearLayout optionsRow = new LinearLayout(getContext());
        optionsRow.setOrientation(LinearLayout.HORIZONTAL);
        optionsRow.setGravity(Gravity.CENTER_VERTICAL);
        optionsRow.setPadding(0, dp10, 0, dp8);
        optionsRow.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

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

        fullLoadingBar = new SiyoXLoadingBar(getContext());
        fullLoadingBar.setColors(isDark);
        fullLoadingBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(4.5f));
        barParams.setMargins(0, 0, 0, dp(6));
        fullLoadingBar.setLayoutParams(barParams);
        cardKeyLayout.addView(fullLoadingBar);

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

LinearLayout bottomActions = new LinearLayout(getContext());
        bottomActions.setOrientation(LinearLayout.HORIZONTAL);
        bottomActions.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(44)));

        fullBtnExit = new Button(getContext());
        fullBtnExit.setText("退出游戏");
        fullBtnExit.setTextSize(14f);
        fullBtnExit.setTypeface(Typeface.DEFAULT_BOLD);
        fullBtnExit.setTextColor(Color.parseColor("#FF3B30"));
        fullBtnExit.setBackground(createExitRippleDrawable(SiyoXTheme.getExitBtnBg(isDark), dp(12)));
        styleCleanButton(fullBtnExit);
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

        fullBtnVerify = new Button(getContext());
        fullBtnVerify.setText("立即验证");
        fullBtnVerify.setTextSize(15f);
        fullBtnVerify.setTypeface(Typeface.DEFAULT_BOLD);
        fullBtnVerify.setTextColor(Color.WHITE); 
        fullBtnVerify.setBackground(createRippleDrawable(Color.parseColor("#0A84FF"), Color.parseColor("#0066CC"), dp(12))); 
        styleCleanButton(fullBtnVerify);
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

LinearLayout mainContentRow = new LinearLayout(getContext());
        mainContentRow.setOrientation(LinearLayout.HORIZONTAL);
        mainContentRow.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

LinearLayout leftSidebar = new LinearLayout(getContext());
        leftSidebar.setOrientation(LinearLayout.VERTICAL);
        leftSidebar.setLayoutParams(new LinearLayout.LayoutParams(dp(118), LayoutParams.MATCH_PARENT));
        leftSidebar.setBackground(createCardBg(SiyoXTheme.getSidebarBg(isDark), Color.TRANSPARENT, dp(14)));
        leftSidebar.setPadding(dp8, dp8, dp8, dp8);

        categoryListLayout = new LinearLayout(getContext());
        categoryListLayout.setOrientation(LinearLayout.VERTICAL);
        categoryListLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        String[] categories = new String[]{"资源列表", "辅助功能", "个人中心", "关于软件"};
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

        if (categoryIndex == 0) {
            
            renderResourceList(isDark);

        } else if (categoryIndex == 1) {
            
            renderAuxiliaryFeatures(isDark);

        } else if (categoryIndex == 2) {
            
            LinearLayout profileCard = createInnerCard(isDark);
            profileCard.setPadding(dp16, dp14, dp16, dp14);

            profileCard.addView(createInfoRowItem("HWID", verifyManager.getHWID(), isDark));
            profileCard.addView(createDivider(isDark));
            if (SiyoXConfig.CURRENT_VERIFY_TYPE == SiyoXConfig.VerifyType.NONE) {
                profileCard.addView(createInfoRowItem("验证模式", "已关闭网络验证", isDark));
                profileCard.addView(createDivider(isDark));
                profileCard.addView(createInfoRowItem("授权状态", "永久", isDark));
                profileCard.addView(createDivider(isDark));
                profileCard.addView(createInfoRowItem("到期时间", "永久", isDark));
            } else {
                profileCard.addView(createInfoRowItem("授权卡密", appSettings.getCard().isEmpty() ? "未绑定" : appSettings.getCard(), isDark));
                profileCard.addView(createDivider(isDark));
                profileCard.addView(createInfoRowItem("到期时间", VerifyManager.formatDate(verifyManager.getExpireTimestamp()), isDark));
            }

            featureListContent.addView(profileCard);

            if (SiyoXConfig.CURRENT_VERIFY_TYPE != SiyoXConfig.VerifyType.NONE) {
                Button btnLogout = new Button(getContext());
                btnLogout.setText("退出登录");
                btnLogout.setTextSize(14f);
                btnLogout.setTypeface(Typeface.DEFAULT_BOLD);
                btnLogout.setTextColor(Color.parseColor("#FF3B30"));
                btnLogout.setBackground(createExitRippleDrawable(SiyoXTheme.getExitBtnBg(isDark), dp(12)));
                styleCleanButton(btnLogout);
                LinearLayout.LayoutParams lpLogout = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(44));
                lpLogout.setMargins(0, dp14, 0, 0);
                btnLogout.setLayoutParams(lpLogout);

                btnLogout.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showConfirmDialog("退出登录", "确定要退出当前登录并清除授权卡密吗？", "退出", true, new Runnable() {
                            @Override
                            public void run() {
                                verifyManager.logout();
                                closePanel();
                                floatingBall.setVisibility(View.GONE);
                                fullScreenVerifyView.setVisibility(View.VISIBLE);
                                fullCardInput.setText("");
                                Toast.makeText(getContext(), "已退出登录", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                featureListContent.addView(btnLogout);
            }

        } else if (categoryIndex == 3) {
            
            LinearLayout aboutCard = createInnerCard(isDark);
            aboutCard.setPadding(dp16, dp14, dp16, dp14);

            aboutCard.addView(createInfoRowItem("客户端名称", SiyoXConfig.CLIENT_NAME, isDark));
            aboutCard.addView(createDivider(isDark));
            aboutCard.addView(createInfoRowItem("客户端作者", SiyoXConfig.CLIENT_AUTHOR, isDark));
            aboutCard.addView(createDivider(isDark));
            aboutCard.addView(createCustomInfoRow("软件名称", createSiyoXTitle(14f, isDark), isDark));
            aboutCard.addView(createDivider(isDark));
            aboutCard.addView(createInfoRowItem("内部版本", String.valueOf(SiyoXConfig.VERSION_CODE), isDark));
            aboutCard.addView(createDivider(isDark));
            aboutCard.addView(createInfoRowItem("软件作者", SiyoXConfig.AUTHOR, isDark)); 
            aboutCard.addView(createDivider(isDark));
            aboutCard.addView(createInfoRowItem("当前作用域", SiyoXConfig.TARGET_PACKAGE, isDark));

            featureListContent.addView(aboutCard);
        }
    }

private void renderResourceList(final boolean isDark) {
        int dp16 = dp(16);
        int dp14 = dp(14);
        int dp12 = dp(12);
        int dp10 = dp(10);
        int dp8 = dp(8);
        int dp6 = dp(6);

LinearLayout subTabRow = new LinearLayout(getContext());
        subTabRow.setOrientation(LinearLayout.HORIZONTAL);
        subTabRow.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        subTabRow.setPadding(0, 0, 0, dp10);

        final TextView tabDefault = new TextView(getContext());
        tabDefault.setText("默认资源");
        tabDefault.setTextSize(12.5f);
        tabDefault.setPadding(dp12, dp6, dp12, dp6);
        tabDefault.setGravity(Gravity.CENTER);

        final TextView tabCustom = new TextView(getContext());
        tabCustom.setText("自定义资源");
        tabCustom.setTextSize(12.5f);
        tabCustom.setPadding(dp12, dp6, dp12, dp6);
        tabCustom.setGravity(Gravity.CENTER);

        if (currentResSubTab == 0) {
            tabDefault.setTypeface(Typeface.DEFAULT_BOLD);
            tabDefault.setTextColor(SiyoXTheme.getAccentBlue());
            tabDefault.setBackground(createCardBg(SiyoXTheme.getActiveTabBg(isDark), Color.TRANSPARENT, dp(8)));

            tabCustom.setTypeface(Typeface.DEFAULT);
            tabCustom.setTextColor(SiyoXTheme.getTextSecondary(isDark));
            tabCustom.setBackground(createCardBg(Color.TRANSPARENT, Color.TRANSPARENT, dp(8)));
        } else {
            tabCustom.setTypeface(Typeface.DEFAULT_BOLD);
            tabCustom.setTextColor(SiyoXTheme.getAccentBlue());
            tabCustom.setBackground(createCardBg(SiyoXTheme.getActiveTabBg(isDark), Color.TRANSPARENT, dp(8)));

            tabDefault.setTypeface(Typeface.DEFAULT);
            tabDefault.setTextColor(SiyoXTheme.getTextSecondary(isDark));
            tabDefault.setBackground(createCardBg(Color.TRANSPARENT, Color.TRANSPARENT, dp(8)));
        }

        tabDefault.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentResSubTab != 0) {
                    currentResSubTab = 0;
                    featureListContent.removeAllViews();
                    renderResourceList(isDark);
                }
            }
        });

        tabCustom.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentResSubTab != 1) {
                    currentResSubTab = 1;
                    featureListContent.removeAllViews();
                    renderResourceList(isDark);
                }
            }
        });

        subTabRow.addView(tabDefault);
        View spacer = new View(getContext());
        spacer.setLayoutParams(new LinearLayout.LayoutParams(dp8, 1));
        subTabRow.addView(spacer);
        subTabRow.addView(tabCustom);

        featureListContent.addView(subTabRow);

        if (currentResSubTab == 0) {
            
            if (SiyoXConfig.DEFAULT_RESOURCES != null && SiyoXConfig.DEFAULT_RESOURCES.length > 0) {
                for (final SiyoXConfig.DefaultResource res : SiyoXConfig.DEFAULT_RESOURCES) {
                    featureListContent.addView(createDefaultResourceCard(res, isDark));
                }
            } else {
                TextView tvEmpty = new TextView(getContext());
                tvEmpty.setText("暂无预置默认资源");
                tvEmpty.setTextSize(12.5f);
                tvEmpty.setTextColor(SiyoXTheme.getTextSecondary(isDark));
                tvEmpty.setPadding(0, dp14, 0, 0);
                featureListContent.addView(tvEmpty);
            }
        } else {
            
            String resPath = "/sdcard/Android/data/" + SiyoXConfig.TARGET_PACKAGE + "/SiyoX/Resources/";
            featureListContent.addView(createDirectoryCard("资源存放目录", resPath, "已复制资源目录路径", isDark));

            File resDir = new File(Environment.getExternalStorageDirectory(), "Android/data/" + SiyoXConfig.TARGET_PACKAGE + "/SiyoX/Resources");
            File[] zipFiles = null;
            try {
                if (resDir.exists() && resDir.isDirectory()) {
                    zipFiles = resDir.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.toLowerCase().endsWith(".zip");
                        }
                    });
                }
            } catch (Throwable ignored) {}

            if (zipFiles != null && zipFiles.length > 0) {
                for (final File zip : zipFiles) {
                    featureListContent.addView(createCustomResourceCard(zip, isDark));
                }
            } else {
                LinearLayout emptyCard = createInnerCard(isDark);
                emptyCard.setPadding(dp16, dp16, dp16, dp16);
                emptyCard.setGravity(Gravity.CENTER_HORIZONTAL);

                TextView tvTip = new TextView(getContext());
                tvTip.setText("暂无自定义材质包\n请将您的 .zip 材质包复制到上方目录中");
                tvTip.setTextSize(12.5f);
                tvTip.setGravity(Gravity.CENTER);
                tvTip.setTextColor(SiyoXTheme.getTextSecondary(isDark));
                tvTip.setLineSpacing(dp(3), 1.15f);
                emptyCard.addView(tvTip);

                Button btnRefresh = new Button(getContext());
                btnRefresh.setText("刷新列表");
                btnRefresh.setTextSize(13f);
                btnRefresh.setTypeface(Typeface.DEFAULT_BOLD);
                btnRefresh.setTextColor(Color.WHITE);
                btnRefresh.setBackground(createRippleDrawable(Color.parseColor("#0A84FF"), Color.parseColor("#0066CC"), dp(10)));
                styleCleanButton(btnRefresh);
                LinearLayout.LayoutParams lpRef = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp(38));
                lpRef.setMargins(0, dp12, 0, 0);
                btnRefresh.setLayoutParams(lpRef);
                btnRefresh.setPadding(dp16, 0, dp16, 0);
                btnRefresh.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        featureListContent.removeAllViews();
                        renderResourceList(isDark);
                        Toast.makeText(getContext(), "已刷新自定义资源列表", Toast.LENGTH_SHORT).show();
                    }
                });
                emptyCard.addView(btnRefresh);

                featureListContent.addView(emptyCard);
            }
        }
    }

    private View createDefaultResourceCard(final SiyoXConfig.DefaultResource res, final boolean isDark) {
        int dp14 = dp(14);
        int dp12 = dp(12);
        int dp10 = dp(10);
        int dp8 = dp(8);
        int dp6 = dp(6);

        LinearLayout card = createInnerCard(isDark);
        card.setPadding(dp14, dp12, dp14, dp12);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp10);
        card.setLayoutParams(cardParams);

LinearLayout topRow = new LinearLayout(getContext());
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(res.name);
        tvTitle.setTextSize(13.5f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        topRow.addView(tvTitle);

        final File localFile = new File(ResourceInjector.getResFilesDir(getContext()), res.getFileName());
        final boolean isDownloaded = localFile.exists() && localFile.length() > 0;

        final TextView tvStatus = new TextView(getContext());
        tvStatus.setText(isDownloaded ? "已下载" : "未下载");
        tvStatus.setTextSize(11f);
        tvStatus.setTextColor(isDownloaded ? SiyoXTheme.getAccentBlue() : SiyoXTheme.getTextSecondary(isDark));
        topRow.addView(tvStatus);
        card.addView(topRow);

if (res.description != null && !res.description.isEmpty()) {
            TextView tvDesc = new TextView(getContext());
            tvDesc.setText(res.description);
            tvDesc.setTextSize(11f);
            tvDesc.setTextColor(SiyoXTheme.getTextSecondary(isDark));
            tvDesc.setPadding(0, dp(3), 0, 0);
            card.addView(tvDesc);
        }

        final ProgressBar pbDownload = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        pbDownload.setMax(100);
        pbDownload.setProgress(0);
        pbDownload.setVisibility(View.GONE);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(4));
        pbParams.setMargins(0, dp6, 0, dp6);
        pbDownload.setLayoutParams(pbParams);

        GradientDrawable bgProg = new GradientDrawable();
        bgProg.setColor(Color.parseColor("#200A84FF"));
        bgProg.setCornerRadius(dp(2));

        GradientDrawable fgProg = new GradientDrawable();
        fgProg.setColor(Color.parseColor("#0A84FF"));
        fgProg.setCornerRadius(dp(2));
        ClipDrawable clipFg = new ClipDrawable(fgProg, Gravity.START, ClipDrawable.HORIZONTAL);

        Drawable[] layers = new Drawable[]{bgProg, clipFg};
        LayerDrawable progressDrawable = new LayerDrawable(layers);
        progressDrawable.setId(0, android.R.id.background);
        progressDrawable.setId(1, android.R.id.progress);
        pbDownload.setProgressDrawable(progressDrawable);

        card.addView(pbDownload);

        LinearLayout actionRow = new LinearLayout(getContext());
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionRowParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        actionRowParams.setMargins(0, dp8, 0, 0);
        actionRow.setLayoutParams(actionRowParams);

        LinearLayout btnDelete = new LinearLayout(getContext());
        btnDelete.setGravity(Gravity.CENTER);
        btnDelete.setBackground(createRippleDrawable(SiyoXTheme.getExitBtnBg(isDark), Color.parseColor("#30FF3B30"), dp(10)));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(38), dp(38));
        deleteParams.setMargins(0, 0, dp8, 0);
        btnDelete.setLayoutParams(deleteParams);
        btnDelete.setClickable(true);

        TrashIconView trashIcon = new TrashIconView(getContext());
        trashIcon.setIconColor(Color.parseColor("#FF3B30"));
        btnDelete.addView(trashIcon);

        final Button btnAction = new Button(getContext());
        String initialActionText = "下载";
        if (isDownloaded) {
            boolean isCurrent = res.getFileName().equals(appSettings.getInjectedPack());
            initialActionText = isCurrent ? "已注入当前资源包" : "注入资源";
        }
        btnAction.setText(initialActionText);
        btnAction.setTextSize(13f);
        btnAction.setTypeface(Typeface.DEFAULT_BOLD);
        btnAction.setTextColor(Color.WHITE);
        btnAction.setBackground(createRippleDrawable(Color.parseColor("#0A84FF"), Color.parseColor("#0066CC"), dp(10)));
        styleCleanButton(btnAction);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, dp(38), 1f);
        btnAction.setLayoutParams(btnParams);

        final ResourceInjector.DownloadTask[] downloadTaskHolder = new ResourceInjector.DownloadTask[1];
        final boolean[] isDownloading = new boolean[]{false};
        final boolean[] isPaused = new boolean[]{false};

        btnDelete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfirmDialog("删除资源", "确定要删除已下载的「" + res.name + "」资源包文件吗？", "删除", true, new Runnable() {
                    @Override
                    public void run() {
                        if (downloadTaskHolder[0] != null) {
                            downloadTaskHolder[0].cancel();
                            downloadTaskHolder[0] = null;
                        }
                        isDownloading[0] = false;
                        isPaused[0] = false;
                        ResourceInjector.deleteResource(getContext(), res.getFileName());
                        btnAction.setEnabled(true);
                        btnAction.setText("下载");
                        pbDownload.setVisibility(View.GONE);
                        pbDownload.setProgress(0);
                        tvStatus.setText("未下载");
                        tvStatus.setTextColor(SiyoXTheme.getTextSecondary(isDark));
                        Toast.makeText(getContext(), "已删除资源包文件", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnAction.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (localFile.exists() && localFile.length() > 0 && !isDownloading[0] && !isPaused[0]) {
                    if (SiyoXConfig.ENABLE_RESOURCE_MD5_VERIFY && res.md5 != null && !res.md5.trim().isEmpty()) {
                        String localMd5 = ResourceInjector.computeFileMd5(localFile);
                        if (localMd5 == null || !localMd5.equalsIgnoreCase(res.md5.trim())) {
                            localFile.delete();
                            btnAction.setText("下载");
                            tvStatus.setText("MD5校验不匹配");
                            tvStatus.setTextColor(Color.parseColor("#FF3B30"));
                            Toast.makeText(getContext(), "本地资源包 MD5 校验不匹配，已自动清除损坏文件，请重新下载！", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    btnAction.setEnabled(false);
                    tvStatus.setText("正在注入...");
                    tvStatus.setTextColor(SiyoXTheme.getAccentBlue());
                    Toast.makeText(getContext(), "开始注入材质资源...", Toast.LENGTH_SHORT).show();

                    ResourceInjector.injectZip(getContext(), localFile, new ResourceInjector.InjectCallback() {
                        @Override
                        public void onProgress(String message) {
                            tvStatus.setText(message);
                        }

                        @Override
                        public void onSuccess(String message) {
                            appSettings.setInjectedPack(res.getFileName());
                            btnAction.setEnabled(true);
                            featureListContent.removeAllViews();
                            renderResourceList(isDark);
                            showCustomConfirmDialog("注入成功", "资源包注入成功，需重启游戏生效，是否重启？", "稍后重启", "立即重启", false, new Runnable() {
                                @Override
                                public void run() {
                                    if (activity != null) {
                                        activity.finishAffinity();
                                    }
                                    android.os.Process.killProcess(android.os.Process.myPid());
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            btnAction.setEnabled(true);
                            tvStatus.setText("注入失败");
                            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                        }
                    });

                } else if (isDownloading[0]) {
                    if (downloadTaskHolder[0] != null) {
                        downloadTaskHolder[0].pause();
                    }
                    isDownloading[0] = false;
                    isPaused[0] = true;
                    btnAction.setText("继续下载");
                    tvStatus.setText("已暂停");

                } else if (isPaused[0]) {
                    isPaused[0] = false;
                    isDownloading[0] = true;
                    btnAction.setText("暂停下载");
                    tvStatus.setText("继续下载中...");
                    tvStatus.setTextColor(SiyoXTheme.getAccentBlue());
                    pbDownload.setVisibility(View.VISIBLE);

                    startDownload(res, localFile, isDark, pbDownload, tvStatus, btnAction, downloadTaskHolder, isDownloading, isPaused);

                } else {
                    showConfirmDialog("下载资源", "确定要下载「" + res.name + "」资源包吗？", "下载", false, new Runnable() {
                        @Override
                        public void run() {
                            isDownloading[0] = true;
                            isPaused[0] = false;
                            btnAction.setText("暂停下载");
                            pbDownload.setVisibility(View.VISIBLE);
                            tvStatus.setText("连接中...");
                            tvStatus.setTextColor(SiyoXTheme.getAccentBlue());

                            startDownload(res, localFile, isDark, pbDownload, tvStatus, btnAction, downloadTaskHolder, isDownloading, isPaused);
                        }
                    });
                }
            }
        });

        actionRow.addView(btnDelete);
        actionRow.addView(btnAction);
        card.addView(actionRow);
        return card;
    }

    private void startDownload(final SiyoXConfig.DefaultResource res, final File localFile, final boolean isDark,
                               final ProgressBar pbDownload, final TextView tvStatus, final Button btnAction,
                               final ResourceInjector.DownloadTask[] taskHolder,
                               final boolean[] isDownloading, final boolean[] isPaused) {
        taskHolder[0] = ResourceInjector.downloadResource(getContext(), res.url, res.getFileName(), res.md5, new ResourceInjector.DownloadCallback() {
            @Override
            public void onProgress(int percent, long currentBytes, long totalBytes) {
                if (percent >= 0) {
                    pbDownload.setProgress(percent);
                    tvStatus.setText("下载中 " + percent + "%");
                } else {
                    tvStatus.setText("下载中...");
                }
            }

            @Override
            public void onPaused() {
                isDownloading[0] = false;
                isPaused[0] = true;
                btnAction.setText("继续下载");
                tvStatus.setText("已暂停");
            }

            @Override
            public void onSuccess(File downloadedFile) {
                isDownloading[0] = false;
                isPaused[0] = false;
                taskHolder[0] = null;
                btnAction.setEnabled(true);
                boolean isCurrent = res.getFileName().equals(appSettings.getInjectedPack());
                btnAction.setText(isCurrent ? "已注入当前资源包" : "注入资源");
                pbDownload.setVisibility(View.GONE);
                tvStatus.setText("已下载");
                Toast.makeText(getContext(), "下载完成，点击“注入资源”即可生效！", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                isDownloading[0] = false;
                isPaused[0] = false;
                taskHolder[0] = null;
                btnAction.setEnabled(true);
                btnAction.setText("重试下载");
                pbDownload.setVisibility(View.GONE);
                tvStatus.setText("下载失败");
                Toast.makeText(getContext(), "下载失败: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private View createCustomResourceCard(final File zipFile, final boolean isDark) {
        int dp14 = dp(14);
        int dp12 = dp(12);
        int dp10 = dp(10);
        int dp8 = dp(8);

        LinearLayout card = createInnerCard(isDark);
        card.setPadding(dp14, dp12, dp14, dp12);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp10);
        card.setLayoutParams(cardParams);

        LinearLayout topRow = new LinearLayout(getContext());
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvName = new TextView(getContext());
        tvName.setText(zipFile.getName());
        tvName.setTextSize(13f);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);
        tvName.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        tvName.setSingleLine(true);
        tvName.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        topRow.addView(tvName);

        TextView tvSize = new TextView(getContext());
        tvSize.setText(formatFileSize(zipFile.length()));
        tvSize.setTextSize(11f);
        tvSize.setTextColor(SiyoXTheme.getTextSecondary(isDark));
        tvSize.setPadding(dp8, 0, 0, 0);
        topRow.addView(tvSize);

        card.addView(topRow);

        LinearLayout actionRow = new LinearLayout(getContext());
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionRowParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        actionRowParams.setMargins(0, dp8, 0, 0);
        actionRow.setLayoutParams(actionRowParams);

        LinearLayout btnDelete = new LinearLayout(getContext());
        btnDelete.setGravity(Gravity.CENTER);
        btnDelete.setBackground(createRippleDrawable(SiyoXTheme.getExitBtnBg(isDark), Color.parseColor("#30FF3B30"), dp(10)));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(38), dp(38));
        deleteParams.setMargins(0, 0, dp8, 0);
        btnDelete.setLayoutParams(deleteParams);
        btnDelete.setClickable(true);

        TrashIconView trashIcon = new TrashIconView(getContext());
        trashIcon.setIconColor(Color.parseColor("#FF3B30"));
        btnDelete.addView(trashIcon);

        btnDelete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfirmDialog("删除资源", "确定要删除自定义材质包「" + zipFile.getName() + "」吗？", "删除", true, new Runnable() {
                    @Override
                    public void run() {
                        if (zipFile.exists()) {
                            zipFile.delete();
                        }
                        featureListContent.removeAllViews();
                        renderResourceList(isDark);
                        Toast.makeText(getContext(), "已删除自定义材质包", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        final Button btnInject = new Button(getContext());
        boolean isCurrent = zipFile.getName().equals(appSettings.getInjectedPack());
        btnInject.setText(isCurrent ? "已注入当前资源包" : "注入资源");
        btnInject.setTextSize(13f);
        btnInject.setTypeface(Typeface.DEFAULT_BOLD);
        btnInject.setTextColor(Color.WHITE);
        btnInject.setBackground(createRippleDrawable(Color.parseColor("#0A84FF"), Color.parseColor("#0066CC"), dp(10)));
        styleCleanButton(btnInject);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, dp(38), 1f);
        btnInject.setLayoutParams(btnParams);

        btnInject.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                btnInject.setEnabled(false);
                Toast.makeText(getContext(), "开始注入自定义材质...", Toast.LENGTH_SHORT).show();

                ResourceInjector.injectZip(getContext(), zipFile, new ResourceInjector.InjectCallback() {
                    @Override
                    public void onProgress(String message) {
                    }

                    @Override
                    public void onSuccess(String message) {
                        appSettings.setInjectedPack(zipFile.getName());
                        btnInject.setEnabled(true);
                        featureListContent.removeAllViews();
                        renderResourceList(isDark);
                        showCustomConfirmDialog("注入成功", "资源包注入成功，需重启游戏生效，是否重启？", "稍后重启", "立即重启", false, new Runnable() {
                            @Override
                            public void run() {
                                if (activity != null) {
                                    activity.finishAffinity();
                                }
                                android.os.Process.killProcess(android.os.Process.myPid());
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        btnInject.setEnabled(true);
                        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        actionRow.addView(btnDelete);
        actionRow.addView(btnInject);
        card.addView(actionRow);
        return card;
    }

    private void renderAuxiliaryFeatures(final boolean isDark) {
        int dp14 = dp(14);
        int dp12 = dp(12);
        int dp10 = dp(10);
        int dp8 = dp(8);
        int dp16 = dp(16);

        TextView titleResManage = createSectionTitle("资源管理", isDark);
        featureListContent.addView(titleResManage);

        LinearLayout clearCard = createInnerCard(isDark);
        clearCard.setOrientation(LinearLayout.HORIZONTAL);
        clearCard.setGravity(Gravity.CENTER_VERTICAL);
        clearCard.setPadding(dp14, dp10, dp12, dp10);
        LinearLayout.LayoutParams ccParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        ccParams.setMargins(0, 0, 0, dp10);
        clearCard.setLayoutParams(ccParams);

        LinearLayout textCol = new LinearLayout(getContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tcParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        tcParams.setMargins(0, 0, dp10, 0);
        textCol.setLayoutParams(tcParams);

        TextView tvClearTitle = new TextView(getContext());
        tvClearTitle.setText("恢复游戏默认材质");
        tvClearTitle.setTextSize(13.5f);
        tvClearTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvClearTitle.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        textCol.addView(tvClearTitle);

        TextView tvClearDesc = new TextView(getContext());
        tvClearDesc.setText("删除已注入的材质，恢复为游戏的默认材质。");
        tvClearDesc.setTextSize(11f);
        tvClearDesc.setTextColor(SiyoXTheme.getTextSecondary(isDark));
        tvClearDesc.setPadding(0, dp(2), 0, 0);
        textCol.addView(tvClearDesc);

        clearCard.addView(textCol);

        Button btnClear = new Button(getContext());
        btnClear.setText("恢复");
        btnClear.setTextSize(12.5f);
        btnClear.setTypeface(Typeface.DEFAULT_BOLD);
        btnClear.setTextColor(Color.WHITE);
        btnClear.setBackground(createRippleDrawable(Color.parseColor("#FF3B30"), Color.parseColor("#D70015"), dp(8)));
        styleCleanButton(btnClear);
        LinearLayout.LayoutParams btnClearParams = new LinearLayout.LayoutParams(dp(64), dp(34));
        btnClear.setLayoutParams(btnClearParams);

        btnClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfirmDialog("恢复默认材质", "确定要删除已注入的材质，恢复为游戏的默认材质吗？", "恢复", true, new Runnable() {
                    @Override
                    public void run() {
                        boolean success = ResourceInjector.restoreBackup(getContext());
                        if (success) {
                            appSettings.setInjectedPack("");
                            featureListContent.removeAllViews();
                            renderAuxiliaryFeatures(isDark);
                            Toast.makeText(getContext(), "已成功恢复游戏默认材质！", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "目标资源目录已是初始状态或无备份", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        clearCard.addView(btnClear);
        featureListContent.addView(clearCard);

TextView titleModule = createSectionTitle("模块功能", isDark);
        featureListContent.addView(titleModule);

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
    }

    private static String formatFileSize(long length) {
        if (length < 1024) {
            return length + " B";
        } else if (length < 1024 * 1024) {
            return String.format(java.util.Locale.getDefault(), "%.1f KB", length / 1024.0);
        } else {
            return String.format(java.util.Locale.getDefault(), "%.1f MB", length / (1024.0 * 1024.0));
        }
    }

private View createDirectoryCard(final String title, final String path, final String toastMsg, boolean isDark) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardParams);
        card.setBackground(createCardBg(SiyoXTheme.getInnerCardBg(isDark), Color.TRANSPARENT, dp(14)));
        card.setPadding(dp(16), dp(12), dp(16), dp(12));

TextView tvLabel = new TextView(getContext());
        tvLabel.setText(title + ": ");
        tvLabel.setTextSize(13f);
        tvLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvLabel.setTextColor(SiyoXTheme.getTextSecondary(isDark));
        card.addView(tvLabel);

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

        MiuiXSwitch miuixSwitch = new MiuiXSwitch(getContext());
        miuixSwitch.setChecked(initial, false);
        miuixSwitch.setOnCheckedChangeListener(listener);
        card.addView(miuixSwitch);

        return card;
    }

private void buildFloatingBall() {
        int ballSize = dp(42);
        floatingBall = new FrameLayout(getContext());
        LayoutParams ballParams = new LayoutParams(ballSize, ballSize);
        ballParams.gravity = Gravity.TOP | Gravity.START;
        ballParams.leftMargin = dp(20);
        ballParams.topMargin = dp(80);
        floatingBall.setLayoutParams(ballParams);
        floatingBall.setVisibility(View.GONE);
        floatingBall.setClipChildren(false);
        floatingBall.setClipToPadding(false);

GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#FFFFFF"));
        bg.setCornerRadius(dp(12));
        floatingBall.setBackground(bg);

        ImageView logoImg = new ImageView(getContext());
        int pad = dp(5);
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
                int screenW = getRealScreenSize()[0];
                int screenH = getRealScreenSize()[1];
                int parentW = getWidth() > 0 ? getWidth() : screenW;
                int parentH = getHeight() > 0 ? getHeight() : screenH;
                int bw = v.getWidth() > 0 ? v.getWidth() : dp(42);
                int bh = v.getHeight() > 0 ? v.getHeight() : dp(42);

int safeMarginX = dp(14);
                int safeMarginY = dp(12);
                int minX = safeMarginX;
                int maxX = Math.max(minX, parentW - bw - safeMarginX);
                int minY = safeMarginY;
                int maxY = Math.max(minY, parentH - bh - safeMarginY);

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        FrameLayout.LayoutParams curLp = (FrameLayout.LayoutParams) v.getLayoutParams();
                        dX = curLp.leftMargin - event.getRawX();
                        dY = curLp.topMargin - event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int newLeft = (int) Math.max(minX, Math.min(event.getRawX() + dX, maxX));
                        int newTop = (int) Math.max(minY, Math.min(event.getRawY() + dY, maxY));

                        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
                        if (lp.leftMargin != newLeft || lp.topMargin != newTop) {
                            lp.leftMargin = newLeft;
                            lp.topMargin = newTop;
                            lp.gravity = Gravity.TOP | Gravity.START;
                            v.setLayoutParams(lp);
                            v.layout(newLeft, newTop, newLeft + bw, newTop + bh);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        float diffX = Math.abs(event.getRawX() - downRawX);
                        float diffY = Math.abs(event.getRawY() - downRawY);
                        if (diffX < touchSlop && diffY < touchSlop) {
                            FrameLayout.LayoutParams curPos = (FrameLayout.LayoutParams) v.getLayoutParams();
                            openPanelWithRipple(curPos.leftMargin + bw / 2f, curPos.topMargin + bh / 2f);
                        }
                        return true;
                }
                return false;
            }
        });
    }

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

if (tvTopExpireBadge != null) {
            tvTopExpireBadge.setText("到期时间: " + VerifyManager.formatDate(verifyManager.getExpireTimestamp()));
        }

rippleWaveView.startExpandAnimation(originX, originY);

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
        checkAndShowUpdateDialog();
    }

    private void checkAndShowUpdateDialog() {
        verifyManager.checkSoftwareUpdate(new VerifyManager.UpdateCallback() {
            @Override
            public void onUpdateResult(final boolean hasUpdate, final VerifyManager.SoftwareUpdate update) {
                if (hasUpdate && update != null) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            showUpdateDialog(update);
                        }
                    });
                }
            }
        });
    }

    private void showUpdateDialog(final VerifyManager.SoftwareUpdate update) {
        if (update == null || !update.hasUpdate) return;
        if (VerifyManager.isUpdateDismissed() && !update.isForce) return;
        try {
            if (updateModalScrim != null) {
                removeView(updateModalScrim);
                updateModalScrim = null;
            }

            boolean isDark = SiyoXTheme.isDarkMode(getContext());
            int dp16 = dp(16);
            int dp14 = dp(14);
            int dp12 = dp(12);
            int dp10 = dp(10);
            int dp8 = dp(8);

            updateModalScrim = new FrameLayout(getContext());
            updateModalScrim.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            updateModalScrim.setBackgroundColor(Color.parseColor("#99000000"));
            updateModalScrim.setClickable(true);
            updateModalScrim.setFocusable(true);

            if (!update.isForce) {
                updateModalScrim.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismissUpdateModal();
                    }
                });
            }

            LinearLayout card = new LinearLayout(getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp16, dp16, dp16, dp16);
            card.setBackground(createCardBg(SiyoXTheme.getCardBg(isDark), Color.TRANSPARENT, dp(18)));
            card.setClickable(true);

            int maxW = Math.min(getRealScreenSize()[0] - dp(48), dp(340));
            FrameLayout.LayoutParams cParams = new FrameLayout.LayoutParams(maxW, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
            card.setLayoutParams(cParams);

            TextView tvTitle = new TextView(getContext());
            tvTitle.setText(update.title != null && !update.title.isEmpty() ? update.title : SiyoXConfig.DEFAULT_UPDATE_TITLE);
            tvTitle.setTextSize(16.5f);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvTitle.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvTitle.setGravity(Gravity.CENTER_HORIZONTAL);
            card.addView(tvTitle);

            LinearLayout verBox = new LinearLayout(getContext());
            verBox.setOrientation(LinearLayout.VERTICAL);
            verBox.setGravity(Gravity.CENTER_HORIZONTAL);
            verBox.setPadding(0, dp(6), 0, dp8);

            TextView tvCurVer = new TextView(getContext());
            tvCurVer.setText("当前版本：" + SiyoXConfig.VERSION_CODE);
            tvCurVer.setTextSize(11.5f);
            tvCurVer.setTypeface(Typeface.DEFAULT_BOLD);
            tvCurVer.setTextColor(SiyoXTheme.getTextSecondary(isDark));
            tvCurVer.setGravity(Gravity.CENTER_HORIZONTAL);
            verBox.addView(tvCurVer);

            TextView tvNewVer = new TextView(getContext());
            tvNewVer.setText("最新版本：" + update.latestVersionCode);
            tvNewVer.setTextSize(11.5f);
            tvNewVer.setTypeface(Typeface.DEFAULT_BOLD);
            tvNewVer.setTextColor(SiyoXTheme.getTextSecondary(isDark));
            tvNewVer.setGravity(Gravity.CENTER_HORIZONTAL);
            tvNewVer.setPadding(0, dp(3), 0, 0);
            verBox.addView(tvNewVer);

            card.addView(verBox);

            card.addView(createDivider(isDark));

            ScrollView scroll = new ScrollView(getContext());
            scroll.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            TextView tvLog = new TextView(getContext());
            tvLog.setText(update.log != null && !update.log.isEmpty() ? update.log : SiyoXConfig.DEFAULT_UPDATE_LOG);
            tvLog.setTextSize(13f);
            tvLog.setTextColor(SiyoXTheme.getTextSecondary(isDark));
            tvLog.setPadding(0, dp10, 0, dp10);
            tvLog.setLineSpacing(dp(2), 1.2f);
            scroll.addView(tvLog);
            card.addView(scroll);

            final LinearLayout progressRow = new LinearLayout(getContext());
            progressRow.setOrientation(LinearLayout.HORIZONTAL);
            progressRow.setGravity(Gravity.CENTER_VERTICAL);
            progressRow.setVisibility(View.GONE);
            LinearLayout.LayoutParams pRowParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            pRowParams.setMargins(0, dp8, 0, dp8);
            progressRow.setLayoutParams(pRowParams);

            final ProgressBar updateProgressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
            updateProgressBar.setMax(100);
            updateProgressBar.setProgress(0);
            updateProgressBar.setIndeterminate(false);

            GradientDrawable pbBg = new GradientDrawable();
            pbBg.setColor(isDark ? Color.parseColor("#2C2C2E") : Color.parseColor("#E5E7EB"));
            pbBg.setCornerRadius(dp(3));

            GradientDrawable pbProgress = new GradientDrawable();
            pbProgress.setColor(Color.parseColor("#0A84FF"));
            pbProgress.setCornerRadius(dp(3));
            ClipDrawable clipDrawable = new ClipDrawable(pbProgress, Gravity.START, ClipDrawable.HORIZONTAL);

            LayerDrawable progressLayer = new LayerDrawable(new Drawable[]{pbBg, clipDrawable});
            progressLayer.setId(0, android.R.id.background);
            progressLayer.setId(1, android.R.id.progress);
            updateProgressBar.setProgressDrawable(progressLayer);

            LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(0, dp(6), 1f);
            updateProgressBar.setLayoutParams(pbParams);
            progressRow.addView(updateProgressBar);

            final TextView tvProgressPercent = new TextView(getContext());
            tvProgressPercent.setText("0%");
            tvProgressPercent.setTextSize(12f);
            tvProgressPercent.setTypeface(Typeface.DEFAULT_BOLD);
            tvProgressPercent.setTextColor(SiyoXTheme.getAccentBlue());
            tvProgressPercent.setPadding(dp8, 0, 0, 0);
            progressRow.addView(tvProgressPercent);

            card.addView(progressRow);

            card.addView(createDivider(isDark));

            final File[] downloadedApk = new File[1];

            LinearLayout btnRow = new LinearLayout(getContext());
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(42));
            rowParams.setMargins(0, dp12, 0, 0);
            btnRow.setLayoutParams(rowParams);

            if (!update.isForce) {
                Button btnCancel = new Button(getContext());
                btnCancel.setText("稍后再说");
                btnCancel.setTextSize(13.5f);
                btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
                btnCancel.setTextColor(isDark ? Color.parseColor("#E5E5EA") : Color.parseColor("#3C3C43"));
                int cancelBg = isDark ? Color.parseColor("#3A3A3C") : Color.parseColor("#E5E7EB");
                int cancelPressed = isDark ? Color.parseColor("#2C2C2E") : Color.parseColor("#D1D5DB");
                btnCancel.setBackground(createRippleDrawable(cancelBg, cancelPressed, dp(10)));
                styleCleanButton(btnCancel);
                LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
                cancelParams.setMargins(0, 0, dp8, 0);
                btnCancel.setLayoutParams(cancelParams);
                btnCancel.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismissUpdateModal();
                    }
                });
                btnRow.addView(btnCancel);
            }

            final Button btnUpdate = new Button(getContext());
            btnUpdate.setText("立即更新");
            btnUpdate.setTextSize(13.5f);
            btnUpdate.setTypeface(Typeface.DEFAULT_BOLD);
            btnUpdate.setTextColor(Color.WHITE);
            int updateBg = Color.parseColor("#0A84FF");
            int updatePressed = Color.parseColor("#0066CC");
            btnUpdate.setBackground(createRippleDrawable(updateBg, updatePressed, dp(10)));
            styleCleanButton(btnUpdate);
            LinearLayout.LayoutParams updateParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
            btnUpdate.setLayoutParams(updateParams);
            btnUpdate.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (downloadedApk[0] != null && downloadedApk[0].exists()) {
                        triggerApkInstall(downloadedApk[0]);
                        return;
                    }
                    if (update.downloadUrl == null || update.downloadUrl.trim().isEmpty()) {
                        Toast.makeText(getContext(), "更新地址未配置", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String rawUrl = update.downloadUrl.trim();
                    if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                        rawUrl = "https://" + rawUrl;
                    }
                    if (isDirectApkUrl(rawUrl)) {
                        startInAppApkUpdate(rawUrl, update.latestVersionCode, btnUpdate, progressRow, updateProgressBar, tvProgressPercent, downloadedApk, update.isForce);
                    } else {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getContext().startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "无法打开网页: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        if (!update.isForce) {
                            dismissUpdateModal();
                        }
                    }
                }
            });
            btnRow.addView(btnUpdate);

            card.addView(btnRow);
            updateModalScrim.addView(card);

            addView(updateModalScrim);
            updateModalScrim.bringToFront();
        } catch (Throwable t) {
            SiyoXLogger.w("SiyoX_OverlayLayout", "Show update dialog exception: " + t.getMessage());
        }
    }

    private void dismissUpdateModal() {
        VerifyManager.setUpdateDismissed(true);
        if (updateModalScrim != null) {
            removeView(updateModalScrim);
            updateModalScrim = null;
        }
    }

    private boolean isDirectApkUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        String u = url.trim().toLowerCase();
        if (u.endsWith(".apk") || u.contains(".apk?") || u.contains(".apk#") || u.contains(".apk/")) {
            return true;
        }
        if (u.contains("/download/apk") || u.contains("type=apk") || u.contains("format=apk")) {
            return true;
        }
        return false;
    }

    private void startInAppApkUpdate(final String url, final int latestVersion, final Button btnUpdate, final LinearLayout progressRow, final ProgressBar progressBar, final TextView tvPercent, final File[] downloadedApk, final boolean isForce) {
        btnUpdate.setEnabled(false);
        btnUpdate.setText("下载中");
        progressRow.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        tvPercent.setText("0%");

        new Thread(new Runnable() {
            @Override
            public void run() {
                File cacheDir = getContext().getExternalCacheDir();
                if (cacheDir == null) cacheDir = getContext().getCacheDir();
                final File apkFile = new File(cacheDir, "siyox_update_" + latestVersion + ".apk");
                try {
                    URL u = new URL(url);
                    HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android)");
                    conn.connect();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 301 || responseCode == 302 || responseCode == 303 || responseCode == 307 || responseCode == 308) {
                        String redirectUrl = conn.getHeaderField("Location");
                        if (redirectUrl != null && !redirectUrl.isEmpty()) {
                            conn.disconnect();
                            u = new URL(redirectUrl);
                            conn = (HttpURLConnection) u.openConnection();
                            conn.setConnectTimeout(15000);
                            conn.setReadTimeout(30000);
                            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android)");
                            conn.connect();
                        }
                    }

                    int totalLength = conn.getContentLength();
                    InputStream is = conn.getInputStream();
                    OutputStream os = new FileOutputStream(apkFile);
                    byte[] buffer = new byte[8192];
                    int read;
                    long downloaded = 0;
                    long lastProgressUpdate = 0;

                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                        downloaded += read;
                        long now = System.currentTimeMillis();
                        if (totalLength > 0 && now - lastProgressUpdate > 100) {
                            lastProgressUpdate = now;
                            final int progress = (int) (downloaded * 100 / totalLength);
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setProgress(progress);
                                    tvPercent.setText(progress + "%");
                                    btnUpdate.setText("下载中");
                                }
                            });
                        }
                    }
                    os.flush();
                    os.close();
                    is.close();
                    conn.disconnect();

                    if (apkFile.exists() && apkFile.length() > 0) {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setProgress(100);
                                tvPercent.setText("100%");
                                downloadedApk[0] = apkFile;
                                btnUpdate.setEnabled(true);
                                btnUpdate.setText("安装");
                                triggerApkInstall(apkFile);
                            }
                        });
                    } else {
                        throw new Exception("下载文件为空");
                    }
                } catch (final Throwable t) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            progressRow.setVisibility(View.GONE);
                            btnUpdate.setEnabled(true);
                            btnUpdate.setText("重试下载");
                            Toast.makeText(getContext(), "应用内下载失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                getContext().startActivity(intent);
                            } catch (Throwable ignored) {}
                        }
                    });
                }
            }
        }).start();
    }

    private void triggerApkInstall(File apkFile) {
        if (apkFile == null || !apkFile.exists()) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= 24) {
                Uri apkUri = androidx.core.content.FileProvider.getUriForFile(
                        getContext(),
                        getContext().getPackageName() + ".fileprovider",
                        apkFile
                );
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            } else {
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            }
            getContext().startActivity(intent);
        } catch (Throwable t) {
            try {
                Intent fallback = new Intent(Intent.ACTION_VIEW);
                fallback.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(fallback);
            } catch (Throwable t2) {
                Toast.makeText(getContext(), "调起安装器失败: " + t2.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

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
        tvSiyo.setTextColor(SiyoXTheme.getTextSiyo(isDark)); 
        layout.addView(tvSiyo);

        TextView tvX = new TextView(context);
        tvX.setText("X");
        tvX.setTextSize(textSize);
        tvX.setTypeface(Typeface.DEFAULT_BOLD);
        tvX.setTextColor(SiyoXTheme.getAccentBlue()); 
        layout.addView(tvX);

        return layout;
    }

    private View createSiyoXTitle(float textSize, boolean isDark) {
        return createSiyoXTitleView(getContext(), textSize, isDark);
    }

private static class RippleWaveView extends View {
        private float centerX = 0f;
        private float centerY = 0f;
        private float currentRadius = 0f;
        private float maxRadius = 0f;
        private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public RippleWaveView(Context context) {
            super(context);
            wavePaint.setColor(Color.parseColor("#80000000")); 
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

    private void showConfirmDialog(String title, String message, String confirmText, boolean isDanger, final Runnable onConfirm) {
        showCustomConfirmDialog(title, message, "取消", confirmText, isDanger, onConfirm);
    }

    private void showCustomConfirmDialog(String title, String message, String cancelText, String confirmText, boolean isDanger, final Runnable onConfirm) {
        try {
            final Dialog dialog = new Dialog(getContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setDimAmount(0.5f);
            }

            boolean isDark = SiyoXTheme.isDarkMode(getContext());
            int dp16 = dp(16);
            int dp14 = dp(14);
            int dp12 = dp(12);
            int dp10 = dp(10);
            int dp8 = dp(8);

            LinearLayout container = new LinearLayout(getContext());
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(dp16, dp16, dp16, dp16);
            container.setBackground(createCardBg(SiyoXTheme.getCardBg(isDark), Color.TRANSPARENT, dp(18)));

            int maxW = Math.min(getRealScreenSize()[0] - dp(64), dp(320));
            LinearLayout.LayoutParams cParams = new LinearLayout.LayoutParams(maxW, LayoutParams.WRAP_CONTENT);
            container.setLayoutParams(cParams);

            TextView tvTitle = new TextView(getContext());
            tvTitle.setText(title);
            tvTitle.setTextSize(16f);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvTitle.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvTitle.setGravity(Gravity.CENTER_HORIZONTAL);
            container.addView(tvTitle);

            TextView tvMsg = new TextView(getContext());
            tvMsg.setText(message);
            tvMsg.setTextSize(13f);
            tvMsg.setTextColor(SiyoXTheme.getTextSecondary(isDark));
            tvMsg.setGravity(Gravity.CENTER_HORIZONTAL);
            tvMsg.setPadding(0, dp8, 0, dp16);
            tvMsg.setLineSpacing(dp(2), 1.15f);
            container.addView(tvMsg);

            LinearLayout btnRow = new LinearLayout(getContext());
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(40));
            btnRow.setLayoutParams(rowParams);

            Button btnCancel = new Button(getContext());
            btnCancel.setText(cancelText);
            btnCancel.setTextSize(13.5f);
            btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
            btnCancel.setTextColor(isDark ? Color.parseColor("#E5E5EA") : Color.parseColor("#3C3C43"));
            int cancelBg = isDark ? Color.parseColor("#3A3A3C") : Color.parseColor("#E5E7EB");
            int cancelPressed = isDark ? Color.parseColor("#2C2C2E") : Color.parseColor("#D1D5DB");
            btnCancel.setBackground(createRippleDrawable(cancelBg, cancelPressed, dp(10)));
            styleCleanButton(btnCancel);
            LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
            cancelParams.setMargins(0, 0, dp8, 0);
            btnCancel.setLayoutParams(cancelParams);
            btnCancel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            btnRow.addView(btnCancel);

            Button btnConfirm = new Button(getContext());
            btnConfirm.setText(confirmText);
            btnConfirm.setTextSize(13.5f);
            btnConfirm.setTypeface(Typeface.DEFAULT_BOLD);
            btnConfirm.setTextColor(Color.WHITE);
            int confirmBg = isDanger ? Color.parseColor("#FF3B30") : Color.parseColor("#0A84FF");
            int confirmPressed = isDanger ? Color.parseColor("#D70015") : Color.parseColor("#0066CC");
            btnConfirm.setBackground(createRippleDrawable(confirmBg, confirmPressed, dp(10)));
            styleCleanButton(btnConfirm);
            LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
            btnConfirm.setLayoutParams(confirmParams);
            btnConfirm.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    if (onConfirm != null) onConfirm.run();
                }
            });
            btnRow.addView(btnConfirm);

            container.addView(btnRow);
            dialog.setContentView(container);
            dialog.show();
        } catch (Throwable t) {
            if (onConfirm != null) onConfirm.run();
        }
    }

    private static void styleCleanButton(Button btn) {
        if (btn == null) return;
        btn.setStateListAnimator(null);
        btn.setElevation(0f);
        btn.setOutlineProvider(null);
        btn.setTransformationMethod(null);
    }

    private TextView createSectionTitle(String title, boolean isDark) {
        TextView tv = new TextView(getContext());
        tv.setText(title);
        tv.setTextSize(13f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(SiyoXTheme.getTextSecondary(isDark));
        tv.setPadding(0, dp(10), 0, dp(4)); 
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

private RippleDrawable createExitRippleDrawable(int normalColor, int radius) {
        GradientDrawable content = createCardBg(normalColor, Color.TRANSPARENT, radius);
        GradientDrawable mask = createCardBg(Color.WHITE, Color.TRANSPARENT, radius);
        return new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#40FFFFFF")), content, mask);
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                v,
                getContext().getResources().getDisplayMetrics()
        );
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                v,
                getContext().getResources().getDisplayMetrics()
        );
    }

    public static class SiyoXLoadingBar extends View {
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF trackRect = new RectF();
        private final RectF barRect = new RectF();
        private ValueAnimator animator;
        private float progressPos = 0f;
        private boolean isRunning = false;

        public SiyoXLoadingBar(Context context) {
            super(context);
            init();
        }

        private void init() {
            trackPaint.setStyle(Paint.Style.FILL);
            barPaint.setStyle(Paint.Style.FILL);
            trackPaint.setColor(Color.parseColor("#1A0A84FF"));
        }

        public void setColors(boolean isDark) {
            trackPaint.setColor(isDark ? Color.parseColor("#220A84FF") : Color.parseColor("#18007AFF"));
            invalidate();
        }

        private void startAnim() {
            if (animator != null && animator.isRunning()) return;
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(1100);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    progressPos = (float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            animator.start();
            isRunning = true;
        }

        private void stopAnim() {
            if (animator != null) {
                animator.cancel();
                animator = null;
            }
            isRunning = false;
        }

        @Override
        public void setVisibility(int visibility) {
            super.setVisibility(visibility);
            if (visibility == VISIBLE) {
                startAnim();
            } else {
                stopAnim();
            }
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (getVisibility() == VISIBLE) {
                startAnim();
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            stopAnim();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            float radius = h / 2f;
            trackRect.set(0, 0, w, h);
            canvas.drawRoundRect(trackRect, radius, radius, trackPaint);

            if (isRunning) {
                float barWidth = w * 0.38f;
                float startX = (w + barWidth) * progressPos - barWidth;
                float endX = startX + barWidth;

                float left = Math.max(0, startX);
                float right = Math.min(w, endX);

                if (right > left) {
                    Shader shader = new LinearGradient(
                            startX, 0, endX, 0,
                            new int[]{Color.parseColor("#00C6FF"), Color.parseColor("#0A84FF"), Color.parseColor("#5E5CE6")},
                            null,
                            Shader.TileMode.CLAMP
                    );
                    barPaint.setShader(shader);
                    barRect.set(left, 0, right, h);
                    canvas.drawRoundRect(barRect, radius, radius, barPaint);
                }
            }
        }
    }
}
