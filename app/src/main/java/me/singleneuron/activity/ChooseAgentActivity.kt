/*
 * QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 dmca@ioctl.cc
 * https://github.com/ferredoxin/QNotified
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/ferredoxin/QNotified/blob/master/LICENSE.md>.
 */
package me.singleneuron.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.content.Intent.ACTION_PICK
import android.content.Intent.EXTRA_ALLOW_MULTIPLE
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import cc.ioctl.util.Reflex
import io.github.qauxv.R
import io.github.qauxv.util.Log
import io.github.qauxv.util.hostInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.singleneuron.base.AbstractChooseActivity
import java.util.Timer
import kotlin.concurrent.schedule

class ChooseAgentActivity : AbstractChooseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.NoDisplay)
        super.onCreate(savedInstanceState)
        bundle = intent.extras
        val intent = if (intent.getBooleanExtra("use_ACTION_PICK", false))
            Intent(ACTION_PICK).apply {
                type = "image/*"
                putExtra(EXTRA_ALLOW_MULTIPLE, true)
            }
        else Intent(ACTION_GET_CONTENT).apply {
            type = intent.type ?: "*/*"
            putExtra(EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK &&
                        (data?.data != null || data?.clipData != null)
                    ) {
                        initSendCacheDir()
                        val uri = data.data
                        val clip = data.clipData
                        if (uri != null) {
                            // Only one file chosen
                            convertUriToPath(uri)?.let {
                                sendAFile(it, data)
                            }
                        } else if (clip != null) {
                            // multiple file chosen
                            convertClipDataToPath(clip).let {
                                var delayTime: Long = 0
                                for (i in it) {
                                    Timer().schedule(delayTime) {
                                        sendAFile(i, data)
                                    }
                                    delayTime += 1000
                                }
                            }
                        }
                    }
                    finish()
                } catch (e: Exception) {
                    runOnUiThread {
                        AlertDialog.Builder(this@ChooseAgentActivity)
                            .setTitle(Reflex.getShortClassName(e))
                            .setMessage(Log.getStackTraceString(e))
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok) { _, _ -> this@ChooseAgentActivity.finish() }
                            .show()
                    }
                    Log.e(e)
                }
            }
        }
    }

    @SuppressLint("WrongConstant")
    fun sendAFile(filePath: String, data: Intent) {
        val intent = Intent().apply {
            component = packageManager.getLaunchIntentForPackage(hostInfo.packageName)!!.component
            flags = 0x14000000
            putExtras(data)
            putExtra("forward_from_jump", true)
            putExtra("preAct", "JumpActivity")
            putExtra("miniAppShareFrom", 0)
            putExtra("system_share", true)
            putExtra("leftBackText", "消息")
            putExtra("task_launched_for_result", true)
            putExtra("isFromShare", true)
            putExtra("needShareCallBack", false)
            putExtra("key_forward_ability_type", 0)
            putExtra("moInputType", 2)
            putExtra("chooseFriendFrom", 1)
            putExtra("forward_source_business_type", -1)
            if (intent.type == "image/*") {
                putExtra("forward_type", 1)
            } else {
                putExtra("forward_type", 0)
            }
            bundle?.let {
                val uin = it.getString("targetUin") ?: it.getLong("key_peerUin").toString()
                putExtra("uin", uin)
                val type = it.getInt("peerType", it.getInt("key_chat_type", 0) - 1)
                putExtra("uintype", type)
                putExtras(it)
            }
            putExtra("selection_mode", 2)
            putExtra("sendMultiple", false)
            putExtra("isBack2Root", true)
            putExtra("open_chatfragment", true)
            putExtra("forward_filepath", filePath)
        }
        Log.d("Start send file: $filePath")
        startActivity(intent)
    }
}
