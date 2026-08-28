package epic.verify.api;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

/**
 * 应用内更新安装结果回调（PackageInstaller.commit 的 resultReceiver）。
 * 安装结果通过 {@link #setListener} 回调给调用方，无监听时用 Toast 提示。
 */
public class InstallReceiver extends BroadcastReceiver {

    public interface Listener {
        void onResult(boolean success, String msg);
    }

    private static volatile Listener listener;

    public static void setListener(Listener l) {
        listener = l;
    }

    public static void clearListener() {
        listener = null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        Listener l = listener;
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirm != null) {
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(confirm);
                } catch (Throwable ignored) {
                }
            }
        } else if (status == PackageInstaller.STATUS_SUCCESS) {
            if (l != null) l.onResult(true, "安装完成");
            else android.widget.Toast.makeText(context, "安装完成", android.widget.Toast.LENGTH_LONG).show();
        } else {
            String e = (msg == null || msg.length() == 0) ? "安装失败" : msg;
            if (l != null) l.onResult(false, e);
            else android.widget.Toast.makeText(context, e, android.widget.Toast.LENGTH_LONG).show();
        }
    }
}
