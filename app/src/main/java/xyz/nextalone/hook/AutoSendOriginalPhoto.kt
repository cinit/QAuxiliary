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
package xyz.nextalone.hook

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.HookUtils
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.tencent.mobileqq.widget.QUICheckBox
import de.robv.android.xposed.XposedBridge
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.clazz
import xyz.nextalone.util.findHostView
import xyz.nextalone.util.get
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.method
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object AutoSendOriginalPhoto : CommonSwitchFunctionHook(SyncUtils.PROC_MAIN or SyncUtils.PROC_PEAK) {

    override val name = "聊天自动发送原图"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

    @SuppressLint("ResourceType")
    override fun initOnce() = throwOrTrue {
        if (QAppUtils.isQQnt()) {   //截至2023.6.21，仍有一些项目在使用旧版组件（如频道），故保留其他hook
            //Lcom/tencent/mobileqq/e/a/ac;->f:Lcom/tencent/mobileqq/widget/QUICheckBox;    //普通模式半屏Panel的原图勾选框
            XposedBridge.hookAllConstructors("com.tencent.mobileqq.e.a.ac".clazz!!,HookUtils.afterIfEnabled(this) { param->
                param.thisObject.get("f", QUICheckBox::class.java)!!.isChecked = true
            })
            //新全屏相册活动
            "com.tencent.qqnt.qbasealbum.WinkHomeActivity".clazz!!.method("onCreate")!!.hookBefore {
                val ctx=it.thisObject as Activity
                ctx.intent.putExtra("key_is_quality_raw",true)
            }
        }
        val method = when {
            requireMinQQVersion(QQVersion.QQ_8_9_33) -> "d0"
            requireMinQQVersion(QQVersion.QQ_8_9_18) -> "c0"
            requireMinQQVersion(QQVersion.QQ_8_9_13) -> "d0"
            requireMinQQVersion(QQVersion.QQ_8_9_2) -> "e0"
            requireMinQQVersion(QQVersion.QQ_8_8_93) -> "Z"
            else -> "a"
        }
        "com.tencent.mobileqq.activity.aio.photo.PhotoListPanel".clazz?.method(method, Void.TYPE, Boolean::class.java)?.hookAfter(this) {
            val ctx = it.thisObject as View
            val sendOriginPhotoCheckbox = ctx.findHostView<CheckBox>("h1y")
            sendOriginPhotoCheckbox?.isChecked = true
        }
        if (requireMinQQVersion(QQVersion.QQ_8_2_0)) {
            val newPhotoOnCreate =
                "com.tencent.mobileqq.activity.photo.album.NewPhotoPreviewActivity".clazz?.method("doOnCreate", Boolean::class.java, Bundle::class.java)
                    ?: "com.tencent.mobileqq.activity.photo.album.NewPhotoPreviewActivity".clazz?.method("onCreate", Void.TYPE, Bundle::class.java)
            newPhotoOnCreate?.hookAfter(this) {
                val ctx = it.thisObject as Activity
                val checkBox = ctx.findHostView<CheckBox>("h1y")
                checkBox?.isChecked = true
            }
        }
    }
}
