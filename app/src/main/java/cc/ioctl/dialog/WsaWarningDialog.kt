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

package cc.ioctl.dialog

import android.content.Context
import androidx.appcompat.app.AlertDialog
import cc.ioctl.util.HostInfo
import io.github.qauxv.R
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.core.MainHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.UiThread

// in host process only
object WsaWarningDialog {

    private const val LATEST_WSA_WARNING_VERSION = 1

    private const val WSA_WARNING_DIALOG_TAG = "WsaWarningDialog.Version"

    private var mHasShownThisTime = false

    private var currentWsaWarningVersion: Int
        get() = ConfigManager.getDefaultConfig().getIntOrDefault(WSA_WARNING_DIALOG_TAG, 0)
        set(value) {
            ConfigManager.getDefaultConfig().putInt(WSA_WARNING_DIALOG_TAG, value)
        }

    private fun isNeedShow(): Boolean {
        return HostInfo.isInHostProcess()
            && MainHook.isWindowsSubsystemForAndroid()
            && currentWsaWarningVersion < LATEST_WSA_WARNING_VERSION
            && !mHasShownThisTime
    }

    @JvmStatic
    @UiThread
    fun showWsaWarningDialogIfNecessary(baseContext: Context) {
        if (!isNeedShow()) {
            return
        }
        mHasShownThisTime = true
        val ctx = CommonContextWrapper.createAppCompatContext(baseContext)
        AlertDialog.Builder(ctx).apply {
            setTitle(R.string.wsa_warning_dialog_title)
            setMessage(R.string.wsa_warning_dialog_message)
            setPositiveButton(android.R.string.ok, null)
            setNeutralButton(R.string.btn_do_not_show_again) { _, _ ->
                currentWsaWarningVersion = LATEST_WSA_WARNING_VERSION
            }
            setCancelable(true)
            show()
        }
    }

}
