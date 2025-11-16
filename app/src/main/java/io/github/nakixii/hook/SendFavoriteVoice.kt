/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
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

package io.github.nakixii.hook

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import cc.ioctl.util.HostInfo
import com.github.kyuubiran.ezxhelper.utils.getFieldByType
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.paramCount
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.data.ContactDescriptor
import io.github.qauxv.util.hostInfo
import io.github.qauxv.util.requireMinQQVersion
import me.ketal.base.PluginDelayableHook
import me.ketal.util.findClass
import nep.timeline.MessageUtils
import xyz.nextalone.util.isFinal
import xyz.nextalone.util.isPublic
import xyz.nextalone.util.method
import xyz.nextalone.util.set
import androidx.core.view.isVisible

@FunctionHookEntry
@UiItemAgentEntry
object SendFavoriteVoice : PluginDelayableHook("send_favorite_voice") {
    override val preference = uiSwitchPreference {
        title = "允许发送收藏的语音"
    }
    override val pluginID = "qqfav.apk"
    override val targetProcesses = SyncUtils.PROC_MAIN or SyncUtils.PROC_QQFAV
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_9_1_70)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun startHook(classLoader: ClassLoader): Boolean {
        "com.qqfav.FavoriteService".findClass(classLoader).method {
            it.returnType == "com.qqfav.data.FavoriteData".findClass(classLoader)
                && it.parameterTypes.contentEquals(arrayOf(Long::class.java, Boolean::class.java))
        }?.hookAfter {
            it.result.set("mSecurityBeat", 0)
        }

        val favoritesListActivity = "com.qqfav.activity.FavoritesListActivity".findClass(classLoader)
        // 多选转发时，这个方法会导致提示有部分内容不能转发（实际上可以）
        favoritesListActivity.declaredFields.first {
            it.type.superclass != null && it.type.superclass == BaseAdapter::class.java
        }.type.method {
            it.returnType == Int::class.java && it.parameterTypes.contentEquals(arrayOf(java.util.List::class.java))
        }?.hookBefore { it.result = 0 }

        val favoritesListAdapter = favoritesListActivity.declaredFields.first {
            it.type.superclass != null && it.type.superclass.typeName == BaseAdapter::class.java.typeName
        }.apply { isAccessible = true }
        val itemViewFactoryFLA = favoritesListAdapter.type.declaredFields
            .first { it.isPublic }.apply { isAccessible = true }
        val getUin = itemViewFactoryFLA.type.method {
            it.returnType == String::class.java && it.paramCount == 0
        }.apply { this?.isAccessible = true }
        val getUinType = itemViewFactoryFLA.type.method {
            it.returnType == Int::class.java && it.paramCount == 0
        }.apply { this?.isAccessible = true }

        val qfavAppInterface = favoritesListActivity.getFieldByType("com.qqfav.QfavAppInterface"
            .findClass(classLoader)).apply { isAccessible = true }
        val getFavoriteService = qfavAppInterface.type.method("getFavoriteService")
        val getFilePath = "com.qqfav.FavoriteService".findClass(classLoader).method {
            it.isPublic && it.isFinal && it.returnType == String::class.java &&
                it.parameterTypes.contentEquals(arrayOf(Long::class.java))
        }.apply { this?.isAccessible = true }

        // 收藏列表
        favoritesListActivity.method {
            it.parameterTypes.contentEquals(arrayOf(ArrayList::class.java))
        }?.hookBefore { it ->
            val itemViewFactory = itemViewFactoryFLA.get(favoritesListAdapter.get(it.thisObject))
            val uin = getUin?.invoke(itemViewFactory) as? String ?: return@hookBefore
            val uinType = getUinType?.invoke(itemViewFactory) as? Int ?: return@hookBefore

            var hasOtherTypes = false
            for (favId in (it.args[0] as java.util.ArrayList<*>)) {
                val favoriteService = getFavoriteService?.invoke(qfavAppInterface.get(it.thisObject))
                val filePath = getFilePath?.invoke(favoriteService, favId) as String
                if (!filePath.isEmpty()) sendVoiceMsgBroadcast(filePath, uin, uinType)
                else hasOtherTypes = true
            }

            if (!hasOtherTypes) {
                (it.thisObject as Activity).finish()
                it.result = null
            }
        }

        val audioItemViewHolder = "com.qqfav.activity.AudioItemViewHolder".findClass(classLoader)
        val checkBox = audioItemViewHolder.getFieldByType(CheckBox::class.java).apply { isAccessible = true }
        val itemViewFactoryIVH = audioItemViewHolder.superclass.declaredFields
            .first { it.type.name.contains("com.qqfav.activity") }.apply { isAccessible = true }

        val favoriteData = audioItemViewHolder.getFieldByType("com.qqfav.data.FavoriteData"
            .findClass(classLoader)).apply { isAccessible = true }
        val getFavId = favoriteData.type.getMethod("getId").apply { isAccessible = true }
        val baseActivity = audioItemViewHolder.getFieldByType("mqq.app.BaseActivity"
            .findClass(Initiator.getHostClassLoader())).apply { isAccessible = true }

        // 搜索结果列表
        audioItemViewHolder.method("onClick")?.hookBefore { it ->
            // 不绕过其他 View 会导致无法在收藏界面播放语音
            if (it.args[0] !is FrameLayout) return@hookBefore

            // 绕过收藏列表，否则无法多选发送；收藏列表的语音在上面处理
            val checkBoxObj = checkBox.get(it.thisObject) as CheckBox
            if (checkBoxObj.isVisible) return@hookBefore

            val itemViewFactory = itemViewFactoryIVH.get(it.thisObject)
            val uin = getUin?.invoke(itemViewFactory) as? String ?: return@hookBefore
            val uinType = getUinType?.invoke(itemViewFactory) as? Int ?: return@hookBefore

            val favoriteDataObj = favoriteData.get(it.thisObject) ?: return@hookBefore
            val favId = getFavId(favoriteDataObj) as Long

            val baseActivityObj = baseActivity.get(it.thisObject) as Activity
            val favoriteService = getFavoriteService?.invoke(qfavAppInterface.get(baseActivityObj))
            val filePath = getFilePath?.invoke(favoriteService, favId) as String
            if (!filePath.isEmpty()) sendVoiceMsgBroadcast(filePath, uin, uinType)

            baseActivityObj.finish()
            it.result = null
        }

        if (SyncUtils.isTargetProcess(SyncUtils.PROC_MAIN)) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val filePath = intent.getStringExtra("file_path") ?: return
                    MessageUtils.sendVoice(filePath, ContactDescriptor().apply {
                        uin = intent.getStringExtra("uin")
                        uinType = intent.getIntExtra("uinType", -1)
                    })
                }
            }

            val intentFilter = IntentFilter()
            intentFilter.addAction("io.github.nakixii.SEND_VOICE_MESSAGE")
            ContextCompat.registerReceiver(
                hostInfo.application, receiver, intentFilter,
                SyncUtils.getDynamicReceiverNotExportedPermission(hostInfo.application),
                null, ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        return true
    }

    private fun sendVoiceMsgBroadcast(filePath: String, uin: String, uinType: Int) {
        val intent = Intent("io.github.nakixii.SEND_VOICE_MESSAGE")
        intent.putExtra("file_path", filePath)
        intent.putExtra("uin", uin)
        intent.putExtra("uinType", uinType)
        intent.setPackage("com.tencent.mobileqq")
        HostInfo.getApplication().sendBroadcast(intent)
    }
}
