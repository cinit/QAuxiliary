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

package cc.ioctl.util

import dalvik.system.BaseDexClassLoader
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.hook.BaseFunctionHook
import io.github.qauxv.util.LicenseStatus
import java.lang.reflect.Constructor
import java.lang.reflect.Method

fun BaseFunctionHook.hookBeforeIfEnabled(m: Method, priority: Int, hook: (XC_MethodHook.MethodHookParam) -> Unit) {
    XposedBridge.hookMethod(m, object : XC_MethodHook(priority) {
        override fun beforeHookedMethod(param: MethodHookParam) {
            try {
                if (isEnabled && !LicenseStatus.sDisableCommonHooks) {
                    hook(param)
                }
            } catch (e: Throwable) {
                traceError(e)
            }
        }
    })
}

fun BaseFunctionHook.hookBeforeIfEnabled(m: Method, hook: (XC_MethodHook.MethodHookParam) -> Unit) {
    hookBeforeIfEnabled(m, 50, hook)
}

fun BaseFunctionHook.hookAfterIfEnabled(m: Method, priority: Int, hook: (XC_MethodHook.MethodHookParam) -> Unit) {
    XposedBridge.hookMethod(m, object : XC_MethodHook(priority) {
        override fun afterHookedMethod(param: MethodHookParam) {
            try {
                if (isEnabled && !LicenseStatus.sDisableCommonHooks) {
                    hook(param)
                }
            } catch (e: Throwable) {
                traceError(e)
            }
        }
    })
}

fun BaseFunctionHook.hookAfterIfEnabled(m: Constructor<*>, priority: Int, hook: (XC_MethodHook.MethodHookParam) -> Unit) {
    XposedBridge.hookMethod(m, object : XC_MethodHook(priority) {
        override fun afterHookedMethod(param: MethodHookParam) {
            try {
                if (isEnabled && !LicenseStatus.sDisableCommonHooks) {
                    hook(param)
                }
            } catch (e: Throwable) {
                traceError(e)
            }
        }
    })
}

fun BaseFunctionHook.hookAfterIfEnabled(m: Method, hook: (XC_MethodHook.MethodHookParam) -> Unit) {
    hookAfterIfEnabled(m, 50, hook)
}

fun BaseFunctionHook.hookAfterIfEnabled(m: Constructor<*>, hook: (XC_MethodHook.MethodHookParam) -> Unit) {
    hookAfterIfEnabled(m, 50, hook)
}

fun BaseFunctionHook.afterHookIfEnabled(priority: Int, hook: (XC_MethodHook.MethodHookParam) -> Unit): XC_MethodHook {
    return object : XC_MethodHook(priority) {
        override fun afterHookedMethod(param: MethodHookParam) {
            try {
                if (isEnabled && !LicenseStatus.sDisableCommonHooks) {
                    hook(param)
                }
            } catch (e: Throwable) {
                traceError(e)
            }
        }
    }
}

fun BaseFunctionHook.afterHookIfEnabled(hook: (XC_MethodHook.MethodHookParam) -> Unit): XC_MethodHook {
    return afterHookIfEnabled(50, hook)
}

fun BaseFunctionHook.beforeHookIfEnabled(priority: Int, hook: (XC_MethodHook.MethodHookParam) -> Unit): XC_MethodHook {
    return object : XC_MethodHook(priority) {
        override fun beforeHookedMethod(param: MethodHookParam) {
            try {
                if (isEnabled && !LicenseStatus.sDisableCommonHooks) {
                    hook(param)
                }
            } catch (e: Throwable) {
                traceError(e)
            }
        }
    }
}

fun BaseFunctionHook.beforeHookIfEnabled(hook: (XC_MethodHook.MethodHookParam) -> Unit): XC_MethodHook {
    return beforeHookIfEnabled(50, hook)
}

fun ClassLoader.findDexClassLoader(): BaseDexClassLoader? {
    var classLoader = this
    while (classLoader !is BaseDexClassLoader) {
        if (classLoader.parent != null) classLoader = classLoader.parent
        else return null
    }
    return classLoader
}
