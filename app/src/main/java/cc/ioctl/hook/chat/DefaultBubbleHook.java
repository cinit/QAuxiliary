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
package cc.ioctl.hook.chat;

import static io.github.qauxv.util.HostInfo.requireMinQQVersion;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.hicore.QApp.QAppUtils;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgBubble;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedBridge;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.QQVersion;
import java.io.File;
import java.lang.reflect.Method;

@FunctionHookEntry
@UiItemAgentEntry
public class DefaultBubbleHook extends CommonSwitchFunctionHook {

    public static final DefaultBubbleHook INSTANCE = new DefaultBubbleHook();

    private DefaultBubbleHook() {
    }

    private static final String[] paths = {"/bubble_info", "/files/bubble_info", "/files/bubble_paster", "/files/vas_material_folder/bubble_dir"};

    @Override
    public boolean isAvailable() {
        return !HostInfo.isTim();
    }

    @Override
    protected boolean initOnce() throws Exception {
        final String bubbleClsName = "com.tencent.qqnt.kernel.nativeinterface.VASMsgBubble";

        if (requireMinQQVersion(QQVersion.QQ_9_0_15)) {
            XposedBridge.hookAllConstructors(Class.forName(bubbleClsName), new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(@NonNull MethodHookParam param) {
                    VASMsgBubble v = (VASMsgBubble) param.thisObject;
                    v.bubbleId = 0;
                    v.subBubbleId = 0;
                }
            });
        } else if (QAppUtils.isQQnt()) {
            Class<?> bubbleCls = Class.forName(bubbleClsName);
            HookUtils.hookBeforeIfEnabled(this, bubbleCls.getDeclaredMethod("getBubbleId"), param -> param.setResult(0));
            HookUtils.hookBeforeIfEnabled(this, bubbleCls.getDeclaredMethod("getSubBubbleId"), param -> param.setResult(0));
        } else {
            updateChmod(true);
            Class<?> kAIOMsgItem = Initiator.load("com.tencent.mobileqq.aio.msg.AIOMsgItem");
            Class<?> kAIOBubbleSkinInfo = Initiator.load("com.tencent.mobileqq.aio.msglist.holder.skin.AIOBubbleSkinInfo");
            if (kAIOMsgItem != null && kAIOBubbleSkinInfo != null) {
                Method m = Reflex.findSingleMethod(kAIOMsgItem, void.class, false, kAIOBubbleSkinInfo);
                HookUtils.hookBeforeIfEnabled(this, m, param -> {
                    param.args[0] = null;
                    param.setResult(null);
                });
            }
        }
        return true;
    }

    private static void updateChmod(boolean enabled) {
        for (String path : paths) {
            File dir = new File(HostInfo.getApplication().getFilesDir().getAbsolutePath() + path);
            boolean curr = !dir.exists() || !dir.canRead();
            if (dir.exists()) {
                if (enabled && !curr) {
                    dir.setWritable(false);
                    dir.setReadable(false);
                    dir.setExecutable(false);
                }
                if (!enabled && curr) {
                    dir.setWritable(true);
                    dir.setReadable(true);
                    dir.setExecutable(true);
                }
            }
        }
    }

    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);
        if (!value) {
            updateChmod(false);
        }
    }

    @NonNull
    @Override
    public String getName() {
        return "强制使用默认消息气泡";
    }

    @Nullable
    @Override
    public String[] getExtraSearchKeywords() {
        return new String[]{"默认气泡"};
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.CHAT_DECORATION;
    }

}
