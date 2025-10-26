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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import androidx.core.util.TypedValueCompat
import cc.ioctl.util.HostInfo
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

@FunctionHookEntry
@UiItemAgentEntry
object CacheManager : CommonConfigFunctionHook(defaultEnabled = true) {
    override val name = "缓存管理"
    override val description = "管理 QAuxiliary 产生的部分非核心缓存文件"
    override val uiItemLocation = FunctionEntryRouter.Locations.ConfigCategory.CONFIG_CATEGORY
    override val valueState = MutableStateFlow("")

    val cacheDir = File(HostInfo.getApplication().cacheDir, "qauxv_cache")

    override fun initOnce(): Boolean {
        return true
    }

    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit
        get() = { _, activity, _ ->
            val files = getAllFiles(cacheDir).sortedByDescending { it.length() }
            val fileNames = files.map {
                "(${formatSize(it.length())}) ${it.name}"
            }

            AlertDialog.Builder(activity)
                .setTitle("缓存文件列表")
                .setMessage(fileNames.joinToString("\n"))
                .setPositiveButton("清空缓存") { _, _ ->
                    cacheDir.deleteRecursively()
                    updateValueState()
                }
                .setNegativeButton("关闭", null)
                .show()
                .findViewById<TextView>(android.R.id.message)
                .apply {
                    typeface = Typeface.MONOSPACE
                    textSize = TypedValueCompat.spToPx(5F, activity.resources.displayMetrics)
                }
        }

    override fun initialize(): Boolean {
        ConfigManager.getDefaultConfig().getStringSetOrDefault("CacaheManager.deleteOnStartupFiles", setOf<String>()).forEach {
            val f = File(it)
            if (f.exists()) {
                f.delete()
            }
        }
        ConfigManager.getDefaultConfig().putStringSet("CacaheManager.deleteOnStartupFiles", setOf<String>())
        updateValueState()
        return super.initialize()
    }

    /**
     * 创建一个临时文件，存放在 QAuxiliary 专用缓存目录中
     * @param prefix 文件名前缀 默认使用调用者类名
     * @param suffix 文件名后缀 默认为空
     * @return 创建的临时文件
     */
    @JvmStatic
    @JvmOverloads
    fun createTempFile(prefix: String? = null, suffix: String? = null): File {
        val prefix = prefix ?: Throwable().stackTrace.find { it.className != this::class.java.name }?.className?.substringAfterLast('.') ?: "tempfile"
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val cacheFile = File.createTempFile(prefix, suffix, cacheDir)
        cacheFile.deleteOnExit()
        updateValueState()
        return cacheFile
    }

    @JvmStatic
    fun File.deleteNextTimeStartup() {
        this.deleteOnExit()
        var markedFiles = ConfigManager.getDefaultConfig().getStringSetOrDefault("CacaheManager.deleteOnStartupFiles", setOf<String>()).toMutableSet()
        markedFiles.add(this.absolutePath)
        ConfigManager.getDefaultConfig().putStringSet("CacaheManager.deleteOnStartupFiles", markedFiles)
    }

    fun updateValueState() {
        if (cacheDir.exists().not()) {
            valueState.value = "无缓存文件"
            return
        }
        val allFiles = getAllFiles(cacheDir)
        val totalSize = allFiles.sumOf { it.length() }
        val fileCount = allFiles.size
        valueState.value = "${formatSize(totalSize)}, $fileCount"
    }

    @SuppressLint("DefaultLocale")
    private fun formatSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB", "BB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return String.format("%.2f %s", size, units[unitIndex])
    }

    private fun getAllFiles(dir: File): List<File> {
        val files = mutableListOf<File>()
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                files.addAll(getAllFiles(file))
            } else {
                files.add(file)
            }
        }
        return files
    }
}