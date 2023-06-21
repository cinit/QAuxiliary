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

import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.hicore.QApp.QAppUtils;
import cc.ioctl.util.HookUtils;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgFont;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.step.Step;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.dexkit.DexDeobfsProvider;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitFinder;
import io.github.qauxv.util.dexkit.DexKitTargetSealedEnum;
import io.github.qauxv.util.dexkit.NTextItemBuilder_setETText;
import io.github.qauxv.util.dexkit.impl.DexKitDeobfs;
import io.luckypray.dexkit.DexKitBridge;
import io.luckypray.dexkit.builder.MethodInvokingArgs;
import io.luckypray.dexkit.builder.MethodUsingFieldArgs;
import io.luckypray.dexkit.descriptor.member.DexFieldDescriptor;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;
import io.luckypray.dexkit.enums.FieldUsingType;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    private final Step mStep = new Step() {
        @Override
        public boolean step() {
            return doFind();
        }

        @Override
        public boolean isDone() {
            return !isNeedFind();
        }

        @Override
        public int getPriority() {
            return 0;
        }


        @Override
        public String getDescription() {
            return "查找字体相关类";
        }
    };

    @Nullable
    @Override
    public Step[] makePreparationSteps() {
        return new Step[]{mStep};
    }

    @Override
    public boolean initOnce() throws ReflectiveOperationException {
        if (QAppUtils.isQQnt()){
            Method getFontID = VASMsgFont.class.getDeclaredMethod("getFontId");
            HookUtils.hookBeforeIfEnabled(this, getFontID, param -> param.setResult(0));
            Method getMagicFontType = VASMsgFont.class.getDeclaredMethod("getMagicFontType");
            HookUtils.hookBeforeIfEnabled(this, getMagicFontType, param -> param.setResult(0));
            return true;
        }else {
            Method method = DexKit.loadMethodFromCache(NTextItemBuilder_setETText.INSTANCE);
            Objects.requireNonNull(method, "NTextItemBuilder_setETText.INSTANCE");
            HookUtils.hookBeforeIfEnabled(this, method, param -> param.setResult(null));

            Method enlargeTextMsg = Initiator.loadClass("com.tencent.mobileqq.vas.font.api.impl.FontManagerServiceImpl")
                    .getDeclaredMethod("enlargeTextMsg", TextView.class);
            HookUtils.hookBeforeIfEnabled(this, enlargeTextMsg, param -> param.setResult(null));
            return true;
        }

    }

    @Override
    public boolean isNeedFind() {
        return DexKit.getMethodDescFromCacheImpl(NTextItemBuilder_setETText.INSTANCE) == null;
    }

    @Override
    public boolean doFind() {
        // protected (BaseBubbleBuilder, TextItemBuilder).void ?(BaseBubbleBuilder.ViewHolder, ChatMessage)
        DexDeobfsProvider.checkDeobfuscationAvailable();
        try (DexKitDeobfs dexKitDeobfs = DexKitDeobfs.newInstance()) {
            DexKitBridge dexKit = dexKitDeobfs.getDexKitBridge();
            Map<DexMethodDescriptor, List<DexFieldDescriptor>> resultMethods = dexKit
                    .findMethodUsingField(new MethodUsingFieldArgs.Builder()
                            .fieldType("Landroid/widget/TextView;")
                            .usingType(FieldUsingType.GET)
                            .callerMethodDeclareClass(Initiator._TextItemBuilder().getName())
                            .callerMethodReturnType("void")
                            .callerMethodParamTypes(new String[]{"", Initiator._ChatMessage().getName()})
                            .build());
            List<DexMethodDescriptor> methods = resultMethods.keySet().stream()
                    .filter(s -> s.getParameterTypesSig().contains("BaseBubbleBuilder"))
                    .collect(Collectors.toList());
            if (methods.size() == 1) {
                try {
                    DexMethodDescriptor descriptor = methods.get(0);
                    descriptor.getMethodInstance(Initiator.getHostClassLoader());
                    NTextItemBuilder_setETText.INSTANCE.setDescCache(descriptor.toString());
                    Log.d("save id: " + DexKitTargetSealedEnum.INSTANCE.nameOf(NTextItemBuilder_setETText.INSTANCE) + ",method: " + descriptor);
                    return true;
                } catch (Throwable e) {
                    traceError(e);
                }
            }
            Map<DexMethodDescriptor, List<DexMethodDescriptor>> resMap = dexKit.findMethodInvoking(
                    new MethodInvokingArgs.Builder()
                            .methodDeclareClass("Lcom/tencent/mobileqq/activity/aio/item/TextItemBuilder;")
                            .methodReturnType("void")
                            .methodParameterTypes(new String[]{"", Initiator._ChatMessage().getName()})
                            .beInvokedMethodDeclareClass("Landroid/text/TextUtils;")
                            .beInvokedMethodName("isEmpty")
                            .beInvokedMethodReturnType("boolean")
                            .build()
            );
            Set<DexMethodDescriptor> methodSet = resMap.keySet().stream()
                    .filter(s -> s.getParameterTypesSig().contains("BaseBubbleBuilder"))
                    .collect(Collectors.toSet());
            List<DexMethodDescriptor> res = methods.stream()
                    .filter(s -> !methodSet.contains(s))
                    .collect(Collectors.toList());
            if (res.size() == 1) {
                try {
                    DexMethodDescriptor descriptor = res.get(0);
                    descriptor.getMethodInstance(Initiator.getHostClassLoader());
                    NTextItemBuilder_setETText.INSTANCE.setDescCache(descriptor.toString());
                    Log.d("save id: " + DexKitTargetSealedEnum.INSTANCE.nameOf(NTextItemBuilder_setETText.INSTANCE) + ",method: " + descriptor);
                    return true;
                } catch (Throwable e) {
                    traceError(e);
                }
            }
            NTextItemBuilder_setETText.INSTANCE.setDescCache(DexKit.NO_SUCH_METHOD.toString());
            return false;
        }
    }
}
