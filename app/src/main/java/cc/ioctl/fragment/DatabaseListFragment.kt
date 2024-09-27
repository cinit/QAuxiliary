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

package cc.ioctl.fragment

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.ui.FaultyDialog
import com.tencent.mobileqq.widget.BounceScrollView
import io.github.qauxv.R
import io.github.qauxv.activity.SettingsUiFragmentHostActivity
import io.github.qauxv.base.IEntityAgent
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.fragment.BaseRootLayoutFragment
import io.github.qauxv.hook.CommonClickableStaticFunctionItem
import io.github.qauxv.util.Toasts
import java.io.File
import java.security.MessageDigest
import java.util.Locale

class DatabaseListFragment : BaseRootLayoutFragment() {

    private var mText: TextView? = null

    // "chat.trace", "123456.db", "123456-IndexQQMsg.db","slowtable_123456.db" ,"toggleFeature.db"
    private val DATABASE_REGEX: String = "(chat\\.trace|\\d+\\.db|\\d+-IndexQQMsg\\.db|slowtable_\\d+\\.db|toggleFeature\\.db)"

    // "chat.trace", "toggleFeature.db"
    private val DATABASE_ALLOW_DELETE_REGEX: String = "(chat\\.trace|toggleFeature\\.db)"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        title = "聊天记录数据库清理"
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.database_file_list_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_refresh -> {
                if (isAdded) {
                    updateStatus()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun doOnCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val ctx = inflater.context
        mText = TextView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setTextColor(ResourcesCompat.getColor(resources, R.color.firstTextColor, ctx.theme))
            val dp8 = LayoutHelper.dip2px(ctx, 8f)
            setPadding(dp8, dp8, dp8, dp8)
            movementMethod = LinkMovementMethod.getInstance()
        }
        rootLayoutView = BounceScrollView(ctx, null).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            addView(mText)
        }
        return rootLayoutView!!
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    @UiThread
    private fun updateStatus() {
        val ctx = requireContext()
        val sb = SpannableStringBuilder()
        val databaseDir = File(ctx.dataDir, "databases")
        val databaseList = databaseDir.listFiles()
        if (databaseList != null) {
            for (db: File in databaseList) {
                val dbName = db.name
                if (db.isFile && dbName.matches(DATABASE_REGEX.toRegex())) {
                    sb.apply {
                        append(db.name)
                        append("\n")
                        append(convertToSizeString(db.length()))
                        append(" [ ")
                        appendClickable("VIEW") {
                            settingsHostActivity!!.presentFragment(DatabaseShrinkFragment().apply {
                                arguments = Bundle().apply {
                                    putString(DatabaseShrinkFragment.KEY_TARGET_DATABASE_PATH, db.absolutePath)
                                }
                            })
                        }
                        if (dbName.matches(DATABASE_ALLOW_DELETE_REGEX.toRegex())) {
                            append(" | ")
                            appendClickable("DELETE") {
                                confirmDeleteFile(db)
                            }
                        }
                        append(" ]\n\n")
                    }
                }
            }
            mText!!.text = sb
        }
    }

    private fun confirmDeleteFile(file: File) {
        val ctx = requireContext()
        AlertDialog.Builder(ctx).setTitle("确定要删除吗？").setMessage(file.absolutePath).setPositiveButton("确定") { _, _ ->
            try {
                if (!file.isFile) {
                    Toasts.error(ctx, "不是文件：${file.absolutePath}")
                    return@setPositiveButton
                }
                if (!file.delete()) {
                    Toasts.error(ctx, "删除失败：${file.absolutePath}")
                } else {
                    Toasts.success(ctx, "删除成功")
                }
                updateStatus()
            } catch (e: Exception) {
                FaultyDialog.show(ctx, e)
            }
        }.setCancelable(true).setNegativeButton("取消", null).show()
    }

    @UiItemAgentEntry
    object DatabaseShrinkItemEntry : CommonClickableStaticFunctionItem() {
        override val titleProvider: (IEntityAgent) -> String = { "清理聊天记录数据库" }
        override val onClickListener: ((IUiItemAgent, Activity, View) -> Unit) = { _, activity, _ ->
            SettingsUiFragmentHostActivity.startFragmentWithContext(activity, DatabaseListFragment::class.java)
        }
        override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
    }

    private companion object {
        @JvmStatic
        private fun SpannableStringBuilder.appendSpanText(str: String, span: Any): SpannableStringBuilder {
            val start = length
            append(str)
            setSpan(span, start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            return this
        }

        @JvmStatic
        private fun SpannableStringBuilder.appendClickable(str: String, onClick: (View) -> Unit): SpannableStringBuilder {
            val start = length
            append(str)
            setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    onClick(widget)
                }
            }, start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            return this
        }

        @JvmStatic
        fun uinToMd5(uin: Long): String {
            return uinToMd5(uin.toString())
        }

        @JvmStatic
        fun uinToMd5(uin: String?): String {
            return if (uin.isNullOrEmpty()) "0" else {
                val md5 = MessageDigest.getInstance("MD5")
                val bytes = md5.digest(uin.toByteArray(Charsets.ISO_8859_1))
                val sb = StringBuilder()
                for (b in bytes) {
                    sb.append(String.format(Locale.ROOT, "%02X", b))
                }
                sb.toString()
            }
        }

        @JvmStatic
        fun convertToSizeString(size: Long): String {
            val unit = arrayOf("B", "K", "M", "G", "T", "P", "E")
            var index = 0
            var result = size.toDouble()
            while (result >= 1024 && index < unit.size - 1) {
                result /= 1024
                index++
            }
            return "${String.format(Locale.ROOT, "%.2f", result)}${unit[index]}"
        }
    }
}
