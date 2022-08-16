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

import android.annotation.SuppressLint
import android.content.Context
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
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import cc.ioctl.hook.OpenFriendChatHistory
import cc.ioctl.hook.OpenProfileCard
import cc.ioctl.util.ExfriendManager
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.TroopManagerHelper
import cc.ioctl.util.ui.FaultyDialog
import io.github.qauxv.R
import io.github.qauxv.SyncUtils
import io.github.qauxv.SyncUtils.async
import io.github.qauxv.SyncUtils.runOnUiThread
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.fragment.BaseRootLayoutFragment
import io.github.qauxv.util.Log
import io.github.qauxv.util.NonUiThread
import io.github.qauxv.util.Toasts
import org.json.JSONObject
import xyz.nextalone.util.SystemServiceUtils
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class DatabaseShrinkFragment : BaseRootLayoutFragment() {

    private var mCurrentUin: Long = 0
    private var mRecyclerView: RecyclerView? = null
    private val mTextItemList = ArrayList<SpannableStringBuilder>()
    private var mDatabase: SQLiteDatabase? = null
    private var mTableSizeLongOrErrorString = HashMap<String, Any>(16)

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
        mRecyclerView = RecyclerView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(ctx)
            adapter = mAdapter
        }
        rootLayoutView = mRecyclerView
        val uin = AppRuntimeHelper.getLongAccountUin()
        mCurrentUin = uin
        loadUinMd5()
        return rootLayoutView!!
    }

    class TextViewHolder(ctx: Context) : RecyclerView.ViewHolder(TextView(ctx)) {
        init {
            val tv = itemView as TextView
            tv.apply {
                textSize = 14f
                typeface = Typeface.MONOSPACE
                setTextIsSelectable(true)
                setTextColor(ResourcesCompat.getColor(resources, R.color.firstTextColor, ctx.theme))
                val dp8 = LayoutHelper.dip2px(ctx, 8f)
                setPadding(dp8, dp8, dp8, dp8)
                movementMethod = LinkMovementMethod.getInstance()
                this.tag = this
            }
        }

        val textView: TextView get() = itemView as TextView
    }

    private val mAdapter = object : RecyclerView.Adapter<TextViewHolder>() {

        private val NO_ITEMS: SpannableStringBuilder by lazy {
            SpannableStringBuilder("No items")
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextViewHolder {
            return TextViewHolder(parent.context)
        }

        override fun onBindViewHolder(holder: TextViewHolder, position: Int) {
            if (mTextItemList.size == 0) {
                holder.textView.text = NO_ITEMS
            } else {
                holder.textView.text = mTextItemList[position]
            }
        }

        override fun getItemCount(): Int {
            return Math.max(mTextItemList.size, 1)
        }
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
            if (mTableSizeLongOrErrorString[table] == null) {
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
        val specialUin = arrayOf("9999", "9987", "9986", "9915")
        for (uin in specialUin) {
            val md5 = uinToMd5(uin)
            mMd5ToUinLut[md5] = uin
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    @SuppressLint("NotifyDataSetChanged")
    @UiThread
    private fun updateStatus() {
        val ctx = requireContext()
        mTextItemList.clear()
        if (mDatabasePath == null) {
            val sb = SpannableStringBuilder()
            sb.append("mDatabasePath is null")
            mTextItemList.add(sb)
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
                    mTextItemList.add(SpannableStringBuilder(Log.getStackTraceString(e)))
                    Log.e(e)
                }
            }
            mDatabase?.let { database ->
                try {
                    val databaseTables = ArrayList<String>()
                    if (mTableListCacheNeedInvalidate) {
                        database.rawQuery("select name from sqlite_master where type=\"table\"", null).use {
                            while (it.moveToNext()) {
                                val name = it.getString(0)
                                if (name != "sqlite_sequence" && name != "android_metadata") {
                                    databaseTables.add(name)
                                }
                            }
                        }
                        mTableListCacheNeedInvalidate = false
                        mTableListCache = databaseTables
                    } else {
                        databaseTables.addAll(mTableListCache)
                    }
                    val tableNames: ArrayList<String> = ArrayList(getShowingTables())
                    // sort with COUNT(*), DESC
                    tableNames.sortWith { o1, o2 ->
                        val r1: Any? = mTableSizeLongOrErrorString[o1]
                        val r2: Any? = mTableSizeLongOrErrorString[o2]
                        val count1: Long = if (r1 == null) {
                            Int.MAX_VALUE.toLong() - 2L
                        } else {
                            if (r1 is Long) {
                                r1
                            } else {
                                Int.MAX_VALUE.toLong()
                            }
                        }
                        val count2: Long = if (r2 == null) {
                            Int.MAX_VALUE.toLong() - 2L
                        } else {
                            if (r2 is Long) {
                                r2
                            } else {
                                Int.MAX_VALUE.toLong()
                            }
                        }
                        if (count1 > count2) {
                            -1
                        } else if (count1 < count2) {
                            1
                        } else {
                            0
                        }
                    }
                    for (table: String in tableNames) {
                        val sb = SpannableStringBuilder()
                        mTextItemList.add(sb)
                        val v: Any? = mTableSizeLongOrErrorString[table]
                        val sizeDesc: String = if (v is Long) {
                            "COUNT(*)=${v}"
                        } else {
                            v?.toString() ?: "COUNT(*)=???"
                        }
                        sb.apply {
                            val parts = table.split("_")
                            var shouldShowFriendChatHistory: Long = 0
                            if (parts[0] == "mr" && parts.size >= 3) {
                                val md5 = parts[2]
                                val uin = mMd5ToUinLut[md5]
                                if (uin != null) {
                                    append(table)
                                    append("\n")
                                    if (parts[1] == "troop") {
                                        appendClickable(uin, clickToOpenTroopProfile(uin))
                                        mTroopName[uin]?.let {
                                            append(" // ")
                                            append(it)
                                        }
                                    } else if (parts[1] == "friend") {
                                        appendClickable(uin, clickToOpenFriendProfile(uin))
                                        mFriendName[uin]?.let {
                                            append(" // ")
                                            append(it)
                                        }
                                        shouldShowFriendChatHistory = uin.toLong()
                                    } else {
                                        append(uin)
                                    }
                                } else {
                                    if (md5.matches("[0-9a-fA-F]{32}".toRegex())) {
                                        // add a link to search md5
                                        append("mr_")
                                        append(parts[1])
                                        append("_")
                                        appendClickable(md5, clickToSearchMd5Online(md5))
                                        for (i in 3 until parts.size) {
                                            append("_")
                                            append(parts[i])
                                        }
                                    }
                                }
                            } else {
                                append(table)
                            }
                            append("\n")
                            append(sizeDesc)
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
                            if (shouldShowFriendChatHistory >= 10000) {
                                append(" | ")
                                appendClickable("VIEW") {
                                    OpenFriendChatHistory.startFriendChatHistoryActivity(ctx, shouldShowFriendChatHistory)
                                }
                            }
                            append(" ]")
                        }
                    }
                } catch (e: Exception) {
                    mTextItemList.add(SpannableStringBuilder(Log.getStackTraceString(e)))
                    Log.e(e)
                }
            }
            // we actually don't know what has changed, so we need to call notifyDataSetChanged()
            mAdapter.notifyDataSetChanged()
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
            mTableSizeLongOrErrorString[table] = java.lang.Long.valueOf(size)
            size
        } catch (e: Exception) {
            mTableSizeLongOrErrorString[table] = e.toString()
            -1L
        }
    }

    override fun onDestroy() {
        mDatabase?.close()
        mDatabase = null
        super.onDestroy()
    }

    private fun clickToCopyText(text: String): (View) -> Unit = {
        SystemServiceUtils.copyToClipboard(it.context, text)
    }

    private fun clickToSearchMd5Online(md5: String): (View) -> Unit {
        return lambda@{
            if (!md5.matches("[a-fA-F0-9]{32}".toRegex())) {
                return@lambda
            }
            val ctx = requireContext()
            val waitDialog = AlertDialog.Builder(ctx)
                .setTitle("正在查询")
                .setMessage("$md5\n通常不会超过一分钟")
                .setCancelable(false)
                .setNegativeButton("取消", null)
                .show()
            async {
                try {
                    val conn = URL("https://api.ioctl.cc/rpc/a4687597-5ef4-4f55-9626-3086728692fb/numeric10md5lookup_test")
                        .openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.doInput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Accept", "application/json")
                    conn.connect()
                    val out = conn.outputStream
                    val writer = BufferedWriter(OutputStreamWriter(out, "UTF-8"))
                    writer.write("{\"h\":\"$md5\"}")
                    writer.flush()
                    writer.close()
                    val inp = conn.inputStream
                    val reader = BufferedReader(InputStreamReader(inp, "UTF-8"))
                    val respBuffer = StringBuilder()
                    var line: String
                    while (true) {
                        line = reader.readLine() ?: break
                        respBuffer.append(line)
                    }
                    reader.close()
                    inp.close()
                    conn.disconnect()
                    val resp = respBuffer.toString()
                    val json = JSONObject(resp.toString())
                    val code = json.getInt("code")
                    if (code == 0) {
                        val plain = json.getString("result")
                        // check if the md5 is correct
                        val testMd5 = uinToMd5(plain)
                        if (testMd5 != md5) {
                            throw IOException("md5 not match, got result=$plain, expected=$md5, testMd5=$testMd5")
                        }
                        mMd5ToUinLut[md5] = plain
                    } else {
                        runOnUiThread {
                            AlertDialog.Builder(ctx).apply {
                                setTitle("查询失败")
                                setMessage("$md5\n$resp")
                                setNeutralButton("复制 MD5") { _, _ ->
                                    SystemServiceUtils.copyToClipboard(ctx, md5)
                                }
                                setPositiveButton("确定", null)
                                show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        FaultyDialog.show(ctx, "查询失败", e.toString())
                    }
                } finally {
                    runOnUiThread {
                        waitDialog.dismiss()
                        updateStatus()
                    }
                }
            }
        }
    }

    companion object {

        const val KEY_TARGET_DATABASE_PATH = "target_database_path"

        private const val CATEGORY_FRIENDS = 2
        private const val CATEGORY_TROOPS = 4
        private const val CATEGORY_GUILDS = 8
        private const val CATEGORY_TROOP_FILE_TRANSFER_ITEM = 16
        private const val CATEGORY_QQ_CALL = 32
        private const val CATEGORY_OTHERS = 256
        private const val CATEGORY_MASK =
            (CATEGORY_FRIENDS or CATEGORY_TROOPS or CATEGORY_GUILDS or CATEGORY_TROOP_FILE_TRANSFER_ITEM or CATEGORY_QQ_CALL or CATEGORY_OTHERS)

        @JvmStatic
        private fun clickToOpenFriendProfile(uin: String): ((View) -> Unit) {
            val longUin = try {
                uin.toLong()
            } catch (e: Exception) {
                -1L
            }
            if (longUin < 10000) {
                return {}
            } else {
                return { OpenProfileCard.openUserProfileCard(it.context, uin.toLong()) }
            }
        }

        @JvmStatic
        private fun clickToOpenTroopProfile(uin: String): ((View) -> Unit) {
            val longUin = try {
                uin.toLong()
            } catch (e: Exception) {
                -1L
            }
            if (longUin < 10000) {
                return {}
            } else {
                return { OpenProfileCard.openTroopProfileActivity(it.context, uin) }
            }
        }

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
    }
}
