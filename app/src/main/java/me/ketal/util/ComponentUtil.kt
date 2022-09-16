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

package me.ketal.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.github.kyuubiran.ezxhelper.utils.argTypes
import com.github.kyuubiran.ezxhelper.utils.args
import com.github.kyuubiran.ezxhelper.utils.getObjectAs
import com.github.kyuubiran.ezxhelper.utils.invokeMethod
import me.kyuubiran.util.loadClass
import java.io.File

fun ComponentName.getEnable(ctx: Context): Boolean {
    val packageManager: PackageManager = ctx.packageManager
    val list = packageManager.queryIntentActivities(
        Intent().setComponent(this), PackageManager.MATCH_DEFAULT_ONLY)
    return list.isNotEmpty()
}

fun ComponentName.setEnable(ctx: Context, enabled: Boolean) {
    val packageManager: PackageManager = ctx.packageManager
    if (this.getEnable(ctx) == enabled) return
    packageManager.setComponentEnabledSetting(this,
        if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP)
}

fun getAllActivity(file: File): List<Any> {
    val parser = loadClass("android.content.pm.PackageParser").newInstance()
    val pkg = parser.invokeMethod("parsePackage", args(file, 1), argTypes(File::class.java, Int::class.java))
    return pkg!!.getObjectAs("activities")
}
