package com.alphi.qhmk.module;

import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.SyncUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import kotlin.collections.ArraysKt;

@UiItemAgentEntry
@FunctionHookEntry
public class DisableX5 extends CommonSwitchFunctionHook {

    public static final DisableX5 INSTANCE = new DisableX5();

    private DisableX5() {
        super(SyncUtils.PROC_MAIN | SyncUtils.PROC_TOOL);
    }

    @Override
    protected boolean initOnce() throws Exception {
        // NT
        Class<?> kQbSdk = Initiator.loadClass("com/tencent/smtt/sdk/QbSdk");
        if (kQbSdk != null) {
            Method method = kQbSdk.getDeclaredMethod("getIsSysWebViewForcedByOuter");
            HookUtils.hookBeforeIfEnabled(this, method, param -> {
                param.setResult(true);
            });
        }
        // older
        Class<?> tbsClass = Initiator.loadClass("com.tencent.smtt.sdk.WebView");
        Class<?> TbsClassConfig = null;
        for (Field field : tbsClass.getDeclaredFields()) {
            Class<?> type = field.getType();
            if (type.getName().contains("com.tencent.smtt.utils.")) {
                TbsClassConfig = type;
            }
        }
        if (TbsClassConfig == null) {
            throw new IllegalStateException("X5Settings init fail!!!");
        }
        Method method = ArraysKt.single(TbsClassConfig.getDeclaredMethods(), m -> m.getReturnType() == void.class);
        HookUtils.hookAfterIfEnabled(this, method, param -> {
            boolean result = false;
            Log.d("hook：" + param.thisObject.getClass());
            for (Field field : param.thisObject.getClass().getFields()) {
                if (field.getType() == boolean.class) {
                    try {
                        field.set(param.thisObject, true);
                        result = true;
                    } catch (IllegalAccessException e) {
                        traceError(e);
                    }
                }
            }
            if (result) {
                Log.d("ForceUseSystemWebView success!");
            } else {
                Log.e("ForceUseSystemWebView fail!!!");
            }
        });
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "禁用浏览器X5内核";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.MISC_CATEGORY;
    }

}
