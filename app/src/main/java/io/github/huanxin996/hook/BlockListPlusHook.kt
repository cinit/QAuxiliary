/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
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

package io.github.huanxin996.hook
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.qauxv.base.IEntityAgent
import io.github.qauxv.base.ISwitchCellAgent
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.requireMinQQVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import me.ketal.data.ConfigData
import xyz.nextalone.util.throwOrTrue
import cc.ioctl.util.hookAfterIfEnabled
import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.bridge.ntapi.RelationNTUinAndUidApi
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log

/**
 * 拉黑Plus：增强 QQ 自带黑名单的屏蔽范围
 */
@FunctionHookEntry
@UiItemAgentEntry
object BlockListPlusHook : CommonSwitchFunctionHook("block_list_plus_hook") {

    override val name = "拉黑Plus"

    override val description =
        "增强QQ黑名单：屏蔽被拉黑人的群消息、群内@、回复与表情回应，以及空间转发动态、@动态与评论"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.FRIEND_CATEGORY

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_9_3_30)

    override val targetProcesses: Int get() = SyncUtils.PROC_MAIN or SyncUtils.PROC_QZONE

    // region 子选项

    const val ITEM_GROUP_MESSAGE = "群消息"
    const val ITEM_AT_MESSAGE = "群内@消息"
    const val ITEM_REPLY_MESSAGE = "回复消息"
    const val ITEM_EMOJI_REPLY = "表情回应"
    const val ITEM_QZONE_FORWARD = "空间转发动态"
    const val ITEM_QZONE_AT = "空间@动态"
    const val ITEM_QZONE_COMMENT = "空间评论"

    @JvmField
    val ALL_ITEMS: Set<String> = setOf(
        ITEM_GROUP_MESSAGE,
        ITEM_AT_MESSAGE,
        ITEM_REPLY_MESSAGE,
        ITEM_EMOJI_REPLY,
        ITEM_QZONE_FORWARD,
        ITEM_QZONE_AT,
        ITEM_QZONE_COMMENT,
    )

    private const val ITEMS_CONFIG_KEY = "block_list_plus_hook.items"

    /** 子设置行的固定文案。 */
    private const val ITEMS_VALUE_TEXT = "此恨绵绵无绝期"

    private val mItemsConfig = ConfigData<Set<String>>(ITEMS_CONFIG_KEY)

    var activeItems: Set<String>
        get() = try {
            mItemsConfig.getOrDefault(ALL_ITEMS)?.toSet() ?: ALL_ITEMS
        } catch (e: ClassCastException) {
            mItemsConfig.remove()
            ALL_ITEMS
        }
        set(value) {
            mItemsConfig.value = value
            mValueState.update { ITEMS_VALUE_TEXT }
        }

    private val mValueState: MutableStateFlow<String?> =
        MutableStateFlow(ITEMS_VALUE_TEXT)

    // endregion

    // region UI

    override val uiItemAgent: IUiItemAgent by lazy {
        object : IUiItemAgent {
            override val titleProvider: (IEntityAgent) -> String = { _ -> name }
            override val summaryProvider: (IEntityAgent, Context) -> CharSequence? = { _, _ -> description }
            override val valueState: StateFlow<String?> = mValueState
            override val validator: ((IUiItemAgent) -> Boolean) = { _ -> true }
            override val switchProvider: ISwitchCellAgent? = object : ISwitchCellAgent {
                override val isCheckable = true
                override var isChecked: Boolean
                    get() = isEnabled
                    set(value) {
                        if (value != isEnabled) {
                            isEnabled = value
                        }
                    }
            }
            override val onClickListener: ((IUiItemAgent, Activity, View) -> Unit)? = { _, activity, view ->
                // 未启用时点击=启用功能：toggle switch 视图（视图刷新 + isChecked setter 写配置）；
                // 已启用时点击=打开子设置
                if (isEnabled) {
                    showItemsDialog(activity)
                } else {
                    toggleSwitchView(view)
                }
            }
            override val extraSearchKeywordProvider: ((IUiItemAgent, Context) -> Array<String>?)? = null
        }
    }

    private fun toggleSwitchView(view: View) {
        findSwitchView(view)?.isChecked = true
    }

    private fun findSwitchView(root: View): android.widget.CompoundButton? {
        if (root is android.widget.CompoundButton) {
            return root
        }
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val child = findSwitchView(root.getChildAt(i)) ?: continue
                return child
            }
        }
        return null
    }

    private fun showItemsDialog(activity: Activity) {
        val context = CommonContextWrapper.createMaterialDesignContext(activity)
        val cache = activeItems.toMutableSet()
        val builder = MaterialAlertDialogBuilder(context)
            .setTitle("选择要屏蔽的项目")
            .setMultiChoiceItems(
                ALL_ITEMS.toTypedArray(),
                ALL_ITEMS.map { it in cache }.toBooleanArray()
            ) { _: DialogInterface, index: Int, _: Boolean ->
                val item = ALL_ITEMS.elementAt(index)
                if (!cache.add(item)) {
                    cache.remove(item)
                }
            }
            .setNegativeButton("取消", null)
            .setPositiveButton("确定") { _: DialogInterface, _: Int ->
                activeItems = cache
                Toasts.show(context, "已保存：${cache.intersect(ALL_ITEMS).size}/${ALL_ITEMS.size} 项")
            }
        builder.show()
    }

    // endregion



    /** 已 hook 的方法集合 */
    val hookedMethods = java.util.concurrent.ConcurrentHashMap.newKeySet<java.lang.reflect.Method>()

    /** 已 hook 的查询回调类集合 */
    val hookedQueryCallbacks = java.util.concurrent.ConcurrentHashMap.newKeySet<Class<*>>()

    /** 已 hook 的最近联系人监听器类集合 */
    val hookedRecentListeners = java.util.concurrent.ConcurrentHashMap.newKeySet<Class<*>>()

    /** 已 hook 的最近联系人查询回调类集合 */
    val hookedRecentCallbacks = java.util.concurrent.ConcurrentHashMap.newKeySet<Class<*>>()

    /**
     * 被拦截消息的 msgSeq 记录
     */
    val blockedMsgSeqs =
        java.util.concurrent.ConcurrentHashMap<String, MutableSet<BlockedMsgSeq>>()

    /** 黑名单消息所在会话 */
    val blockedPeers = java.util.concurrent.ConcurrentHashMap<String, BlockedPeer>()

    /** 会话最近一条可见（非黑名单）消息，用于卡片摘要回退 */
    val lastVisible = java.util.concurrent.ConcurrentHashMap<String, VisibleMsg>()

    const val BLOCKED_MSG_TTL_MS = 10 * 60 * 1000L

    const val BLOCKED_PEER_TTL_MS = 60 * 1000L

    /** 被拦截消息的 msgSeq 记录项 */
    class BlockedMsgSeq(val msgSeq: Long, val expireAt: Long)

    /** 黑名单消息所在会话记录项 */
    class BlockedPeer(val peerUid: String, val expireAt: Long)

    /** 会话最近可见消息 */
    class VisibleMsg(val senderUin: Long, val msgTime: Long, val msgSeq: Long, val msgType: Int)

    /** 安全读取字段 */
    private fun Any?.gs(name: String): Any? {
        if (this == null) {
            return null
        }
        return readFieldSafe(this, name)
    }


    /** IKernelMsgListener 中携带消息列表的回调方法名 */
    private val MSG_CALLBACK_NAMES = arrayOf(
        "onRecvMsg",
        "onMsgInfoListAdd",
        "onMsgInfoListUpdate",
        "onFileMsgCome",
        "onRecvOnlineFileMsg",
    )

    /**
     * hook 内核消息监听器注册
     * （9.3.30 com.tencent.qqnt.kernel.api.impl.mk），在消息进入应用层前摘除黑名单消息
     */
    fun hookMsgListenerRegistration(): Boolean = throwOrTrue {
        
        val wrapperClass = Initiator.load("com.tencent.qqnt.kernel.api.impl.mk")
        if (wrapperClass != null) {
            hookListenerMsgCallbacks(wrapperClass)
            Log.i("BlockListPlus: wrapper class pre-hooked: ${wrapperClass.name}")
        }
        
        val cppProxy = Initiator.load("com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService\$CppProxy")
        val addKernelListener = cppProxy?.declaredMethods?.firstOrNull {
            it.name == "addKernelMsgListener" && it.parameterCount == 1
        }
        if (addKernelListener == null) {
            error("BlockListPlus: CppProxy.addKernelMsgListener 不可用（版本不兼容）")
        }
        Log.i("BlockListPlus: hook CppProxy.addKernelMsgListener")
        hookAfterIfEnabled(addKernelListener) { param ->
            val listener = param.args[0] ?: return@hookAfterIfEnabled
            hookListenerMsgCallbacks(listener.javaClass)
        }
    }

    /** hook 监听器类的消息回调：回调列表直接摘除黑名单消息。 */
    private fun hookListenerMsgCallbacks(listenerClass: Class<*>) {
        var hookedCount = 0
        for (msgCallbackName in MSG_CALLBACK_NAMES) {
            var c: Class<*>? = listenerClass
            while (c != null && c != Any::class.java) {
                for (m in c.declaredMethods) {
                    if (m.name == msgCallbackName && m.parameterCount == 1 &&
                        java.util.List::class.java.isAssignableFrom(m.parameterTypes[0]) &&
                        hookedMethods.add(m)
                    ) {
                        hookBeforeIfEnabled(m) { param ->
                            val list = param.args[0] as? MutableList<*> ?: return@hookBeforeIfEnabled
                            removeBlockedMsgs(list, param.thisObject?.javaClass?.name ?: "unknown")
                        }
                        hookedCount++
                    }
                }
                c = c.superclass
            }
        }
        if (hookedCount > 0) {
            Log.i("BlockListPlus: hooked $hookedCount msg callbacks in ${listenerClass.name}")
        }
    }

    /** hook 内核消息查询回调（AIO 历史/翻页加载）移除黑名单消息。 */
    fun hookMsgQueryCallbacks(): Boolean = throwOrTrue {
        for (clazzName in arrayOf(
            "com.tencent.qqnt.kernel.api.impl.MsgService",
            "com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService\$CppProxy"
        )) {
            val clazz = Initiator.load(clazzName) ?: continue
            var hookedCount = 0
            for (methodName in arrayOf(
                "getMsgs", "getMsgsExt", "getMsgsBySeqRange", "getMsgsBySeqList",
                "getMsgsByMsgId", "getMsgsBySeqAndCount", "getMsgsWithStatus",
                "getMsgsIncludeSelf", "getMsgsByTypeFilter", "getMsgsByTypeFilters",
                "getLatestDbMsgs", "getLastMessageList", "getLastLiveMsgs",
                "getAioFirstViewLatestMsgs", "getAioFirstViewLatestMsgsForAioPopup",
                "getMsgAbstract", "getMsgAbstracts", "getGuestMsgAbstracts"
            )) {
                val method = clazz.declaredMethods.firstOrNull { m ->
                    m.name == methodName && m.parameterCount >= 1
                } ?: continue
                hookAfterIfEnabled(method) { param ->
                    val callback = param.args[param.args.size - 1] ?: return@hookAfterIfEnabled
                    hookQueryCallbackOnResult(callback.javaClass)
                }
                hookedCount++
            }
            Log.i("BlockListPlus: query callbacks hooked in $clazzName, count=$hookedCount")
        }
    }

    private fun hookQueryCallbackOnResult(callbackClass: Class<*>) {
        if (!hookedQueryCallbacks.add(callbackClass)) {
            return
        }
        val onResult = findOnResultMethod(callbackClass)
        if (onResult == null) {
            hookedQueryCallbacks.remove(callbackClass)
            Log.e("BlockListPlus: onResult not found in ${callbackClass.name}")
            return
        }
        // 按参数类型定位消息列表参数：不同回调接口的 onResult 布局不同
        val listArgIndex = onResult.parameterTypes.indexOfFirst { p ->
            java.util.List::class.java.isAssignableFrom(p)
        }
        val msgsRspArgIndex = onResult.parameterTypes.indexOfFirst { p ->
            p.name == "com.tencent.qqnt.kernel.nativeinterface.MsgsRsp"
        }
        Log.i(
            "BlockListPlus: hook query onResult in ${callbackClass.name}, " +
                "listArg=$listArgIndex sig=${onResult.parameterTypes.joinToString(",") { it.simpleName }}"
        )
        hookBeforeIfEnabled(onResult) { param ->
            if (listArgIndex >= 0 && listArgIndex < param.args.size) {
                removeBlockedMsgs(param.args[listArgIndex], callbackClass.name)
            } else if (msgsRspArgIndex >= 0 && msgsRspArgIndex < param.args.size) {
                val rsp = param.args[msgsRspArgIndex] ?: return@hookBeforeIfEnabled
                removeBlockedMsgs(rsp.gs("msgList"), callbackClass.name)
            }
        }
    }

    /**
     * 直接在列表对象上移除黑名单消息
     */
    private fun removeBlockedMsgs(msgs: Any?, source: String) {
        val list = msgs as? MutableList<*> ?: return
        if (list.isEmpty()) {
            return
        }
        var removed = 0
        try {
            val it = list.iterator()
            while (it.hasNext()) {
                val msg = it.next()
                if (msg == null) {
                    continue
                }
                if (shouldBlockNtMsg(msg)) {
                    it.remove()
                    removed++
                    recordBlockedPeer(msg)
                    recordBlockedMsgSeq(msg)
                } else {
                    // 正常消息：更新该会话最后可见消息（用于卡片摘要回退）
                    recordVisibleMsg(msg)
                }
            }
        } catch (e: UnsupportedOperationException) {
            return
        } catch (e: Throwable) {
            Log.e("BlockListPlus: removeBlockedMsgs failed from $source", e)
        }
        if (removed > 0) {
            Log.i("BlockListPlus: removed $removed blocked msgs from $source")
        }
    }

    /** 记录黑名单消息的 msgSeq */
    private fun recordBlockedMsgSeq(msg: Any) {
        try {
            val peerUid = msg.gs("peerUid") as? String ?: return
            val msgSeq = (msg.gs("msgSeq") as? Number)?.toLong() ?: return
            val now = System.currentTimeMillis()
            blockedMsgSeqs.computeIfAbsent(peerUid) {
                java.util.concurrent.ConcurrentHashMap.newKeySet()
            }.add(BlockedMsgSeq(msgSeq, now + BLOCKED_MSG_TTL_MS))
        } catch (e: Throwable) {
            // 字段缺失跳过
        }
    }

    /** 指定会话的最后消息 seq 是否命中被拦截消息 */
    fun isBlockedMsgSeq(peerUid: String?, lastSeq: Long): Boolean {
        if (peerUid.isNullOrEmpty()) {
            return false
        }
        val set = blockedMsgSeqs[peerUid] ?: return false
        val now = System.currentTimeMillis()
        val it = set.iterator()
        var hit = false
        while (it.hasNext()) {
            val entry = it.next()
            if (now > entry.expireAt) {
                it.remove()
            } else if (entry.msgSeq == lastSeq) {
                hit = true
            }
        }
        return hit
    }

    /** 记录黑名单消息所在会话 */
    fun recordBlockedPeer(msg: Any) {
        val now = System.currentTimeMillis()
        val peerUid = msg.gs("peerUid") as? String
        if (!peerUid.isNullOrEmpty()) {
            blockedPeers[peerUid] =
                BlockedPeer(peerUid, now + BLOCKED_PEER_TTL_MS)
        }
        val peerUin = msg.gs("peerUin")
        when (peerUin) {
            is Number -> {
                val key = peerUin.toString()
                blockedPeers[key] =
                    BlockedPeer(key, now + BLOCKED_PEER_TTL_MS)
            }
            is String -> {
                if (peerUin.isNotBlank()) {
                    blockedPeers[peerUin] =
                        BlockedPeer(peerUin, now + BLOCKED_PEER_TTL_MS)
                }
            }
        }
    }

    /** 会话是否在黑名单消息会话集合中。 */
    fun isBlockedPeer(peerUid: String?): Boolean {
        if (peerUid.isNullOrEmpty()) {
            return false
        }
        val entry = blockedPeers[peerUid] ?: return false
        if (System.currentTimeMillis() > entry.expireAt) {
            blockedPeers.remove(peerUid)
            return false
        }
        return true
    }

    /** 记录会话最近可见消息（仅当消息时间更新时覆盖）。 */
    private fun recordVisibleMsg(msg: Any) {
        try {
            val peerUid = msg.gs("peerUid") as? String ?: return
            val msgTime = (msg.gs("msgTime") as? Number)?.toLong() ?: return
            val senderUin = (msg.gs("senderUin") as? Number)?.toLong() ?: return
            val msgSeq = (msg.gs("msgSeq") as? Number)?.toLong() ?: 0L
            val msgType = (msg.gs("msgType") as? Number)?.toInt() ?: 0
            val prev = lastVisible[peerUid]
            if (prev == null || msgTime >= prev.msgTime) {
                lastVisible[peerUid] =
                    VisibleMsg(senderUin, msgTime, msgSeq, msgType)
            }
        } catch (e: Throwable) {
            // 字段缺失跳过
        }
    }

    /**
     * 沿继承链查找携带消息列表的 onResult 方法
     */
    fun findOnResultMethod(clazz: Class<*>): java.lang.reflect.Method? {
        var c: Class<*>? = clazz
        while (c != null && c != Any::class.java) {
            var fallback: java.lang.reflect.Method? = null
            for (m in c.declaredMethods) {
                if (m.name != "onResult") {
                    continue
                }
                val hasList = m.parameterTypes.any { p ->
                    java.util.List::class.java.isAssignableFrom(p)
                }
                val hasMsgsRsp = m.parameterTypes.any { p ->
                    p.name == "com.tencent.qqnt.kernel.nativeinterface.MsgsRsp"
                }
                if (hasList || hasMsgsRsp) {
                    return m
                }
                if (fallback == null && (m.parameterCount == 1 || m.parameterCount == 3 || m.parameterCount == 4)) {
                    fallback = m
                }
            }
            if (fallback != null) {
                return fallback
            }
            c = c.superclass
        }
        return null
    }

    /** hook 内核通知回调（红点/通知栏数据源），摘除黑名单发送者的通知项 */
    fun hookMsgNotifyListener(): Boolean = throwOrTrue {
        val notifyListener = Initiator.load("com.tencent.qqnt.kernel.api.impl.MsgService\$b")
        if (notifyListener == null) {
            Log.e("BlockListPlus: MsgService\$b not found")
            return@throwOrTrue
        }
        val onMsgNotify = notifyListener.declaredMethods.firstOrNull { m ->
            m.name == "onMsgNotify" && m.parameterCount == 2
        }
        if (onMsgNotify == null) {
            Log.e("BlockListPlus: onMsgNotify not found in MsgService\$b")
            return@throwOrTrue
        }
        Log.i("BlockListPlus: hook onMsgNotify in MsgService\$b")
        hookBeforeIfEnabled(onMsgNotify) { param ->
            val list = param.args[0] as? MutableList<*> ?: return@hookBeforeIfEnabled
            if (list.isEmpty()) {
                return@hookBeforeIfEnabled
            }
            var removed = 0
            try {
                val it = list.iterator()
                while (it.hasNext()) {
                    val item = it.next() ?: continue
                    val tinyId = (item.gs("fromTinyId") as? Number)?.toLong() ?: continue
                    val uin = try {
                        RelationNTUinAndUidApi.getUinFromUid("u_$tinyId")
                    } catch (e: Throwable) {
                        continue
                    }
                    if (BlockListManager.isBlocked(uin)) {
                        it.remove()
                        removed++
                    }
                }
            } catch (e: UnsupportedOperationException) {
                if (hasBlockedNotifyItem(list)) {
                    Log.i("BlockListPlus: BLOCK onMsgNotify (immutable list, blocked sender)")
                    param.result = null
                }
                return@hookBeforeIfEnabled
            }
            if (removed > 0) {
                Log.i("BlockListPlus: removed $removed notify items")
            }
        }
    }

    /** 通知项列表是否包含黑名单发送者（MsgNotifyItem.fromTinyId） */
    private fun hasBlockedNotifyItem(list: List<*>): Boolean {
        for (item in list) {
            if (item == null) {
                continue
            }
            val tinyId = (item.gs("fromTinyId") as? Number)?.toLong() ?: continue
            val uin = try {
                RelationNTUinAndUidApi.getUinFromUid("u_$tinyId")
            } catch (e: Throwable) {
                continue
            }
            if (BlockListManager.isBlocked(uin)) {
                return true
            }
        }
        return false
    }

    // region 消息判断

    /**
     * NT 消息是否命中黑名单：发送者黑名单、群内@、回复、表情回应。
     * 逐项受子选项控制。
     */
    private fun shouldBlockNtMsg(msg: Any): Boolean {
        val items = BlockListPlusHook.activeItems
        val senderUin = (msg.gs("senderUin") as? Number)?.toLong()
        if (senderUin != null && senderUin > 0L && BlockListManager.isBlocked(senderUin)) {
            return true
        }
        val msgType = (msg.gs("msgType") as? Number)?.toInt() ?: 0
        if (msgType == NT_GRAY_TIP_TYPE && BlockListPlusHook.ITEM_EMOJI_REPLY in items) {
            if (isEmoReplyFromBlocked(msg, msgType, senderUin)) {
                return true
            }
        }
        if (BlockListPlusHook.ITEM_AT_MESSAGE in items && hasBlockedAtNt(msg)) {
            return true
        }
        return BlockListPlusHook.ITEM_REPLY_MESSAGE in items && isReplyToBlockedNt(msg)
    }

    /**
     * 表情回应通知（grayTip subType=12）是否来自黑名单用户。
     * uin 来源：xmlElement.templParam.mqq_uin / content `<qq jp="uin"/>`，兼容 emojiReplyElement.tinyId。
     */
    private fun isEmoReplyFromBlocked(msg: Any, msgType: Int, senderUin: Long?): Boolean {
        val elements = msg.gs("elements") as? List<*> ?: return false
        for (element in elements) {
            if (element == null) {
                continue
            }
            val grayTipElement = element.gs("grayTipElement") ?: continue
            val subType = (grayTipElement.gs("subType") as? Number)?.toInt() ?: continue
            if (subType != 12) {
                continue
            }
            // 数据在 xmlElement：templParam.mqq_uin + content 中 <qq jp="uin"/>
            val xmlElement = grayTipElement.gs("xmlElement") ?: continue
            val templParam = xmlElement.gs("templParam")
            val mqqUin = templParam?.gs("mqq_uin")
            val uinFromParam = when (mqqUin) {
                is Number -> mqqUin.toLong()
                is String -> mqqUin.toLongOrNull()
                else -> null
            }
            if (uinFromParam != null && uinFromParam > 0L && BlockListManager.isBlocked(uinFromParam)) {
                Log.i("BlockListPlus: blocked emo-reply via templParam ${maskUin(uinFromParam)}")
                return true
            }
            val content = xmlElement.gs("content") as? String
            if (content != null) {
                val m = EMOTION_REPLY_UIN_REGEX.find(content)
                val uinFromXml = m?.groupValues?.getOrNull(1)?.toLongOrNull()
                if (uinFromXml != null && uinFromXml > 0L && BlockListManager.isBlocked(uinFromXml)) {
                    Log.i("BlockListPlus: blocked emo-reply via xml ${maskUin(uinFromXml)}")
                    return true
                }
            }
            // 兼容 emojiReplyElement.tinyId（与 uin 同源）
            val emojiReplyElement = element.gs("emojiReplyElement")
            val tinyId = emojiReplyElement?.gs("tinyId")
            val uinFromTiny = when (tinyId) {
                is Number -> tinyId.toLong()
                is String -> tinyId.toLongOrNull()
                else -> null
            }
            if (uinFromTiny != null && uinFromTiny > 0L && BlockListManager.isBlocked(uinFromTiny)) {
                Log.i("BlockListPlus: blocked emo-reply via tinyId ${maskUin(uinFromTiny)}")
                return true
            }
            // 兜底：senderUin 为表情回应操作者
            if (senderUin != null && senderUin > 0L && BlockListManager.isBlocked(senderUin)) {
                return true
            }
        }
        return false
    }

    /** 表情回应通知中 uin 提取正则（content 内 `<qq jp="uin"/>`）。 */
    private val EMOTION_REPLY_UIN_REGEX = Regex("jp=\"(\\d+)\"")

    /** 消息是否 @ 了黑名单用户（textElement.atUidArr）。 */
    private fun hasBlockedAtNt(msg: Any): Boolean {
        val elements = msg.gs("elements") as? List<*> ?: return false
        for (element in elements) {
            if (element == null) {
                continue
            }
            val textElement = element.gs("textElement") ?: continue
            val atUidArr = textElement.gs("atUidArr") as? List<*> ?: continue
            for (atUid in atUidArr) {
                if (atUid == null) {
                    continue
                }
                val uin = when (atUid) {
                    is Number -> atUid.toLong()
                    is String -> atUid.toLongOrNull()
                    else -> null
                } ?: continue
                if (BlockListManager.isBlocked(uin)) {
                    Log.i("BlockListPlus: blocked NT at-msg")
                    return true
                }
            }
        }
        return false
    }

    /** 消息是否回复了黑名单用户（replyElement.senderUidStr）。 */
    private fun isReplyToBlockedNt(msg: Any): Boolean {
        val elements = msg.gs("elements") as? List<*> ?: return false
        for (element in elements) {
            if (element == null) {
                continue
            }
            val replyElement = element.gs("replyElement") ?: continue
            val senderUidStr = replyElement.gs("senderUidStr") as? String ?: continue
            val uin = try {
                RelationNTUinAndUidApi.getUinFromUid(senderUidStr)
            } catch (e: Throwable) {
                continue
            }
            if (BlockListManager.isBlocked(uin)) {
                return true
            }
        }
        return false
    }

    /** NT 灰字消息类型常量（表情回应通知等）。 */
    private const val NT_GRAY_TIP_TYPE = 5

    // endregion



    /** hook 最近联系人查询（主页面会话列表数据源），黑名单会话未读清零/移除。 */
    fun hookRecentContactList(): Boolean = throwOrTrue {
        for (clazzName in arrayOf(
            "com.tencent.qqnt.kernel.nativeinterface.IKernelRecentContactService\$CppProxy",
            "com.tencent.qqnt.kernel.api.impl.RecentContactService"
        )) {
            val clazz = Initiator.load(clazzName) ?: continue
            // 主界面会话列表可能用 getRecentContactList / getRecentContactInfos / SnapShot 任一查询
            for (queryName in arrayOf(
                "getRecentContactList",
                "getRecentContactInfos",
                "getRecentContactListSnapShot"
            )) {
                val query = clazz.declaredMethods.firstOrNull { m ->
                    m.name == queryName && m.parameterCount >= 1
                } ?: continue
                Log.i("BlockListPlus: hook $queryName in ${clazz.name}")
                hookAfterIfEnabled(query) { param ->
                    val callback = param.args[param.args.size - 1] ?: return@hookAfterIfEnabled
                    hookRecentContactCallback(callback.javaClass)
                }
            }
            // 同步查询：getRecentContactListSync 直接返回 CompleteRecentContactInfo，after 过滤返回值
            for (syncName in arrayOf("getRecentContactListSync", "getRecentContactListSyncLimit")) {
                val sync = clazz.declaredMethods.firstOrNull { m ->
                    m.name == syncName && m.returnType.name == "com.tencent.qqnt.kernel.nativeinterface.CompleteRecentContactInfo"
                } ?: continue
                Log.i("BlockListPlus: hook $syncName (sync) in ${clazz.name}")
                hookAfterIfEnabled(sync) { param ->
                    val result = param.result ?: return@hookAfterIfEnabled
                    val sorted = readFieldSafe(result, "sortedContactList") as? MutableList<*>
                    if (sorted != null) {
                        removeBlockedRecentInfos(sorted)
                    }
                    val changed = readFieldSafe(result, "changedList") as? MutableList<*>
                    if (changed != null) {
                        removeBlockedRecentInfos(changed)
                    }
                }
            }
        }
    }

    /** hook 最近联系人监听器注册，与查询过滤互补 */
    fun hookRecentContactListenerRegistration(): Boolean = throwOrTrue {

        val cppProxy = Initiator.load("com.tencent.qqnt.kernel.nativeinterface.IKernelRecentContactService\$CppProxy")
        if (cppProxy != null) {
            val addListener = cppProxy.declaredMethods.firstOrNull {
                it.name == "addKernelRecentContactListener" && it.parameterCount == 1
            }
            if (addListener != null) {
                Log.i("BlockListPlus: hook CppProxy.addKernelRecentContactListener")
                hookAfterIfEnabled(addListener) { param ->
                    val listener = param.args[0] ?: return@hookAfterIfEnabled
                    hookRecentContactListenerCallbacks(listener.javaClass)
                }
            }
        }
        
        val serviceClass = Initiator.load("com.tencent.qqnt.kernel.api.impl.RecentContactService")
        if (serviceClass != null) {
            val addDefault = serviceClass.declaredMethods.firstOrNull {
                it.name == "addDefaultListener" && it.parameterCount == 0
            }
            if (addDefault != null) {
                Log.i("BlockListPlus: hook RecentContactService.addDefaultListener")
                hookAfterIfEnabled(addDefault) { param ->
                    hookRecentContactListenerCallbacks(serviceClass)
                }
            }
        }
    }

    /** hook 监听器变更回调：会话进入 UI 前移除黑名单会话（onRecentContactListChanged 系列/未读更新）。 */
    private fun hookRecentContactListenerCallbacks(listenerClass: Class<*>) {
        if (!hookedRecentListeners.add(listenerClass)) {
            return
        }
        var hookedCount = 0
        for (callbackName in arrayOf(
            "onRecentContactListChanged",
            "onRecentContactListChangedVer2",
            "onRecentContactNotification",
            "onMsgUnreadCountUpdate"
        )) {
            var c: Class<*>? = listenerClass
            while (c != null && c != Any::class.java) {
                for (m in c.declaredMethods) {
                    if (m.name == callbackName && m.parameterCount >= 1 &&
                        java.util.List::class.java.isAssignableFrom(m.parameterTypes[0])
                    ) {
                        hookBeforeIfEnabled(m) { param ->
                            val list = param.args[0] as? MutableList<*> ?: return@hookBeforeIfEnabled
                            removeBlockedRecentContacts(list)
                        }
                        hookedCount++
                    } else if (m.name == "onMsgUnreadCountUpdate" && m.parameterCount == 1 &&
                        java.util.Map::class.java.isAssignableFrom(m.parameterTypes[0])
                    ) {
                        hookBeforeIfEnabled(m) { param ->
                            clearBlockedUnread(param.args[0])
                        }
                        hookedCount++
                    }
                }
                c = c.superclass
            }
        }
        if (hookedCount > 0) {
            Log.i("BlockListPlus: hooked $hookedCount recent listener callbacks in ${listenerClass.name}")
        }
    }

    /**
     * 从会话变更列表中移除黑名单会话
     */
    private fun removeBlockedRecentContacts(list: MutableList<*>) {
        if (list.isEmpty()) {
            return
        }
        val it = list.iterator()
        while (it.hasNext()) {
            val item = it.next() ?: continue
            try {
                val changedList = readFieldSafe(item, "changedList") as? MutableList<*>
                val sortedList = readFieldSafe(item, "sortedContactList") as? MutableList<*>
                val removedChanged = changedList?.let { removeBlockedRecentInfos(it) } ?: false
                val removedSorted = sortedList?.let { removeBlockedRecentInfos(it) } ?: false
                if (removedChanged || removedSorted) {
                    // 命中黑名单：清零未读
                    setFieldSafe(item, "unreadCnt", 0L)
                    // 两个列表都为空才移除外层项，否则保留
                    if ((changedList == null || changedList.isEmpty()) &&
                        (sortedList == null || sortedList.isEmpty())
                    ) {
                        it.remove()
                    }
                }
            } catch (e: UnsupportedOperationException) {
                return
            }
        }
    }

    /**
     * 从会话列表移除黑名单会话项
     */
    private fun removeBlockedRecentInfos(infos: MutableList<*>): Boolean {
        var removed = false
        try {
            val it = infos.iterator()
            while (it.hasNext()) {
                val item = it.next() ?: continue
                val senderUin = getRecentSenderUin(item)
                val peerUid = readFieldSafe(item, "peerUid") as? String
                val lastMsg = readFieldSafe(item, "lastMsgInfo")
                val lastSeq = lastMsg?.let { (readFieldSafe(it, "msgSeq") as? Number)?.toLong() }
                val hitByBlockedSeq = lastSeq != null && isBlockedMsgSeq(peerUid, lastSeq)
                val hit = if (senderUin != null && senderUin > 0L) {
                    BlockListManager.isBlocked(senderUin) || hitByBlockedSeq
                } else {
                    isBlockedPeer(peerUid) || hitByBlockedSeq
                }
                if (hit) {
                    val visible = peerUid?.let { lastVisible[it] }
                    if (visible != null) {
                        rollbackLastMsgInfo(item, visible)
                    } else {
                        it.remove()
                        removed = true
                    }
                }
            }
        } catch (e: UnsupportedOperationException) {
            return removed
        }
        return removed
    }

    /** 回退会话最后消息为最近可见（非黑名单）消息，卡片摘要显示上一条可见消息 */
    private fun rollbackLastMsgInfo(item: Any, visible: VisibleMsg) {
        val lastMsg = readFieldSafe(item, "lastMsgInfo") ?: return
        setFieldSafe(lastMsg, "senderUin", visible.senderUin)
        setFieldSafe(lastMsg, "msgTime", visible.msgTime)
        setFieldSafe(lastMsg, "msgSeq", visible.msgSeq)
        setFieldSafe(lastMsg, "msgType", visible.msgType)
        // 未读清零（黑名单消息不应计入未读）
        setFieldSafe(item, "unreadCnt", 0L)
    }

    /** 解析会话项最后消息发送者 uin：优先 lastMsgInfo.senderUin */
    private fun getRecentSenderUin(item: Any): Long? {
        val lastMsg = readFieldSafe(item, "lastMsgInfo")
        if (lastMsg != null) {
            when (val sender = readFieldSafe(lastMsg, "senderUin")) {
                is Number -> return sender.toLong()
                is String -> return sender.toLongOrNull()
            }
        }
        return when (val sender = readFieldSafe(item, "senderUin")) {
            is Number -> sender.toLong()
            is String -> sender.toLongOrNull()
            else -> null
        }
    }

    /**
     * 处理内核推送的未读计数更新（onMsgUnreadCountUpdate）
     * HashMap 的 key 为会话标识（peerUin/peerUid），黑名单消息所在会话的未读清零
     */
    private fun clearBlockedUnread(map: Any?) {
        val unreadMap = map as? MutableMap<*, *> ?: return
        if (unreadMap.isEmpty()) {
            return
        }
        val it = unreadMap.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            val key = entry.key
            // key 可能是 peerUin（数字字符串）或 peerUid（u_ 开头）或 Long
            val keyStr = key?.toString()
            val isBlockedSession = if (keyStr != null && keyStr.startsWith("u_")) {
                isBlockedPeer(keyStr)
            } else {
                val uin: Long? = when (key) {
                    is Number -> (key as Number).toLong()
                    is String -> key.toLongOrNull()
                    else -> null
                }
                uin != null && (BlockListManager.isBlocked(uin) ||
                    blockedPeers.keys.any { it.endsWith(uin.toString()) })
            }
            if (isBlockedSession) {
                Log.i("BlockListPlus: cleared unread entry $keyStr")
                it.remove()
            }
        }
    }

    /**
     * hook 最近联系人查询回调 onResult
     */
    private fun hookRecentContactCallback(callbackClass: Class<*>) {
        if (!hookedRecentCallbacks.add(callbackClass)) {
            return
        }
        val onResult = findOnResultMethod(callbackClass)
        if (onResult == null) {
            hookedRecentCallbacks.remove(callbackClass)
            Log.e("BlockListPlus: recent onResult not found in ${callbackClass.name}")
            return
        }
        Log.i("BlockListPlus: hook recent onResult in ${callbackClass.name}")
        hookBeforeIfEnabled(onResult) { param ->
            for (arg in param.args) {
                if (arg != null &&
                    arg.javaClass.name == "com.tencent.qqnt.kernel.nativeinterface.CompleteRecentContactInfo"
                ) {
                    val sorted = readFieldSafe(arg, "sortedContactList") as? MutableList<*>
                    if (sorted != null) {
                        removeBlockedRecentInfos(sorted)
                    }
                    val changed = readFieldSafe(arg, "changedList") as? MutableList<*>
                    if (changed != null) {
                        removeBlockedRecentInfos(changed)
                    }
                }
            }
            val list = param.args.firstOrNull { it is MutableList<*> } as? MutableList<*> ?: return@hookBeforeIfEnabled
            var removedCount = 0
            val it = list.iterator()
            while (it.hasNext()) {
                val item = it.next() ?: continue
                try {
                    val senderUin = getRecentSenderUin(item) ?: continue
                    if (BlockListManager.isBlocked(senderUin)) {
                        it.remove()
                        removedCount++
                    } else {
                        clearRecentUnread(item)
                    }
                } catch (e: Throwable) {
                    // 单条解析失败跳过
                }
            }
            if (removedCount > 0) {
                Log.i("BlockListPlus: recent onResult removed $removedCount from ${callbackClass.name}")
            }
        }
    }

    /** 清零会话可能的残留未读字段（unreadCnt/unreadChatCnt） */
    private fun clearRecentUnread(item: Any) {
        for (fieldName in arrayOf("unreadCnt", "unreadChatCnt")) {
            setFieldSafe(item, fieldName, 0L)
        }
    }

    // region 空间

    /** 空间数据源拦截入口 */
    private fun hookQZoneDataSources() {
        hookGetDataList()
        hookDataSetters()
        hookFeedProDataSource()
    }

    /** getDataList 过滤结果缓存 */
    private val mFilteredListCache = java.util.WeakHashMap<Any, Pair<List<*>, Int>>()

    /** hook BaseListViewAdapter.getDataList()：过滤黑名单并修改原列表（数据层移除，getItem/getItemCount 一致）。 */
    private fun hookGetDataList() {
        val adapterClass = try {
            Initiator.load("com.tencent.biz.richframework.part.block.base.BaseListViewAdapter")
        } catch (e: Throwable) {
            return
        } ?: return
        var hooked = 0
        for (m in adapterClass.declaredMethods) {
            if (m.name == "getDataList" && m.parameterCount == 0 &&
                m.returnType == java.util.List::class.java
            ) {
                hookAfterIfEnabled(m) { param ->
                    val list = param.result as? MutableList<*> ?: return@hookAfterIfEnabled
                    if (list.isNotEmpty()) {
                        // 同对象且 size 未变（滚动/复用）：已过滤，直接返回
                        val cached = synchronized(mFilteredListCache) { mFilteredListCache[list] }
                        if (cached != null && cached.second == list.size) {
                            return@hookAfterIfEnabled
                        }
                        filterDataList(list)
                        synchronized(mFilteredListCache) {
                            mFilteredListCache[list] = Pair(list, list.size)
                        }
                    }
                }
                hooked++
            }
        }
        if (hooked == 0) {
            error("BaseListViewAdapter.getDataList not found")
        }
    }

    /** hook BaseListViewAdapter 数据设置方法（setDatas/setDatasDefault/submitList/addData）过滤黑名单。 */
    private fun hookDataSetters() {
        val adapterClass = try {
            Initiator.load("com.tencent.biz.richframework.part.block.base.BaseListViewAdapter")
        } catch (e: Throwable) {
            return
        } ?: return
        var hooked = 0
        for (m in adapterClass.declaredMethods) {
            if (m.parameterCount >= 1 && m.parameterTypes[0] == java.util.List::class.java) {
                if (m.name == "setDatas" || m.name == "setDatasDefault" ||
                    m.name == "submitList" || m.name == "addData"
                ) {
                    hookBeforeIfEnabled(m) { param ->
                        val list = param.args[0] as? List<*>
                        if (list != null && list.isNotEmpty()) {
                            val filtered = list.filter { it != null && !shouldBlockData(it) }
                            if (filtered.size != list.size) {
                                Log.i(
                                    "BlockListPlus: qzone setter ${m.name} " +
                                        "filtered ${list.size - filtered.size}"
                                )
                                param.args[0] = filtered
                            }
                        }
                    }
                    hooked++
                }
            }
        }
        if (hooked == 0) {
            error("BaseListViewAdapter data setters not found")
        }
    }

    /** hook feedpro viewmodel 数据设置（e2/f2 的 q11.eb.d 列表、c2 的 List 参数）过滤黑名单。 */
    private fun hookFeedProDataSource() {
        val vmClass = try {
            Initiator.load("com.qzone.reborn.feedpro.viewmodel.v")
        } catch (e: Throwable) {
            return
        } ?: return
        var hooked = 0
        for (m in vmClass.declaredMethods) {
            val params = m.parameterTypes
            // 静态方法：首参为 v 实例，数据在 args[1]
            if ((m.name == "e2" || m.name == "f2") && params.size == 2 && params[1].name == "q11.eb") {
                hookBeforeIfEnabled(m) { param ->
                    filterEbList(param.args[1])
                }
                hooked++
            } else if (m.name == "c2" && params.size == 3 && params[1] == java.util.List::class.java) {
                hookBeforeIfEnabled(m) { param ->
                    val list = param.args[1] as? List<*>
                    if (list != null && list.isNotEmpty()) {
                        val filtered = list.filter { it != null && !shouldBlockPbData(it) }
                        if (filtered.size != list.size) {
                            Log.i(
                                "BlockListPlus: qzone c2 filtered ${list.size - filtered.size}"
                            )
                            param.args[1] = filtered
                        }
                    }
                }
                hooked++
            }
        }
        if (hooked == 0) {
            error("feedpro viewmodel data source not found")
        }
    }

    /** 过滤 q11.eb 的 d 字段（动态列表），移除黑名单动态。 */
    private fun filterEbList(eb: Any?) {
        try {
            if (eb == null) {
                return
            }
            val dField = eb.javaClass.declaredFields.firstOrNull { it.name == "d" } ?: return
            dField.isAccessible = true
            val list = dField.get(eb) as? List<*> ?: return
            if (list.isEmpty()) {
                return
            }
            val filtered = list.filter { it != null && !shouldBlockPbData(it) }
            if (filtered.size != list.size) {
                Log.i(
                    "BlockListPlus: qzone data-source filtered ${list.size - filtered.size}"
                )
                dField.set(eb, filtered)
            }
        } catch (e: Throwable) {
            // 数据源结构变化时放行
        }
    }

    // endregion

    override fun initOnce(): Boolean = throwOrTrue {
        BlockListManager.prepare()
        hookMsgListenerRegistration()
        hookMsgQueryCallbacks()
        hookMsgNotifyListener()
        hookRecentContactList()
        hookRecentContactListenerRegistration()
        hookQZoneDataSources()
        hookAioFeedSources()
    }

    /** AIO 动态页/详情页数据源拦截：feedx 评论 Block（base.l.d0）与 QZoneFeedService 动态列表过滤。 */
    private fun hookAioFeedSources() {
        // feedx 评论 Block（extends base.l）：d0() 返回数据列表，过滤黑名单动态/评论
        val baseLClass = try {
            Initiator.load("com.qzone.reborn.base.l")
        } catch (e: Throwable) {
            return
        } ?: return
        val d0 = baseLClass.declaredMethods.firstOrNull {
            it.name == "d0" && it.parameterCount == 0 && it.returnType == java.util.List::class.java
        }
        if (d0 != null) {
            hookAfterIfEnabled(d0) { param ->
                val list = param.result as? MutableList<*>
                if (list != null && list.isNotEmpty()) {
                    filterDataList(list)
                }
            }
        }
        // QZoneFeedService 动态列表（AIO 动态页）：List 参数过滤
        val serviceClass = try {
            Initiator.load("com.qzone.feed.business.service.QZoneFeedService")
        } catch (e: Throwable) {
            return
        } ?: return
        for (m in serviceClass.declaredMethods) {
            if (m.parameterCount >= 1 && m.parameterTypes.any {
                    java.util.List::class.java.isAssignableFrom(it)
                }
            ) {
                hookAfterIfEnabled(m) { param ->
                    val list = param.args[0] as? MutableList<*>
                    if (list != null && list.isNotEmpty()) {
                        filterDataList(list)
                    }
                }
            }
        }
    }

    /** 数据列表过滤：移除黑名单动态/评论；正常动态移除黑名单评论预览（保留动态）。 */
    private fun filterDataList(list: MutableList<*>) {
        val it = list.iterator()
        var removed = 0
        var commentsFiltered = 0
        while (it.hasNext()) {
            val item = it.next()
            if (item == null) {
                continue
            }
            if (shouldBlockData(item)) {
                it.remove()
                removed++
            } else {
                // 正常动态：移除黑名单评论预览（保留动态）
                val isBiz = item.javaClass.name.contains("BusinessFeedData")
                if (isBiz) {
                    if (filterBusinessComments(item)) {
                        commentsFiltered++
                    }
                } else if (item.javaClass.name == "q11.ca") {
                    if (filterFeedComments(item)) {
                        commentsFiltered++
                    }
                }
            }
        }
        if (removed > 0) {
            Log.i("BlockListPlus: qzone data removed $removed")
        }
        if (commentsFiltered > 0) {
            Log.i("BlockListPlus: qzone comments filtered $commentsFiltered")
        }
    }
}

// region 空间

/**
 * PB 动态数据是否命中黑名单：
 * b.a.a / m.b.a.a / e.a[].g 为转发原作者 uin，a.c 为动态主人 uin，i.c[].a 为 @ 用户 uin。
 */
private fun shouldBlockPbData(data: Any): Boolean {
    val items = BlockListPlusHook.activeItems
    val blockForward = BlockListPlusHook.ITEM_QZONE_FORWARD in items
    val blockAt = BlockListPlusHook.ITEM_QZONE_AT in items
    val uinB = extractUinFromPath(data, arrayOf("b", "a", "a"))
    val uinM = extractUinFromPath(data, arrayOf("m", "b", "a", "a"))
    val ownerUin = extractUinFromPath(data, arrayOf("a", "c"))
    if (blockForward && ((uinB != null && BlockListManager.isBlocked(uinB)) ||
        (uinM != null && BlockListManager.isBlocked(uinM)) ||
        (ownerUin != null && ownerUin > 0L && BlockListManager.isBlocked(ownerUin)))
    ) {
        return true
    }
    // 转发来源/原作者 uin：e.a 列表元素的 g 字段
    if (blockForward) {
        val eObj = readFieldSafe(data, "e")
        val eA = eObj?.let { readFieldSafe(it, "a") } as? List<*>
        if (eA != null) {
            for (item in eA) {
                if (item == null) {
                    continue
                }
                val srcUin = extractUinFromPath(item, arrayOf("g"))
                if (srcUin != null && srcUin > 0L && BlockListManager.isBlocked(srcUin)) {
                    Log.i("BlockListPlus: qzone forward-source HIT ${maskUin(srcUin)}")
                    return true
                }
            }
        }
    }
    // @ 用户列表：i.c 列表元素的 a 字段（uin）
    if (blockAt) {
        val iObj = readFieldSafe(data, "i")
        val iC = iObj?.let { readFieldSafe(it, "c") } as? List<*>
        if (iC != null) {
            for (at in iC) {
                if (at == null) {
                    continue
                }
                val uin = extractUinFromPath(at, arrayOf("a"))
                if (uin != null && uin > 0L && BlockListManager.isBlocked(uin)) {
                    Log.i("BlockListPlus: qzone at HIT ${maskUin(uin)}")
                    return true
                }
            }
        }
    }
    return false
}

/** 统一数据判断：动态（q11.ca）/评论包装（e.b=q11.bi）/BusinessFeedData 包装动态。 */
private fun shouldBlockData(item: Any): Boolean {
    val blockComment = BlockListPlusHook.ITEM_QZONE_COMMENT in BlockListPlusHook.activeItems
    when (item.javaClass.name) {
        "q11.ca" -> {
            if (shouldBlockPbData(item)) {
                return true
            }
            // 动态本体正常但评论预览含黑名单：移除黑名单评论（保留动态）
            if (blockComment && filterFeedComments(item)) {
                Log.i("BlockListPlus: qzone feed comments filtered")
            }
            return false
        }
    }
    // 评论包装 e.b = q11.bi
    if (blockComment) {
        val b = readFieldSafe(item, "b")
        if (b != null && b.javaClass.name == "q11.bi") {
            return shouldBlockCommentData(item)
        }
    }
    // 详情页评论项（feedx detailcomment）：c=Comment（评论者 user.uin）、d=Reply（回复者 user.uin）
    if (item.javaClass.name == "com.qzone.reborn.feedx.presenter.detailcomment.a") {
        val commentUin = extractUinFromPath(item, arrayOf("c", "user", "uin"))
        if (commentUin != null && commentUin > 0L && BlockListManager.isBlocked(commentUin)) {
            return true
        }
        val replyUin = extractUinFromPath(item, arrayOf("d", "user", "uin"))
        if (replyUin != null && replyUin > 0L && BlockListManager.isBlocked(replyUin)) {
            return true
        }
    }
    // Comment 评论对象（详情页评论列表）：评论者 user.uin 黑名单
    if (item.javaClass.name == "com.qzone.proxy.feedcomponent.model.Comment") {
        val uin = extractUinFromPath(item, arrayOf("user", "uin"))
        return uin != null && uin > 0L && BlockListManager.isBlocked(uin)
    }
    // BusinessFeedData 包装（AIO 动态页旧架构）：作者/转发原作者 uin 黑名单
    if (item.javaClass.name.contains("BusinessFeedData")) {
        // 作者 uin：cellUserInfo.user.uin
        val authorUin = extractUinFromPath(item, arrayOf("cellUserInfo", "user", "uin"))
        if (authorUin != null && authorUin > 0L && BlockListManager.isBlocked(authorUin)) {
            return true
        }
        // 转发原作者：cellOriginalInfo（嵌套 BusinessFeedData）递归判断
        val original = readFieldSafe(item, "cellOriginalInfo")
        if (original != null && original.javaClass.name.contains("BusinessFeedData")) {
            return shouldBlockData(original)
        }
    }
    return false
}

/**
 * 移除 BusinessFeedData 卡片评论预览（cellCommentInfo.commments）中的黑名单评论，动态本体保留。
 */
private fun filterBusinessComments(item: Any): Boolean {
    val commentInfo = readFieldSafe(item, "cellCommentInfo") ?: return false
    val comments = readFieldSafe(commentInfo, "commments") as? MutableList<*> ?: return false
    var removed = false
    try {
        val it = comments.iterator()
        while (it.hasNext()) {
            val c = it.next() ?: continue
            val uin = extractUinFromPath(c, arrayOf("user", "uin"))
            if (uin != null && uin > 0L && BlockListManager.isBlocked(uin)) {
                it.remove()
                removed = true
            }
        }
    } catch (e: UnsupportedOperationException) {
        // 列表不可变：跳过评论过滤，不影响动态本体
    }
    return removed
}

/** 移除动态卡片评论预览（q11.ca.h.b）中的黑名单评论及其回复，动态本体保留。 */
private fun filterFeedComments(data: Any): Boolean {
    val hObj = readFieldSafe(data, "h") ?: return false
    val hB = readFieldSafe(hObj, "b") as? MutableList<*> ?: return false
    var removed = false
    try {
        val it = hB.iterator()
        while (it.hasNext()) {
            val comment = it.next() ?: continue
            if (commentHasBlockedUin(comment)) {
                it.remove()
                removed = true
                continue
            }
            // 回复列表 e 同样过滤
            val eList = readFieldSafe(comment, "e") as? MutableList<*>
            if (eList != null) {
                val eIt = eList.iterator()
                while (eIt.hasNext()) {
                    val reply = eIt.next() ?: continue
                    if (commentHasBlockedUin(reply)) {
                        eIt.remove()
                        removed = true
                    }
                }
            }
        }
    } catch (e: UnsupportedOperationException) {
        // 列表不可变：跳过评论过滤，不影响动态本体
    }
    return removed
}

/** 评论/回复项是否含黑名单 uin（b.a 为 uin，i.a 为作者主页 URL 兜底）。 */
private fun commentHasBlockedUin(item: Any): Boolean {
    val uinA = extractUinFromPath(item, arrayOf("b", "a"))
    if (uinA != null && uinA > 0L && BlockListManager.isBlocked(uinA)) {
        return true
    }
    val url = readFieldSafe(item, "i")?.let { readFieldSafe(it, "a") } as? String
    if (url != null) {
        val m = COMMENT_URL_UIN_REGEX.find(url)
        val uinFromUrl = m?.groupValues?.getOrNull(1)?.toLongOrNull()
        if (uinFromUrl != null && uinFromUrl > 0L && BlockListManager.isBlocked(uinFromUrl)) {
            return true
        }
    }
    return false
}

/** 评论作者主页 URL 中 uin 提取正则（user.qzone.qq.com/{uin}/311/）。 */
private val COMMENT_URL_UIN_REGEX = Regex("user\\.qzone\\.qq\\.com/(\\d+)/")

/** 评论数据是否命中黑名单：评论包装 e.b=q11.bi，评论者 uin 在 bi.b.a。 */
private fun shouldBlockCommentData(item: Any): Boolean {
    val bi = readFieldSafe(item, "b") ?: return false
    if (bi.javaClass.name != "q11.bi") {
        return false
    }
    val uin = extractUinFromPath(bi, arrayOf("b", "a")) ?: return false
    return uin > 0L && BlockListManager.isBlocked(uin)
}

/**
 * 沿字段路径链式读取并提取 uin（PB 嵌套结构，如 b.a.a 为 String uin）。
 */
private fun extractUinFromPath(root: Any, path: Array<String>): Long? {
    var current: Any? = root
    for (name in path) {
        current = readFieldSafe(current, name) ?: return null
    }
    return when (current) {
        is Number -> current.toLong()
        is String -> current.toLongOrNull()
        else -> null
    }
}

// endregion

/** 反射字段缓存（类名.字段名 → Field），避免重复 getDeclaredField。 */
private val mFieldCache = java.util.concurrent.ConcurrentHashMap<String, java.lang.reflect.Field>()

/** 安全反射读取字段：字段不存在/访问失败返回 null，Field 按类缓存。 */
private fun readFieldSafe(obj: Any?, name: String): Any? {
    if (obj == null) {
        return null
    }
    val cacheKey = obj.javaClass.name + "." + name
    var f = mFieldCache[cacheKey]
    if (f == null) {
        var c: Class<*>? = obj.javaClass
        while (c != null && c != Any::class.java) {
            f = try {
                c.getDeclaredField(name)
            } catch (e: NoSuchFieldException) {
                c = c.superclass
                continue
            }
            break
        }
        if (f != null) {
            f.isAccessible = true
            mFieldCache[cacheKey] = f
        }
    }
    return try {
        f?.get(obj)
    } catch (e: Throwable) {
        null
    }
}

/** 安全反射写入字段：字段不存在/写入失败返回 false */
private fun setFieldSafe(obj: Any, name: String, value: Any): Boolean {
    var c: Class<*>? = obj.javaClass
    while (c != null && c != Any::class.java) {
        val f = try {
            c.getDeclaredField(name)
        } catch (e: NoSuchFieldException) {
            c = c.superclass
            continue
        }
        return try {
            f.isAccessible = true
            when (f.type) {
                java.lang.Long.TYPE -> f.setLong(obj, (value as Number).toLong())
                java.lang.Integer.TYPE -> f.setInt(obj, (value as Number).toInt())
                java.lang.String::class.java -> f.set(obj, value.toString())
                else -> f.set(obj, value)
            }
            true
        } catch (e: Throwable) {
            false
        }
    }
    return false
}

/** uin 日志脱敏 */
private fun maskUin(uin: Long?): String {
    if (uin == null) {
        return "null"
    }
    val s = uin.toString()
    return if (s.length > 4) "****${s.takeLast(4)}" else "****"
}
