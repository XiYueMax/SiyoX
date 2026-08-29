// Copyright 2026, SiyoX contributors
// SPDX-License-Identifier: Apache-2.0

package XiYue.SiyoX.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

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

    // 目标资源包路径 (网易我的世界特定首发材质包目录)
    public static final String TARGET_PACK_REL_PATH = "games/com.netease/resource_packs/3.9_FirstPatch_2024_res_s1_texture_647d7cd2-1f2d-5959-a82f-c1093988afd0_0_0_2";

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface DownloadCallback {
        void onProgress(int percent, long currentBytes, long totalBytes);
        void onSuccess(File downloadedFile);
        void onError(String error);
    }

    public interface InjectCallback {
        void onProgress(String message);
        void onSuccess(String message);
        void onError(String error);
    }

    /**
     * 获取目标资源包所在目录
     */
    public static File getTargetPackDir(Context context) {
        if (context == null) {
            return new File("/data/user/0/com.netease.x19/files/" + TARGET_PACK_REL_PATH);
        }
        return new File(context.getFilesDir(), TARGET_PACK_REL_PATH);
    }

    /**
     * 获取直链下载文件的存放目录 (隐藏私有目录，不展示在UI)
     */
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

    /**
     * 获取原版资源备份目录
     */
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

    /**
     * 检查并确保已备份初始原版资源
     */
    public static void ensureBackup(Context context) {
        try {
            File targetDir = getTargetPackDir(context);
            File backupDir = getBackupDir(context);
            if (targetDir.exists() && targetDir.isDirectory()) {
                File[] existingBackups = backupDir.listFiles();
                if (existingBackups == null || existingBackups.length == 0) {
                    Log.i(TAG, "Creating initial backup of resource pack...");
                    copyDirectory(targetDir, backupDir);
                    Log.i(TAG, "Initial backup completed successfully.");
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to ensure backup: " + t.getMessage(), t);
        }
    }

    /**
     * 清空资源包：将目标目录恢复至注入前的初始原版状态
     */
    public static boolean restoreBackup(Context context) {
        try {
            File targetDir = getTargetPackDir(context);
            File backupDir = getBackupDir(context);

            if (backupDir.exists() && backupDir.isDirectory()) {
                File[] backupFiles = backupDir.listFiles();
                if (backupFiles != null && backupFiles.length > 0) {
                    // 1. 清空当前目标目录
                    deleteDirectory(targetDir);
                    targetDir.mkdirs();

                    // 2. 复制备份文件回目标目录
                    copyDirectory(backupDir, targetDir);

                    // 3. 重新生成 folder_md5.json 确保校验通过
                    regenerateFolderMd5(targetDir);
                    return true;
                }
            }

            // 若无完整备份，则清空非核心文件并重构
            if (targetDir.exists()) {
                deleteDirectory(targetDir);
                targetDir.mkdirs();
                return true;
            }
            return false;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to restore backup: " + t.getMessage(), t);
            return false;
        }
    }

    /**
     * 下载网络直链资源包 (带进度回调)
     */
    public static void downloadResource(final Context context, final String urlStr, final String fileName, final DownloadCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                InputStream is = null;
                OutputStream os = null;
                HttpURLConnection conn = null;
                try {
                    File resDir = getResFilesDir(context);
                    final File targetFile = new File(resDir, fileName);
                    File tempFile = new File(resDir, fileName + ".tmp");

                    URL url = new URL(urlStr);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 SiyoX-Client/1.0");

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
                        responseCode = conn.getResponseCode();
                    }

                    if (responseCode != HttpURLConnection.HTTP_OK && responseCode != 206) {
                        throw new Exception("HTTP " + responseCode + ": " + conn.getResponseMessage());
                    }

                    final long totalBytes = conn.getContentLength();
                    is = new BufferedInputStream(conn.getInputStream());
                    os = new BufferedOutputStream(new FileOutputStream(tempFile));

                    byte[] buffer = new byte[8192];
                    int len;
                    long downloadedBytes = 0;
                    long lastNotifyTime = 0;

                    while ((len = is.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                        downloadedBytes += len;

                        long now = System.currentTimeMillis();
                        if (now - lastNotifyTime > 100 || downloadedBytes == totalBytes) {
                            lastNotifyTime = now;
                            final int percent = totalBytes > 0 ? (int) ((downloadedBytes * 100) / totalBytes) : -1;
                            final long cur = downloadedBytes;
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) {
                                        callback.onProgress(percent, cur, totalBytes);
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

                    if (targetFile.exists()) {
                        targetFile.delete();
                    }
                    tempFile.renameTo(targetFile);

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onSuccess(targetFile);
                            }
                        }
                    });

                } catch (final Throwable t) {
                    Log.e(TAG, "Download error: " + t.getMessage(), t);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onError(t.getMessage());
                            }
                        }
                    });
                } finally {
                    try { if (os != null) os.close(); } catch (Throwable ignored) {}
                    try { if (is != null) is.close(); } catch (Throwable ignored) {}
                    if (conn != null) conn.disconnect();
                }
            }
        });
    }

    /**
     * 执行资源包注入
     * 1. 备份原版目录
     * 2. 移除 contents.json 与 manifest.json
     * 3. 解压所有资源文件并覆盖
     * 4. 重新计算并生成 folder_md5.json
     */
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

                    // 1. 删除目标目录下可能残留的 contents.json 或 manifest.json
                    File targetContents = new File(targetDir, "contents.json");
                    if (targetContents.exists()) targetContents.delete();
                    File targetManifest = new File(targetDir, "manifest.json");
                    if (targetManifest.exists()) targetManifest.delete();

                    postProgress(callback, "正在解压材质资源文件...");

                    ZipFile zip = new ZipFile(zipFile);
                    Enumeration<? extends ZipEntry> entries = zip.entries();

                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        String name = entry.getName();

                        // 统一路径分隔符
                        String normName = name.replace('\\', '/');

                        // 自动过滤/删除 contents.json 与 manifest.json，绝不解压
                        String lowerName = normName.toLowerCase();
                        if (lowerName.equals("contents.json") || lowerName.endsWith("/contents.json")
                                || lowerName.equals("manifest.json") || lowerName.endsWith("/manifest.json")) {
                            Log.i(TAG, "Skipping filtered file in zip: " + name);
                            continue;
                        }

                        File outFile = new File(targetDir, normName);
                        if (entry.isDirectory()) {
                            outFile.mkdirs();
                        } else {
                            if (outFile.getParentFile() != null && !outFile.getParentFile().exists()) {
                                outFile.getParentFile().mkdirs();
                            }

                            InputStream in = zip.getInputStream(entry);
                            OutputStream out = new FileOutputStream(outFile);
                            byte[] buf = new byte[8192];
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                            out.flush();
                            out.close();
                            in.close();
                        }
                    }
                    zip.close();

                    // 2. 再次确保目标目录没有 contents.json 和 manifest.json
                    if (targetContents.exists()) targetContents.delete();
                    if (targetManifest.exists()) targetManifest.delete();

                    postProgress(callback, "正在重新生成 folder_md5.json 校验索引...");

                    // 3. 递归扫描整个目标目录，重新生成 folder_md5.json
                    regenerateFolderMd5(targetDir);

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onSuccess("材质资源注入成功！");
                            }
                        }
                    });

                } catch (final Throwable t) {
                    Log.e(TAG, "Inject error: " + t.getMessage(), t);
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

    /**
     * 递归遍历 targetDir，计算所有文件的 MD5 (排除 folder_md5.json)，输出标准的 folder_md5.json
     */
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
        FileOutputStream fos = new FileOutputStream(md5File);
        fos.write(json.toString().getBytes("UTF-8"));
        fos.flush();
        fos.close();

        Log.i(TAG, "Generated folder_md5.json with " + md5Map.size() + " entries.");
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
                    continue; // 排除自身
                }

                // 获取相对路径
                String rootPath = rootDir.getAbsolutePath();
                String filePath = f.getAbsolutePath();
                if (filePath.startsWith(rootPath)) {
                    String relPath = filePath.substring(rootPath.length());
                    if (relPath.startsWith(File.separator)) {
                        relPath = relPath.substring(1);
                    }

                    // 网易 Minecraft 资源包规范在 folder_md5.json 中通常使用反斜杠 \\ 或正斜杠
                    // 统一替换为反斜杠 \\ 匹配官方格式 (textures\sfxs\...)
                    String formattedKey = relPath.replace('/', '\\');

                    String md5 = computeFileMd5(f);
                    if (md5 != null) {
                        md5Map.put(formattedKey, md5);
                    }
                }
            }
        }
    }

    /**
     * 计算文件的 32 位小写 MD5 Hex 字符串
     */
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
            Log.e(TAG, "Failed to compute MD5 for " + file.getAbsolutePath() + ": " + t.getMessage());
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
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
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
