

package XiYue.SiyoX.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import XiYue.SiyoX.SiyoXConfig;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResourceInjector {

    private static final String TAG = "SiyoX_ResourceInjector";

public static final String TARGET_PACK_REL_PATH = "games/com.netease/resource_packs/3.9_FirstPatch_2024_res_s1_texture_647d7cd2-1f2d-5959-a82f-c1093988afd0_0_0_2";

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static class DownloadTask {
        private volatile boolean isPaused = false;
        private volatile boolean isCancelled = false;

        public void pause() {
            this.isPaused = true;
        }

        public void cancel() {
            this.isCancelled = true;
        }

        public boolean isPaused() {
            return isPaused;
        }

        public boolean isCancelled() {
            return isCancelled;
        }
    }

    public interface DownloadCallback {
        void onProgress(int percent, long currentBytes, long totalBytes);
        void onPaused();
        void onSuccess(File downloadedFile);
        void onError(String error);
    }

    public interface InjectCallback {
        void onProgress(String message);
        void onSuccess(String message);
        void onError(String error);
    }

    public interface GlobalResourceDownloadListener {
        void onDownloadProgress(String packName, int percent, long currentBytes, long totalBytes);
        void onDownloadComplete(String packName, boolean success, String message);
    }

    private static GlobalResourceDownloadListener globalDownloadListener;

    public static void setGlobalDownloadListener(GlobalResourceDownloadListener listener) {
        globalDownloadListener = listener;
    }

    public static GlobalResourceDownloadListener getGlobalDownloadListener() {
        return globalDownloadListener;
    }

public static File getTargetPackDir(Context context) {
        if (context == null) {
            return new File("/data/user/0/com.netease.x19/files/" + TARGET_PACK_REL_PATH);
        }
        return new File(context.getFilesDir(), TARGET_PACK_REL_PATH);
    }

public static File getResFilesDir(Context context) {
        File dir;
        if (context == null) {
            dir = new File("/data/user/0/com.netease.x19/files/SiyoX/ResFiles");
        } else {
            dir = new File(context.getFilesDir(), "SiyoX/ResFiles");
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

public static File getBackupDir(Context context) {
        File dir;
        if (context == null) {
            dir = new File("/data/user/0/com.netease.x19/files/SiyoX/Backup/3.9_FirstPatch");
        } else {
            dir = new File(context.getFilesDir(), "SiyoX/Backup/3.9_FirstPatch");
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

public static void ensureBackup(Context context) {
        try {
            File targetDir = getTargetPackDir(context);
            File backupDir = getBackupDir(context);
            if (targetDir.exists() && targetDir.isDirectory()) {
                File[] existingBackups = backupDir.listFiles();
                if (existingBackups == null || existingBackups.length == 0) {
                    SiyoXLogger.i(TAG, "Creating initial backup of resource pack...");
                    copyDirectory(targetDir, backupDir);
                    SiyoXLogger.i(TAG, "Initial backup completed successfully.");
                }
            }
        } catch (Throwable t) {
            SiyoXLogger.e(TAG, "Failed to ensure backup: " + t.getMessage(), t);
        }
    }

public static boolean restoreBackup(Context context) {
        try {
            File targetDir = getTargetPackDir(context);
            File backupDir = getBackupDir(context);

            if (backupDir.exists() && backupDir.isDirectory()) {
                File[] backupFiles = backupDir.listFiles();
                if (backupFiles != null && backupFiles.length > 0) {
                    
                    deleteDirectory(targetDir);
                    targetDir.mkdirs();

copyDirectory(backupDir, targetDir);

regenerateFolderMd5(targetDir);
                    return true;
                }
            }

if (targetDir.exists()) {
                deleteDirectory(targetDir);
                targetDir.mkdirs();
                return true;
            }
            return false;
        } catch (Throwable t) {
            SiyoXLogger.e(TAG, "Failed to restore backup: " + t.getMessage(), t);
            return false;
        }
    }

    public static DownloadTask downloadResource(final Context context, final String urlStr, final String fileName, final DownloadCallback callback) {
        return downloadResource(context, urlStr, fileName, null, callback);
    }

    public static DownloadTask downloadResource(final Context context, final String urlStr, final String fileName, final String expectedMd5, final DownloadCallback callback) {
        final DownloadTask task = new DownloadTask();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                InputStream is = null;
                OutputStream os = null;
                HttpURLConnection conn = null;
                File tempFile = null;
                try {
                    File resDir = getResFilesDir(context);
                    final File targetFile = new File(resDir, fileName);
                    tempFile = new File(resDir, fileName + ".tmp");

                    long existingBytes = 0;
                    if (tempFile.exists() && tempFile.isFile()) {
                        existingBytes = tempFile.length();
                    }

                    URL url = new URL(urlStr);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 SiyoX-Client/1.0");

                    if (existingBytes > 0) {
                        conn.setRequestProperty("Range", "bytes=" + existingBytes + "-");
                    }

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                            responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                            responseCode == 307 || responseCode == 308) {
                        String newUrl = conn.getHeaderField("Location");
                        conn.disconnect();
                        url = new URL(newUrl);
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(30000);
                        if (existingBytes > 0) {
                            conn.setRequestProperty("Range", "bytes=" + existingBytes + "-");
                        }
                        responseCode = conn.getResponseCode();
                    }

                    boolean isPartial = (responseCode == 206);
                    if (responseCode != HttpURLConnection.HTTP_OK && responseCode != 206) {
                        if (existingBytes > 0 && responseCode == 416) {
                            tempFile.delete();
                            existingBytes = 0;
                            conn.disconnect();
                            conn = (HttpURLConnection) url.openConnection();
                            conn.setConnectTimeout(15000);
                            conn.setReadTimeout(30000);
                            responseCode = conn.getResponseCode();
                            isPartial = false;
                        } else {
                            throw new Exception("HTTP " + responseCode + ": " + conn.getResponseMessage());
                        }
                    }

                    long serverContentLength = conn.getContentLength();
                    final long totalBytes = isPartial ? (existingBytes + serverContentLength) : (serverContentLength > 0 ? serverContentLength : -1);
                    long downloadedBytes = isPartial ? existingBytes : 0;

                    is = new BufferedInputStream(conn.getInputStream());
                    os = new BufferedOutputStream(new FileOutputStream(tempFile, isPartial));

                    byte[] buffer = new byte[8192];
                    int len;
                    long lastNotifyTime = 0;

                    while ((len = is.read(buffer)) != -1) {
                        if (task.isCancelled()) {
                            try { os.close(); is.close(); } catch (Throwable ignored) {}
                            if (tempFile.exists()) tempFile.delete();
                            return;
                        }
                        if (task.isPaused()) {
                            try { os.close(); is.close(); } catch (Throwable ignored) {}
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) callback.onPaused();
                                }
                            });
                            return;
                        }

                        os.write(buffer, 0, len);
                        downloadedBytes += len;

                        long now = System.currentTimeMillis();
                        if (now - lastNotifyTime > 100 || (totalBytes > 0 && downloadedBytes == totalBytes)) {
                            lastNotifyTime = now;
                            final int percent = totalBytes > 0 ? (int) ((downloadedBytes * 100) / totalBytes) : -1;
                            final long cur = downloadedBytes;
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) {
                                        callback.onProgress(percent, cur, totalBytes);
                                    }
                                    if (globalDownloadListener != null) {
                                        globalDownloadListener.onDownloadProgress(fileName, percent, cur, totalBytes);
                                    }
                                }
                            });
                        }
                    }

                    os.flush();
                    os.close();
                    os = null;
                    is.close();
                    is = null;

                    if (expectedMd5 != null && !expectedMd5.trim().isEmpty() && SiyoXConfig.ENABLE_RESOURCE_MD5_VERIFY) {
                        String tempMd5 = computeFileMd5(tempFile);
                        if (tempMd5 == null || !tempMd5.equalsIgnoreCase(expectedMd5.trim())) {
                            if (tempFile.exists()) {
                                tempFile.delete();
                            }
                            throw new Exception("MD5 校验不匹配 (预期: " + expectedMd5 + ", 实际: " + tempMd5 + ")");
                        }
                    }

                    if (targetFile.exists()) {
                        targetFile.delete();
                    }
                    boolean renamed = tempFile.renameTo(targetFile);
                    if (!renamed) {
                        copyFile(tempFile, targetFile);
                        tempFile.delete();
                    }

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onSuccess(targetFile);
                            }
                            if (globalDownloadListener != null) {
                                globalDownloadListener.onDownloadComplete(fileName, true, "下载完成");
                            }
                        }
                    });

                } catch (final Throwable t) {
                    if (task.isCancelled() || task.isPaused()) return;
                    SiyoXLogger.e(TAG, "Download error: " + t.getMessage(), t);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onError(t.getMessage());
                            }
                            if (globalDownloadListener != null) {
                                globalDownloadListener.onDownloadComplete(fileName, false, t.getMessage());
                            }
                        }
                    });
                } finally {
                    try { if (os != null) os.close(); } catch (Throwable ignored) {}
                    try { if (is != null) is.close(); } catch (Throwable ignored) {}
                    try { if (conn != null) conn.disconnect(); } catch (Throwable ignored) {}
                }
            }
        });
        return task;
    }

    public static boolean deleteResource(Context context, String fileName) {
        try {
            File resDir = getResFilesDir(context);
            File targetFile = new File(resDir, fileName);
            File tempFile = new File(resDir, fileName + ".tmp");
            boolean d1 = !targetFile.exists() || targetFile.delete();
            boolean d2 = !tempFile.exists() || tempFile.delete();
            return d1 && d2;
        } catch (Throwable t) {
            return false;
        }
    }

public static void injectZip(final Context context, final File zipFile, final InjectCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (zipFile == null || !zipFile.exists() || zipFile.length() == 0) {
                        throw new Exception("资源压缩包文件不存在或为空！");
                    }

                    postProgress(callback, "正在校验并备份原版资源...");
                    ensureBackup(context);

                    File targetDir = getTargetPackDir(context);
                    if (!targetDir.exists()) {
                        targetDir.mkdirs();
                    }

                    File targetContents = new File(targetDir, "contents.json");
                    if (targetContents.exists()) targetContents.delete();
                    File targetManifest = new File(targetDir, "manifest.json");
                    if (targetManifest.exists()) targetManifest.delete();

                    postProgress(callback, "正在解压材质资源文件...");

                    ZipFile zip = null;
                    try {
                        String canonicalTargetDir = targetDir.getCanonicalPath();
                        zip = new ZipFile(zipFile);
                        Enumeration<? extends ZipEntry> entries = zip.entries();

                        while (entries.hasMoreElements()) {
                            ZipEntry entry = entries.nextElement();
                            String name = entry.getName();

                            String normName = name.replace('\\', '/');

                            String lowerName = normName.toLowerCase();
                            if (lowerName.equals("contents.json") || lowerName.endsWith("/contents.json")
                                    || lowerName.equals("manifest.json") || lowerName.endsWith("/manifest.json")) {
                                SiyoXLogger.i(TAG, "Skipping filtered file in zip: " + name);
                                continue;
                            }

                            File outFile = new File(targetDir, normName);
                            String canonicalOut = outFile.getCanonicalPath();
                            if (!canonicalOut.startsWith(canonicalTargetDir + File.separator) && !canonicalOut.equals(canonicalTargetDir)) {
                                SiyoXLogger.w(TAG, "Skipping malicious zip entry: " + name);
                                continue;
                            }

                            if (entry.isDirectory()) {
                                outFile.mkdirs();
                            } else {
                                if (outFile.getParentFile() != null && !outFile.getParentFile().exists()) {
                                    outFile.getParentFile().mkdirs();
                                }

                                InputStream zis = zip.getInputStream(entry);
                                OutputStream zos = new FileOutputStream(outFile);
                                try {
                                    byte[] zbuf = new byte[8192];
                                    int zlen;
                                    while ((zlen = zis.read(zbuf)) > 0) {
                                        zos.write(zbuf, 0, zlen);
                                    }
                                    zos.flush();
                                } finally {
                                    try { zos.close(); } catch (Throwable ignored) {}
                                    try { zis.close(); } catch (Throwable ignored) {}
                                }
                            }
                        }
                    } finally {
                        if (zip != null) {
                            try { zip.close(); } catch (Throwable ignored) {}
                        }
                    }

                    postProgress(callback, "正在构建资源校验数据...");
                    regenerateFolderMd5(targetDir);

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onSuccess("材质资源注入成功！请重启游戏应用以生效。");
                            }
                        }
                    });

                } catch (final Throwable t) {
                    SiyoXLogger.e(TAG, "Inject error: " + t.getMessage(), t);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onError("注入失败: " + t.getMessage());
                            }
                        }
                    });
                }
            }
        });
    }

    private static void postProgress(final InjectCallback callback, final String msg) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onProgress(msg);
                }
            }
        });
    }

    public static void regenerateFolderMd5(File targetDir) throws Exception {
        if (targetDir == null || !targetDir.exists() || !targetDir.isDirectory()) {
            return;
        }

        Map<String, String> md5Map = new LinkedHashMap<>();
        scanDirectoryForMd5(targetDir, targetDir, md5Map);

        JSONObject json = new JSONObject();
        for (Map.Entry<String, String> e : md5Map.entrySet()) {
            json.put(e.getKey(), e.getValue());
        }

        File md5File = new File(targetDir, "folder_md5.json");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(md5File);
            fos.write(json.toString().getBytes("UTF-8"));
            fos.flush();
        } finally {
            if (fos != null) {
                try { fos.close(); } catch (Throwable ignored) {}
            }
        }

        SiyoXLogger.i(TAG, "Generated folder_md5.json with " + md5Map.size() + " entries.");
    }

    private static void scanDirectoryForMd5(File rootDir, File currentDir, Map<String, String> md5Map) {
        File[] files = currentDir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                scanDirectoryForMd5(rootDir, f, md5Map);
            } else if (f.isFile()) {
                String fileName = f.getName();
                if (fileName.equalsIgnoreCase("folder_md5.json")) {
                    continue; 
                }

                String rootPath = rootDir.getAbsolutePath();
                String filePath = f.getAbsolutePath();
                if (filePath.startsWith(rootPath)) {
                    String relPath = filePath.substring(rootPath.length());
                    if (relPath.startsWith(File.separator)) {
                        relPath = relPath.substring(1);
                    }

                    String formattedKey = relPath.replace('/', '\\');

                    String md5 = computeFileMd5(f);
                    if (md5 != null) {
                        md5Map.put(formattedKey, md5);
                    }
                }
            }
        }
    }

    public static String computeFileMd5(File file) {
        if (file == null || !file.exists() || !file.isFile()) return null;
        InputStream is = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            is = new FileInputStream(file);
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) > 0) {
                digest.update(buf, 0, len);
            }
            byte[] md5Bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : md5Bytes) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Throwable t) {
            SiyoXLogger.e(TAG, "Failed to compute MD5 for " + file.getAbsolutePath() + ": " + t.getMessage());
            return null;
        } finally {
            try { if (is != null) is.close(); } catch (Throwable ignored) {}
        }
    }

    private static void copyDirectory(File src, File dest) throws Exception {
        if (src.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdirs();
            }
            String[] children = src.list();
            if (children != null) {
                for (String child : children) {
                    copyDirectory(new File(src, child), new File(dest, child));
                }
            }
        } else {
            copyFile(src, dest);
        }
    }

    private static void copyFile(File src, File dest) throws Exception {
        if (dest.getParentFile() != null && !dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dest);
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

    private static void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteDirectory(f);
                }
            }
        }
        dir.delete();
    }
}
