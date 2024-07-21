package com.alphi.qhmk.module;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedBridge;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.SyncUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import kotlin.collections.ArraysKt;

@UiItemAgentEntry
@FunctionHookEntry
public class HookSwiftMenuQWeb extends CommonSwitchFunctionHook {

    private HookSwiftMenuQWeb() {
        super(SyncUtils.PROC_MAIN | SyncUtils.PROC_TOOL);
    }

    public static final HookSwiftMenuQWeb INSTANCE = new HookSwiftMenuQWeb();

    @Override
    protected boolean initOnce() throws Exception {
        Class<?> klass = Initiator.findClassWithSynthetics("com/tencent/mobileqq/webview/swift/component/SwiftBrowserShareMenuHandler", 1);
        Objects.requireNonNull(klass, "SwiftBrowserShareMenuHandler");
        Method method1 = ArraysKt.single(klass.getDeclaredMethods(), m -> {
            if (m.getReturnType() == void.class && Modifier.isPublic(m.getModifiers())) {
                Class<?>[] parameterTypes = m.getParameterTypes();
                return parameterTypes.length == 2 && parameterTypes[1] == long.class && !parameterTypes[0].isPrimitive();
            }
            return false;
        });
        Method method2 = Reflex.findSingleMethodOrNull(klass, void.class, false, method1.getParameterTypes()[0],
                long.class, boolean.class, boolean.class);
        XC_MethodHook hook = HookUtils.beforeIfEnabled(this, param -> {
            // i&512==0 menuItem:openWithQQBrowser
            if (((long) param.args[1] & 512) == 0) {
                param.args[1] = (long) param.args[1] + 512;
            }
        });
        XposedBridge.hookMethod(method1, hook);
        if (method2 != null) {
            XposedBridge.hookMethod(method2, hook);
        }
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "移除内置浏览器菜单栏的“QQ浏览器”";
    }

    @Nullable
    @Override
    public CharSequence getDescription() {
        return "致敬QHMK";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Simplify.UI_MISC;
    }

}
