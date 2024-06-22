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
import de.robv.android.xposed.XC_MethodHook
import io.github.duzhaokun123.hook.MessageCopyHook
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.hook.BasePersistBackgroundHook
import io.github.qauxv.util.Initiator
import me.hd.hook.CopyMarkdown
import me.ketal.hook.PicCopyToClipboard
import top.xunflash.hook.MiniAppDirectJump
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
        CopyMarkdown
    )

    override fun initOnce(): Boolean {
        if (QAppUtils.isQQnt()) { // NT only
            val msgClass = Initiator.loadClass("com.tencent.mobileqq.aio.msg.AIOMsgItem")
            val baseContentComponentClass = Initiator.loadClass("com.tencent.mobileqq.aio.msglist.holder.component.BaseContentComponent")
            val getMsgMethod: Method = baseContentComponentClass.declaredMethods.first {
                it.returnType == msgClass && it.parameterTypes.isEmpty()
            }.apply { isAccessible = true }
            val listMethodName: String = baseContentComponentClass.declaredMethods.first {
                it.isAbstract && it.returnType == MutableList::class.java && it.parameterTypes.isEmpty()
            }.name
            val targets = mutableSetOf<String>()
            for (decorator in decorators) {
                targets.addAll(decorator.targetComponentTypes)
            }
            for (target in targets) {
                val targetClass = Initiator.loadClass(target)
                HookUtils.hookAfterAlways(this, targetClass.getMethod(listMethodName), 48) {
                    val msg = getMsgMethod.invoke(it.thisObject)!!
                    for (decorator in decorators) {
                        if (target in decorator.targetComponentTypes) {
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
    val targetComponentTypes: Array<String>

    @Throws(Exception::class)
    fun onGetMenuNt(
        msg: Any,
        componentType: String,
        param: XC_MethodHook.MethodHookParam
    )
}
