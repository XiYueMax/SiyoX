

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

try {
            cachedLogo = LogoData.getEmbeddedLogo();
            if (cachedLogo != null) return cachedLogo;
        } catch (Throwable ignored) {}

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
