package XiYue.SiyoX.ui;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import org.json.JSONObject;

public class KeyDisplayView extends View {
    public static final String[] KEYS = {"W", "A", "S", "D"};
    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF[] keyRects = new RectF[4];

    private final boolean[] pressed = new boolean[4];
    private float alpha = 0.85f;

    private float overallScale = 1.0f;
    public KeyDisplayView(Context context) {
        super(context);
        for (int i = 0; i < keyRects.length; i++) keyRects[i] = new RectF();
        boxPaint.setStyle(Paint.Style.FILL);
        borderPaint.setStyle(Paint.Style.STROKE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        setFocusable(false);
        setFocusableInTouchMode(false);
        setClickable(false);
    }
    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
    public void setAlphaLevel(float a) {
        this.alpha = Math.max(0f, Math.min(1.0f, a));
        invalidate();
    }
    public void setOverallScale(float scale) {
        this.overallScale = Math.max(0.5f, Math.min(3.0f, scale));
        invalidate();
    }
    public float getOverallScale() {
        return overallScale;
    }

    public void setKeyGap(float gapPx) {
        this.keyGapPx = Math.max(0f, gapPx);
        invalidate();
    }
    public float getKeyGap() {
        return keyGapPx;
    }
    public RectF getKeyRect(int index) {
        if (index < 0 || index >= keyRects.length) return null;
        return keyRects[index];
    }

    public void setKeyPressed(int index, boolean isPressed) {
        if (index < 0 || index >= pressed.length) return;
        if (pressed[index] != isPressed) {
            pressed[index] = isPressed;
            invalidate();
        }
    }
    public void clearPressed() {
        boolean changed = false;
        for (int i = 0; i < pressed.length; i++) {
            if (pressed[i]) { pressed[i] = false; changed = true; }
        }
        if (changed) invalidate();
    }
    public void applyLayout(JSONObject layout, int screenW, int screenH) {
        applyLayout(layout, screenW, screenH, 0f, 0f);
    }

    private float keyGapPx = 1f;

    private static final int[] KEY_ROW = {0, 1, 1, 1};
    private static final int[] KEY_COL = {1, 0, 1, 2};
    public void applyLayout(JSONObject layout, int screenW, int screenH, float offsetX, float offsetY) {
        float defSize = Math.max(dp(14), screenW * 0.0375f);
        final float gap = Math.max(0f, keyGapPx);
        float boxLeft = screenW * 0.05f;
        float boxTop = screenH * 0.06f;
        for (int i = 0; i < KEYS.length; i++) {
            float size = defSize * overallScale;
            float cx;
            float cy;
            JSONObject o = layout == null ? null : layout.optJSONObject(KEYS[i]);
            if (o != null && o.has("x") && o.has("y")) {

                float customSize = (float) o.optDouble("size", defSize);
                size = Math.max(dp(16), customSize * overallScale);
                cx = (float) o.optDouble("x", 0) + offsetX;
                cy = (float) o.optDouble("y", 0) + offsetY;
            } else {

                size = Math.max(dp(10), size);
                cx = boxLeft + KEY_COL[i] * (size + gap) + size / 2f + offsetX;
                cy = boxTop + KEY_ROW[i] * (size + gap) + size / 2f + offsetY;
            }
            keyRects[i].set(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f);
        }
        invalidate();
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int normalFill = Color.parseColor("#CC1C1C1E");
        int normalText = Color.parseColor("#E6FFFFFF");

        int pressedFill = Color.parseColor("#FFFFFFFF");
        int pressedBorder = Color.parseColor("#FF000000");
        int pressedText = Color.parseColor("#FF000000");
        for (int i = 0; i < keyRects.length; i++) {
            RectF r = keyRects[i];
            boolean isPressed = pressed[i];
            float radius = r.width() * 0.14f;
            boxPaint.setColor(isPressed ? pressedFill : normalFill);
            boxPaint.setAlpha((int) (255 * alpha));
            canvas.drawRoundRect(r, radius, radius, boxPaint);
            if (isPressed) {

                borderPaint.setColor(pressedBorder);
                borderPaint.setStrokeWidth(Math.max(dp(1.5f), r.width() * 0.045f));
                borderPaint.setAlpha((int) (255 * alpha));
                canvas.drawRoundRect(r, radius, radius, borderPaint);
            }
            textPaint.setColor(isPressed ? pressedText : normalText);
            textPaint.setAlpha((int) (255 * alpha));
            textPaint.setTextSize(r.width() * 0.44f);
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float baseline = r.centerY() - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(KEYS[i], r.centerX(), baseline, textPaint);
        }
    }
}
