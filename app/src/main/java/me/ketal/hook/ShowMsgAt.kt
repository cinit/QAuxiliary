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

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.forEach
import cc.hicore.QApp.QAppUtils
import cc.ioctl.hook.profile.OpenProfileCard
import cc.ioctl.util.HostInfo
import cc.ioctl.util.ui.FaultyDialog
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import com.tencent.qqnt.kernel.nativeinterface.TextElement
import de.robv.android.xposed.XC_MethodHook
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.ntapi.ChatTypeConstants
import io.github.qauxv.bridge.ntapi.RelationNTUinAndUidApi
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.step.Step
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.DexDeobfsProvider
import io.github.qauxv.util.dexkit.DexFlow
import io.github.qauxv.util.dexkit.DexKitFinder
import io.github.qauxv.util.dexkit.DexMethodDescriptor
import io.github.qauxv.util.dexkit.HostMainDexHelper
import io.github.qauxv.util.hostInfo
import io.github.qauxv.util.isTim
import io.github.qauxv.util.requireMinQQVersion
import me.ketal.dispacher.BaseBubbleBuilderHook
import me.ketal.dispacher.OnBubbleBuilder
import me.singleneuron.data.MsgRecordData
import org.luckypray.dexkit.query.enums.StringMatchType
import xyz.nextalone.util.SystemServiceUtils
import xyz.nextalone.util.clazz
import xyz.nextalone.util.findHostView
import xyz.nextalone.util.get
import xyz.nextalone.util.invoke
import xyz.nextalone.util.method

@UiItemAgentEntry
@FunctionHookEntry
object ShowMsgAt : CommonSwitchFunctionHook(), OnBubbleBuilder, DexKitFinder {

    override val name = "消息显示At对象"
    override val description = "可能导致聊天界面滑动掉帧"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
    override val extraSearchKeywords: Array<String> = arrayOf("@", "艾特")

    private val mTextViewId: Int // 0 for unknown, -1 for not found
        get() {
            val cache = ConfigManager.getCache()
            val lastVersion = cache.getIntOrDefault("ShowMsgAt_ex1_id_version_code", 0)
            val id = cache.getIntOrDefault("ShowMsgAt_ex1_id_value", 0)
            return if (HostInfo.getVersionCode() == lastVersion) {
                id
            } else 0
        }

    override fun initOnce(): Boolean {
        if (isTim()) {
            return false
        }
        if (!BaseBubbleBuilderHook.initialize()) {
            return false
        }
        if (QAppUtils.isQQnt()) {
            return mTextViewId > 0
        }
        return true
    }

    override fun onGetView(
        rootView: ViewGroup,
        chatMessage: MsgRecordData,
        param: XC_MethodHook.MethodHookParam
    ) {
        if (!isEnabled || 1 != chatMessage.isTroop) return
        val textMsgType = "com.tencent.mobileqq.data.MessageForText".clazz!!
        val extStr = chatMessage.msgRecord.invoke(
            "getExtInfoFromExtStr",
            "troop_at_info_list", String::class.java
        ) ?: return
        if ("" == extStr) return
        val atList = (textMsgType.method("getTroopMemberInfoFromExtrJson")
            ?.invoke(null, extStr) ?: return) as List<*>
        when (val content = rootView.findHostView<View>("chat_item_content_layout")) {
            is TextView -> {
                copeAtInfo(content, atList)
            }

            is ViewGroup -> {
                content.forEach {
                    if (it is TextView)
                        copeAtInfo(it, atList)
                }
            }

            else -> {
                Log.d("暂不支持的控件类型--->$content")
                return
            }
        }
    }

    @JvmStatic
    public fun createUnknownUidDialog(outerContext: Context, uid: String) {
        val ctx = CommonContextWrapper.createAppCompatContext(outerContext)
        // BTN: OK, COPY
        AlertDialog.Builder(ctx)
            .setTitle("未知的 UID")
            .setMessage(uid + "\n如果您是第一次遇到此情况，建议您在右上角打开群资料卡后点开群聊成员列表，并等待其全部加载完成后返回重试。")
            .setPositiveButton("确认") { _, _ -> }
            .setNegativeButton("复制") { _, _ ->
                SystemServiceUtils.copyToClipboard(ctx, uid)
                Toasts.show(ctx, "已复制到剪贴板")
            }.show()
    }

    private fun createClickSpanForUid(uid: String?, troopUin: Long): ClickableSpan {
        return object : ClickableSpan() {
            override fun onClick(widget: View) {
                val ctx = widget.context
                if (uid?.startsWith("u_") == true) {
                    try {
                        val uin = RelationNTUinAndUidApi.getUinFromUid(uid)
                        if (uin.isNullOrEmpty()) {
                            createUnknownUidDialog(ctx, uid)
                            return
                        } else {
                            OpenProfileCard.openUserProfileCard(ctx, uin.toLong(), troopUin)
                        }
                    } catch (e: Exception) {
                        FaultyDialog.show(ctx, e)
                    }
                }
            }
        }
    }

    private fun setAtSpanBySearch(textView: TextView, atElements: List<TextElement>, troopUin: Long) {
        val text = textView.text
        val ssb = SpannableString(text)
        var searchIndexStart = 0
        for (at in atElements) {
            if (at.content.length >= 2) {
                val uid = at.atNtUid
                val start = text.indexOf(at.content, searchIndexStart)
                if (start == -1) continue
                val end = start + at.content.length
                searchIndexStart = end
                ssb.setSpan(createClickSpanForUid(uid, troopUin), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        textView.text = ssb
    }

    override fun onGetViewNt(rootView: ViewGroup, chatMessage: MsgRecord, param: XC_MethodHook.MethodHookParam) {
        if (!isEnabled) return
        if (chatMessage.chatType != ChatTypeConstants.GROUP) return
        // MIX 2, REPLY 9, STRUCT_LONG_MSG 12
        val msgType = chatMessage.msgType
        if (msgType != 2 && msgType != 9 && msgType != 12) return
        val elements = chatMessage.elements ?: return
        val atElements = mutableListOf<TextElement>()
        elements.forEach {
            if (it.textElement != null && it.textElement.atType != 0) {
                atElements.add(it.textElement)
            }
        }
        if (atElements.isEmpty()) {
            return
        }
        if (mTextViewId <= 0) {
            return
        }
        val tv = rootView.findViewById<TextView>(mTextViewId) ?: return
        // TODO 2023-07-19 更稳定查找TextView
        setAtSpanBySearch(tv, atElements, chatMessage.peerUin)
    }

    private fun copeAtInfo(textView: TextView, atList: List<*>) {
        val spannableString = SpannableString(textView.text)
        atList.forEach {
            val uin = it.get("uin") as Long
            val start = (it.get("startPos") as Short).toInt()
            val length = it.get("textLen") as Short
            if (start < spannableString.length) {
                if (spannableString[start] == '@') {
                    spannableString.setSpan(ProfileCardSpan(uin), start, start + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } else if (start == spannableString.length) {
                // workaround for host bug: start index is at the end of the string
                // there is a space at the end of the at string
                val possibleStart = start - length - 1
                if (possibleStart >= 0 && spannableString[possibleStart] == '@') {
                    spannableString.setSpan(ProfileCardSpan(uin), possibleStart, possibleStart + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } else {
                Log.e("艾特信息超出范围")
                Log.e("start:$start, length:$length")
                Log.e("text:'${textView.text}', length:${textView.text.length}")
            }
        }
        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    override val isNeedFind: Boolean
        get() {
            return QAppUtils.isQQnt() && mTextViewId == 0
        }

    override fun doFind(): Boolean {
        val fnSaveResult = { id: Int ->
            val hostVersion = hostInfo.versionCode32
            val cache = ConfigManager.getCache()
            cache.putInt("ShowMsgAt_ex1_id_version_code", hostVersion)
            cache.putInt("ShowMsgAt_ex1_id_value", id)
        }
        // step 1 find target class
        // "Lcom/tencent/mobileqq/aio/msglist/holder/component/text/util/TextContentViewUtil;"
        val result = DexDeobfsProvider.getCurrentBackend().use {
            it.getDexKitBridge().findClass {
                matcher {
                    if (requireMinQQVersion(QQVersion.QQ_9_0_60)) {
                        usingStrings("TextContentViewUtil")
                    } else {
                        addAnnotation {
                            type = "kotlin.Metadata"
                            addElement {
                                name = "d2"
                                value {
                                    arrayValue {
                                        addString(
                                            "Lcom/tencent/mobileqq/aio/msglist/holder/component/text/util/TextContentViewUtil;",
                                            StringMatchType.Equals
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        val klass: Class<*>
        if (result.size == 1) {
            klass = Initiator.loadClass(result[0].name)
        } else {
            val errMsg = "ShowMsgAt: cannot find class got ${result.size} results" + result.joinToString()
            fnSaveResult(-1)
            traceError(RuntimeException(errMsg))
            return false
        }
        // step 2 find specified method
        val method = klass.declaredMethods.single {
            val argt = it.parameterTypes
            argt.size == 3 && argt[0] == Context::class.java && argt[1] == Integer.TYPE
        }
        val methodDesc = DexMethodDescriptor(method)
        // step 3 load dex
        val dex = HostMainDexHelper.findDexWithClass(klass)
        if (dex == null) {
            Log.e("ShowMsgAt: cannot find dex: $klass")
            return false
        }
        // step 4 ???
        val idList = DexFlow.getViewSetIdP1Values(dex, methodDesc)
        if (idList.size == 1) {
            // good
            fnSaveResult(idList[0])
            return true
        } else {
            // ???
            val errMsg = "ShowMsgAt: cannot find id got ${idList.size} results" + idList.joinToString()
            traceError(RuntimeException(errMsg))
            fnSaveResult(-1)
            return false
        }
    }

    private val mStep: Step = object : Step {

        override fun step(): Boolean {
            return doFind()
        }

        override fun isDone(): Boolean {
            if (!QAppUtils.isQQnt()) {
                // no need this on non-NT
                return true
            }
            return mTextViewId != 0
        }

        override fun getPriority() = -99

        override fun getDescription() = "ShowMsgAt: find id"

    }

    private val mSteps by lazy {
        val steps = mutableListOf(mStep)
        super.makePreparationSteps()?.let {
            steps.addAll(it)
        }
        steps.toTypedArray()
    }

    override fun makePreparationSteps(): Array<Step> = mSteps

}

class ProfileCardSpan(val qq: Long) : ClickableSpan() {
    override fun onClick(v: View) {
        // 0 for @all
        if (qq > 10000) {
            OpenProfileCard.openUserProfileCard(v.context, qq)
        }
    }
}
