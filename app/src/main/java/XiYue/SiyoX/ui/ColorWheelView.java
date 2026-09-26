package XiYue.SiyoX.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.view.MotionEvent;
import android.view.View;

public class ColorWheelView extends View {

    private final Paint wheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float centerX = 0f;
    private float centerY = 0f;
    private float radius = 0f;
    private float thumbX = 0f;
    private float thumbY = 0f;

    private int currentColor = Color.parseColor("#0A84FF");
    private OnColorChangeListener listener;

    public interface OnColorChangeListener {
        void onColorChanged(int color);
    }

    public ColorWheelView(Context context) {
        super(context);
        init();
    }

    private void init() {
        thumbStrokePaint.setStyle(Paint.Style.STROKE);
        thumbStrokePaint.setStrokeWidth(dp(2.5f));
        thumbStrokePaint.setColor(Color.WHITE);
        thumbStrokePaint.setShadowLayer(dp(2), 0, dp(1), 0x80000000);
        thumbPaint.setStyle(Paint.Style.FILL);
    }

    private float dp(float v) {
        return v * getContext().getResources().getDisplayMetrics().density;
    }

    public void setOnColorChangeListener(OnColorChangeListener listener) {
        this.listener = listener;
    }

    public int getColor() {
        return currentColor;
    }

    public void setColor(int color) {
        this.currentColor = color;
        updateThumbFromColor();
        invalidate();
    }

    private void updateThumbFromColor() {
        if (radius <= 0) return;
        float[] hsv = new float[3];
        Color.colorToHSV(currentColor, hsv);
        float hue = hsv[0];
        float sat = hsv[1];
        double rad = Math.toRadians(hue);
        float dist = sat * radius;
        thumbX = (float) (centerX + dist * Math.cos(rad));
        thumbY = (float) (centerY + dist * Math.sin(rad));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        radius = Math.min(centerX, centerY) - dp(12);

        if (radius > 0) {
            int[] colors = new int[]{
                    Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
            };
            SweepGradient sweep = new SweepGradient(centerX, centerY, colors, null);
            RadialGradient radial = new RadialGradient(centerX, centerY, radius, Color.WHITE, 0x00FFFFFF, Shader.TileMode.CLAMP);
            ComposeShader shader = new ComposeShader(sweep, radial, PorterDuff.Mode.SRC_OVER);
            wheelPaint.setShader(shader);
            updateThumbFromColor();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (radius <= 0) return;

        canvas.drawCircle(centerX, centerY, radius, wheelPaint);

        thumbPaint.setColor(currentColor);
        float thumbR = dp(8.5f);
        canvas.drawCircle(thumbX, thumbY, thumbR, thumbPaint);
        canvas.drawCircle(thumbX, thumbY, thumbR, thumbStrokePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float dx = x - centerX;
        float dy = y - centerY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > radius) {
            float ratio = radius / dist;
            dx *= ratio;
            dy *= ratio;
            dist = radius;
        }

        thumbX = centerX + dx;
        thumbY = centerY + dy;

        double angle = Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) {
            angle += 360.0;
        }

        float hue = (float) angle;
        float sat = Math.max(0f, Math.min(1.0f, dist / radius));
        currentColor = Color.HSVToColor(new float[]{hue, sat, 1.0f});

        invalidate();

        if (listener != null) {
            listener.onColorChanged(currentColor);
        }

        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        return true;
    }
}
