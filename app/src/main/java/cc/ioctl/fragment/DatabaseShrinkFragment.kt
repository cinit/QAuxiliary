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
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import cc.ioctl.util.ExfriendManager
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.TroopManagerHelper
import cc.ioctl.util.ui.FaultyDialog
import com.tencent.mobileqq.widget.BounceScrollView
import io.github.qauxv.R
import io.github.qauxv.SyncUtils.async
import io.github.qauxv.SyncUtils.runOnUiThread
import io.github.qauxv.activity.SettingsUiFragmentHostActivity
import io.github.qauxv.base.ISwitchCellAgent
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.IUiItemAgentProvider
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.fragment.BaseRootLayoutFragment
import io.github.qauxv.util.Log
import io.github.qauxv.util.NonUiThread
import io.github.qauxv.util.Toasts
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

class DatabaseShrinkFragment : BaseRootLayoutFragment() {

    private var mCurrentUin: Long = 0
    private var mText: TextView? = null
    private var mDatabase: SQLiteDatabase? = null
    private var mTableSizeDesc = HashMap<String, String>(16)

    // MD5 in upper case
    private val mMd5ToUinLut = HashMap<String, String>(16)
    private val mTroopName = HashMap<String, String>(16)
    private val mFriendName = HashMap<String, String>(16)

    private val mIsCalcSize: AtomicBoolean = AtomicBoolean(false)

    override fun getTitle() = "聊天记录数据库清理"

    override fun doOnCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = inflater.context
        mText = TextView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
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
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            addView(mText)
        }
        val uin = AppRuntimeHelper.getLongAccountUin()
        mCurrentUin = uin
        loadUinMd5()
        return rootLayoutView!!
    }

    private fun loadUinMd5() {
        if (mCurrentUin < 10000) {
            return
        }
        try {
            val troops = TroopManagerHelper.getTroopInfoList()
            troops?.let {
                for (troop in it) {
                    val uin = troop.troopuin!!
                    val md5 = uinToMd5(uin)
                    mMd5ToUinLut[md5] = uin
                    mTroopName[uin] = troop.troopname
                }
            }
        } catch (e: Exception) {
            Log.e(e)
        }
        try {
            val friends = ExfriendManager.get(mCurrentUin).persons
            friends?.let {
                for (friend in it) {
                    val uin = friend.key.toString()
                    var nick = friend.value.remark
                    if (nick.isNullOrEmpty()) {
                        nick = friend.value.nick
                    }
                    val md5 = uinToMd5(uin)
                    mMd5ToUinLut[md5] = uin
                    mFriendName[uin] = nick
                }
            }
        } catch (e: Exception) {
            Log.e(e)
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    @UiThread
    private fun updateStatus() {
        val ctx = requireContext()
        val sb = SpannableStringBuilder()
        if (mCurrentUin < 10000) {
            sb.append("无法获取当前账号，请先 ")
            sb.appendClickable("选择账号") {
                val editText = EditText(ctx)
                AlertDialog.Builder(requireContext())
                    .setTitle("请输入账号")
                    .setView(editText)
                    .setPositiveButton("确定") { _, _ ->
                        val uin = editText.text.toString().toLongOrNull()
                        if (uin != null) {
                            mCurrentUin = uin
                            updateStatus()
                        } else {
                            Toasts.error(ctx, "账号格式错误")
                        }
                    }
            }
            subtitle = "请先选择账号"
        } else {
            subtitle = "当前账号：$mCurrentUin"
            val uinStr = mCurrentUin.toString()
            if (mDatabase == null) {
                try {
//                    val factory = Reflex.newInstance(
//                        Initiator.loadClass("com.tencent.mobileqq.persistence.qslowtable.QSlowTableEntityManagerFactory"),
//                        uinStr, String::class.java
//                    )
//                    val helper = Reflex.invokeVirtual(factory, "build", uinStr, String::class.java)
//                    val wrapper = Reflex.invokeVirtual(helper, "getWritableDatabase")
//                    mDatabase = if (wrapper != null && wrapper !is SQLiteDatabase) {
//                        Reflex.getInstanceObject(wrapper, "db", SQLiteDatabase::class.java)
//                    } else {
//                        wrapper as SQLiteDatabase
//                    }
                    mDatabase = SQLiteDatabase.openDatabase(
                        File(ctx.dataDir, "databases/slowtable_$mCurrentUin.db").absolutePath,
                        null,
                        SQLiteDatabase.OPEN_READWRITE
                    )
                } catch (e: Exception) {
                    sb.append(Log.getStackTraceString(e))
                }
            }
            mDatabase?.let { database ->
                try {
                    val tableNames = ArrayList<String>()
                    database.rawQuery("select name from sqlite_master where type=\"table\" and name like \"mr_%\"", null).use {
                        while (it.moveToNext()) {
                            val name = it.getString(0)
                            tableNames.add(name)
                        }
                    }
                    requestUpdateTableSizeIfUnknown(database, tableNames)
                    for (table: String in tableNames) {
                        val sizeDesc: String = mTableSizeDesc[table] ?: "COUNT(*)=<unknown>"
                        sb.apply {
                            append(table)
                            append("\n")
                            val parts = table.split("_")
                            if (parts[0] == "mr") {
                                val md5 = parts[2]
                                val uin = mMd5ToUinLut[md5]
                                if (uin != null) {
                                    var ext: String? = null
                                    if (parts[1] == "troop") {
                                        ext = mTroopName[uin]
                                    } else if (parts[1] == "friend") {
                                        ext = mFriendName[uin]
                                    }
                                    if (ext != null) {
                                        append("$uin // $sizeDesc // $ext")
                                    } else {
                                        append("$uin")
                                    }
                                } else {
                                    append("<unknown> // $sizeDesc")
                                }
                            }
                            append("\n[ ")
                            appendClickable("COUNT") {
                                async {
                                    getTableSize(database, table)
                                    runOnUiThread {
                                        updateStatus()
                                    }
                                }
                            }
                            append(" | ")
                            appendClickable("DROP") {
                                confirmAndExecuteSql(database, "DROP TABLE '$table';")
                            }
                            append(" ]\n\n")
                        }
                    }
                } catch (e: Exception) {
                    sb.append(Log.getStackTraceString(e))
                }
            }
            mText!!.text = sb
        }
    }

    private fun confirmAndExecuteSql(db: SQLiteDatabase, sql: String) {
        val ctx = requireContext()
        AlertDialog.Builder(ctx)
            .setTitle("确定要执行该语句吗？")
            .setMessage(sql)
            .setPositiveButton("确定") { _, _ ->
                val waitDialog = AlertDialog.Builder(ctx)
                    .setTitle("请稍候")
                    .setMessage(sql)
                    .setCancelable(false)
                    .show()
                async {
                    try {
                        db.execSQL(sql)
                        runOnUiThread {
                            Toasts.success(ctx, "操作完成")
                            updateStatus()
                        }
                    } catch (e: Exception) {
                        runOnUiThread { FaultyDialog.show(ctx, e) }
                    } finally {
                        runOnUiThread { waitDialog.dismiss() }
                    }
                }
            }
            .setCancelable(true)
            .setNegativeButton("取消", null)
            .show()
    }

    @NonUiThread
    private fun getTableSize(db: SQLiteDatabase, table: String): Long {
        if (!table.matches("[a-zA-Z0-9_]+".toRegex())) {
            throw IllegalArgumentException("Invalid table name: '$table'")
        }
        var size: Long = -1
        return try {
            db.rawQuery("SELECT COUNT(*) FROM '$table'", null).use {
                if (it.moveToNext()) {
                    size = it.getLong(0)
                }
            }
            mTableSizeDesc[table] = "COUNT(*)=$size"
            size
        } catch (e: Exception) {
            mTableSizeDesc[table] = e.toString()
            -1L
        }
    }

    private fun requestUpdateTableSizeIfUnknown(db: SQLiteDatabase, tables: ArrayList<String>) {
        if (mIsCalcSize.get()) {
            return
        }
        // find unknown tables
        val unknownTables = ArrayList<String>()
        for (table in tables) {
            if (mTableSizeDesc[table] == null) {
                unknownTables.add(table)
            }
        }
        if (unknownTables.isEmpty()) {
            return
        }
        // do async update
        if (!mIsCalcSize.compareAndSet(false, true)) {
            return
        }
        async {
            try {
                for (table in unknownTables) {
                    getTableSize(db, table)
                    runOnUiThread { updateStatus() }
                }
            } catch (e: Exception) {
                if (isAdded) {
                    runOnUiThread { FaultyDialog.show(requireContext(), e) }
                }
            } finally {
                mIsCalcSize.set(false)
            }
        }
    }

    override fun onDestroy() {
        mDatabase?.close()
        mDatabase = null
        super.onDestroy()
    }

    @UiItemAgentEntry
    object DatabaseShrinkItemEntry : IUiItemAgentProvider, IUiItemAgent {
        override val titleProvider: (IUiItemAgent) -> String = { "清理聊天记录数据库" }
        override val summaryProvider: ((IUiItemAgent, Context) -> String?)? = null
        override val valueState: MutableStateFlow<String?>? = null
        override val validator: ((IUiItemAgent) -> Boolean)? = null
        override val switchProvider: ISwitchCellAgent? = null
        override val onClickListener: ((IUiItemAgent, Activity, View) -> Unit) = { _, activity, _ ->
            SettingsUiFragmentHostActivity.startFragmentWithContext(activity, DatabaseShrinkFragment::class.java)
        }
        override val extraSearchKeywordProvider: ((IUiItemAgent, Context) -> Array<String>?)? = null
        override val uiItemAgent: IUiItemAgent = this
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
                    sb.append(String.format("%02X", b))
                }
                sb.toString()
            }
        }
    }
}
