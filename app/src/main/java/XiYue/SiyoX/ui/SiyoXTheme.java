

package XiYue.SiyoX.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;

public class SiyoXTheme {

    public static boolean isDarkMode(Context context) {
        if (context == null) return false;
        try {
            int mode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return mode == Configuration.UI_MODE_NIGHT_YES;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static int getWindowBg(boolean isDark) {
        return isDark ? Color.parseColor("#F2121214") : Color.parseColor("#E6000000");
    }

    public static int getCardBg(boolean isDark) {
        return isDark ? Color.parseColor("#1E1E22") : Color.parseColor("#F9FAFC");
    }

    public static int getInnerCardBg(boolean isDark) {
        return isDark ? Color.parseColor("#2A2A2E") : Color.parseColor("#FFFFFF");
    }

    public static int getSidebarBg(boolean isDark) {
        return isDark ? Color.parseColor("#252529") : Color.parseColor("#FFFFFF");
    }

    public static int getTextPrimary(boolean isDark) {
        return isDark ? Color.parseColor("#FFFFFF") : Color.parseColor("#1C1C1E");
    }

    public static int getTextSecondary(boolean isDark) {
        return isDark ? Color.parseColor("#9E9EA4") : Color.parseColor("#8E8E93");
    }

    public static int getTextSiyo(boolean isDark) {
        return isDark ? Color.parseColor("#FFFFFF") : Color.parseColor("#1C1C1E");
    }

    public static int getAccentBlue() {
        return Color.parseColor("#0A84FF");
    }

    public static int getInputBg(boolean isDark) {
        return isDark ? Color.parseColor("#2A2A2E") : Color.parseColor("#FFFFFF");
    }

    public static int getInputBorder(boolean isDark) {
        return isDark ? Color.parseColor("#3E3E44") : Color.parseColor("#E5E7EB");
    }

    public static int getInputHint(boolean isDark) {
        return isDark ? Color.parseColor("#707078") : Color.parseColor("#AEAEB2");
    }

    public static int getDivider(boolean isDark) {
        return isDark ? Color.parseColor("#36363B") : Color.parseColor("#E5E9F0");
    }

    public static int getActiveTabBg(boolean isDark) {
        return isDark ? Color.parseColor("#1A3860") : Color.parseColor("#EBF5FF");
    }

    public static int getExpireBadgeBg(boolean isDark) {
        return isDark ? Color.parseColor("#1A3860") : Color.parseColor("#EBF5FF");
    }

    public static int getExitBtnBg(boolean isDark) {
        return isDark ? Color.parseColor("#3B1D1D") : Color.parseColor("#FDE8E8");
    }
}
