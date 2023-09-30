package com.alphi.qhmk.module;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Method;

@UiItemAgentEntry
@FunctionHookEntry
public class HookQWallet extends CommonSwitchFunctionHook {

    private HookQWallet() {
    }

    public static final HookQWallet INSTANCE = new HookQWallet();

    @Override
    protected boolean initOnce() throws Exception {
        // 高版本 已测试 8.8.50
        Class<?> aClass = Initiator.load("Lcom/tencent/mobileqq/qwallet/config/impl/QWalletConfigServiceImpl;");
        if (aClass != null) {
            for (Method method : aClass.getDeclaredMethods()) {
                XposedBridge.hookMethod(method, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) {
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
        return "去除QQ钱包广告";
    }

    @Nullable
    @Override
    public CharSequence getDescription() {
        return "省流量方案需重启";
    }

    @Override
    public boolean isApplicationRestartRequired() {
        return true;
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Simplify.UI_MISC;
    }

}
