package com.alphi.qhmk.module;

import android.content.res.Resources;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.BaseFunctionHook;
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
        hiddenVipMetal();
        optimizeQLevel(12, this);
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "隐藏侧滑面板的VIP图标";
    }

    @Nullable
    @Override
    public CharSequence getDescription() {
        return "致敬QHMK";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Simplify.SLIDING_UI;
    }

    private void hiddenVipMetal() throws ClassNotFoundException, NoSuchMethodException  {
        Class<?> qsmClass = Initiator.loadClass("Lcom/tencent/mobileqq/widget/QVipMedalView;");
        Method onLayout = qsmClass.getDeclaredMethod("onLayout", boolean.class, int.class, int.class, int.class, int.class);
        Method onMeasure = qsmClass.getDeclaredMethod("onMeasure", int.class, int.class);
        Method setMeasuredDimension = View.class.getDeclaredMethod("setMeasuredDimension", int.class, int.class);
        setMeasuredDimension.setAccessible(true);
        HookUtils.hookBeforeIfEnabled(this, onMeasure, param -> {
            View v = (View) param.thisObject;
            setMeasuredDimension.invoke(v, 0, 0);
            param.setResult(null);
        });
        HookUtils.hookBeforeIfEnabled(this, onLayout, param -> {
            View obj = (View) param.thisObject;
            obj.setClickable(false);
            param.setResult(null);
        });
    }

    /**
     *  这是针对较新版本的QQ等级显示不完整用大黄省略号代替，本方法实现的是让QQ策划面板的等级显示完整~
     **/
    public void optimizeQLevel(int num, BaseFunctionHook this0) {
        try {
            Class<?> qsmClass = Initiator.loadClass("com.tencent.mobileqq.activity.qqsettingme.api.impl.QQSettingMeApiImpl");
            Method m_parseQQLevel = qsmClass.getDeclaredMethod("parseQQLevel", Resources.class, int.class, int.class, int.class);
            HookUtils.hookBeforeIfEnabled(this0, m_parseQQLevel, param -> {
                param.args[3] = num;    // 显示图标数量，新版QQ默认为3
            });
        } catch (Exception e) {
            // 如果抛错也不要紧，这仅仅只是优化QQ等级显示而已，目前错误的原因是因为旧版本吧。。。已测试QQ9.0
        }
    }
}
