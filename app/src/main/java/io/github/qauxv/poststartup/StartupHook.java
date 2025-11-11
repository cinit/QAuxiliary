/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */
package io.github.qauxv.poststartup;

import android.content.Context;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dalvik.system.BaseDexClassLoader;
import io.github.qauxv.startup.HybridClassLoader;
import io.github.qauxv.util.IoUtils;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedBridge;
import io.github.qauxv.util.xpcompat.XposedHelpers;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Startup hook for QQ/TIM They should act differently according to the process they belong to.
 * <p>
 * I don't want to cope with them anymore, enjoy it as long as possible.
 * <p>
 * DO NOT MODIFY ANY CODE HERE UNLESS NECESSARY.
 *
 * @author cinit
 */
public class StartupHook {

    private static StartupHook sInstance;
    private static boolean sSecondStageInit = false;

    private StartupHook() {
    }

    /**
     * Entry point for static or dynamic initialization. NOTICE: Do NOT change the method name or signature.
     *
     * @param ctx         Application context for host
     * @param step        Step instance
     * @param lpwReserved null, not used
     * @param bReserved   false, not used
     */
    public static void execStartupInit(@NonNull Context ctx, @Nullable Object step, String lpwReserved, boolean bReserved) {
        if (sSecondStageInit) {
            throw new IllegalStateException("Second stage init already executed");
        }
        HybridClassLoader.setHostClassLoader(ctx.getClassLoader());
        StartupRoutine.execPostStartupInit(ctx, step, lpwReserved, bReserved);
        sSecondStageInit = true;
        deleteDirIfNecessaryNoThrow(ctx);
    }

    static void deleteDirIfNecessaryNoThrow(Context ctx) {
        try {
            deleteFile(new File(ctx.getDataDir(), "app_qqprotect"));
        } catch (Throwable e) {
            log_e(e);
        }
    }

    public static StartupHook getInstance() {
        if (sInstance == null) {
            sInstance = new StartupHook();
        }
        return sInstance;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static boolean deleteFile(File file) {
        if (!file.exists()) {
            return false;
        }
        if (file.isFile()) {
            file.delete();
        } else if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            if (listFiles != null) {
                for (File deleteFile : listFiles) {
                    deleteFile(deleteFile);
                }
            }
            file.delete();
        }
        return !file.exists();
    }

    static void log_e(Throwable th) {
        if (th == null) {
            return;
        }
        String msg = Log.getStackTraceString(th);
        Log.e("QAuxv", msg);
        try {
            StartupInfo.getLoaderService().log(th);
        } catch (NoClassDefFoundError | NullPointerException e) {
            Log.e("Xposed", msg);
            Log.e("EdXposed-Bridge", msg);
        }
    }

    public void initializeAfterAppCreate(@NonNull Context ctx) {
        execStartupInit(ctx, null, null, false);
        applyTargetDpiIfNecessary(ctx);
        deleteDirIfNecessaryNoThrow(ctx);
    }

    public void initializeBeforeAppCreate(@NonNull ClassLoader initialClassLoader) {
        // We need to be aware that Tinker may not have been applied yet.
        // Hook com.tencent.common.app.QFixApplicationImplProxy#attachBaseContext(Context) to get the correct class loader.
        final AtomicReference<ClassLoader> pSecondaryClassLoader = new AtomicReference<>();
        final AtomicReference<Thread> pCallingThread = new AtomicReference<>();
        try {
            Class<?> kQFixApplicationImplProxy = initialClassLoader.loadClass("com.tencent.common.app.QFixApplicationImplProxy");
            Method attachBaseContext = kQFixApplicationImplProxy.getDeclaredMethod("attachBaseContext", Context.class);
            final AtomicReference<XC_MethodHook.Unhook> pUnhook1 = new AtomicReference<>();
            pUnhook1.set(XposedBridge.hookMethod(attachBaseContext, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Thread currentThread = Thread.currentThread();
                    pCallingThread.set(currentThread);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    pCallingThread.set(null);
                    ClassLoader cl = pSecondaryClassLoader.get();
                    if (cl == null) {
                        // No Tinker patch seems to be there. The initial class loader is the one we want.
                        cl = initialClassLoader;
                    }
                    // unhook the method to avoid multiple calls
                    XC_MethodHook.Unhook unhook = pUnhook1.getAndSet(null);
                    if (unhook != null) {
                        unhook.unhook();
                    }
                    initializeAfterTinkerApplied(cl);
                }
            }));
            // hook BaseDexClassLoader constructor to get the class loader
            final AtomicReference<Set<XC_MethodHook.Unhook>> pUnhooks = new AtomicReference<>();
            pUnhooks.set(XposedBridge.hookAllConstructors(BaseDexClassLoader.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    ClassLoader cl = (ClassLoader) param.thisObject;
                    Thread callingThread = pCallingThread.get();
                    if (callingThread == null || callingThread != Thread.currentThread()) {
                        // this is not the thread that called attachBaseContext, ignore
                        return;
                    }
                    // TODO: 2025-08-19 replace this dirty classloader <init> hook with a more reliable one to get the Tinker class loader
                    if (cl.toString().contains("io.github.qauxv")) {
                        // wtf?
                        Log.w("QAuxv", "BaseDexClassLoader constructor called in QAuxv, ignoring: " + cl, new Throwable());
                        return;
                    }
                    if (!cl.toString().contains("com.tencent.") && !cl.toString().contains("TinkerClassLoader")) {
                        // The Tinker class loader should look like this:
                        // dalvik.system.DelegateLastClassLoader[DexPathList[[zip file "/data/data/com.tencent.mobileqq/tinker/patch-1.apk"],
                        // dexElements=[Ljava.lang.Object;@...], nativeLibraryDirectories=[/data/data/com.tencent.mobileqq/tinker/patch-1.apk!/lib/arm64-v8a,
                        // /system/lib64, /system/vendor/lib64]]
                        // If it is not Tinker's BaseDexClassLoader, ignore
                        return;
                    }
                    if (pSecondaryClassLoader.compareAndSet(null, cl)) {
                        Log.i("QAuxv", "Using BaseDexClassLoader.<init> for Tinker hot patch: " + cl);
                        // we have found the class loader, unhook the method
                        Set<XC_MethodHook.Unhook> unhooks = pUnhooks.getAndSet(null);
                        if (unhooks != null) {
                            for (XC_MethodHook.Unhook unhook : unhooks) {
                                unhook.unhook();
                            }
                        }
                    } else {
                        // already found, ignore this call
                    }
                }
            }));
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // maybe this is an old version of QQ or TIM, just ignore it
            initializeAfterTinkerApplied(initialClassLoader);
        }
    }

    public void initializeAfterTinkerApplied(@NonNull ClassLoader rtLoader) {
        try {
            XC_MethodHook startup = new XC_MethodHook(51) {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    ClassLoader cl = param.thisObject.getClass().getClassLoader();
                    assert cl != null;
                    Context app = StartupAgent.getBaseApplicationImpl(cl);
                    execStartupInit(app, param.thisObject, null, false);
                }
            };
            Class<?> loadDex = findLoadDexTaskClass(rtLoader);
            Method[] ms = loadDex.getDeclaredMethods();
            Method run = null;
            Method doStep = null;
            for (Method method : ms) {
                // QQ NT: 8.9.58.11040 (4054)+
                // public void run(Context)
                if (method.getReturnType() == void.class && method.getParameterTypes().length == 1 &&
                        method.getParameterTypes()[0] == Context.class) {
                    run = method;
                }
                // public boolean doStep()
                if (method.getReturnType() == boolean.class && method.getParameterTypes().length == 0) {
                    doStep = method;
                }
            }
            // We should try `public void LoadDexTask.run(Context)` first,
            // because there exists `public void LoadDexTask.blockUntilFinish()`.
            if (run == null && doStep == null) {
                throw new RuntimeException("neither LoadDexTask.run(Context) nor LoadDex.doStep() found");
            }
            Method m = run != null ? run : doStep;
            XposedBridge.hookMethod(m, startup);
        } catch (Throwable e) {
            if (e.toString().contains("com.bug.zqq")) {
                return;
            }
            if (e.toString().contains("com.google.android.webview")) {
                return;
            }
            log_e(e);
            throw IoUtils.unsafeThrow(e);
        }
        try {
            XposedHelpers.findAndHookMethod(rtLoader.loadClass("com.tencent.mobileqq.qfix.QFixApplication"),
                    "attachBaseContext", Context.class, new XC_MethodHook() {
                        @Override
                        public void beforeHookedMethod(MethodHookParam param) {
                            applyTargetDpiIfNecessary((Context) param.args[0]);
                            deleteDirIfNecessaryNoThrow((Context) param.args[0]);
                        }
                    });
        } catch (ClassNotFoundException ignored) {
        }
    }

    private static void applyTargetDpiIfNecessary(Context ctx) {
        File safeMode = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),
                "Android/data/" + ctx.getPackageName() + "/qauxv_safe_mode");
        if (safeMode.exists()) {
            return;
        }
        String KEY_TARGET_DPI = "qa_target_dpi";
        File f = new File(ctx.getFilesDir(), KEY_TARGET_DPI);
        if (!f.exists()) {
            return;
        }
        // read 4 bytes
        byte[] buf = new byte[4];
        try (FileInputStream fis = new FileInputStream(f)) {
            if (fis.read(buf) != 4) {
                return;
            }
        } catch (IOException e) {
            log_e(e);
        }
        // little endian
        final int targetDpi = (buf[0] & 0xff) | ((buf[1] & 0xff) << 8) | ((buf[2] & 0xff) << 16) | ((buf[3] & 0xff) << 24);
        if (targetDpi >= 100 && targetDpi <= 1600) {
            try {
                Method getDisplayMetrics = ctx.getResources().getClass().getMethod("getDisplayMetrics");
                XposedBridge.hookMethod(getDisplayMetrics, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        DisplayMetrics dm = (DisplayMetrics) param.getResult();
                        if (dm == null) {
                            return;
                        }
                        float scaleFactory = dm.scaledDensity / dm.density;
                        dm.density = targetDpi / 160f;
                        dm.densityDpi = targetDpi;
                        dm.scaledDensity = targetDpi / 160f * scaleFactory;
                        dm.xdpi = targetDpi;
                        dm.ydpi = targetDpi;
                    }
                });
            } catch (ReflectiveOperationException e) {
                log_e(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<?> findLoadDexTaskClass(ClassLoader cl) throws ClassNotFoundException {
        try {
            return cl.loadClass("com.tencent.mobileqq.startup.step.LoadDex");
        } catch (ClassNotFoundException ignored) {
            // ignore
        }
        // for NT QQ
        // TODO: 2023-04-19 'com.tencent.mobileqq.startup.task.config.a' is not a good way to find the class
        Class<?> kTaskFactory = cl.loadClass("com.tencent.mobileqq.startup.task.config.a");
        Class<?> kITaskFactory;
        try {
            kITaskFactory = cl.loadClass("com.tencent.qqnt.startup.task.d");
        } catch (ClassNotFoundException e) {
            // for QQ 9.2.30
            kITaskFactory = cl.loadClass("com.tencent.qqnt.startup.task.c");
        }
        // check cast so that we can sure that we have found the right class
        if (!kITaskFactory.isAssignableFrom(kTaskFactory)) {
            kTaskFactory = cl.loadClass("com.tencent.mobileqq.startup.task.config.b");
            if (!kITaskFactory.isAssignableFrom(kTaskFactory)) {
                throw new AssertionError(kITaskFactory + " is not assignable from " + kTaskFactory);
            }
        }
        Field taskClassMapField = null;
        for (Field field : kTaskFactory.getDeclaredFields()) {
            if (field.getType() == HashMap.class && Modifier.isStatic(field.getModifiers())) {
                taskClassMapField = field;
                break;
            }
        }
        if (taskClassMapField == null) {
            throw new AssertionError("taskClassMapField not found");
        }
        taskClassMapField.setAccessible(true);
        HashMap<String, Class<?>> taskClassMap;
        try {
            // XXX: this will cause <clinit>() to be called, check whether it will cause any problem
            taskClassMap = (HashMap<String, Class<?>>) taskClassMapField.get(null);
        } catch (IllegalAccessException e) {
            // should not happen
            throw new AssertionError(e);
        }
        assert taskClassMap != null;
        Class<?> loadDexTaskClass = taskClassMap.get("LoadDexTask");
        if (loadDexTaskClass == null) {
            throw new AssertionError("loadDexTaskClass not found");
        }
        return loadDexTaskClass;
    }

}
