package XiYue.SiyoX.ui;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;

public class FpsDisplayView extends View implements Choreographer.FrameCallback {
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean chinese = false;
    private float alpha = 0.9f;
    private volatile boolean running = false;

    private int frameCount = 0;
    private long windowStart = 0L;

    private int currentFps = 0;
    private int lastFrameFps = 0;
    public FpsDisplayView(Context context) {
        super(context);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(Color.parseColor("#99000000"));
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setColor(Color.WHITE);
        setFocusable(false);
        setFocusableInTouchMode(false);
        setClickable(false);
    }
    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
    public void setChinese(boolean zh) {
        this.chinese = zh;
        invalidate();
    }
    public void setAlphaLevel(float a) {
        this.alpha = Math.max(0.1f, Math.min(1.0f, a));
        invalidate();
    }
    public void setTextSizeSp(float sp) {
        textPaint.setTextSize(sp * getResources().getDisplayMetrics().scaledDensity);
        requestLayout();
        invalidate();
    }
    public int getCurrentFps() {
        return currentFps;
    }
    public void start() {
        if (running) return;
        running = true;
        frameCount = 0;
        windowStart = 0L;
        Choreographer.getInstance().postFrameCallback(this);
    }
    public void stop() {
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
    }
    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) return;
        if (windowStart == 0L) {
            windowStart = frameTimeNanos;
            frameCount = 0;
        } else {
            frameCount++;
            long elapsedMs = (frameTimeNanos - windowStart) / 1_000_000L;

            if (elapsedMs >= 1000L) {
                lastFrameFps = Math.round(frameCount * 1000f / elapsedMs);
                frameCount = 0;
                windowStart = frameTimeNanos;
                post(new Runnable() {
                    @Override
                    public void run() {
                        currentFps = lastFrameFps;
                        requestLayout();
                        invalidate();
                    }
                });
            }
        }
        Choreographer.getInstance().postFrameCallback(this);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        return false;
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        String label = buildLabel();
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textW = textPaint.measureText(label);
        float textH = fm.descent - fm.ascent;
        int w = (int) Math.ceil(textW + dp(14));
        int h = (int) Math.ceil(textH + dp(8));
        setMeasuredDimension(w, h);
    }
    private String buildLabel() {
        return (chinese ? "帧率：" : "FPS：") + currentFps;
    }

    private static final float LOW_ALPHA_THRESHOLD = 0.45f;
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        bgPaint.setAlpha((int) (255 * alpha * 0.6f));
        float radius = dp(6);
        canvas.drawRoundRect(0, 0, getWidth(), getHeight(), radius, radius, bgPaint);

        textPaint.setColor(alpha < LOW_ALPHA_THRESHOLD ? Color.BLACK : Color.WHITE);
        textPaint.setAlpha((int) (255 * Math.max(alpha, 0.75f)));
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = getHeight() / 2f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(buildLabel(), dp(7), baseline, textPaint);
    }
}
