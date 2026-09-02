package XiYue.SiyoX.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import XiYue.SiyoX.SiyoXConfig;

public class LoginVideoManager {

    private static final String TAG = "SiyoX_LoginVideo";
    private static volatile LoginVideoManager instance;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean isRunning = false;
    private LoginVideoListener globalListener;

    public interface LoginVideoListener {
        void onProgress(int percent, String status);
        void onComplete(boolean success, String message);
    }

    private LoginVideoManager() {}

    public static LoginVideoManager get() {
        if (instance == null) {
            synchronized (LoginVideoManager.class) {
                if (instance == null) {
                    instance = new LoginVideoManager();
                }
            }
        }
        return instance;
    }

    public void setGlobalListener(LoginVideoListener listener) {
        this.globalListener = listener;
    }

    public LoginVideoListener getGlobalListener() {
        return this.globalListener;
    }

    public static File getLoginVideoDir(Context context) {
        File dir;
        if (context == null) {
            dir = new File("/data/user/0/com.netease.x19/files/SiyoX/LoginVideo");
        } else {
            dir = new File(context.getFilesDir(), "SiyoX/LoginVideo");
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getTargetLoginVideoFile(Context context) {
        File dir;
        if (context == null) {
            dir = new File("/data/user/0/com.netease.x19/files/games/com.netease/storge/asset");
        } else {
            dir = new File(context.getFilesDir(), "games/com.netease/storge/asset");
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "loginVideoNew.mp4");
    }

    public void checkAndStartLoginVideo(final Context context) {
        if (!SiyoXConfig.ENABLE_LOGIN_VIDEO_REPLACE) {
            return;
        }
        final String videoUrl = SiyoXConfig.LOGIN_VIDEO_URL;
        if (videoUrl == null || videoUrl.trim().isEmpty() || videoUrl.contains("example.com")) {
            return;
        }
        if (isRunning) {
            return;
        }

        isRunning = true;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                InputStream is = null;
                OutputStream os = null;
                File tempFile = null;
                try {
                    notifyProgress(0, "正在连接登录视频直链...");
                    File downloadDir = getLoginVideoDir(context);
                    tempFile = new File(downloadDir, "loginVideo_temp.mp4");
                    File localCachedFile = new File(downloadDir, "loginVideoNew.mp4");

                    URL url = new URL(videoUrl.trim());
                    int redirectCount = 0;
                    while (redirectCount < 5) {
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(30000);
                        conn.setInstanceFollowRedirects(true);
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android)");
                        conn.connect();

                        int responseCode = conn.getResponseCode();
                        if (responseCode == 301 || responseCode == 302 || responseCode == 303 || responseCode == 307 || responseCode == 308) {
                            String redirectUrl = conn.getHeaderField("Location");
                            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                                conn.disconnect();
                                url = new URL(redirectUrl);
                                redirectCount++;
                                continue;
                            }
                        }
                        break;
                    }

                    long totalBytes = conn.getContentLength();
                    is = conn.getInputStream();
                    os = new FileOutputStream(tempFile);
                    byte[] buffer = new byte[8192];
                    int read;
                    long downloaded = 0;
                    long lastUpdateTime = 0;

                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                        downloaded += read;
                        long now = System.currentTimeMillis();
                        if (totalBytes > 0 && (now - lastUpdateTime > 80 || downloaded == totalBytes)) {
                            lastUpdateTime = now;
                            int percent = (int) (downloaded * 100 / totalBytes);
                            notifyProgress(percent, "正在下载登录视频 (" + percent + "%)");
                        }
                    }

                    os.flush();
                    os.close();
                    os = null;
                    is.close();
                    is = null;
                    conn.disconnect();
                    conn = null;

                    if (!tempFile.exists() || tempFile.length() == 0) {
                        throw new Exception("下载的视频文件为空");
                    }

                    if (localCachedFile.exists()) {
                        localCachedFile.delete();
                    }
                    if (!tempFile.renameTo(localCachedFile)) {
                        copyFile(tempFile, localCachedFile);
                        tempFile.delete();
                    }

                    notifyProgress(95, "正在替换游戏登录视频...");
                    File targetVideo = getTargetLoginVideoFile(context);
                    if (targetVideo.exists()) {
                        targetVideo.delete();
                    }
                    copyFile(localCachedFile, targetVideo);

                    notifyProgress(100, "登录视频替换成功");
                    notifyComplete(true, "登录视频已替换为自定义视频");
                    SiyoXLogger.i(TAG, "Login video replaced successfully to " + targetVideo.getAbsolutePath());

                } catch (final Throwable t) {
                    SiyoXLogger.e(TAG, "Failed to download and replace login video: " + t.getMessage(), t);
                    notifyComplete(false, "登录视频替换失败: " + t.getMessage());
                } finally {
                    try { if (os != null) os.close(); } catch (Throwable ignored) {}
                    try { if (is != null) is.close(); } catch (Throwable ignored) {}
                    try { if (conn != null) conn.disconnect(); } catch (Throwable ignored) {}
                    isRunning = false;
                }
            }
        });
    }

    private static void copyFile(File src, File dst) throws Exception {
        if (dst.getParentFile() != null && !dst.getParentFile().exists()) {
            dst.getParentFile().mkdirs();
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
        } finally {
            try { if (out != null) out.close(); } catch (Throwable ignored) {}
            try { if (in != null) in.close(); } catch (Throwable ignored) {}
        }
    }

    private void notifyProgress(final int percent, final String status) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (globalListener != null) {
                    globalListener.onProgress(percent, status);
                }
            }
        });
    }

    private void notifyComplete(final boolean success, final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (globalListener != null) {
                    globalListener.onComplete(success, message);
                }
            }
        });
    }
}
