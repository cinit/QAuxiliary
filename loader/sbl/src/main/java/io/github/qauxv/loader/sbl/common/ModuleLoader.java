package io.github.qauxv.loader.sbl.common;

import android.content.pm.ApplicationInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dalvik.system.BaseDexClassLoader;
import io.github.qauxv.loader.hookapi.IHookBridge;
import io.github.qauxv.loader.hookapi.ILoaderService;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ModuleLoader {

    private ModuleLoader() {
    }

    private static boolean sLoaded = false;
    private static final ArrayList<Throwable> sInitErrors = new ArrayList<>(1);

    @Nullable
    public static String findTargetModulePath(@NonNull ApplicationInfo ai) {
        // TODO: 2024-07-21 implement this method
        return null;
    }

    public static ClassLoader createTargetClassLoader(@NonNull File path, @NonNull ApplicationInfo ai) {
        ClassLoader parent = new TransitClassLoader();
        String dataDirPath = ai.dataDir;
        File dataDir = new File(dataDirPath);
        if (!dataDir.canWrite()) {
            sInitErrors.add(new IOException("createTargetClassLoader: dataDir is not writable: " + dataDirPath));
            return null;
        }
        // create odex directory if sdk < 26
        File odexDir;
        if (android.os.Build.VERSION.SDK_INT < 26) {
            odexDir = new File(dataDir, "app_odex");
            if (!odexDir.exists() && !odexDir.mkdirs()) {
                sInitErrors.add(new IOException("createTargetClassLoader: failed to create odexDir: " + odexDir));
                return null;
            }
        } else {
            // optimizedDirectory – this parameter is deprecated and has no effect since API level 26.
            odexDir = null;
        }
        // create new class loader
        ClassLoader cl = new BaseDexClassLoader(path.getAbsolutePath(), odexDir, null, parent);
        return cl;
    }

    public static void initialize(
            @NonNull ApplicationInfo ai,
            @NonNull ClassLoader hostClassLoader,
            @NonNull ILoaderService loaderService,
            @Nullable IHookBridge hookBridge,
            @NonNull String selfPath
    ) throws ReflectiveOperationException {
        if (sLoaded) {
            return;
        }
        File targetModule = null;
        boolean useDynamicLoad = false;
        try {
            String path = findTargetModulePath(ai);
            if (path != null) {
                targetModule = new File(path);
            }
        } catch (Exception | Error e) {
            sInitErrors.add(e);
            android.util.Log.e("QAuxv", "initialize: findTargetModulePath failed", e);
        }
        if (targetModule != null && targetModule.isFile() && !targetModule.canWrite()) {
            // ART requires W^X since Android 14
            useDynamicLoad = true;
        }
        ClassLoader targetClassLoader = null;
        try {
            if (useDynamicLoad) {
                targetClassLoader = createTargetClassLoader(targetModule, ai);
            }
        } catch (Exception | Error e) {
            sInitErrors.add(e);
            android.util.Log.e("QAuxv", "initialize: createTargetClassLoader failed", e);
        }
        // if we failed to create targetClassLoader, fallback to normal startup
        String modulePath;
        if (targetClassLoader == null) {
            targetClassLoader = ModuleLoader.class.getClassLoader();
            modulePath = selfPath;
        } else {
            modulePath = targetModule.getAbsolutePath();
        }
        assert targetClassLoader != null;
        // invoke the startup routine
        Class<?> kUnifiedEntryPoint = targetClassLoader.loadClass("io.github.qauxv.startup.UnifiedEntryPoint");
        Method initialize = kUnifiedEntryPoint.getMethod("entry",
                String.class, ApplicationInfo.class, ILoaderService.class, ClassLoader.class, IHookBridge.class);
        sLoaded = true;
        initialize.invoke(null, modulePath, ai, loaderService, hostClassLoader, hookBridge);
    }

    public static List<Throwable> getInitErrors() {
        return sInitErrors;
    }
}
