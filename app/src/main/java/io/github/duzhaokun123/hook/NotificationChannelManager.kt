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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.updatePadding
import com.tencent.mobileqq.widget.BounceScrollView
import io.github.qauxv.activity.SettingsUiFragmentHostActivity
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.fragment.BaseRootLayoutFragment
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.util.SyncUtils

@FunctionHookEntry
@UiItemAgentEntry
object NotificationChannelManager : CommonConfigFunctionHook(targetProc = SyncUtils.PROC_MAIN) {
    override val name = "通知渠道管理"
    override val description = "管理应用内的通知渠道"
    override val valueState = null
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MISC_CATEGORY

    override fun initOnce(): Boolean {
        return true
    }

    override val onUiItemClickListener = { _: IUiItemAgent, activity: Activity, _: View ->
        val settingActivity = activity as SettingsUiFragmentHostActivity
        settingActivity.presentFragment(NotificationChannelManagerFragment())
    }
}

class NotificationChannelManagerFragment : BaseRootLayoutFragment() {

    @SuppressLint("SetTextI18n")
    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        title = "通知渠道管理"
        val context = requireContext()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return TextView(context).apply { text = "系统不支持" }
        }
        val notificationManager = NotificationManagerCompat.from(context)
        val rootView = BounceScrollView(context, null)
        rootLayoutView = rootView
        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        rootView.addView(linearLayout, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        notificationManager.notificationChannelGroupsCompat.forEach { group ->
            val ll_group = LinearLayout(context).apply ll_group@{
                orientation = LinearLayout.VERTICAL
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(Button(context).apply {
                        text = "删除"
                        setOnClickListener {
                            AlertDialog.Builder(context).setTitle("删除渠道组")
                                .setMessage("确认删除渠道组 ${group.name}(${group.id}) 及其所有子渠道吗？")
                                .setNegativeButton("取消", null)
                                .setPositiveButton("删除") { _, _ ->
                                    notificationManager.deleteNotificationChannelGroup(group.id)
                                    linearLayout.removeView(this@ll_group)
                                }.show()
                        }
                    }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
                    addView(TextView(context).apply {
                        text = "渠道组：${group.name}(${group.id})"
                        if (group.description != null) {
                            append("\n${group.description}")
                        }
                    }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        weight = 1F
                    })
                })
            }
            linearLayout.addView(ll_group)
            group.channels.forEach { channel ->
                ll_group.addView(createChannelView(channel, context, notificationManager, ll_group).apply {
                    updatePadding(left = 50)
                })
            }
        }
        notificationManager.notificationChannelsCompat.forEach { channel ->
            if (channel.group != null) {
                // 已经在组内的渠道跳过
                return@forEach
            }
            linearLayout.addView(createChannelView(channel, context, notificationManager, linearLayout))
        }
        return rootView
    }

    @SuppressLint("SetTextI18n")
    private fun createChannelView(
        channel: NotificationChannelCompat,
        context: android.content.Context,
        notificationManager: NotificationManagerCompat,
        parentLayout: LinearLayout
    ): LinearLayout {
        return LinearLayout(context).apply ll@{
            orientation = LinearLayout.HORIZONTAL
            addView(Button(context).apply {
                text = "删除"
                setOnClickListener {
                    AlertDialog.Builder(context).setTitle("删除渠道")
                        .setMessage("确认删除渠道 ${channel.name}(${channel.id}) 吗？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("删除") { _, _ ->
                            notificationManager.deleteNotificationChannel(channel.id)
                            parentLayout.removeView(this@ll)
                        }.show()
                }
            }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
            addView(TextView(context).apply {
                text = "渠道：${channel.name}(${channel.id})"
                if (channel.parentChannelId != null) {
                    append(" 父渠道：${channel.parentChannelId}")
                }
                if (channel.description != null) {
                    append("\n${channel.description}")
                }
            }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                weight = 1F
            })
        }
    }
}