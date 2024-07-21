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

package cc.ioctl.util;

import androidx.annotation.NonNull;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedBridge;
import io.github.qauxv.base.ITraceableDynamicHook;
import io.github.qauxv.hook.BaseFunctionHook;
import io.github.qauxv.hook.BaseHookDispatcher;
import io.github.qauxv.hook.BasePersistBackgroundHook;
import io.github.qauxv.util.LicenseStatus;
import java.lang.reflect.Method;
import java.util.Objects;

public class HookUtils {

    public interface BeforeHookedMethod {

        void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable;
    }

    public interface AfterHookedMethod {

        void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable;
    }

    public interface BeforeAndAfterHookedMethod {

        void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable;

        void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable;
    }

    public static void hookAfterIfEnabled(final @NonNull BaseFunctionHook this0, final @NonNull Method method,
                                          int priority, final @NonNull AfterHookedMethod afterHookedMethod) {
        Objects.requireNonNull(this0, "this0 == null");
        Objects.requireNonNull(method, "method == null");
        XposedBridge.hookMethod(method, new XC_MethodHook(priority) {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    if (this0.isEnabled() && !LicenseStatus.sDisableCommonHooks) {
                        afterHookedMethod.afterHookedMethod(param);
                    }
                } catch (Throwable e) {
                    this0.traceError(e);
                    throw e;
                }
            }
        });
    }

    public static XC_MethodHook beforeIfEnabled(final @NonNull BaseFunctionHook this0, int priority,
                                               final @NonNull BeforeHookedMethod beforeHookedMethod) {
        Objects.requireNonNull(this0, "this0 == null");
        return new XC_MethodHook(priority) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    if (this0.isEnabled() && !LicenseStatus.sDisableCommonHooks) {
                        beforeHookedMethod.beforeHookedMethod(param);
                    }
                } catch (Throwable e) {
                    this0.traceError(e);
                    throw e;
                }
            }
        };
    }

    public static XC_MethodHook afterIfEnabled(final @NonNull BaseFunctionHook this0, int priority,
                                               final @NonNull AfterHookedMethod afterHookedMethod) {
        Objects.requireNonNull(this0, "this0 == null");
        return new XC_MethodHook(priority) {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    if (this0.isEnabled() && !LicenseStatus.sDisableCommonHooks) {
                        afterHookedMethod.afterHookedMethod(param);
                    }
                } catch (Throwable e) {
                    this0.traceError(e);
                    throw e;
                }
            }
        };
    }

    public static XC_MethodHook beforeIfEnabled(final @NonNull BaseFunctionHook this0,
                                                final @NonNull BeforeHookedMethod beforeHookedMethod) {
        return beforeIfEnabled(this0, 50, beforeHookedMethod);
    }

    public static XC_MethodHook afterIfEnabled(final @NonNull BaseFunctionHook this0,
                                               final @NonNull AfterHookedMethod afterHookedMethod) {
        return afterIfEnabled(this0, 50, afterHookedMethod);
    }

    public static void hookAfterIfEnabled(final @NonNull BasePersistBackgroundHook this0, final @NonNull Method method,
                                          int priority, final @NonNull AfterHookedMethod afterHookedMethod) {
        Objects.requireNonNull(this0, "this0 == null");
        Objects.requireNonNull(method, "method == null");
        XposedBridge.hookMethod(method, new XC_MethodHook(priority) {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    if (this0.isEnabled() && !LicenseStatus.sDisableCommonHooks) {
                        afterHookedMethod.afterHookedMethod(param);
                    }
                } catch (Throwable e) {
                    this0.traceError(e);
                    throw e;
                }
            }
        });
    }

    public static void hookAfterIfEnabled(final @NonNull BaseHookDispatcher<?> this0, final @NonNull Method method,
                                          int priority, final @NonNull AfterHookedMethod afterHookedMethod) {
        Objects.requireNonNull(this0, "this0 == null");
        Objects.requireNonNull(method, "method == null");
        XposedBridge.hookMethod(method, new XC_MethodHook(priority) {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    if (this0.isEnabled() && !LicenseStatus.sDisableCommonHooks) {
                        afterHookedMethod.afterHookedMethod(param);
                    }
                } catch (Throwable e) {
                    this0.traceError(e);
                    throw e;
                }
            }
        });
    }

    public static void hookBeforeIfEnabled(final @NonNull BaseFunctionHook this0, final @NonNull Method method,
                                           int priority, final @NonNull BeforeHookedMethod beforeHookedMethod) {
        Objects.requireNonNull(this0, "this0 == null");
        Objects.requireNonNull(method, "method == null");
        XposedBridge.hookMethod(method, new XC_MethodHook(priority) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    if (this0.isEnabled() && !LicenseStatus.sDisableCommonHooks) {
                        beforeHookedMethod.beforeHookedMethod(param);
                    }
                } catch (Throwable e) {
                    this0.traceError(e);
                    throw e;
                }
            }
        });
    }

    public static void hookAfterIfEnabled(final @NonNull BaseFunctionHook this0, final @NonNull Method method,
                                          final @NonNull AfterHookedMethod afterHookedMethod) {
        hookAfterIfEnabled(this0, method, 50, afterHookedMethod);
    }

    public static void hookBeforeIfEnabled(final @NonNull BaseFunctionHook this0, final @NonNull Method method,
                                           final @NonNull BeforeHookedMethod beforeHookedMethod) {
        hookBeforeIfEnabled(this0, method, 50, beforeHookedMethod);
    }

    public static void hookAfterAlways(final @NonNull ITraceableDynamicHook this0, final @NonNull Method method,
                                       int priority, final @NonNull AfterHookedMethod afterHookedMethod) {
        Objects.requireNonNull(this0, "this0 == null");
        Objects.requireNonNull(method, "method == null");
        XposedBridge.hookMethod(method, afterAlways(this0, priority, afterHookedMethod));
    }

    public static void hookBeforeAlways(final @NonNull ITraceableDynamicHook this0, final @NonNull Method method,
                                        int priority, final @NonNull BeforeHookedMethod beforeHookedMethod) {
        Objects.requireNonNull(this0, "this0 == null");
        Objects.requireNonNull(method, "method == null");
        XposedBridge.hookMethod(method, beforeAlways(this0, priority, beforeHookedMethod));
    }

    public static XC_MethodHook beforeAlways(final @NonNull ITraceableDynamicHook this0, int priority,
                                             final @NonNull BeforeHookedMethod beforeHookedMethod) {
        return new XC_MethodHook(priority) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    beforeHookedMethod.beforeHookedMethod(param);
                } catch (Throwable e) {
                    this0.traceError(e);
                    throw e;
                }
            }
        };
    }

    public static XC_MethodHook afterAlways(final @NonNull ITraceableDynamicHook this0, int priority,
                                            final @NonNull AfterHookedMethod afterHookedMethod) {
        return new XC_MethodHook(priority) {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    afterHookedMethod.afterHookedMethod(param);
                } catch (Throwable e) {
                    this0.traceError(e);
                    throw e;
                }
            }
        };
    }

    public static void hookAfterAlways(final @NonNull ITraceableDynamicHook this0, final @NonNull Method method,
                                       final @NonNull AfterHookedMethod afterHookedMethod) {
        hookAfterAlways(this0, method, 50, afterHookedMethod);
    }

    public static void hookBeforeAlways(final @NonNull ITraceableDynamicHook this0, final @NonNull Method method,
                                        final @NonNull BeforeHookedMethod beforeHookedMethod) {
        hookBeforeAlways(this0, method, 50, beforeHookedMethod);
    }

    public static void hookBeforeAndAfterIfEnabled(final @NonNull ITraceableDynamicHook this0, final @NonNull Method method,
                                                   int priority, final @NonNull BeforeAndAfterHookedMethod hook) {
        Objects.requireNonNull(this0, "this0 == null");
        Objects.requireNonNull(method, "method == null");
        XposedBridge.hookMethod(method, new XC_MethodHook(priority) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (this0.isEnabled() && !LicenseStatus.sDisableCommonHooks) {
                    try {
                        hook.beforeHookedMethod(param);
                    } catch (Throwable e) {
                        this0.traceError(e);
                        throw e;
                    }
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (this0.isEnabled() && !LicenseStatus.sDisableCommonHooks) {
                    try {
                        hook.afterHookedMethod(param);
                    } catch (Throwable e) {
                        this0.traceError(e);
                        throw e;
                    }
                }
            }
        });
    }
}
