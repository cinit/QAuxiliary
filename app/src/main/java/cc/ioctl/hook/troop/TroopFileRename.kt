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

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.utils.findFieldObjectAs
import com.github.kyuubiran.ezxhelper.utils.getObjectByTypeAs
import com.github.kyuubiran.ezxhelper.utils.invokeMethodAs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tencent.mobileqq.app.QQAppInterface
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.bridge.TroopFileProtocol
import io.github.qauxv.bridge.protocol.TroopFileGetOneFileInfoObserver
import io.github.qauxv.bridge.protocol.TroopFileRenameObserver
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.requireMinQQVersion
import me.ketal.base.PluginDelayableHook
import me.ketal.data.TroopFileInfo
import me.ketal.util.findClass
import tencent.im.cs.group_file_common.group_file_common
import xyz.nextalone.data.TroopInfo
import xyz.nextalone.util.clazz
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.method
import xyz.nextalone.util.throwOrTrue
import java.lang.reflect.Modifier

@[FunctionHookEntry UiItemAgentEntry]
object TroopFileRename : PluginDelayableHook("ketal_TroopFileRename"), View.OnClickListener {
    override val pluginID = "troop_plugin.apk"
    override val preference = uiSwitchPreference {
        title = "群文件重命名"
    }
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.FILE_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_6_0)

    override fun startHook(classLoader: ClassLoader) = throwOrTrue {
        val builderClzName = if (requireMinQQVersion(QQVersion.QQ_9_0_20)) "i"
        else if (requireMinQQVersion(QQVersion.QQ_8_9_88)) "h"
        else if (requireMinQQVersion(QQVersion.QQ_8_9_0)) "g"
        else "TroopFileItemBuilder"
        val builder = ("com.tencent.mobileqq.troop.widget.$builderClzName").findClass(classLoader)
        builder.declaredMethods.find {
            val args = it.parameterTypes
            it.returnType == Void.TYPE && args[1].equals(View::class.java)
        }?.hookBefore(this) {
            val ctx = it.args[0]
            val obj = it.args[2] as Array<*>
            val booleans = obj[0] as BooleanArray
            val fileInfo = TroopFileInfo(obj[1]!!)
            val uploader = fileInfo.uploader
            val gid = ctx.findFieldObjectAs<String> {
                if (type != String::class.java) return@findFieldObjectAs false
                isAccessible = true
                runCatching { (get(ctx) as String).toLong() }.isSuccess
            }
            val troopInfo = TroopInfo(gid)
            val account = AppRuntimeHelper.getAccount()
            // the folder's size is 0, so it's a file
            if (fileInfo.size > 0) {
                if (account == uploader || account == troopInfo.troopOwnerUin || account in troopInfo.troopAdmin!!) {
                    // move rename delete
                    booleans[1] = true // show rename button if has permission
                }
            }
        }

        val clazzName = if (requireMinQQVersion(QQVersion.QQ_9_0_20)) "e"
        else if (requireMinQQVersion(QQVersion.QQ_8_9_88)) "d"
        else if (requireMinQQVersion(QQVersion.QQ_8_9_0)) "c"
        else "TrooFileTextViewMenuBuilder"
        val updateRightMenuItem =
            "Lcom/tencent/mobileqq/troop/widget/$clazzName;->updateRightMenuItem(ILjava/lang/Object;Lcom/tencent/widget/SwipRightMenuBuilder\$SwipRightMenuItem;Landroid/view/View\$OnClickListener;)Landroid/view/View;".method
        updateRightMenuItem.hookAfter(this) {
            val obj = it.args[1] as Array<*>
            val info = TroopFileInfo(obj[1]!!)
            val textview = it.result as View
            if ("重命名" != textview.invokeMethodAs<String>("getText")) return@hookAfter
            textview.setBackgroundColor(Color.parseColor("#2498F6"))
            if (info.size > 0) {
                // the folder's size is 0, so it's a file
                textview.setOnClickListener(this)
            }
        }
    }

    override fun onClick(v: View) {
        runCatching {
            val info = TroopFileInfo(v.tag)
            val item = (v.parent as View).tag
            val ctx = CommonContextWrapper.createMaterialDesignContext(v.context)
            val qQAppInterface = item.getObjectByTypeAs<QQAppInterface>(QQAppInterface::class.java)
            val gid = item.getObjectByTypeAs<Long>(Long::class.java)
            val tv = if (requireMinQQVersion(QQVersion.QQ_9_0_65)) {
                item.javaClass.declaredFields.first { it.type == TextView::class.java && Modifier.isProtected(it.modifiers) }
                    .apply { isAccessible = true }.get(item) as TextView
            } else {
                item.getObjectByTypeAs("com.tencent.mobileqq.troop.widget.EllipsizingTextView".clazz!!)
            }
            TroopFileProtocol.getFileInfo(qQAppInterface, gid, info.path, object : TroopFileGetOneFileInfoObserver() {
                override fun onResult(result: Boolean, code: Int, fileInfo: group_file_common.FileInfo?) {
                    if (!result || fileInfo == null) {
                        Toasts.error(ctx, "获取文件信息失败")
                        return
                    }
                    showInput(ctx, qQAppInterface, gid, fileInfo, tv)
                }
            })
        }.onFailure { traceError(it) }
    }

    private fun showInput(
        ctx: Context,
        qQAppInterface: QQAppInterface,
        gid: Long,
        fileInfo: group_file_common.FileInfo,
        tv: TextView
    ) {
        MaterialAlertDialogBuilder(ctx).apply {
            setTitle("请输入文件名称")
            val editTextPreference: EditText = EditText(ctx).apply {
                setText(fileInfo.str_file_name.get())
                hint = "重命名文件"
            }
            setView(editTextPreference)
            setPositiveButton("确定") { _, _ ->
                val newName = editTextPreference.text.toString()
                TroopFileProtocol.renameFile(qQAppInterface, gid, fileInfo, newName, object : TroopFileRenameObserver() {
                    override fun onResult(result: Boolean, code: Int, fileName: String, fileId: String) {
                        if (!result) {
                            Toasts.error(ctx, "重命名失败")
                            return
                        }
                        Toasts.success(ctx, "重命名成功")
                        tv.text = fileName
                    }
                })
            }
            setNegativeButton("取消") { _, _ -> }
            setCancelable(false)
        }.show()
    }
}
