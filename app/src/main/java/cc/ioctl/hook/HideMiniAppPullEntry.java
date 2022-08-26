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

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HostInfo;
import de.robv.android.xposed.XC_MethodReplacement;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.config.ConfigItems;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.step.Step;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.dexkit.DexFlow;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexMethodDescriptor;
import io.github.qauxv.util.dexkit.impl.LegacyDexDeobfs;
import java.util.ArrayList;
import java.util.HashSet;

@FunctionHookEntry
@UiItemAgentEntry
public class HideMiniAppPullEntry extends CommonSwitchFunctionHook implements Step {

    public static final HideMiniAppPullEntry INSTANCE = new HideMiniAppPullEntry();

    protected HideMiniAppPullEntry() {
        super(ConfigItems.qn_hide_msg_list_miniapp);
    }

    @Override
    protected boolean initOnce() {
        if (HostInfo.isTim()) {
            return false;
        }
        String methodName = getInitMiniAppObfsName();
        if (methodName == null) {
            traceError(new RuntimeException("getInitMiniAppObfsName() == null"));
            return false;
        }
        findAndHookMethod(Initiator._Conversation(), methodName,
                XC_MethodReplacement.returnConstant(null));
        return true;
    }

    /**
     * Fast fail
     */
    @Nullable
    private String getInitMiniAppObfsName() {
        ConfigManager cache = ConfigManager.getCache();
        int lastVersion = cache.getIntOrDefault("qn_hide_miniapp_v2_version_code", 0);
        String methodName = cache.getString("qn_hide_miniapp_v2_method_name");
        if (HostInfo.getVersionCode() == lastVersion) {
            return methodName;
        }
        return null;
    }

    @Override
    public boolean isApplicationRestartRequired() {
        return true;
    }

    @Override
    public boolean isPreparationRequired() {
        return !isDone();
    }

    @Nullable
    @Override
    public Step[] makePreparationSteps() {
        return new Step[]{this};
    }

    @Override
    public boolean step() {
        if (getInitMiniAppObfsName() != null) {
            return true;
        }
        try {
            Class<?> clz = Initiator._Conversation();
            if (clz == null) {
                return false;
            }
            String smaliConversation = DexMethodDescriptor.getTypeSig(clz);
            byte[] dex = DexKit.getClassDeclaringDex(smaliConversation, null);
            if (dex == null) {
                Log.e("Error getClassDeclaringDex Conversation.class");
                return false;
            }
            for (byte[] key : new byte[][]{
                    DexFlow.packUtf8("initMiniAppEntryLayout."),
                    DexFlow.packUtf8("initMicroAppEntryLayout."),
                    DexFlow.packUtf8("init Mini App, cost=")
            }) {
                HashSet<DexMethodDescriptor> rets = new HashSet<>();
                ArrayList<Integer> opcodeOffsets = LegacyDexDeobfs.a(dex, key);
                for (int j = 0; j < opcodeOffsets.size(); j++) {
                    try {
                        DexMethodDescriptor desc = DexFlow
                                .getDexMethodByOpOffset(dex, opcodeOffsets.get(j), true);
                        if (desc != null) {
                            rets.add(desc);
                        }
                    } catch (InternalError ignored) {
                    }
                }
                for (DexMethodDescriptor desc : rets) {
                    if (smaliConversation.equals(desc.declaringClass)
                            && "()V".equals(desc.signature)) {
                        // save and return
                        ConfigManager cache = ConfigManager.getCache();
                        cache.putInt("qn_hide_miniapp_v2_version_code",
                                HostInfo.getVersionCode());
                        cache.putString("qn_hide_miniapp_v2_method_name", desc.name);
                        cache.save();
                        return true;
                    }
                }
            }
            traceError(new RuntimeException("No Conversation.?() func found"));
            return false;
        } catch (Exception e) {
            traceError(e);
            return false;
        }
    }

    @Override
    public boolean isDone() {
        return getInitMiniAppObfsName() != null;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Nullable
    @Override
    public String getDescription() {
        return "生成屏蔽下拉小程序解决方案";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.MAIN_UI_TITLE;
    }

    @NonNull
    @Override
    public String getName() {
        return "隐藏下拉小程序";
    }
}
