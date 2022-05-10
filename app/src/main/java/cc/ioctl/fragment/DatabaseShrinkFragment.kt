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

import android.database.sqlite.SQLiteDatabase
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
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
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
import io.github.qauxv.SyncUtils
import io.github.qauxv.SyncUtils.async
import io.github.qauxv.SyncUtils.runOnUiThread
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.fragment.BaseRootLayoutFragment
import io.github.qauxv.util.Log
import io.github.qauxv.util.NonUiThread
import io.github.qauxv.util.Toasts
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

class DatabaseShrinkFragment : BaseRootLayoutFragment() {

    private var mCurrentUin: Long = 0
    private var mText: TextView? = null
    private var mDatabase: SQLiteDatabase? = null
    private var mTableSizeDesc = HashMap<String, String>(16)

    private var mTableListCache = ArrayList<String>()
    private var mTableListCacheNeedInvalidate = true

    private var mDatabaseName: String? = null
    private var mDatabasePath: String? = null

    private var mTableFilter: Int = 0

    // MD5 in upper case
    private val mMd5ToUinLut = HashMap<String, String>(16)
    private val mTroopName = HashMap<String, String>(16)
    private val mFriendName = HashMap<String, String>(16)

    private val mIsCalcSize: AtomicBoolean = AtomicBoolean(false)

    override fun getTitle() = mDatabaseName ?: "null"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mDatabasePath = arguments?.getString(KEY_TARGET_DATABASE_PATH)
        if (mDatabasePath == null) {
            finishFragment()
            return
        }
        mDatabaseName = File(mDatabasePath!!).name
    }

    override fun doOnCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val ctx = inflater.context
        mText = TextView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setTextColor(ResourcesCompat.getColor(resources, R.color.firstTextColor, ctx.theme))
            val dp8 = LayoutHelper.dip2px(ctx, 8f)
            setPadding(dp8, dp8, dp8, dp8)
            movementMethod = LinkMovementMethod.getInstance()
        }
        rootLayoutView = BounceScrollView(ctx, null).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            addView(mText)
        }
        val uin = AppRuntimeHelper.getLongAccountUin()
        mCurrentUin = uin
        loadUinMd5()
        return rootLayoutView!!
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.database_view_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_database_filter -> {
                showFilterDialog()
                true
            }
            R.id.menu_calculate_size -> {
                calculateItemCountForeground()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun calculateItemCountForeground() {
        val ctx = requireContext()
        val db = mDatabase ?: return
        val tableList = getShowingTables()
        mIsCalcSize.set(false)
        if (mIsCalcSize.get()) {
            return
        }
        // find unknown tables
        val unknownTables = ArrayList<String>()
        for (table in tableList) {
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
        val dialog = AlertDialog.Builder(ctx)
            .setTitle("COUNT(*)")
            .setMessage("正在计算表的数据量，请稍候...")
            .setCancelable(false)
            .setNegativeButton("取消") { _, _ ->
                mIsCalcSize.set(false)
            }
            .show()
        async {
            try {
                for (i in 0 until unknownTables.size) {
                    if (!mIsCalcSize.get()) {
                        runOnUiThread { dialog.dismiss() }
                        return@async
                    } else {
                        runOnUiThread {
                            dialog.setMessage("${unknownTables[i]}\n${i + 1}/${unknownTables.size} ${(i + 1) * 100 / unknownTables.size}%")
                        }
                    }
                    getTableSize(db, unknownTables[i])
                }
            } catch (e: Exception) {
                if (isAdded) {
                    runOnUiThread { FaultyDialog.show(requireContext(), e) }
                }
            } finally {
                mIsCalcSize.set(false)
                runOnUiThread {
                    dialog.dismiss()
                    if (isAdded) {
                        updateStatus()
                    }
                }
            }
        }
    }

    private fun showFilterDialog() {
        val ctx = requireContext()
        val choicesNames: Array<String> = arrayOf("好友", "群组", "频道", "群文件", "通话", "其他")
        val choicesValues: IntArray = intArrayOf(
            CATEGORY_FRIENDS, CATEGORY_TROOPS, CATEGORY_GUILDS,
            CATEGORY_TROOP_FILE_TRANSFER_ITEM, CATEGORY_QQ_CALL, CATEGORY_OTHERS
        )
        AlertDialog.Builder(ctx).apply {
            setTitle("筛选")
            setMultiChoiceItems(
                choicesNames, booleanArrayOf(
                    (mTableFilter and CATEGORY_FRIENDS) != 0,
                    (mTableFilter and CATEGORY_TROOPS) != 0,
                    (mTableFilter and CATEGORY_GUILDS) != 0,
                    (mTableFilter and CATEGORY_TROOP_FILE_TRANSFER_ITEM) != 0,
                    (mTableFilter and CATEGORY_QQ_CALL) != 0,
                    (mTableFilter and CATEGORY_OTHERS) != 0,
                )
            ) { _, _, _ -> }
            setPositiveButton("确定") { d, _ ->
                val dialog = d as AlertDialog
                dialog.dismiss()
                var filters = 0
                val choices = dialog.listView.checkedItemPositions
                for (i in choicesValues.indices) {
                    if (choices[i]) {
                        filters = filters or choicesValues[i]
                    }
                }
                if (filters == CATEGORY_MASK) {
                    filters = 0
                }
                mTableFilter = filters
                updateStatus()
                subtitle = if (mTableFilter != 0) {
                    val types = ArrayList<String>()
                    for (i in choicesValues.indices) {
                        if (mTableFilter and choicesValues[i] != 0) {
                            types.add(choicesNames[i])
                        }
                    }
                    "筛选: " + types.joinToString(", ")
                } else {
                    null
                }
            }
            setNegativeButton("取消") { _, _ -> }
            setCancelable(true)
        }.show()
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
        if (mDatabasePath == null) {
            sb.append("mDatabasePath is null")
        } else {
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
                        mDatabasePath!!, null, SQLiteDatabase.OPEN_READWRITE
                    )
                } catch (e: Exception) {
                    sb.append(Log.getStackTraceString(e))
                }
            }
            mDatabase?.let { database ->
                try {
                    val databaseTables = ArrayList<String>()
                    if (mTableListCacheNeedInvalidate) {
                        Log.d("select * from sqlite_master where type='table'; START")
                        database.rawQuery("select name from sqlite_master where type=\"table\"", null).use {
                            while (it.moveToNext()) {
                                val name = it.getString(0)
                                if (name != "sqlite_sequence" && name != "android_metadata") {
                                    databaseTables.add(name)
                                }
                            }
                        }
                        Log.d("select * from sqlite_master where type='table'; END")
                        mTableListCacheNeedInvalidate = false
                        mTableListCache = databaseTables
                    } else {
                        databaseTables.addAll(mTableListCache)
                    }
                    val tableNames: ArrayList<String> = getShowingTables()
                    if (tableNames.isEmpty()) {
                        sb.append("No tables found.")
                    }
                    for (table: String in tableNames) {
                        val sizeDesc: String = mTableSizeDesc[table] ?: "COUNT(*)=???"
                        sb.apply {
                            append(table)
                            append("\n")
                            val parts = table.split("_")
                            if (parts[0] == "mr" && parts.size >= 3) {
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
                            } else {
                                append(sizeDesc)
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

    private fun getShowingTables(): ArrayList<String> {
        val databaseTables = mTableListCache
        return if (mTableFilter != 0) {
            ArrayList<String>().also {
                for (tableName in databaseTables) {
                    val category: Int = if (tableName.startsWith("mr_friend_")) {
                        CATEGORY_FRIENDS
                    } else if (tableName.startsWith("mr_troop_")) {
                        CATEGORY_TROOPS
                    } else if (tableName.startsWith("mr_guild_")) {
                        CATEGORY_GUILDS
                    } else if (tableName.startsWith("TroopFileTansferItemEntity")) {
                        CATEGORY_TROOP_FILE_TRANSFER_ITEM
                    } else if (tableName.startsWith("qc_")) {
                        CATEGORY_QQ_CALL
                    } else {
                        CATEGORY_OTHERS
                    }
                    if ((mTableFilter and category) != 0) {
                        it.add(tableName)
                    }
                }
            }
        } else {
            databaseTables
        }
    }

    private fun confirmAndExecuteSql(db: SQLiteDatabase, sql: String) {
        val ctx = requireContext()
        AlertDialog.Builder(ctx).setTitle("确定要执行该语句吗？").setMessage(sql).setPositiveButton("确定") { _, _ ->
            val waitDialog = AlertDialog.Builder(ctx).setTitle("请稍候").setMessage(sql).setCancelable(false).show()
            async {
                try {
                    db.execSQL(sql)
                    runOnUiThread {
                        mTableListCacheNeedInvalidate = true
                        Toasts.success(ctx, "操作完成")
                        updateStatus()
                    }
                } catch (e: Exception) {
                    runOnUiThread { FaultyDialog.show(ctx, e) }
                } finally {
                    runOnUiThread { waitDialog.dismiss() }
                }
            }
        }.setCancelable(true).setNegativeButton("取消", null).show()
    }

    @NonUiThread
    private fun getTableSize(db: SQLiteDatabase, table: String): Long {
        SyncUtils.requiresNonUiThread();
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

    override fun onDestroy() {
        mDatabase?.close()
        mDatabase = null
        super.onDestroy()
    }

    companion object {

        const val KEY_TARGET_DATABASE_PATH = "target_database_path"

        private const val CATEGORY_FRIENDS = 2
        private const val CATEGORY_TROOPS = 4
        private const val CATEGORY_GUILDS = 8
        private const val CATEGORY_TROOP_FILE_TRANSFER_ITEM = 16
        private const val CATEGORY_QQ_CALL = 32
        private const val CATEGORY_OTHERS = 256
        private const val CATEGORY_MASK = (CATEGORY_FRIENDS or CATEGORY_TROOPS or CATEGORY_GUILDS or
            CATEGORY_TROOP_FILE_TRANSFER_ITEM or CATEGORY_QQ_CALL or CATEGORY_OTHERS)

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
