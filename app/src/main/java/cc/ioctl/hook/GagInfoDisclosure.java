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
package cc.ioctl.hook;

import static cc.ioctl.util.Reflex.findMethodByTypes_1;
import static io.github.qauxv.bridge.GreyTipBuilder.MSG_TYPE_TROOP_GAP_GRAY_TIPS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.bridge.ContactUtils;
import io.github.qauxv.bridge.GreyTipBuilder;
import io.github.qauxv.bridge.QQMessageFacade;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.LicenseStatus;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@FunctionHookEntry
@UiItemAgentEntry
public class GagInfoDisclosure extends CommonSwitchFunctionHook {

    public static final GagInfoDisclosure INSTANCE = new GagInfoDisclosure();

    private GagInfoDisclosure() {
        // TODO: 2020/6/12 Figure out whether MSF is really needed
        super(SyncUtils.PROC_MAIN | SyncUtils.PROC_MSF, new int[]{
                DexKit.C_MessageRecordFactory,
                DexKit.N_ContactUtils_getDiscussionMemberShowName,
                DexKit.N_ContactUtils_getBuddyName,
        });
    }

    public static String getGagTimeString(long sec) {
        String _min = "分钟";
        String _hour = "小时";
        String _day = "天";
        if (sec < 60) {
            return 1 + _min;
        }
        long fsec = 59 + sec;
        long d = fsec / 86400;
        long h = (fsec - (86400 * d)) / 3600;
        long m = ((fsec - (86400 * d)) - (3600 * h)) / 60;
        String ret = "";
        if (d > 0) {
            ret = ret + d + _day;
        }
        if (h > 0) {
            ret = ret + h + _hour;
        }
        if (m > 0) {
            return ret + m + _min;
        }
        return ret;
    }

    @Override
    public boolean initOnce() throws Exception {
        Class<?> clzGagMgr = Initiator._TroopGagMgr();
        Method m1 = findMethodByTypes_1(clzGagMgr, void.class, String.class, long.class,
                long.class, int.class, String.class, String.class, boolean.class);
        XposedBridge.hookMethod(m1, new XC_MethodHook(48) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (LicenseStatus.sDisableCommonHooks) {
                    return;
                }
                if (!isEnabled()) {
                    return;
                }
                String selfUin = AppRuntimeHelper.getAccount() + "";
                String troopUin = (String) param.args[0];
                long time = (long) param.args[1];
                long interval = (long) param.args[2];
                int msgseq = (int) param.args[3];
                String opUin = (String) param.args[4];
                String victimUin = (String) param.args[5];
                String opName = ContactUtils.getTroopMemberNick(troopUin, opUin);
                String victimName = ContactUtils.getTroopMemberNick(troopUin, victimUin);
                GreyTipBuilder builder = GreyTipBuilder.create(MSG_TYPE_TROOP_GAP_GRAY_TIPS);
                if (selfUin.endsWith(victimUin)) {
                    builder.append("你");
                } else {
                    builder.append(' ').appendTroopMember(victimUin, victimName).append(' ');
                }
                builder.append("被");
                if (selfUin.endsWith(opUin)) {
                    builder.append("你");
                } else {
                    builder.append(' ').appendTroopMember(opUin, opName).append(' ');
                }
                if (interval == 0) {
                    builder.append("解除禁言");
                } else {
                    builder.append("禁言").append(getGagTimeString(interval));
                }
                Object msg = builder.build(troopUin, 1, opUin, time, msgseq);
                List<Object> list = new ArrayList<>();
                list.add(msg);
                QQMessageFacade.commitMessageRecordList(list);
                param.setResult(null);
            }
        });
        Method m2 = findMethodByTypes_1(clzGagMgr, void.class, String.class, String.class,
                long.class, long.class, int.class, boolean.class, boolean.class);
        HookUtils.hookBeforeIfEnabled(this, m2, 47, param -> {
            String selfUin = AppRuntimeHelper.getAccount() + "";
            String troopUin = (String) param.args[0];
            String opUin = (String) param.args[1];
            long time = (long) param.args[2];
            long interval = (long) param.args[3];
            int msgseq = (int) param.args[4];
            boolean gagTroop = (boolean) param.args[5];
            String opName = ContactUtils.getTroopMemberNick(troopUin, opUin);
            GreyTipBuilder builder = GreyTipBuilder.create(MSG_TYPE_TROOP_GAP_GRAY_TIPS);
            if (gagTroop) {
                if (selfUin.endsWith(opUin)) {
                    builder.append("你");
                } else {
                    builder.append(' ').appendTroopMember(opUin, opName).append(' ');
                }
                builder.append(interval == 0 ? "关闭了全员禁言" : "开启了全员禁言");
            } else {
                builder.append("你被 ").appendTroopMember(opUin, opName);
                if (interval == 0) {
                    builder.append(" 解除禁言");
                } else {
                    builder.append(" 禁言").append(getGagTimeString(interval));
                }
            }
            Object msg = builder.build(troopUin, 1, opUin, time, msgseq);
            List<Object> list = new ArrayList<>();
            list.add(msg);
            QQMessageFacade.commitMessageRecordList(list);
            param.setResult(null);
        });
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "显示设置禁言的管理";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "即使你只是普通群成员";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.CHAT_CATEGORY;
    }
}
