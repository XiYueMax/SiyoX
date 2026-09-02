

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
}
