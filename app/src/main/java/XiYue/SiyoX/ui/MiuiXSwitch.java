

package XiYue.SiyoX.ui;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class MiuiXSwitch extends View {

    public interface OnCheckedChangeListener {
        void onCheckedChanged(MiuiXSwitch switchView, boolean isChecked);
    }

    private boolean isChecked = false;
    private OnCheckedChangeListener listener;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect = new RectF();

    private float progress = 0f; 
    private ValueAnimator animator;

    private static final int OFF_COLOR = Color.parseColor("#E5E7EB");
    private static final int ON_COLOR = Color.parseColor("#0A84FF");

    public MiuiXSwitch(Context context) {
        super(context);
        init();
    }

    public MiuiXSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);

        thumbPaint.setColor(Color.WHITE);
        thumbPaint.setShadowLayer(dp(2), 0, dp(1), Color.parseColor("#33000000"));
        setLayerType(LAYER_TYPE_SOFTWARE, thumbPaint);

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
        animator.setDuration(220);
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
        int w = dp(46);
        int h = dp(26);
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float radius = h / 2f;

trackRect.set(0, 0, w, h);
        int currentTrackColor = evaluateColor(progress, OFF_COLOR, ON_COLOR);
        trackPaint.setColor(currentTrackColor);
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint);

float thumbPadding = dpF(2f);
        float thumbRadius = radius - thumbPadding;
        float minCenterX = thumbPadding + thumbRadius;
        float maxCenterX = w - thumbPadding - thumbRadius;
        float currentCenterX = minCenterX + (maxCenterX - minCenterX) * progress;
        float centerY = h / 2f;

        canvas.drawCircle(currentCenterX, centerY, thumbRadius, thumbPaint);
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

