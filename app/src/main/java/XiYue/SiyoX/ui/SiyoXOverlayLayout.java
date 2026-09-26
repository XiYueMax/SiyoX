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
import org.json.JSONObject;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
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
import XiYue.SiyoX.SiyoXEntityKillerConfig;
import XiYue.SiyoX.data.AppSettings;
import XiYue.SiyoX.data.EntityKillerManager;
import XiYue.SiyoX.data.LoginVideoManager;
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
    private DynamicIslandView dynamicIslandView;

    private KeyDisplayView keyDisplayView;
    private KeyTriggerView keyTriggerView;
    private FpsDisplayView fpsDisplayView;

    private int editingKeyIndex = -1;

    private FrameLayout keyEditOverlay;

    private boolean keyEditMode = false;
    private TextView tvWatermark;
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

        fixOverlayTouchable();
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
        buildKeyDisplayView();
        buildKeyTriggerView();
        buildFpsDisplayView();
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
        dynamicIslandView = new DynamicIslandView(getContext());
        dynamicIslandView.setVisibility(appSettings.isDynamicIslandEnabled() && verifyManager.isVerified() ? View.VISIBLE : View.GONE);
        addView(dynamicIslandView, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.TOP));
        tvWatermark = new TextView(getContext());
        tvWatermark.setText(SiyoXConfig.WATERMARK_TEXT);
        tvWatermark.setTextSize(10.5f);
        tvWatermark.setTextColor(Color.parseColor("#668E8E93"));
        tvWatermark.setShadowLayer(dp(1.2f), 1, 1, Color.parseColor("#80000000"));
        tvWatermark.setClickable(false);
        tvWatermark.setFocusable(false);
        FrameLayout.LayoutParams wmParams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.END);
        wmParams.setMargins(0, 0, dp(12), dp(8));
        addView(tvWatermark, wmParams);
        updateWatermarkVisibility();
        updateCategories();
        switchCategory(0);
    }
    private void updateCategories() {
        if (categoryListLayout == null) return;
        categoryListLayout.removeAllViews();
        categoryTabViews.clear();
        int dp10 = dp(10);
        int dp8 = dp(8);
        List<String> categories = new ArrayList<>();
        categories.add("资源列表");
        categories.add("辅助功能");
        categories.add("个人中心");
        categories.add("关于软件");
        if (appSettings.isDevModeEnabled()) {
            categories.add("开发调试");
        }
        for (int i = 0; i < categories.size(); i++) {
            final int index = i;
            TextView tabView = new TextView(getContext());
            tabView.setText(categories.get(i));
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
    }
    private void updateWatermarkVisibility() {
        if (tvWatermark == null) return;
        if (!verifyManager.isVerified()) {
            tvWatermark.setVisibility(View.GONE);
            return;
        }
        boolean show = SiyoXConfig.ENABLE_WATERMARK;
        if (SiyoXConfig.ALLOW_PANEL_TOGGLE_WATERMARK) {
            show = show && appSettings.isWatermarkEnabled();
        }
        tvWatermark.setVisibility(show ? View.VISIBLE : View.GONE);
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
            TextView tvVersion = new TextView(getContext());
            tvVersion.setText(String.valueOf(SiyoXConfig.VERSION_CODE));
            tvVersion.setTextSize(13f);
            tvVersion.setTypeface(Typeface.DEFAULT_BOLD);
            tvVersion.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvVersion.setSoundEffectsEnabled(false);
            tvVersion.setClickable(true);
            tvVersion.setFocusable(false);
            tvVersion.setBackground(null);
            View versionRow = createCustomInfoRow("模块版本", tvVersion, isDark);
            versionRow.setSoundEffectsEnabled(false);
            versionRow.setBackground(null);
            versionRow.setClickable(true);
            OnClickListener devModeTrigger = new OnClickListener() {
                private int clickCount = 0;
                private long lastClickTime = 0;
                @Override
                public void onClick(View v) {
                    long now = System.currentTimeMillis();
                    if (now - lastClickTime > 2500) {
                        clickCount = 0;
                    }
                    lastClickTime = now;
                    clickCount++;
                    if (clickCount >= 5) {
                        clickCount = 0;
                        if (!appSettings.isDevModeEnabled()) {
                            showCustomConfirmDialog("开发者调试模式", "是否进入开发者调试模式？开启后将在左侧导航栏显示“开发调试”入口。", "取消", "确定进入", false, new Runnable() {
                                @Override
                                public void run() {
                                    appSettings.setDevModeEnabled(true);
                                    updateCategories();
                                    switchCategory(4);
                                    Toast.makeText(getContext(), "已进入开发者调试模式", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(getContext(), "开发者调试模式已处于开启状态", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            };
            tvVersion.setOnClickListener(devModeTrigger);
            versionRow.setOnClickListener(devModeTrigger);
            aboutCard.addView(versionRow);
            aboutCard.addView(createDivider(isDark));
            aboutCard.addView(createInfoRowItem("软件作者", SiyoXConfig.AUTHOR, isDark));
            aboutCard.addView(createDivider(isDark));
            aboutCard.addView(createInfoRowItem("当前作用域", SiyoXConfig.TARGET_PACKAGE, isDark));
            featureListContent.addView(aboutCard);
        } else if (categoryIndex == 4) {
            renderDevDebugFeatures(isDark);
        }
    }
    private void renderDevDebugFeatures(final boolean isDark) {
        int dp16 = dp(16);
        int dp14 = dp(14);
        int dp12 = dp(12);
        int dp10 = dp(10);
        int dp8 = dp(8);
        LinearLayout islandTestCard = createInnerCard(isDark);
        islandTestCard.setPadding(dp16, dp14, dp16, dp14);
        TextView tvIslandHeader = new TextView(getContext());
        tvIslandHeader.setText("灵动岛状态与进度模拟");
        tvIslandHeader.setTextSize(13.5f);
        tvIslandHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvIslandHeader.setTextColor(SiyoXTheme.getAccentBlue());
        tvIslandHeader.setPadding(0, 0, 0, dp10);
        islandTestCard.addView(tvIslandHeader);
        LinearLayout row1 = new LinearLayout(getContext());
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(38)));
        Button btnSimResProg = new Button(getContext());
        btnSimResProg.setText("模拟资源下载 (45%)");
        btnSimResProg.setTextSize(12f);
        btnSimResProg.setTypeface(Typeface.DEFAULT_BOLD);
        btnSimResProg.setTextColor(Color.WHITE);
        btnSimResProg.setBackground(createRippleDrawable(Color.parseColor("#0A84FF"), Color.parseColor("#0066CC"), dp(8)));
        styleCleanButton(btnSimResProg);
        LinearLayout.LayoutParams lpBtn1 = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        lpBtn1.setMargins(0, 0, dp8, 0);
        btnSimResProg.setLayoutParams(lpBtn1);
        btnSimResProg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ResourceInjector.getGlobalDownloadListener() != null) {
                    ResourceInjector.getGlobalDownloadListener().onDownloadProgress("测试资源包.zip", 45, 4500, 10000);
                }
            }
        });
        row1.addView(btnSimResProg);
        Button btnSimResDone = new Button(getContext());
        btnSimResDone.setText("模拟下载完成");
        btnSimResDone.setTextSize(12f);
        btnSimResDone.setTypeface(Typeface.DEFAULT_BOLD);
        btnSimResDone.setTextColor(Color.WHITE);
        btnSimResDone.setBackground(createRippleDrawable(Color.parseColor("#30D158"), Color.parseColor("#249A40"), dp(8)));
        styleCleanButton(btnSimResDone);
        LinearLayout.LayoutParams lpBtn2 = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        btnSimResDone.setLayoutParams(lpBtn2);
        btnSimResDone.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ResourceInjector.getGlobalDownloadListener() != null) {
                    ResourceInjector.getGlobalDownloadListener().onDownloadComplete("测试资源包.zip", true, "下载完成");
                }
            }
        });
        row1.addView(btnSimResDone);
        islandTestCard.addView(row1);
        LinearLayout rowSimAnim = new LinearLayout(getContext());
        rowSimAnim.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lpRowSim = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(38));
        lpRowSim.setMargins(0, dp8, 0, 0);
        rowSimAnim.setLayoutParams(lpRowSim);
        Button btnSimFull = new Button(getContext());
        btnSimFull.setText("模拟完整下载过程");
        btnSimFull.setTextSize(12f);
        btnSimFull.setTypeface(Typeface.DEFAULT_BOLD);
        btnSimFull.setTextColor(Color.WHITE);
        btnSimFull.setBackground(createRippleDrawable(Color.parseColor("#5856D6"), Color.parseColor("#4442A8"), dp(8)));
        styleCleanButton(btnSimFull);
        LinearLayout.LayoutParams lpBtnFull = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        lpBtnFull.setMargins(0, 0, dp8, 0);
        btnSimFull.setLayoutParams(lpBtnFull);
        btnSimFull.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startSimulatedDownload();
            }
        });
        rowSimAnim.addView(btnSimFull);
        Button btnStopSim = new Button(getContext());
        btnStopSim.setText("停止模拟进度");
        btnStopSim.setTextSize(12f);
        btnStopSim.setTypeface(Typeface.DEFAULT_BOLD);
        btnStopSim.setTextColor(Color.WHITE);
        btnStopSim.setBackground(createRippleDrawable(Color.parseColor("#8E8E93"), Color.parseColor("#636366"), dp(8)));
        styleCleanButton(btnStopSim);
        LinearLayout.LayoutParams lpBtnStop = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        btnStopSim.setLayoutParams(lpBtnStop);
        btnStopSim.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSimulatedDownload();
            }
        });
        rowSimAnim.addView(btnStopSim);
        islandTestCard.addView(rowSimAnim);
        LinearLayout row2 = new LinearLayout(getContext());
        row2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lpRow2 = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(38));
        lpRow2.setMargins(0, dp8, 0, 0);
        row2.setLayoutParams(lpRow2);
        Button btnSimVidProg = new Button(getContext());
        btnSimVidProg.setText("模拟视频下载 (80%)");
        btnSimVidProg.setTextSize(12f);
        btnSimVidProg.setTypeface(Typeface.DEFAULT_BOLD);
        btnSimVidProg.setTextColor(Color.WHITE);
        btnSimVidProg.setBackground(createRippleDrawable(Color.parseColor("#FF9500"), Color.parseColor("#CC7700"), dp(8)));
        styleCleanButton(btnSimVidProg);
        LinearLayout.LayoutParams lpBtn3 = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        lpBtn3.setMargins(0, 0, dp8, 0);
        btnSimVidProg.setLayoutParams(lpBtn3);
        btnSimVidProg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LoginVideoManager.get().getGlobalListener() != null) {
                    LoginVideoManager.get().getGlobalListener().onProgress(80, "下载中");
                }
            }
        });
        row2.addView(btnSimVidProg);
        Button btnSimFail = new Button(getContext());
        btnSimFail.setText("模拟下载失败");
        btnSimFail.setTextSize(12f);
        btnSimFail.setTypeface(Typeface.DEFAULT_BOLD);
        btnSimFail.setTextColor(Color.WHITE);
        btnSimFail.setBackground(createRippleDrawable(Color.parseColor("#FF3B30"), Color.parseColor("#CC2E26"), dp(8)));
        styleCleanButton(btnSimFail);
        LinearLayout.LayoutParams lpBtn4 = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        btnSimFail.setLayoutParams(lpBtn4);
        btnSimFail.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ResourceInjector.getGlobalDownloadListener() != null) {
                    ResourceInjector.getGlobalDownloadListener().onDownloadComplete("测试资源包.zip", false, "网络连接失败");
                }
            }
        });
        row2.addView(btnSimFail);
        islandTestCard.addView(row2);
        featureListContent.addView(islandTestCard);
        LinearLayout dialogTestCard = createInnerCard(isDark);
        dialogTestCard.setPadding(dp16, dp14, dp16, dp14);
        LinearLayout.LayoutParams lpDiaCard = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lpDiaCard.setMargins(0, dp12, 0, 0);
        dialogTestCard.setLayoutParams(lpDiaCard);
        TextView tvDialogHeader = new TextView(getContext());
        tvDialogHeader.setText("弹窗与功能触发测试");
        tvDialogHeader.setTextSize(13.5f);
        tvDialogHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvDialogHeader.setTextColor(SiyoXTheme.getAccentBlue());
        tvDialogHeader.setPadding(0, 0, 0, dp10);
        dialogTestCard.addView(tvDialogHeader);
        LinearLayout row3 = new LinearLayout(getContext());
        row3.setOrientation(LinearLayout.HORIZONTAL);
        row3.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(38)));
        Button btnTestUpdate = new Button(getContext());
        btnTestUpdate.setText("触发更新弹窗");
        btnTestUpdate.setTextSize(12f);
        btnTestUpdate.setTypeface(Typeface.DEFAULT_BOLD);
        btnTestUpdate.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1C1C1E"));
        int btnBg = isDark ? Color.parseColor("#3A3A3C") : Color.parseColor("#E5E7EB");
        int btnPressed = isDark ? Color.parseColor("#2C2C2E") : Color.parseColor("#D1D5DB");
        btnTestUpdate.setBackground(createRippleDrawable(btnBg, btnPressed, dp(8)));
        styleCleanButton(btnTestUpdate);
        LinearLayout.LayoutParams lpBtn5 = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        lpBtn5.setMargins(0, 0, dp8, 0);
        btnTestUpdate.setLayoutParams(lpBtn5);
        btnTestUpdate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                VerifyManager.setUpdateDismissed(false);
                VerifyManager.SoftwareUpdate update = new VerifyManager.SoftwareUpdate();
                update.hasUpdate = true;
                update.latestVersionCode = 999;
                update.latestVersionName = "9.9.9";
                update.title = "开发者测试更新";
                update.log = "1. 这是开发者调试模式下的模拟更新弹窗\n2. 测试当前版本与最新版本排版展示\n3. 欢迎测试各项按钮响应";
                update.downloadUrl = "https://example.com/test.apk";
                update.isForce = false;
                showUpdateDialog(update);
            }
        });
        row3.addView(btnTestUpdate);
        Button btnTestNotice = new Button(getContext());
        btnTestNotice.setText("触发弹窗");
        btnTestNotice.setTextSize(12f);
        btnTestNotice.setTypeface(Typeface.DEFAULT_BOLD);
        btnTestNotice.setTextColor(isDark ? Color.WHITE : Color.parseColor("#1C1C1E"));
        btnTestNotice.setBackground(createRippleDrawable(btnBg, btnPressed, dp(8)));
        styleCleanButton(btnTestNotice);
        LinearLayout.LayoutParams lpBtn6 = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        btnTestNotice.setLayoutParams(lpBtn6);
        btnTestNotice.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomConfirmDialog("测试官方公告", "这是一条在开发者调试模式下触发的模拟公告信息，用于检查公告弹窗的字体排版、边距与滚动交互。", "关闭", "确定", false, null);
            }
        });
        row3.addView(btnTestNotice);
        dialogTestCard.addView(row3);
        featureListContent.addView(dialogTestCard);
        Button btnCloseDevMode = new Button(getContext());
        btnCloseDevMode.setText("关闭开发者调试模式");
        btnCloseDevMode.setTextSize(13.5f);
        btnCloseDevMode.setTypeface(Typeface.DEFAULT_BOLD);
        btnCloseDevMode.setTextColor(Color.parseColor("#FF3B30"));
        btnCloseDevMode.setBackground(createExitRippleDrawable(SiyoXTheme.getExitBtnBg(isDark), dp(12)));
        styleCleanButton(btnCloseDevMode);
        LinearLayout.LayoutParams lpCloseDev = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(44));
        lpCloseDev.setMargins(0, dp14, 0, 0);
        btnCloseDevMode.setLayoutParams(lpCloseDev);
        btnCloseDevMode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfirmDialog("关闭开发调试", "确定要关闭开发者调试模式并隐藏开发调试分类吗？", "确定关闭", true, new Runnable() {
                    @Override
                    public void run() {
                        stopSimulatedDownload();
                        appSettings.setDevModeEnabled(false);
                        updateCategories();
                        switchCategory(3);
                        Toast.makeText(getContext(), "已退出开发者调试模式", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        featureListContent.addView(btnCloseDevMode);
    }
    private boolean isSimulatingDownload = false;
    private int simProgress = 0;
    private final Handler simHandler = new Handler(Looper.getMainLooper());
    private final Runnable simRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isSimulatingDownload) return;
            simProgress += 4;
            if (simProgress < 100) {
                if (ResourceInjector.getGlobalDownloadListener() != null) {
                    ResourceInjector.getGlobalDownloadListener().onDownloadProgress("模拟资源包.zip", simProgress, simProgress * 1024L * 1024L, 100L * 1024L * 1024L);
                }
                simHandler.postDelayed(this, 120);
            } else {
                isSimulatingDownload = false;
                simProgress = 0;
                if (ResourceInjector.getGlobalDownloadListener() != null) {
                    ResourceInjector.getGlobalDownloadListener().onDownloadComplete("模拟资源包.zip", true, "下载完成");
                }
            }
        }
    };
    private void startSimulatedDownload() {
        stopSimulatedDownload();
        isSimulatingDownload = true;
        simProgress = 0;
        if (ResourceInjector.getGlobalDownloadListener() != null) {
            ResourceInjector.getGlobalDownloadListener().onDownloadProgress("模拟资源包.zip", 0, 0, 100L * 1024L * 1024L);
        }
        simHandler.postDelayed(simRunnable, 120);
    }
    private void stopSimulatedDownload() {
        isSimulatingDownload = false;
        simProgress = 0;
        simHandler.removeCallbacks(simRunnable);
        if (dynamicIslandView != null) {
            dynamicIslandView.resetToIdle();
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
                    tvStatus.setText("注入中");
                    tvStatus.setTextColor(SiyoXTheme.getAccentBlue());
                    Toast.makeText(getContext(), "注入中", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getContext(), "下载完成，点击“注入资源”生效", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getContext(), "注入中", Toast.LENGTH_SHORT).show();
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
        featureListContent.addView(createDynamicIslandFeatureCard("灵动岛悬浮顶栏", "在游戏顶部实时显示客户端名称、精准时钟与各项下载进度", appSettings.isDynamicIslandEnabled(), isDark, new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDynamicIslandSettingsDialog();
            }
        }, new MiuiXSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(MiuiXSwitch switchView, boolean isChecked) {
                appSettings.setDynamicIslandEnabled(isChecked);
                if (dynamicIslandView != null) {
                    dynamicIslandView.setVisibility(isChecked && verifyManager.isVerified() ? View.VISIBLE : View.GONE);
                }
            }
        }));
        if (SiyoXConfig.ALLOW_PANEL_TOGGLE_WATERMARK) {
            featureListContent.addView(createMiuiXFeatureCard("屏幕右下角水印", "在屏幕右下角以灰色字体常驻显示专属水印信息", appSettings.isWatermarkEnabled(), isDark, new MiuiXSwitch.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(MiuiXSwitch switchView, boolean isChecked) {
                    appSettings.setWatermarkEnabled(isChecked);
                    updateWatermarkVisibility();
                }
            }));
        }
        featureListContent.addView(createMiuiXFeatureCard("绕过热更新", "拦截网易 UniFix 热更新请求，绕过远程补丁拉取与动态反作弊模块装载", appSettings.isUniFixBypassEnabled(), isDark, new MiuiXSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(MiuiXSwitch switchView, boolean isChecked) {
                appSettings.setUniFixBypassEnabled(isChecked);
                Toast.makeText(getContext(), isChecked ? "已开启绕过热更新，重启游戏后生效" : "已关闭绕过热更新，重启游戏后生效", Toast.LENGTH_SHORT).show();
            }
        }));
        boolean isCloudKillerEnabled = SiyoXEntityKillerConfig.ENABLE_CUSTOM_ENTITY_KILLER || SiyoXConfig.ENABLE_ENTITY_KILLER;
        String killerDesc = isCloudKillerEnabled
                ? (SiyoXEntityKillerConfig.ENABLE_CUSTOM_ENTITY_KILLER
                    ? "实时删除网易的Entity文件，让你的材质正常显示（已启用自定义配置）"
                    : "实时删除网易的Entity文件，让你的材质正常显示")
                : "实时删除网易的Entity文件，让你的材质正常显示（已在cpp配置中关闭）";
        boolean isKillerChecked = isCloudKillerEnabled && appSettings.isEntityKillerEnabled();
        featureListContent.addView(createEntityKillerCard("EntityKiller", killerDesc, isKillerChecked, isDark, new OnClickListener() {
            private volatile boolean isManualKilling = false;
            @Override
            public void onClick(View v) {
                if (isManualKilling) {
                    Toast.makeText(getContext(), "正在运行EntityKiller文件扫描...", Toast.LENGTH_SHORT).show();
                    return;
                }
                isManualKilling = true;
                Toast.makeText(getContext(), "正在运行EntityKiller文件扫描...", Toast.LENGTH_SHORT).show();
                new Thread(() -> {
                    try {
                        int count = EntityKillerManager.cleanAllNow();
                        post(() -> {
                            if (count > 0) {
                                Toast.makeText(getContext(), "清理完成！共清除 " + count + " 个文件", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "扫描完成，当前未发现待清理的文件。", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Throwable t) {
                        post(() -> Toast.makeText(getContext(), "清理异常: " + t.getMessage(), Toast.LENGTH_SHORT).show());
                    } finally {
                        isManualKilling = false;
                    }
                }, "SiyoX-ManualKill").start();
            }
        }, new MiuiXSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(MiuiXSwitch switchView, boolean isChecked) {
                if (!SiyoXEntityKillerConfig.ENABLE_CUSTOM_ENTITY_KILLER && !SiyoXConfig.ENABLE_ENTITY_KILLER) {
                    switchView.setChecked(false);
                    Toast.makeText(getContext(), "已在 cpp 配置中关闭此功能", Toast.LENGTH_SHORT).show();
                    return;
                }
                appSettings.setEntityKillerEnabled(isChecked);
                EntityKillerManager.setEnabled(isChecked);
            }
        }));

        featureListContent.addView(createKeyDisplayCard(isDark));

        featureListContent.addView(createFpsDisplayCard(isDark));
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
    private View createDynamicIslandFeatureCard(String title, String desc, boolean initial, boolean isDark, OnClickListener settingsClickListener, MiuiXSwitch.OnCheckedChangeListener listener) {
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
        LinearLayout btnSettings = new LinearLayout(getContext());
        btnSettings.setGravity(Gravity.CENTER);
        btnSettings.setBackground(createRippleDrawable(Color.TRANSPARENT, Color.parseColor("#20000000"), dp(8)));
        btnSettings.setClickable(true);
        btnSettings.setPadding(dp(6), dp(6), dp(6), dp(6));
        LinearLayout.LayoutParams btnSettingsParams = new LinearLayout.LayoutParams(dp(32), dp(32));
        btnSettingsParams.setMargins(0, 0, dp(8), 0);
        btnSettings.setLayoutParams(btnSettingsParams);
        SettingsIconView settingsIcon = new SettingsIconView(getContext());
        settingsIcon.setIconColor(Color.BLACK);
        btnSettings.addView(settingsIcon);
        btnSettings.setOnClickListener(settingsClickListener);
        card.addView(btnSettings);
        MiuiXSwitch miuixSwitch = new MiuiXSwitch(getContext());
        miuixSwitch.setChecked(initial, false);
        miuixSwitch.setOnCheckedChangeListener(listener);
        card.addView(miuixSwitch);
        return card;
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
    private View createEntityKillerCard(String title, String desc, boolean initial, boolean isDark, OnClickListener onKillListener, MiuiXSwitch.OnCheckedChangeListener switchListener) {
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
        Button btnKill = new Button(getContext());
        btnKill.setText("手动清理");
        btnKill.setTextSize(12f);
        btnKill.setTypeface(Typeface.DEFAULT_BOLD);
        btnKill.setTextColor(Color.WHITE);
        btnKill.setBackground(createRippleDrawable(Color.parseColor("#0A84FF"), Color.parseColor("#0066CC"), dp(10)));
        styleCleanButton(btnKill);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp(30));
        btnParams.rightMargin = dp(10);
        btnKill.setLayoutParams(btnParams);
        btnKill.setPadding(dp(12), 0, dp(12), 0);
        btnKill.setOnClickListener(onKillListener);
        card.addView(btnKill);
        MiuiXSwitch miuixSwitch = new MiuiXSwitch(getContext());
        miuixSwitch.setChecked(initial, false);
        miuixSwitch.setOnCheckedChangeListener(switchListener);
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

        setGameplayOverlaysVisible(false);
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

        fixOverlayTouchable();
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

                        setGameplayOverlaysVisible(true);
                        if (verifyManager.isVerified()) {
                            floatingBall.setVisibility(View.VISIBLE);
                        }
                        if (dynamicIslandView != null && appSettings.isDynamicIslandEnabled() && verifyManager.isVerified()) {
                            dynamicIslandView.bringToFront();
                        }
                        if (appSettings.isKeyDisplayEnabled() && keyDisplayView != null) {
                            keyDisplayView.bringToFront();
                        }
                        if (appSettings.isFpsDisplayEnabled() && fpsDisplayView != null) {
                            fpsDisplayView.bringToFront();
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
            if (dynamicIslandView != null) {
                dynamicIslandView.setVisibility(View.GONE);
            }
            if (tvWatermark != null) {
                tvWatermark.setVisibility(View.GONE);
            }
        }
    }
    private void onVerifySuccess() {
        fullScreenVerifyView.setVisibility(View.GONE);
        floatingBall.setVisibility(View.VISIBLE);
        if (dynamicIslandView != null) {
            dynamicIslandView.updateClientName();
            dynamicIslandView.setVisibility(appSettings.isDynamicIslandEnabled() ? View.VISIBLE : View.GONE);
        }
        updateWatermarkVisibility();
        if (tvTopExpireBadge != null) {
            tvTopExpireBadge.setText("到期时间: " + VerifyManager.formatDate(verifyManager.getExpireTimestamp()));
        }
        LoginVideoManager.get().checkAndStartLoginVideo(getContext());
        checkAndShowUpdateDialog();
    }
    private void checkAndShowUpdateDialog() {
        verifyManager.checkSoftwareUpdate(new VerifyManager.UpdateCallback() {
            @Override
            public void onUpdateResult(final boolean hasUpdate, final VerifyManager.SoftwareUpdate update) {
                if (hasUpdate && update != null && (!VerifyManager.isUpdateDismissed() || update.isForce)) {
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
                HttpURLConnection conn = null;
                InputStream is = null;
                OutputStream os = null;
                try {
                    URL u = new URL(url);
                    int redirectCount = 0;
                    while (redirectCount < 5) {
                        conn = (HttpURLConnection) u.openConnection();
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
                                redirectCount++;
                                continue;
                            }
                        }
                        break;
                    }
                    int totalLength = conn.getContentLength();
                    is = conn.getInputStream();
                    os = new FileOutputStream(apkFile);
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
                    os = null;
                    is.close();
                    is = null;
                    conn.disconnect();
                    conn = null;
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
                } finally {
                    try { if (os != null) os.close(); } catch (Throwable ignored) {}
                    try { if (is != null) is.close(); } catch (Throwable ignored) {}
                    try { if (conn != null) conn.disconnect(); } catch (Throwable ignored) {}
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
    private void showDynamicIslandSettingsDialog() {
        try {
            final Dialog dialog = new Dialog(getContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setDimAmount(0.4f);
            }
            final boolean isDark = SiyoXTheme.isDarkMode(getContext());
            int dp16 = dp(16);
            int dp12 = dp(12);
            int dp10 = dp(10);
            int dp8 = dp(8);
            int dp6 = dp(6);
            final LinearLayout container = new LinearLayout(getContext());
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(dp16, dp16, dp16, dp16);
            container.setBackground(createCardBg(SiyoXTheme.getCardBg(isDark), Color.TRANSPARENT, dp(18)));
            int maxW = Math.min(getRealScreenSize()[0] - dp(32), dp(620));
            LinearLayout.LayoutParams cParams = new LinearLayout.LayoutParams(maxW, LayoutParams.WRAP_CONTENT);
            container.setLayoutParams(cParams);
            TextView tvTitle = new TextView(getContext());
            tvTitle.setText("灵动岛个性化设置");
            tvTitle.setTextSize(16.5f);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvTitle.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvTitle.setGravity(Gravity.CENTER_HORIZONTAL);
            tvTitle.setPadding(0, 0, 0, dp12);
            container.addView(tvTitle);
            final Runnable setTranslucent = new Runnable() {
                @Override
                public void run() {
                    container.animate().alpha(0.18f).setDuration(120).start();
                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setDimAmount(0.05f);
                    }
                }
            };
            final Runnable setOpaque = new Runnable() {
                @Override
                public void run() {
                    container.animate().alpha(1.0f).setDuration(120).start();
                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setDimAmount(0.4f);
                    }
                }
            };
            LinearLayout bodyRow = new LinearLayout(getContext());
            bodyRow.setOrientation(LinearLayout.HORIZONTAL);
            bodyRow.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            LinearLayout leftCol = new LinearLayout(getContext());
            leftCol.setOrientation(LinearLayout.VERTICAL);
            leftCol.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.25f));
            leftCol.setPadding(0, 0, dp10, 0);
            TextView tvLeftTitle = new TextView(getContext());
            tvLeftTitle.setText("位置与尺寸调节");
            tvLeftTitle.setTextSize(12.5f);
            tvLeftTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvLeftTitle.setTextColor(SiyoXTheme.getAccentBlue());
            tvLeftTitle.setPadding(0, 0, 0, dp6);
            leftCol.addView(tvLeftTitle);
            final TextView tvScaleVal = new TextView(getContext());
            tvScaleVal.setText("缩放大小: " + appSettings.getIslandScale() + "%");
            tvScaleVal.setTextSize(11.5f);
            tvScaleVal.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvScaleVal.setTypeface(Typeface.DEFAULT_BOLD);
            leftCol.addView(tvScaleVal);
            final SeekBar sbScale = new SeekBar(getContext());
            sbScale.setMax(100);
            sbScale.setProgress(appSettings.getIslandScale() - 50);
            sbScale.setSplitTrack(false);
            sbScale.setProgressTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbScale.setThumbTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbScale.setPadding(dp(12), dp(2), dp(12), dp(8));
            sbScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int val = 50 + progress;
                    tvScaleVal.setText("缩放大小: " + val + "%");
                    appSettings.setIslandScale(val);
                    if (dynamicIslandView != null) {
                        dynamicIslandView.applyTransform();
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    setTranslucent.run();
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    setOpaque.run();
                }
            });
            leftCol.addView(sbScale);
            final TextView tvPosXVal = new TextView(getContext());
            tvPosXVal.setText("水平偏移: " + appSettings.getIslandPosX() + " dp");
            tvPosXVal.setTextSize(11.5f);
            tvPosXVal.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvPosXVal.setTypeface(Typeface.DEFAULT_BOLD);
            leftCol.addView(tvPosXVal);
            final SeekBar sbPosX = new SeekBar(getContext());
            sbPosX.setMax(600);
            sbPosX.setProgress(appSettings.getIslandPosX() + 300);
            sbPosX.setSplitTrack(false);
            sbPosX.setProgressTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbPosX.setThumbTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbPosX.setPadding(dp(12), dp(2), dp(12), dp(8));
            sbPosX.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int val = progress - 300;
                    tvPosXVal.setText("水平偏移: " + val + " dp");
                    appSettings.setIslandPosX(val);
                    if (dynamicIslandView != null) {
                        dynamicIslandView.applyTransform();
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    setTranslucent.run();
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    setOpaque.run();
                }
            });
            leftCol.addView(sbPosX);
            final TextView tvPosYVal = new TextView(getContext());
            tvPosYVal.setText("垂直位置: " + appSettings.getIslandPosY() + " dp");
            tvPosYVal.setTextSize(11.5f);
            tvPosYVal.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvPosYVal.setTypeface(Typeface.DEFAULT_BOLD);
            leftCol.addView(tvPosYVal);
            final SeekBar sbPosY = new SeekBar(getContext());
            sbPosY.setMax(300);
            sbPosY.setProgress(appSettings.getIslandPosY());
            sbPosY.setSplitTrack(false);
            sbPosY.setProgressTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbPosY.setThumbTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbPosY.setPadding(dp(12), dp(2), dp(12), dp(8));
            sbPosY.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    tvPosYVal.setText("垂直位置: " + progress + " dp");
                    appSettings.setIslandPosY(progress);
                    if (dynamicIslandView != null) {
                        dynamicIslandView.applyTransform();
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    setTranslucent.run();
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    setOpaque.run();
                }
            });
            leftCol.addView(sbPosY);
            final TextView tvRadiusVal = new TextView(getContext());
            tvRadiusVal.setText("圆角弧度: " + appSettings.getIslandCornerRadius() + " dp");
            tvRadiusVal.setTextSize(11.5f);
            tvRadiusVal.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvRadiusVal.setTypeface(Typeface.DEFAULT_BOLD);
            leftCol.addView(tvRadiusVal);
            final SeekBar sbRadius = new SeekBar(getContext());
            sbRadius.setMax(18);
            sbRadius.setProgress(appSettings.getIslandCornerRadius() - 6);
            sbRadius.setSplitTrack(false);
            sbRadius.setProgressTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbRadius.setThumbTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbRadius.setPadding(dp(12), dp(2), dp(12), dp(6));
            sbRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int val = 6 + progress;
                    tvRadiusVal.setText("圆角弧度: " + val + " dp");
                    appSettings.setIslandCornerRadius(val);
                    if (dynamicIslandView != null) {
                        dynamicIslandView.applyTransform();
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    setTranslucent.run();
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    setOpaque.run();
                }
            });
            leftCol.addView(sbRadius);
            bodyRow.addView(leftCol);
            View vDivider = new View(getContext());
            vDivider.setBackgroundColor(isDark ? Color.parseColor("#2C2C2E") : Color.parseColor("#E5E7EB"));
            LinearLayout.LayoutParams vDivParams = new LinearLayout.LayoutParams(dp(1), LayoutParams.MATCH_PARENT);
            vDivParams.setMargins(dp6, dp6, dp6, dp6);
            vDivider.setLayoutParams(vDivParams);
            bodyRow.addView(vDivider);
            LinearLayout rightCol = new LinearLayout(getContext());
            rightCol.setOrientation(LinearLayout.VERTICAL);
            rightCol.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));
            rightCol.setPadding(dp10, 0, 0, 0);
            TextView tvRightTitle = new TextView(getContext());
            tvRightTitle.setText("功能开关");
            tvRightTitle.setTextSize(12.5f);
            tvRightTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvRightTitle.setTextColor(SiyoXTheme.getAccentBlue());
            tvRightTitle.setPadding(0, 0, 0, dp6);
            rightCol.addView(tvRightTitle);
            final MiuiXSwitch switchTime = new MiuiXSwitch(getContext());
            LinearLayout switchTimeRow = createDialogSwitchRow("系统时间", switchTime, appSettings.isIslandShowTime(), isDark, new MiuiXSwitch.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(MiuiXSwitch switchView, boolean isChecked) {
                    appSettings.setIslandShowTime(isChecked);
                    if (dynamicIslandView != null) {
                        dynamicIslandView.applySettingsConfig();
                    }
                }
            });
            rightCol.addView(switchTimeRow);
            final MiuiXSwitch switchAuthor = new MiuiXSwitch(getContext());
            LinearLayout switchAuthorRow = createDialogSwitchRow("作者信息", switchAuthor, appSettings.isIslandShowAuthor(), isDark, new MiuiXSwitch.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(MiuiXSwitch switchView, boolean isChecked) {
                    appSettings.setIslandShowAuthor(isChecked);
                    if (dynamicIslandView != null) {
                        dynamicIslandView.applySettingsConfig();
                    }
                }
            });
            rightCol.addView(switchAuthorRow);
            final MiuiXSwitch switchProgress = new MiuiXSwitch(getContext());
            LinearLayout switchProgressRow = createDialogSwitchRow("下载进度", switchProgress, appSettings.isIslandShowProgress(), isDark, new MiuiXSwitch.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(MiuiXSwitch switchView, boolean isChecked) {
                    appSettings.setIslandShowProgress(isChecked);
                    if (dynamicIslandView != null) {
                        dynamicIslandView.applySettingsConfig();
                    }
                }
            });
            rightCol.addView(switchProgressRow);

            LinearLayout dotRow = new LinearLayout(getContext());
            dotRow.setOrientation(LinearLayout.HORIZONTAL);
            dotRow.setGravity(Gravity.CENTER_VERTICAL);
            dotRow.setPadding(0, dp(3), 0, dp(3));
            TextView tvDotLabel = new TextView(getContext());
            tvDotLabel.setText("状态圆点");
            tvDotLabel.setTextSize(13f);
            tvDotLabel.setTypeface(Typeface.DEFAULT_BOLD);
            tvDotLabel.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvDotLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            dotRow.addView(tvDotLabel);

            final View dotColorBadge = new View(getContext());
            int badgeSize = dp(20);
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(badgeSize, badgeSize);
            badgeParams.setMargins(0, 0, dp(10), 0);
            dotColorBadge.setLayoutParams(badgeParams);
            final Runnable updateBadgeColor = new Runnable() {
                @Override
                public void run() {
                    GradientDrawable badgeDrawable = new GradientDrawable();
                    badgeDrawable.setShape(GradientDrawable.OVAL);
                    badgeDrawable.setColor(appSettings.getIslandDotColor());
                    badgeDrawable.setStroke(dp(1.5f), isDark ? Color.parseColor("#555558") : Color.parseColor("#D1D5DB"));
                    dotColorBadge.setBackground(badgeDrawable);
                }
            };
            updateBadgeColor.run();
            dotColorBadge.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDotColorPickerDialog(new Runnable() {
                        @Override
                        public void run() {
                            updateBadgeColor.run();
                            if (dynamicIslandView != null) {
                                dynamicIslandView.applySettingsConfig();
                            }
                        }
                    });
                }
            });
            dotRow.addView(dotColorBadge);
            final MiuiXSwitch switchDot = new MiuiXSwitch(getContext());
            switchDot.setChecked(appSettings.isIslandShowDot(), false);
            switchDot.setOnCheckedChangeListener(new MiuiXSwitch.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(MiuiXSwitch switchView, boolean isChecked) {
                    appSettings.setIslandShowDot(isChecked);
                    if (dynamicIslandView != null) {
                        dynamicIslandView.applySettingsConfig();
                    }
                }
            });
            dotRow.addView(switchDot);
            rightCol.addView(dotRow);
            bodyRow.addView(rightCol);
            container.addView(bodyRow);
            View hDivider = createDivider(isDark);
            LinearLayout.LayoutParams hDivParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1));
            hDivParams.setMargins(0, dp10, 0, dp10);
            hDivider.setLayoutParams(hDivParams);
            container.addView(hDivider);
            LinearLayout btnRow = new LinearLayout(getContext());
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(38));
            btnRow.setLayoutParams(rowParams);
            Button btnReset = new Button(getContext());
            btnReset.setText("恢复默认");
            btnReset.setTextSize(13f);
            btnReset.setTypeface(Typeface.DEFAULT_BOLD);
            btnReset.setTextColor(isDark ? Color.parseColor("#E5E5EA") : Color.parseColor("#3C3C43"));
            int resetBg = isDark ? Color.parseColor("#3A3A3C") : Color.parseColor("#E5E7EB");
            int resetPressed = isDark ? Color.parseColor("#2C2C2E") : Color.parseColor("#D1D5DB");
            btnReset.setBackground(createRippleDrawable(resetBg, resetPressed, dp(10)));
            styleCleanButton(btnReset);
            LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
            resetParams.setMargins(0, 0, dp8, 0);
            btnReset.setLayoutParams(resetParams);
            btnReset.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    appSettings.setIslandScale(100);
                    appSettings.setIslandPosX(0);
                    appSettings.setIslandPosY(10);
                    appSettings.setIslandCornerRadius(18);
                    appSettings.setIslandShowTime(true);
                    appSettings.setIslandShowAuthor(true);
                    appSettings.setIslandShowProgress(true);
                    sbScale.setProgress(50);
                    sbPosX.setProgress(300);
                    sbPosY.setProgress(10);
                    sbRadius.setProgress(12);
                    tvScaleVal.setText("缩放大小: 100%");
                    tvPosXVal.setText("水平偏移: 0 dp");
                    tvPosYVal.setText("垂直位置: 10 dp");
                    tvRadiusVal.setText("圆角弧度: 18 dp");
                    switchTime.setChecked(true, true);
                    switchAuthor.setChecked(true, true);
                    switchProgress.setChecked(true, true);
                    if (dynamicIslandView != null) {
                        dynamicIslandView.applyTransform();
                        dynamicIslandView.applySettingsConfig();
                    }
                }
            });
            btnRow.addView(btnReset);
            Button btnDone = new Button(getContext());
            btnDone.setText("完成");
            btnDone.setTextSize(13f);
            btnDone.setTypeface(Typeface.DEFAULT_BOLD);
            btnDone.setTextColor(Color.WHITE);
            btnDone.setBackground(createRippleDrawable(Color.parseColor("#0A84FF"), Color.parseColor("#0066CC"), dp(10)));
            styleCleanButton(btnDone);
            LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
            btnDone.setLayoutParams(doneParams);
            btnDone.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            btnRow.addView(btnDone);
            container.addView(btnRow);
            dialog.setContentView(container);
            dialog.show();
        } catch (Throwable t) {
            SiyoXLogger.e("SiyoX_Overlay", "Error showing dynamic island settings dialog: " + t.getMessage(), t);
        }
    }
    private LinearLayout createDialogSwitchRow(String title, MiuiXSwitch miuiSwitch, boolean initial, boolean isDark, MiuiXSwitch.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));
        TextView tv = new TextView(getContext());
        tv.setText(title);
        tv.setTextSize(13f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tv);
        miuiSwitch.setChecked(initial, false);
        miuiSwitch.setOnCheckedChangeListener(listener);
        row.addView(miuiSwitch);
        return row;
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
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopSimulatedDownload();
        simHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        installRootTouchObserver();
        post(new Runnable() {
            @Override
            public void run() {
                refreshDisplayOverlays();
            }
        });
    }

    private void buildKeyDisplayView() {
        keyDisplayView = new KeyDisplayView(getContext());
        keyDisplayView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        int[] size = getRealScreenSize();

        keyDisplayView.setOverallScale(appSettings.getKeyDisplayScale());
        keyDisplayView.applyLayout(appSettings.getKeyDisplayLayout(), size[0], size[1],
                dp(appSettings.getKeyDisplayPosX()), dp(appSettings.getKeyDisplayPosY()));
        keyDisplayView.setKeyGap(dp(appSettings.getKeyDisplayGap()));
        keyDisplayView.setAlphaLevel(appSettings.getKeyDisplayAlpha());
        keyDisplayView.setVisibility(appSettings.isKeyDisplayEnabled() ? View.VISIBLE : View.GONE);
        addView(keyDisplayView);
    }

    private void buildKeyTriggerView() {
        keyTriggerView = new KeyTriggerView(getContext());
        keyTriggerView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        int[] size = getRealScreenSize();
        keyTriggerView.applyLayout(appSettings.getKeyTriggerLayout(), size[0], size[1]);
        keyTriggerView.setJoystickMode(appSettings.isKeyDisplayJoystickMode());
        keyTriggerView.setTriggerListener(new KeyTriggerView.TriggerListener() {
            @Override
            public void onKeyTriggered(int keyIndex, boolean pressed) {

                if (keyDisplayView != null) keyDisplayView.setKeyPressed(keyIndex, pressed);
            }
        });
        keyTriggerView.setVisibility(appSettings.isKeyDisplayEnabled() ? View.VISIBLE : View.GONE);
        addView(keyTriggerView);
    }

    private void installRootTouchObserver() {
        try {
            if (!(getContext() instanceof android.app.Activity)) return;
            android.app.Activity act = (android.app.Activity) getContext();
            if (act.getWindow() == null) return;
            final View decor = act.getWindow().getDecorView();
            if (decor == null) return;
            decor.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (keyTriggerView != null
                            && appSettings.isKeyDisplayEnabled()
                            && !keyEditMode) {
                        try {
                            keyTriggerView.onGlobalTouch(event.getActionMasked(),
                                    event.getRawX(), event.getRawY());
                        } catch (Throwable ignored) {}
                    }

                    return false;
                }
            });
        } catch (Throwable t) {
            SiyoXLogger.w("SiyoX_OverlayLayout", "installRootTouchObserver: " + t.getMessage());
        }
    }

    private void mountTriggerCatchers() {
        if (keyTriggerView == null) return;
        int[] screen = getRealScreenSize();
        keyTriggerView.applyLayout(appSettings.getKeyTriggerLayout(), screen[0], screen[1]);
        keyTriggerView.setVisibility(appSettings.isKeyDisplayEnabled() ? View.VISIBLE : View.GONE);
    }

    private void setTriggerCatchersVisible(boolean visible) {
        if (keyTriggerView != null) {
            keyTriggerView.setVisibility(visible && appSettings.isKeyDisplayEnabled()
                    ? View.VISIBLE : View.GONE);
        }
    }

    private void fixOverlayTouchable() {
        setFocusable(false);
        setFocusableInTouchMode(false);

        setClickable(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    private void buildFpsDisplayView() {
        fpsDisplayView = new FpsDisplayView(getContext());
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        fpsDisplayView.setLayoutParams(p);

        applyFpsPosition();
        fpsDisplayView.setChinese(appSettings.isFpsDisplayChinese());
        fpsDisplayView.setAlphaLevel(appSettings.getFpsDisplayAlpha());
        fpsDisplayView.setTextSizeSp(appSettings.getFpsDisplayTextSize());
        fpsDisplayView.setVisibility(appSettings.isFpsDisplayEnabled() ? View.VISIBLE : View.GONE);
        addView(fpsDisplayView);
    }

    private void refreshDisplayOverlays() {

        final boolean overlaysAllowed = !isPanelOpen;
        if (keyDisplayView != null) {
            int[] size = getRealScreenSize();
            keyDisplayView.setOverallScale(appSettings.getKeyDisplayScale());
            keyDisplayView.applyLayout(appSettings.getKeyDisplayLayout(), size[0], size[1],
                    dp(appSettings.getKeyDisplayPosX()), dp(appSettings.getKeyDisplayPosY()));
            keyDisplayView.setKeyGap(dp(appSettings.getKeyDisplayGap()));
            keyDisplayView.setAlphaLevel(appSettings.getKeyDisplayAlpha());
            keyDisplayView.setVisibility(overlaysAllowed && appSettings.isKeyDisplayEnabled() ? View.VISIBLE : View.GONE);
            if (keyDisplayView.getVisibility() == View.VISIBLE) keyDisplayView.bringToFront();
        }
        if (keyTriggerView != null) {
            int[] size = getRealScreenSize();
            keyTriggerView.applyLayout(appSettings.getKeyTriggerLayout(), size[0], size[1]);
            keyTriggerView.setJoystickMode(appSettings.isKeyDisplayJoystickMode());
            keyTriggerView.setVisibility(appSettings.isKeyDisplayEnabled() ? View.VISIBLE : View.GONE);
            mountTriggerCatchers();
        }
        if (fpsDisplayView != null) {
            fpsDisplayView.setChinese(appSettings.isFpsDisplayChinese());
            fpsDisplayView.setAlphaLevel(appSettings.getFpsDisplayAlpha());
            fpsDisplayView.setTextSizeSp(appSettings.getFpsDisplayTextSize());
            fpsDisplayView.setVisibility(overlaysAllowed && appSettings.isFpsDisplayEnabled() ? View.VISIBLE : View.GONE);
            applyFpsPosition();
            if (overlaysAllowed && appSettings.isFpsDisplayEnabled()) {
                fpsDisplayView.start();
                fpsDisplayView.bringToFront();
            } else {
                fpsDisplayView.stop();
            }
        }
    }

    private void showKeyDisplaySettingsDialog(final boolean isDark) {
        try {
            final Dialog dialog = new Dialog(getContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setDimAmount(0.4f);
            }
            int dp16 = dp(16);
            int dp12 = dp(12);
            int dp10 = dp(10);
            int dp8 = dp(8);
            int dp6 = dp(6);
            final LinearLayout container = new LinearLayout(getContext());
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(dp16, dp16, dp16, dp16);
            container.setBackground(createCardBg(SiyoXTheme.getCardBg(isDark), Color.TRANSPARENT, dp(18)));

            int maxW = Math.min(getRealScreenSize()[0] - dp(32), dp(540));
            LinearLayout.LayoutParams cParams = new LinearLayout.LayoutParams(maxW, LayoutParams.WRAP_CONTENT);
            container.setLayoutParams(cParams);
            TextView tvTitle = new TextView(getContext());
            tvTitle.setText("按键显示设置");
            tvTitle.setTextSize(16.5f);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvTitle.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvTitle.setGravity(Gravity.CENTER_HORIZONTAL);
            tvTitle.setPadding(0, 0, 0, dp12);
            container.addView(tvTitle);

            final Runnable setTranslucent = new Runnable() {
                @Override
                public void run() {
                    container.animate().alpha(0.25f).setDuration(120).start();
                    if (dialog.getWindow() != null) dialog.getWindow().setDimAmount(0.05f);
                }
            };
            final Runnable setOpaque = new Runnable() {
                @Override
                public void run() {
                    container.animate().alpha(1.0f).setDuration(120).start();
                    if (dialog.getWindow() != null) dialog.getWindow().setDimAmount(0.4f);
                }
            };
            LinearLayout bodyRow = new LinearLayout(getContext());
            bodyRow.setOrientation(LinearLayout.HORIZONTAL);
            bodyRow.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            LinearLayout leftCol = new LinearLayout(getContext());
            leftCol.setOrientation(LinearLayout.VERTICAL);
            leftCol.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.15f));
            leftCol.setPadding(0, 0, dp10, 0);
            TextView tvLeftTitle = new TextView(getContext());
            tvLeftTitle.setText("位置与尺寸调节");
            tvLeftTitle.setTextSize(12.5f);
            tvLeftTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvLeftTitle.setTextColor(SiyoXTheme.getAccentBlue());
            tvLeftTitle.setPadding(0, 0, 0, dp6);
            leftCol.addView(tvLeftTitle);
            final TextView tvScaleVal = new TextView(getContext());
            tvScaleVal.setText("按键大小: " + Math.round(appSettings.getKeyDisplayScale() * 100) + "%");
            tvScaleVal.setTextSize(11.5f);
            tvScaleVal.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvScaleVal.setTypeface(Typeface.DEFAULT_BOLD);
            leftCol.addView(tvScaleVal);
            final SeekBar sbScale = new SeekBar(getContext());
            sbScale.setMax(250);
            sbScale.setProgress((int) (appSettings.getKeyDisplayScale() * 100) - 50);
            sbScale.setSplitTrack(false);
            sbScale.setProgressTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbScale.setThumbTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbScale.setPadding(dp(12), dp(2), dp(12), dp8);
            sbScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float scale = (progress + 50) / 100f;
                    tvScaleVal.setText("按键大小: " + Math.round(scale * 100) + "%");
                    appSettings.setKeyDisplayScale(scale);
                    refreshDisplayOverlays();
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) { setTranslucent.run(); }
                @Override public void onStopTrackingTouch(SeekBar seekBar) { setOpaque.run(); }
            });
            leftCol.addView(sbScale);
            final int[] keyPosBuf = new int[]{appSettings.getKeyDisplayPosX(), appSettings.getKeyDisplayPosY()};

            final int maxOffsetX = 300;
            final int maxOffsetY = 300;
            final TextView tvPosXVal = new TextView(getContext());
            tvPosXVal.setText("水平偏移: " + keyPosBuf[0] + " dp");
            tvPosXVal.setTextSize(11.5f);
            tvPosXVal.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvPosXVal.setTypeface(Typeface.DEFAULT_BOLD);
            leftCol.addView(tvPosXVal);
            final SeekBar sbPosX = new SeekBar(getContext());
            sbPosX.setMax(maxOffsetX * 2);
            sbPosX.setProgress(keyPosBuf[0] + maxOffsetX);
            sbPosX.setSplitTrack(false);
            sbPosX.setProgressTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbPosX.setThumbTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbPosX.setPadding(dp(12), dp(2), dp(12), dp8);
            sbPosX.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int val = progress - maxOffsetX;
                    tvPosXVal.setText("水平偏移: " + val + " dp");
                    keyPosBuf[0] = val;
                    appSettings.setKeyDisplayPos(keyPosBuf[0], keyPosBuf[1]);
                    refreshDisplayOverlays();
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) { setTranslucent.run(); }
                @Override public void onStopTrackingTouch(SeekBar seekBar) { setOpaque.run(); }
            });
            leftCol.addView(sbPosX);
            final TextView tvPosYVal = new TextView(getContext());
            tvPosYVal.setText("垂直位置: " + keyPosBuf[1] + " dp");
            tvPosYVal.setTextSize(11.5f);
            tvPosYVal.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvPosYVal.setTypeface(Typeface.DEFAULT_BOLD);
            leftCol.addView(tvPosYVal);
            final SeekBar sbPosY = new SeekBar(getContext());
            sbPosY.setMax(maxOffsetY * 2);
            sbPosY.setProgress(keyPosBuf[1] + maxOffsetY);
            sbPosY.setSplitTrack(false);
            sbPosY.setProgressTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbPosY.setThumbTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbPosY.setPadding(dp(12), dp(2), dp(12), dp8);
            sbPosY.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int val = progress - maxOffsetY;
                    tvPosYVal.setText("垂直位置: " + val + " dp");
                    keyPosBuf[1] = val;
                    appSettings.setKeyDisplayPos(keyPosBuf[0], keyPosBuf[1]);
                    refreshDisplayOverlays();
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) { setTranslucent.run(); }
                @Override public void onStopTrackingTouch(SeekBar seekBar) { setOpaque.run(); }
            });
            leftCol.addView(sbPosY);
            final TextView tvAlphaVal = new TextView(getContext());
            tvAlphaVal.setText("透明度: " + Math.round(appSettings.getKeyDisplayAlpha() * 100) + "%");
            tvAlphaVal.setTextSize(11.5f);
            tvAlphaVal.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvAlphaVal.setTypeface(Typeface.DEFAULT_BOLD);
            leftCol.addView(tvAlphaVal);
            final SeekBar sbAlpha = new SeekBar(getContext());
            sbAlpha.setMax(100);
            sbAlpha.setProgress((int) (appSettings.getKeyDisplayAlpha() * 100));
            sbAlpha.setSplitTrack(false);
            sbAlpha.setProgressTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbAlpha.setThumbTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbAlpha.setPadding(dp(12), dp(2), dp(12), dp6);
            sbAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float a = progress / 100f;
                    tvAlphaVal.setText("透明度: " + Math.round(a * 100) + "%");
                    appSettings.setKeyDisplayAlpha(a);
                    refreshDisplayOverlays();
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) { setTranslucent.run(); }
                @Override public void onStopTrackingTouch(SeekBar seekBar) { setOpaque.run(); }
            });
            leftCol.addView(sbAlpha);
            final TextView tvGapVal = new TextView(getContext());
            tvGapVal.setText("按键间距: " + Math.round(appSettings.getKeyDisplayGap()) + "px");
            tvGapVal.setTextSize(11.5f);
            tvGapVal.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvGapVal.setTypeface(Typeface.DEFAULT_BOLD);
            leftCol.addView(tvGapVal);
            final SeekBar sbGap = new SeekBar(getContext());
            sbGap.setMax(20);
            sbGap.setProgress((int) appSettings.getKeyDisplayGap());
            sbGap.setSplitTrack(false);
            sbGap.setProgressTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbGap.setThumbTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbGap.setPadding(dp(12), dp(2), dp(12), dp(6));
            sbGap.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    tvGapVal.setText("按键间距: " + progress + "px");
                    appSettings.setKeyDisplayGap(progress);
                    refreshDisplayOverlays();
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) { setTranslucent.run(); }
                @Override public void onStopTrackingTouch(SeekBar seekBar) { setOpaque.run(); }
            });
            leftCol.addView(sbGap);
            bodyRow.addView(leftCol);
            View vDivider = new View(getContext());
            vDivider.setBackgroundColor(isDark ? Color.parseColor("#2C2C2E") : Color.parseColor("#E5E7EB"));
            LinearLayout.LayoutParams vDivParams = new LinearLayout.LayoutParams(dp(1), LayoutParams.MATCH_PARENT);
            vDivParams.setMargins(dp6, dp6, dp6, dp6);
            vDivider.setLayoutParams(vDivParams);
            bodyRow.addView(vDivider);
            LinearLayout rightCol = new LinearLayout(getContext());
            rightCol.setOrientation(LinearLayout.VERTICAL);
            rightCol.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));
            rightCol.setPadding(dp10, 0, 0, 0);
            TextView tvRightTitle = new TextView(getContext());
            tvRightTitle.setText("触发区域");
            tvRightTitle.setTextSize(12.5f);
            tvRightTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvRightTitle.setTextColor(SiyoXTheme.getAccentBlue());
            tvRightTitle.setPadding(0, 0, 0, dp6);
            rightCol.addView(tvRightTitle);

            Button btnCustomTrigger = new Button(getContext());
            btnCustomTrigger.setText("自定义触发区域");
            btnCustomTrigger.setTextSize(12.5f);
            btnCustomTrigger.setTypeface(Typeface.DEFAULT_BOLD);
            btnCustomTrigger.setTextColor(Color.WHITE);
            btnCustomTrigger.setBackground(createRippleDrawable(Color.parseColor("#0A84FF"), Color.parseColor("#0066CC"), dp(10)));
            styleCleanButton(btnCustomTrigger);
            LinearLayout.LayoutParams ctp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(38));
            btnCustomTrigger.setLayoutParams(ctp);
            btnCustomTrigger.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    enterKeyEditMode();
                }
            });
            rightCol.addView(btnCustomTrigger);
            bodyRow.addView(rightCol);
            container.addView(bodyRow);
            container.addView(createDivider(isDark));

            LinearLayout btnRow = new LinearLayout(getContext());
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setGravity(Gravity.CENTER_VERTICAL);
            btnRow.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(42)));
            Button btnReset = new Button(getContext());
            btnReset.setText("恢复默认");
            btnReset.setTextSize(13f);
            btnReset.setTypeface(Typeface.DEFAULT_BOLD);
            btnReset.setTextColor(isDark ? Color.parseColor("#E5E5EA") : Color.parseColor("#3C3C43"));
            btnReset.setBackground(createRippleDrawable(isDark ? Color.parseColor("#3A3A3C") : Color.parseColor("#E5E7EB"), isDark ? Color.parseColor("#2C2C2E") : Color.parseColor("#D1D5DB"), dp(10)));
            styleCleanButton(btnReset);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
            rp.setMargins(0, 0, dp8, 0);
            btnReset.setLayoutParams(rp);
            btnReset.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetKeyDisplayDefaults();
                }
            });
            btnRow.addView(btnReset);
            Button btnConfirm = new Button(getContext());
            btnConfirm.setText("确认");
            btnConfirm.setTextSize(13f);
            btnConfirm.setTypeface(Typeface.DEFAULT_BOLD);
            btnConfirm.setTextColor(Color.WHITE);
            btnConfirm.setBackground(createRippleDrawable(Color.parseColor("#0A84FF"), Color.parseColor("#0066CC"), dp(10)));
            styleCleanButton(btnConfirm);
            btnConfirm.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
            btnConfirm.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            btnRow.addView(btnConfirm);
            container.addView(btnRow);
            dialog.setContentView(container);
            dialog.show();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(maxW, WindowManager.LayoutParams.WRAP_CONTENT);
            }
        } catch (Throwable t) {
            SiyoXLogger.w("SiyoX_OverlayLayout", "Show key display settings exception: " + t.getMessage());
        }
    }

    private View makeSwitchRow(String label, boolean initial, final boolean isDark, MiuiXSwitch.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(5), 0, dp(5));
        TextView tv = new TextView(getContext());
        tv.setText(label);
        tv.setTextSize(12.5f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tv);
        MiuiXSwitch sw = new MiuiXSwitch(getContext());
        sw.setChecked(initial, false);
        sw.setOnCheckedChangeListener(listener);
        row.addView(sw);
        return row;
    }

    private void showFpsDisplaySettingsDialog(final boolean isDark) {
        try {
            final Dialog dialog = new Dialog(getContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setDimAmount(0.4f);
            }
            int dp16 = dp(16);
            int dp12 = dp(12);
            int dp10 = dp(10);
            int dp8 = dp(8);
            int dp6 = dp(6);
            final LinearLayout container = new LinearLayout(getContext());
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(dp16, dp16, dp16, dp16);
            container.setBackground(createCardBg(SiyoXTheme.getCardBg(isDark), Color.TRANSPARENT, dp(18)));
            int maxW = Math.min(getRealScreenSize()[0] - dp(32), dp(540));
            container.setLayoutParams(new LinearLayout.LayoutParams(maxW, LayoutParams.WRAP_CONTENT));
            TextView tvTitle = new TextView(getContext());
            tvTitle.setText("帧率显示设置");
            tvTitle.setTextSize(16.5f);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvTitle.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvTitle.setGravity(Gravity.CENTER_HORIZONTAL);
            tvTitle.setPadding(0, 0, 0, dp12);
            container.addView(tvTitle);
            final Runnable setTranslucent = new Runnable() {
                @Override
                public void run() {
                    container.animate().alpha(0.25f).setDuration(120).start();
                    if (dialog.getWindow() != null) dialog.getWindow().setDimAmount(0.05f);
                }
            };
            final Runnable setOpaque = new Runnable() {
                @Override
                public void run() {
                    container.animate().alpha(1.0f).setDuration(120).start();
                    if (dialog.getWindow() != null) dialog.getWindow().setDimAmount(0.4f);
                }
            };
            LinearLayout bodyRow = new LinearLayout(getContext());
            bodyRow.setOrientation(LinearLayout.HORIZONTAL);
            bodyRow.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            LinearLayout leftCol = new LinearLayout(getContext());
            leftCol.setOrientation(LinearLayout.VERTICAL);
            leftCol.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.15f));
            leftCol.setPadding(0, 0, dp10, 0);
            TextView tvLeftTitle = new TextView(getContext());
            tvLeftTitle.setText("位置与尺寸调节");
            tvLeftTitle.setTextSize(12.5f);
            tvLeftTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvLeftTitle.setTextColor(SiyoXTheme.getAccentBlue());
            tvLeftTitle.setPadding(0, 0, 0, dp6);
            leftCol.addView(tvLeftTitle);
            final TextView tvSizeVal = new TextView(getContext());
            tvSizeVal.setText("文字大小: " + Math.round(appSettings.getFpsDisplayTextSize()) + "sp");
            tvSizeVal.setTextSize(11.5f);
            tvSizeVal.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvSizeVal.setTypeface(Typeface.DEFAULT_BOLD);
            leftCol.addView(tvSizeVal);
            final SeekBar sbSize = new SeekBar(getContext());
            sbSize.setMax(40);
            sbSize.setProgress((int) appSettings.getFpsDisplayTextSize() - 8);
            sbSize.setSplitTrack(false);
            sbSize.setProgressTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbSize.setThumbTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbSize.setPadding(dp(12), dp(2), dp(12), dp8);
            sbSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float sz = progress + 8;
                    tvSizeVal.setText("文字大小: " + Math.round(sz) + "sp");
                    appSettings.setFpsDisplayTextSize(sz);
                    refreshDisplayOverlays();
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) { setTranslucent.run(); }
                @Override public void onStopTrackingTouch(SeekBar seekBar) { setOpaque.run(); }
            });
            leftCol.addView(sbSize);
            final int[] fpsPos = new int[]{
                    appSettings.getFpsDisplayPosX(), appSettings.getFpsDisplayPosY()};

            final int fpsMaxX = 600;
            final int fpsMaxY = 300;
            final TextView tvPosXVal = new TextView(getContext());
            tvPosXVal.setText("水平偏移: " + (fpsPos[0] - 300) + " dp");
            tvPosXVal.setTextSize(11.5f);
            tvPosXVal.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvPosXVal.setTypeface(Typeface.DEFAULT_BOLD);
            leftCol.addView(tvPosXVal);
            final SeekBar sbPosX = new SeekBar(getContext());
            sbPosX.setMax(fpsMaxX);
            sbPosX.setProgress(Math.max(0, Math.min(fpsMaxX, fpsPos[0] + 300)));
            sbPosX.setSplitTrack(false);
            sbPosX.setProgressTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbPosX.setThumbTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbPosX.setPadding(dp(12), dp(2), dp(12), dp8);
            sbPosX.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    tvPosXVal.setText("水平偏移: " + (progress - 300) + " dp");
                    fpsPos[0] = progress - 300;
                    appSettings.setFpsDisplayPos(fpsPos[0], fpsPos[1]);
                    applyFpsPosition();
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) { setTranslucent.run(); }
                @Override public void onStopTrackingTouch(SeekBar seekBar) { setOpaque.run(); }
            });
            leftCol.addView(sbPosX);
            final TextView tvPosYVal = new TextView(getContext());
            tvPosYVal.setText("垂直位置: " + fpsPos[1] + " dp");
            tvPosYVal.setTextSize(11.5f);
            tvPosYVal.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvPosYVal.setTypeface(Typeface.DEFAULT_BOLD);
            leftCol.addView(tvPosYVal);
            final SeekBar sbPosY = new SeekBar(getContext());
            sbPosY.setMax(fpsMaxY);
            sbPosY.setProgress(Math.max(0, Math.min(fpsMaxY, fpsPos[1])));
            sbPosY.setSplitTrack(false);
            sbPosY.setProgressTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbPosY.setThumbTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbPosY.setPadding(dp(12), dp(2), dp(12), dp8);
            sbPosY.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    tvPosYVal.setText("垂直位置: " + progress + " dp");
                    fpsPos[1] = progress;
                    appSettings.setFpsDisplayPos(fpsPos[0], fpsPos[1]);
                    applyFpsPosition();
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) { setTranslucent.run(); }
                @Override public void onStopTrackingTouch(SeekBar seekBar) { setOpaque.run(); }
            });
            leftCol.addView(sbPosY);
            final TextView tvAlphaVal = new TextView(getContext());
            tvAlphaVal.setText("透明度: " + Math.round(appSettings.getFpsDisplayAlpha() * 100) + "%");
            tvAlphaVal.setTextSize(11.5f);
            tvAlphaVal.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvAlphaVal.setTypeface(Typeface.DEFAULT_BOLD);
            leftCol.addView(tvAlphaVal);
            final SeekBar sbAlpha = new SeekBar(getContext());
            sbAlpha.setMax(100);
            sbAlpha.setProgress((int) (appSettings.getFpsDisplayAlpha() * 100));
            sbAlpha.setSplitTrack(false);
            sbAlpha.setProgressTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbAlpha.setThumbTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
            sbAlpha.setPadding(dp(12), dp(2), dp(12), dp6);
            sbAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float a = progress / 100f;
                    tvAlphaVal.setText("透明度: " + Math.round(a * 100) + "%");
                    appSettings.setFpsDisplayAlpha(a);
                    refreshDisplayOverlays();
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) { setTranslucent.run(); }
                @Override public void onStopTrackingTouch(SeekBar seekBar) { setOpaque.run(); }
            });
            leftCol.addView(sbAlpha);
            bodyRow.addView(leftCol);
            View vDivider = new View(getContext());
            vDivider.setBackgroundColor(isDark ? Color.parseColor("#2C2C2E") : Color.parseColor("#E5E7EB"));
            LinearLayout.LayoutParams vDivParams = new LinearLayout.LayoutParams(dp(1), LayoutParams.MATCH_PARENT);
            vDivParams.setMargins(dp6, dp6, dp6, dp6);
            vDivider.setLayoutParams(vDivParams);
            bodyRow.addView(vDivider);
            LinearLayout rightCol = new LinearLayout(getContext());
            rightCol.setOrientation(LinearLayout.VERTICAL);
            rightCol.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));
            rightCol.setPadding(dp10, 0, 0, 0);
            TextView tvRightTitle = new TextView(getContext());
            tvRightTitle.setText("语言");
            tvRightTitle.setTextSize(12.5f);
            tvRightTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvRightTitle.setTextColor(SiyoXTheme.getAccentBlue());
            tvRightTitle.setPadding(0, 0, 0, dp6);
            rightCol.addView(tvRightTitle);
            rightCol.addView(makeSwitchRow("中文显示", appSettings.isFpsDisplayChinese(), isDark, new MiuiXSwitch.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(MiuiXSwitch v, boolean checked) {
                    appSettings.setFpsDisplayChinese(checked);
                    refreshDisplayOverlays();
                }
            }));
            bodyRow.addView(rightCol);
            container.addView(bodyRow);
            container.addView(createDivider(isDark));
            LinearLayout btnRow = new LinearLayout(getContext());
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setGravity(Gravity.CENTER_VERTICAL);
            btnRow.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(42)));
            Button btnReset = new Button(getContext());
            btnReset.setText("恢复默认");
            btnReset.setTextSize(13f);
            btnReset.setTypeface(Typeface.DEFAULT_BOLD);
            btnReset.setTextColor(isDark ? Color.parseColor("#E5E5EA") : Color.parseColor("#3C3C43"));
            btnReset.setBackground(createRippleDrawable(isDark ? Color.parseColor("#3A3A3C") : Color.parseColor("#E5E7EB"), isDark ? Color.parseColor("#2C2C2E") : Color.parseColor("#D1D5DB"), dp(10)));
            styleCleanButton(btnReset);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
            rp.setMargins(0, 0, dp8, 0);
            btnReset.setLayoutParams(rp);
            btnReset.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetFpsDisplayDefaults();
                }
            });
            btnRow.addView(btnReset);
            Button btnConfirm = new Button(getContext());
            btnConfirm.setText("确认");
            btnConfirm.setTextSize(13f);
            btnConfirm.setTypeface(Typeface.DEFAULT_BOLD);
            btnConfirm.setTextColor(Color.WHITE);
            btnConfirm.setBackground(createRippleDrawable(Color.parseColor("#0A84FF"), Color.parseColor("#0066CC"), dp(10)));
            styleCleanButton(btnConfirm);
            btnConfirm.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
            btnConfirm.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            btnRow.addView(btnConfirm);
            container.addView(btnRow);
            dialog.setContentView(container);
            dialog.show();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(maxW, WindowManager.LayoutParams.WRAP_CONTENT);
            }
        } catch (Throwable t) {
            SiyoXLogger.w("SiyoX_OverlayLayout", "Show fps display settings exception: " + t.getMessage());
        }
    }

    private void applyFpsPosition() {
        if (fpsDisplayView == null) return;
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) fpsDisplayView.getLayoutParams();
        if (p == null) return;

        int screenW = getRealScreenSize()[0];
        p.leftMargin = Math.max(0, screenW / 2 + appSettings.getFpsDisplayPosX());
        p.topMargin = Math.max(0, appSettings.getFpsDisplayPosY());
        fpsDisplayView.setLayoutParams(p);
    }

    private void enterKeyEditMode() {
        if (keyEditMode) return;
        keyEditMode = true;
        editingKeyIndex = -1;
        final boolean isDark = SiyoXTheme.isDarkMode(getContext());
        int[] size = getRealScreenSize();
        int screenW = size[0];
        int screenH = size[1];

        if (panelContainer != null) panelContainer.setVisibility(View.GONE);
        if (floatingBall != null) floatingBall.setVisibility(View.GONE);

        setTriggerCatchersVisible(false);

        if (keyDisplayView != null) {
            keyDisplayView.setVisibility(View.VISIBLE);
            keyDisplayView.clearPressed();
            keyDisplayView.bringToFront();
        }

        keyEditOverlay = new FrameLayout(getContext());
        keyEditOverlay.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        keyEditOverlay.setBackgroundColor(Color.parseColor("#66000000"));

        keyEditOverlay.setClickable(true);
        keyEditOverlay.setFocusable(true);
        keyEditOverlay.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                return true;
            }
        });

        LinearLayout longCard = new LinearLayout(getContext());
        longCard.setOrientation(LinearLayout.VERTICAL);
        longCard.setPadding(dp(16), dp(12), dp(16), dp(12));
        longCard.setBackground(createCardBg(SiyoXTheme.getCardBg(isDark), Color.TRANSPARENT, dp(16)));

        int cardW = (int) (screenW * 0.34f);
        int cardH = (int) (cardW * 9f / 16f);
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(cardW, cardH, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        cardParams.topMargin = (int) (screenH * 0.08f);
        longCard.setLayoutParams(cardParams);

        final TextView tvSelected = new TextView(getContext());
        tvSelected.setTextSize(11.5f);
        tvSelected.setGravity(Gravity.CENTER_HORIZONTAL);
        tvSelected.setPadding(0, dp(4), 0, dp(4));
        longCard.addView(tvSelected);

        final LinearLayout sepRow = new LinearLayout(getContext());
        sepRow.setOrientation(LinearLayout.HORIZONTAL);
        sepRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView tvSep = new TextView(getContext());
        tvSep.setText("单独调整按键大小");
        tvSep.setTextSize(12.5f);
        tvSep.setTypeface(Typeface.DEFAULT_BOLD);
        tvSep.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        tvSep.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        sepRow.addView(tvSep);
        final MiuiXSwitch swSep = new MiuiXSwitch(getContext());
        swSep.setChecked(appSettings.isKeyEditSeparateSize(), false);
        sepRow.addView(swSep);
        longCard.addView(sepRow);

        final TextView tvSizeVal = new TextView(getContext());
        tvSizeVal.setTextSize(11.5f);
        tvSizeVal.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        tvSizeVal.setTypeface(Typeface.DEFAULT_BOLD);
        tvSizeVal.setPadding(0, dp(6), 0, 0);
        longCard.addView(tvSizeVal);
        final SeekBar sbSize = new SeekBar(getContext());
        sbSize.setMax(150);
        sbSize.setSplitTrack(false);
        sbSize.setProgressTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
        sbSize.setThumbTintList(ColorStateList.valueOf(SiyoXTheme.getAccentBlue()));
        sbSize.setPadding(dp(12), dp(2), dp(12), dp(4));
        longCard.addView(sbSize);

        final float baseTrigSize = keyTriggerView == null ? dp(56) : keyTriggerView.getDefaultTriggerSize();
        final Runnable[] syncSizeUiHolder = new Runnable[1];
        final Runnable[] syncNameUiHolder = new Runnable[1];

        syncNameUiHolder[0] = new Runnable() {
            @Override
            public void run() {
                if (editingKeyIndex >= 0) {
                    tvSelected.setText("当前选中：按键 " + KeyDisplayView.KEYS[editingKeyIndex] + "（拖动下方对应方块调整）");
                    tvSelected.setTextColor(SiyoXTheme.getAccentBlue());
                } else if (swSep.isChecked()) {
                    tvSelected.setText("请先点选一个按键方块，再调整大小");
                    tvSelected.setTextColor(SiyoXTheme.getTextSecondary(isDark));
                } else {
                    tvSelected.setText("整体调节：滑条将统一修改四个按键");
                    tvSelected.setTextColor(SiyoXTheme.getTextSecondary(isDark));
                }
            }
        };

        syncSizeUiHolder[0] = new Runnable() {
            @Override
            public void run() {
                float cur;
                if (appSettings.isKeyEditSeparateSize() && editingKeyIndex >= 0 && keyTriggerView != null) {
                    RectF r = keyTriggerView.getTriggerRect(editingKeyIndex);
                    cur = r == null ? baseTrigSize : r.width();
                } else if (keyTriggerView != null) {
                    RectF r = keyTriggerView.getTriggerRect(0);
                    cur = r == null ? baseTrigSize : r.width();
                } else {
                    cur = baseTrigSize;
                }
                sbSize.setProgress(Math.max(0, Math.min(150, (int) (cur - baseTrigSize) + 50)));
                tvSizeVal.setText("按键大小: " + Math.round(cur / getResources().getDisplayMetrics().density) + "dp");
                if (syncNameUiHolder[0] != null) syncNameUiHolder[0].run();
            }
        };
        sbSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                float newSize = baseTrigSize + (progress - 50);
                newSize = Math.max(dp(24), newSize);
                tvSizeVal.setText("按键大小: " + Math.round(newSize / getResources().getDisplayMetrics().density) + "dp");
                applyTriggerSize(newSize, appSettings.isKeyEditSeparateSize(), editingKeyIndex);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        swSep.setOnCheckedChangeListener(new MiuiXSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(MiuiXSwitch v, boolean checked) {
                appSettings.setKeyEditSeparateSize(checked);
                if (syncSizeUiHolder[0] != null) syncSizeUiHolder[0].run();
                refreshKeyHandleLabels();
            }
        });

        Button btnDone = new Button(getContext());
        btnDone.setText("完成");
        btnDone.setTextSize(13.5f);
        btnDone.setTypeface(Typeface.DEFAULT_BOLD);
        btnDone.setTextColor(Color.WHITE);
        btnDone.setBackground(createRippleDrawable(Color.parseColor("#0A84FF"), Color.parseColor("#0066CC"), dp(10)));
        styleCleanButton(btnDone);
        LinearLayout.LayoutParams doneP = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(38));
        doneP.setMargins(0, dp(6), 0, 0);
        btnDone.setLayoutParams(doneP);
        btnDone.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                exitKeyEditMode();
            }
        });
        longCard.addView(btnDone);
        keyEditOverlay.addView(longCard);
        addView(keyEditOverlay);
        keyEditOverlay.bringToFront();

        mountKeyDragHandles(syncSizeUiHolder);
        if (syncSizeUiHolder[0] != null) syncSizeUiHolder[0].run();
    }

    private void applyTriggerSize(float newSize, boolean separate, int index) {
        if (keyTriggerView == null) return;
        try {
            JSONObject layout = readTriggerLayout();
            int[] size = getRealScreenSize();
            keyTriggerView.applyLayout(layout, size[0], size[1]);
            for (int i = 0; i < KeyDisplayView.KEYS.length; i++) {
                if (separate && index >= 0 && i != index) continue;
                RectF r = keyTriggerView.getTriggerRect(i);
                if (r == null) continue;
                float cx = r.centerX();
                float cy = r.centerY();
                JSONObject one = new JSONObject();
                one.put("x", (double) cx);
                one.put("y", (double) cy);
                one.put("size", (double) newSize);
                layout.put(KeyDisplayView.KEYS[i], one);
            }
            appSettings.setKeyTriggerLayout(layout);
            keyTriggerView.applyLayout(layout, size[0], size[1]);
            repositionKeyHandles();
        } catch (Throwable t) {
            SiyoXLogger.w("SiyoX_OverlayLayout", "applyTriggerSize: " + t.getMessage());
        }
    }

    private JSONObject readTriggerLayout() {
        try {
            JSONObject o = appSettings.getKeyTriggerLayout();
            return o == null ? new JSONObject() : o;
        } catch (Throwable t) {
            return new JSONObject();
        }
    }

    private void refreshKeyHandleLabels() {
        if (keyEditOverlay == null) return;
        for (int i = 0; i < KeyDisplayView.KEYS.length; i++) {
            View v = keyEditOverlay.findViewWithTag("keyhandle_" + i);
            if (v instanceof TextView) {
                boolean sel = editingKeyIndex == i;

                ((TextView) v).setText(KeyDisplayView.KEYS[i]);
                v.setBackground(createCardBg(
                        sel ? Color.parseColor("#CC0A84FF") : Color.parseColor("#66000000"),
                        sel ? Color.parseColor("#FF0A84FF") : Color.parseColor("#66FFFFFF"),
                        dp(10)));
            }
        }
    }

    private void repositionKeyHandles() {
        if (keyEditOverlay == null || keyTriggerView == null) return;
        for (int i = 0; i < KeyDisplayView.KEYS.length; i++) {
            View v = keyEditOverlay.findViewWithTag("keyhandle_" + i);
            RectF r = keyTriggerView.getTriggerRect(i);
            if (v == null || r == null) continue;
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
            lp.width = (int) r.width();
            lp.height = (int) r.height();
            lp.leftMargin = (int) r.left;
            lp.topMargin = (int) r.top;
            v.setLayoutParams(lp);
        }
    }

    private void mountKeyDragHandles(final Runnable[] syncSizeUi) {
        if (keyEditOverlay == null || keyTriggerView == null) return;
        final float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < KeyDisplayView.KEYS.length; i++) {
            final int idx = i;
            RectF r = keyTriggerView.getTriggerRect(i);
            if (r == null) continue;
            final TextView handle = new TextView(getContext());
            handle.setTag("keyhandle_" + i);
            handle.setGravity(Gravity.CENTER);
            handle.setText(KeyDisplayView.KEYS[i]);
            handle.setTextSize(15f);
            handle.setTypeface(Typeface.DEFAULT_BOLD);
            handle.setTextColor(Color.WHITE);
            handle.setBackground(createCardBg(Color.parseColor("#66000000"), Color.parseColor("#66FFFFFF"), dp(10)));
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams((int) r.width(), (int) r.height());
            lp.leftMargin = (int) r.left;
            lp.topMargin = (int) r.top;
            handle.setLayoutParams(lp);
            handle.setOnTouchListener(new OnTouchListener() {
                private float downRawX = 0f, downRawY = 0f;
                private int startLeft = 0, startTop = 0;
                private boolean dragged = false;
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            downRawX = event.getRawX();
                            downRawY = event.getRawY();
                            dragged = false;
                            FrameLayout.LayoutParams p0 = (FrameLayout.LayoutParams) v.getLayoutParams();
                            startLeft = p0.leftMargin;
                            startTop = p0.topMargin;
                            return true;
                        case MotionEvent.ACTION_MOVE: {
                            float dx = event.getRawX() - downRawX;
                            float dy = event.getRawY() - downRawY;
                            if (Math.abs(dx) > dp(4) || Math.abs(dy) > dp(4)) dragged = true;
                            FrameLayout.LayoutParams p1 = (FrameLayout.LayoutParams) v.getLayoutParams();
                            p1.leftMargin = startLeft + (int) dx;
                            p1.topMargin = startTop + (int) dy;
                            v.setLayoutParams(p1);
                            return true;
                        }
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL: {
                            FrameLayout.LayoutParams p2 = (FrameLayout.LayoutParams) v.getLayoutParams();
                            float cx = p2.leftMargin + p2.width / 2f;
                            float cy = p2.topMargin + p2.height / 2f;
                            saveTriggerPosition(idx, cx, cy, p2.width);
                            if (!dragged) {

                                editingKeyIndex = idx;
                                refreshKeyHandleLabels();
                                if (syncSizeUi != null && syncSizeUi[0] != null) syncSizeUi[0].run();
                            } else {
                                repositionKeyHandles();
                            }
                            return true;
                        }
                        default:
                            return false;
                    }
                }
            });
            keyEditOverlay.addView(handle);
        }
        refreshKeyHandleLabels();

        TextView legend = new TextView(getContext());
        legend.setText("方块上的字母即该按键的触发区域");
        legend.setTextSize(10.5f);
        legend.setTextColor(Color.parseColor("#CCFFFFFF"));
        legend.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams lg = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lg.bottomMargin = dp(18);
        legend.setLayoutParams(lg);
        keyEditOverlay.addView(legend);
    }

    private void saveTriggerPosition(int index, float centerX, float centerY, float size) {
        if (index < 0 || index >= KeyDisplayView.KEYS.length) return;
        try {
            JSONObject layout = readTriggerLayout();
            JSONObject one = new JSONObject();
            one.put("x", (double) centerX);
            one.put("y", (double) centerY);
            one.put("size", (double) size);
            layout.put(KeyDisplayView.KEYS[index], one);
            appSettings.setKeyTriggerLayout(layout);
            int[] screen = getRealScreenSize();
            if (keyTriggerView != null) keyTriggerView.applyLayout(layout, screen[0], screen[1]);
        } catch (Throwable t) {
            SiyoXLogger.w("SiyoX_OverlayLayout", "saveTriggerPosition: " + t.getMessage());
        }
    }

    private void exitKeyEditMode() {
        keyEditMode = false;
        editingKeyIndex = -1;
        if (keyEditOverlay != null) {
            removeView(keyEditOverlay);
            keyEditOverlay = null;
        }
        if (keyDisplayView != null) keyDisplayView.clearPressed();
        if (panelContainer != null) panelContainer.setVisibility(View.VISIBLE);
        if (floatingBall != null) floatingBall.setVisibility(View.VISIBLE);
        refreshDisplayOverlays();
        setTriggerCatchersVisible(true);
    }

    private void resetKeyDisplayDefaults() {
        appSettings.setKeyDisplayLayout(new JSONObject());
        appSettings.setKeyTriggerLayout(new JSONObject());
        appSettings.setKeyDisplayPos(0, 0);
        appSettings.setKeyDisplayScale(1.0f);
        appSettings.setKeyDisplayAlpha(0.85f);
        appSettings.setKeyDisplayJoystickMode(false);
        appSettings.setKeyEditSeparateSize(true);
        refreshDisplayOverlays();
        Toast.makeText(getContext(), "按键显示已恢复默认", Toast.LENGTH_SHORT).show();
    }

    private void resetFpsDisplayDefaults() {
        appSettings.setFpsDisplayTextSize(15f);
        appSettings.setFpsDisplayPos(600, 5);
        appSettings.setFpsDisplayAlpha(0.9f);
        appSettings.setFpsDisplayChinese(false);
        refreshDisplayOverlays();
        Toast.makeText(getContext(), "帧率显示已恢复默认", Toast.LENGTH_SHORT).show();
    }

    private View createKeyDisplayCard(final boolean isDark) {
        return createDynamicIslandFeatureCard(
                "按键显示",
                "在屏幕上添加一个按键显示" + (appSettings.isKeyDisplayJoystickMode() ? "（摇杆模式）" : ""),
                appSettings.isKeyDisplayEnabled(),
                isDark,
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showKeyDisplaySettingsDialog(isDark);
                    }
                },
                new MiuiXSwitch.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(MiuiXSwitch switchView, boolean isChecked) {
                        appSettings.setKeyDisplayEnabled(isChecked);
                        refreshDisplayOverlays();
                    }
                });
    }

    private View createFpsDisplayCard(final boolean isDark) {
        return createDynamicIslandFeatureCard(
                "帧率显示",
                "在屏幕上显示当前的实时帧率",
                appSettings.isFpsDisplayEnabled(),
                isDark,
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showFpsDisplaySettingsDialog(isDark);
                    }
                },
                new MiuiXSwitch.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(MiuiXSwitch switchView, boolean isChecked) {
                        appSettings.setFpsDisplayEnabled(isChecked);
                        refreshDisplayOverlays();
                    }
                });
    }

    private void setGameplayOverlaysVisible(boolean visible) {

        if (fpsDisplayView != null) {
            boolean show = visible && appSettings.isFpsDisplayEnabled();
            fpsDisplayView.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                fpsDisplayView.start();
            } else {
                fpsDisplayView.stop();
            }
        }

        if (keyDisplayView != null) {
            boolean show = visible && appSettings.isKeyDisplayEnabled();
            if (!show) keyDisplayView.clearPressed();
            keyDisplayView.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        if (keyTriggerView != null) {
            keyTriggerView.setVisibility(visible && appSettings.isKeyDisplayEnabled()
                    ? View.VISIBLE : View.GONE);
        }

        if (dynamicIslandView != null) {
            boolean show = visible && appSettings.isDynamicIslandEnabled() && verifyManager.isVerified();
            dynamicIslandView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showDotColorPickerDialog(final Runnable onColorUpdated) {
        try {
            final Dialog dialog = new Dialog(getContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setDimAmount(0.45f);
            }
            final boolean isDark = SiyoXTheme.isDarkMode(getContext());
            int dp14 = dp(14);
            int dp10 = dp(10);
            int dp8 = dp(8);
            int dp6 = dp(6);
            int dp4 = dp(4);

            int screenW = getRealScreenSize()[0];
            int dialogW = Math.min(screenW - dp(32), dp(440));
            int dialogH = (int) (dialogW * 0.75f);

            LinearLayout root = new LinearLayout(getContext());
            root.setOrientation(LinearLayout.HORIZONTAL);
            root.setPadding(dp14, dp14, dp14, dp14);
            root.setBackground(createCardBg(SiyoXTheme.getCardBg(isDark), Color.TRANSPARENT, dp(18)));
            root.setLayoutParams(new LinearLayout.LayoutParams(dialogW, dialogH));

            LinearLayout leftCol = new LinearLayout(getContext());
            leftCol.setOrientation(LinearLayout.VERTICAL);
            leftCol.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.05f);
            leftCol.setLayoutParams(leftParams);

            final ColorWheelView wheelView = new ColorWheelView(getContext());
            int wheelSize = Math.min(dp(160), dialogH - dp(36));
            LinearLayout.LayoutParams wheelParams = new LinearLayout.LayoutParams(wheelSize, wheelSize);
            wheelParams.gravity = Gravity.CENTER;
            wheelView.setLayoutParams(wheelParams);
            wheelView.setColor(appSettings.getIslandDotColor());
            leftCol.addView(wheelView);
            root.addView(leftCol);

            View vDivider = new View(getContext());
            vDivider.setBackgroundColor(isDark ? Color.parseColor("#2C2C2E") : Color.parseColor("#E5E7EB"));
            LinearLayout.LayoutParams vDivParams = new LinearLayout.LayoutParams(dp(1), LayoutParams.MATCH_PARENT);
            vDivParams.setMargins(dp6, dp4, dp10, dp4);
            vDivider.setLayoutParams(vDivParams);
            root.addView(vDivider);

            LinearLayout rightCol = new LinearLayout(getContext());
            rightCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.25f);
            rightCol.setLayoutParams(rightParams);

            TextView tvTitle = new TextView(getContext());
            tvTitle.setText("圆点颜色调节");
            tvTitle.setTextSize(14.5f);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvTitle.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvTitle.setPadding(0, 0, 0, dp6);
            rightCol.addView(tvTitle);

            LinearLayout previewRow = new LinearLayout(getContext());
            previewRow.setOrientation(LinearLayout.HORIZONTAL);
            previewRow.setGravity(Gravity.CENTER_VERTICAL);
            previewRow.setPadding(0, 0, 0, dp8);

            final View colorPreview = new View(getContext());
            int prevSize = dp(22);
            colorPreview.setLayoutParams(new LinearLayout.LayoutParams(prevSize, prevSize));
            previewRow.addView(colorPreview);

            final TextView tvHex = new TextView(getContext());
            tvHex.setTextSize(12.5f);
            tvHex.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            tvHex.setTextColor(SiyoXTheme.getTextPrimary(isDark));
            tvHex.setPadding(dp8, 0, 0, 0);
            previewRow.addView(tvHex);
            rightCol.addView(previewRow);

            final int[] curSelectedColor = new int[]{appSettings.getIslandDotColor()};

            final Runnable updatePreview = new Runnable() {
                @Override
                public void run() {
                    GradientDrawable d = new GradientDrawable();
                    d.setShape(GradientDrawable.OVAL);
                    d.setColor(curSelectedColor[0]);
                    d.setStroke(dp(1.5f), isDark ? Color.parseColor("#555555") : Color.parseColor("#CCCCCC"));
                    colorPreview.setBackground(d);
                    tvHex.setText(String.format("#%06X", (0xFFFFFF & curSelectedColor[0])));
                }
            };
            updatePreview.run();

            wheelView.setOnColorChangeListener(new ColorWheelView.OnColorChangeListener() {
                @Override
                public void onColorChanged(int color) {
                    curSelectedColor[0] = color;
                    updatePreview.run();
                    appSettings.setIslandDotColor(color);
                    if (onColorUpdated != null) {
                        onColorUpdated.run();
                    }
                }
            });

            TextView tvPaletteTitle = new TextView(getContext());
            tvPaletteTitle.setText("常用推荐色");
            tvPaletteTitle.setTextSize(11f);
            tvPaletteTitle.setTextColor(SiyoXTheme.getTextSecondary(isDark));
            tvPaletteTitle.setPadding(0, 0, 0, dp4);
            rightCol.addView(tvPaletteTitle);

            final int[] presets = new int[]{
                    Color.parseColor("#0A84FF"),
                    Color.parseColor("#30D158"),
                    Color.parseColor("#FF9F0A"),
                    Color.parseColor("#FF453A"),
                    Color.parseColor("#BF5AF2"),
                    Color.parseColor("#FF375F"),
                    Color.parseColor("#64D2FF"),
                    Color.parseColor("#FFD60A")
            };

            LinearLayout paletteRow1 = new LinearLayout(getContext());
            paletteRow1.setOrientation(LinearLayout.HORIZONTAL);
            paletteRow1.setGravity(Gravity.CENTER_VERTICAL);
            paletteRow1.setPadding(0, 0, 0, dp4);

            LinearLayout paletteRow2 = new LinearLayout(getContext());
            paletteRow2.setOrientation(LinearLayout.HORIZONTAL);
            paletteRow2.setGravity(Gravity.CENTER_VERTICAL);
            paletteRow2.setPadding(0, 0, 0, dp6);

            for (int i = 0; i < presets.length; i++) {
                final int presetColor = presets[i];
                View presetDot = new View(getContext());
                int pSize = dp(22);
                LinearLayout.LayoutParams pParams = new LinearLayout.LayoutParams(pSize, pSize);
                pParams.setMargins(0, 0, dp8, 0);
                presetDot.setLayoutParams(pParams);

                GradientDrawable pd = new GradientDrawable();
                pd.setShape(GradientDrawable.OVAL);
                pd.setColor(presetColor);
                pd.setStroke(dp(1), isDark ? Color.parseColor("#444444") : Color.parseColor("#DDDDDD"));
                presetDot.setBackground(pd);

                presetDot.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        curSelectedColor[0] = presetColor;
                        wheelView.setColor(presetColor);
                        updatePreview.run();
                        appSettings.setIslandDotColor(presetColor);
                        if (onColorUpdated != null) {
                            onColorUpdated.run();
                        }
                    }
                });
                if (i < 4) {
                    paletteRow1.addView(presetDot);
                } else {
                    paletteRow2.addView(presetDot);
                }
            }
            rightCol.addView(paletteRow1);
            rightCol.addView(paletteRow2);

            View spacer = new View(getContext());
            spacer.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f));
            rightCol.addView(spacer);

            LinearLayout btnRow = new LinearLayout(getContext());
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams btnRowParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(34));
            btnRow.setLayoutParams(btnRowParams);

            Button btnDefault = new Button(getContext());
            btnDefault.setText("默认蓝色");
            btnDefault.setTextSize(11.5f);
            btnDefault.setTypeface(Typeface.DEFAULT_BOLD);
            btnDefault.setTextColor(isDark ? Color.parseColor("#E5E5EA") : Color.parseColor("#3C3C43"));
            int defBg = isDark ? Color.parseColor("#3A3A3C") : Color.parseColor("#E5E7EB");
            int defPressed = isDark ? Color.parseColor("#2C2C2E") : Color.parseColor("#D1D5DB");
            btnDefault.setBackground(createRippleDrawable(defBg, defPressed, dp(8)));
            styleCleanButton(btnDefault);
            LinearLayout.LayoutParams defParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
            defParams.setMargins(0, 0, dp6, 0);
            btnDefault.setLayoutParams(defParams);
            btnDefault.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int defColor = Color.parseColor("#0A84FF");
                    curSelectedColor[0] = defColor;
                    wheelView.setColor(defColor);
                    updatePreview.run();
                    appSettings.setIslandDotColor(defColor);
                    if (onColorUpdated != null) {
                        onColorUpdated.run();
                    }
                }
            });
            btnRow.addView(btnDefault);

            Button btnConfirm = new Button(getContext());
            btnConfirm.setText("确定");
            btnConfirm.setTextSize(11.5f);
            btnConfirm.setTypeface(Typeface.DEFAULT_BOLD);
            btnConfirm.setTextColor(Color.WHITE);
            btnConfirm.setBackground(createRippleDrawable(Color.parseColor("#0A84FF"), Color.parseColor("#0066CC"), dp(8)));
            styleCleanButton(btnConfirm);
            LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
            btnConfirm.setLayoutParams(confirmParams);
            btnConfirm.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            btnRow.addView(btnConfirm);
            rightCol.addView(btnRow);

            root.addView(rightCol);

            dialog.setContentView(root);
            dialog.show();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(dialogW, dialogH);
            }
        } catch (Throwable t) {
            SiyoXLogger.e("SiyoX_Overlay", "Error showing dot color picker dialog: " + t.getMessage(), t);
        }
    }

}
