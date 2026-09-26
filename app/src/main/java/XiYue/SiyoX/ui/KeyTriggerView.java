package XiYue.SiyoX.ui;
import android.content.Context;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import org.json.JSONObject;

public class KeyTriggerView extends View {

    public interface TriggerListener {
        void onKeyTriggered(int keyIndex, boolean pressed);
    }
    private final RectF[] triggerRects = new RectF[4];
    private TriggerListener listener;

    private boolean joystickMode = false;
    private float joystickCenterX = 0f;
    private float joystickCenterY = 0f;
    private float joystickRadius = 0f;
    private final boolean[] pressed = new boolean[4];

    private static final float[][] CROSS_REL = {
            {0.5f, 0.0f},
            {0.0f, 0.5f},
            {0.5f, 1.0f},
            {1.0f, 0.5f}
    };
    public KeyTriggerView(Context context) {
        super(context);
        for (int i = 0; i < triggerRects.length; i++) triggerRects[i] = new RectF();
        setFocusable(false);
        setFocusableInTouchMode(false);
        setClickable(false);
    }
    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
    public void setTriggerListener(TriggerListener l) {
        this.listener = l;
    }
    public void setJoystickMode(boolean joystick) {
        this.joystickMode = joystick;
        releaseAll();
    }
    public boolean isJoystickMode() {
        return joystickMode;
    }

    public RectF getTriggerRect(int index) {
        if (index < 0 || index >= triggerRects.length) return null;
        return triggerRects[index];
    }

    public float getDefaultTriggerSize() {
        return dp(60);
    }
    private void releaseAll() {
        for (int i = 0; i < pressed.length; i++) {
            if (pressed[i]) {
                pressed[i] = false;
                if (listener != null) listener.onKeyTriggered(i, false);
            }
        }
    }
    public void applyLayout(JSONObject layout, int screenW, int screenH) {
        float defSize = dp(60);

        float boxW = defSize * 2f;
        float boxH = defSize * 2f;

        float boxLeft = screenW * 0.04f;
        float boxTop = screenH - boxH - screenH * 0.22f;
        for (int i = 0; i < KeyDisplayView.KEYS.length; i++) {
            float size = defSize;
            float cx;
            float cy;
            JSONObject o = layout == null ? null : layout.optJSONObject(KeyDisplayView.KEYS[i]);
            if (o != null && o.has("x") && o.has("y")) {
                size = (float) o.optDouble("size", defSize);
                cx = (float) o.optDouble("x", 0);
                cy = (float) o.optDouble("y", 0);
            } else {
                cx = boxLeft + CROSS_REL[i][0] * boxW + defSize / 2f;
                cy = boxTop + CROSS_REL[i][1] * boxH + defSize / 2f;
            }
            size = Math.max(dp(24), size);
            triggerRects[i].set(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f);
        }

        float sumX = 0f, sumY = 0f;
        for (RectF r : triggerRects) {
            sumX += r.centerX();
            sumY += r.centerY();
        }
        joystickCenterX = sumX / triggerRects.length;
        joystickCenterY = sumY / triggerRects.length;
        float maxDist = 0f;
        for (RectF r : triggerRects) {
            float d = (float) Math.hypot(r.centerX() - joystickCenterX, r.centerY() - joystickCenterY);
            if (d > maxDist) maxDist = d;
        }
        joystickRadius = Math.max(dp(40), maxDist);
    }

    private void handleTouch(float x, float y) {
        boolean[] next = new boolean[4];
        if (joystickMode) {
            next = computeJoystick(x, y);
        } else {
            for (int i = 0; i < triggerRects.length; i++) {
                next[i] = triggerRects[i].contains(x, y);
            }
        }
        for (int i = 0; i < pressed.length; i++) {
            if (pressed[i] != next[i]) {
                pressed[i] = next[i];
                if (listener != null) listener.onKeyTriggered(i, next[i]);
            }
        }
    }

    private boolean[] computeJoystick(float x, float y) {
        boolean[] next = new boolean[4];
        float dx = x - joystickCenterX;
        float dy = y - joystickCenterY;
        float dist = (float) Math.hypot(dx, dy);
        if (dist < joystickRadius * 0.3f) return next;
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        if (angle >= -112.5 && angle < -67.5) {
            next[0] = true;
        } else if (angle >= -67.5 && angle < -22.5) {
            next[0] = true; next[3] = true;
        } else if (angle >= -22.5 && angle < 22.5) {
            next[3] = true;
        } else if (angle >= 22.5 && angle < 67.5) {
            next[2] = true; next[3] = true;
        } else if (angle >= 67.5 && angle < 112.5) {
            next[2] = true;
        } else if (angle >= 112.5 && angle < 157.5) {
            next[1] = true; next[2] = true;
        } else if ((angle >= 157.5 && angle <= 180) || (angle >= -180 && angle < -157.5)) {
            next[1] = true;
        } else {
            next[0] = true; next[1] = true;
        }
        return next;
    }

    public void onGlobalTouch(int action, float x, float y) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                handleTouch(x, y);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                releaseAll();
                break;
            default:
                break;
        }
    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        onGlobalTouch(event.getActionMasked(), event.getX(), event.getY());
        return false;
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
}
