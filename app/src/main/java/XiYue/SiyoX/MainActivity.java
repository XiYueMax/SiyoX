// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import XiYue.SiyoX.data.AppSettings;
import XiYue.SiyoX.data.VerifyManager;
import XiYue.SiyoX.ui.LogoLoader;
import XiYue.SiyoX.ui.SiyoXOverlayLayout;
import XiYue.SiyoX.ui.SiyoXTheme;

public class MainActivity extends Activity {

    private VerifyManager verifyManager;
    private TextView tvStatusBadge;
    private TextView tvAndroidId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppSettings.init(getApplicationContext());
        verifyManager = VerifyManager.init(getApplicationContext());

        initUI();
    }

    private void initUI() {
        boolean isDark = SiyoXTheme.isDarkMode(this);

        int dp16 = dp(16);
        int dp20 = dp(20);
        int dp12 = dp(12);
        int dp8 = dp(8);

        FrameLayout rootFrame = new FrameLayout(this);
        rootFrame.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rootFrame.setBackgroundColor(isDark ? Color.parseColor("#121214") : Color.parseColor("#F2F3F7"));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        contentLayout.setPadding(dp16, dp20 + dp(24), dp16, dp20);
        contentLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        // 1. App Header Card (Logo, 软件名, 版本号, 包名, 作者)
        LinearLayout headerCard = createCard(isDark);
        headerCard.setPadding(dp20, dp20, dp20, dp20);
        headerCard.setGravity(Gravity.CENTER_HORIZONTAL);

        // Logo
        ImageView logoView = new ImageView(this);
        int logoSize = dp(80);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(logoSize, logoSize);
        logoView.setLayoutParams(logoParams);
        Bitmap logoBmp = LogoLoader.getLogo(this);
        if (logoBmp != null) {
            logoView.setImageBitmap(logoBmp);
        } else {
            logoView.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        logoView.setBackground(createCardBg(isDark ? Color.parseColor("#2A2A2E") : Color.WHITE, Color.TRANSPARENT, dp(20)));
        logoView.setClipToOutline(true);
        headerCard.addView(logoView);

        // 软件名 (Siyo 黑/白 + X 蓝)
        View titleView = SiyoXOverlayLayout.createSiyoXTitleView(this, 24f, isDark);
        titleView.setPadding(0, dp12, 0, 0);
        headerCard.addView(titleView);

        // 版本号 (不显示验证提供商)
        TextView tvVersion = new TextView(this);
        tvVersion.setText("版本号: " + SiyoXConfig.VERSION_NAME);
        tvVersion.setTextSize(13f);
        tvVersion.setTextColor(SiyoXTheme.getTextSecondary(isDark));
        tvVersion.setPadding(0, dp(4), 0, 0);
        headerCard.addView(tvVersion);

        // 包名
        TextView tvPackage = new TextView(this);
        tvPackage.setText("包名: " + SiyoXConfig.PACKAGE_NAME);
        tvPackage.setTextSize(13f);
        tvPackage.setTextColor(SiyoXTheme.getTextSecondary(isDark));
        tvPackage.setPadding(0, dp(2), 0, 0);
        headerCard.addView(tvPackage);

        // 作者
        TextView tvAuthor = new TextView(this);
        tvAuthor.setText("作者: " + SiyoXConfig.AUTHOR); // @XiYueMax
        tvAuthor.setTextSize(14f);
        tvAuthor.setTypeface(Typeface.DEFAULT_BOLD);
        tvAuthor.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        tvAuthor.setPadding(0, dp(6), 0, 0);
        headerCard.addView(tvAuthor);

        contentLayout.addView(headerCard);

        // 2. GitHub Card
        LinearLayout githubCard = createCard(isDark);
        githubCard.setPadding(dp16, dp16, dp16, dp16);

        TextView tvGhTitle = new TextView(this);
        tvGhTitle.setText("开源项目仓库");
        tvGhTitle.setTextSize(14f);
        tvGhTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvGhTitle.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        githubCard.addView(tvGhTitle);

        TextView tvGhUrl = new TextView(this);
        tvGhUrl.setText("项目地址: " + SiyoXConfig.GITHUB_URL);
        tvGhUrl.setTextSize(12f);
        tvGhUrl.setTextColor(SiyoXTheme.getTextSecondary(isDark));
        tvGhUrl.setPadding(0, dp(4), 0, dp12);
        githubCard.addView(tvGhUrl);

        Button btnGithub = new Button(this);
        btnGithub.setText("前往 GitHub 查看源码");
        btnGithub.setTextSize(14f);
        btnGithub.setTypeface(Typeface.DEFAULT_BOLD);
        btnGithub.setTextColor(Color.WHITE);
        btnGithub.setBackground(createRippleDrawable(Color.parseColor("#0A84FF"), Color.parseColor("#0066CC"), dp(12)));
        btnGithub.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));
        btnGithub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(SiyoXConfig.GITHUB_URL));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        githubCard.addView(btnGithub);

        contentLayout.addView(githubCard);

        // 3. Module & Environment Card (不显示网络验证提供商)
        LinearLayout envCard = createCard(isDark);
        envCard.setPadding(dp16, dp16, dp16, dp16);

        TextView tvEnvTitle = new TextView(this);
        tvEnvTitle.setText("模块运行状态");
        tvEnvTitle.setTextSize(14f);
        tvEnvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvEnvTitle.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        envCard.addView(tvEnvTitle);

        envCard.addView(createDivider(isDark));

        // 客户端名称
        envCard.addView(createInfoRow("客户端名称", SiyoXConfig.CLIENT_NAME, isDark));

        // 客户端作者
        envCard.addView(createInfoRow("客户端作者", SiyoXConfig.CLIENT_AUTHOR, isDark));

        // 作用域目标
        envCard.addView(createInfoRow("作用域目标", SiyoXConfig.TARGET_PACKAGE, isDark));


        // Android ID
        tvAndroidId = new TextView(this);
        tvAndroidId.setText(verifyManager.getAndroidId());
        tvAndroidId.setTextSize(12f);
        tvAndroidId.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        envCard.addView(createCustomInfoRow("设备 Android ID", tvAndroidId, isDark));

        // 授权状态
        tvStatusBadge = new TextView(this);
        boolean isVer = verifyManager.isVerified();
        tvStatusBadge.setText(isVer ? "已激活" : "未激活");
        tvStatusBadge.setTextSize(11f);
        tvStatusBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvStatusBadge.setTextColor(Color.WHITE);
        tvStatusBadge.setPadding(dp8, dp(3), dp8, dp(3));
        tvStatusBadge.setBackground(createCardBg(isVer ? Color.parseColor("#34C759") : Color.parseColor("#FF9500"), Color.TRANSPARENT, dp(8)));
        envCard.addView(createCustomInfoRow("卡密授权状态", tvStatusBadge, isDark));

        contentLayout.addView(envCard);

        scrollView.addView(contentLayout);
        rootFrame.addView(scrollView);
        setContentView(rootFrame);
    }

    private LinearLayout createCard(boolean isDark) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(params);
        card.setBackground(createCardBg(SiyoXTheme.getCardBg(isDark), Color.TRANSPARENT, dp(16)));
        return card;
    }

    private View createInfoRow(String label, String value, boolean isDark) {
        TextView valTv = new TextView(this);
        valTv.setText(value);
        valTv.setTextSize(13f);
        valTv.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        return createCustomInfoRow(label, valTv, isDark);
    }

    private View createCustomInfoRow(String label, View rightView, boolean isDark) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(4), 0, dp(4));
        row.setLayoutParams(p);

        TextView labelTv = new TextView(this);
        labelTv.setText(label);
        labelTv.setTextSize(13f);
        labelTv.setTextColor(SiyoXTheme.getTextSecondary(isDark));
        labelTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(labelTv);

        row.addView(rightView);
        return row;
    }

    private View createDivider(boolean isDark) {
        View div = new View(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        p.setMargins(0, dp(8), 0, dp(8));
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

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                v,
                getResources().getDisplayMetrics()
        );
    }
}
