// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.data

import android.content.Context
import android.content.SharedPreferences

class AppSettings private constructor(context: Context) {

    private val sp: SharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

    var card: String
        get() = sp.getString(KEY_CARD, "") ?: ""
        set(value) = sp.edit().putString(KEY_CARD, value).apply()

    var expireTime: Long
        get() = sp.getLong(KEY_EXPIRE_TIME, 0L)
        set(value) = sp.edit().putLong(KEY_EXPIRE_TIME, value).apply()

    var token: String
        get() = sp.getString(KEY_TOKEN, "") ?: ""
        set(value) = sp.edit().putString(KEY_TOKEN, value).apply()

    var ignoredVersion: String
        get() = sp.getString(KEY_IGNORED_VER, "") ?: ""
        set(value) = sp.edit().putString(KEY_IGNORED_VER, value).apply()

    var announcementMd5: String
        get() = sp.getString(KEY_ANN_MD5, "") ?: ""
        set(value) = sp.edit().putString(KEY_ANN_MD5, value).apply()

    var autoVerify: Boolean
        get() = sp.getBoolean(KEY_AUTO_VERIFY, true)
        set(value) = sp.edit().putBoolean(KEY_AUTO_VERIFY, value).apply()

    var isNightVisionEnabled: Boolean
        get() = sp.getBoolean(KEY_NIGHT_VISION, false)
        set(value) = sp.edit().putBoolean(KEY_NIGHT_VISION, value).apply()

    var isXrayEnabled: Boolean
        get() = sp.getBoolean(KEY_XRAY, false)
        set(value) = sp.edit().putBoolean(KEY_XRAY, value).apply()

    companion object {
        private const val SP_NAME = "siyox_preferences"
        private const val KEY_CARD = "card_key"
        private const val KEY_EXPIRE_TIME = "expire_time"
        private const val KEY_TOKEN = "login_token"
        private const val KEY_IGNORED_VER = "ignored_version"
        private const val KEY_ANN_MD5 = "announcement_md5"
        private const val KEY_AUTO_VERIFY = "auto_verify"
        private const val KEY_NIGHT_VISION = "night_vision"
        private const val KEY_XRAY = "xray"

        @Volatile
        private var instance: AppSettings? = null

        fun init(context: Context): AppSettings {
            return instance ?: synchronized(this) {
                instance ?: AppSettings(context.applicationContext ?: context).also { instance = it }
            }
        }

        fun get(): AppSettings {
            return instance ?: throw IllegalStateException("AppSettings must be initialized first")
        }
    }
}
