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

package cc.ioctl.fragment

import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import io.github.qauxv.databinding.FragmentPcm2silkTestBinding
import io.github.qauxv.fragment.BaseRootLayoutFragment
import io.github.qauxv.util.Log
import io.github.qauxv.util.NonUiThread
import io.github.qauxv.util.SafUtils
import io.github.qauxv.util.SyncUtils.runOnUiThread
import io.github.qauxv.util.ptt.SilkEncodeUtils
import java.io.File

class Pcm2SilkTestFragment : BaseRootLayoutFragment() {

    private var binding: FragmentPcm2silkTestBinding? = null

    override fun getTitle() = "PCM16LE 转 SILK"

    private fun errorDialog(msg: String) {
        runOnUiThread {
            AlertDialog.Builder(requireContext())
                .setTitle("错误")
                .setMessage(msg)
                .setPositiveButton("确定", null)
                .show()
        }
    }

    private fun errorDialog(msg: String, e: Throwable) {
        Log.e(msg, e)
        runOnUiThread {
            AlertDialog.Builder(requireContext())
                .setTitle("错误")
                .setMessage(msg + "\n" + e.toString())
                .setPositiveButton("确定", null)
                .show()
        }
    }

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPcm2silkTestBinding.inflate(inflater, container, false).apply {
            silkTestButtonBrowseInputFile.setOnClickListener {
                SafUtils.requestOpenFile(requireContext())
                    .setMimeType("application/octet-stream")
                    .onResult { uri ->
                        try {
                            SafUtils.openFileDescriptor(requireContext(), uri, "r").close()
                            silkTestEditTextInputFilePath.setText(uri.toString())
                        } catch (e: Exception) {
                            errorDialog("打开文件失败", e)
                        }
                    }
                    .commit()
            }
            silkTestButtonBrowseOutputFile.setOnClickListener {
                SafUtils.requestSaveFile(requireContext())
                    .setDefaultFileName("output-${System.currentTimeMillis()}.slk")
                    .setMimeType("application/octet-stream")
                    .onResult { uri ->
                        try {
                            SafUtils.openFileDescriptor(requireContext(), uri, "rw").close()
                            silkTestEditTextOutputFilePath.setText(uri.toString())
                        } catch (e: Exception) {
                            errorDialog("打开文件失败", e)
                        }
                    }
                    .commit()
            }
            silkTestButtonNextStep.setOnClickListener {
                val tencentFormat = silkTestCheckBoxTencent.isChecked
                val input = silkTestEditTextInputFilePath.text.toString()
                val output = silkTestEditTextOutputFilePath.text.toString()
                val sampleRate = try {
                    silkTestEditTextSampleRate.text.toString().toInt()
                } catch (e: NumberFormatException) {
                    errorDialog("采样率必须是整数")
                    return@setOnClickListener
                }
                val bitRate = try {
                    silkTestEditTextBitRate.text.toString().toInt()
                } catch (e: NumberFormatException) {
                    errorDialog("比特率必须是整数")
                    return@setOnClickListener
                }
                runOnUiThread {
                    if (convertToSilk(input, output, sampleRate, bitRate, tencentFormat)) {
                        runOnUiThread {
                            Snackbar.make(binding!!.root, "转换完成", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        rootLayoutView = binding!!.root
        return binding!!.root
    }

    @NonUiThread
    private fun openPath(path: String, write: Boolean): ParcelFileDescriptor {
        return if (path.startsWith("content://")) {
            SafUtils.openFileDescriptor(requireContext(), Uri.parse(path), if (write) "rw" else "r")
        } else if (path.startsWith("/")) {
            val file = File(path)
            if (!file.exists() && write) {
                file.createNewFile()
            }
            ParcelFileDescriptor.open(
                file,
                if (write) ParcelFileDescriptor.MODE_READ_WRITE else ParcelFileDescriptor.MODE_READ_ONLY
            )
        } else {
            throw IllegalArgumentException("path must start with / or content://")
        }
    }

    @NonUiThread
    private fun convertToSilk(inputPath: String, outputPath: String, sampleRate: Int, bitRate: Int, tencentFormat: Boolean): Boolean {
        try {
            openPath(inputPath, false).use { inputFd ->
                openPath(outputPath, true).use { outputFd ->
                    SilkEncodeUtils.nativePcm16leToSilkII(
                        inputFd.detachFd(), outputFd.detachFd(),
                        sampleRate, bitRate, 320, tencentFormat
                    )
                    return true
                }
            }
        } catch (e: Exception) {
            errorDialog("转换失败", e)
            return false
        }
    }

}
