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

package io.github.duzhaokun123.hook

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.core.content.FileProvider
import com.github.kyuubiran.ezxhelper.utils.getObjectByType
import com.github.kyuubiran.ezxhelper.utils.getObjectByTypeAs
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.SharePanelSceneData
import io.github.qauxv.util.dexkit.SharePanel_Handler_OtherApp_openImageByOtherApp
import io.github.qauxv.util.dexkit.SharePanel_Handler_OtherApp_openVideoByOtherApp
import xyz.nextalone.util.SystemServiceUtils
import xyz.nextalone.util.clazz
import java.io.File
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

@UiItemAgentEntry
@FunctionHookEntry
object PhotoShareHook : CommonSwitchFunctionHook(targets = arrayOf(
    SharePanelSceneData, SharePanel_Handler_OtherApp_openImageByOtherApp, SharePanel_Handler_OtherApp_openVideoByOtherApp
)) {
    override val name = "聊天照片分享增强"
    override val description = "替换外部打开为打开分享复制"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    override fun initOnce(): Boolean {
        DexKit.requireClassFromCache(SharePanelSceneData)
            .constructors[0]
            .hookBefore {
                @Suppress("UNCHECKED_CAST")
                val channels = it.args[2] as List<Any>
                channels.findLast { channel ->
                    channel.getObjectByType(String::class.java) == "otherapp"
                }!!.let { shareOtherApp ->
                    val a = shareOtherApp.javaClass.superclass!!
                    a.declaredFields.forEach { field ->
                        if (field.type == Int::class.javaPrimitiveType) {
                            field.isAccessible = true
                            field.setInt(shareOtherApp, R.drawable.ic_item_share_72dp)
                        }
                        if (field.type == CharSequence::class.java) {
                            field.isAccessible = true
                            field.set(shareOtherApp, "外部分享")
                        }
                    }
                }
            }
        val class_WeakReference = "Lmqq/util/WeakReference;".clazz!!
        DexKit.requireMethodFromCache(SharePanel_Handler_OtherApp_openImageByOtherApp)
            .hookReplace {
                val activity = it.thisObject.getObjectByTypeAs<WeakReference<Activity>>(class_WeakReference).get()!!
                val context = CommonContextWrapper.createMaterialDesignContext(activity)
                val imageShareData = it.args[0] as Parcelable
                val parcel = Parcel.obtain()
                imageShareData.writeToParcel(parcel, 0)
                parcel.setDataPosition(0)
                val filePath = parcel.readString()!!
                val title = parcel.readString()
                val desc = parcel.readString()
                parcel.recycle()
                val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", File(filePath))
                showShareDialog(context, uri, "image/*", title, desc)
            }
        DexKit.requireMethodFromCache(SharePanel_Handler_OtherApp_openVideoByOtherApp)
            .hookReplace {
                val activity = it.thisObject.getObjectByTypeAs<WeakReference<Activity>>(class_WeakReference).get()!!
                val context = CommonContextWrapper.createMaterialDesignContext(activity)
                val videoShareData = it.args[0] as Parcelable
                val parcel = Parcel.obtain()
                videoShareData.writeToParcel(parcel, 0)
                parcel.setDataPosition(0)
                val filePath = parcel.readString()!!
                val coverUrl = parcel.readString()
                val title = parcel.readString()
                val desc = parcel.readString()
                parcel.recycle()
                val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", File(filePath))
                showShareDialog(context, uri, "video/*", title, desc)
            }
        return true
    }

    fun showShareDialog(
        context: Context, uri: Uri, type: String,
        title: String?, desc: String?,
    ) {
        AlertDialog.Builder(context)
            .setTitle("外部分享")
            .setItems(arrayOf("打开", "分享", "复制")) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, type)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "打开..."))
                    }
                    1 -> {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            setType(type)
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_TITLE, title)
                            putExtra(Intent.EXTRA_SUBJECT, desc)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "分享到"))
                    }
                    2 -> {
                        thread {
                            SystemServiceUtils.copyToClipboard(context, uri)
                        }
                    }
                }
            }.setNeutralButton(android.R.string.cancel, null)
            .show()
    }
}