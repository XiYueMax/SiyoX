// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.InputStream;

import XiYue.SiyoX.data.SiyoXDirManager;

public class LogoLoader {

    private static final String TAG = "SiyoX_LogoLoader";
    private static Bitmap cachedLogo = null;

    public static synchronized Bitmap getLogo(Context context) {
        if (cachedLogo != null && !cachedLogo.isRecycled()) {
            return cachedLogo;
        }

        if (context == null) return null;

        // 1. 优先从私有目录 /data/user/0/com.netease.x19/files/SiyoX/Logo.png 读取
        try {
            File privateLogo = SiyoXDirManager.getPrivateLogoFile(context);
            if (privateLogo != null && privateLogo.exists() && privateLogo.length() > 0) {
                cachedLogo = BitmapFactory.decodeFile(privateLogo.getAbsolutePath());
                if (cachedLogo != null) return cachedLogo;
            } else {
                // 如果文件尚未提取，尝试提取一次
                SiyoXDirManager.initDirectories(context);
                privateLogo = SiyoXDirManager.getPrivateLogoFile(context);
                if (privateLogo != null && privateLogo.exists() && privateLogo.length() > 0) {
                    cachedLogo = BitmapFactory.decodeFile(privateLogo.getAbsolutePath());
                    if (cachedLogo != null) return cachedLogo;
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error loading logo from private files dir: " + t.getMessage());
        }

        // 2. 从输入流读取
        try {
            InputStream is = SiyoXDirManager.openLogoInputStream(context);
            if (is != null) {
                cachedLogo = BitmapFactory.decodeStream(is);
                is.close();
                if (cachedLogo != null) return cachedLogo;
            }
        } catch (Throwable ignored) {}

        // 3. 从系统资源中获取
        try {
            int resId = context.getResources().getIdentifier("logo", "drawable", context.getPackageName());
            if (resId != 0) {
                cachedLogo = BitmapFactory.decodeResource(context.getResources(), resId);
                if (cachedLogo != null) return cachedLogo;
            }
        } catch (Throwable ignored) {}

        return null;
    }
}
