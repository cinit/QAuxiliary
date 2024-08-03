package io.github.qauxv.loader.sbl.frida;

import android.app.Application;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import java.io.File;

import io.github.qauxv.loader.sbl.common.ModuleLoader;

/**
 * Entry point for runtime injection.
 * <p>
 * No hook provider is available in this way.
 */
@Keep
public class FridaInjectEntry {

    /**
     * Entry point for runtime injection without hook provider.
     *
     * @param modulePath  path to the module, e.g. "/data/app/io.github.qauxv-1/base.apk"
     * @param hostDataDir path to the host data directory, e.g. "/data/data/com.example"
     */
    @Keep
    public static void entry2(@NonNull String modulePath, @NonNull String hostDataDir) throws ReflectiveOperationException {
        try {
            startup(new File(modulePath), new File(hostDataDir));
        } catch (Throwable e) {
            android.util.Log.e("QAuxv", "FridaInjectEntry.entry2: failed", e);
            throw e;
        }
    }

    private static void startup(@NonNull File modulePath, @NonNull File hostDataDir) throws ReflectiveOperationException {
        if (!modulePath.canRead()) {
            throw new IllegalArgumentException("modulePath is not readable: " + modulePath);
        }
        if (!hostDataDir.canRead()) {
            throw new IllegalArgumentException("hostDataDir is not readable: " + hostDataDir);
        }
        FridaStartupImpl.INSTANCE.setModulePath(modulePath);
        FridaStartupImpl.INSTANCE.setHostDataDir(hostDataDir);
        ClassLoader cl = findHostClassLoader();
        android.util.Log.i("QAuxv", "FridaInjectEntry.startup: modulePath=" + modulePath + ", hostDataDir=" + hostDataDir + ", cl=" + cl);
        ModuleLoader.initialize(hostDataDir.getAbsolutePath(), cl, FridaStartupImpl.INSTANCE, null, modulePath.getAbsolutePath(), false);
    }

    @NonNull
    private static ClassLoader findHostClassLoader() throws ReflectiveOperationException {
        // case 1: ActivityThread.currentActivityThread().getApplication().getClassLoader()
        Class<?> kActivityThread = Class.forName("android.app.ActivityThread");
        Object activityThread = kActivityThread.getMethod("currentActivityThread").invoke(null);
        Application app = (Application) kActivityThread.getMethod("getApplication").invoke(activityThread);
        return app.getClassLoader();
    }

}
