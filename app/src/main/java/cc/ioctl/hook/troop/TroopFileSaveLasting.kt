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

package cc.ioctl.hook.troop

import android.view.View
import cc.ioctl.util.Reflex
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.LicenseStatus
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import me.ketal.base.PluginDelayableHook
import me.ketal.util.findClass
import me.ketal.util.hookMethod
import xyz.nextalone.util.invoke
import xyz.nextalone.util.throwOrTrue
import java.lang.reflect.Field

@[FunctionHookEntry UiItemAgentEntry]
object TroopFileSaveLasting : PluginDelayableHook("ketal_TroopFileSaveLasting") {
    override val preference = uiSwitchPreference {
        title = "群文件长按转存永久"
    }
    override val pluginID = if (requireMinQQVersion(QQVersion.QQ_9_1_50)) "" else "troop_plugin.apk"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.FILE_CATEGORY

    override fun startHook(classLoader: ClassLoader) = throwOrTrue {
        val troopFileShowAdapter = try { (
            if (requireMinQQVersion(QQVersion.QQ_9_1_50)) "com.tencent.mobileqq.troop.file.data.TroopFileShowAdapter$1"
            else "com.tencent.mobileqq.troop.data.TroopFileShowAdapter$1"
            ).findClass(classLoader).getDeclaredField("this$0").type
        } catch (e: Throwable) {
            "com.tencent.mobileqq.troop.data.TroopFileShowAdapter".findClass(classLoader)
        }
        val infoClass = troopFileShowAdapter.declaredFields.find {
            it.type == List::class.java
        }?.actualTypeArguments?.get(0) as Class<*>

        val itemClass = troopFileShowAdapter.declaredFields.find {
            it.type == Map::class.java
        }?.actualTypeArguments?.get(1) as Class<*>

        itemClass.declaredMethods.find {
            it.returnType == Boolean::class.java
                && it.parameterTypes.contentEquals(arrayOf(View::class.java))
        }?.hookMethod(object : XC_MethodHook() {
            lateinit var fields: List<Field>
            lateinit var tag: Any
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (!isEnabled or LicenseStatus.sDisableCommonHooks) return
                throwOrTrue {
                    val view = param.args[0] as View
                    tag = view.tag
                    val info = Reflex.getFirstByType(param.thisObject, infoClass)
                    fields = infoClass.declaredFields.filter {
                        it.isAccessible = true
                        it.type == Int::class.java
                            && it.get(info) == 102
                    }
                    fields.forEach {
                        it.set(info, 114514)
                    }
                    view.tag = info
                }
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                if (!isEnabled or LicenseStatus.sDisableCommonHooks) return
                throwOrTrue {
                    val view = param.args[0] as View
                    val info = view.tag
                    fields.forEach {
                        it.set(info, 102)
                    }
                    view.tag = tag
                }
            }
        })
    }

    private val Field.actualTypeArguments: Array<*>
        get() {
            return genericType.invoke("getActualTypeArguments") as Array<*>
        }
}
