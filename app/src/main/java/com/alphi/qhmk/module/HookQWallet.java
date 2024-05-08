package com.alphi.qhmk.module;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.SyncUtils;
import java.lang.reflect.Method;
import kotlin.collections.ArraysKt;

@UiItemAgentEntry
@FunctionHookEntry
public class HookQWallet extends CommonSwitchFunctionHook {

    private HookQWallet() {
        super(SyncUtils.PROC_MAIN | SyncUtils.PROC_TOOL);
    }

    public static final HookQWallet INSTANCE = new HookQWallet();

    @Override
    protected boolean initOnce() throws Exception {
        // NT
        Class<?> kQWalletHomeFragment = Initiator.load("com/tencent/mobileqq/qwallet/home/QWalletHomeFragment");
        if (kQWalletHomeFragment != null) {
            Class<?> kQWalletHomePreviewController = Initiator.load("com/tencent/mobileqq/qwallet/home/QWalletHomePreviewController");
            if (kQWalletHomePreviewController != null) {
                // 预防广告视图显示导致的黑屏
                // public final QWalletHomePreviewController.?(QWallBaseFragment|QWalletBaseFragment)Z
                Method methodIsShowAdView = ArraysKt.single(kQWalletHomePreviewController.getDeclaredMethods(),
                        it -> it.getReturnType() == boolean.class &&
                                it.getParameterTypes().length == 1 &&
                                it.getParameterTypes()[0].getSimpleName().endsWith("BaseFragment"));
                HookUtils.hookBeforeIfEnabled(this, methodIsShowAdView, param -> param.setResult(true));
            }
            Method onViewCreated = kQWalletHomeFragment.getDeclaredMethod("onViewCreated", View.class, Bundle.class);
            HookUtils.hookBeforeIfEnabled(this, onViewCreated, param -> {
                // 加载广告及视图初始化
                param.setResult(null);
            });
            return true;
        }
        // 高版本 已测试 8.8.50
        Class<?> aClass = Initiator.load("Lcom/tencent/mobileqq/qwallet/config/impl/QWalletConfigServiceImpl;");
        if (aClass != null) {
            for (Method method : aClass.getDeclaredMethods()) {
                HookUtils.hookBeforeIfEnabled(this, method, new HookUtils.BeforeHookedMethod() {
                    @Override
                    public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
                        param.setResult(resultObj());
                    }

                    private Object resultObj() {
                        if (method.getReturnType() == boolean.class) {
                            return false;
                        }
                        if (method.getReturnType() == int.class) {
                            return 0;
                        }
                        if (method.getReturnType() == long.class) {
                            return 0L;
                        }
                        return null;
                    }
                });
            }
            return true;
        }

        // 针对低版本 8.4.0
        aClass = Initiator.loadClass("Lcom/tencent/mobileqq/activity/qwallet/config/QWalletConfigManager;");
        XposedBridge.hookAllConstructors(aClass, HookUtils.beforeIfEnabled(this, param -> param.args[0] = null));
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "隐藏QQ钱包超值精选";
    }

    @Nullable
    @Override
    public CharSequence getDescription() {
        return "致敬 QHMK";
    }

    @Override
    public boolean isApplicationRestartRequired() {
        return true;
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Simplify.SLIDING_UI;
    }

}
