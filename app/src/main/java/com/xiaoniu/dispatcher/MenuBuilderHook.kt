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

package com.xiaoniu.dispatcher

import cc.hicore.QApp.QAppUtils
import cc.hicore.hook.RepeaterPlus
import cc.hicore.hook.stickerPanel.Hooker.StickerPanelEntryHooker
import cc.ioctl.hook.msg.CopyCardMsg
import cc.ioctl.hook.msg.PicMd5Hook
import cc.ioctl.hook.msg.PttForwardHook
import cc.ioctl.util.HookUtils
import com.github.kyuubiran.ezxhelper.utils.isAbstract
import io.github.duzhaokun123.hook.MessageCopyHook
import io.github.duzhaokun123.hook.MessageTTSHook
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.hook.BasePersistBackgroundHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.xpcompat.XC_MethodHook
import me.hd.hook.menu.CopyMarkdown
import me.hd.hook.menu.EditTextContent
import me.hd.hook.menu.RecallMsgRecord
import me.hd.hook.menu.RepeatToImg
import me.ketal.hook.PicCopyToClipboard
import me.qcuncle.hook.TranslateTextMsg
import top.xunflash.hook.MiniAppDirectJump
import xyz.nextalone.util.hookAfterAllConstructors
import java.lang.reflect.Method

@FunctionHookEntry
object MenuBuilderHook : BasePersistBackgroundHook() {
    // These hooks are called when the menu is being built.
    private val decorators: Array<OnMenuBuilder> = arrayOf(
        RepeaterPlus.INSTANCE,
        StickerPanelEntryHooker.INSTANCE,
        PicMd5Hook.INSTANCE,
        PttForwardHook.INSTANCE,
        CopyCardMsg,
        MessageCopyHook,
        PicCopyToClipboard,
        MiniAppDirectJump,
        CopyMarkdown,
        MessageTTSHook,
        EditTextContent,
        TranslateTextMsg,
        RecallMsgRecord,
        RepeatToImg,
    )

    override fun initOnce(): Boolean {
        if (QAppUtils.isQQnt()) { // NT only
            val msgItemClass = Initiator.loadClass("com.tencent.mobileqq.aio.msg.AIOMsgItem")
            val baseComponentClass = Initiator.loadClass("com.tencent.mobileqq.aio.msglist.holder.component.BaseContentComponent")
            val getMsgMethod: Method = baseComponentClass.declaredMethods.first {
                it.returnType == msgItemClass && it.parameterTypes.isEmpty()
            }.apply { isAccessible = true }
            val listMethodName: String = baseComponentClass.declaredMethods.first {
                it.isAbstract && it.returnType == MutableList::class.java && it.parameterTypes.isEmpty()
            }.name
            val hookedClasses = mutableSetOf<Class<*>>()
            baseComponentClass.hookAfterAllConstructors {
                val componentClass = it.thisObject.javaClass
                if (componentClass in hookedClasses) return@hookAfterAllConstructors
                hookedClasses.add(componentClass)
                val target = componentClass.name
                HookUtils.hookAfterAlways(this, componentClass.getMethod(listMethodName), 48) { param ->
                    val msg = getMsgMethod.invoke(param.thisObject)!!
                    for (decorator in decorators) {
                        if (decorator.targetComponentTypes == null || target in decorator.targetComponentTypes!!) {
                            try {
                                decorator.onGetMenuNt(msg, target, it)
                            } catch (e: Exception) {
                                traceError(e)
                            }
                        }
                    }
                }
            }
        }
        return true
    }
}

interface OnMenuBuilder {
    /**
     * [com.tencent.mobileqq.aio.msglist.holder.component.BaseContentComponent] 的子类名
     * null 表示所有
     */
    val targetComponentTypes: Array<String>?

    /**
     * 同一消息可能按照父类多次回调
     */
    @Throws(Exception::class)
    fun onGetMenuNt(
        msg: Any,
        componentType: String,
        param: XC_MethodHook.MethodHookParam
    )
}


enum class ComponentType(private val desc: String, private val clazz: String) {
    TROOP_GIFT("群礼物", "com.tencent.mobileqq.aio.aiogift.AIOTroopGiftComponent"),
    ANI_STICKER_CONTENT("动态表情", "com.tencent.mobileqq.aio.msglist.holder.component.anisticker.AIOAniStickerContentComponent"),
    TEMPLATE_MSG("卡片模板", "com.tencent.mobileqq.aio.msglist.holder.component.template.AIOTemplateMsgComponent"),// high ver 9.1.55+
    ARK_CONTENT("卡片ARK", "com.tencent.mobileqq.aio.msglist.holder.component.ark.AIOArkContentComponent"),// old ver 9.1.55-
    CHAIN_ANI_STICKER_CONTENT("连续动态表情", "com.tencent.mobileqq.aio.msglist.holder.component.chain.ChainAniStickerContentComponent"),// 9.0.20+
    FACE_BUBBLE_CONTENT("表情泡泡", "com.tencent.mobileqq.aio.msglist.holder.component.facebubble.AIOFaceBubbleContentComponent"),
    FILE_CONTENT("文件消息", "com.tencent.mobileqq.aio.msglist.holder.component.file.AIOFileContentComponent"),
    ONLINE_FILE_CONTENT("在线文件", "com.tencent.mobileqq.aio.msglist.holder.component.file.AIOOnlineFileContentComponent"),
    FLASH_PIC_CONTENT("闪照消息", "com.tencent.mobileqq.aio.msglist.holder.component.flashpic.AIOFlashPicContentComponent"),
    FOLD_CONTENT("折叠消息", "com.tencent.mobileqq.aio.msglist.holder.component.fold.AIOFoldContentComponent"),
    COMMON_GRAY_TIPS("通用提示", "com.tencent.mobileqq.aio.msglist.holder.component.graptips.common.CommonGrayTipsComponent"),
    REVOKE_GRAY_TIPS("撤回提示", "com.tencent.mobileqq.aio.msglist.holder.component.graptips.revoke.RevokeGrayTipsComponent"),
    ICE_CREAK_CONTENT("打招呼", "com.tencent.mobileqq.aio.msglist.holder.component.ickbreak.AIOIceBreakContentComponent"),
    LOCATION_SHARE("位置", "com.tencent.mobileqq.aio.msglist.holder.component.LocationShare.AIOLocationShareComponent"),
    LONG_MSG_CONTENT("长消息", "com.tencent.mobileqq.aio.msglist.holder.component.longmsg.AIOLongMsgContentComponent"),
    RICH_CONTENT("富文本", "com.tencent.mobileqq.aio.msglist.holder.component.markdown.AIORichContentComponent"),// high ver 9.1.55+
    MARKDOWN_CONTENT("Markdown", "com.tencent.mobileqq.aio.msglist.holder.component.markdown.AIOMarkdownContentComponent"),// old ver 9.1.55-
    MARKET_FACE("商店表情", "com.tencent.mobileqq.aio.msglist.holder.component.marketface.AIOMarketFaceComponent"),
    MIX_CONTENT("图文", "com.tencent.mobileqq.aio.msglist.holder.component.mix.AIOMixContentComponent"),
    MULTI_FOWARD_CONTENT("合并转发", "com.tencent.mobileqq.aio.msglist.holder.component.multifoward.AIOMultifowardContentComponent"),
    MULTI_PIC_CONTENT("多图", "com.tencent.mobileqq.aio.msglist.holder.component.multipci.AIOMultiPicContentComponent"),// 9.0.80+
    PIC_CONTENT("图片", "com.tencent.mobileqq.aio.msglist.holder.component.pic.AIOPicContentComponent"),
    POKE_CONTENT("戳一戳", "com.tencent.mobileqq.aio.msglist.holder.component.poke.AIOPokeContentComponent"),
    PROLOGUE_CONTENT("开场白", "com.tencent.mobileqq.aio.msglist.holder.component.prologue.AIOPrologueContentComponent"),// 9.0.50+
    PTT_CONTENT("语音", "com.tencent.mobileqq.aio.msglist.holder.component.ptt.AIOPttContentComponent"),
    REPLY("回复", "com.tencent.mobileqq.aio.msglist.holder.component.reply.AIOReplyComponent"),
    TEXT_CONTENT("文本", "com.tencent.mobileqq.aio.msglist.holder.component.text.AIOTextContentComponent"),
    UN_SUPPORT_CONTENT("未支持", "com.tencent.mobileqq.aio.msglist.holder.component.text.AIOUnsuportContentComponent"),
    TOFU_CONTENT("名片", "com.tencent.mobileqq.aio.msglist.holder.component.tofu.AIOTofuContentComponent"),
    VIDEO_CONTENT("视频", "com.tencent.mobileqq.aio.msglist.holder.component.video.AIOVideoContentComponent"),
    VIDEO_RESULT_CONTENT("通话", "com.tencent.mobileqq.aio.msglist.holder.component.videochat.AIOVideoResultContentComponent"),
    Z_PLAN_CONTENT("QQ秀", "com.tencent.mobileqq.aio.msglist.holder.component.zplan.AIOZPlanContentComponent"),
    Q_WALLET("钱包", "com.tencent.mobileqq.aio.qwallet.AIOQWalletComponent"),
    SHOP_ARK_CONTENT("店铺", "com.tencent.mobileqq.aio.shop.AIOShopArkContentComponent"),
    BUSINESS_SAMPLE_CONTENT("示例", "com.tencent.qqnt.aio.sample.BusinessSampleContentComponent");

    companion object {
        val TYPE_ANI_STICKER = ANI_STICKER_CONTENT.clazz
        val TYPE_CARD by lazy {
            if (requireMinQQVersion(QQVersion.QQ_9_1_55)) {
                TEMPLATE_MSG.clazz
            } else {
                ARK_CONTENT.clazz
            }
        }
        val TYPE_FACE_BUBBLE = FACE_BUBBLE_CONTENT.clazz
        val TYPE_FILE = FILE_CONTENT.clazz
        val TYPE_FLASH_PIC= FLASH_PIC_CONTENT.clazz
        val TYPE_LOCATION = LOCATION_SHARE.clazz
        val TYPE_LONG_MSG = LONG_MSG_CONTENT.clazz
        val TYPE_RICH by lazy {
            if (requireMinQQVersion(QQVersion.QQ_9_1_55)) {
                RICH_CONTENT.clazz
            } else {
                MARKDOWN_CONTENT.clazz
            }
        }
        val TYPE_MARKET_FACE = MARKET_FACE.clazz
        val TYPE_MIX = MIX_CONTENT.clazz
        val TYPE_MULTI_FOWARD = MULTI_FOWARD_CONTENT.clazz
        val TYPE_PIC = PIC_CONTENT.clazz
        val TYPE_POKE = POKE_CONTENT.clazz
        val TYPE_PTT = PTT_CONTENT.clazz
        val TYPE_REPLY = REPLY.clazz
        val TYPE_TEXT = TEXT_CONTENT.clazz
        val TYPE_VIDEO = VIDEO_CONTENT.clazz
        val TYPE_WALLET = Q_WALLET.clazz
    }
}
