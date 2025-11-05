/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
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

package io.github.duzhaokun123.hook

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.LayoutHelper
import com.github.kyuubiran.ezxhelper.utils.argTypes
import com.github.kyuubiran.ezxhelper.utils.args
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.newInstance
import com.xiaoniu.util.ContextUtils
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.RecentPopup_onClickAction
import xyz.nextalone.util.get
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object InternalBrowserMenu : CommonSwitchFunctionHook(targets = arrayOf(RecentPopup_onClickAction)) {
    override val name = "内部浏览器菜单"
    override val description = "在首页加号菜单中添加打开内部浏览器功能"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MISC_CATEGORY
    override val isAvailable = QAppUtils.isQQnt()

    private const val BOOKMARKS_KEY = "internal_browser_bookmarks"
    private const val DEFAULT_URL = "https://www.google.com"

    override fun initOnce(): Boolean {
        return throwOrTrue {
            val menuItemId = 414415
            val entryMenuItem = Initiator.loadClass("com.tencent.widget.PopupMenuDialog\$MenuItem").newInstance(
                args(menuItemId, "内部浏览器", "内部浏览器", R.drawable.ic_item_tool_72dp),
                argTypes(Int::class.java, String::class.java, String::class.java, Int::class.java)
            )!!

            Initiator.loadClass("com.tencent.widget.PopupMenuDialog").getDeclaredMethod(
                "conversationPlusBuild",
                Activity::class.java,
                List::class.java,
                Initiator.loadClass("com.tencent.widget.PopupMenuDialog\$OnClickActionListener"),
                Initiator.loadClass("com.tencent.widget.PopupMenuDialog\$OnDismissListener")
            ).hookBefore {
                it.args[1] = it.args[1] as List<*> + listOf(entryMenuItem)
            }

            DexKit.requireMethodFromCache(RecentPopup_onClickAction).hookBefore {
                if (it.args[0].get("id") == menuItemId) {
                    openInternalBrowser(CommonContextWrapper.createMaterialDesignContext(ContextUtils.getCurrentActivity()))
                    it.result = null
                }
            }
        }
    }

    private fun openInternalBrowser(context: Context) {
        var dialog: Dialog? = null
        val bookmarks = getBookmarks().toList()
        
        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                LayoutHelper.dip2px(context, 16F),
                LayoutHelper.dip2px(context, 16F),
                LayoutHelper.dip2px(context, 16F),
                LayoutHelper.dip2px(context, 16F)
            )
        }
        
        val editText = EditText(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
            hint = "请输入URL"
        }
        
        val inputLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(
                editText,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1F)
            )
        }
        
        mainLayout.addView(inputLayout)
        
        if (bookmarks.isNotEmpty()) {
            val bookmarkTitle = TextView(context).apply {
                text = "书签:"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
                setPadding(0, LayoutHelper.dip2px(context, 16F), 0, LayoutHelper.dip2px(context, 8F))
            }
            mainLayout.addView(bookmarkTitle)
            
            val bookmarkLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }
            
            for (bookmark in bookmarks) {
                val bookmarkView = TextView(context).apply {
                    text = bookmark
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
                    setPadding(
                        0,
                        LayoutHelper.dip2px(context, 4F),
                        0,
                        LayoutHelper.dip2px(context, 4F)
                    )
                    setOnClickListener {
                        dialog?.dismiss()
                        startInternalBrowserActivity(context, bookmark)
                    }
                }
                bookmarkLayout.addView(bookmarkView)
            }
            
            val scrollView = ScrollView(context).apply {
                addView(bookmarkLayout)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    LayoutHelper.dip2px(context, 200F)
                )
            }
            
            mainLayout.addView(scrollView)
        }

        dialog = AlertDialog.Builder(context)
            .setTitle("内部浏览器")
            .setView(mainLayout)
            .setCancelable(true)
            .setPositiveButton("打开") { _, _ ->
                val url = editText.text.toString()
                if (TextUtils.isEmpty(url)) {
                    Toasts.error(context, "URL 不能为空")
                    return@setPositiveButton
                }
                startInternalBrowserActivity(context, url)
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("管理书签") { _, _ ->
                showManageBookmarksDialog(context)
            }
            .show()
    }

    private fun showManageBookmarksDialog(context: Context) {
        val bookmarks = getBookmarks().toMutableList()
        
        val items = bookmarks.toTypedArray()
        val checkedItems = BooleanArray(items.size) { false }
        
        AlertDialog.Builder(context)
            .setTitle("管理书签")
            .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("删除") { _, _ ->
                val toRemove = mutableListOf<String>()
                for (i in checkedItems.indices) {
                    if (checkedItems[i]) {
                        toRemove.add(items[i])
                    }
                }
                bookmarks.removeAll(toRemove)
                saveBookmarks(bookmarks)
                Toasts.success(context, "已删除 ${toRemove.size} 个书签")
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("添加") { _, _ ->
                showAddBookmarkDialog(context)
            }
            .show()
    }

    private fun showAddBookmarkDialog(context: Context) {
        val editText = EditText(context)
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
        val linearLayout = LinearLayout(context)
        linearLayout.addView(
            editText,
            LayoutHelper.newLinearLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        AlertDialog.Builder(context)
            .setTitle("添加书签")
            .setView(linearLayout)
            .setCancelable(true)
            .setPositiveButton("添加") { _, _ ->
                val url = editText.text.toString()
                if (TextUtils.isEmpty(url)) {
                    Toasts.error(context, "URL 不能为空")
                    return@setPositiveButton
                }
                addBookmark(url)
                Toasts.success(context, "已添加书签")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun getBookmarks(): Set<String> {
        val configManager = ConfigManager.getDefaultConfig()
        return configManager.getStringSetOrDefault(BOOKMARKS_KEY, setOf(DEFAULT_URL))
    }

    private fun saveBookmarks(bookmarks: List<String>) {
        val configManager = ConfigManager.getDefaultConfig()
        configManager.putStringSet(BOOKMARKS_KEY, bookmarks.toSet())
        configManager.save()
    }

    private fun addBookmark(url: String) {
        val bookmarks = getBookmarks().toMutableList()
        if (!bookmarks.contains(url)) {
            bookmarks.add(url)
            saveBookmarks(bookmarks)
        }
    }

    private fun startInternalBrowserActivity(context: Context, url: String) {
        try {
            val browser = Initiator.loadClass("com.tencent.mobileqq.activity.QQBrowserDelegationActivity")
            context.startActivity(
                Intent(context, browser).apply {
                    putExtra("fling_action_key", 2)
                    putExtra("fling_code_key", context.hashCode())
                    putExtra("useDefBackText", true)
                    putExtra("param_force_internal_browser", true)
                    putExtra("url", url)
                }
            )
        } catch (_: Exception) {
            Toasts.error(context, "无法启动内置浏览器")
        }
    }
}