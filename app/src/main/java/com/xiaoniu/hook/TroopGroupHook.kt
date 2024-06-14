/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package com.xiaoniu.hook

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.HostInfo
import com.github.kyuubiran.ezxhelper.utils.ArgTypes
import com.github.kyuubiran.ezxhelper.utils.Args
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.newInstance
import com.xiaoniu.util.ContextUtils
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.IoUtils
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.RecentPopup_onClickAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import top.linl.util.ScreenParamUtils
import xyz.nextalone.util.clazz
import xyz.nextalone.util.get
import xyz.nextalone.util.method
import xyz.nextalone.util.throwOrTrue
import java.io.File

@FunctionHookEntry
@UiItemAgentEntry
object TroopGroupHook : CommonSwitchFunctionHook(arrayOf(RecentPopup_onClickAction)) {
    override val name = "群聊分组"

    override val description = "在首页加号菜单中添加群聊分组功能"

    private fun context() = CommonContextWrapper.createMaterialDesignContext(ContextUtils.getCurrentActivity())

    private lateinit var adapter: GroupAdapter

    class GroupAdapter : RecyclerView.Adapter<GroupAdapter.MyViewHolder>() {
        private var editMode = false
        private var items = emptyList<GroupItemData>()

        class MyViewHolder(root: ViewGroup) : RecyclerView.ViewHolder(root) {
            val tv = root.getChildAt(0) as TextView
            val btnAdd = root.getChildAt(1) as Button
            val btnDelete = root.getChildAt(2) as Button
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val root = LinearLayout(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                orientation = LinearLayout.HORIZONTAL
                setPadding(ScreenParamUtils.dpToPx(context, 10F))
            }
            val tv = TextView(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                textSize = 16f
            }
            val btnAdd = Button(context()).apply {
                visibility = View.GONE
                text = "+"
            }
            val btnDelete = Button(context()).apply {
                visibility = View.GONE
                text = "×"
            }
            root.addView(tv)
            root.addView(btnAdd)
            root.addView(btnDelete)

            return MyViewHolder(root).apply {
                root.setOnClickListener {
                    showDetailDialog(items[bindingAdapterPosition])
                }
                btnAdd.setOnClickListener {
                    //todo 使用QQ自己的选择页面进行选择
                    val etName = EditText(context()).apply {
                        hint = "群聊名称"
                    }
                    val etUin = EditText(context()).apply {
                        hint = "群号"
                    }
                    val l = LinearLayout(context()).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(etName)
                        addView(etUin)
                    }
                    AlertDialog.Builder(context())
                        .setTitle("添加群至分组“${items[bindingAdapterPosition].name}”")
                        .setView(l)
                        .setNegativeButton("取消", null)
                        .setPositiveButton("确定") { _, _ ->
                            addTroopToGroup(items[bindingAdapterPosition].id, TroopInfo(etName.text.toString(), etUin.text.toString()))
                            adapter.refresh()
                        }
                        .show()
                }
                btnDelete.setOnClickListener {
                    AlertDialog.Builder(context())
                        .setTitle("删除分组")
                        .setMessage("确定删除分组“${items[bindingAdapterPosition].name}”吗？")
                        .setCancelable(false)
                        .setNegativeButton("取消", null)
                        .setPositiveButton("确定") { _, _ ->
                            deleteGroupItem(items[bindingAdapterPosition].id)
                            refresh()
                        }
                        .show()
                }
            }
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.tv.text = items[position].name
            holder.btnAdd.visibility = if (editMode) View.VISIBLE else View.GONE
            holder.btnDelete.visibility = if (editMode) View.VISIBLE else View.GONE
        }

        @SuppressLint("NotifyDataSetChanged")
        fun refresh() {
            items = getGroupItems().sortedBy { it.position }
            notifyDataSetChanged()
        }

        @SuppressLint("NotifyDataSetChanged")
        fun setEditMode(mode: Boolean) {
            editMode = mode
            notifyDataSetChanged()
        }

    }

    private val mainDialog = {
        val recyclerView = RecyclerView(context()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutManager = LinearLayoutManager(context)
            this@TroopGroupHook.adapter = GroupAdapter().apply {
                refresh()
            }
            adapter = this@TroopGroupHook.adapter
        }
        val layoutManageBtns = LinearLayout(context()).apply {
            visibility = View.GONE
        }
        val btnGroupManage = Button(context()).apply {
            text = "分组管理"
            setOnClickListener {
                adapter.setEditMode(true)
                visibility = View.GONE
                layoutManageBtns.visibility = View.VISIBLE
            }
        }
        val btnAddGroup = Button(context()).apply {
            text = "添加分组"
            setOnClickListener {
                val et = EditText(context)
                AlertDialog.Builder(context)
                    .setTitle("分组名称")
                    .setView(et)
                    .setCancelable(false)
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定") { _, _ ->
                        if (et.text.isEmpty()) return@setPositiveButton
                        addGroupItem(et.text.toString())
                        adapter.refresh()
                    }
                    .show()
            }
        }
        val btnExitManage = Button(context()).apply {
            text = "退出管理"
            setOnClickListener {
                adapter.setEditMode(false)
                layoutManageBtns.visibility = View.GONE
                btnGroupManage.visibility = View.VISIBLE
            }
        }
        layoutManageBtns.addView(btnExitManage)
        layoutManageBtns.addView(btnAddGroup)

        val mainView = LinearLayout(context()).apply {
            orientation = LinearLayout.VERTICAL
            addView(btnGroupManage)
            addView(layoutManageBtns)
            addView(recyclerView)
        }
        AlertDialog.Builder(context()).apply {
            setTitle("群聊分组")
            setView(mainView)
            setPositiveButton("确定", null)
        }.create()
    }

    private fun showDetailDialog(group: GroupItemData) {
        val recyclerView = RecyclerView(context()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutManager = LinearLayoutManager(context)
            adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val tv = TextView(parent.context).apply {
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        textSize = 16f
                        setPadding(ScreenParamUtils.dpToPx(context, 10F))
                    }
                    return object : RecyclerView.ViewHolder(tv) {}.apply {
                        tv.setOnClickListener {
                            val clz = Class.forName("com.tencent.mobileqq.activity.ChatActivity")
                            val intent = Intent(context, clz).apply {
                                putExtra("uinname", group.troops[bindingAdapterPosition].name)
                                putExtra("uin", group.troops[bindingAdapterPosition].uin)
                                putExtra("uintype", 1)
                                putExtra("key_from", "17")
                            }
                            context.startActivity(intent)
                        }
                        tv.setOnLongClickListener {
                            AlertDialog.Builder(context)
                                .setTitle("删除")
                                .setMessage("确定删除“${tv.text}”吗？")
                                .setNegativeButton("取消", null)
                                .setPositiveButton("确定") { _, _ ->
                                    deleteTroopFromGroup(group.id, group.troops[bindingAdapterPosition])
                                    group.troops.removeAt(bindingAdapterPosition)
                                    adapter?.notifyItemRemoved(bindingAdapterPosition)
                                }
                                .show()
                            true
                        }
                    }
                }

                override fun getItemCount() = group.troops.size

                @SuppressLint("SetTextI18n")
                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    (holder.itemView as TextView).text = "${group.troops[position].name}(${group.troops[position].uin})"
                }
            }
        }
        AlertDialog.Builder(context()).apply {
            setTitle(group.name)
            setView(recyclerView)
            setPositiveButton("确定", null)
        }.show()
    }

    @Serializable
    data class GroupItemData(val id: Int, val name: String, val position: Int, val troops: MutableList<TroopInfo>)

    @Serializable
    data class TroopInfo(val name: String, val uin: String)

    private fun getGroupItems(): List<GroupItemData> {
        val file = File(HostInfo.getApplication().filesDir, "qa_misc" + File.separator + "group.json")
        if (file.exists()) {
            file.readText().let {
                return try {
                    Json.decodeFromString(it)
                } catch (e: Exception) {
                    traceError(e)
                    Toasts.error(context(), "读取群聊分组失败")
                    emptyList()
                }
            }
        } else {
            return emptyList()
        }
    }

    private fun writeGroupItems(list: List<GroupItemData>) {
        val json = Json.encodeToString(list)
        File(IoUtils.mkdirsOrThrow(File(HostInfo.getApplication().filesDir, "qa_misc")), "group.json")
            .writeText(json)
    }

    private fun addGroupItem(name: String) {
        val list = getGroupItems().toMutableList()
        list.add(GroupItemData(list.size + 1, name, list.size, mutableListOf()))
        writeGroupItems(list)
    }

    private fun deleteGroupItem(id: Int) {
        val list = getGroupItems().toMutableList()
        list.removeIf { it.id == id }
        writeGroupItems(list)
    }

    private fun addTroopToGroup(groupId: Int, troop: TroopInfo) {
        val list = getGroupItems().toMutableList()
        list.firstOrNull { it.id == groupId }?.troops?.add(troop)
        writeGroupItems(list)
    }

    private fun deleteTroopFromGroup(groupId: Int, troop: TroopInfo) {
        val list = getGroupItems().toMutableList()
        list.firstOrNull { it.id == groupId }?.troops?.remove(troop)
        writeGroupItems(list)
    }


    override fun initOnce() = throwOrTrue {
        val itemClazz = "Lcom/tencent/widget/PopupMenuDialog\$MenuItem;".clazz!!
        val entryItem = itemClazz.newInstance(
            Args(arrayOf(415411, "群聊分组", "群聊分组", R.drawable.ic_troop_group)),
            ArgTypes(arrayOf(Int::class.java, String::class.java, String::class.java, Int::class.java))
        )!!
        "Lcom/tencent/widget/PopupMenuDialog;->conversationPlusBuild(Landroid/app/Activity;Ljava/util/List;Lcom/tencent/widget/PopupMenuDialog\$OnClickActionListener;Lcom/tencent/widget/PopupMenuDialog\$OnDismissListener;)Lcom/tencent/widget/PopupMenuDialog;".method.hookBefore {
            val list = it.args[1] as MutableList<Any>
            list.add(0, entryItem)
        }
        DexKit.requireMethodFromCache(RecentPopup_onClickAction).hookBefore {
            if (it.args[0].get("id") == 415411) {
                mainDialog.invoke().show()
                it.result = null
            }
        }
    }

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.GROUP_CATEGORY

    override val isAvailable = QAppUtils.isQQnt()
}