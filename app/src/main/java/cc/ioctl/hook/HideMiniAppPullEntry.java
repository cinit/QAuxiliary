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
import static io.luckypray.dexkit.descriptor.DexDescriptorUtil.getTypeSig;

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
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.dexkit.DexDeobfsProvider;
import io.github.qauxv.util.dexkit.DexKitFinder;
import io.github.qauxv.util.dexkit.impl.DexKitDeobfs;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@FunctionHookEntry
@UiItemAgentEntry
public class HideMiniAppPullEntry extends CommonSwitchFunctionHook implements DexKitFinder {

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

    @Override
    public boolean isNeedFind() {
        return getInitMiniAppObfsName() == null;
    }

    @Override
    public boolean doFind() {
        Class<?> clz = Initiator._Conversation();
        if (clz == null) {
            return false;
        }
        String conversationSig = getTypeSig(clz);
        DexKitDeobfs dexKitDeobfs = (DexKitDeobfs) DexDeobfsProvider.INSTANCE.getCurrentBackend();
        String[] strings = new String[]{
                "initMiniAppEntryLayout.",
                "initMicroAppEntryLayout.",
                "init Mini App, cost="
        };
        Map<String, Set<String>> map = new HashMap<>(16);
        for (int i = 0; i < strings.length; i++) {
            Set<String> set = new HashSet<>(1);
            set.add(strings[i]);
            map.put("Conversation_" + i, set);
        }
        Map<String, List<DexMethodDescriptor>> res = dexKitDeobfs.getDexKitBridge().batchFindMethodsUsingStrings(map, false, new int[0]);
        for (List<DexMethodDescriptor> methods: res.values()) {
            for (DexMethodDescriptor descriptor: methods) {
                if (descriptor.getDeclaringClassSig().equals(conversationSig)
                        && "()V".equals(descriptor.getMethodTypeSig())) {
                    // save and return
                    ConfigManager cache = ConfigManager.getCache();
                    cache.putInt("qn_hide_miniapp_v2_version_code",
                            HostInfo.getVersionCode());
                    cache.putString("qn_hide_miniapp_v2_method_name", descriptor.getName());
                    cache.save();
                    return true;
                }
            }
        }
        return false;
    }
}
