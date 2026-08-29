// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class CopyIconView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF backRect = new RectF();
    private final RectF frontRect = new RectF();
    private int iconColor = Color.parseColor("#0A84FF");

    public CopyIconView(Context context) {
        super(context);
        init();
    }

    public CopyIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setIconColor(int color) {
        this.iconColor = color;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = dp(20);
        setMeasuredDimension(resolveSize(size, widthMeasureSpec), resolveSize(size, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        paint.setColor(iconColor);
        paint.setStrokeWidth(dpF(1.5f));

        float corner = dpF(2.5f);

        // 1. Back rectangle (Top-Right)
        float bw = w * 0.55f;
        float bh = h * 0.65f;
        backRect.set(w * 0.35f, h * 0.10f, w * 0.35f + bw, h * 0.10f + bh);
        canvas.drawRoundRect(backRect, corner, corner, paint);

        // 2. Front rectangle (Bottom-Left)
        float fw = w * 0.55f;
        float fh = h * 0.65f;
        frontRect.set(w * 0.10f, h * 0.25f, w * 0.10f + fw, h * 0.25f + fh);

        // Fill background of front rect so lines don't overlap awkwardly
        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        boolean isDark = SiyoXTheme.isDarkMode(getContext());
        fillPaint.setColor(SiyoXTheme.getInnerCardBg(isDark));
        canvas.drawRoundRect(frontRect, corner, corner, fillPaint);

        canvas.drawRoundRect(frontRect, corner, corner, paint);
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private float dpF(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
