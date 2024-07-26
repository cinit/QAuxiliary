package io.github.qauxv.startup;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.qauxv.loader.hookapi.IHookBridge;
import io.github.qauxv.loader.hookapi.ILoaderService;
import java.lang.reflect.Field;

@Keep
public class UnifiedEntryPoint {

    private static boolean sInitialized = false;

    private UnifiedEntryPoint() {
    }

    @Keep
    public static void entry(
            @NonNull String modulePath,
            @NonNull ApplicationInfo appInfo,
            @NonNull ILoaderService loaderService,
            @NonNull ClassLoader hostClassLoader,
            @Nullable IHookBridge hookBridge
    ) {
        if (sInitialized) {
            throw new IllegalStateException("UnifiedEntryPoint already initialized");
        }
        sInitialized = true;
        // fix up the class loader
        HybridClassLoader loader = HybridClassLoader.INSTANCE;
        ClassLoader self = UnifiedEntryPoint.class.getClassLoader();
        assert self != null;
        ClassLoader parent = self.getParent();
        HybridClassLoader.setLoaderParentClassLoader(parent);
        injectClassLoader(self, loader);
        callNextStep(modulePath, appInfo, loaderService, hostClassLoader, hookBridge);
    }

    private static void callNextStep(
            @NonNull String modulePath,
            @NonNull ApplicationInfo appInfo,
            @NonNull ILoaderService loaderService,
            @NonNull ClassLoader hostClassLoader,
            @Nullable IHookBridge hookBridge
    ) {
        try {
            Class<?> kStartupAgent = Class.forName("io.github.qauxv.poststartup.StartupAgent");
            kStartupAgent.getMethod("startup", String.class, ApplicationInfo.class, ILoaderService.class, ClassLoader.class, IHookBridge.class)
                    .invoke(null, modulePath, appInfo, loaderService, hostClassLoader, hookBridge);
        } catch (ReflectiveOperationException e) {
            android.util.Log.e("QAuxv", "StartupAgent.startup: failed", e);
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    @SuppressLint("DiscouragedPrivateApi")
    private static void injectClassLoader(ClassLoader self, ClassLoader newParent) {
        try {
            Field fParent = ClassLoader.class.getDeclaredField("parent");
            fParent.setAccessible(true);
            fParent.set(self, newParent);
        } catch (Exception e) {
            android.util.Log.e("QAuxv", "injectClassLoader: failed", e);
        }
    }

}
