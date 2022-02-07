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
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import cc.ioctl.util.Reflex
import de.robv.android.xposed.XposedHelpers
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.ui.ResUtils
import io.github.qauxv.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import me.ketal.data.ConfigData
import me.ketal.ui.activity.ModifyLeftSwipeReplyActivity
import io.github.qauxv.tlb.ConfigTable.getConfig
import xyz.nextalone.util.hookAfter
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
        val intent = Intent(activity, ModifyLeftSwipeReplyActivity::class.java)
        activity.startActivity(intent)
    }

    private val LEFT_SWIPE_NO_ACTION = ConfigData<Boolean>("ketal_left_swipe_noAction")
    private val LEFT_SWIPE_MULTI_CHOOSE = ConfigData<Boolean>("ketal_left_swipe_multiChoose")
    private val LEFT_SWIPE_REPLY_DISTANCE = ConfigData<Int>("ketal_left_swipe_replyDistance")
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
    var replyDistance: Int
        get() = LEFT_SWIPE_REPLY_DISTANCE.getOrDefault(-1)
        set(replyDistance) {
            LEFT_SWIPE_REPLY_DISTANCE.value = replyDistance
        }
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
        var methodName = if (isTim()) "L" else "a"
        XposedHelpers.findMethodBestMatch(
            hookClass,
            methodName,
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
        methodName = if (isTim()) getConfig(LeftSwipeReplyHook::class.java.simpleName) else "a"
        Reflex.hasMethod(hookClass, methodName, Int::class.java)
            .hookAfter(this) {
                if (replyDistance <= 0) {
                    replyDistance = it.result as Int
                } else {
                    it.result = replyDistance
                }
            }
    }
}
