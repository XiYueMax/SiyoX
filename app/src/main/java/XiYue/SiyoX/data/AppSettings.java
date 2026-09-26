package XiYue.SiyoX.data;
import android.content.Context;
import android.content.SharedPreferences;
public class AppSettings {
    private static final String SP_NAME = "siyox_preferences";
    private static final String KEY_CARD = "card_key";
    private static final String KEY_EXPIRE_TIME = "expire_time";
    private static final String KEY_AUTO_VERIFY = "auto_verify";
    private static final String KEY_REMEMBER_CARD = "remember_card";
    private static final String KEY_INJECTED_PACK = "injected_pack";
    private static final String KEY_DYNAMIC_ISLAND = "dynamic_island";
    private final SharedPreferences sp;
    private static volatile AppSettings instance;
    private AppSettings(Context context) {
        this.sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }
    public static AppSettings init(Context context) {
        if (instance == null) {
            synchronized (AppSettings.class) {
                if (instance == null) {
                    instance = new AppSettings(context.getApplicationContext() != null ? context.getApplicationContext() : context);
                }
            }
        }
        return instance;
    }
    public static AppSettings get() {
        if (instance == null) {
            throw new IllegalStateException("AppSettings must be initialized first");
        }
        return instance;
    }
    public String getCard() {
        return sp.getString(KEY_CARD, "");
    }
    public void setCard(String card) {
        sp.edit().putString(KEY_CARD, card).apply();
    }
    public long getExpireTime() {
        return sp.getLong(KEY_EXPIRE_TIME, 0L);
    }
    public void setExpireTime(long expireTime) {
        sp.edit().putLong(KEY_EXPIRE_TIME, expireTime).apply();
    }
    public boolean isAutoVerify() {
        return sp.getBoolean(KEY_AUTO_VERIFY, true);
    }
    public void setAutoVerify(boolean autoVerify) {
        sp.edit().putBoolean(KEY_AUTO_VERIFY, autoVerify).apply();
    }
    public boolean isRememberCard() {
        return sp.getBoolean(KEY_REMEMBER_CARD, true);
    }
    public void setRememberCard(boolean remember) {
        sp.edit().putBoolean(KEY_REMEMBER_CARD, remember).apply();
    }
    public String getInjectedPack() {
        return sp.getString(KEY_INJECTED_PACK, "");
    }
    public void setInjectedPack(String pack) {
        sp.edit().putString(KEY_INJECTED_PACK, pack != null ? pack : "").apply();
    }
    public boolean isDynamicIslandEnabled() {
        return sp.getBoolean(KEY_DYNAMIC_ISLAND, true);
    }
    public void setDynamicIslandEnabled(boolean enabled) {
        sp.edit().putBoolean(KEY_DYNAMIC_ISLAND, enabled).apply();
    }
    public int getIslandScale() {
        return sp.getInt("island_scale", 100);
    }
    public void setIslandScale(int scale) {
        sp.edit().putInt("island_scale", scale).apply();
    }

    public boolean isIslandShowDot() {
        return sp.getBoolean("island_show_dot", true);
    }
    public void setIslandShowDot(boolean show) {
        sp.edit().putBoolean("island_show_dot", show).apply();
    }

    public int getIslandDotColor() {
        return sp.getInt("island_dot_color", 0xFF0A84FF);
    }
    public void setIslandDotColor(int color) {
        sp.edit().putInt("island_dot_color", color).apply();
    }
    public int getIslandPosX() {
        return sp.getInt("island_pos_x", 0);
    }
    public void setIslandPosX(int posX) {
        sp.edit().putInt("island_pos_x", posX).apply();
    }
    public int getIslandPosY() {
        return sp.getInt("island_pos_y", 10);
    }
    public void setIslandPosY(int posY) {
        sp.edit().putInt("island_pos_y", posY).apply();
    }
    public boolean isIslandShowTime() {
        return sp.getBoolean("island_show_time", true);
    }
    public void setIslandShowTime(boolean show) {
        sp.edit().putBoolean("island_show_time", show).apply();
    }
    public boolean isIslandShowAuthor() {
        return sp.getBoolean("island_show_author", true);
    }
    public void setIslandShowAuthor(boolean show) {
        sp.edit().putBoolean("island_show_author", show).apply();
    }
    public boolean isIslandShowProgress() {
        return sp.getBoolean("island_show_progress", true);
    }
    public void setIslandShowProgress(boolean show) {
        sp.edit().putBoolean("island_show_progress", show).apply();
    }
    public boolean isWatermarkEnabled() {
        return sp.getBoolean("watermark_enabled", true);
    }
    public void setWatermarkEnabled(boolean enabled) {
        sp.edit().putBoolean("watermark_enabled", enabled).apply();
    }
    public int getIslandCornerRadius() {
        return sp.getInt("island_corner_radius", 18);
    }
    public void setIslandCornerRadius(int radius) {
        sp.edit().putInt("island_corner_radius", radius).apply();
    }
    public boolean isDevModeEnabled() {
        return sp.getBoolean("dev_mode_enabled", false);
    }
    public void setDevModeEnabled(boolean enabled) {
        sp.edit().putBoolean("dev_mode_enabled", enabled).apply();
    }

    public boolean isUniFixBypassEnabled() {
        return sp.getBoolean("unifix_bypass_enabled", true);
    }
    public void setUniFixBypassEnabled(boolean enabled) {
        sp.edit().putBoolean("unifix_bypass_enabled", enabled).apply();
    }

    public boolean isEntityKillerEnabled() {
        return sp.getBoolean("entity_killer_enabled", true);
    }
    public void setEntityKillerEnabled(boolean enabled) {
        sp.edit().putBoolean("entity_killer_enabled", enabled).apply();
    }

    public boolean isKeyDisplayEnabled() {
        return sp.getBoolean("key_display_enabled", false);
    }
    public void setKeyDisplayEnabled(boolean enabled) {
        sp.edit().putBoolean("key_display_enabled", enabled).apply();
    }

    public boolean isKeyDisplayJoystickMode() {
        return sp.getBoolean("key_display_joystick", false);
    }
    public void setKeyDisplayJoystickMode(boolean enabled) {
        sp.edit().putBoolean("key_display_joystick", enabled).apply();
    }

    public boolean isKeyDisplaySeparateSize() {
        return sp.getBoolean("key_display_separate_size", false);
    }
    public void setKeyDisplaySeparateSize(boolean separate) {
        sp.edit().putBoolean("key_display_separate_size", separate).apply();
    }

    public float getKeyDisplayAlpha() {
        return sp.getFloat("key_display_alpha", 0.85f);
    }
    public void setKeyDisplayAlpha(float alpha) {
        sp.edit().putFloat("key_display_alpha", Math.max(0f, Math.min(1.0f, alpha))).apply();
    }

    public float getKeyDisplayScale() {
        return sp.getFloat("key_display_scale", 1.0f);
    }
    public void setKeyDisplayScale(float scale) {
        sp.edit().putFloat("key_display_scale", Math.max(0.5f, Math.min(3.0f, scale))).apply();
    }

    public org.json.JSONObject getKeyDisplayLayout() {
        String raw = sp.getString("key_display_layout", "");
        if (raw == null || raw.isEmpty()) return new org.json.JSONObject();
        try {
            return new org.json.JSONObject(raw);
        } catch (Throwable t) {
            return new org.json.JSONObject();
        }
    }
    public void setKeyDisplayLayout(org.json.JSONObject layout) {
        if (layout == null) return;
        sp.edit().putString("key_display_layout", layout.toString()).apply();
    }

    public boolean isFpsDisplayEnabled() {
        return sp.getBoolean("fps_display_enabled", false);
    }
    public void setFpsDisplayEnabled(boolean enabled) {
        sp.edit().putBoolean("fps_display_enabled", enabled).apply();
    }

    public boolean isFpsDisplayChinese() {
        return sp.getBoolean("fps_display_chinese", false);
    }
    public void setFpsDisplayChinese(boolean chinese) {
        sp.edit().putBoolean("fps_display_chinese", chinese).apply();
    }
    public float getFpsDisplayAlpha() {
        return sp.getFloat("fps_display_alpha", 0.9f);
    }
    public void setFpsDisplayAlpha(float alpha) {
        sp.edit().putFloat("fps_display_alpha", Math.max(0f, Math.min(1.0f, alpha))).apply();
    }

    public float getFpsDisplayTextSize() {
        return sp.getFloat("fps_display_text_size", 15f);
    }
    public void setFpsDisplayTextSize(float size) {
        sp.edit().putFloat("fps_display_text_size", Math.max(8f, Math.min(48f, size))).apply();
    }
    public int getFpsDisplayPosX() {
        return sp.getInt("fps_display_pos_x", 600);
    }
    public int getFpsDisplayPosY() {
        return sp.getInt("fps_display_pos_y", 5);
    }
    public void setFpsDisplayPos(int x, int y) {
        sp.edit().putInt("fps_display_pos_x", x).putInt("fps_display_pos_y", y).apply();
    }

    public org.json.JSONObject getKeyTriggerLayout() {
        try {
            String raw = sp.getString("key_trigger_layout", "");
            if (raw == null || raw.trim().isEmpty()) return new org.json.JSONObject();
            return new org.json.JSONObject(raw);
        } catch (Throwable t) {
            return new org.json.JSONObject();
        }
    }
    public void setKeyTriggerLayout(org.json.JSONObject layout) {
        sp.edit().putString("key_trigger_layout", layout == null ? "" : layout.toString()).apply();
    }

    public float getKeyDisplayGap() {
        return sp.getFloat("key_display_gap", 1f);
    }
    public void setKeyDisplayGap(float gapDp) {
        sp.edit().putFloat("key_display_gap", Math.max(0f, Math.min(20f, gapDp))).apply();
    }

    public boolean isKeyEditSeparateSize() {
        return sp.getBoolean("key_edit_separate_size", true);
    }
    public void setKeyEditSeparateSize(boolean v) {
        sp.edit().putBoolean("key_edit_separate_size", v).apply();
    }

    public int getKeyDisplayPosX() {
        return sp.getInt("key_display_pos_x", 0);
    }
    public int getKeyDisplayPosY() {
        return sp.getInt("key_display_pos_y", 0);
    }
    public void setKeyDisplayPos(int x, int y) {
        sp.edit().putInt("key_display_pos_x", x).putInt("key_display_pos_y", y).apply();
    }
}
