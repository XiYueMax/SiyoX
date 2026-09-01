

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

public class CopyIconView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF frontRect = new RectF();
    private final Path backPath = new Path();
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
        int size = dp(16);
        setMeasuredDimension(resolveSize(size, widthMeasureSpec), resolveSize(size, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        paint.setColor(iconColor);
        paint.setStrokeWidth(dpF(1.3f));

        float corner = dpF(2f);

float bLeft = w * 0.35f;
        float bTop = h * 0.12f;
        float bRight = w * 0.88f;
        float bBottom = h * 0.72f;

        float fLeft = w * 0.12f;
        float fTop = h * 0.28f;
        float fRight = w * 0.65f;
        float fBottom = h * 0.88f;

        backPath.reset();
        
        backPath.moveTo(bLeft, fTop - dpF(1f));
        backPath.lineTo(bLeft, bTop + corner);
        backPath.quadTo(bLeft, bTop, bLeft + corner, bTop);
        backPath.lineTo(bRight - corner, bTop);
        backPath.quadTo(bRight, bTop, bRight, bTop + corner);
        backPath.lineTo(bRight, bBottom - corner);
        backPath.quadTo(bRight, bBottom, bRight - corner, bBottom);
        backPath.lineTo(fRight + dpF(1f), bBottom);

        canvas.drawPath(backPath, paint);

frontRect.set(fLeft, fTop, fRight, fBottom);
        canvas.drawRoundRect(frontRect, corner, corner, paint);
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private float dpF(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
