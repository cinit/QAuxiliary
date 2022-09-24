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
package com.rymmmmm.hook;

import static io.github.qauxv.util.HostInfo.requireMinQQVersion;

import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.step.DexKitDeobfStep;
import io.github.qauxv.step.Step;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.dexkit.DexDeobfsProvider;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitFinder;
import io.github.qauxv.util.dexkit.DexKitTargetSealedEnum;
import io.github.qauxv.util.dexkit.DexMethodDescriptor;
import io.github.qauxv.util.dexkit.NTextItemBuilder_setETText;
import io.github.qauxv.util.dexkit.impl.DexKitDeobfs;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import me.teble.DexKitHelper;

//强制使用默认字体
@FunctionHookEntry
@UiItemAgentEntry
public class DefaultFont extends CommonSwitchFunctionHook implements DexKitFinder {

    public static final DefaultFont INSTANCE = new DefaultFont();

    protected DefaultFont() {
        super("rq_default_font");
    }

    @NonNull
    @Override
    public String getName() {
        return "强制使用默认字体";
    }

    @NonNull
    @Override
    public String getDescription() {
        return "禁用特殊字体, 以及大字体";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.UI_CHAT_MSG;
    }

    @Override
    public boolean isAvailable() {
        return requireMinQQVersion(QQVersion.QQ_8_5_0);
    }

    @Nullable
    @Override
    public Step[] makePreparationSteps() {
        return new Step[]{new DexKitDeobfStep()};
    }

    @Override
    public boolean initOnce() throws ReflectiveOperationException {
        if (!DexDeobfsProvider.INSTANCE.isDexKitBackend()) {
            throw new IllegalStateException("该功能仅限DexKit引擎");
        }
        Method method = DexKit.INSTANCE.loadMethodFromCache(NTextItemBuilder_setETText.INSTANCE);
        Objects.requireNonNull(method);
        HookUtils.hookBeforeIfEnabled(this, method, param -> param.setResult(null));

        Method enlargeTextMsg = Initiator.loadClass("com.tencent.mobileqq.vas.font.api.impl.FontManagerServiceImpl")
                .getDeclaredMethod("enlargeTextMsg", TextView.class);
        HookUtils.hookBeforeIfEnabled(this, enlargeTextMsg, param -> param.setResult(null));
        return true;
    }

    @Override
    public boolean isNeedFind() {
        return DexKit.INSTANCE.getMethodDescFromCache(NTextItemBuilder_setETText.INSTANCE) == null;
    }

    @Override
    public boolean doFind() {
        // protected (BaseBubbleBuilder, TextItemBuilder).void ?(BaseBubbleBuilder.ViewHolder, ChatMessage)
        DexKitDeobfs dexKitDeobfs = (DexKitDeobfs) DexDeobfsProvider.INSTANCE.getCurrentBackend();
        DexKitHelper helper = dexKitDeobfs.getDexKitHelper();
        String[] resultMethods = helper.findMethodUsedField(
                "",
                "",
                "",
                "Landroid/widget/TextView;",
                DexKitHelper.FLAG_GETTING,
                Initiator._TextItemBuilder().getName(),
                "",
                "void",
                new String[]{"", Initiator._ChatMessage().getName()},
                null
        );
        List<String> descs = Arrays.stream(resultMethods)
                .filter(s -> s.contains("BaseBubbleBuilder"))
                .collect(Collectors.toList());
        if (descs.size() == 1) {
            try {
                DexMethodDescriptor descriptor = new DexMethodDescriptor(descs.get(0));
                descriptor.getMethodInstance(Initiator.getHostClassLoader());
                NTextItemBuilder_setETText.INSTANCE.setDescCache(descriptor.toString());
                Log.d("save id: " + DexKitTargetSealedEnum.INSTANCE.nameOf(NTextItemBuilder_setETText.INSTANCE) + ",method: " + descs.get(0));
                return true;
            } catch (NoSuchMethodException e) {
                Log.e(e);
            }
        }
        Map<String, String[]> resMap = helper.findMethodInvoking(
                "",
                "Lcom/tencent/mobileqq/activity/aio/item/TextItemBuilder;",
                "",
                "void",
                new String[]{"", Initiator._ChatMessage().getName()},
                "Landroid/text/TextUtils;",
                "isEmpty",
                "boolean",
                null,
                null
        );
        Set<String> classSet = resMap.keySet().stream()
                .filter(s -> s.contains("BaseBubbleBuilder"))
                .collect(Collectors.toSet());
        List<String> res = descs.stream()
                .filter(s -> !classSet.contains(s))
                .collect(Collectors.toList());
        if (res.size() == 1) {
            try {
                DexMethodDescriptor descriptor = new DexMethodDescriptor(descs.get(0));
                descriptor.getMethodInstance(Initiator.getHostClassLoader());
                NTextItemBuilder_setETText.INSTANCE.setDescCache(descriptor.toString());
                Log.d("save id: " + DexKitTargetSealedEnum.INSTANCE.nameOf(NTextItemBuilder_setETText.INSTANCE) + ",method: " + descs.get(0));
                return true;
            } catch (NoSuchMethodException e) {
                Log.e(e);
            }
        }
        NTextItemBuilder_setETText.INSTANCE.setDescCache(DexKit.INSTANCE.getNO_SUCH_METHOD().toString());
        return false;
    }
}
