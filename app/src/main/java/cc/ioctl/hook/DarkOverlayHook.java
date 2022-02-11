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

import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.step.DexDeobfStep;
import io.github.qauxv.step.Step;
import io.github.qauxv.util.DexFieldDescriptor;
import io.github.qauxv.util.DexFlow;
import io.github.qauxv.util.DexKit;
import io.github.qauxv.util.DexMethodDescriptor;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.QQVersion;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@FunctionHookEntry
@UiItemAgentEntry
public class DarkOverlayHook extends CommonSwitchFunctionHook {

    public static final DarkOverlayHook INSTANCE = new DarkOverlayHook();
    private static final String cache_night_mask_field = "cache_night_mask_field";
    private static final String cache_night_mask_field_version_code = "cache_night_mask_field_version_code";

    private DarkOverlayHook() {
        super();
    }

    static Field fMask = null;

    @Override
    protected boolean initOnce() throws Exception {
        Method handleNightMask = DexKit.doFindMethod(DexKit.N_BASE_CHAT_PIE__handleNightMask);
        HookUtils.hookAfterIfEnabled(this, handleNightMask, 49, param -> {
            if (fMask == null) {
                DexFieldDescriptor desc = FindNightMask.getNightMaskField();
                if (desc == null) {
                    Log.e("FindNightMask/E getNightMaskField return null");
                    return;
                }
                fMask = desc.getFieldInstance(Initiator.getHostClassLoader());
                if (fMask != null) {
                    fMask.setAccessible(true);
                }
            }
            if (fMask != null) {
                Object chatPie = param.thisObject;
                View mask = (View) fMask.get(chatPie);
                if (mask != null) {
                    mask.setVisibility(View.GONE);
                }
            }
        });
        return true;
    }

    @Override
    public boolean isEnabled() {
        if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_6_0)) {
            return false;
        }
        return super.isEnabled();
    }

    private static class FindNightMask implements Step {

        public static DexFieldDescriptor getNightMaskField() {
            String fieldName = null;
            ConfigManager cache = ConfigManager.getCache();
            int lastVersion = cache.getIntOrDefault(cache_night_mask_field_version_code, 0);
            int version = HostInfo.getVersionCode32();
            if (version == lastVersion) {
                String name = cache.getString(cache_night_mask_field);
                if (name != null && name.length() > 0) {
                    fieldName = name;
                }
            }
            if (fieldName != null) {
                return new DexFieldDescriptor(fieldName);
            }
            Class<?> baseChatPie = Initiator._BaseChatPie();
            if (baseChatPie == null) {
                return null;
            }
            DexMethodDescriptor handleNightMask = DexKit
                    .doFindMethodDesc(DexKit.N_BASE_CHAT_PIE__handleNightMask);
            if (handleNightMask == null) {
                Log.i("getNightMaskField: handleNightMask is null");
                return null;
            }
            byte[] dex = DexKit.getClassDeclaringDex(DexMethodDescriptor.getTypeSig(baseChatPie),
                    DexKit.d(DexKit.N_BASE_CHAT_PIE__handleNightMask));
            DexFieldDescriptor field;
            try {
                field = DexFlow.guessFieldByNewInstance(dex, handleNightMask, View.class);
            } catch (Exception e) {
                Log.e(e);
                return null;
            }
            if (field != null) {
                cache.putString(cache_night_mask_field, field.toString());
                cache.putInt(cache_night_mask_field_version_code, version);
                cache.save();
                return field;
            }
            return null;
        }

        @Override
        public boolean step() {
            return getNightMaskField() != null;
        }

        @Override
        public boolean isDone() {
            try {
                ConfigManager cache = ConfigManager.getCache();
                int lastVersion = cache.getIntOrDefault(cache_night_mask_field_version_code, 0);
                if (HostInfo.getVersionCode32() != lastVersion) {
                    return false;
                }
                String name = cache.getString(cache_night_mask_field);
                return name != null && name.length() > 0;
            } catch (Exception e) {
                Log.e(e);
                return false;
            }
        }

        @Override
        public int getPriority() {
            return 20;
        }

        @Override
        public String getDescription() {
            return "定位 BaseChatPie->mMask:View";
        }
    }

    @NonNull
    @Override
    public String getName() {
        return "移除夜间模式遮罩";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "移除夜间模式下聊天界面的深色遮罩";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.CHAT_DECORATION;
    }

    @Override
    public boolean isPreparationRequired() {
        return FindNightMask.getNightMaskField() == null
                || DexKit.isRunDexDeobfuscationRequired(DexKit.N_BASE_CHAT_PIE__handleNightMask);
    }

    @Nullable
    @Override
    public Step[] makePreparationSteps() {
        return new Step[]{new DexDeobfStep(DexKit.N_BASE_CHAT_PIE__handleNightMask),
                new FindNightMask()};
    }
}
