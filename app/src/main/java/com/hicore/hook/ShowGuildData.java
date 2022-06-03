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

import android.view.View;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.QQVersion;

@FunctionHookEntry
@UiItemAgentEntry
public class ShowGuildData extends CommonSwitchFunctionHook {

    public static final ShowGuildData INSTANCE = new ShowGuildData();

    private ShowGuildData() {
    }

    @NonNull
    @Override
    public String getName() {
        return "频道显示更多信息";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.GUILD_CATEGORY;
    }

    @Override
    protected boolean initOnce() throws Exception {
        HookUtils.hookAfterIfEnabled(this,
                Initiator.loadClass("com.tencent.mobileqq.guild.setting.popup.GuildMainSettingDialogFragment")
                        .getDeclaredMethod(HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_93) ? "mk" : "o"),
                param -> {
                    View v = Reflex.getInstanceObject(param.thisObject, "o", RelativeLayout.class);
                    v.setVisibility(View.VISIBLE);
                    v.setOnClickListener((View.OnClickListener) param.thisObject);
                }
        );
        return true;
    }
}
