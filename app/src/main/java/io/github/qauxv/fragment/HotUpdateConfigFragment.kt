/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package io.github.qauxv.fragment

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import cc.ioctl.util.ui.ThemeAttrUtils
import io.github.qauxv.R
import io.github.qauxv.databinding.FragmentHotUpdateConfigBinding
import io.github.qauxv.util.hotupdate.HotUpdateManager

class HotUpdateConfigFragment : BaseRootLayoutFragment(), View.OnClickListener {

    private var binding: FragmentHotUpdateConfigBinding? = null

    private val channelIdToViewId = mapOf(
        HotUpdateManager.CHANNEL_DISABLED to R.id.hotUpdateConfig_channel_disabled,
        HotUpdateManager.CHANNEL_STABLE to R.id.hotUpdateConfig_channel_stable,
        HotUpdateManager.CHANNEL_BETA to R.id.hotUpdateConfig_channel_beta,
        HotUpdateManager.CHANNEL_CANARY to R.id.hotUpdateConfig_channel_canary,
    )

    private val actionIdToViewId = mapOf(
        HotUpdateManager.ACTION_DISABLE to R.id.hotUpdateConfig_action_disabled,
        HotUpdateManager.ACTION_QUERY to R.id.hotUpdateConfig_action_query_before_update,
        HotUpdateManager.ACTION_AUTO_UPDATE_WITH_NOTIFICATION to R.id.hotUpdateConfig_notice_after_update,
        HotUpdateManager.ACTION_AUTO_UPDATE_WITHOUT_NOTIFICATION to R.id.hotUpdateConfig_auto_update_without_notice,
    )

    private fun viewIdToChannelId(viewId: Int) = channelIdToViewId.filterValues { it == viewId }.keys.first()

    private fun viewIdToActionId(viewId: Int) = actionIdToViewId.filterValues { it == viewId }.keys.first()

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        title = "热更新配置"
        binding = FragmentHotUpdateConfigBinding.inflate(inflater, container, false).apply {
            hotUpdateConfigChannelDisabled.setOnClickListener(this@HotUpdateConfigFragment)
            hotUpdateConfigChannelStable.setOnClickListener(this@HotUpdateConfigFragment)
            hotUpdateConfigChannelBeta.setOnClickListener(this@HotUpdateConfigFragment)
            hotUpdateConfigChannelCanary.setOnClickListener(this@HotUpdateConfigFragment)

            hotUpdateConfigActionDisabled.setOnClickListener(this@HotUpdateConfigFragment)
            hotUpdateConfigActionQueryBeforeUpdate.setOnClickListener(this@HotUpdateConfigFragment)
            hotUpdateConfigNoticeAfterUpdate.setOnClickListener(this@HotUpdateConfigFragment)
            hotUpdateConfigAutoUpdateWithoutNotice.setOnClickListener(this@HotUpdateConfigFragment)

            updateViewStatus(this)

            fun adjustTitleTextSize(v: AppCompatTextView) {
                val text = v.text.toString()
                if (text.contains("\n")) {
                    val title = text.substringBefore("\n")
                    val subtitle = text.substringAfter("\n")
                    val ssb = SpannableStringBuilder().apply {
                        append(title)
                        append("\n")
                        append(subtitle)
                        // title 1.1
                        setSpan(RelativeSizeSpan(1.1f), 0, title.length, 0)
                        // body 0.9
                        setSpan(RelativeSizeSpan(0.9f), title.length + 1, length, 0)
                    }
                    v.text = ssb
                }
            }

            adjustTitleTextSize(hotUpdateConfigChannelDisabled)
            adjustTitleTextSize(hotUpdateConfigChannelStable)
            adjustTitleTextSize(hotUpdateConfigChannelBeta)
            adjustTitleTextSize(hotUpdateConfigChannelCanary)

            adjustTitleTextSize(hotUpdateConfigActionDisabled)
            adjustTitleTextSize(hotUpdateConfigActionQueryBeforeUpdate)
            adjustTitleTextSize(hotUpdateConfigNoticeAfterUpdate)
            adjustTitleTextSize(hotUpdateConfigAutoUpdateWithoutNotice)
        }
        rootLayoutView = binding!!.root
        return binding!!.root
    }

    override fun onResume() {
        super.onResume()
        binding?.let { updateViewStatus(it) }
    }

    private fun updateViewStatus(binding: FragmentHotUpdateConfigBinding) {
        val ctx = requireContext()
        val currentChannel = HotUpdateManager.currentChannel
        val currentAction = HotUpdateManager.currentAction

        binding.hotUpdateConfigCurrentInfo.text = "别看了，这个功能还没做好，选什么都没用"

        val channelButtons = arrayOf(
            binding.hotUpdateConfigChannelDisabled,
            binding.hotUpdateConfigChannelStable,
            binding.hotUpdateConfigChannelBeta,
            binding.hotUpdateConfigChannelCanary,
        )
        val actionButtons = arrayOf(
            binding.hotUpdateConfigActionDisabled,
            binding.hotUpdateConfigActionQueryBeforeUpdate,
            binding.hotUpdateConfigNoticeAfterUpdate,
            binding.hotUpdateConfigAutoUpdateWithoutNotice,
        )
        val accentColor = ThemeAttrUtils.resolveColorOrDefaultColorRes(
            ctx,
            androidx.appcompat.R.attr.colorAccent,
            R.color.colorAccent
        )
        val secondTextColor = ctx.getColor(R.color.secondTextColor)
        channelButtons.forEach {
            val channelId = viewIdToChannelId(it.id)
            if (currentChannel == channelId) {
                it.setTextColor(accentColor)
                if (it.compoundDrawables[2] == null) {
                    it.setCompoundDrawablesWithIntrinsicBounds(
                        null, null, ResourcesCompat.getDrawable(
                            ctx.resources, R.drawable.ic_check_24, ctx.theme
                        ), null
                    )
                }
            } else {
                it.setCompoundDrawables(null, null, null, null)
                it.setTextColor(secondTextColor)
            }
        }
        actionButtons.forEach {
            val actionId = viewIdToActionId(it.id)
            if (currentAction == actionId) {
                it.setTextColor(accentColor)
                if (it.compoundDrawables[2] == null) {
                    it.setCompoundDrawablesWithIntrinsicBounds(
                        null, null, ResourcesCompat.getDrawable(
                            ctx.resources, R.drawable.ic_check_24, ctx.theme
                        ), null
                    )
                }
            } else {
                it.setCompoundDrawables(null, null, null, null)
                it.setTextColor(secondTextColor)
            }
        }
    }

    private fun onChannelClick(v: View, channelId: Int) {
        if (HotUpdateManager.currentChannel == channelId) {
            return
        }
        val fn = {
            HotUpdateManager.currentChannel = channelId
            updateViewStatus(binding!!)
        }
        if (channelId == HotUpdateManager.CHANNEL_BETA) {
            AlertDialog.Builder(requireContext())
                .setTitle("警告")
                .setMessage("您确定要切换到 Beta 测试频道吗？\n这个频道的更新可能不稳定，可能会导致应用崩溃。")
                .setPositiveButton("确定") { _, _ -> fn() }
                .setNegativeButton("取消", null)
                .show()
        } else if (channelId == HotUpdateManager.CHANNEL_CANARY) {
            AlertDialog.Builder(requireContext())
                .setTitle("严重警告")
                .setMessage(
                    "您确定要切换到 Canary 测试频道吗？\n" +
                        "Canary 频道的更新是由开发者的代码提交自动触发的，仅供开发者测试使用。其代码未经任何审查和测试，因此很不安全。\n" +
                        "如果您使用 Canary 频道，可能会导致应用崩溃、数据丢失，甚至信息泄漏、设备被入侵、永久的硬件损伤等严重后果。\n" +
                        "请不要在生产环境中使用 Canary 频道。\n" +
                        "如果您不知道这是什么，那么请不要使用 Canary 频道。\n" +
                        "如果您使用 Canary 频道，您将自行承担所有风险。"
                )
                .setPositiveButton("确定") { _, _ -> fn() }
                .setNegativeButton("取消", null)
                .show()
        } else {
            fn()
        }
    }

    private fun onActionClick(v: View, actionId: Int) {
        if (HotUpdateManager.currentAction == actionId) {
            return
        }
        HotUpdateManager.currentAction = actionId
        updateViewStatus(binding!!)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.hotUpdateConfig_channel_disabled -> onChannelClick(v, HotUpdateManager.CHANNEL_DISABLED)
            R.id.hotUpdateConfig_channel_stable -> onChannelClick(v, HotUpdateManager.CHANNEL_STABLE)
            R.id.hotUpdateConfig_channel_beta -> onChannelClick(v, HotUpdateManager.CHANNEL_BETA)
            R.id.hotUpdateConfig_channel_canary -> onChannelClick(v, HotUpdateManager.CHANNEL_CANARY)

            R.id.hotUpdateConfig_action_disabled -> onActionClick(v, HotUpdateManager.ACTION_DISABLE)
            R.id.hotUpdateConfig_action_query_before_update -> onActionClick(v, HotUpdateManager.ACTION_QUERY)
            R.id.hotUpdateConfig_notice_after_update -> onActionClick(v, HotUpdateManager.ACTION_AUTO_UPDATE_WITH_NOTIFICATION)
            R.id.hotUpdateConfig_auto_update_without_notice -> onActionClick(v, HotUpdateManager.ACTION_AUTO_UPDATE_WITHOUT_NOTIFICATION)
        }
    }

}
