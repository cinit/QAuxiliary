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

package io.github.duzhaokun123.util

import android.app.Activity
import android.content.Intent
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.get
import androidx.core.view.isVisible
import com.github.kyuubiran.ezxhelper.utils.findViewByIdName
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import xyz.nextalone.util.method
import java.io.File

object AioChatPieClipPasteHookUtils {
    var data: ByteArray? = null

    /**
     * @return true 已处理
     */
    @JvmStatic
    fun preHandleSendUriPicturePrConfirmSend(aioRootView: ViewGroup, data: ByteArray): Boolean {
        runCatching {
            val send_btn = aioRootView.findViewByIdName("send_btn")!!
            val parent = send_btn.parent as ViewGroup
            val ll = parent[(parent).indexOfChild(send_btn) + 1] as LinearLayout
            if (ll.isVisible.not()) return false
            val iv_image = ll[0] as ImageView
            this.data = data
            iv_image.callOnClick()
        }.onFailure {
            return false
        }
        return true
    }

    @JvmStatic
    fun preInitOnce() {
        "Lcom/tencent/qqnt/qbasealbum/WinkHomeActivity;->onCreate(Landroid/os/Bundle;)V".method
            .hookAfter {
                if (data == null) return@hookAfter

                val activity = it.thisObject as Activity
                val cacheFile = CacheManager.createTempFile("paste", ".png")
                cacheFile.writeBytes(data!!)
                data = null
                val albumResult = AlbumResultUtils.create(cacheFile.path)
                val result = Intent().apply {
                    putExtra("ALBUM_RESULT", albumResult)
                }
                activity.setResult(Activity.RESULT_OK, result)
                activity.finish()
            }
    }
}