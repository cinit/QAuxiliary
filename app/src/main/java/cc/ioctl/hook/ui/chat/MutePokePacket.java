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
package cc.ioctl.hook.ui.chat;

import static io.github.qauxv.util.Initiator.load;

import androidx.annotation.NonNull;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedHelpers;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.LicenseStatus;

@FunctionHookEntry
@UiItemAgentEntry
public class MutePokePacket extends CommonSwitchFunctionHook {

    public static final MutePokePacket INSTANCE = new MutePokePacket();

    private MutePokePacket() {
        super();
    }

    @NonNull
    @Override
    public String getName() {
        return "屏蔽戳一戳";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.UI_CHAT_MSG;
    }

    @Override
    public boolean initOnce() {
        XposedHelpers.findAndHookMethod(load("com.tencent.mobileqq.data.MessageForPoke"),
                "doParse", new XC_MethodHook(200) {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (LicenseStatus.sDisableCommonHooks || !isEnabled()) {
                            return;
                        }
                        try {
                            XposedHelpers.setObjectField(param.thisObject, "isPlayed", true);
                        } catch (Throwable e) {
                            traceError(e);
                            throw e;
                        }

                    }
                });
        return true;
    }
}
