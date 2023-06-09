/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package cc.hicore.hook;

import android.content.Context;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;

@FunctionHookEntry
@UiItemAgentEntry
public class IgnorePhoneCallState extends CommonSwitchFunctionHook {

    public static final IgnorePhoneCallState INSTANCE = new IgnorePhoneCallState();

    private IgnorePhoneCallState() {
    }

    @NonNull
    @Override
    public String getName() {
        return "忽略部分通话状态";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.MISC_CATEGORY;
    }

    @Override
    protected boolean initOnce() throws Exception {
        HookUtils.hookBeforeIfEnabled(this,
                Initiator.loadClass("com.tencent.mobileqq.kandian.glue.video.VideoVolumeControl")
                        .getDeclaredMethod("isInPhoneCall", Context.class),
                param -> param.setResult(false));
        HookUtils.hookBeforeIfEnabled(this,
                Initiator._QQAppInterface().getDeclaredMethod("isVideoChatting"),
                param -> param.setResult(false));

        HookUtils.hookBeforeIfEnabled(this,
                Reflex.findSingleMethod(Initiator.loadClass("com.tencent.av.camera.QavCameraUsage"),
                        boolean.class, false, Context.class, boolean.class),
                param -> param.setResult(false));
        return true;
    }
}
