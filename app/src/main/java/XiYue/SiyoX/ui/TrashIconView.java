package XiYue.SiyoX.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class TrashIconView extends View {

    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int iconColor = Color.parseColor("#FF3B30");

    public TrashIconView(Context context) {
        this(context, null);
    }

    public TrashIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setColor(iconColor);
        strokePaint.setStrokeWidth(dp(1.6f));
    }

    public void setIconColor(int color) {
        this.iconColor = color;
        strokePaint.setColor(color);
        invalidate();
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

        float stroke = dp(1.5f);
        strokePaint.setStrokeWidth(stroke);

        float handleW = w * 0.28f;
        float handleLeft = (w - handleW) / 2f;
        float handleRight = handleLeft + handleW;
        float handleTop = h * 0.12f;
        float handleBottom = h * 0.22f;
        Path handlePath = new Path();
        handlePath.moveTo(handleLeft, handleBottom);
        handlePath.lineTo(handleLeft, handleTop);
        handlePath.lineTo(handleRight, handleTop);
        handlePath.lineTo(handleRight, handleBottom);
        canvas.drawPath(handlePath, strokePaint);

        float lidY = h * 0.24f;
        float lidLeft = w * 0.15f;
        float lidRight = w * 0.85f;
        canvas.drawLine(lidLeft, lidY, lidRight, lidY, strokePaint);

        float bodyTop = lidY + stroke;
        float bodyBottom = h * 0.88f;
        float bodyLeftTop = w * 0.24f;
        float bodyRightTop = w * 0.76f;
        float bodyLeftBot = w * 0.28f;
        float bodyRightBot = w * 0.72f;
        float cornerR = dp(2f);

        Path bodyPath = new Path();
        bodyPath.moveTo(bodyLeftTop, bodyTop);
        bodyPath.lineTo(bodyLeftBot, bodyBottom - cornerR);
        bodyPath.quadTo(bodyLeftBot, bodyBottom, bodyLeftBot + cornerR, bodyBottom);
        bodyPath.lineTo(bodyRightBot - cornerR, bodyBottom);
        bodyPath.quadTo(bodyRightBot, bodyBottom, bodyRightBot, bodyBottom - cornerR);
        bodyPath.lineTo(bodyRightTop, bodyTop);
        canvas.drawPath(bodyPath, strokePaint);

        float ribTop = bodyTop + h * 0.14f;
        float ribBottom = bodyBottom - h * 0.12f;
        float rib1X = w * 0.40f;
        float rib2X = w * 0.60f;
        canvas.drawLine(rib1X, ribTop, rib1X, ribBottom, strokePaint);
        canvas.drawLine(rib2X, ribTop, rib2X, ribBottom, strokePaint);
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                v,
                getContext().getResources().getDisplayMetrics()
        );
    }
}
