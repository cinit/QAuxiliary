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

package xyz.nextalone.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.net.URLConnection

object SystemServiceUtils {
    /**
     * Copy text to system clipboard
     *
     * @param context [Context]
     * @param text    text will be copied.
     */
    @JvmStatic
    fun copyToClipboard(context: Context, text: CharSequence) {
        if (text.isEmpty()) {
            return
        }
        val clipData = ClipData.newPlainText("", text)
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(clipData)
    }

    /**
     * Copy file to system clipboard
     *
     * @param context [Context]
     * @param file    [File]
     */
    @JvmStatic
    fun copyToClipboard(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        copyToClipboard(context, uri)
    }

    /**
     * Copy uri to system clipboard
     *
     * @param context [Context]
     * @param uri     [Uri]
     */
    @JvmStatic
    fun copyToClipboard(context: Context, uri: Uri) {
        val item = ClipData.Item(uri)
        val mimeType = context.contentResolver.openInputStream(uri)?.buffered().use {
            URLConnection.guessContentTypeFromStream(it)
        }
        val clipData = ClipData("", arrayOf(mimeType), item)
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(clipData)
    }
}
