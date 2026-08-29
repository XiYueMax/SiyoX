// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SiyoXDirManager {

    private static final String TAG = "SiyoX_DirManager";

    public static void initDirectories(Context context) {
        if (context == null) return;

        try {
            String pkgName = context.getPackageName();

            // 1. 私有目录: /data/user/0/com.netease.x19/files/SiyoX/
            File privateDir = new File(context.getFilesDir(), "SiyoX");
            if (!privateDir.exists()) {
                privateDir.mkdirs();
            }

            File privateLogoFile = new File(privateDir, "Logo.png");
            if (!privateLogoFile.exists() || privateLogoFile.length() == 0) {
                extractLogoTo(context, privateLogoFile);
            }

            // 2. 外部存储作用域目录: /sdcard/Android/data/com.netease.x19/SiyoX/
            File extBaseDir = null;
            try {
                File extFiles = context.getExternalFilesDir(null);
                if (extFiles != null && extFiles.getParentFile() != null) {
                    extBaseDir = new File(extFiles.getParentFile(), "SiyoX");
                }
            } catch (Throwable ignored) {}

            if (extBaseDir == null) {
                extBaseDir = new File(Environment.getExternalStorageDirectory(), "Android/data/" + pkgName + "/SiyoX");
            }

            File resourcesDir = new File(extBaseDir, "Resources");
            File scriptDir = new File(extBaseDir, "Script");

            if (!resourcesDir.exists()) resourcesDir.mkdirs();
            if (!scriptDir.exists()) scriptDir.mkdirs();

            // 也复制一份到 Android/data/com.netease.x19/SiyoX/Resources/
            File extLogoFile = new File(resourcesDir, "Logo.png");
            if (!extLogoFile.exists() || extLogoFile.length() == 0) {
                extractLogoTo(context, extLogoFile);
            }

            Log.i(TAG, "SiyoX directories initialized successfully!");

        } catch (Throwable t) {
            Log.e(TAG, "Error initializing SiyoX directories: " + t.getMessage(), t);
        }
    }

    public static boolean extractLogoTo(Context context, File targetFile) {
        try {
            targetFile.getParentFile().mkdirs();
            InputStream is = openLogoInputStream(context);
            if (is == null) return false;

            OutputStream os = new FileOutputStream(targetFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
            os.flush();
            os.close();
            is.close();
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to extract Logo.png to " + targetFile.getAbsolutePath() + ": " + t.getMessage());
            return false;
        }
    }

    public static InputStream openLogoInputStream(Context context) {
        // 1. Try from createPackageContext
        try {
            Context modCtx = context.createPackageContext("XiYue.SiyoX", Context.CONTEXT_IGNORE_SECURITY);
            return modCtx.getAssets().open("logo.png");
        } catch (Throwable ignored) {}

        // 2. Try from module APK file
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo("XiYue.SiyoX", 0);
            if (appInfo.sourceDir != null) {
                ZipFile zip = new ZipFile(new File(appInfo.sourceDir));
                ZipEntry entry = zip.getEntry("assets/logo.png");
                if (entry == null) entry = zip.getEntry("res/drawable/logo.png");
                if (entry != null) {
                    return zip.getInputStream(entry);
                }
            }
        } catch (Throwable ignored) {}

        // 3. Try from module Resources via PM
        try {
            PackageManager pm = context.getPackageManager();
            Resources modRes = pm.getResourcesForApplication("XiYue.SiyoX");
            int resId = modRes.getIdentifier("logo", "drawable", "XiYue.SiyoX");
            if (resId != 0) {
                return modRes.openRawResource(resId);
            }
        } catch (Throwable ignored) {}

        // 4. Try current context assets/resources
        try {
            return context.getAssets().open("logo.png");
        } catch (Throwable ignored) {}

        try {
            int resId = context.getResources().getIdentifier("logo", "drawable", context.getPackageName());
            if (resId != 0) {
                return context.getResources().openRawResource(resId);
            }
        } catch (Throwable ignored) {}

        return null;
    }

    public static File getPrivateLogoFile(Context context) {
        if (context == null) return null;
        File f = new File(context.getFilesDir(), "SiyoX/Logo.png");
        if (f.exists() && f.length() > 0) return f;
        return null;
    }
}
