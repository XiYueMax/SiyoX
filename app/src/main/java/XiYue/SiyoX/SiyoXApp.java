

package XiYue.SiyoX;

import android.app.Application;
import XiYue.SiyoX.data.AppSettings;
import XiYue.SiyoX.data.VerifyManager;

public class SiyoXApp extends Application {

    private static SiyoXApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        AppSettings.init(this);
        VerifyManager.init(this);
    }

    public static SiyoXApp getInstance() {
        return instance;
    }
}
