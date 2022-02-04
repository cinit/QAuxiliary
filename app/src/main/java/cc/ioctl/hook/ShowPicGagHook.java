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

import static io.github.qauxv.util.Initiator._TroopPicEffectsController;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import java.lang.reflect.Method;

@FunctionHookEntry
@UiItemAgentEntry
public class ShowPicGagHook extends CommonSwitchFunctionHook {

    @NonNull
    @Override
    public String getName() {
        return "禁止秀图自动展示";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.CHAT_EMOTICON;
    }

    public static final ShowPicGagHook INSTANCE = new ShowPicGagHook();

    private ShowPicGagHook() {
        super();
    }

    @Override
    public boolean initOnce() {
        Method showPicEffect = null;
        for (Method m : _TroopPicEffectsController().getDeclaredMethods()) {
            Class<?>[] argt = m.getParameterTypes();
            if (argt.length > 2 && argt[1].equals(Bitmap.class)) {
                showPicEffect = m;
                break;
            }
        }
        HookUtils.hookBeforeIfEnabled(this, showPicEffect, 49, param -> param.setResult(null));
        return true;
    }

    @Override
    public boolean isAvailable() {
        return !HostInfo.isTim();
    }
}
