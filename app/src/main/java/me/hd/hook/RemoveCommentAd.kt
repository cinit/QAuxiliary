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

import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.get
import xyz.nextalone.util.set

@FunctionHookEntry
@UiItemAgentEntry
object RemoveCommentAd : CommonSwitchFunctionHook() {

    override val name = "移除评论广告"
    override val description = "移除短视频评论列表中的二楼广告"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.MAIN_UI_MISC
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        val commentBlockClass = Initiator.loadClass("com.tencent.biz.qqcircle.adapter.QFSCommentBlock")
        val viewHolderClass = Initiator.loadClass("androidx.recyclerview.widget.RecyclerView\$ViewHolder")
        val onBindViewHolderMethod = commentBlockClass.getDeclaredMethod("onBindViewHolder", viewHolderClass, Int::class.java)
        hookBeforeIfEnabled(onBindViewHolderMethod) { param ->
            val adDataList = HashSet<Any>()
            val mDataList = param.thisObject.get("mDataList") as ArrayList<*>
            for (mDataItem in mDataList) {
                val comment = mDataItem.get("comment")
                val cmtBlockType = comment.get("cmt_block_type")
                val value = cmtBlockType.get("value")
                if (value == 1) {
                    adDataList.add(mDataItem)
                }
            }
            mDataList.removeAll(adDataList)
            param.thisObject.set("mDataList", mDataList)
        }
        return true
    }
}