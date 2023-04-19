/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */
package io.github.qauxv.startup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

/**
 * Startup hook for QQ/TIM They should act differently according to the process they belong to. I don't want to cope
 * with them any more, enjoy it as long as possible. DO NOT INVOKE ANY METHOD THAT MAY GET IN TOUCH WITH KOTLIN HERE. DO
 * NOT MODIFY ANY CODE HERE UNLESS NECESSARY.
 *
 * @author cinit
 */
public class StartupHook {

    private static StartupHook sInstance;
    private static boolean sSecondStageInit = false;
    private boolean mFirstStageInit = false;

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
    public static void execStartupInit(Context ctx, Object step, String lpwReserved, boolean bReserved) {
        if (sSecondStageInit) {
            return;
        }
        ClassLoader classLoader = ctx.getClassLoader();
        if (classLoader == null) {
            throw new AssertionError("ERROR: classLoader == null");
        }
        if ("true".equals(System.getProperty(StartupHook.class.getName()))) {
            XposedBridge.log("Err:QAuxiliary reloaded??");
            //I don't know... What happened?
            return;
        }
        System.setProperty(StartupHook.class.getName(), "true");
        injectClassLoader(classLoader);
        StartupRoutine.execPostStartupInit(ctx, step, lpwReserved, bReserved);
        sSecondStageInit = true;
        deleteDirIfNecessaryNoThrow(ctx);
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    @SuppressLint("DiscouragedPrivateApi")
    private static void injectClassLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new NullPointerException("classLoader == null");
        }
        try {
            Field fParent = ClassLoader.class.getDeclaredField("parent");
            fParent.setAccessible(true);
            ClassLoader mine = StartupHook.class.getClassLoader();
            ClassLoader curr = (ClassLoader) fParent.get(mine);
            if (curr == null) {
                curr = XposedBridge.class.getClassLoader();
            }
            if (!curr.getClass().getName().equals(HybridClassLoader.class.getName())) {
                fParent.set(mine, new HybridClassLoader(curr, classLoader));
            }
        } catch (Exception e) {
            log_e(e);
        }
    }

    static void deleteDirIfNecessaryNoThrow(Context ctx) {
        try {
            deleteFile(new File(ctx.getDataDir(), "app_qqprotect"));
            if (new File(ctx.getFilesDir(), "qn_disable_hot_patch").exists()) {
                deleteFile(ctx.getFileStreamPath("hotpatch"));
            }
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
            XposedBridge.log(th);
        } catch (NoClassDefFoundError e) {
            Log.e("Xposed", msg);
            Log.e("EdXposed-Bridge", msg);
        }
    }

    private static void checkClassLoaderIsolation() {
        Class<?> stub;
        try {
            stub = Class.forName("com.tencent.common.app.BaseApplicationImpl");
        } catch (ClassNotFoundException e) {
            Log.d("QAuxv", "checkClassLoaderIsolation success");
            return;
        }
        Log.e("QAuxv", "checkClassLoaderIsolation failure!");
        Log.e("QAuxv", "HostApp: " + stub.getClassLoader());
        Log.e("QAuxv", "Module: " + StartupHook.class.getClassLoader());
        Log.e("QAuxv", "Module.parent: " + StartupHook.class.getClassLoader().getParent());
        Log.e("QAuxv", "XposedBridge: " + XposedBridge.class.getClassLoader());
        Log.e("QAuxv", "SystemClassLoader: " + ClassLoader.getSystemClassLoader());
        Log.e("QAuxv", "Context.class: " + Context.class.getClassLoader());
    }

    public void initialize(ClassLoader rtLoader) throws Throwable {
        if (mFirstStageInit) {
            return;
        }
        try {
            XC_MethodHook startup = new XC_MethodHook(51) {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Context app;
                        Class<?> clz = param.thisObject.getClass().getClassLoader()
                                .loadClass("com.tencent.common.app.BaseApplicationImpl");
                        Field fsApp = null;
                        for (Field f : clz.getDeclaredFields()) {
                            if (f.getType() == clz) {
                                fsApp = f;
                                break;
                            }
                        }
                        if (fsApp == null) {
                            throw new NoSuchFieldException("field BaseApplicationImpl.sApplication not found");
                        }
                        app = (Context) fsApp.get(null);
                        execStartupInit(app, param.thisObject, null, false);
                    } catch (Throwable e) {
                        log_e(e);
                        throw e;
                    }
                }
            };
            Class<?> loadDex = findLoadDexTaskClass(rtLoader);
            Method[] ms = loadDex.getDeclaredMethods();
            Method m = null;
            for (Method method : ms) {
                if (method.getReturnType().equals(boolean.class) && method.getParameterTypes().length == 0) {
                    m = method;
                    break;
                }
                // QQ NT: 8.9.58.11040 (4054)+
                // public void run(Context)
                if (method.getReturnType() == void.class && method.getParameterTypes().length == 1 &&
                        method.getParameterTypes()[0] == Context.class) {
                    m = method;
                    break;
                }
            }
            XposedBridge.hookMethod(m, startup);
            mFirstStageInit = true;
        } catch (Throwable e) {
            if ((e + "").contains("com.bug.zqq")) {
                return;
            }
            if ((e + "").contains("com.google.android.webview")) {
                return;
            }
            log_e(e);
            throw e;
        }
        try {
            XposedHelpers.findAndHookMethod(rtLoader.loadClass("com.tencent.mobileqq.qfix.QFixApplication"),
                    "attachBaseContext", Context.class, new XC_MethodHook() {
                        @Override
                        public void beforeHookedMethod(MethodHookParam param) {
                            deleteDirIfNecessaryNoThrow((Context) param.args[0]);
                        }
                    });
        } catch (ClassNotFoundException ignored) {
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
        Class<?> kITaskFactory = cl.loadClass("com.tencent.qqnt.startup.task.d");
        // check cast so that we can sure that we have found the right class
        if (!kITaskFactory.isAssignableFrom(kTaskFactory)) {
            throw new AssertionError(kITaskFactory + " is not assignable from " + kTaskFactory);
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
