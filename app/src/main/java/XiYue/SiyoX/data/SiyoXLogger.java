package XiYue.SiyoX.data;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import XiYue.SiyoX.SiyoXConfig;

public class SiyoXLogger {

    private static final String TAG = "SiyoX_Logger";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final ExecutorService logExecutor = Executors.newSingleThreadExecutor();

    private static volatile boolean initialized = false;
    private static File logDir = null;
    private static File errorLogFile = null;
    private static File warnLogFile = null;
    private static File infoLogFile = null;
    private static File allLogFile = null;

    public static synchronized void init(Context context) {
        if (initialized) return;
        try {
            String pkgName = SiyoXConfig.TARGET_PACKAGE;
            if (context != null && context.getPackageName() != null && !context.getPackageName().isEmpty()) {
                pkgName = context.getPackageName();
            }

            File extBaseDir = null;
            if (context != null) {
                try {
                    File extFiles = context.getExternalFilesDir(null);
                    if (extFiles != null && extFiles.getParentFile() != null) {
                        extBaseDir = new File(extFiles.getParentFile(), "SiyoX");
                    }
                } catch (Throwable ignored) {}
            }

            if (extBaseDir == null) {
                extBaseDir = new File(Environment.getExternalStorageDirectory(), "Android/data/" + pkgName + "/SiyoX");
            }

            logDir = new File(extBaseDir, "Log");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            errorLogFile = new File(logDir, "SiyoX_Error_Log.txt");
            warnLogFile = new File(logDir, "SiyoX_Warn_Log.txt");
            infoLogFile = new File(logDir, "SiyoX_Info_Log.txt");
            allLogFile = new File(logDir, "SiyoX_Log.txt");

            clearFile(errorLogFile);
            clearFile(warnLogFile);
            clearFile(infoLogFile);
            clearFile(allLogFile);

            initialized = true;
            i("SiyoX_Logger", "SiyoX Logger initialized for session, logs cleared.");
        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize SiyoXLogger: " + t.getMessage(), t);
        }
    }

    private static void clearFile(File file) {
        try {
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(file, false);
            fos.close();
        } catch (Throwable ignored) {}
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
        writeLogEntry("INFO", tag, msg, null, true, false, false);
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
        writeLogEntry("WARN", tag, msg, null, false, true, false);
    }

    public static void w(String tag, String msg, Throwable tr) {
        Log.w(tag, msg, tr);
        writeLogEntry("WARN", tag, msg, tr, false, true, false);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
        writeLogEntry("ERROR", tag, msg, null, false, false, true);
    }

    public static void e(String tag, String msg, Throwable tr) {
        Log.e(tag, msg, tr);
        writeLogEntry("ERROR", tag, msg, tr, false, false, true);
    }

    public static void d(String tag, String msg) {
        Log.d(tag, msg);
        writeLogEntry("DEBUG", tag, msg, null, false, false, false);
    }

    private static void writeLogEntry(final String level, final String tag, final String msg, final Throwable tr,
                                      final boolean toInfo, final boolean toWarn, final boolean toError) {
        final String time = getFormattedTime();
        logExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ensureFilesReady();
                    StringBuilder sb = new StringBuilder();
                    sb.append("[").append(time).append("] [").append(level).append("] [").append(tag).append("] ").append(msg).append("\n");
                    if (tr != null) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        tr.printStackTrace(pw);
                        sb.append(sw.toString()).append("\n");
                    }
                    String formatted = sb.toString();

                    appendToFile(allLogFile, formatted);

                    if (toInfo) {
                        appendToFile(infoLogFile, formatted);
                    }
                    if (toWarn) {
                        appendToFile(warnLogFile, formatted);
                    }
                    if (toError) {
                        appendToFile(errorLogFile, formatted);
                    }
                } catch (Throwable ignored) {}
            }
        });
    }

    private static synchronized void ensureFilesReady() {
        if (allLogFile == null || !allLogFile.exists()) {
            if (logDir == null) {
                logDir = new File(Environment.getExternalStorageDirectory(), "Android/data/" + SiyoXConfig.TARGET_PACKAGE + "/SiyoX/Log");
            }
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            if (errorLogFile == null) errorLogFile = new File(logDir, "SiyoX_Error_Log.txt");
            if (warnLogFile == null) warnLogFile = new File(logDir, "SiyoX_Warn_Log.txt");
            if (infoLogFile == null) infoLogFile = new File(logDir, "SiyoX_Info_Log.txt");
            if (allLogFile == null) allLogFile = new File(logDir, "SiyoX_Log.txt");
        }
    }

    private static void appendToFile(File file, String text) {
        if (file == null) return;
        FileWriter writer = null;
        try {
            writer = new FileWriter(file, true);
            writer.write(text);
            writer.flush();
        } catch (Throwable ignored) {
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable ignored) {}
            }
        }
    }

    private static String getFormattedTime() {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(new Date());
        }
    }
}
