// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import XiYue.SiyoX.data.AppSettings;
import XiYue.SiyoX.data.VerifyManager;
import XiYue.SiyoX.ui.CopyIconView;
import XiYue.SiyoX.ui.LogoLoader;
import XiYue.SiyoX.ui.SiyoXOverlayLayout;
import XiYue.SiyoX.ui.SiyoXTheme;

public class MainActivity extends Activity {

    private VerifyManager verifyManager;

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
        int dp14 = dp(14);
        int dp12 = dp(12);
        int dp10 = dp(10);
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

        // ==========================================
        // 1. 第一个卡片：左侧是 Logo，右侧是 SiyoX，第二行显示版本号
        // ==========================================
        LinearLayout headerCard = createCard(isDark);
        headerCard.setPadding(dp16, dp16, dp16, dp16);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // 左侧 Logo
        ImageView logoView = new ImageView(this);
        int logoSize = dp(56);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(logoSize, logoSize);
        logoView.setLayoutParams(logoParams);
        Bitmap logoBmp = LogoLoader.getLogo(this);
        if (logoBmp != null) {
            logoView.setImageBitmap(logoBmp);
        } else {
            logoView.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        logoView.setBackground(createCardBg(isDark ? Color.parseColor("#2A2A2E") : Color.WHITE, Color.TRANSPARENT, dp(14)));
        logoView.setClipToOutline(true);
        headerRow.addView(logoView);

        // 右侧文字列 (第一行 SiyoX，第二行版本号)
        LinearLayout headerTextCol = new LinearLayout(this);
        headerTextCol.setOrientation(LinearLayout.VERTICAL);
        headerTextCol.setGravity(Gravity.CENTER_VERTICAL);
        headerTextCol.setPadding(dp14, 0, 0, 0);
        headerTextCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        // 第一行: Siyo (黑/白) + X (蓝)
        View titleView = SiyoXOverlayLayout.createSiyoXTitleView(this, 22f, isDark);
        headerTextCol.addView(titleView);

        // 第二行: 版本号
        TextView tvVersion = new TextView(this);
        tvVersion.setText("版本号: " + SiyoXConfig.VERSION_NAME);
        tvVersion.setTextSize(13f);
        tvVersion.setTextColor(SiyoXTheme.getTextSecondary(isDark));
        tvVersion.setPadding(0, dp(4), 0, 0);
        headerTextCol.addView(tvVersion);

        headerRow.addView(headerTextCol);
        headerCard.addView(headerRow);
        contentLayout.addView(headerCard);

        // ==========================================
        // 2. 第二个卡片：模块状态 (客户端名称，客户端作者，模块名称，模块作者，HWID)
        // ==========================================
        LinearLayout statusCard = createCard(isDark);
        statusCard.setPadding(dp16, dp16, dp16, dp16);

        TextView tvStatusTitle = new TextView(this);
        tvStatusTitle.setText("模块信息");
        tvStatusTitle.setTextSize(14f);

        tvStatusTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvStatusTitle.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        statusCard.addView(tvStatusTitle);

        statusCard.addView(createDivider(isDark));

        // 1) 客户端名称
        statusCard.addView(createInfoRow("客户端名称", SiyoXConfig.CLIENT_NAME, isDark));
        statusCard.addView(createDivider(isDark));

        // 2) 客户端作者
        statusCard.addView(createInfoRow("客户端作者", SiyoXConfig.CLIENT_AUTHOR, isDark));
        statusCard.addView(createDivider(isDark));

        // 3) 模块名称
        statusCard.addView(createCustomInfoRow("模块名称", SiyoXOverlayLayout.createSiyoXTitleView(this, 13f, isDark), isDark));
        statusCard.addView(createDivider(isDark));

        // 4) 模块作者
        statusCard.addView(createInfoRow("模块作者", SiyoXConfig.AUTHOR, isDark)); // @XiYueMax

        contentLayout.addView(statusCard);


        // ==========================================
        // 3. 第三个卡片：GitHub 开源链接，点击即可跳转
        // ==========================================
        LinearLayout githubCard = createCard(isDark);
        githubCard.setPadding(dp16, dp16, dp16, dp16);
        githubCard.setBackground(createRippleDrawable(SiyoXTheme.getCardBg(isDark), isDark ? Color.parseColor("#2A3A50") : Color.parseColor("#EBF5FF"), dp(16)));
        githubCard.setClickable(true);

        LinearLayout ghHeaderRow = new LinearLayout(this);
        ghHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        ghHeaderRow.setGravity(Gravity.CENTER_VERTICAL);
        ghHeaderRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvGhTitle = new TextView(this);
        tvGhTitle.setText("GitHub 开源链接");
        tvGhTitle.setTextSize(14f);
        tvGhTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvGhTitle.setTextColor(SiyoXTheme.getTextPrimary(isDark));
        tvGhTitle.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        ghHeaderRow.addView(tvGhTitle);

        TextView tvJumpHint = new TextView(this);
        tvJumpHint.setText("点击跳转 ➔");
        tvJumpHint.setTextSize(12f);
        tvJumpHint.setTypeface(Typeface.DEFAULT_BOLD);
        tvJumpHint.setTextColor(SiyoXTheme.getAccentBlue());
        ghHeaderRow.addView(tvJumpHint);

        githubCard.addView(ghHeaderRow);
        githubCard.addView(createDivider(isDark));

        TextView tvGhUrl = new TextView(this);
        tvGhUrl.setText(SiyoXConfig.GITHUB_URL);
        tvGhUrl.setTextSize(12.5f);
        tvGhUrl.setTextColor(SiyoXTheme.getAccentBlue());
        tvGhUrl.setPadding(0, dp(4), 0, 0);
        githubCard.addView(tvGhUrl);

        githubCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(SiyoXConfig.GITHUB_URL));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "无法打开浏览器: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        contentLayout.addView(githubCard);

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
        valTv.setTypeface(Typeface.DEFAULT_BOLD);
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
