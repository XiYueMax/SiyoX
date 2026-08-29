// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.data;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettings {

    private static final String SP_NAME = "siyox_preferences";
    private static final String KEY_CARD = "card_key";
    private static final String KEY_EXPIRE_TIME = "expire_time";
    private static final String KEY_AUTO_VERIFY = "auto_verify";

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
}
