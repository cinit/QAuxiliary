package io.github.qauxv.loader.sbl.lsp100;

import android.content.pm.ApplicationInfo;
import androidx.annotation.NonNull;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.annotations.XposedApiExact;
import io.github.qauxv.loader.sbl.common.ModuleLoader;
import io.github.qauxv.loader.sbl.common.WellKnownConstants;
import io.github.qauxv.loader.sbl.lsp10x.Lsp10xHookEntryHandler;

/**
 * Entry point for libxpsoed API 100 (typically LSPosed).
 * <p>
 * The libxpsoed API is used as ART hook implementation.
 */
@XposedApiExact(100)
public class Lsp100HookEntry implements Lsp10xHookEntryHandler {

    private final XposedModule self;
    private XposedModule.ModuleLoadedParam mModule;

    /**
     * Instantiates a new Xposed module.
     * <p>
     * When the module is loaded into the target process, the constructor will be called.
     *
     * @param self  the Xposed module instance (this module)
     * @param param Information about the process in which the module is loaded
     */
    public Lsp100HookEntry(@NonNull XposedModule self, @NonNull XposedModule.ModuleLoadedParam param) {
        this.self = self;
        mModule = param;
        Lsp100HookImpl.init(self);
    }

    @XposedApiExact(100)
    public void onPackageLoaded(@NonNull XposedModule.PackageLoadedParam param) {
        String packageName = param.getPackageName();
        switch (packageName) {
            case WellKnownConstants.PACKAGE_NAME_QQ:
            case WellKnownConstants.PACKAGE_NAME_QQ_INTERNATIONAL:
            case WellKnownConstants.PACKAGE_NAME_QQ_LITE:
            case WellKnownConstants.PACKAGE_NAME_QQ_HD:
            case WellKnownConstants.PACKAGE_NAME_TIM:
                // Initialize the module
                if (param.isFirstPackage()) {
                    String modulePath = self.getApplicationInfo().sourceDir;
                    handleLoadHostPackage(param.getClassLoader(), param.getApplicationInfo(), modulePath);
                }
                break;
            default:
                // Do nothing
                break;
        }
    }

    private void handleLoadHostPackage(@NonNull ClassLoader cl, @NonNull ApplicationInfo ai, @NonNull String modulePath) {
        String dataDir = ai.dataDir;
        android.util.Log.d("QAuxv", "Lsp100HookEntry.handleLoadHostPackage: dataDir=" + dataDir + ", modulePath=" + modulePath);
        try {
            ModuleLoader.initialize(dataDir, cl, Lsp100HookImpl.INSTANCE, Lsp100HookImpl.INSTANCE, modulePath, true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

}
