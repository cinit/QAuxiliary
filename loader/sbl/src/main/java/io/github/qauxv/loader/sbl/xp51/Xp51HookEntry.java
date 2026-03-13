package io.github.qauxv.loader.sbl.xp51;

import androidx.annotation.Keep;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.qauxv.loader.sbl.common.ModuleLoader;
import io.github.qauxv.loader.sbl.common.WellKnownConstants;

/**
 * Entry point for started Xposed API 51-99.
 * <p>
 * Xposed is used as ART hook implementation.
 */
@Keep
public class Xp51HookEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static XC_LoadPackage.LoadPackageParam sLoadPackageParam = null;
    private static IXposedHookZygoteInit.StartupParam sInitZygoteStartupParam = null;
    private static String sModulePath = null;

    public static String sCurrentPackageName = null;

    /**
     * *** No kotlin code should be invoked here.*** May cause a crash.
     */
    @Keep
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws ReflectiveOperationException {
        sLoadPackageParam = lpparam;
        // check LSPosed dex-obfuscation
        Class<?> kXposedBridge = XposedBridge.class;
        switch (lpparam.packageName) {
            case WellKnownConstants.PACKAGE_NAME_SELF: {
                Xp51HookStatusInit.init(lpparam.classLoader);
                break;
            }
            case WellKnownConstants.PACKAGE_NAME_TIM:
            case WellKnownConstants.PACKAGE_NAME_QQ:
            case WellKnownConstants.PACKAGE_NAME_QQ_HD:
            case WellKnownConstants.PACKAGE_NAME_QQ_LITE: {
                if (sInitZygoteStartupParam == null) {
                    throw new IllegalStateException("handleLoadPackage: sInitZygoteStartupParam is null");
                }
                sCurrentPackageName = lpparam.packageName;
                ModuleLoader.initialize(lpparam.appInfo.dataDir, lpparam.classLoader,
                        Xp51HookImpl.INSTANCE, Xp51HookImpl.INSTANCE, getModulePath(), true);
                break;
            }
            case WellKnownConstants.PACKAGE_NAME_QQ_INTERNATIONAL: {
                //coming...
                break;
            }
            default:
                break;
        }
    }

    /**
     * *** No kotlin code should be invoked here.*** May cause a crash.
     */
    @Override
    public void initZygote(StartupParam startupParam) {
        sInitZygoteStartupParam = startupParam;
        sModulePath = startupParam.modulePath;
    }

    /**
     * Get the {@link XC_LoadPackage.LoadPackageParam} of the current module.
     *
     * @return the lpparam
     */
    public static XC_LoadPackage.LoadPackageParam getLoadPackageParam() {
        if (sLoadPackageParam == null) {
            throw new IllegalStateException("LoadPackageParam is null");
        }
        return sLoadPackageParam;
    }

    /**
     * Get the path of the current module.
     *
     * @return the module path
     */
    public static String getModulePath() {
        if (sModulePath == null) {
            throw new IllegalStateException("Module path is null");
        }
        return sModulePath;
    }

    /**
     * Get the {@link IXposedHookZygoteInit.StartupParam} of the current module.
     *
     * @return the initZygote param
     */
    public static IXposedHookZygoteInit.StartupParam getInitZygoteStartupParam() {
        if (sInitZygoteStartupParam == null) {
            throw new IllegalStateException("InitZygoteStartupParam is null");
        }
        return sInitZygoteStartupParam;
    }

}
