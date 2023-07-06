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
package io.github.qauxv.util

import android.content.Context
import cc.ioctl.util.Reflex
import com.github.kyuubiran.ezxhelper.utils.findMethod
import io.github.qauxv.util.dexkit.AbstractQQCustomMenuItem
import io.github.qauxv.util.dexkit.DexKit
import net.bytebuddy.ByteBuddy
import net.bytebuddy.android.AndroidClassLoadingStrategy
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.matcher.ElementMatchers
import java.lang.reflect.Field

object CustomMenu {
    @Throws(ReflectiveOperationException::class)
    fun createItem(clazz: Class<*>, id: Int, title: String?, icon: Int): Any {
        return try {
            try {
                val initWithArgv = clazz.getConstructor(Int::class.javaPrimitiveType, String::class.java, Int::class.javaPrimitiveType)
                initWithArgv.newInstance(id, title, icon)
            } catch (unused: NoSuchMethodException) {
                //no direct constructor, reflex
                val item = createItem(clazz, id, title)
                var f: Field? = Reflex.findFieldOrNull(clazz, Int::class.javaPrimitiveType, "b")
                if (f == null) {
                    f = Reflex.findField(clazz, Int::class.javaPrimitiveType, "icon")
                }
                f!!.isAccessible = true
                f[item] = icon
                item
            }
        } catch (e: ReflectiveOperationException) {
            Log.w(e.toString())
            //sign... drop icon
            createItem(clazz, id, title)
        }
    }

    @JvmStatic
    @Throws(ReflectiveOperationException::class)
    fun createItem(clazz: Class<*>, id: Int, title: String?): Any {
        try {
            val initWithArgv = clazz.getConstructor(Int::class.javaPrimitiveType, String::class.java)
            return initWithArgv.newInstance(id, title)
        } catch (ignored: NoSuchMethodException) {
        } catch (e: IllegalAccessException) {
            throw AssertionError(e)
        }
        val item: Any = clazz.newInstance()
        var f: Field? = Reflex.findFieldOrNull(clazz, Int::class.javaPrimitiveType, "id")
        if (f == null) {
            f = Reflex.findField(clazz, Int::class.javaPrimitiveType, "a")
        }
        f!!.isAccessible = true
        f[item] = id
        f = Reflex.findFieldOrNull(clazz, String::class.java, "title")
        if (f == null) {
            f = Reflex.findField(clazz, String::class.java, "a")
        }
        f!!.isAccessible = true
        f[item] = title
        return item
    }


    private val strategy by lazy {
        AndroidClassLoadingStrategy.Wrapping(
            hostInfo.application.getDir(
                "generated",
                Context.MODE_PRIVATE
            )
        )
    }

    @JvmStatic
    fun createItemNt(msg: Any, text: String, id: Int, click: () -> Unit): Any {
        val msgClass = Initiator.loadClass("com.tencent.mobileqq.aio.msg.AIOMsgItem")
        val absMenuItem = DexKit.loadClassFromCache(AbstractQQCustomMenuItem)!!
        val clickName = absMenuItem.findMethod {
            returnType == Void.TYPE && parameterTypes.isEmpty()
        }.name
        val menuItemClass = ByteBuddy()
            .subclass(absMenuItem)
            .method(ElementMatchers.returns(String::class.java))
            .intercept(FixedValue.value(text))
            .method(ElementMatchers.returns(Int::class.java))
            .intercept(FixedValue.value(id))
            .method(ElementMatchers.named(clickName))
            .intercept(MethodCall.call { click() })
            .make()
            .load(absMenuItem.classLoader, strategy)
            .loaded
        return menuItemClass.getDeclaredConstructor(msgClass)
            .newInstance(msg)
    }

    fun checkArrayElementNonNull(array: Array<Any?>?) {
        if (array == null) {
            throw NullPointerException("array is null")
        }
        for (i in array.indices) {
            if (array[i] == null) {
                throw NullPointerException("array[" + i + "] is null, length=" + array.size)
            }
        }
    }
}
