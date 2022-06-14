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

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.core.view.plusAssign
import cc.ioctl.util.Reflex.getFirstByType
import com.tencent.mobileqq.app.BaseActivity
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.DexKit
import io.github.qauxv.util.isTim
import me.ketal.util.findViewByType
import xyz.nextalone.util.clazz
import xyz.nextalone.util.get
import xyz.nextalone.util.hide
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.hookAfterAllConstructors
import xyz.nextalone.util.hostDrawable
import xyz.nextalone.util.invoke
import xyz.nextalone.util.isPublic
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object HideSearch : CommonSwitchFunctionHook(
    dexDeobfIndexes = intArrayOf(DexKit.N_Conversation_onCreate)) {

    override val name = "隐藏搜索编辑框"

    override val description = "谨慎开启"

    override val uiItemLocation = FunctionEntryRouter.Locations.Entertainment.ENTERTAIN_CATEGORY

    override val isAvailable: Boolean get() = !isTim()

    override fun initOnce() = throwOrTrue {
        copeConversation()
        copeContacts()
        copeLeba()
    }

    //处理首页
    private fun copeConversation() {
        DexKit.doFindMethod(DexKit.N_Conversation_onCreate)
            ?.hookAfter(this) {
                val relativeLayout = it.thisObject.get("b", RelativeLayout::class.java)
                relativeLayout?.isVisible = false
                //隐藏顶栏
                val list = getFirstByType(it.thisObject, "com.tencent.mobileqq.fpsreport.FPSSwipListView".clazz) as View
                val searchView = it.thisObject.get("b", View::class.java)!!
                list.invoke("removeHeaderView", searchView, View::class.java)
                //移除消息列表搜索框
                val parent = relativeLayout?.parent as ViewGroup
                val toolbar = RelativeLayout(parent.context)
                toolbar.layoutParams = relativeLayout.layoutParams
                parent += toolbar
                //顶栏添加toolbar
                addButton(toolbar, searchView as RelativeLayout)
                //添加按钮
            }
    }

    private fun addButton(toolbar: RelativeLayout, searchView: ViewGroup) {
        val ctx = toolbar.context
        val density = toolbar.resources.displayMetrics.density
        val widthPixels = toolbar.resources.displayMetrics.widthPixels
        val w = (30.0f * density + 0.5f).toInt()
        val plusButton = ImageView(ctx)
        val layoutParams = RelativeLayout.LayoutParams(w, w)
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL)
        layoutParams.rightMargin = 60
        plusButton.layoutParams = layoutParams
        plusButton.setImageResource(ctx.hostDrawable("header_btn_add")!!)
        plusButton.setOnClickListener {
            throwOrTrue {
                val n2 = (widthPixels - (180.0f * density + 0.5f) - (6.0f * density + 0.5f)).toInt()
                val n3 = (density * 1.0f + 0.5f).toInt()
                val popBar = "com.tencent.mobileqq.activity.recent.RecentOptPopBar".clazz
                    ?.getConstructor(BaseActivity::class.java)?.newInstance(ctx)
                popBar?.invoke("a", toolbar, n2, n3, View::class.java, Int::class.java, Int::class.java)
            }
        }
        toolbar += plusButton
        val searchButton = ImageView(ctx)
        val layoutParams2 = RelativeLayout.LayoutParams(w, w)
        layoutParams2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        layoutParams2.addRule(RelativeLayout.CENTER_VERTICAL)
        layoutParams2.rightMargin = 60 + w + 30
        searchButton.layoutParams = layoutParams2
        searchButton.setImageResource(ctx.hostDrawable("qb_group_menu_search_group")!!)
        searchButton.setColorFilter(Color.WHITE)
        searchButton.setOnClickListener {
            searchView.findViewByType(EditText::class.java)?.performClick()
        }
        toolbar += searchButton
    }

    //处理联系人页
    private fun copeContacts() {
        "com.tencent.mobileqq.activity.contacts.base.SearchBarAssistant".clazz
            ?.hookAfterAllConstructors {
                val searchView = getFirstByType(it.thisObject, EditText::class.java)?.parent as View
                searchView.isVisible = !isEnabled
            }
    }

    //处理动态页
    private fun copeLeba() {
        val clazz = "com/tencent/mobileqq/leba/business/mainbiz/LebaSearchPart".clazz
            ?: return
        for (m in clazz.declaredMethods) {
            if (m.isPublic && m.returnType == Void.TYPE && m.parameterTypes.contentDeepEquals(
                    arrayOf(View::class.java))) {
                m.hookAfter(this) {
                    val searchView = getFirstByType(it.thisObject, RelativeLayout::class.java)
                    searchView.hide()
                }
            }
        }
    }
}
