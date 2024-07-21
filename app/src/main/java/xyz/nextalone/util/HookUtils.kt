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
package xyz.nextalone.util

import android.os.Looper
import cc.ioctl.util.Reflex
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XC_MethodReplacement
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.util.xpcompat.XposedHelpers
import io.github.qauxv.base.IDynamicHook
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.hook.BaseFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.DexMethodDescriptor
import io.github.qauxv.util.hostInfo
import io.github.qauxv.config.ConfigManager.getDefaultConfig
import io.github.qauxv.config.ConfigManager.getExFriendCfg
import xyz.nextalone.bridge.NAMethodHook
import xyz.nextalone.bridge.NAMethodReplacement
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal val String.clazz: Class<*>?
    get() = Initiator.load(this).also {
        if (it == null) {
            Log.e(ClassNotFoundException("Class $this not found"))
        }
    }

internal val ArrayList<String>.clazz: Class<*>
    get() = firstNotNullOf { Initiator.load(it) }

internal val String.method: Method
    get() = DexMethodDescriptor(
        this.replace(".", "/").replace(" ", "")
    ).getMethodInstance(Initiator.getHostClassLoader())

internal fun Class<*>.method(name: String): Method? = this.declaredMethods.run {
    this.forEach {
        if (it.name == name) {
            return it
        }
    }
    Log.w(NoSuchMethodException("No such method $name in $this"))
    null
}

internal fun Class<*>.method(name: String, returnType: Class<*>?, vararg argsTypes: Class<*>?): Method? = this.run {
    this.declaredMethods.forEach {
        if (name == it.name && returnType == it.returnType && it.parameterTypes.contentEquals(argsTypes)) {
            return it
        }
    }
    Log.w(NoSuchMethodException("No such method $name in ${this.simpleName} with returnType $returnType and argsTypes " + argsTypes.joinToString()))
    null
}

internal fun Class<*>.methodWithSuper(name: String, returnType: Class<*>?, vararg argsTypes: Class<*>?): Method? = this.run {
    (this.declaredMethods + this.methods).forEach {
        if (name == it.name && returnType == it.returnType && it.parameterTypes.contentEquals(argsTypes)) {
            return it
        }
    }
    Log.w(NoSuchMethodException("No such method $name in ${this.simpleName} with returnType $returnType and argsTypes " + argsTypes.joinToString()))
    null
}

internal fun Class<*>.method(
    condition: (method: Method) -> Boolean = { true }
): Method? = this.run {
    this.declaredMethods.forEach {
        if (condition(it)) {
            return it
        }
    }
    Log.w(NoSuchMethodException("No such method in ${this.simpleName}"))
    null
}

internal fun Class<*>.method(
    size: Int,
    returnType: Class<*>?,
    condition: (method: Method) -> Boolean = { true }
): Method? = this.run {
    this.declaredMethods.forEach {
        if (it.returnType == returnType && it.parameterTypes.size == size && condition(it)) {
            return it
        }
    }
    Log.w(NoSuchMethodException("No such method in ${this.simpleName} with returnType $returnType and argsSize $size"))
    null
}

internal fun Class<*>.method(
    name: String,
    size: Int,
    returnType: Class<*>?,
    condition: (method: Method) -> Boolean = { true }
): Method? = this.run {
    this.declaredMethods.forEach {
        if (it.name == name && it.returnType == returnType && it.parameterTypes.size == size && condition(it)) {
            return it
        }
    }
    Log.w(NoSuchMethodException("No such method $name in ${this.simpleName} with returnType $returnType and argsSize $size"))
    null
}

internal val Member.isStatic: Boolean
    get() = Modifier.isStatic(this.modifiers)

internal val Member.isPrivate: Boolean
    get() = Modifier.isPrivate(this.modifiers)

internal val Member.isPublic: Boolean
    get() = Modifier.isPublic(this.modifiers)

internal val Member.isFinal: Boolean
    get() = Modifier.isFinal(this.modifiers)

internal val Class<*>.isAbstract: Boolean
    get() = Modifier.isAbstract(this.modifiers)

internal fun <T : IDynamicHook> T.throwOrTrue(function: () -> Unit): Boolean {
    if (!this.isAvailable) return false
    function()
    return true
}

internal fun Any?.get(name: String): Any? = this.get(name, null)

internal fun <T> Any?.get(name: String, type: Class<out T>? = null): T? =
    Reflex.getInstanceObjectOrNull(this, name, type)

internal fun <T> Any?.get(type: Class<out T>? = null): T? {
    var clz = this?.javaClass
    while (clz != null && clz != Any::class.java) {
        for (f in clz.declaredFields) {
            if (f.type != type) {
                continue
            }
            f.isAccessible = true
            try {
                return f[this] as T
            } catch (ignored: IllegalAccessException) {
                //should not happen
            }
        }
        clz = clz.superclass
    }
    return null
}

internal fun <T> Any?.getAll(type: Class<out T>? = null): MutableList<T> {
    var clz = this?.javaClass
    val objMutableList = mutableListOf<T>()
    while (clz != null && clz != Any::class.java) {
        for (f in clz.declaredFields) {
            if (f.type != type) {
                continue
            }
            f.isAccessible = true
            try {
                objMutableList.add(f[this] as T)
            } catch (ignored: IllegalAccessException) {
                //should not happen
            }
        }
        clz = clz.superclass
    }
    return objMutableList
}

internal fun Any.set(name: String, value: Any): Any = Reflex.setInstanceObject(this, name, value)

internal fun Any.set(name: String, type: Class<*>?, value: Any): Any =
    Reflex.setInstanceObject(this, name, type, value)

internal fun Class<*>?.instance(vararg arg: Any?): Any = XposedHelpers.newInstance(this, *arg)

internal fun Class<*>?.instance(type: Array<Class<*>>, vararg arg: Any?): Any =
    XposedHelpers.newInstance(this, type, *arg)

internal fun Any.invoke(name: String, vararg args: Any): Any? =
    Reflex.invokeVirtual(this, name, *args)

internal fun Member.hook(callback: NAMethodHook) = try {
    XposedBridge.hookMethod(this, callback)
} catch (e: Throwable) {
    Log.e(e)
    null
}

internal fun Member.hookBefore(
    baseHook: IDynamicHook,
    hooker: (XC_MethodHook.MethodHookParam) -> Unit
) = hook(object : NAMethodHook(baseHook) {
    override fun beforeMethod(param: MethodHookParam) = try {
        hooker(param)
    } catch (e: Throwable) {
        if (baseHook is BaseFunctionHook) {
            baseHook.traceError(e)
        } else {
            Log.e(e)
        }
    }
})

internal fun Member.hookAfter(
    baseHook: IDynamicHook,
    hooker: (XC_MethodHook.MethodHookParam) -> Unit
) = hook(object : NAMethodHook(baseHook) {
    override fun afterMethod(param: MethodHookParam) = try {
        hooker(param)
    } catch (e: Throwable) {
        if (baseHook is BaseFunctionHook) {
            baseHook.traceError(e)
        } else {
            Log.e(e)
        }
    }
})

internal fun Member.replace(baseHook: IDynamicHook, result: Any?) = this.replace(baseHook) {
    result
}

internal fun <T : Any> Member.replace(
    baseHook: IDynamicHook,
    hooker: (XC_MethodHook.MethodHookParam) -> T?
) = hook(object : NAMethodReplacement(baseHook) {
    override fun replaceMethod(param: MethodHookParam) = try {
        hooker(param)
    } catch (e: Throwable) {
        if (baseHook is BaseFunctionHook) {
            baseHook.traceError(e)
        } else {
            Log.e(e)
        }
        null
    }
})

internal fun Class<*>.hook(method: String?, vararg args: Any?) = try {
    XposedHelpers.findAndHookMethod(this, method, *args)
} catch (e: NoSuchMethodError) {
    Log.e(e)
    null
} catch (e: XposedHelpers.ClassNotFoundError) {
    Log.e(e)
    null
} catch (e: ClassNotFoundException) {
    Log.e(e)
    null
}

internal fun Class<*>.hookBefore(
    baseHook: IDynamicHook,
    method: String?,
    vararg args: Any?,
    hooker: (XC_MethodHook.MethodHookParam) -> Unit
) = hook(method, *args, object : NAMethodHook(baseHook) {
    override fun beforeMethod(param: MethodHookParam) = try {
        hooker(param)
    } catch (e: Throwable) {
        Log.e(e)
    }
})

internal fun Class<*>.hookAfter(
    baseHook: IDynamicHook,
    method: String?,
    vararg args: Any?,
    hooker: (XC_MethodHook.MethodHookParam) -> Unit
) = hook(method, *args, object : NAMethodHook(baseHook) {
    override fun afterMethod(param: MethodHookParam) = try {
        hooker(param)
    } catch (e: Throwable) {
        Log.e(e)
    }
})

internal fun Class<*>.replace(
    method: String?,
    vararg args: Any?,
    hooker: (XC_MethodHook.MethodHookParam) -> Any?
) = hook(method, *args, object : XC_MethodReplacement() {
    override fun replaceHookedMethod(param: MethodHookParam) = try {
        hooker(param)
    } catch (e: Throwable) {
        Log.e(e)
        null
    }
})

internal fun Class<*>.hookAllMethods(
    methodName: String?,
    hooker: XC_MethodHook
): Set<XC_MethodHook.Unhook> = try {
    XposedBridge.hookAllMethods(this, methodName, hooker)
} catch (e: NoSuchMethodError) {
    Log.e(e)
    emptySet()
} catch (e: XposedHelpers.ClassNotFoundError) {
    Log.e(e)
    emptySet()
} catch (e: ClassNotFoundException) {
    Log.e(e)
    emptySet()
}

internal fun Class<*>.hookBeforeAllMethods(
    baseHook: IDynamicHook,
    methodName: String?,
    hooker: (XC_MethodHook.MethodHookParam) -> Unit
): Set<XC_MethodHook.Unhook> = hookAllMethods(methodName, object : NAMethodHook(baseHook) {
    override fun beforeMethod(param: MethodHookParam) = try {
        hooker(param)
    } catch (e: Throwable) {
        Log.e(e)
    }
})

internal fun Class<*>.hookAfterAllMethods(
    baseHook: IDynamicHook,
    methodName: String?,
    hooker: (XC_MethodHook.MethodHookParam) -> Unit
): Set<XC_MethodHook.Unhook> = hookAllMethods(methodName, object : NAMethodHook(baseHook) {
    override fun afterMethod(param: MethodHookParam) = try {
        hooker(param)
    } catch (e: Throwable) {
        Log.e(e)
    }
})

internal fun Class<*>.replaceAfterAllMethods(
    methodName: String?,
    hooker: (XC_MethodHook.MethodHookParam) -> Any?
): Set<XC_MethodHook.Unhook> = hookAllMethods(methodName, object : XC_MethodReplacement() {
    override fun replaceHookedMethod(param: MethodHookParam) = try {
        hooker(param)
    } catch (e: Throwable) {
        Log.e(e)
        null
    }
})

internal fun Class<*>.hookAllConstructors(hooker: XC_MethodHook): Set<XC_MethodHook.Unhook> = try {
    XposedBridge.hookAllConstructors(this, hooker)
} catch (e: NoSuchMethodError) {
    Log.e(e)
    emptySet()
} catch (e: XposedHelpers.ClassNotFoundError) {
    Log.e(e)
    emptySet()
} catch (e: ClassNotFoundException) {
    Log.e(e)
    emptySet()
}

internal fun Class<*>.hookBeforeAllConstructors(hooker: (XC_MethodHook.MethodHookParam) -> Unit) =
    hookAllConstructors(object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) = try {
            hooker(param)
        } catch (e: Throwable) {
            Log.e(e)
        }
    })

internal fun Class<*>.hookAfterAllConstructors(hooker: (XC_MethodHook.MethodHookParam) -> Unit) =
    hookAllConstructors(object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) = try {
            hooker(param)
        } catch (e: Throwable) {
            Log.e(e)
        }
    })

internal fun putValue(keyName: String, obj: Any, mgr: ConfigManager) {
    try {
        mgr.putObject(keyName, obj)
        mgr.save()
    } catch (e: Exception) {
        Log.e(e)
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toasts.error(hostInfo.application, e.toString() + "")
        } else {
            SyncUtils.post { Toasts.error(hostInfo.application, e.toString() + "") }
        }
    }
}

internal fun putDefault(keyName: String, obj: Any) = putValue(keyName, obj, getDefaultConfig())

internal fun putExFriend(keyName: String, obj: Any) = getExFriendCfg()?.let { putValue(keyName, obj, it) }
