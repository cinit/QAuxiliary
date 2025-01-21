/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.hicore.hook;

import static io.github.qauxv.util.HostInfo.requireMinTimVersion;
import static io.github.qauxv.util.HostInfo.requireMinQQVersion;

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
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.TIMVersion;

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
        if (requireMinQQVersion(QQVersion.QQ_9_0_75) || requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)) {
            HookUtils.hookBeforeIfEnabled(this,
                    Reflex.findMethod(Initiator.loadClass("com.tencent.qqnt.troop.impl.TroopGagUtils"), String.class,
                            "remainingTimeToStringCountDown", long.class), param -> {
                        long time = (long) param.args[0];
                        if (time <= 0) {
                            param.setResult("[0秒]");
                            return;
                        }
                        param.setResult(secondToTime(time));
                    });
            return true;
        }

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

        HookUtils.hookBeforeIfEnabled(this, Reflex.findSingleMethod(Initiator._TroopGagMgr(),
                String.class, false, Context.class, long.class, long.class), param -> {
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
