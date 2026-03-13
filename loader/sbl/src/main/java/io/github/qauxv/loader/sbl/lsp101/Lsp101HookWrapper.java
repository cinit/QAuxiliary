/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package io.github.qauxv.loader.sbl.lsp101;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.annotations.XposedApiMin;
import io.github.qauxv.loader.hookapi.IHookBridge;
import io.github.qauxv.loader.sbl.common.CheckUtils;
import java.lang.reflect.Member;
import java.util.Set;

@RequiresApi(26)
@XposedApiMin(101)
public class Lsp101HookWrapper {

    private Lsp101HookWrapper() {
        throw new AssertionError("No instance for you!");
    }

    public static XposedModule self = null;


    public static IHookBridge.MemberUnhookHandle hookAndRegisterMethodCallback(
            final @NonNull Member method,
            final @NonNull IHookBridge.IMemberHookCallback callback,
            final int priority
    ) {
        CheckUtils.checkNonNull(method, "method");
        CheckUtils.checkNonNull(callback, "callback");
        throw new UnsupportedOperationException("not implemented");
    }

    public static int getHookCounter() {
        throw new UnsupportedOperationException("not implemented");
    }

    public static Set<Member> getHookedMethodsRaw() {
        throw new UnsupportedOperationException("not implemented");
    }

}
