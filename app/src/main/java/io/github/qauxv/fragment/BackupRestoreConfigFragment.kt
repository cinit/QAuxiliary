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

package io.github.qauxv.fragment

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.AnyThread
import androidx.appcompat.app.AlertDialog
import cc.ioctl.util.Reflex
import cc.ioctl.util.ui.FaultyDialog
import io.github.duzhaokun123.util.FilePicker
import io.github.qauxv.R
import io.github.qauxv.config.BackupConfigSession
import io.github.qauxv.config.RestoreConfigSession
import io.github.qauxv.databinding.FragmentBackupRestoreConfigBinding
import io.github.qauxv.ui.CustomDialog
import io.github.qauxv.util.NonUiThread
import io.github.qauxv.util.SafUtils
import io.github.qauxv.util.SyncUtils.async
import io.github.qauxv.util.SyncUtils.runOnUiThread
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.UiThread
import java.io.File
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class BackupRestoreConfigFragment : BaseRootLayoutFragment(), View.OnClickListener {

    private var binding: FragmentBackupRestoreConfigBinding? = null
    private var mBackupSession: BackupConfigSession? = null
    private var mRestoreSession: RestoreConfigSession? = null
    private var mBackupResultZipFile: File? = null

    private var editTextBackupLocation: EditText? = null
    private var editTextRestoreLocation: EditText? = null
    private val mTmpCacheFiles = ArrayList<File>()

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        title = "备份与恢复"
        binding = FragmentBackupRestoreConfigBinding.inflate(inflater, container, false).apply {
            editTextBackupLocation = backupRestoreConfigEditTextBackupPath
            editTextRestoreLocation = backupRestoreConfigEditTextRestorePath
            backupRestoreConfigRadioGroupOperationType.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.backupRestoreConfig_radioButton_backup -> {
                        backupRestoreConfigLinearLayoutBackupLayout.visibility = View.VISIBLE
                        backupRestoreConfigLinearLayoutRestoreLayout.visibility = View.GONE
                        backupRestoreConfigButtonNextStep.visibility = View.VISIBLE
                        if (backupRestoreConfigEditTextBackupPath.text.isEmpty()) {
                            backupRestoreConfigEditTextBackupPath.setText(generateDefaultBackupLocation())
                        }
                    }
                    R.id.backupRestoreConfig_radioButton_restore -> {
                        backupRestoreConfigLinearLayoutBackupLayout.visibility = View.GONE
                        backupRestoreConfigLinearLayoutRestoreLayout.visibility = View.VISIBLE
                        backupRestoreConfigButtonNextStep.visibility = View.VISIBLE
                    }
                    else -> {
                        backupRestoreConfigLinearLayoutBackupLayout.visibility = View.GONE
                        backupRestoreConfigLinearLayoutRestoreLayout.visibility = View.GONE
                        backupRestoreConfigButtonNextStep.visibility = View.GONE
                    }
                }
            }
            backupRestoreConfigButtonNextStep.setOnClickListener(this@BackupRestoreConfigFragment)
            backupRestoreConfigButtonBrowseFile.setOnClickListener(this@BackupRestoreConfigFragment)
            backupRestoreConfigButtonBrowseFile2.setOnClickListener(this@BackupRestoreConfigFragment)
            backupRestoreConfigButtonBrowseFile3.setOnClickListener(this@BackupRestoreConfigFragment)
        }
        rootLayoutView = binding!!.root
        return binding!!.root
    }

    @UiThread
    private fun startBackupProcedure(location: String) {
        val context = requireContext()
        try {
            if (mBackupSession == null) {
                mBackupSession = BackupConfigSession(context)
            }
            val mmkvList: Array<String> = mBackupSession!!.listMmkvConfig()
            val availableChoices: ArrayList<String> = ArrayList()
            // don't backup the cache
            mmkvList.forEach {
                if (it != "global_cache") {
                    availableChoices.add(it)
                }
            }
            if (availableChoices.isEmpty()) {
                Toasts.error(context, "没有可备份的配置文件")
                return
            }
            // show multi choice dialog, all choices are checked by default
            AlertDialog.Builder(context)
                .setTitle("选择要备份的配置文件")
                .setMultiChoiceItems(availableChoices.toTypedArray(), BooleanArray(availableChoices.size) { true }) { _, _, _ -> }
                .setPositiveButton("确定") { dialog, _ ->
                    val selectedItems = (dialog as AlertDialog).listView.checkedItemPositions
                    val selectedItemsList = ArrayList<String>()
                    for (i in 0 until availableChoices.size) {
                        if (selectedItems.get(i)) {
                            selectedItemsList.add(availableChoices[i])
                        }
                    }
                    if (selectedItemsList.isEmpty()) {
                        Toasts.error(context, "没有选择要备份的配置文件")
                        return@setPositiveButton
                    }
                    async {
                        executeBackupTask(location, selectedItemsList.toTypedArray())
                    }
                }
                .setNegativeButton("取消") { _, _ -> }
                .show()
        } catch (e: Exception) {
            showErrorDialog(e)
            return
        }
    }

    @NonUiThread
    private fun executeBackupTask(location: String, configs: Array<String>) {
        mBackupSession!!.also { session ->
            try {
                configs.forEach {
                    session.backupMmkvConfigToWorkDir(it)
                }
                mBackupResultZipFile = session.createBackupZipFile()
                // copy zip file to location
                val targetFile = File(location)
                mBackupResultZipFile!!.copyTo(targetFile, true)
                mBackupResultZipFile!!.delete()
                mBackupResultZipFile = null
                session.close()
                mBackupSession = null
                Toasts.success(requireContext(), "备份完成")
            } catch (e: Exception) {
                showErrorDialog(e)
            }
        }
    }

    @UiThread
    private fun startRestoreProcedure(location: String) {
        val context = requireContext()
        try {
            if (mRestoreSession == null) {
                mRestoreSession = RestoreConfigSession(context)
            }
            mRestoreSession!!.also {
                it.loadBackupFile(File(location))
                val availableChoices: ArrayList<String> = it.listBackupMmkvConfig().toCollection(ArrayList())
                // remove the cache
                availableChoices.remove("global_cache")
                if (availableChoices.isEmpty()) {
                    Toasts.error(context, "没有可恢复的配置文件")
                    return
                }
                // show multi choice dialog
                AlertDialog.Builder(context)
                    .setTitle("选择要恢复的配置文件")
                    .setMultiChoiceItems(availableChoices.toTypedArray(), null) { _, _, _ -> }
                    .setPositiveButton("确定") { dialog, _ ->
                        val selectedItems = (dialog as AlertDialog).listView.checkedItemPositions
                        val selectedItemsList = ArrayList<String>()
                        for (i in 0 until availableChoices.size) {
                            if (selectedItems.get(i)) {
                                selectedItemsList.add(availableChoices[i])
                            }
                        }
                        if (selectedItemsList.isEmpty()) {
                            Toasts.error(context, "没有选择要恢复的配置文件")
                            return@setPositiveButton
                        }
                        confirmRestoreOverwrite(location, selectedItemsList.toTypedArray())
                    }
                    .setNegativeButton("取消") { _, _ -> }
                    .show()
            }
        } catch (e: Exception) {
            showErrorDialog(e)
            return
        }
    }

    @UiThread
    private fun confirmRestoreOverwrite(location: String, choices: Array<String>) {
        val context = requireContext()
        val onDeviceConfigList: Array<String> = mRestoreSession!!.listOnDeviceMmkvConfig()
        val overwriteList = ArrayList<String>()
        for (choice in choices) {
            if (onDeviceConfigList.contains(choice)) {
                overwriteList.add(choice)
            }
        }
        if (overwriteList.isEmpty()) {
            async {
                executeRestoreTask(choices)
            }
            return
        }
        // ask user to confirm overwrite
        val message = StringBuilder().apply {
            append("您选择了以下配置文件要恢复：\n")
            append(choices.joinToString(", ")).append("\n")
            append("但是在设备上已经存在以下配置文件：\n")
            append(overwriteList.joinToString(", ")).append("\n")
            append("已存在的配置文件将被覆盖，确定要恢复吗？")
        }
        AlertDialog.Builder(context)
            .setTitle("恢复配置文件")
            .setMessage(message)
            .setPositiveButton("确定") { _, _ ->
                async {
                    executeRestoreTask(choices)
                }
            }
            .setNegativeButton("取消") { _, _ -> }
            .show()
    }

    @NonUiThread
    private fun executeRestoreTask(configs: Array<String>) {
        mRestoreSession!!.also { session ->
            try {
                configs.forEach {
                    session.restoreBackupMmkvConfig(it)
                }
                session.close()
                mRestoreSession = null
                // ask user to restart app
                runOnUiThread {
                    AlertDialog.Builder(requireContext())
                        .setTitle("恢复完成")
                        .setMessage("恢复完成，部分功能需要重启应用才能生效，是否现在重启应用？")
                        .setCancelable(false)
                        .setPositiveButton("现在重启") { _, _ ->
                            mRestoreSession?.close()
                            mRestoreSession = null
                            cleanUpTmpFiles()
                            Thread.sleep(100)
                            exitProcess(0)
                            // AM will restart us on most platforms, we don't need to do anything
                        }
                        .setNegativeButton("稍后重启") { _, _ -> }
                        .show()
                }
            } catch (e: Exception) {
                showErrorDialog(e)
            }
        }
    }

    private fun generateDefaultBackupLocation(): String {
        val context = requireContext()
        // qauxv_backup_yyyy-MM-dd_HH-mm-ss.zip
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT)
        val name = "qauxv_backup_${sdf.format(Date())}.zip"
        // save to Android standard Download folder
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadDir, name).absolutePath
    }

    @UiThread
    private fun checkBackupSaveLocation(location: String): Boolean {
        val file = File(location)
        val context = requireContext()
        if (!location.startsWith("/")) {
            Toasts.error(context, "备份路径必须是绝对路径(以 / 开头)")
            return false
        }
        if (file.exists()) {
            if (file.isDirectory) {
                AlertDialog.Builder(context)
                    .setTitle("路径错误")
                    .setMessage("请输入完整的带文件名的路径，而不是目录！")
                    .setPositiveButton("确定") { _, _ -> }
                    .show()
            } else {
                Toasts.error(context, "备份文件已存在")
            }
            return false
        }
        val parentDir = file.parentFile!!
        if (!parentDir.exists()) {
            Toasts.error(context, "备份文件所在目录不存在")
            return false
        }
        if (!parentDir.canWrite()) {
            Toasts.error(context, "备份文件所在目录不可写")
            return false
        }
        if (checkBadLocation(location)) {
            Toasts.error(context, "禁止接触")
            return false
        }
        return true
    }

    @UiThread
    private fun checkRestoreSourceLocation(location: String): Boolean {
        val file = File(location)
        val context = requireContext()
        if (!location.startsWith("/")) {
            Toasts.error(context, "恢复路径必须是绝对路径(以 / 开头)")
            return false
        }
        if (!file.exists()) {
            Toasts.error(context, "恢复文件不存在")
            return false
        }
        if (!file.canRead()) {
            Toasts.error(context, "恢复文件不可读")
            return false
        }
        if (checkBadLocation(location)) {
            Toasts.error(context, "禁止接触")
            return false
        }
        return true
    }

    override fun onClick(v: View) {
        if (v.id == R.id.backupRestoreConfig_button_nextStep) {
            when (binding!!.backupRestoreConfigRadioGroupOperationType.checkedRadioButtonId) {
                R.id.backupRestoreConfig_radioButton_backup -> {
                    val location = editTextBackupLocation!!.text.toString()
                    if (checkBackupSaveLocation(location)) {
                        startBackupProcedure(location)
                    }
                }
                R.id.backupRestoreConfig_radioButton_restore -> {
                    val location = editTextRestoreLocation!!.text.toString()
                    if (checkRestoreSourceLocation(location)) {
                        startRestoreProcedure(location)
                    }
                }
                else -> {
                    Toasts.error(requireContext(), "请选择操作类型")
                }
            }
        } else if (v.id == R.id.backupRestoreConfig_buttonBrowseFile) {
            SafUtils.requestOpenFile(requireContext())
                .setMimeType("application/zip")
                .onResult { uri ->
                    editTextRestoreLocation!!.setText(uri.toString())
                    async {
                        val file = copyContentToTmpCache(uri)
                        if (file != null) {
                            runOnUiThread {
                                startRestoreProcedure(file.absolutePath)
                            }
                        }
                    }
                }.commit()
        } else if (v.id == R.id.backupRestoreConfig_buttonBrowseFile2) {
            FilePicker.pickFile(requireContext(), "选择备份文件", Environment.getExternalStorageDirectory().absolutePath) { path ->
                editTextRestoreLocation!!.setText(path)
                startRestoreProcedure(path)
            }
        } else if (v.id == R.id.backupRestoreConfig_buttonBrowseFile3) {
            FilePicker.createFile(requireContext(), "选择备份文件保存目录", generateDefaultBackupLocation()) { path ->
                editTextBackupLocation!!.setText(path)
            }
        }
    }

    @AnyThread
    private fun showErrorDialog(e: Throwable) {
        runOnUiThread {
            val context = requireContext()
            CustomDialog.createFailsafe(context)
                .setMessage(e.toString())
                .setTitle("错误: " + Reflex.getShortClassName(e))
                .setCancelable(false)
                .ok().show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
        mBackupSession?.close()
        mBackupSession = null
        mRestoreSession?.close()
        mRestoreSession = null
        async { cleanUpTmpFiles() }
    }

    private fun cleanUpTmpFiles() {
        if (mTmpCacheFiles.isNotEmpty()) {
            for (file in mTmpCacheFiles) {
                if (file.isFile) {
                    file.delete()
                }
            }
        }
    }

    @NonUiThread
    private fun copyContentToTmpCache(uri: Uri): File? {
        try {
            SafUtils.openInputStream(requireContext(), uri)
                .use { input ->
                    val time = System.currentTimeMillis()
                    val dir = File(requireContext().cacheDir, "qa_cp_ext")
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    val file = File(dir, "tmp_$time.zip")
                    mTmpCacheFiles.add(file)
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                    return file
                }
        } catch (e: Exception) {
            FaultyDialog.show(requireContext(), e)
            return null
        }
    }

    private fun checkBadLocation(path: String): Boolean {
        val specs: Array<String> = arrayOf("/dev/", "/sys/", "/acct/", "/d/")
        for (spec in specs) {
            if (path.startsWith(spec)) {
                return true
            }
        }
        if (path.startsWith("/proc/")) {
            // allow /proc/[pid]/fd/[fd], where [pid] may be self and thread-self
            return !(path.startsWith("/proc/self/fd/")
                || path.startsWith("/proc/thread-self/fd/")
                || path.matches("/proc/\\d+/fd/\\d+".toRegex()))
        }
        return false
    }
}
