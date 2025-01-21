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
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import cc.hicore.QApp.QAppUtils
import cc.hicore.message.chat.SessionHooker
import cc.hicore.message.chat.SessionUtils
import cc.ioctl.util.Reflex
import cc.ioctl.util.ui.FaultyDialog
import com.github.kyuubiran.ezxhelper.utils.argTypes
import com.github.kyuubiran.ezxhelper.utils.args
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.newInstance
import io.github.qauxv.R
import io.github.qauxv.base.RuntimeErrorTracer
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.bridge.QQMessageFacade
import io.github.qauxv.bridge.ntapi.MsgServiceHelper
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.ResUtils
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Initiator._BaseChatPie
import io.github.qauxv.util.Log
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.dexkit.CMessageCache
import io.github.qauxv.util.dexkit.CMessageRecordFactory
import io.github.qauxv.util.dexkit.CMultiMsgManager
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.MultiSelectBarVM
import io.github.qauxv.util.dexkit.MultiSelectToBottomIntent
import io.github.qauxv.util.dexkit.NBaseChatPie_createMulti
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.requireMinTimVersion
import mqq.app.AppActivity
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.invoke
import xyz.nextalone.util.method
import xyz.nextalone.util.throwOrTrue
import java.lang.Thread.sleep
import kotlin.concurrent.thread

@FunctionHookEntry
@UiItemAgentEntry
object MultiActionHook : CommonSwitchFunctionHook(
    arrayOf(
        CMessageCache,
        CMessageRecordFactory,
        NBaseChatPie_createMulti,
        CMultiMsgManager,
        MultiSelectToBottomIntent,
        MultiSelectBarVM,
    )
), SessionHooker.IAIOParamUpdate {

    override val name = "批量撤回消息"
    override val description = "多选消息后撤回"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

    private var baseChatPie: Any? = null
    private var nt_aioParam: Any? = null

    public override fun initOnce() = throwOrTrue {
        if (QAppUtils.isQQnt()) {
            hookNt()
            return@throwOrTrue
        }
        val m = DexKit.loadMethodFromCache(NBaseChatPie_createMulti)!!
        m.hookAfter(this) {
            val rootView = findView(m.declaringClass, it.thisObject) ?: return@hookAfter
            val context = rootView.context as AppActivity
            baseChatPie = if (m.declaringClass.isAssignableFrom(_BaseChatPie())) it.thisObject
            else Reflex.getFirstByType(it.thisObject, _BaseChatPie())
            val count = rootView.childCount
            val enableTalkBack = rootView.getChildAt(0).contentDescription != null
            val iconResId: Int = if (ResUtils.isInNightMode()) R.drawable.ic_recall_28dp_white else R.drawable.ic_recall_28dp_black
            if (rootView.findViewById<View?>(R.id.ketalRecallImageView) == null) rootView.addView(
                create(context, iconResId, enableTalkBack, null),
                count - 1
            )
            setMargin(rootView)
        }
    }

    private fun hookNt() {
        if (requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)) {
            Initiator.loadClass("com.tencent.tim.aio.inputbar.TimMultiSelectBarVB").method("J")
        } else {
            Initiator.loadClass("com.tencent.mobileqq.aio.input.multiselect.MultiSelectBarVB").method("onCreateView")
        }!!.hookAfter(this) {
            val rootView = findViewNt(it.method.declaringClass, it.thisObject) ?: return@hookAfter
            val context = rootView.context as AppActivity
            val count = rootView.childCount
            val iconResId: Int = if (ResUtils.isInNightMode()) R.drawable.ic_recall_28dp_white else R.drawable.ic_recall_28dp_black
            if (rootView.findViewById<View?>(R.id.ketalRecallImageView) == null) {
                if (requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)) {
                    // 20241228 TIM_NT 待优化布局
                    val enableTalkBack = rootView.getChildAt(0).contentDescription != null
                    rootView.addView(
                        create(context, iconResId, enableTalkBack, it.thisObject).apply {
                            layoutParams = rootView.getChildAt(0).layoutParams
                        },
                        count - 1
                    )
                    return@hookAfter
                }
                if (count >= 11) {
                    // Since QQ 9.0.30, using view to separate
                    val enableTalkBack = rootView.getChildAt(1).contentDescription != null
                    val separator = View(context).apply {
                        layoutParams = rootView.getChildAt(0).layoutParams
                    }
                    rootView.addView(
                        create(context, iconResId, enableTalkBack, it.thisObject).apply {
                            layoutParams = rootView.getChildAt(1).layoutParams
                        },
                        count - 2
                    )
                    rootView.addView(separator, count - 1)
                } else {
                    val enableTalkBack = rootView.getChildAt(0).contentDescription != null
                    rootView.addView(
                        create(context, iconResId, enableTalkBack, it.thisObject),
                        count - 1
                    )
                    setMargin(rootView)
                }
            }
        }

        if (requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)) {
            // 20241228 TIM_NT 貌似使用别的方法
            DexKit.requireClassFromCache(MultiSelectBarVM).method("L")
        } else if (requireMinQQVersion(QQVersion.QQ_9_1_5_BETA_20015)) {
            DexKit.requireClassFromCache(MultiSelectBarVM).method("handleIntent")
        } else {
            Initiator.loadClass("com.tencent.mobileqq.aio.input.multiselect.MultiSelectBarVM").method("handleIntent")
        }!!.hookBefore(this) {
            // 劫持一个 intent 自己传参
            val intent = it.args[0]

            val intentClass = DexKit.requireClassFromCache(MultiSelectToBottomIntent)
            if (intent.javaClass.isAssignableFrom(intentClass)) {
                val flags = Reflex.getFirstByType(intent, Int::class.java)
                if (flags != -114514) return@hookBefore
                val mContext = it.thisObject.invoke("getMContext")!!
                val multiSelectUtilClazz = Initiator.loadClass("com.tencent.mobileqq.aio.msglist.holder.component.multifoward.b")
                val selectUtil = Reflex.getStaticObject(multiSelectUtilClazz, "a")
                val m = multiSelectUtilClazz.findMethod { returnType.isAssignableFrom(List::class.java) }
                val list = (m.invoke(selectUtil, mContext) as List<*>)
                    .map { it!!.invoke("getMsgId") as Long }
                Log.d("handleIntent, msg: ${list.joinToString("\n") { it.toString() }}")
                val msgServer = MsgServiceHelper.getKernelMsgService(AppRuntimeHelper.getAppRuntime()!!)
                thread {
                    list.chunked(10).forEach { subList ->
                        msgServer!!.recallMsg(SessionUtils.AIOParam2Contact(nt_aioParam), ArrayList<Long>(subList)) { i2, str ->
                            Log.d("do recallMsg result:$str")
                        }
                        sleep(3500)
                    }
                }
                it.result = null
            }
        }

    }

    private fun recall(ctx: Context) {
        thread {
            try {
                val clazz = DexKit.requireClassFromCache(CMultiMsgManager)
                val manager = Reflex.findMethodByTypes_1(clazz, clazz).invoke(null)
                val list = Reflex.findMethodByTypes_1(clazz, MutableList::class.java)
                    .invoke(manager) as List<*>
                for (msg in list) {
                    QQMessageFacade.revokeMessage(msg)
                    sleep(250)
                }
                SyncUtils.runOnUiThread {
                    try {   // 为了防止多次点击导致闪退
                        Reflex.invokeVirtualAny(
                            baseChatPie,
                            false,
                            null,
                            false,
                            Boolean::class.javaPrimitiveType,
                            Initiator._ChatMessage(),
                            Boolean::class.javaPrimitiveType
                        )
                        baseChatPie = null
                    } catch (e: Throwable) {
                        FaultyDialog.show(ctx, "批量撤回不需要重复点击哟~", e)
                    }
                }
            } catch (t: Throwable) {
                FaultyDialog.show(ctx, t)
            }
        }
    }

    private fun recallNt(ctx: Context, vb: Any) {
        runCatching {
            val baseVB = Initiator.loadClass("com.tencent.mvi.mvvm.BaseVB")
            val intentClass = DexKit.requireClassFromCache(MultiSelectToBottomIntent)
            val flags: Int = -114514
            val intent = intentClass.newInstance(args(flags), argTypes(Int::class.java))!!
            baseVB.method(if (requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)) "L1" else "sendIntent")!!.invoke(vb, intent)
            (ctx as Activity).onBackPressed()
        }.onFailure {
            Log.e(it)
        }
    }

    private fun setMargin(rootView: LinearLayout) {
        val width = rootView.resources.displayMetrics.widthPixels
        val count = rootView.childCount
        var rootMargin = (rootView.layoutParams as ViewGroup.MarginLayoutParams).leftMargin
        if (rootMargin == 0) {
            rootMargin = (rootView.getChildAt(0).layoutParams as LinearLayout.LayoutParams).leftMargin
        }
        val w = (rootView.getChildAt(0).layoutParams as LinearLayout.LayoutParams).height
        val leftMargin = (width - rootMargin * 2 - w * count) / (count - 1)
        for (i in 1 until count) {
            val view = rootView.getChildAt(i)
            val layoutParams = LinearLayout.LayoutParams(w, w)
            layoutParams.marginStart = leftMargin
            layoutParams.gravity = Gravity.CENTER_VERTICAL
            view.layoutParams = layoutParams
        }
    }

    private fun findView(clazz: Class<*>, obj: Any): LinearLayout? {
        for (f in clazz.declaredFields) {
            if (f.type == LinearLayout::class.java) {
                f.isAccessible = true
                val view = f[obj] ?: continue
                if (check(view as LinearLayout))
                    return view
            }
        }
        return null
    }

    private fun findViewNt(clazz: Class<*>, obj: Any): LinearLayout? {
        for (f in clazz.declaredFields) {
            if (f.type.name == "kotlin.Lazy") {
                f.isAccessible = true
                val lazyObj = f[obj]!!.invoke("getValue")
                if (lazyObj is LinearLayout && check(lazyObj))
                    return lazyObj
            }
        }
        return null
    }

    private fun check(rootView: LinearLayout): Boolean {
        val count = rootView.childCount
        val num = if (QAppUtils.isQQnt()) 2 else 1
        // 完全想不起来为啥是 1
        if (count <= num) return false
        for (i in 0 until count) {
            val view = rootView.getChildAt(i)
            if (view is TextView) return false
        }
        return true
    }

    private fun create(context: Context, resId: Int, enableTalkBack: Boolean, vb: Any?): ImageView {
        val imageView = ImageView(context)
        if (enableTalkBack) {
            imageView.contentDescription = "撤回"
        }
        imageView.setOnClickListener {
            if (QAppUtils.isQQnt()) {
                recallNt(it.context, vb!!)
            } else {
                recall(it.context)
            }
        }
        imageView.setImageResource(resId)
        imageView.id = R.id.ketalRecallImageView
        return imageView
    }

    override fun onAIOParamUpdate(AIOParam: Any?) {
        nt_aioParam = AIOParam
    }

    override val runtimeErrorDependentComponents: List<RuntimeErrorTracer> = listOf(SessionHooker.INSTANCE)

}
