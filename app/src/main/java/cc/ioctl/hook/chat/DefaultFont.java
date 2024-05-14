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
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
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
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import kotlin.Lazy;
import kotlin.LazyKt;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.UsingType;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;

//强制使用默认字体
@FunctionHookEntry
@UiItemAgentEntry
public class DefaultFont extends CommonSwitchFunctionHook implements DexKitFinder {

    public static final DefaultFont INSTANCE = new DefaultFont();

    private final Lazy<Class<VASMsgFont>> lazyVsfCls = LazyKt.lazy(() -> VASMsgFont.class);

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
        if (requireMinQQVersion(QQVersion.QQ_9_0_15)) {
            XposedBridge.hookAllConstructors(lazyVsfCls.getValue(), new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    VASMsgFont v = (VASMsgFont) param.thisObject;
                    v.fontId = 0;
                    v.magicFontType = 0;
                }
            });
        } else if (QAppUtils.isQQnt()) {
            Method getFontID = lazyVsfCls.getValue().getDeclaredMethod("getFontId");
            HookUtils.hookBeforeIfEnabled(this, getFontID, param -> param.setResult(0));
            Method getMagicFontType = lazyVsfCls.getValue().getDeclaredMethod("getMagicFontType");
            HookUtils.hookBeforeIfEnabled(this, getMagicFontType, param -> param.setResult(0));
        } else {
            Method method = DexKit.loadMethodFromCache(NTextItemBuilder_setETText.INSTANCE);
            Objects.requireNonNull(method, "NTextItemBuilder_setETText.INSTANCE");
            HookUtils.hookBeforeIfEnabled(this, method, param -> param.setResult(null));

            Method enlargeTextMsg = Initiator.loadClass("com.tencent.mobileqq.vas.font.api.impl.FontManagerServiceImpl")
                    .getDeclaredMethod("enlargeTextMsg", TextView.class);
            HookUtils.hookBeforeIfEnabled(this, enlargeTextMsg, param -> param.setResult(null));
        }
        return true;
    }

    @Override
    public boolean isNeedFind() {
        return !QAppUtils.isQQnt() && DexKit.getMethodDescFromCacheImpl(NTextItemBuilder_setETText.INSTANCE) == null;
    }

    @Override
    public boolean doFind() {
        // protected (BaseBubbleBuilder, TextItemBuilder).void ?(BaseBubbleBuilder.ViewHolder, ChatMessage)
        DexDeobfsProvider.checkDeobfuscationAvailable();
        try (DexKitDeobfs dexKitDeobfs = DexKitDeobfs.newInstance()) {
            DexKitBridge dexKit = dexKitDeobfs.getDexKitBridge();
            MethodDataList resultMethods = dexKit.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .declaredClass(Initiator._TextItemBuilder().getName())
                            .returnType("void")
                            .paramTypes(null, Initiator._ChatMessage().getName())
                            .addUsingField(FieldMatcher.create().type("android/widget/TextView"), UsingType.Read)
                    )
            );
            List<MethodData> methods = resultMethods.stream()
                    .filter(s -> s.toDexMethod().getMethodSign().contains("BaseBubbleBuilder"))
                    .collect(Collectors.toList());
            if (methods.size() == 1) {
                try {
                    MethodData methodData = methods.get(0);
                    methodData.getMethodInstance(Initiator.getHostClassLoader());
                    NTextItemBuilder_setETText.INSTANCE.setDescCache(methodData.getDescriptor());
                    Log.d("save id: " + DexKitTargetSealedEnum.INSTANCE.nameOf(NTextItemBuilder_setETText.INSTANCE) + ",method: " + methodData.getDescriptor());
                    return true;
                } catch (Throwable e) {
                    traceError(e);
                }
            }
            MethodDataList methods1 = dexKit.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            .declaredClass("com/tencent/mobileqq/activity/aio/item/TextItemBuilder")
                            .returnType("void")
                            .paramTypes(null, Initiator._ChatMessage().getName())
                            .addInvoke("Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z")
                    )
            );
            Set<MethodData> methodSet = methods1.stream()
                    .filter(s -> s.toDexMethod().getMethodSign().contains("BaseBubbleBuilder"))
                    .collect(Collectors.toSet());
            List<MethodData> res = methods1.stream()
                    .filter(s -> !methodSet.contains(s))
                    .collect(Collectors.toList());
            if (res.size() == 1) {
                try {
                    MethodData methodData = res.get(0);
                    methodData.getMethodInstance(Initiator.getHostClassLoader());
                    NTextItemBuilder_setETText.INSTANCE.setDescCache(methodData.toString());
                    Log.d("save id: " + DexKitTargetSealedEnum.INSTANCE.nameOf(NTextItemBuilder_setETText.INSTANCE) + ",method: " + methodData.getDescriptor());
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
