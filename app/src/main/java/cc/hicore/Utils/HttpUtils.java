package cc.hicore.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import cc.hicore.Env;
import io.github.qauxv.util.xpcompat.XposedBridge;
import io.github.qauxv.ui.CommonContextWrapper;
import io.github.qauxv.util.Toasts;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class HttpUtils {
    public static String getContent(String Path) {
        try {
            if (Thread.currentThread().getName().equals("main")) {
                StringBuilder builder = new StringBuilder();
                Thread thread = new Thread(() -> builder.append(getContent(Path)));
                thread.start();
                thread.join();
                return builder.toString();
            }
            HttpURLConnection connection = (HttpURLConnection) new URL(Path).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            InputStream ins = connection.getInputStream();
            String Content = new String(DataUtils.readAllBytes(ins), StandardCharsets.UTF_8);
            ins.close();
            return Content;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean DownloadToFile(String url, String local) {
        try {
            if (Thread.currentThread().getName().equals("main")) {
                AtomicBoolean builder = new AtomicBoolean();
                Thread thread = new Thread(() ->builder.getAndSet(DownloadToFile(url, local)));
                thread.start();
                thread.join();
                return builder.get();
            }
            String cachePath = Env.app_save_path + "/Cache/"+Math.random();
            File parent = new File(local).getParentFile();
            if (!parent.exists()) parent.mkdirs();



            FileOutputStream fOut = new FileOutputStream(cachePath);
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            InputStream ins = connection.getInputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = ins.read(buffer)) != -1) {
                fOut.write(buffer, 0, read);

                //线程中断
                if (Thread.currentThread().isInterrupted()){
                    fOut.close();
                    ins.close();
                    return false;
                }
            }
            fOut.flush();
            fOut.close();
            ins.close();

            if (new File(cachePath).length() < 1)return false;
            FileUtils.copy(cachePath,local);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String PostForResult(String URL, String key, byte[] buffer, int size) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(URL).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("key", key);
            connection.setDoOutput(true);
            OutputStream out = connection.getOutputStream();
            out.write(buffer, 0, size);
            out.flush();
            out.close();
            InputStream ins = connection.getInputStream();
            byte[] result = DataUtils.readAllBytes(ins);
            ins.close();
            return new String(result);
        } catch (Exception e) {
            return "";
        }
    }
    public static String Post(String u,byte[] buffer){
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(u).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            OutputStream out = connection.getOutputStream();
            out.write(buffer);
            out.flush();
            out.close();

            InputStream ins = connection.getInputStream();
            byte[] result = DataUtils.readAllBytes(ins);
            ins.close();
            return new String(result);

        } catch (Exception e) {
            XposedBridge.log(Log.getStackTraceString(e));
            return "";
        }
    }
    public static void ProgressDownload(String url, String filepath, Runnable callback, Context context) {
        AlertDialog al = new AlertDialog.Builder(CommonContextWrapper.createAppCompatContext(context)).create();
        al.setTitle("下载中...");
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        File mSaveFile = new File(filepath);
        if (!mSaveFile.getParentFile().exists()) mSaveFile.getParentFile().mkdirs();
        TextView mFileName = new TextView(context);
        mFileName.setTextColor(Color.BLACK);
        mFileName.setTextSize(18);
        mFileName.setText("文件名:" + mSaveFile.getName());
        layout.addView(mFileName);
        TextView mAllSize = new TextView(context);
        mAllSize.setTextSize(18);
        mAllSize.setTextColor(Color.BLACK);
        layout.addView(mAllSize);
        TextView mDownedSize = new TextView(context);
        mDownedSize.setTextSize(18);
        mDownedSize.setTextColor(Color.BLACK);
        layout.addView(mDownedSize);
        al.setCancelable(false);
        al.setView(layout);
        al.show();
        new Thread(() -> {
            try {
                URL u = new URL(url);
                URLConnection conn = u.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                long msize = conn.getContentLengthLong();
                long readed = 0;
                new Handler(Looper.getMainLooper()).post(() -> mAllSize.setText("文件大小:" + ((int) msize / 1024) + "KB"));
                long finalReaded = readed;
                new Handler(Looper.getMainLooper()).post(() -> mDownedSize.setText("当前已下载:" + ((int) finalReaded / 1024) + "KB"));
                InputStream inp = conn.getInputStream();
                byte[] buffer = new byte[1024];
                FileOutputStream fos = new FileOutputStream(filepath);
                int readthis;
                while ((readthis = inp.read(buffer)) != -1) {
                    readed += readthis;
                    long finalReaded1 = readed;
                    new Handler(Looper.getMainLooper()).post(() -> mDownedSize.setText("当前已下载:" + ((int) finalReaded1 / 1024) + "KB"));
                    fos.write(buffer, 0, readthis);
                }
                fos.close();
                inp.close();
                new Handler(Looper.getMainLooper()).post(al::dismiss);
                callback.run();
            } catch (Throwable th) {
                Toasts.info(context,"下载失败:\n" + th);
                new File(filepath).delete();
                new Handler(Looper.getMainLooper()).post(al::dismiss);
            }
        }).start();
    }
    public static long GetFileLength(String Url) {
        AtomicLong mLong = new AtomicLong();
        Thread mThread = new Thread(()->{
            InputStreamReader isr = null;
            try {
                URL urlObj = new URL(Url);
                URLConnection uc = urlObj.openConnection();

                mLong.set(uc.getContentLengthLong());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (null != isr) {
                        isr.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        mThread.start();
        try {
            mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return mLong.get();
    }
}
