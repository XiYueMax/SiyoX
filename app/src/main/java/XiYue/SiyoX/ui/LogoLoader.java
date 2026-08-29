// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.ui;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LogoLoader {

    private static final String TAG = "SiyoX_LogoLoader";
    private static Bitmap cachedLogo = null;

    public static synchronized Bitmap getLogo(Context context) {
        if (cachedLogo != null && !cachedLogo.isRecycled()) {
            return cachedLogo;
        }

        if (context == null) return null;

        // 1. Try from Module Resources via PackageManager
        try {
            PackageManager pm = context.getPackageManager();
            Resources modRes = pm.getResourcesForApplication("XiYue.SiyoX");
            int resId = modRes.getIdentifier("logo", "drawable", "XiYue.SiyoX");
            if (resId != 0) {
                cachedLogo = BitmapFactory.decodeResource(modRes, resId);
                if (cachedLogo != null) return cachedLogo;
            }
        } catch (Throwable ignored) {}

        // 2. Try from createPackageContext assets
        try {
            Context modCtx = context.createPackageContext("XiYue.SiyoX", Context.CONTEXT_IGNORE_SECURITY);
            InputStream is = modCtx.getAssets().open("logo.png");
            cachedLogo = BitmapFactory.decodeStream(is);
            is.close();
            if (cachedLogo != null) return cachedLogo;
        } catch (Throwable ignored) {}

        // 3. Try reading directly from module APK path
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo("XiYue.SiyoX", 0);
            if (appInfo.sourceDir != null) {
                ZipFile zip = new ZipFile(new File(appInfo.sourceDir));
                ZipEntry entry = zip.getEntry("assets/logo.png");
                if (entry == null) entry = zip.getEntry("res/drawable/logo.png");
                if (entry != null) {
                    InputStream is = zip.getInputStream(entry);
                    cachedLogo = BitmapFactory.decodeStream(is);
                    is.close();
                    zip.close();
                    if (cachedLogo != null) return cachedLogo;
                }
                zip.close();
            }
        } catch (Throwable ignored) {}

        // 4. Try current context's own resources
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
