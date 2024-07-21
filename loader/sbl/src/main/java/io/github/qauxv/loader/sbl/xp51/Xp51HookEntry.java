package io.github.qauxv.loader.sbl.xp51;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.qauxv.loader.sbl.common.ModuleLoader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Entry point for started Xposed API 51-99.
 * <p>
 * Xposed is used as ART hook implementation.
 */
@Keep
public class Xp51HookEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    public static final String PACKAGE_NAME_QQ = "com.tencent.mobileqq";
    public static final String PACKAGE_NAME_QQ_INTERNATIONAL = "com.tencent.mobileqqi";
    public static final String PACKAGE_NAME_QQ_LITE = "com.tencent.qqlite";
    public static final String PACKAGE_NAME_QQ_HD = "com.tencent.minihd.qq";
    public static final String PACKAGE_NAME_TIM = "com.tencent.tim";
    public static final String PACKAGE_NAME_SELF = "io.github.qauxv";
    public static final String PACKAGE_NAME_XPOSED_INSTALLER = "de.robv.android.xposed.installer";

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
            case PACKAGE_NAME_SELF: {
                Class<?> kHookStatusInit = Class.forName("io.github.qauxv.util.hookstatus.HookStatusInit");
                Method init = kHookStatusInit.getDeclaredMethod("init", ClassLoader.class);
                init.invoke(null, lpparam.classLoader);
                break;
            }
            case PACKAGE_NAME_TIM:
            case PACKAGE_NAME_QQ:
            case PACKAGE_NAME_QQ_HD:
            case PACKAGE_NAME_QQ_LITE: {
                if (sInitZygoteStartupParam == null) {
                    throw new IllegalStateException("handleLoadPackage: sInitZygoteStartupParam is null");
                }
                sCurrentPackageName = lpparam.packageName;
                ModuleLoader.initialize(lpparam.appInfo, lpparam.classLoader,
                        Xp51HookImpl.INSTANCE, Xp51HookImpl.INSTANCE, getModulePath());
                break;
            }
            case PACKAGE_NAME_QQ_INTERNATIONAL: {
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
