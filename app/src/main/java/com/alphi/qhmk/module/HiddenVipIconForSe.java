package com.alphi.qhmk.module;

import android.view.View;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Method;

@UiItemAgentEntry
@FunctionHookEntry
public class HiddenVipIconForSe extends CommonSwitchFunctionHook {

    private HiddenVipIconForSe() {
    }

    public static final HiddenVipIconForSe INSTANCE = new HiddenVipIconForSe();

    @Override
    protected boolean initOnce() throws Exception {
        Class<?> qsmClass = Initiator.loadClass("Lcom/tencent/mobileqq/widget/QVipMedalView;");
        Method onLayout = qsmClass.getDeclaredMethod("onLayout", boolean.class, int.class, int.class, int.class, int.class);
        HookUtils.hookBeforeIfEnabled(this, onLayout, param -> {
            View obj = (View) param.thisObject;
            obj.setClickable(false);
            param.setResult(null);
        });
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "隐藏侧滑面板的VIP图标";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Simplify.SLIDING_UI;
    }

}
