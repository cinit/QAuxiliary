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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import cc.ioctl.util.Reflex
import io.github.qauxv.activity.SettingsUiFragmentHostActivity
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.ui.ResUtils
import io.github.qauxv.util.DexKit
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.PlayQQVersion
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.requireMinVersion
import kotlinx.coroutines.flow.MutableStateFlow
import me.ketal.data.ConfigData
import me.ketal.ui.activity.ModifyLeftSwipeReplyFragment
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object LeftSwipeReplyHook : CommonConfigFunctionHook(
    "ketal_left_swipe_action",
    intArrayOf(DexKit.N_LeftSwipeReply_Helper__reply, DexKit.N_BASE_CHAT_PIE__chooseMsg)
) {

    override val name = "修改消息左滑动作"
    override val valueState: MutableStateFlow<String?>? = null
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, activity, _ ->
        val settingActivity = activity as SettingsUiFragmentHostActivity
        settingActivity.presentFragment(ModifyLeftSwipeReplyFragment())
    }

    private val LEFT_SWIPE_NO_ACTION = ConfigData<Boolean>("ketal_left_swipe_noAction")
    private val LEFT_SWIPE_MULTI_CHOOSE = ConfigData<Boolean>("ketal_left_swipe_multiChoose")
    var isNoAction: Boolean
        get() = LEFT_SWIPE_NO_ACTION.getOrDefault(false)
        set(on) {
            LEFT_SWIPE_NO_ACTION.value = on
        }
    var isMultiChose: Boolean
        get() = LEFT_SWIPE_MULTI_CHOOSE.getOrDefault(false)
        set(on) {
            LEFT_SWIPE_MULTI_CHOOSE.value = on
        }
    // todo: put png in module and use it here
    private var img: Bitmap? = null
    private val multiBitmap: Bitmap?
        get() {
            if (img == null || img!!.isRecycled) img =
                BitmapFactory.decodeStream(ResUtils.openAsset("list_checkbox_selected_nopress.png"))
            return img
        }

    override val isAvailable: Boolean
        get() = requireMinVersion(QQVersion.QQ_8_2_6, TIMVersion.TIM_3_1_1, PlayQQVersion.PlayQQ_8_2_9)

    override fun initOnce() = throwOrTrue {
        val replyMethod = DexKit.doFindMethod(DexKit.N_LeftSwipeReply_Helper__reply)
        val hookClass = replyMethod!!.declaringClass
        Reflex.findSingleMethod(
            hookClass,
            Void.TYPE,
            false,
            Float::class.java,
            Float::class.java
        ).hookBefore(this) {
            if (isNoAction) it.result = null
        }
        Reflex.findMethodByTypes_1(
            hookClass,
            Void.TYPE,
            View::class.java,
            Int::class.javaPrimitiveType
        ).hookBefore(this) {
            if (!isMultiChose) return@hookBefore
            val iv = it.args[0] as ImageView
            if (iv.tag == null) {
                iv.setImageBitmap(multiBitmap)
                iv.tag = true
            }
        }
        replyMethod.hookBefore(this) {
            if (!isMultiChose) return@hookBefore
            val message = Reflex.invokeVirtualAny(it.thisObject, Initiator._ChatMessage())
            val baseChatPie =
                Reflex.getFirstByType(it.thisObject, Initiator._BaseChatPie() as Class<*>)
            DexKit.doFindMethod(DexKit.N_BASE_CHAT_PIE__chooseMsg)!!.invoke(baseChatPie, message)
            it.result = null
        }
    }
}
