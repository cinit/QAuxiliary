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

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.dexkit.DexDeobfsProvider;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitFinder;
import io.github.qauxv.util.dexkit.DexMethodDescriptor;
import io.github.qauxv.util.dexkit.impl.DexKitDeobfs;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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
    public boolean initOnce() throws ReflectiveOperationException {
        if (!DexDeobfsProvider.INSTANCE.isDexKitBackend()) {
            throw new IllegalStateException("该功能仅限DexKit引擎");
        }
        Method method = DexKit.getMethodFromCache(DexKit.N_TextItemBuilder_setETText);
        Objects.requireNonNull(method);
        HookUtils.hookBeforeIfEnabled(this, method, param -> param.setResult(null));
        // m.getName().equals(HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_93) ? "q0" : "a") &&
        Method enlargeTextMsg = Initiator.loadClass("com.tencent.mobileqq.vas.font.api.impl.FontManagerServiceImpl")
                .getDeclaredMethod("enlargeTextMsg", TextView.class);
        HookUtils.hookBeforeIfEnabled(this, enlargeTextMsg, param -> param.setResult(null));
        return true;
    }

    @Override
    public boolean getNeedFind() {
        return DexKit.getMethodFromCache(DexKit.N_TextItemBuilder_setETText) != null;
    }

    @Override
    public boolean doFind() {
        if (!DexDeobfsProvider.INSTANCE.isDexKitBackend()) {
            return false;
        }
        // protected (BaseBubbleBuilder, TextItemBuilder).void ?(BaseBubbleBuilder.ViewHolder, ChatMessage)
        Set<Method> candidates = new HashSet<>(2);
        for (Method m : Initiator._TextItemBuilder().getDeclaredMethods()) {
            if (m.getModifiers() == Modifier.PROTECTED && m.getReturnType() == void.class) {
                Class<?>[] argt = m.getParameterTypes();
                if (argt.length == 2 && argt[0] != View.class && argt[1] == Initiator._ChatMessage()) {
                    candidates.add(m);
                }
            }
        }
        if (candidates.size() != 2) {
            throw new RuntimeException("expect 2 methods, got " + candidates.size());
        }
        DexKitDeobfs dexKitDeobf = (DexKitDeobfs) DexDeobfsProvider.INSTANCE.getCurrentBackend();
        String[] res = dexKitDeobf.doFindMethodUsedField(
                "",
                "",
                "",
                "Landroid/widget/TextView;",
                DexKitHelper.FLAG_GETTING,
                Initiator._TextItemBuilder().getName(),
                "",
                "void",
                new String[]{"", Initiator._ChatMessage().getName()},
                new int[0]
        );
        for (String desc : res) {
            DexMethodDescriptor descriptor = new DexMethodDescriptor(desc);
            try {
                Method method = descriptor.getMethodInstance(Initiator.getHostClassLoader());
                if (candidates.contains(method)) {
                    dexKitDeobf.saveDescriptor(DexKit.N_TextItemBuilder_setETText, descriptor);
                    Log.d("save id: " + DexKit.N_TextItemBuilder_setETText + ",method: " + desc);
                    return true;
                }
            } catch (NoSuchMethodException e) {
                Log.e(e);
            }
        }
        dexKitDeobf.saveDescriptor(DexKit.N_TextItemBuilder_setETText, new DexMethodDescriptor("Lxxxxx;->xxxxx()V"));
        return false;
    }
}
