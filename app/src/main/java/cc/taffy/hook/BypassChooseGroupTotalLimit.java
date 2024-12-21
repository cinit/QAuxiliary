/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */
package cc.taffy.hook;

import static io.github.qauxv.util.HostInfo.requireMaxQQVersion;
import static io.github.qauxv.util.HostInfo.requireMinQQVersion;

import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.QQVersion;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

// FunctionHookEntry 和 UiItemAgentEntry 用于注册功能，这两个注解都是必要的
@FunctionHookEntry
@UiItemAgentEntry
public final class BypassChooseGroupTotalLimit extends CommonSwitchFunctionHook {

    // INSTANCE 是必须的，因为 Java 没有 object，所以我们需要一个单例
    public static final BypassChooseGroupTotalLimit INSTANCE = new BypassChooseGroupTotalLimit();

    @Override
    public String getName() {
        //例如 多群去重成员 的群选择器
        return "绕过选择群时的数量的限制";
    }

    @Override
    public String getDescription() {
        return "适用部分选择群的UI(如群成员去重-群选择UI).";
    }

    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.GROUP_CATEGORY;
    }

    @Override
    public boolean initOnce() throws Exception {

        boolean succeed = false;
        Class<?> clazz = Initiator.loadClass("com.tencent.mobileqq.selectmember.troop.SelectTroopListFragment");

        // 9.1.30以后仅能通过hook内部来进行
        if(requireMaxQQVersion(QQVersion.QQ_9_1_28)) {
            try {
                Class<?> implClass = Initiator.loadClass("com.tencent.mobileqq.troop.api.access.impl.TroopManageAccessHandlerApiImpl");
                Method fetchLimitMethod = implClass.getDeclaredMethod("getSelectGroupUpperLimit", String.class, String.class, int.class);

                HookUtils.hookAfterIfEnabled(this, fetchLimitMethod,
                        param -> param.setResult(9999));
                succeed = true;
            }
            finally {

            }
        }

        Method onCreateViewMethod = clazz.getDeclaredMethod("onCreateView",
                android.view.LayoutInflater.class,
                android.view.ViewGroup.class,
                android.os.Bundle.class);

        // 兜底
        boolean finalSucceed = succeed;
        HookUtils.hookAfterIfEnabled(this, onCreateViewMethod, param -> {

            try
            {
            Field TroopMaxCount = clazz.getDeclaredField(requireMinQQVersion(QQVersion.QQ_9_1_30)?"W":"f0");
            TroopMaxCount.setAccessible(true);

            if((int)TroopMaxCount.get(param.thisObject)<1000)
                TroopMaxCount.setInt(param.thisObject, 9999);
            }
            catch (Exception ex)
            {
                if(!finalSucceed)
                    throw ex;
            }
        });

        return succeed;
    }
}
