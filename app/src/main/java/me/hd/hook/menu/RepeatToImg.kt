/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
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

package me.hd.hook.menu

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import cc.hicore.message.common.MsgSender
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.hookAfterIfEnabled
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import com.xiaoniu.dispatcher.OnMenuBuilder
import com.xiaoniu.util.ContextUtils
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.kernelcompat.ContactCompat
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.CustomMenu
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.AbstractQQCustomMenuItem
import io.github.qauxv.util.hostInfo
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedHelpers
import xyz.nextalone.util.findHostView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


@FunctionHookEntry
@UiItemAgentEntry
object RepeatToImg : CommonSwitchFunctionHook(
    targets = arrayOf(AbstractQQCustomMenuItem)
), OnMenuBuilder {

    override val name = "复读消息为图片"
    override val description = "消息菜单中新增功能, 长时间使用可能导致页面卡顿"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    private val msgViewHashMap = HashMap<Long, ViewGroup>()

    override fun initOnce(): Boolean {
        val clazz = Initiator.loadClass("com.tencent.mobileqq.aio.msglist.holder.AIOBubbleMsgItemVB")
        hookAfterIfEnabled(clazz.declaredMethods.single { method ->
            method.name == "onCreateView"
        }) { msgViewHashMap.clear() }
        hookAfterIfEnabled(clazz.declaredMethods.single { method ->
            val params = method.parameterTypes
            params.size == 4 && params[0] == Int::class.java && params[2] == List::class.java && params[3] == Bundle::class.java
        }) { param ->
            val msg = param.args[1]
            val msgRecord = XposedHelpers.callMethod(msg, "getMsgRecord") as MsgRecord
            clazz.declaredFields.single { field ->
                field.type == View::class.java
            }.apply {
                isAccessible = true
                msgViewHashMap[msgRecord.msgId] = get(param.thisObject) as ViewGroup
            }
        }
        return true
    }

    private fun getViewBitmap(view: ViewGroup): Bitmap {
        return Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).apply {
            view.setBackgroundColor(0xFFF1F1F1.toInt())
            view.draw(Canvas(this))
        }
    }

    private fun processBitmap(context: Context, view: ViewGroup, msgRecord: MsgRecord): Bitmap {
        val bitmap = getViewBitmap(view)
        val (contentId, avatarId, nickId) = when (hostInfo.versionCode) {
            QQVersion.QQ_9_0_75 -> Triple("ny6", "t6o", "tiq")
            QQVersion.QQ_9_0_0 -> Triple("nv2", "snz", "szp")
            QQVersion.QQ_8_9_88 -> Triple("ntl", "skw", "swf")
            else -> Triple("", "", "")
        }
        if (contentId.isNotEmpty() && avatarId.isNotEmpty() && nickId.isNotEmpty()) {
            val dp5 = LayoutHelper.dip2px(context, 5f)
            val contentView = view.findHostView<LinearLayout>(contentId)!!
            return if (msgRecord.chatType == 1) {//好友
                val avatarView = view.findHostView<FrameLayout>(avatarId)!!
                if (msgRecord.sendType == 0) {//别人
                    Bitmap.createBitmap(
                        bitmap,
                        0, avatarView.top - dp5, contentView.right, contentView.bottom - (avatarView.top - dp5)
                    )
                } else {//自己
                    Bitmap.createBitmap(
                        bitmap,
                        contentView.left, avatarView.top - dp5,
                        bitmap.width - contentView.left, contentView.bottom - (avatarView.top - dp5)
                    )
                }
            } else if (msgRecord.chatType == 2) {//群聊
                val nickView = view.findHostView<FrameLayout>(nickId)!!
                if (msgRecord.sendType == 0) {//别人
                    Bitmap.createBitmap(
                        bitmap,
                        0, nickView.top - dp5,
                        maxOf(contentView.right, nickView.right), contentView.bottom - (nickView.top - dp5)
                    )
                } else {//自己
                    Bitmap.createBitmap(
                        bitmap,
                        minOf(contentView.left, nickView.left), nickView.top - dp5,
                        bitmap.width - minOf(contentView.left, nickView.left), contentView.bottom - (nickView.top - dp5)
                    )
                }
            } else {
                bitmap
            }
        } else {
            return bitmap
        }
    }

    private fun sendBitmap(context: Context, view: ViewGroup, msgRecord: MsgRecord) {
        val bitmap = processBitmap(context, view, msgRecord)
        val imgFile = File(context.externalCacheDir, "hd_temp/img").apply { parentFile!!.mkdirs() }
        try {
            FileOutputStream(imgFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            MsgSender.send_pic_by_contact(
                ContactCompat(msgRecord.chatType, msgRecord.peerUid, ""),
                imgFile.absolutePath
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override val targetComponentTypes = arrayOf(
        "com.tencent.mobileqq.aio.msglist.holder.component.anisticker.AIOAniStickerContentComponent",
        if (requireMinQQVersion(QQVersion.QQ_9_1_55)) {
            "com.tencent.mobileqq.aio.msglist.holder.component.template.AIOTemplateMsgComponent"
        } else {
            "com.tencent.mobileqq.aio.msglist.holder.component.ark.AIOArkContentComponent"
        },
        "com.tencent.mobileqq.aio.msglist.holder.component.facebubble.AIOFaceBubbleContentComponent",
        "com.tencent.mobileqq.aio.msglist.holder.component.file.AIOFileContentComponent",
        "com.tencent.mobileqq.aio.msglist.holder.component.flashpic.AIOFlashPicContentComponent",
        "com.tencent.mobileqq.aio.msglist.holder.component.LocationShare.AIOLocationShareComponent",
        if (requireMinQQVersion(QQVersion.QQ_9_1_55)) {
            "com.tencent.mobileqq.aio.msglist.holder.component.markdown.AIORichContentComponent"
        } else {
            "com.tencent.mobileqq.aio.msglist.holder.component.markdown.AIOMarkdownContentComponent"
        },
        "com.tencent.mobileqq.aio.msglist.holder.component.marketface.AIOMarketFaceComponent",
        "com.tencent.mobileqq.aio.msglist.holder.component.mix.AIOMixContentComponent",
        "com.tencent.mobileqq.aio.msglist.holder.component.pic.AIOPicContentComponent",
        "com.tencent.mobileqq.aio.msglist.holder.component.poke.AIOPokeContentComponent",
        "com.tencent.mobileqq.aio.msglist.holder.component.ptt.AIOPttContentComponent",
        "com.tencent.mobileqq.aio.msglist.holder.component.reply.AIOReplyComponent",
        "com.tencent.mobileqq.aio.msglist.holder.component.text.AIOTextContentComponent",
        "com.tencent.mobileqq.aio.msglist.holder.component.video.AIOVideoContentComponent",
        "com.tencent.mobileqq.aio.qwallet.AIOQWalletComponent",
    )

    override fun onGetMenuNt(msg: Any, componentType: String, param: XC_MethodHook.MethodHookParam) {
        if (!isEnabled) return
        val item = CustomMenu.createItemIconNt(msg, "复读图片", R.drawable.ic_item_repeat_72dp, R.id.item_repeat_to_img) {
            val context = CommonContextWrapper.createAppCompatContext(ContextUtils.getCurrentActivity())
            val msgRecord = XposedHelpers.callMethod(msg, "getMsgRecord") as MsgRecord
            val msgView = msgViewHashMap[msgRecord.msgId]
            msgView?.let { sendBitmap(context, it, msgRecord) }
        }
        param.result = listOf(item) + param.result as List<*>
    }
}