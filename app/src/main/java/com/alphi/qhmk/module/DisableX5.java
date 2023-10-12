package com.alphi.qhmk.module;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.X5_Properties_conf;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import kotlin.collections.ArraysKt;

@UiItemAgentEntry
@FunctionHookEntry
public class DisableX5 extends CommonSwitchFunctionHook {

    public static final DisableX5 INSTANCE = new DisableX5();

    private DisableX5() {
        super(SyncUtils.PROC_MAIN | SyncUtils.PROC_TOOL, new DexKitTarget[]{X5_Properties_conf.INSTANCE});
    }

    @Override
    protected boolean initOnce() throws Exception {
        // NT
        Class<?> kQbSdk = Initiator.load("com/tencent/smtt/sdk/QbSdk");
        if (kQbSdk != null) {
            Method method = kQbSdk.getDeclaredMethod("getIsSysWebViewForcedByOuter");
            HookUtils.hookBeforeIfEnabled(this, method, param -> param.setResult(true));
            return true;
        }
        // older
        Class<?> tbsClassConfig = DexKit.requireClassFromCache(X5_Properties_conf.INSTANCE);
        List<Method> methods = ArraysKt.filter(tbsClassConfig.getDeclaredMethods(), m -> m.getReturnType() == void.class);
        if (methods.isEmpty()) {
            throw new RuntimeException("DisableX5: no method found in " + tbsClassConfig);
        }
        for (Method method : methods) {
            HookUtils.hookAfterIfEnabled(this, method, param -> {
                Log.d("hook：" + param.thisObject.getClass());
                for (Field field : param.thisObject.getClass().getFields()) {
                    if (field.getType() == boolean.class) {
                        try {
                            field.set(param.thisObject, true);
                        } catch (IllegalAccessException e) {
                            traceError(e);
                        }
                    }
                }
            });
        }
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "禁用浏览器X5内核";
    }

    @Nullable
    @Override
    public CharSequence getDescription() {
        return "致敬QHMK";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.MISC_CATEGORY;
    }

}
