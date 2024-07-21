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
package cc.ioctl.hook.bak;

import static io.github.qauxv.util.Initiator._MessageRecord;
import static io.github.qauxv.util.Initiator._QQAppInterface;
import static io.github.qauxv.util.Initiator.load;

import cc.ioctl.util.ExfriendManager;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedHelpers;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.config.ConfigItems;
import io.github.qauxv.hook.BasePersistBackgroundHook;
import io.github.qauxv.util.LicenseStatus;
import java.lang.reflect.Method;

public class MuteAtAllAndRedPacket extends BasePersistBackgroundHook {

    public static final MuteAtAllAndRedPacket INSTANCE = new MuteAtAllAndRedPacket();

    private MuteAtAllAndRedPacket() {
        super();
    }

    @Override
    public boolean initOnce() throws Exception {
        Class<?> cl_MessageInfo = load("com/tencent/mobileqq/troop/data/MessageInfo");
        if (cl_MessageInfo == null) {
            Class<?> c = _MessageRecord();
            cl_MessageInfo = c.getDeclaredField("mMessageInfo").getType();
        }
        /* @author qiwu */
        final int at_all_type = 13;
        for (Method m : cl_MessageInfo.getDeclaredMethods()) {
            if (m.getReturnType().equals(int.class)) {
                Class<?>[] argt = m.getParameterTypes();
                if (argt.length == 3) {
                    if (argt[0].equals(_QQAppInterface()) && argt[1].equals(boolean.class)
                            && argt[2].equals(String.class)) {
                        HookUtils.hookAfterIfEnabled(this, m, 60, param -> {
                            int ret = (int) param.getResult();
                            String troopuin = (String) param.args[2];
                            if (ret != at_all_type) {
                                return;
                            }
                            String muted = "," + ExfriendManager.getCurrent().getConfig()
                                    .getString(ConfigItems.qn_muted_at_all) + ",";
                            if (muted.contains("," + troopuin + ",")) {
                                param.setResult(0);
                            }
                        });
                        break;
                    }
                }
            }
        }
        XposedHelpers.findAndHookMethod(load("com.tencent.mobileqq.data.MessageForQQWalletMsg"),
                "doParse", new XC_MethodHook(98) {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (LicenseStatus.sDisableCommonHooks) {
                            return;
                        }
                        try {
                            boolean mute = false;
                            int istroop = (Integer) Reflex.getInstanceObjectOrNull(param.thisObject, "istroop");
                            if (istroop != 1) {
                                return;
                            }
                            String troopuin = (String) Reflex.getInstanceObjectOrNull(param.thisObject,
                                    "frienduin");
                            String muted = "," + ExfriendManager.getCurrent().getConfig()
                                    .getString(ConfigItems.qn_muted_red_packet) + ",";
                            if (muted.contains("," + troopuin + ",")) {
                                mute = true;
                            }
                            if (mute) {
                                XposedHelpers.setObjectField(param.thisObject, "isread", true);
                            }
                        } catch (Throwable e) {
                            traceError(e);
                            throw e;
                        }
                    }
                });
        return true;
    }

    @Override
    public int getTargetProcesses() {
        return SyncUtils.PROC_MAIN | SyncUtils.PROC_MSF;
    }
}
