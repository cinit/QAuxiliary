package io.github.qauxv.loader.sbl.lsp100;

import android.content.pm.ApplicationInfo;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.qauxv.loader.sbl.common.ModuleLoader;

/**
 * Entry point for libxpsoed API 100 (typically LSPosed).
 * <p>
 * The libxpsoed API is used as ART hook implementation.
 */
@Keep
public class Lsp100HookEntry extends XposedModule {

    /**
     * Instantiates a new Xposed module.
     * <p>
     * When the module is loaded into the target process, the constructor will be called.
     *
     * @param base  The implementation interface provided by the framework, should not be used by the module
     * @param param Information about the process in which the module is loaded
     */
    public Lsp100HookEntry(@NonNull XposedInterface base, @NonNull ModuleLoadedParam param) {
        super(base, param);
        mModule = param;
        Lsp100HookImpl.init(this);
    }

    private ModuleLoadedParam mModule;

    public static final String PACKAGE_NAME_QQ = "com.tencent.mobileqq";
    public static final String PACKAGE_NAME_QQ_INTERNATIONAL = "com.tencent.mobileqqi";
    public static final String PACKAGE_NAME_QQ_LITE = "com.tencent.qqlite";
    public static final String PACKAGE_NAME_QQ_HD = "com.tencent.minihd.qq";
    public static final String PACKAGE_NAME_TIM = "com.tencent.tim";

    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        String packageName = param.getPackageName();
        switch (packageName) {
            case PACKAGE_NAME_QQ:
            case PACKAGE_NAME_QQ_INTERNATIONAL:
            case PACKAGE_NAME_QQ_LITE:
            case PACKAGE_NAME_QQ_HD:
            case PACKAGE_NAME_TIM:
                // Initialize the module
                if (param.isFirstPackage()) {
                    String modulePath = this.getApplicationInfo().sourceDir;
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
