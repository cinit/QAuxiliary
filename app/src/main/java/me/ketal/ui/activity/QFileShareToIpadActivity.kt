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

package me.ketal.ui.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.os.Bundle
import io.github.qauxv.util.PackageConstants

class QFileShareToIpadActivity : Activity() {
    companion object {
        const val SEND_TO_IPAD_CMD = "send_to_iPad_cmd"
        const val ENABLE_SEND_TO_IPAD = "enable_send_to_iPad"
        const val ENABLE_SEND_TO_IPAD_STATUS = "enable_send_to_iPad_status"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = PackageConstants.PACKAGE_NAME_QQ
        intent.apply {
            putExtra("targetUin", "9962")
            putExtra("device_type", 1)
            component = ComponentName(pkg, "com.tencent.mobileqq.activity.qfileJumpActivity")
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            AlertDialog.Builder(this).setTitle("出错啦")
                .setMessage("拉起QQ分享失败, 请确认 $pkg 已安装并启用(没有被关冰箱或被冻结停用)\n$e")
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    override fun onStart() {
        super.onStart()
        setVisible(true)
    }
}
