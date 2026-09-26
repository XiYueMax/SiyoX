package XiYue.SiyoX.data;

import android.os.FileObserver;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import XiYue.SiyoX.SiyoXConfig;
import XiYue.SiyoX.SiyoXEntityKillerConfig;

public final class EntityKillerManager {
    private static final String TAG = "SiyoX_EntityKiller";
    private static final Object LOCK = new Object();
    private static final Map<String, FileObserver> observers = new ConcurrentHashMap<>();
    private static volatile boolean started;
    private static Thread sweeperThread;

    private static final ExecutorService sWorkerPool = Executors.newFixedThreadPool(
            Math.max(2, Math.min(Runtime.getRuntime().availableProcessors(), 4)),
            r -> {
                Thread t = new Thread(r, "SiyoX-EntityKiller-Worker");
                t.setDaemon(true);
                return t;
            }
    );

    private static volatile String sCachedPatternString = null;
    private static volatile List<Pattern> sCachedCompiledPatterns = null;
    private static volatile Pattern sCachedCompositePattern = null;

    private EntityKillerManager() {}

    public static boolean isEnabled() {
        if (!SiyoXEntityKillerConfig.ENABLE_CUSTOM_ENTITY_KILLER && !SiyoXConfig.ENABLE_ENTITY_KILLER) {
            return false;
        }
        try {
            return AppSettings.get().isEntityKillerEnabled();
        } catch (Throwable t) {
            return true;
        }
    }

    public static void setEnabled(boolean enabled) {
        try {
            AppSettings.get().setEntityKillerEnabled(enabled);
        } catch (Throwable ignored) {}
        if (enabled && (SiyoXEntityKillerConfig.ENABLE_CUSTOM_ENTITY_KILLER || SiyoXConfig.ENABLE_ENTITY_KILLER)) {
            start();
        } else {
            stop();
        }
    }

    public static void start() {
        if (!isEnabled()) {
            SiyoXLogger.i(TAG, "EntityKiller 已被设置禁用，跳过启动");
            return;
        }
        synchronized (LOCK) {
            if (started) return;
            started = true;
        }
        new Thread(EntityKillerManager::applyConfig, "SiyoX-EntityKiller-Init").start();
        startSweeperDaemon();
    }

    public static void stop() {
        synchronized (LOCK) {
            started = false;
            stopObserversLocked();
            stopSweeperDaemonLocked();
        }
        SiyoXLogger.i(TAG, "EntityKiller 已停止监听并释放资源");
    }

    public static void restart() {
        stop();
        synchronized (LOCK) {
            sCachedPatternString = null;
            sCachedCompiledPatterns = null;
            sCachedCompositePattern = null;
        }
        if (isEnabled()) {
            start();
        }
    }

    public static int cleanAllNow() {
        long startTime = System.currentTimeMillis();

        try {
            healCorruptedPackcache();
        } catch (Throwable t) {
            SiyoXLogger.w(TAG, "healCorruptedPackcache 异常: " + t.getMessage());
        }

        List<Pattern> patterns = getCompiledPatterns();
        List<File> dirs = getTargetDirectories();
        SiyoXLogger.i(TAG, "开始高性能并发清理 Entity，目标根目录数: " + dirs.size() + ", 规则数: " + patterns.size());

        List<File> packDirs = new ArrayList<>();
        for (File dir : dirs) {
            if (dir.exists() && dir.isDirectory()) {
                File[] children = dir.listFiles();
                if (children != null) {
                    for (File child : children) {
                        if (child.isDirectory()) {
                            packDirs.add(child);
                        }
                    }
                }
            }
        }

        int total = 0;
        if (!packDirs.isEmpty()) {
            List<Future<Integer>> futures = new ArrayList<>();
            for (File pack : packDirs) {
                futures.add(sWorkerPool.submit(() -> cleanDirectory(pack, patterns, true )));
            }
            for (Future<Integer> f : futures) {
                try {
                    total += f.get();
                } catch (Throwable ignored) {}
            }
        } else {
            for (File dir : dirs) {
                if (dir.exists() && dir.isDirectory()) {
                    total += cleanDirectory(dir, patterns, true );
                }
            }
        }

        long cost = System.currentTimeMillis() - startTime;
        SiyoXLogger.i(TAG, "手动执行清理完成，本次总计清除: " + total + " 个项目，耗时: " + cost + "ms");
        return total;
    }

    private static void startSweeperDaemon() {
        synchronized (LOCK) {
            if (sweeperThread != null && sweeperThread.isAlive()) return;
            sweeperThread = new Thread(() -> {
                SiyoXLogger.i(TAG, "EntityKiller 后台巡检守护线程已启动");
                while (started && isEnabled()) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        break;
                    }
                    if (!started || !isEnabled()) break;
                    try {
                        List<Pattern> patterns = getCompiledPatterns();
                        if (!patterns.isEmpty()) {
                            List<File> targetDirs = getTargetDirectories();
                            int sweepCleaned = 0;
                            for (File dir : targetDirs) {
                                if (dir.exists() && dir.isDirectory()) {
                                    sweepCleaned += cleanDirectory(dir, patterns, false );
                                }
                            }
                            if (sweepCleaned > 0) {
                                SiyoXLogger.i(TAG, "后台巡检自动清理了 " + sweepCleaned + " 个网易实体项目");
                            }
                        }
                    } catch (Throwable ignored) {}
                }
                SiyoXLogger.i(TAG, "EntityKiller 后台巡检守护线程已退出");
            }, "SiyoX-EntityKiller-Sweeper");
            sweeperThread.setDaemon(true);
            sweeperThread.start();
        }
    }

    private static void stopSweeperDaemonLocked() {
        if (sweeperThread != null) {
            sweeperThread.interrupt();
            sweeperThread = null;
        }
    }

    private static void applyConfig() {
        if (!isEnabled()) return;

        try {
            healCorruptedPackcache();
        } catch (Throwable t) {
            SiyoXLogger.w(TAG, "启动时自动修复缓存包异常: " + t.getMessage());
        }

        List<Pattern> patterns = getCompiledPatterns();
        List<File> dirs = getTargetDirectories();
        SiyoXLogger.i(TAG, "EntityKiller 开始扫描，目标根目录数: " + dirs.size() + ", 规则数: " + patterns.size());

        int initialCleaned = 0;
        for (File dir : dirs) {
            initialCleaned += cleanDirectory(dir, patterns, false );
            startFileObserverRecursive(dir, patterns);
        }
        SiyoXLogger.i(TAG, "EntityKiller 初始扫描完成，已清理: " + initialCleaned + " 个项目");
    }

    public static List<File> getTargetDirectories() {
        Set<String> addedCanonical = new HashSet<>();
        List<File> result = new ArrayList<>();

        String[] baseCandidates = new String[] {
            "/data/data/com.netease.x19/files/games/com.netease",
            "/data/user/0/com.netease.x19/files/games/com.netease",
            "/sdcard/Android/data/com.netease.x19/files/games/com.netease",
            "/storage/emulated/0/Android/data/com.netease.x19/files/games/com.netease"
        };

        String[] subCandidates = new String[] {
            "/resource_packs",
            "/packcache",
            "/world_resource_packs"
        };

        for (String base : baseCandidates) {
            for (String sub : subCandidates) {
                File dir = new File(base + sub);
                try {
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    if (dir.exists() && isAllowedPath(dir)) {
                        String canon;
                        try {
                            canon = dir.getCanonicalPath();
                        } catch (Throwable ignored) {
                            canon = dir.getAbsolutePath();
                        }
                        if (addedCanonical.add(canon)) {
                            result.add(dir);
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }
        return result;
    }

    public static boolean isAllowedPath(File dir) {
        if (dir == null) return false;
        String path = dir.getAbsolutePath().replace('\\', '/').toLowerCase();
        return (path.contains("/games/com.netease/resource_packs")
                || path.contains("/games/com.netease/packcache")
                || path.contains("/games/com.netease/world_resource_packs"))
                && !path.contains("/vanilla/") && !path.contains("/siyox/");
    }

    public static boolean isRootTargetDir(File file) {
        if (file == null) return true;
        String name = file.getName().toLowerCase();
        return name.equals("resource_packs")
                || name.equals("packcache")
                || name.equals("world_resource_packs")
                || name.equals("com.netease")
                || name.equals("games")
                || name.equals("files");
    }

    public static boolean isPackRootDir(File file) {
        if (file == null || !file.isDirectory()) return false;
        File parent = file.getParentFile();
        if (parent == null) return false;
        return isRootTargetDir(parent);
    }

    public static boolean isEntityTargetDirectory(File file) {
        if (file == null || !file.isDirectory() || isProtectedFile(file) || isRootTargetDir(file) || isPackRootDir(file)) {
            return false;
        }
        String name = file.getName().toLowerCase();
        String path = file.getAbsolutePath().replace('\\', '/').toLowerCase();

        if (name.equals("attachables") || name.equals("attachable")) {
            return true;
        }

        if (name.equals("animations")) {
            return true;
        }

        if (name.equals("animation_controllers")) {
            return true;
        }

        if (name.equals("render_controllers")) {
            return true;
        }

        if (name.equals("entity") || name.equals("entities")) {
            return true;
        }

        if (name.equals("entity") && path.contains("/models/")) {
            return true;
        }

        if (name.equals("models") && !new File(file, "blocks").exists()) {
            return true;
        }

        if (name.equals("entity") && path.contains("/textures/")) {
            return true;
        }

        return false;
    }

    public static boolean isActivelyDownloading(File file) {
        if (file == null || !file.exists()) return false;
        String name = file.getName().toLowerCase();
        if (name.endsWith(".tmp") || name.endsWith(".download") || name.endsWith(".crdownload") || name.contains(".temp")) {
            return true;
        }
        long now = System.currentTimeMillis();
        try {
            long lastMod = file.lastModified();
            if (lastMod > 0 && Math.abs(now - lastMod) < 1500L) {
                return true;
            }
        } catch (Throwable ignored) {}

        return false;
    }

    public static boolean shouldPruneDirectory(File dir) {
        if (dir == null) return false;
        String name = dir.getName().toLowerCase();
        if (name.equals("blocks") || name.equals("terrain_texture") || name.equals("colormap")
                || name.equals("sounds") || name.equals("texts") || name.equals("materials")
                || name.equals("font") || name.equals("ugc_image") || name.equals("clientcache")
                || name.equals("ccmini") || name.equals("minecraftworlds") || name.equals("exportworlds")) {
            return true;
        }
        return false;
    }

    public static boolean isProtectedFile(File file) {
        if (file == null) return true;
        String name = file.getName().toLowerCase();

        if (name.equals("manifest.json")
                || name.equals("pack_icon.png")
                || name.equals("pack_manifest.json")
                || name.equals("contents.json")
                || name.equals("terrain_texture.json")
                || name.equals("item_texture.json")
                || name.equals("flipbook_textures.json")
                || name.equals("textures_list.json")
                || name.equals("blocks.json")
                || name.equals("biomes_client.json")
                || name.equals("settings_common.json")
                || name.equals("ui_common.json")
                || name.equals("start_screen.json")
                || name.equals("hud_screen.json")
                || name.equals("sound_definitions.json")
                || name.equals("sounds.json")) {
            return true;
        }

        if (name.endsWith(".material") || name.endsWith(".vertex") || name.endsWith(".fragment")
                || name.endsWith(".lang") || name.endsWith(".properties") || name.endsWith(".loc")) {
            return true;
        }

        if (name.equals("ugc_image") || name.equals("clientcache") || name.equals("ccmini")
                || name.equals("storage") || name.equals("storge") || name.equals("minecraftworlds")
                || name.equals("exportworlds")) {
            return true;
        }

        String absPath = file.getAbsolutePath();
        if (absPath == null) return false;
        String lower = absPath.toLowerCase().replace('\\', '/');

        if (lower.contains("/siyox/")) return true;

        if (lower.contains("/vanilla/") || lower.contains("/minecraftpe/")) return true;

        String targetRel = ResourceInjector.TARGET_PACK_REL_PATH.toLowerCase().replace('\\', '/');
        if (lower.contains(targetRel) || lower.contains("3.9_firstpatch_2024_res_s1_texture_647d7cd2-1f2d-5959-a82f-c1093988afd0_0_0_2")) {
            return true;
        }

        if (lower.contains("/models/blocks/") || lower.endsWith("/models/blocks")) {
            return true;
        }

        if (lower.contains("/materials/") || lower.endsWith("/materials")
                || lower.contains("/texts/") || lower.endsWith("/texts")
                || lower.contains("/sounds/") || lower.endsWith("/sounds")
                || lower.contains("/font/") || lower.endsWith("/font")
                || lower.contains("/textures/blocks/")
                || lower.contains("/textures/terrain_texture/")
                || lower.contains("/textures/colormap/")) {
            return true;
        }

        return false;
    }

    public static int cleanDirectory(File dir, List<Pattern> patterns) {
        return cleanDirectory(dir, patterns, false);
    }

    public static int cleanDirectory(File dir, List<Pattern> patterns, boolean isManual) {
        int cleaned = 0;
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return cleaned;
        }

        if (shouldPruneDirectory(dir)) {
            return cleaned;
        }

        if (isProtectedFile(dir)) {
            return cleaned;
        }

        if (!isManual && isActivelyDownloading(dir)) {
            return cleaned;
        }

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) return cleaned;

        for (File file : files) {
            if (isProtectedFile(file)) continue;
            if (!isManual && isActivelyDownloading(file)) continue;

            if (file.isDirectory()) {

                if (shouldPruneDirectory(file)) {
                    continue;
                }

                if (isEntityTargetDirectory(file)) {
                    int count = deleteRecursive(file);
                    if (count > 0) {
                        cleaned += count;
                        SiyoXLogger.i(TAG, "已清理目标实体目录: " + getRelativePathFromPack(file) + " (包含 " + count + " 个文件)");
                        continue;
                    }
                }

                if (!isRootTargetDir(file) && !isPackRootDir(file) && !isProtectedFile(file) && shouldDelete(file, patterns, isManual)) {
                    int count = deleteRecursive(file);
                    if (count > 0) {
                        cleaned += count;
                        SiyoXLogger.i(TAG, "已清理目标自定义实体目录: " + getRelativePathFromPack(file) + " (包含 " + count + " 个文件)");
                        continue;
                    }
                }

                cleaned += cleanDirectory(file, patterns, isManual);
            } else {
                if (shouldDelete(file, patterns, isManual)) {
                    if (file.delete()) {
                        cleaned++;
                    }
                }
            }
        }
        return cleaned;
    }

    public static String getRelativePathFromPack(File file) {
        if (file == null) return "";
        String full = file.getAbsolutePath().replace('\\', '/');
        int idx = full.indexOf("/resource_packs/");
        if (idx < 0) idx = full.indexOf("/packcache/");
        if (idx < 0) idx = full.indexOf("/world_resource_packs/");
        if (idx >= 0) {
            int firstSlash = full.indexOf('/', idx + 1);
            if (firstSlash >= 0) {
                int secondSlash = full.indexOf('/', firstSlash + 1);
                if (secondSlash >= 0 && secondSlash + 1 < full.length()) {
                    return full.substring(secondSlash + 1);
                }
            }
        }
        return file.getName();
    }

    public static boolean shouldDelete(File file, List<Pattern> patterns) {
        return shouldDelete(file, patterns, false);
    }

    public static boolean shouldDelete(File file, List<Pattern> patterns, boolean isManual) {
        if (file == null || isRootTargetDir(file) || isPackRootDir(file) || isProtectedFile(file)) {
            return false;
        }
        if (!isManual && isActivelyDownloading(file)) {
            return false;
        }

        String name = file.getName();

        Pattern composite = getCompositePattern();
        if (composite != null) {
            try {

                if (name != null && composite.matcher(name).find()) {
                    return true;
                }

                String relPath = getRelativePathFromPack(file);
                if (relPath != null && composite.matcher(relPath).find()) {
                    return true;
                }
                return false;
            } catch (Throwable ignored) {}
        }

        String relPath = getRelativePathFromPack(file);
        for (Pattern p : patterns) {
            try {
                if ((name != null && p.matcher(name).find()) || (relPath != null && p.matcher(relPath).find())) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    private static void startFileObserverRecursive(final File dir, final List<Pattern> patterns) {
        if (shouldPruneDirectory(dir) || isProtectedFile(dir)) return;
        if (!dir.exists() && !dir.mkdirs()) return;
        startFileObserver(dir, patterns);

        File[] subDirs = dir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                if (!shouldPruneDirectory(subDir) && !isProtectedFile(subDir)) {
                    startFileObserverRecursive(subDir, patterns);
                }
            }
        }
    }

    private static void startFileObserver(final File dir, final List<Pattern> patterns) {
        if (!dir.exists() || !dir.isDirectory() || shouldPruneDirectory(dir) || isProtectedFile(dir)) return;
        String resolvedPath;
        try {
            resolvedPath = dir.getCanonicalPath();
        } catch (Throwable ignored) {
            resolvedPath = dir.getAbsolutePath();
        }
        final String dirPath = resolvedPath;
        synchronized (LOCK) {
            if (observers.containsKey(dirPath)) return;
        }

        FileObserver observer = new FileObserver(dirPath, FileObserver.MOVED_TO | FileObserver.CLOSE_WRITE) {
            @Override
            public void onEvent(int event, String path) {
                if (path == null || path.isEmpty()) return;
                File file = new File(dir, path);
                if (isProtectedFile(file) || isActivelyDownloading(file)) return;

                List<Pattern> curPatterns = getCompiledPatterns();

                if (file.isDirectory()) {
                    if (shouldPruneDirectory(file)) return;
                    if (isEntityTargetDirectory(file) || (!isRootTargetDir(file) && !isPackRootDir(file) && shouldDelete(file, curPatterns, false))) {
                        int count = deleteRecursive(file);
                        if (count > 0) {
                            SiyoXLogger.i(TAG, "实时监听清理目标目录: " + getRelativePathFromPack(file) + " (包含 " + count + " 个文件)");
                            return;
                        }
                    }
                    cleanDirectory(file, curPatterns, false);
                    startFileObserverRecursive(file, curPatterns);
                } else {
                    if (shouldDelete(file, curPatterns, false)) {
                        if (file.delete()) {
                            SiyoXLogger.d(TAG, "实时监听清理目标文件: " + getRelativePathFromPack(file));
                        }
                    }
                }
            }
        };

        synchronized (LOCK) {
            observers.put(dirPath, observer);
        }
        try {
            observer.startWatching();
        } catch (Throwable t) {
            SiyoXLogger.w(TAG, "启动 FileObserver 失败: " + dir.getName());
        }
    }

    public static void healCorruptedPackcache() {
        List<File> dirs = getTargetDirectories();
        for (File dir : dirs) {
            String dirName = dir.getName().toLowerCase();
            if (!dirName.equals("packcache")) continue;
            File[] packs = dir.listFiles();
            if (packs == null) continue;
            for (File pack : packs) {
                if (!pack.isDirectory() || isProtectedFile(pack) || isActivelyDownloading(pack)) {
                    continue;
                }
                boolean corrupted = false;
                String packName = pack.getName().toLowerCase();

                File texturesDir = new File(pack, "textures");
                if (texturesDir.exists() && texturesDir.isDirectory()) {
                    File terrainJson = new File(texturesDir, "terrain_texture.json");
                    File blocksDir = new File(texturesDir, "blocks");
                    if ((blocksDir.exists() || packName.contains("customblocks") || packName.contains("baseblocks")) && !terrainJson.exists()) {
                        corrupted = true;
                    }
                }

                if (corrupted) {
                    SiyoXLogger.w(TAG, "自动清除受损的服务器缓存包（修复资源包加载失败，使客户端可重新完整下载）: " + pack.getAbsolutePath());
                    deleteRecursive(pack);
                }
            }
        }
    }

    private static void stopObserversLocked() {
        for (FileObserver observer : observers.values()) {
            try {
                observer.stopWatching();
            } catch (Throwable ignored) {}
        }
        observers.clear();
    }

    private static int deleteRecursive(File file) {
        if (file == null || !file.exists()) return 0;
        int count = 0;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    count += deleteRecursive(child);
                }
            }
        }
        if (file.delete()) {
            count++;
        }
        return count;
    }

    private static List<String> readStrings(String value) {
        List<String> result = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            value = SiyoXEntityKillerConfig.getEffectivePatterns();
        }
        for (String s : value.split(",")) {
            if (!s.trim().isEmpty()) {
                result.add(s.trim());
            }
        }
        return result;
    }

    public static List<Pattern> getCompiledPatterns() {
        String currentStr = SiyoXEntityKillerConfig.getEffectivePatterns();
        if (sCachedCompiledPatterns != null && currentStr != null && currentStr.equals(sCachedPatternString)) {
            return sCachedCompiledPatterns;
        }
        synchronized (LOCK) {
            if (sCachedCompiledPatterns != null && currentStr != null && currentStr.equals(sCachedPatternString)) {
                return sCachedCompiledPatterns;
            }
            sCachedCompiledPatterns = compilePatterns(currentStr);
            sCachedCompositePattern = buildCompositePattern(sCachedCompiledPatterns);
            sCachedPatternString = currentStr;
            return sCachedCompiledPatterns;
        }
    }

    public static Pattern getCompositePattern() {
        getCompiledPatterns();
        return sCachedCompositePattern;
    }

    private static Pattern buildCompositePattern(List<Pattern> patterns) {
        if (patterns == null || patterns.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        sb.append("(?i)(");
        boolean first = true;
        for (Pattern p : patterns) {
            if (p == null) continue;
            if (!first) {
                sb.append('|');
            }
            sb.append("(?:").append(p.pattern()).append(')');
            first = false;
        }
        sb.append(')');
        try {
            return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
        } catch (Throwable t) {
            return null;
        }
    }

    private static List<Pattern> compilePatterns(String value) {
        List<Pattern> result = new ArrayList<>();
        for (String raw : readStrings(value)) {
            try {
                result.add(Pattern.compile(raw, Pattern.CASE_INSENSITIVE));
            } catch (PatternSyntaxException e) {
                SiyoXLogger.w(TAG, "忽略无效 EntityKiller 正则: " + raw);
            }
        }
        return result;
    }
}
