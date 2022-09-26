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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.dexkit.CQzoneMsgNotify;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import java.lang.reflect.Method;

@FunctionHookEntry
@UiItemAgentEntry
public class MuteQZoneThumbsUp extends CommonSwitchFunctionHook {

    @NonNull
    @Override
    public String getName() {
        return "被赞说说不提醒";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "不影响评论,转发或击掌的通知";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.NOTIFICATION_CATEGORY;
    }

    public static final MuteQZoneThumbsUp INSTANCE = new MuteQZoneThumbsUp();
    protected int MSG_INFO_OFFSET = -1;

    private MuteQZoneThumbsUp() {
        super(new DexKitTarget[]{CQzoneMsgNotify.INSTANCE});
    }

    @Override
    public boolean initOnce() {
        Class<?> clz = DexKit.requireClassFromCache(CQzoneMsgNotify.INSTANCE);
        Method showQZoneMsgNotification = null;
        for (Method m : clz.getDeclaredMethods()) {
            if (m.getReturnType().equals(void.class)) {
                if (showQZoneMsgNotification == null ||
                        m.getParameterTypes().length > showQZoneMsgNotification.getParameterTypes().length) {
                    showQZoneMsgNotification = m;
                }
            }
        }
        HookUtils.hookBeforeIfEnabled(this, showQZoneMsgNotification, param -> {
            if (MSG_INFO_OFFSET < 0) {
                Class<?>[] argt = ((Method) param.method).getParameterTypes();
                int hit = 0;
                for (int i = 0; i < argt.length; i++) {
                    if (argt[i].equals(String.class)) {
                        if (hit == 1) {
                            MSG_INFO_OFFSET = i;
                            break;
                        } else {
                            hit++;
                        }
                    }
                }
            }
            String desc = (String) param.args[MSG_INFO_OFFSET];
            if (desc != null && (desc.endsWith("赞了你的说说") || desc.endsWith("赞了你的分享") || desc
                    .endsWith("赞了你的照片"))) {
                param.setResult(null);
            }
        });
        return true;
    }
}
