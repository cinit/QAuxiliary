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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import cc.ioctl.util.Reflex
import com.tencent.mobileqq.app.BaseActivity
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.QQMessageFacade
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.ResUtils
import io.github.qauxv.util.DexKit
import io.github.qauxv.util.Initiator
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.throwOrTrue
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object MultiActionHook : CommonSwitchFunctionHook(
    intArrayOf(
        DexKit.C_MessageCache,
        DexKit.C_MSG_REC_FAC,
        DexKit.N_BASE_CHAT_PIE__createMulti,
        DexKit.C_MultiMsg_Manager)
) {

    override val name = "批量撤回消息"
    override val description = "多选消息后撤回"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

    private var baseChatPie: Any? = null
    private var img: Bitmap? = null
    private val recallBitmap: Bitmap?
        get() {
            if (img == null || img!!.isRecycled) img =
                BitmapFactory.decodeStream(ResUtils.openAsset("recall.png"))
            return img
        }

    public override fun initOnce() = throwOrTrue {
        val m = DexKit.doFindMethod(DexKit.N_BASE_CHAT_PIE__createMulti)
        m?.hookAfter(this) {
            val rootView = findView(m.declaringClass, it.thisObject) ?: return@hookAfter
            val context = rootView.context as BaseActivity
            baseChatPie =
                Reflex.getFirstByType(it.thisObject, Initiator._BaseChatPie() as Class<*>)
            val count = rootView.childCount
            val enableTalkBack = rootView.getChildAt(0).contentDescription != null
            if (rootView.findViewById<View?>(R.id.ketalRecallImageView) == null) rootView.addView(
                create(context, recallBitmap, enableTalkBack),
                count - 1
            )
            setMargin(rootView)
        }
    }

    private fun recall() {
        throwOrTrue {
            val clazz = DexKit.doFindClass(DexKit.C_MultiMsg_Manager)
            val manager = Reflex.findMethodByTypes_1(clazz, clazz).invoke(null)
            val list = Reflex.findMethodByTypes_1(clazz, MutableList::class.java)
                .invoke(manager) as List<*>
            if (list.isNotEmpty()) {
                for (msg in list) QQMessageFacade.revokeMessage(msg)
            }
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
        }
    }

    private fun setMargin(rootView: LinearLayout) {
        val width = rootView.resources.displayMetrics.widthPixels
        val count = rootView.childCount
        var rootMargin = (rootView.layoutParams as RelativeLayout.LayoutParams).leftMargin
        if (rootMargin == 0) {
            rootMargin = (rootView.getChildAt(0).layoutParams as LinearLayout.LayoutParams).leftMargin
        }
        val w = (rootView.getChildAt(0).layoutParams as LinearLayout.LayoutParams).height
        val leftMargin = (width - rootMargin * 2 - w * count) / (count - 1)
        for (i in 1 until count) {
            val view = rootView.getChildAt(i)
            val layoutParams = LinearLayout.LayoutParams(w, w)
            layoutParams.setMargins(leftMargin, 0, 0, 0)
            layoutParams.gravity = 16
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

    private fun check(rootView: LinearLayout): Boolean {
        val count = rootView.childCount
        if (count <= 1) return false
        for (i in 0 until count) {
            val view = rootView.getChildAt(i)
            if (view is TextView) return false
        }
        return true
    }

    private fun create(context: Context, bitmap: Bitmap?, enableTalkBack: Boolean): ImageView {
        val imageView = ImageView(context)
        if (enableTalkBack) {
            imageView.contentDescription = "撤回"
        }
        imageView.setOnClickListener { recall() }
        imageView.setImageBitmap(bitmap)
        imageView.id = R.id.ketalRecallImageView
        return imageView
    }
}
