package cc.hicore.Utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.impl.LoadingPopupView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Async {
    public static void runOnUi(Runnable run){
        new Handler(Looper.getMainLooper()).post(()->{
            try {
                run.run();
            }catch (Throwable t){
                XLog.e("AsyncRunOnUI",t);
            }
        });
    }public static void runOnUi(Runnable run,long time){
        new Handler(Looper.getMainLooper()).postDelayed(()->{
            try {
                run.run();
            }catch (Throwable t){
                XLog.e("AsyncRunOnUI",t);
            }
        },time);
    }
    public static void runAsyncLoading(Context mContext,String title,Runnable runnable){
        runOnUi(()->{
            LoadingPopupView popupView = new XPopup.Builder(mContext)
                    .dismissOnTouchOutside(false)
                    .dismissOnBackPressed(false)
                    .asLoading(title);
            popupView.show();
            runAsync(param->{
                runnable.run();
                return null;
            }).doLast(popupView::dismiss).exec();
        });
    }

    public interface AsyncRunnable {
        Object onRun(Object param) throws Throwable;
    }
    public interface AsyncRunnableNoReturn {
        void onRun() throws Throwable;
    }
    public static class AsyncRunnableInner implements Runnable {
        private Object param;
        private AsyncRunnableInner nextTask;
        private AsyncRunnable mainTask;
        private GlobalContainer container;
        private boolean isUI;
        @Override
        public void run() {
            try {
                Object ret = mainTask.onRun(param);
                if (nextTask != null){
                    nextTask.param = ret;
                    postAsyncRunnable(nextTask);
                }else {
                    postDoLast();
                }
            } catch (Throwable e) {
                if (container.exceptionReceiver != null){
                    try {
                        container.exceptionReceiver.onRun(e);
                    } catch (Throwable ignored) { }
                }else {
                    XLog.e("RunAsyncNotHandlerException",e);
                }
                postDoLast();
            }
        }
        private void postDoLast(){
            if (container.doLastUI != null){
                runOnUi(() -> {
                    try {
                        container.doLastUI.onRun();
                    } catch (Throwable ignored) { }
                });
                container.threadPool.shutdown();
            }
        }
    }
    private static class GlobalContainer{
        private AsyncRunnable exceptionReceiver;
        private AsyncRunnableNoReturn doLastUI;
        private final ExecutorService threadPool = Executors.newCachedThreadPool();
    }
    private static void postAsyncRunnable(AsyncRunnableInner runnable){
        if (runnable.isUI){
            runOnUi(runnable);
        }else {
            runnable.container.threadPool.execute(runnable);
        }
    }

    public static AsyncRun runAsync(AsyncRunnable execTask, Object param){
        AsyncRunnableInner runnable = new AsyncRunnableInner();
        runnable.mainTask = execTask;
        runnable.param = param;


        AsyncRun newRunBuilder = new AsyncRun();
        newRunBuilder.currentRun = runnable;
        newRunBuilder.firstRun = runnable;
        runnable.container = new GlobalContainer();
        return newRunBuilder;
    }
    public static AsyncRun runAsync(AsyncRunnable execTask){
        return runAsync(execTask, null);
    }
    public static AsyncRun runAsyncUI(AsyncRunnable mTask, Object param){
        AsyncRunnableInner runnable = new AsyncRunnableInner();
        runnable.mainTask = mTask;
        runnable.param = param;


        AsyncRun newRunBuilder = new AsyncRun();
        newRunBuilder.currentRun = runnable;
        newRunBuilder.firstRun = runnable;
        runnable.container = new GlobalContainer();
        runnable.isUI = true;
        return newRunBuilder;
    }
    public static AsyncRun runAsyncUI(AsyncRunnable mTask){
        return runAsyncUI(mTask, null);
    }
    public static class AsyncRun{
        private AsyncRunnableInner firstRun;
        private AsyncRunnableInner currentRun;
        public AsyncRun next(AsyncRunnable mtask){
            AsyncRunnableInner runnable = new AsyncRunnableInner();
            runnable.mainTask = mtask;

            AsyncRun newBuilder = new AsyncRun();
            newBuilder.firstRun = firstRun;
            runnable.container = newBuilder.firstRun.container;

            newBuilder.currentRun = runnable;
            currentRun.nextTask = runnable;
            return newBuilder;
        }
        public AsyncRun nextUI(AsyncRunnable mtask){
            AsyncRunnableInner runnable = new AsyncRunnableInner();
            runnable.mainTask = mtask;

            AsyncRun newBuilder = new AsyncRun();
            newBuilder.firstRun = firstRun;
            runnable.container = newBuilder.firstRun.container;


            newBuilder.currentRun = runnable;
            currentRun.nextTask = runnable;
            runnable.isUI = true;
            return newBuilder;
        }
        public AsyncRun onException(AsyncRunnable mtask){
            firstRun.container.exceptionReceiver = mtask;
            return this;
        }
        public AsyncRun doLast(AsyncRunnableNoReturn run){
            firstRun.container.doLastUI = run;
            return this;
        }
        public void exec(){
            postAsyncRunnable(firstRun);
        }
    }
}
