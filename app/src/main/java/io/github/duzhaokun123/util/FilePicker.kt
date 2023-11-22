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

package io.github.duzhaokun123.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setPadding
import com.github.kyuubiran.ezxhelper.utils.showToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.qauxv.R
import java.io.File

object FilePicker {
    private var dialog: AlertDialog? = null

    @SuppressLint("SetTextI18n")
    fun pick(context: Context, message: String, path: String, dorOnly: Boolean, onPick: (String) -> Unit) {
        val file = File(path)
        var entries = file.listFiles()?.toList()
        if (dorOnly) entries = entries?.filter { it.isDirectory }
        entries = entries?.sorted()
        val item = listOf("..") + (entries?.map { it.name } ?: if (file.isFile) listOf("<entry is file>") else listOf("<can't list files>"))
        val dialogBuilder = MaterialAlertDialogBuilder(context)
        dialogBuilder.setCustomTitle(TextView(context).apply {
            text = message + "\n" + path
            setPadding(40)
            typeface = Typeface.MONOSPACE
        })
        val layoutInflater = LayoutInflater.from(context)
        dialogBuilder.setView(ScrollView(context).apply {
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                item.forEachIndexed { i, s ->
                    val view = layoutInflater.inflate(R.layout.item_file_picker, this, false)
                    view.findViewById<TextView>(R.id.tv_name).text = s
                    view.findViewById<TextView>(R.id.tv_type).text =
                        when {
                            i == 0 -> "[PAR]"
                            entries?.get(i - 1)?.isDirectory == true -> "[DIR]"
                            entries?.get(i - 1)?.isFile == true -> "[FIL]"
                            else -> "[UNK]"
                        }
                    view.setOnClickListener {
                        when(i) {
                            0 -> pick(context, message, file.parent ?: "/", dorOnly, onPick)
                            else -> entries?.get(i - 1)?.let { pick(context, message, it.absolutePath, dorOnly, onPick) }
                                ?: run { context.showToast("nope") }
                        }
                    }
                    addView(view)
                }
            })
        })
        dialogBuilder.setPositiveButton(android.R.string.ok) { _, _ ->
            onPick(path)
            dialog = null
        }
        dialogBuilder.setNegativeButton(android.R.string.cancel) { _, _ ->
            dialog = null
        }
        dialog?.dismiss()
        dialog = dialogBuilder.show()
    }

    fun pickDir(context: Context, message: String, path: String, onPick: (String) -> Unit) =
        pick(context, message, path, true, onPick)

    fun pickFile(context: Context, message: String, path: String, onPick: (String) -> Unit) =
        pick(context, message, path, false, onPick)

    fun createFile(context: Context, message: String, path: String, onPick: (String) -> Unit) {
        val file = File(path)
        pickDir(context, message, file.parent ?: "/") {
            onPick(it + "/" + file.name)
        }
    }
}