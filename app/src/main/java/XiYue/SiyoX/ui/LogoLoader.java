// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;

import XiYue.SiyoX.data.SiyoXDirManager;

public class LogoLoader {

    private static final String TAG = "SiyoX_LogoLoader";
    private static Bitmap cachedLogo = null;

    public static synchronized Bitmap getLogo(Context context) {
        if (cachedLogo != null && !cachedLogo.isRecycled()) {
            return cachedLogo;
        }

        // 1. 优先使用内置 Base64 极速解码（零依赖、跨进程 100% 成功）
        try {
            cachedLogo = LogoData.getEmbeddedLogo();
            if (cachedLogo != null) return cachedLogo;
        } catch (Throwable ignored) {}

        // 2. 从应用私有目录 /data/user/0/.../files/SiyoX/Logo.png 解码
        if (context != null) {
            try {
                File privateLogo = SiyoXDirManager.getPrivateLogoFile(context);
                if (privateLogo != null && privateLogo.exists() && privateLogo.length() > 0) {
                    cachedLogo = BitmapFactory.decodeFile(privateLogo.getAbsolutePath());
                    if (cachedLogo != null) return cachedLogo;
                }
            } catch (Throwable ignored) {}
        }

        return null;
    }
}
