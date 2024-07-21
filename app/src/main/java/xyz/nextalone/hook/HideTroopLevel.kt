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

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.ntapi.ChatTypeConstants
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator._TroopMemberLevelView
import io.github.qauxv.util.isTim
import me.ketal.dispacher.BaseBubbleBuilderHook
import me.ketal.dispacher.OnBubbleBuilder
import me.ketal.util.findViewByType
import me.singleneuron.data.MsgRecordData
import xyz.nextalone.data.TroopInfo

@UiItemAgentEntry
object HideTroopLevel : CommonSwitchFunctionHook(), OnBubbleBuilder {

    override val name = "隐藏群聊群成员头衔"
    override val description = "可能导致聊天界面滑动掉帧"

    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_GROUP_TITLE

    private val levelClass
        get() = _TroopMemberLevelView()

    override val isAvailable = !isTim() && levelClass != null

    override fun initOnce(): Boolean {
        return isAvailable && BaseBubbleBuilderHook.initialize()
    }

    override fun onGetView(
        rootView: ViewGroup,
        chatMessage: MsgRecordData,
        param: XC_MethodHook.MethodHookParam
    ) {
        if (!isEnabled || 1 != chatMessage.isTroop) return
        if (levelClass == null) return
        val sendUin = chatMessage.senderUin
        val troopInfo = TroopInfo(chatMessage.friendUin)
        val ownerUin = troopInfo.troopOwnerUin
        val admin = troopInfo.troopAdmin
        val levelView = rootView.findViewByType(levelClass)
        val isAdmin = admin?.contains(sendUin) == true || ownerUin == sendUin
        levelView?.isVisible = isAdmin
    }

    override fun onGetViewNt(rootView: ViewGroup, chatMessage: MsgRecord, param: XC_MethodHook.MethodHookParam) {
        if (!isEnabled || chatMessage.chatType != ChatTypeConstants.GROUP) return
        if (levelClass == null) return
        val levelView = rootView.findViewByType(levelClass) ?: return
        val sendUin = chatMessage.senderUin.toString()
        val troopInfo = TroopInfo(chatMessage.peerUid)
        val ownerUin = troopInfo.troopOwnerUin
        val admin = troopInfo.troopAdmin
        val isAdmin = admin?.contains(sendUin) == true || ownerUin == sendUin
        //levelView.children.filter { it !is TextView }.forEach { it.visibility = android.view.View.GONE }
        // 如果forEach，则会隐藏昵称右侧的部分图标
        (levelView as LinearLayout).children.first().isVisible = isAdmin
    }
}
