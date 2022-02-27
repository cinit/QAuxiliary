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
package me.kyuubiran.util

import android.content.Context
import android.os.Looper
import android.view.View
import android.widget.Toast
import cc.ioctl.util.HookUtils
import cc.ioctl.util.Reflex
import de.robv.android.xposed.XC_MethodHook
import io.github.qauxv.SyncUtils
import io.github.qauxv.hook.BaseFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Toasts
import java.lang.reflect.Method

fun Context.showToastByTencent(
    text: CharSequence,
    type: Int = Toasts.TYPE_INFO,
    duration: Int = Toast.LENGTH_SHORT
) {
    if (Looper.getMainLooper() == Looper.myLooper())
        Toasts.showToast(this, type, text, duration)
    else SyncUtils.runOnUiThread { showToastByTencent(text, duration) }
}

fun View.setViewZeroSize() {
    this.layoutParams.height = 0
    this.layoutParams.width = 0
}

fun getObjectOrNull(obj: Any?, objName: String, clz: Class<*>? = null): Any? {
    return Reflex.getInstanceObjectOrNull(obj, objName, clz)
}

fun putObject(obj: Any, name: String, value: Any?, type: Class<*>? = null) {
    Reflex.setInstanceObject(obj, name, type, value)
}

fun loadClass(clzName: String): Class<*> {
    return Initiator.load(clzName)
}

fun getMethods(clzName: String): Array<Method> {
    return Initiator.load(clzName).declaredMethods
}

fun makeSpaceMsg(str: String): String {
    val sb = StringBuilder()
    if (str.length > 1) {
        for (i in str.indices) {
            sb.append(str[i])
            if (i != str.length - 1) sb.append(" ")
        }
    } else {
        sb.append(str)
    }
    return sb.toString()
}
