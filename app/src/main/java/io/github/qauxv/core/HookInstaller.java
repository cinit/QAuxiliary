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

package io.github.qauxv.core;

import android.content.Context;
import androidx.annotation.NonNull;
import io.github.qauxv.base.IDynamicHook;
import io.github.qauxv.base.RuntimeErrorTracer;
import io.github.qauxv.util.Log;

public class HookInstaller {

    private static IDynamicHook[] sAnnotatedHooks = null;

    @NonNull
    public static IDynamicHook[] queryAllAnnotatedHooks() {
        if (sAnnotatedHooks != null) {
            return sAnnotatedHooks;
        }
        synchronized (HookInstaller.class) {
            if (sAnnotatedHooks == null) {
                sAnnotatedHooks = io.github.qauxv.gen.AnnotatedFunctionHookEntryList.getAnnotatedFunctionHookEntryList();
            }
        }
        return sAnnotatedHooks;
    }

    public static int getHookIndex(@NonNull IDynamicHook hook) {
        IDynamicHook[] hooks = queryAllAnnotatedHooks();
        for (int i = 0; i < hooks.length; i++) {
            if (hooks[i] == hook) {
                return i;
            }
        }
        return -1;
    }

    public static void allowEarlyInit(@NonNull IDynamicHook hook) {
        if (hook == null) {
            return;
        }
        try {
            if (hook.isTargetProcess() && hook.isEnabled() && !hook.isPreparationRequired() && !hook.isInitialized()) {
                hook.initialize();
            }
        } catch (Throwable e) {
            if (hook instanceof RuntimeErrorTracer) {
                ((RuntimeErrorTracer) hook).traceError(e);
            } else {
                Log.e(e);
            }
        }
    }

    public static IDynamicHook getHookById(int index) {
        return queryAllAnnotatedHooks()[index];
    }

    public static void doSetupAndInit(@NonNull Context context, @NonNull IDynamicHook hook) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
