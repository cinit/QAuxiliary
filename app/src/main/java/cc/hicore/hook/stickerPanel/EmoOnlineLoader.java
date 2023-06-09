package cc.hicore.hook.stickerPanel;

import android.os.Handler;
import android.os.Looper;
import cc.hicore.Env;
import cc.hicore.Utils.DataUtils;
import cc.hicore.Utils.HttpUtils;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmoOnlineLoader {
    static ExecutorService savePool = Executors.newFixedThreadPool(16);
    static ExecutorService savePoolSingle = Executors.newSingleThreadExecutor();
    public static ExecutorService syncThread = Executors.newFixedThreadPool(16);

    public static void submit(EmoPanel.EmoInfo info, Runnable run) {
        syncThread.submit(() -> {
            try {
                String CacheDir = Env.app_save_path + "/Cache/img_" + info.MD5;
                if (info.MD5.equals(DataUtils.getFileMD5(new File(CacheDir)))) {
                    info.Path = CacheDir;
                    new Handler(Looper.getMainLooper()).post(run);
                    return;
                }
                new File(CacheDir).delete();

                HttpUtils.DownloadToFile(info.URL, CacheDir);
                info.Path = CacheDir;
                new Handler(Looper.getMainLooper()).post(run);
            } catch (Throwable th) {
                new Handler(Looper.getMainLooper()).post(run);
            }

        });
    }

    public static void submit2(EmoPanel.EmoInfo info, Runnable run) {
        savePool.submit(() -> {
            try {
                String CacheDir = Env.app_save_path+ "/Cache/img_" + info.MD5;
                if (info.MD5.equals(DataUtils.getFileMD5(new File(CacheDir)))) {
                    info.Path = CacheDir;
                    new Handler(Looper.getMainLooper()).post(run);
                    return;
                }
                new File(CacheDir).delete();

                HttpUtils.DownloadToFile(info.URL, CacheDir);
                info.Path = CacheDir;
                new Handler(Looper.getMainLooper()).post(run);
            } catch (Throwable th) {
                new Handler(Looper.getMainLooper()).post(run);
            }

        });
    }
}
