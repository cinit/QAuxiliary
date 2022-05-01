/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package com.hicore.hook;

import android.content.Context;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;

@FunctionHookEntry
@UiItemAgentEntry
public class ShowAccurateGaggedTime extends CommonSwitchFunctionHook {

    public static final ShowAccurateGaggedTime INSTANCE = new ShowAccurateGaggedTime();

    private ShowAccurateGaggedTime() {
    }

    @NonNull
    @Override
    public String getName() {
        return "被禁言剩余时间精确到秒";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY;
    }

    @Override
    protected boolean initOnce() throws Exception {
        HookUtils.hookBeforeIfEnabled(this,
                Reflex.findMethod(Initiator.loadClass("com.tencent.mobileqq.troop.troopgag.api.impl.TroopGagServiceImpl"), String.class,
                        "gagTimeToStringCountDown", Context.class, long.class), param -> {
                    long time = (long) param.args[1] + 30;
                    long serverTime = AppRuntimeHelper.getServerTime();
                    time = time - serverTime;
                    if (time <= 0) {
                        param.setResult("[解禁了,返回再进吧]");
                        return;
                    }
                    param.setResult(secondToTime(time));
                });

        HookUtils.hookBeforeIfEnabled(this, Reflex.findMethod(Initiator._TroopGagMgr(),
                String.class, "a", Context.class, long.class, long.class), param -> {
            long time = (long) param.args[1];
            if (time <= 0) {
                param.setResult("[0秒]");
                return;
            }
            param.setResult(secondToTime(time));
        });
        return true;
    }

    public static String secondToTime(long second) {
        if (second == 0) {
            return "0秒";
        }
        long days = second / 86400L;
        second = second % 86400L;
        long hours = second / 3600L;
        second = second % 3600L;
        long minutes = second / 60L;
        second = second % 60L;
        return (days == 0 ? "" : days + "天") + (hours == 0 ? "" : hours + "小时") + (minutes == 0 ? "" : minutes + "分钟") + (second == 0 ? "" : second + "秒");
    }
}
