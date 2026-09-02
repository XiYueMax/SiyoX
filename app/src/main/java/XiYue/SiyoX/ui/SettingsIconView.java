package XiYue.SiyoX.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class SettingsIconView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobHolePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int iconColor = Color.parseColor("#1C1C1E");

    public SettingsIconView(Context context) {
        this(context, null);
    }

    public SettingsIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setColor(iconColor);

        knobPaint.setStyle(Paint.Style.FILL);
        knobPaint.setColor(iconColor);

        knobHolePaint.setStyle(Paint.Style.FILL);
        knobHolePaint.setColor(Color.WHITE);
    }

    public void setIconColor(int color) {
        this.iconColor = color;
        linePaint.setColor(color);
        knobPaint.setColor(color);
        invalidate();
    }

    private int dp(float dpValue) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                getResources().getDisplayMetrics()
        );
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = dp(18);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float strokeW = dp(1.6f);
        linePaint.setStrokeWidth(strokeW);

        float padX = w * 0.12f;
        float left = padX;
        float right = w - padX;
        float knobR = dp(2.4f);
        float holeR = dp(1.1f);

        float y1 = h * 0.26f;
        float kx1 = left + (right - left) * 0.32f;
        canvas.drawLine(left, y1, right, y1, linePaint);
        canvas.drawCircle(kx1, y1, knobR, knobPaint);
        canvas.drawCircle(kx1, y1, holeR, knobHolePaint);

        float y2 = h * 0.50f;
        float kx2 = left + (right - left) * 0.72f;
        canvas.drawLine(left, y2, right, y2, linePaint);
        canvas.drawCircle(kx2, y2, knobR, knobPaint);
        canvas.drawCircle(kx2, y2, holeR, knobHolePaint);

        float y3 = h * 0.74f;
        float kx3 = left + (right - left) * 0.42f;
        canvas.drawLine(left, y3, right, y3, linePaint);
        canvas.drawCircle(kx3, y3, knobR, knobPaint);
        canvas.drawCircle(kx3, y3, holeR, knobHolePaint);
    }
}
