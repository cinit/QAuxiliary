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

package me.hd.hook

import android.content.Context
import cc.ioctl.util.hookBeforeIfEnabled
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import io.github.qauxv.base.RuntimeErrorTracer
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.kernelcompat.ContactCompat
import io.github.qauxv.core.HookInstaller
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.dsl.uiClickableItem
import io.github.qauxv.hook.BaseFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import me.ketal.data.ConfigData
import me.ketal.util.ignoreResult
import xyz.nextalone.util.method
import java.io.Serializable

@FunctionHookEntry
@UiItemAgentEntry
object FakePicSize : BaseFunctionHook(
    hookKey = "hd_FakePicSize"
) {

    private val sizeMap = mapOf(
        "默认" to 0,
        "最小" to 1,
        "略小" to 64,
        "略大" to 256,
        "最大" to 512
    )
    private val sizeIndexKey = ConfigData<Int>("hd_FakePicSize_sizeIndex")
    private var sizeIndex: Int
        get() = sizeIndexKey.getOrDefault(0)
        set(value) {
            sizeIndexKey.value = value
        }

    override val uiItemAgent by lazy {
        uiClickableItem {
            title = "篡改图片比例"
            summary = "篡改发送的图片显示大小"
            onClickListener = { _, activity, _ ->
                showDialog(activity)
            }
        }
    }
    override val uiItemLocation = FunctionEntryRouter.Locations.Entertainment.ENTERTAIN_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)
    override val runtimeErrorDependentComponents: List<RuntimeErrorTracer>? = null

    private fun showDialog(activity: Context) {
        MaterialDialog(activity).show {
            title(text = "篡改图片比例")
            listItemsSingleChoice(
                items = sizeMap.keys.toList(),
                initialSelection = sizeIndex,
                waitForPositiveButton = true,
                selection = { _, index, _ ->
                    sizeIndex = index
                    if (sizeIndex != 0) {
                        isEnabled = true
                        if (!isInitialized) HookInstaller.initializeHookForeground(context, this@FakePicSize)
                    }
                }
            ).ignoreResult()
            positiveButton(text = "保存")
            negativeButton(text = "取消")
        }
    }

    override fun initOnce(): Boolean {
        if (sizeIndex == 0) return true
        val msgServiceClass = Initiator.loadClass("com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService\$CppProxy")
        hookBeforeIfEnabled(msgServiceClass.method("sendMsg")!!) { param ->
            val size = sizeMap.values.toList()[sizeIndex]
            val contact = ContactCompat.fromKernelObject(param.args[1] as Serializable)
            val elements = param.args[2] as ArrayList<*>
            for (element in elements) {
                val msgElement = (element as MsgElement)
                msgElement.picElement?.apply {
                    if (contact.chatType != 4) picSubType = 0
                    if (size == 1) {
                        picWidth = size
                        picHeight = size
                    } else {
                        val (oldWidth, oldHeight) = Pair(picWidth, picHeight)
                        val ratio = oldWidth.toDouble() / oldHeight.toDouble()
                        if (oldWidth > oldHeight) {
                            picWidth = size
                            picHeight = (size / ratio).toInt()
                        } else {
                            picWidth = (size * ratio).toInt()
                            picHeight = size
                        }
                    }
                }
            }
        }
        return true
    }
}