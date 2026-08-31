

package XiYue.SiyoX.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class MiuiXCheckBox extends View {

    public interface OnCheckedChangeListener {
        void onCheckedChanged(MiuiXCheckBox checkBox, boolean isChecked);
    }

    private boolean isChecked = false;
    private OnCheckedChangeListener listener;

    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF boxRect = new RectF();
    private final Path checkPath = new Path();

    private float progress = 0f; 
    private ValueAnimator animator;

    private static final int CHECKED_COLOR = Color.parseColor("#0A84FF");

    public MiuiXCheckBox(Context context) {
        super(context);
        init();
    }

    public MiuiXCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpF(1.5f));

        checkPaint.setColor(Color.WHITE);
        checkPaint.setStyle(Paint.Style.STROKE);
        checkPaint.setStrokeCap(Paint.Cap.ROUND);
        checkPaint.setStrokeJoin(Paint.Join.ROUND);
        checkPaint.setStrokeWidth(dpF(2f));

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });
    }

    public void setChecked(boolean checked) {
        setChecked(checked, true);
    }

    public void setChecked(boolean checked, boolean animate) {
        if (this.isChecked != checked) {
            this.isChecked = checked;
            if (listener != null) {
                listener.onCheckedChanged(this, checked);
            }
        }
        if (animate) {
            animateToProgress(checked ? 1f : 0f);
        } else {
            progress = checked ? 1f : 0f;
            invalidate();
        }
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void toggle() {
        setChecked(!isChecked, true);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    private void animateToProgress(float target) {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        animator = ValueAnimator.ofFloat(progress, target);
        animator.setDuration(180);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                progress = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = dp(20);
        setMeasuredDimension(resolveSize(size, widthMeasureSpec), resolveSize(size, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        boolean isDark = SiyoXTheme.isDarkMode(getContext());
        int uncheckedBg = isDark ? Color.parseColor("#2A2A2E") : Color.parseColor("#FFFFFF");
        int uncheckedBorder = isDark ? Color.parseColor("#4A4A52") : Color.parseColor("#D1D5DB");

        int w = getWidth();
        int h = getHeight();
        float radius = dpF(5f);

        boxRect.set(dpF(1.5f), dpF(1.5f), w - dpF(1.5f), h - dpF(1.5f));

int currentBgColor = evaluateColor(progress, uncheckedBg, CHECKED_COLOR);
        boxPaint.setColor(currentBgColor);
        boxPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(boxRect, radius, radius, boxPaint);

if (progress < 1f) {
            int currentBorderColor = evaluateColor(progress, uncheckedBorder, CHECKED_COLOR);
            borderPaint.setColor(currentBorderColor);
            canvas.drawRoundRect(boxRect, radius, radius, borderPaint);
        }

if (progress > 0f) {
            checkPaint.setAlpha((int) (255 * progress));

            float startX = w * 0.28f;
            float startY = h * 0.52f;
            float midX = w * 0.44f;
            float midY = h * 0.70f;
            float endX = w * 0.74f;
            float endY = h * 0.32f;

            checkPath.reset();
            checkPath.moveTo(startX, startY);

            if (progress <= 0.5f) {
                float seg1 = progress / 0.5f;
                checkPath.lineTo(startX + (midX - startX) * seg1, startY + (midY - startY) * seg1);
            } else {
                checkPath.lineTo(midX, midY);
                float seg2 = (progress - 0.5f) / 0.5f;
                checkPath.lineTo(midX + (endX - midX) * seg2, midY + (endY - midY) * seg2);
            }

            canvas.drawPath(checkPath, checkPaint);
        }
    }

    private int evaluateColor(float fraction, int startColor, int endColor) {
        int startA = (startColor >> 24) & 0xff;
        int startR = (startColor >> 16) & 0xff;
        int startG = (startColor >> 8) & 0xff;
        int startB = startColor & 0xff;

        int endA = (endColor >> 24) & 0xff;
        int endR = (endColor >> 16) & 0xff;
        int endG = (endColor >> 8) & 0xff;
        int endB = endColor & 0xff;

        return ((startA + (int) (fraction * (endA - startA))) << 24) |
                ((startR + (int) (fraction * (endR - startR))) << 16) |
                ((startG + (int) (fraction * (endG - startG))) << 8) |
                ((startB + (int) (fraction * (endB - startB))));
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private float dpF(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
