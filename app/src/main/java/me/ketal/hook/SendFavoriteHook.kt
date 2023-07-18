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
package me.ketal.hook

import android.app.Activity
import android.view.View
import android.widget.TextView
import cc.ioctl.util.Reflex
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.requireMinQQVersion
import me.ketal.base.PluginDelayableHook
import me.ketal.util.findClass
import me.ketal.util.getMethod
import xyz.nextalone.util.get
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object SendFavoriteHook : PluginDelayableHook("ketal_send_favorite") {
    override val preference = uiSwitchPreference {
        title = "发送收藏消息添加分组"
    }

    override val targetProcesses = SyncUtils.PROC_QQFAV
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_2_0)

    override val pluginID = "qqfav.apk"

    override fun startHook(classLoader: ClassLoader) = throwOrTrue {
        "Lcom/qqfav/activity/FavoritesListActivity;->onCreate(Landroid/os/Bundle;)V"
            .getMethod(classLoader)
            ?.hookAfter(this) {
                val thisObj = it.thisObject as Activity
                val isHooked = thisObj.intent.getBooleanExtra("bEnterToSelect", false)
                if (!isHooked) return@hookAfter
                val tv = findTitleTV(
                    thisObj,
                    "com.qqfav.activity.QfavBaseActivity".findClass(classLoader)
                )!!
                try {
                    val logic = Reflex.newInstance(
                        "com.qqfav.activity.FavoriteGroupLogic".findClass(classLoader),
                        thisObj, tv, thisObj::class.java, View::class.java
                    )

                    tv.setOnClickListener {
                        throwOrTrue {
                            Reflex.invokeVirtual(logic, "b")
                            val menu = logic.get("b", View::class.java)
                                ?: logic.get("f", View::class.java)!!
                            if (menu.height == 0 || menu.visibility != View.VISIBLE) {
                                // show
                                Reflex.invokeVirtual(logic, "a")
                            } else {
                                // hide
                                Reflex.invokeVirtual(logic, "a", true, Boolean::class.java)
                            }
                        }
                    }
                } catch (_: Exception) {
                    val logic = Reflex.newInstance(
                        "com.qqfav.activity.a".findClass(classLoader),
                        thisObj, tv, thisObj::class.java, View::class.java
                    )

                    tv.setOnClickListener {
                        throwOrTrue {
                            Reflex.invokeVirtual(logic, "a")
                            val menu = logic.get("h", View::class.java)!!
                            if (menu.height == 0 || menu.visibility != View.VISIBLE) {
                                // show
                                Reflex.invokeVirtual(logic, "f")
                            } else {
                                // hide
                                Reflex.invokeVirtual(logic, "b", true, Boolean::class.java)
                            }
                        }
                    }

                }
            }
    }

    private fun findTitleTV(thisObject: Any, clazz: Class<*>): TextView? {
        for (field in clazz.declaredFields) {
            field.isAccessible = true
            if (field[thisObject] is TextView) {
                val tv = field[thisObject] as TextView
                if (tv.text == "选择收藏") {
                    tv.text = "选择分组"
                    return tv
                }
            }
        }
        return null
    }
}
