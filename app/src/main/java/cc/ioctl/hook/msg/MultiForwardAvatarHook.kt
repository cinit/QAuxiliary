/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */
package cc.ioctl.hook.msg

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.buildSpannedString
import androidx.core.text.method.LinkMovementMethodCompat
import cc.hicore.QApp.QAppUtils
import cc.ioctl.hook.profile.OpenProfileCard
import cc.ioctl.util.HostInfo
import cc.ioctl.util.HostStyledViewBuilder
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.Reflex
import cc.ioctl.util.hookBeforeIfEnabled
import cc.ioctl.util.ui.FaultyDialog
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import io.github.qauxv.R
import io.github.qauxv.base.annotation.DexDeobfs
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.ntapi.ChatTypeConstants
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.step.Step
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.ui.CustomDialog
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.UiThread
import io.github.qauxv.util.dexkit.CAIOUtils
import io.github.qauxv.util.dexkit.DexDeobfsProvider.getCurrentBackend
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.DexKit.loadClassFromCache
import io.github.qauxv.util.dexkit.DexKitFinder
import io.github.qauxv.util.dexkit.DexKitTargetSealedEnum.nameOf
import io.github.qauxv.util.dexkit.Multiforward_Avatar_setListener_NT
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.xpcompat.XC_MethodHook.MethodHookParam
import me.ketal.dispacher.BaseBubbleBuilderHook
import me.ketal.dispacher.OnBubbleBuilder
import me.singleneuron.data.MsgRecordData
import xyz.nextalone.util.invoke
import java.lang.reflect.Field
import java.lang.reflect.Modifier

@FunctionHookEntry
@UiItemAgentEntry
object MultiForwardAvatarHook : CommonSwitchFunctionHook(arrayOf(CAIOUtils, Multiforward_Avatar_setListener_NT)), OnBubbleBuilder, DexKitFinder {

    override val name = "转发消息点头像查看详细信息"
    override val description = "仅限合并转发的消息"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    private var mChatItemHeadIconViewId = 0


    override val isNeedFind: Boolean
        get() = QAppUtils.isQQnt() && Multiforward_Avatar_setListener_NT.descCache == null

    override fun doFind(): Boolean {
        getCurrentBackend().use { backend ->
            val dexKit = backend.getDexKitBridge()
            val m = dexKit.findMethod {
                matcher {
                    declaredClass = "com.tencent.mobileqq.aio.msglist.holder.component.avatar.AIOAvatarContentComponent"
                    returnType = "void"
                    paramTypes(*arrayOf<Class<*>>())
                    addInvoke {
                        name = "setOnClickListener"
                    }
                }
            }
            if (m.size != 1) return false
            Log.d("save id: " + nameOf(Multiforward_Avatar_setListener_NT) + ",method: " + m.first().descriptor)
            Multiforward_Avatar_setListener_NT.descCache = m.first().descriptor
            return true
        }
    }

    private val mStep: Step = object : Step {
        override fun step(): Boolean {
            return doFind()
        }

        override fun isDone(): Boolean {
            return !isNeedFind
        }

        override fun getPriority(): Int {
            return 0
        }

        override fun getDescription(): String {
            return "点击事件相关方法查找中"
        }
    }

    private val mSteps by lazy {
        val steps = mutableListOf(mStep)
        super.makePreparationSteps()?.let {
            steps.addAll(it)
        }
        steps.toTypedArray()
    }

    override fun makePreparationSteps(): Array<Step> = mSteps

    override val isPreparationRequired: Boolean
        get() = isNeedFind || DexKit.isRunDexDeobfuscationRequired(CAIOUtils)

    @SuppressLint("DiscouragedApi")
    @Throws(Exception::class)
    public override fun initOnce(): Boolean {
        if (requireMinQQVersion(QQVersion.QQ_8_9_63_BETA_11345)) {
            val clz = Initiator.loadClass("com.tencent.mobileqq.aio.msglist.holder.component.avatar.AIOAvatarContentComponent")
            // 设置头像点击和长按事件的方法
            DexKit.requireMethodFromCache(Multiforward_Avatar_setListener_NT).hookBefore { param ->
                var layout: RelativeLayout?
                clz.declaredFields.single {//Lazy avatarContainer
                    it.name == when {
                        requireMinQQVersion(QQVersion.QQ_9_1_50) -> "i"
                        requireMinQQVersion(QQVersion.QQ_9_0_90) -> "h"
                        requireMinQQVersion(QQVersion.QQ_9_0_65) -> "i"
                        else -> "h"
                    }
                }.let {
                    it.isAccessible = true  //Lazy
                    layout = (it.get(param.thisObject))!!.invoke("getValue") as RelativeLayout
                }
                if (layout!!.context.javaClass.name == "com.tencent.mobileqq.activity.MultiForwardActivity") {
                    layout!!.setOnClickListener {
                        clz.declaredFields.single {
                            it.type.name == "com.tencent.mobileqq.aio.msg.AIOMsgItem"
                        }.let {
                            it.isAccessible = true
                            try {
                                (it.get(param.thisObject)!!.invoke("getMsgRecord")!! as MsgRecord).let {
                                    val senderUin = it.senderUin   //对方QQ
                                    val troopUin = if (it.chatType == ChatTypeConstants.GROUP) it.peerUin else null
                                    createAndShowDialogCommon(layout!!.context, it, senderUin, troopUin)
                                }
                            } catch (e: Exception) {
                                FaultyDialog.show(layout!!.context, e)
                            }
                        }
                    }
                    param.result = null
                }
            }
            return true
        }


        BaseBubbleBuilderHook.initialize()
        val kBaseBubbleBuilder = Initiator.loadClass("com/tencent/mobileqq/activity/aio/BaseBubbleBuilder")
        val onClick = kBaseBubbleBuilder.getMethod("onClick", View::class.java)
        hookBeforeIfEnabled(onClick, 49) { param: MethodHookParam ->
            val ctx = Reflex.getInstanceObjectOrNull(param.thisObject, "a", Context::class.java)
                ?: Reflex.getFirstNSFByType(param.thisObject, Context::class.java)
            val view = param.args[0] as View
            if (ctx == null || isLeftCheckBoxVisible) {
                return@hookBeforeIfEnabled
            }
            val activityName = ctx.javaClass.name
            val needShow = activityName == "com.tencent.mobileqq.activity.MultiForwardActivity" &&
                (view.javaClass.name == "com.tencent.mobileqq.vas.avatar.VasAvatar" ||
                    view.javaClass == ImageView::class.java ||
                    view.javaClass == Initiator.load("com.tencent.widget.CommonImageView"))
            if (!needShow) {
                return@hookBeforeIfEnabled
            }
            val msg = getChatMessageByView(view) ?: return@hookBeforeIfEnabled
            try {
                val istroop = Reflex.getInstanceObjectOrNull(msg, "istroop") as Int
                when (istroop) {
                    0 -> {
                        // private chat
                        val senderUin = (Reflex.getInstanceObjectOrNull(msg, "senderuin") as String).toLong()
                        createAndShowDialogCommon(ctx, msg, senderUin, null)
                    }

                    1, 3000 -> {
                        // troop and discuss group
                        val senderUin = (Reflex.getInstanceObjectOrNull(msg, "senderuin") as String).toLong()
                        val troopUin = (Reflex.getInstanceObjectOrNull(msg, "frienduin") as String).toLong()
                        createAndShowDialogCommon(ctx, msg, senderUin, troopUin)
                    }

                    else -> {
                        // wtf?
                        createAndShowDialogForDetail(ctx, msg)
                    }
                }
            } catch (e: Exception) {
                FaultyDialog.show(ctx, e)
            }
        }
        mChatItemHeadIconViewId = HostInfo.getApplication().resources
            .getIdentifier("chat_item_head_icon", "id", HostInfo.getPackageName())
        check(mChatItemHeadIconViewId != 0) { "R.id.chat_item_head_icon not found" }
        return true
    }

    override fun onGetView(rootView: ViewGroup, chatMessage: MsgRecordData, param: MethodHookParam) {
        // XXX: performance sensitive, peak frequency: ~68 invocations per second
        if (!isEnabled || mChatItemHeadIconViewId == 0) {
            return
        }
        // For versions >= x (x exists, where x <= 8.9.15), @[R.id.chat_item_head_icon].onCLickListener = null
        // register @[R.id.chat_item_head_icon] click event for versions >= x
        val baseBubbleBuilderOnClick = param.thisObject as View.OnClickListener
        val headIconView: View? = rootView.getTag(R.id.tag_chat_item_head_icon) as View?
            ?: rootView.findViewById(mChatItemHeadIconViewId)
        if (headIconView != null) {
            // set tag to avoid future findViewById to improve performance
            rootView.setTag(R.id.tag_chat_item_head_icon, headIconView)
        } else {
            // give up.
            return
        }
        headIconView.setOnClickListener(baseBubbleBuilderOnClick)
    }

    override fun onGetViewNt(rootView: ViewGroup, chatMessage: MsgRecord, param: MethodHookParam) {
        // 此处无需实现
    }

    private var mLeftCheckBoxVisible: Field? = null

    /**
     * Target TIM or QQ<=7.6.0 Here we use DexKit!!!
     *
     * @param v the view in bubble
     * @return message or null
     */
    @JvmStatic
    @DexDeobfs(CAIOUtils::class)
    fun getChatMessageByView(v: View): Any? {
        val kAIOUtils = loadClassFromCache(CAIOUtils) ?: return null
        return try {
            Reflex.invokeStaticAny(kAIOUtils, v, View::class.java, Initiator._ChatMessage())
        } catch (e: NoSuchMethodException) {
            null
        } catch (e: Exception) {
            Log.e(e)
            null
        }
    }


    @UiThread
    private fun createAndShowDialogCommon(hostContext: Context, msg: Any, senderUin: Long, troopUin: Long?) {
        check(senderUin > 0) { "senderUin must be positive, got $senderUin" }
        //check(troopUin == null || troopUin > 0) { "troopUin must be positive or null, got $troopUin" }
        // 2023.7.20 NT版收到的合并转发中群号变为0
        // 2023.8.1 收到的合并转发中群号变为284840486
        val isTroopUinAvailable = troopUin != null && troopUin != 0L && troopUin != 284840486L
        // 2023-09-06 起合并转发中 senderUin 变为 1094950020
        val isSenderUinAvailable = senderUin != 0L && senderUin != 1094950020L
        val ctx = CommonContextWrapper.createAppCompatContext(hostContext)
        val dialog = CustomDialog.createFailsafe(ctx).setTitle(Reflex.getShortClassName(msg))
            .setPositiveButton("确认", null).setCancelable(true)
            .setNeutralButton("详情") { _: DialogInterface, _: Int ->
                createAndShowDialogForDetail(ctx, msg)
            }
        val ll = LinearLayout(ctx)
        ll.orientation = LinearLayout.VERTICAL
        val p = LayoutHelper.dip2px(ctx, 10f)
        ll.setPadding(p, p / 3, p, p / 3)
        if (troopUin != null) {
            // troop
            if (isTroopUinAvailable) {
                HostStyledViewBuilder.newDialogClickableItemClickToCopy(ctx, "群号", troopUin.toString(), ll, true) {
                    OpenProfileCard.openTroopProfileActivity(ctx, troopUin.toString())
                }
            } else {
                HostStyledViewBuilder.newDialogClickableItem(ctx, "群号", "已被服务器端屏蔽", null, null, ll, true)
            }
            if (isSenderUinAvailable) {
                HostStyledViewBuilder.newDialogClickableItemClickToCopy(ctx, "成员", senderUin.toString(), ll, true) {
                    if (senderUin > 10000) {
                        OpenProfileCard.openUserProfileCard(ctx, senderUin)
                    }
                }
            } else {
                HostStyledViewBuilder.newDialogClickableItem(ctx, "成员", "已被服务器端屏蔽", null, null, ll, true)
            }
        } else {
            // private chat
            if (isSenderUinAvailable) {
                HostStyledViewBuilder.newDialogClickableItemClickToCopy(ctx, "发送者", senderUin.toString(), ll, true) {
                    if (senderUin > 10000) {
                        OpenProfileCard.openUserProfileCard(ctx, senderUin)
                    }
                }
            } else {
                HostStyledViewBuilder.newDialogClickableItem(ctx, "发送者", "已被服务器端屏蔽", null, null, ll, true)
            }
        }
        val disclaimerTextView = AppCompatTextView(ctx).apply {
            // required for ClickableSpan
            movementMethod = LinkMovementMethodCompat.getInstance()
        }
        val runnable = {
            val disclaimer = """
                警告：以上信息可能是完全错误的。
                问: “已被服务器端屏蔽”是什么意思？ 
                答：QQ 服务器端屏蔽（和谐）了该信息，因此无法获取，我也没有办法。
                QQ 的合并转发消息内容是由转发者的客户端生成后上传服务器的，而不是在服务器上合并后再下发的。
                因此，合并转发消息的内容存在被篡改和伪造的可能。
                在非恶意情形下，PC 端 QQ 在合并转发提供的群号可能是错误的，而 Android 端 QQ 合并转发提供的群号通常是正确的。
                以上信息仅供参考，本模块不对以上信息的正确性负责，以上信息不得作为任何依据。
            """.trimIndent()
            disclaimerTextView.text = disclaimer
        }
        disclaimerTextView.text = buildSpannedString {
            append("单击可打开，长按可复制\n")
            append("问: “已被服务器端屏蔽”是什么意思？ \n答：QQ 服务器端屏蔽（和谐）了该信息，因此无法获取，我也没有办法。\n")
            append("以上信息仅供参考，本模块不对以上信息的正确性负责，以上信息不得作为任何依据。\n")
            append("了解详情", object : ClickableSpan() {
                override fun onClick(widget: View) {
                    runnable()
                }
            }, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        ll.addView(disclaimerTextView)
        dialog.setView(ll)
        dialog.show()
    }

    @UiThread
    private fun createAndShowDialogForDetail(ctx: Context?, msg: Any?) {
        if (msg == null) {
            Log.e("createAndShowDialogForDetail/E msg == null")
            return
        }
        CustomDialog.createFailsafe(ctx)
            .setTitle(Reflex.getShortClassName(msg))
            .setMessage(msg.toString())
            .setCancelable(true)
            .setPositiveButton("确定", null)
            .show()
            .apply {
                findViewById<TextView>(android.R.id.message).setTextIsSelectable(true)
            }
    }

    private val isLeftCheckBoxVisible: Boolean
        get() {
            var a: Field? = null
            var b: Field? = null
            return try {
                if (mLeftCheckBoxVisible != null) {
                    mLeftCheckBoxVisible!!.getBoolean(null)
                } else {
                    for (f in Initiator.loadClass("com/tencent/mobileqq/activity/aio/BaseChatItemLayout").declaredFields) {
                        if (Modifier.isStatic(f.modifiers) && Modifier.isPublic(f.modifiers) && f.type == Boolean::class.javaPrimitiveType) {
                            if ("a" == f.name) {
                                a = f
                            }
                            if ("b" == f.name) {
                                b = f
                            }
                        }
                    }
                    if (a != null) {
                        mLeftCheckBoxVisible = a
                        return a.getBoolean(null)
                    }
                    if (b != null) {
                        mLeftCheckBoxVisible = b
                        return b.getBoolean(null)
                    }
                    false
                }
            } catch (e: Exception) {
                traceError(e)
                false
            }
        }
}
