package com.alphi.qhmk.module;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.QQVersion;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import kotlin.collections.ArraysKt;

/**
 * IDEA 2022/02/21 隐藏侧滑厘米秀
 */
@UiItemAgentEntry
@FunctionHookEntry
public class HiddenApolloView extends CommonSwitchFunctionHook {

    private HiddenApolloView() {
    }

    public static final HiddenApolloView INSTANCE = new HiddenApolloView();

    @Override
    protected boolean initOnce() throws Exception {
        Class<?> aClass = Initiator.loadClass("com/tencent/mobileqq/apollo/SettingMeApolloViewController");
        Field field = ArraysKt.single(aClass.getDeclaredFields(), f -> f.getType().getSimpleName().contains("View"));
        field.setAccessible(true);
        Method method = ArraysKt.single(aClass.getDeclaredMethods(), m -> m.getReturnType() == void.class);
        HookUtils.hookAfterIfEnabled(this, method, param -> {
            View view = (View) field.get(param.thisObject);
            view.setVisibility(View.GONE);
        });
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "隐藏侧滑厘米秀";
    }

    @Nullable
    @Override
    public CharSequence getDescription() {
        return "最高支持 8.4.10 致敬QHMK";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Simplify.SLIDING_UI;
    }

    @Override
    public boolean isAvailable() {
        return HostInfo.isQQ() && HostInfo.getVersionCode() <= QQVersion.QQ_8_4_10;
    }

}
