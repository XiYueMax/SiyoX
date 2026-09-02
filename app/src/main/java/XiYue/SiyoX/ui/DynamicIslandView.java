package XiYue.SiyoX.ui;

import android.animation.LayoutTransition;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import XiYue.SiyoX.SiyoXConfig;
import XiYue.SiyoX.data.AppSettings;
import XiYue.SiyoX.data.LoginVideoManager;
import XiYue.SiyoX.data.ResourceInjector;

public class DynamicIslandView extends FrameLayout {

    private final Handler timeHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private LinearLayout islandCard;
    private LinearLayout topRow;
    private View statusDot;
    private TextView tvCompactClient;
    private View sepAuthor;
    private TextView tvCompactAuthor;
    private View sepTime;
    private TextView tvCompactTime;
    private View dividerProgress;
    private TextView tvCompactStatus;
    private ProgressBar pbCompact;

    private boolean isResDownloading = false;
    private boolean isVideoDownloading = false;

    private final Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            updateTime();
            timeHandler.postDelayed(this, 1000);
        }
    };

    public DynamicIslandView(Context context) {
        super(context);
        initView();
        setupListeners();
        startTimeTicker();
        applyTransform();
        applySettingsConfig();
    }

    private int dp(float dpValue) {
        return (int) (dpValue * getContext().getResources().getDisplayMetrics().density + 0.5f);
    }

    private void initView() {
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.TOP));
        setClipChildren(false);
        setClipToPadding(false);

        islandCard = new LinearLayout(getContext());
        islandCard.setOrientation(LinearLayout.VERTICAL);
        islandCard.setGravity(Gravity.CENTER_HORIZONTAL);
        islandCard.setPadding(dp(13), dp(5.5f), dp(13), dp(7));
        updateIslandBackground(false);
        islandCard.setElevation(dp(8));
        islandCard.setClipChildren(false);
        islandCard.setClipToPadding(false);

        LayoutParams cardParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        islandCard.setLayoutParams(cardParams);

        LayoutTransition transition = new LayoutTransition();
        transition.setDuration(160);
        transition.enableTransitionType(LayoutTransition.CHANGING);
        islandCard.setLayoutTransition(transition);

        topRow = new LinearLayout(getContext());
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp(22)));

        statusDot = new View(getContext());
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(Color.parseColor("#0A84FF"));
        statusDot.setBackground(dotBg);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(7), dp(7));
        dotParams.setMargins(0, 0, dp(6), 0);
        statusDot.setLayoutParams(dotParams);
        topRow.addView(statusDot);

        tvCompactClient = new TextView(getContext());
        String cName = SiyoXConfig.CLIENT_NAME;
        if (cName == null || cName.trim().isEmpty()) {
            cName = SiyoXConfig.APP_NAME;
        }
        tvCompactClient.setText(cName);
        tvCompactClient.setTextSize(11.5f);
        tvCompactClient.setTypeface(Typeface.DEFAULT_BOLD);
        tvCompactClient.setTextColor(Color.WHITE);
        topRow.addView(tvCompactClient);

        sepAuthor = new View(getContext());
        sepAuthor.setBackgroundColor(Color.parseColor("#33FFFFFF"));
        LinearLayout.LayoutParams sepAuthorParams = new LinearLayout.LayoutParams(dp(1), dp(10));
        sepAuthorParams.setMargins(dp(7), 0, dp(7), 0);
        sepAuthor.setLayoutParams(sepAuthorParams);
        sepAuthor.setVisibility(View.GONE);
        topRow.addView(sepAuthor);

        tvCompactAuthor = new TextView(getContext());
        String cAuthor = SiyoXConfig.CLIENT_AUTHOR;
        if (cAuthor == null || cAuthor.trim().isEmpty()) {
            cAuthor = SiyoXConfig.AUTHOR;
        }
        tvCompactAuthor.setText(cAuthor);
        tvCompactAuthor.setTextSize(11f);
        tvCompactAuthor.setTextColor(Color.parseColor("#9CA3AF"));
        tvCompactAuthor.setVisibility(View.GONE);
        topRow.addView(tvCompactAuthor);

        sepTime = new View(getContext());
        sepTime.setBackgroundColor(Color.parseColor("#33FFFFFF"));
        LinearLayout.LayoutParams sepTimeParams = new LinearLayout.LayoutParams(dp(1), dp(10));
        sepTimeParams.setMargins(dp(7), 0, dp(7), 0);
        sepTime.setLayoutParams(sepTimeParams);
        topRow.addView(sepTime);

        tvCompactTime = new TextView(getContext());
        tvCompactTime.setText(timeFormat.format(new Date()));
        tvCompactTime.setTextSize(11.5f);
        tvCompactTime.setTypeface(Typeface.DEFAULT_BOLD);
        tvCompactTime.setTextColor(Color.parseColor("#D1D5DB"));
        topRow.addView(tvCompactTime);

        dividerProgress = new View(getContext());
        dividerProgress.setBackgroundColor(Color.parseColor("#33FFFFFF"));
        LinearLayout.LayoutParams divProgParams = new LinearLayout.LayoutParams(dp(1), dp(10));
        divProgParams.setMargins(dp(7), 0, dp(7), 0);
        dividerProgress.setLayoutParams(divProgParams);
        dividerProgress.setVisibility(View.GONE);
        topRow.addView(dividerProgress);

        tvCompactStatus = new TextView(getContext());
        tvCompactStatus.setTextSize(11f);
        tvCompactStatus.setTypeface(Typeface.DEFAULT_BOLD);
        tvCompactStatus.setTextColor(Color.parseColor("#0A84FF"));
        tvCompactStatus.setVisibility(View.GONE);
        topRow.addView(tvCompactStatus);

        islandCard.addView(topRow);

        pbCompact = createProgressBar();
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(4.5f));
        pbParams.setMargins(0, dp(4.5f), 0, dp(1.5f));
        pbCompact.setLayoutParams(pbParams);
        pbCompact.setVisibility(View.GONE);
        islandCard.addView(pbCompact);

        addView(islandCard);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (islandCard != null && islandCard.getWidth() > 0) {
            islandCard.setPivotX(islandCard.getWidth() / 2f);
            islandCard.setPivotY(0);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    public void applyTransform() {
        try {
            AppSettings settings = AppSettings.get();
            final int scale = settings.getIslandScale();
            final int posX = settings.getIslandPosX();
            final int posY = settings.getIslandPosY();

            if (islandCard != null) {
                islandCard.setScaleX(scale / 100f);
                islandCard.setScaleY(scale / 100f);
                islandCard.setTranslationX(dp(posX));
                islandCard.setTranslationY(dp(posY));
                islandCard.post(new Runnable() {
                    @Override
                    public void run() {
                        if (islandCard != null && islandCard.getWidth() > 0) {
                            islandCard.setPivotX(islandCard.getWidth() / 2f);
                            islandCard.setPivotY(0);
                        }
                    }
                });
            }
            updateIslandBackground(isResDownloading || isVideoDownloading);
        } catch (Throwable ignored) {}
    }

    public void applySettingsConfig() {
        try {
            AppSettings settings = AppSettings.get();
            boolean showTime = settings.isIslandShowTime();
            boolean showAuthor = settings.isIslandShowAuthor();
            boolean showProgress = settings.isIslandShowProgress();

            if (sepTime != null && tvCompactTime != null) {
                sepTime.setVisibility(showTime ? View.VISIBLE : View.GONE);
                tvCompactTime.setVisibility(showTime ? View.VISIBLE : View.GONE);
            }

            if (sepAuthor != null && tvCompactAuthor != null) {
                sepAuthor.setVisibility(showAuthor ? View.VISIBLE : View.GONE);
                tvCompactAuthor.setVisibility(showAuthor ? View.VISIBLE : View.GONE);
            }

            if (!showProgress) {
                if (dividerProgress != null) dividerProgress.setVisibility(View.GONE);
                if (tvCompactStatus != null) tvCompactStatus.setVisibility(View.GONE);
                if (pbCompact != null) pbCompact.setVisibility(View.GONE);
                updateIslandBackground(false);
            } else if (isResDownloading || isVideoDownloading) {
                if (dividerProgress != null) dividerProgress.setVisibility(View.VISIBLE);
                if (tvCompactStatus != null) tvCompactStatus.setVisibility(View.VISIBLE);
                if (pbCompact != null) pbCompact.setVisibility(View.VISIBLE);
                updateIslandBackground(true);
            } else {
                updateIslandBackground(false);
            }
        } catch (Throwable ignored) {}
    }

    private void updateIslandBackground(boolean isExpanded) {
        if (islandCard == null) return;
        AppSettings settings = AppSettings.get();
        int baseRadius = settings != null ? settings.getIslandCornerRadius() : 18;
        int targetRadius = isExpanded ? Math.max(6, (int) (baseRadius * 0.6f)) : baseRadius;

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#E6121214"));
        bg.setCornerRadius(dp(targetRadius));
        bg.setStroke(dp(1), Color.parseColor("#26FFFFFF"));
        islandCard.setBackground(bg);
    }

    private ProgressBar createProgressBar() {
        ProgressBar pb = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        pb.setProgress(0);
        pb.setIndeterminate(false);

        GradientDrawable pbBg = new GradientDrawable();
        pbBg.setColor(Color.parseColor("#2C2C2E"));
        pbBg.setCornerRadius(dp(2));

        GradientDrawable pbProgress = new GradientDrawable();
        pbProgress.setColor(Color.parseColor("#0A84FF"));
        pbProgress.setCornerRadius(dp(2));
        ClipDrawable clipDrawable = new ClipDrawable(pbProgress, Gravity.START, ClipDrawable.HORIZONTAL);

        LayerDrawable progressLayer = new LayerDrawable(new Drawable[]{pbBg, clipDrawable});
        progressLayer.setId(0, android.R.id.background);
        progressLayer.setId(1, android.R.id.progress);
        pb.setProgressDrawable(progressLayer);
        return pb;
    }

    private void updateTime() {
        if (tvCompactTime != null) {
            tvCompactTime.setText(timeFormat.format(new Date()));
        }
    }

    private void startTimeTicker() {
        timeHandler.removeCallbacks(timeRunnable);
        timeHandler.post(timeRunnable);
    }

    public void updateClientName() {
        if (tvCompactClient != null) {
            String cName = SiyoXConfig.CLIENT_NAME;
            if (cName == null || cName.trim().isEmpty()) {
                cName = SiyoXConfig.APP_NAME;
            }
            tvCompactClient.setText(cName);
        }
        if (tvCompactAuthor != null) {
            String cAuthor = SiyoXConfig.CLIENT_AUTHOR;
            if (cAuthor == null || cAuthor.trim().isEmpty()) {
                cAuthor = SiyoXConfig.AUTHOR;
            }
            tvCompactAuthor.setText(cAuthor);
        }
    }

    private void setupListeners() {
        ResourceInjector.setGlobalDownloadListener(new ResourceInjector.GlobalResourceDownloadListener() {
            @Override
            public void onDownloadProgress(final String packName, final int percent, long currentBytes, long totalBytes) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        isResDownloading = true;
                        if (AppSettings.get().isIslandShowProgress()) {
                            dividerProgress.setVisibility(View.VISIBLE);
                            tvCompactStatus.setVisibility(View.VISIBLE);
                            tvCompactStatus.setTextColor(Color.parseColor("#0A84FF"));
                            tvCompactStatus.setText("资源 " + percent + "%");
                            pbCompact.setVisibility(View.VISIBLE);
                            pbCompact.setProgress(Math.max(0, percent));
                            updateIslandBackground(true);
                        }
                        if (statusDot != null) {
                            GradientDrawable dot = (GradientDrawable) statusDot.getBackground();
                            dot.setColor(Color.parseColor("#0A84FF"));
                        }
                    }
                });
            }

            @Override
            public void onDownloadComplete(final String packName, final boolean success, final String message) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        isResDownloading = false;
                        if (AppSettings.get().isIslandShowProgress()) {
                            if (success) {
                                pbCompact.setProgress(100);
                                tvCompactStatus.setTextColor(Color.parseColor("#30D158"));
                                tvCompactStatus.setText("下载完成");
                            } else {
                                tvCompactStatus.setTextColor(Color.parseColor("#FF3B30"));
                                tvCompactStatus.setText("下载失败");
                            }
                        }
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!isResDownloading && !isVideoDownloading) {
                                    dividerProgress.setVisibility(View.GONE);
                                    tvCompactStatus.setVisibility(View.GONE);
                                    pbCompact.setVisibility(View.GONE);
                                    updateIslandBackground(false);
                                    if (statusDot != null) {
                                        GradientDrawable dot = (GradientDrawable) statusDot.getBackground();
                                        dot.setColor(Color.parseColor("#0A84FF"));
                                    }
                                }
                            }
                        }, 3500);
                    }
                });
            }
        });

        LoginVideoManager.get().setGlobalListener(new LoginVideoManager.LoginVideoListener() {
            @Override
            public void onProgress(final int percent, final String status) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        isVideoDownloading = true;
                        if (AppSettings.get().isIslandShowProgress()) {
                            dividerProgress.setVisibility(View.VISIBLE);
                            tvCompactStatus.setVisibility(View.VISIBLE);
                            tvCompactStatus.setTextColor(Color.parseColor("#0A84FF"));
                            tvCompactStatus.setText("视频 " + percent + "%");
                            pbCompact.setVisibility(View.VISIBLE);
                            pbCompact.setProgress(Math.max(0, percent));
                            updateIslandBackground(true);
                        }
                        if (statusDot != null) {
                            GradientDrawable dot = (GradientDrawable) statusDot.getBackground();
                            dot.setColor(Color.parseColor("#0A84FF"));
                        }
                    }
                });
            }

            @Override
            public void onComplete(final boolean success, final String message) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        isVideoDownloading = false;
                        if (AppSettings.get().isIslandShowProgress()) {
                            if (success) {
                                pbCompact.setProgress(100);
                                tvCompactStatus.setTextColor(Color.parseColor("#30D158"));
                                tvCompactStatus.setText("下载完成");
                            } else {
                                tvCompactStatus.setTextColor(Color.parseColor("#FF3B30"));
                                tvCompactStatus.setText("下载失败");
                            }
                        }
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!isResDownloading && !isVideoDownloading) {
                                    dividerProgress.setVisibility(View.GONE);
                                    tvCompactStatus.setVisibility(View.GONE);
                                    pbCompact.setVisibility(View.GONE);
                                    updateIslandBackground(false);
                                    if (statusDot != null) {
                                        GradientDrawable dot = (GradientDrawable) statusDot.getBackground();
                                        dot.setColor(Color.parseColor("#0A84FF"));
                                    }
                                }
                            }
                        }, 3500);
                    }
                });
            }
        });
    }

    public void resetToIdle() {
        post(new Runnable() {
            @Override
            public void run() {
                isResDownloading = false;
                isVideoDownloading = false;
                if (dividerProgress != null) dividerProgress.setVisibility(View.GONE);
                if (tvCompactStatus != null) tvCompactStatus.setVisibility(View.GONE);
                if (pbCompact != null) {
                    pbCompact.setVisibility(View.GONE);
                    pbCompact.setProgress(0);
                }
                updateIslandBackground(false);
                if (statusDot != null) {
                    GradientDrawable dot = (GradientDrawable) statusDot.getBackground();
                    if (dot != null) dot.setColor(Color.parseColor("#0A84FF"));
                }
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        timeHandler.removeCallbacks(timeRunnable);
    }
}
