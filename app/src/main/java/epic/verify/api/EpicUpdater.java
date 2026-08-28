package epic.verify.api;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 应用内更新：下载 APK 到应用私有目录（cacheDir），用 PackageInstaller 安装（无需 FileProvider）。
 * 由 MainActivity / 接入方调用，结果通过回调返回。
 */
public class EpicUpdater {

    public interface Callback {
        void onResult(boolean ok, String msg);
    }

    /** 下载 APK 并安装；url 非法、下载失败、安装失败均回调失败信息。 */
    public static void downloadAndInstall(final Context context, final String url, final int version, final Callback cb) {
        if (url == null || url.length() == 0 || !url.startsWith("http")) {
            notify(cb, false, "下载地址无效");
            return;
        }
        final File file = new File(context.getCacheDir(), "upgrade_" + version + ".apk");
        if (file.exists() && file.length() > 0) {   // 已下载过则直接安装
            installApk(context, file, cb);
            return;
        }
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(30000);
                    conn.setRequestMethod("GET");
                    InputStream in = conn.getInputStream();
                    OutputStream out = new java.io.FileOutputStream(file);
                    byte[] buf = new byte[10240];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    in.close();
                    out.close();
                    conn.disconnect();
                    if (file.length() > 0) {
                        installApk(context, file, cb);
                    } else {
                        EpicUpdater.notify(cb, false, "下载失败: 文件为空");
                    }
                } catch (final Exception e) {
                    EpicUpdater.notify(cb, false, "下载失败: " + e.getMessage());
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /** PackageInstaller 安装（API 21+）；API 19-20 回退 file:// 安装。 */
    public static void installApk(Context context, File file, Callback cb) {
        try {
            if (Build.VERSION.SDK_INT < 21) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                notify(cb, true, "已发起安装");
                return;
            }
            PackageInstaller pi = context.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params =
                    new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int id = pi.createSession(params);
            PackageInstaller.Session session = pi.openSession(id);
            OutputStream out = session.openWrite("base.apk", 0, -1);
            InputStream in = new FileInputStream(file);
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            out.close();
            InstallReceiver.setListener(new InstallReceiver.Listener() {
                public void onResult(boolean success, String msg) {
                    if (cb != null) cb.onResult(success, msg);
                }
            });
            PendingIntent pi2 = PendingIntent.getBroadcast(context, 0,
                    new Intent(context, InstallReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
            session.commit(pi2.getIntentSender());
            session.close();
        } catch (Exception e) {
            EpicUpdater.notify(cb, false, "安装失败: " + e.getMessage());
        }
    }

    private static void notify(final Callback cb, final boolean ok, final String msg) {
        if (cb == null) return;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                cb.onResult(ok, msg);
            }
        });
    }
}
