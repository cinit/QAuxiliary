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

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import cc.ioctl.util.BugUtils
import cc.ioctl.util.ui.FaultyDialog
import cc.ioctl.hook.misc.CustomSplash
import io.github.qauxv.R
import io.github.qauxv.databinding.FragmentCustomSplashConfigBinding
import io.github.qauxv.fragment.BaseRootLayoutFragment
import io.github.qauxv.util.IoUtils
import io.github.qauxv.util.SafUtils
import io.github.qauxv.util.SyncUtils.async
import io.github.qauxv.util.SyncUtils.runOnUiThread
import io.github.qauxv.util.Toasts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

class CustomSplashConfigFragment : BaseRootLayoutFragment() {

    private var mBinding: FragmentCustomSplashConfigBinding? = null
    private val mHasUnsavedChanges: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private var mLightSplashData: ByteArray? = null
    private var mDarkSplashData: ByteArray? = null
    private var mUseCustomLightSplash = false
    private var mUseDifferentSplashInDarkMode = false
    private var mEnableFunction = false

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        title = "自定义启动图"
        mBinding = FragmentCustomSplashConfigBinding.inflate(inflater, container, false).apply {
            buttonCustomSplashCancel.setOnClickListener { confirmFinishFragment() }
            buttonCustomSplashSave.setOnClickListener { checkAndSavePreference() }
            radioGroupLight.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.radioButtonLightCustom -> {
                        mUseCustomLightSplash = true
                        layoutLightCustomLayout.visibility = View.VISIBLE
                    }
                    else -> {
                        mUseCustomLightSplash = false
                        layoutLightCustomLayout.visibility = View.GONE
                    }
                }
            }
            radioGroupDark.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.radioButtonDarkCustom -> {
                        mUseDifferentSplashInDarkMode = true
                        linearLayoutDarkCustomLayout.visibility = View.VISIBLE
                    }
                    else -> {
                        mUseDifferentSplashInDarkMode = false
                        linearLayoutDarkCustomLayout.visibility = View.GONE
                    }
                }
            }
            checkboxEnableCustomSplash.setOnCheckedChangeListener { _, isChecked -> mEnableFunction = isChecked }
            buttonCustomSplashLightBrowse.setOnClickListener { browseSafForLightSplash() }
            buttonCustomSplashDarkBrowse.setOnClickListener { browseSafForDarkSplash() }
            buttonCustomSplashLightLoadFile.setOnClickListener { loadLightSplashFromFile() }
            buttonCustomSplashDarkLoadFile.setOnClickListener { loadDarkSplashFromFile() }
            updateStatus(this)
        }
        rootLayoutView = mBinding!!.root
        return rootLayoutView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // confirm unsaved changes when navigate back
        requireActivity().onBackPressedDispatcher.addCallback(this, mOnBackPressedCallback)
        // observe unsaved changes
        lifecycleScope.launch {
            mHasUnsavedChanges.collect { hasChanges ->
                mOnBackPressedCallback.isEnabled = hasChanges
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }

    @UiThread
    private fun updateStatus(binding: FragmentCustomSplashConfigBinding) {
        val hook = CustomSplash.INSTANCE
        mEnableFunction = hook.isEnabled
        mUseCustomLightSplash = hook.isUseCustomLightSplash
        mUseDifferentSplashInDarkMode = hook.isUseDifferentDarkSplash
        binding.apply {
            checkboxEnableCustomSplash.isChecked = mEnableFunction
            radioGroupLight.check(if (mUseCustomLightSplash) R.id.radioButtonLightCustom else R.id.radioButtonLightAppDefault)
            radioGroupDark.check(if (mUseDifferentSplashInDarkMode) R.id.radioButtonDarkCustom else R.id.radioButtonSameAsLight)
            if (mUseCustomLightSplash) {
                layoutLightCustomLayout.visibility = View.VISIBLE
            } else {
                layoutLightCustomLayout.visibility = View.GONE
            }
            if (mUseDifferentSplashInDarkMode) {
                linearLayoutDarkCustomLayout.visibility = View.VISIBLE
            } else {
                linearLayoutDarkCustomLayout.visibility = View.GONE
            }
            async {
                val lightRes = hook.openSplashInputStream(CustomSplash.FILE_NAME_SPLASH_LIGHT)
                lightRes?.let {
                    mLightSplashData = IoUtils.readFully(it)
                }
                val darkRes = hook.openSplashInputStream(CustomSplash.FILE_NAME_SPLASH_DARK)
                darkRes?.let {
                    mDarkSplashData = IoUtils.readFully(it)
                }
                runOnUiThread {
                    if (mLightSplashData != null) {
                        updateLightSplash(mLightSplashData!!)
                    }
                    if (mDarkSplashData != null) {
                        updateDarkSplash(mDarkSplashData!!)
                    }
                }
            }
        }
    }

    @UiThread
    private fun loadLightSplashFromFile() {
        val path = mBinding!!.editTextCustomSplashLightPath.text.toString()
        checkFilePath(path)?.let { file ->
            async {
                try {
                    val data = IoUtils.readFile(file)
                    runOnUiThread {
                        updateLightSplash(data)
                        mHasUnsavedChanges.value = true
                    }
                } catch (e: Exception) {
                    FaultyDialog.show(requireContext(), e)
                }
            }
        }
    }

    @UiThread
    private fun loadDarkSplashFromFile() {
        val path = mBinding!!.editTextCustomSplashDarkPath.text.toString()
        checkFilePath(path)?.let { file ->
            async {
                try {
                    val data = IoUtils.readFile(file)
                    runOnUiThread {
                        updateDarkSplash(data)
                        mHasUnsavedChanges.value = true
                    }
                } catch (e: Exception) {
                    FaultyDialog.show(requireContext(), e)
                }
            }
        }
    }

    @UiThread
    private fun browseSafForLightSplash() {
        val ctx = requireContext()
        SafUtils.requestOpenFile(ctx).setMimeType("image/*").onResult { uri ->
            val inputStream = SafUtils.openInputStream(ctx, uri)
            if (inputStream == null) {
                Toasts.error(ctx, "打开文件失败")
            } else async {
                val data = IoUtils.readFully(inputStream)
                runOnUiThread {
                    updateLightSplash(data)
                    mHasUnsavedChanges.value = true
                }
            }
        }.commit()
    }

    @UiThread
    private fun browseSafForDarkSplash() {
        val ctx = requireContext()
        SafUtils.requestOpenFile(ctx).setMimeType("image/*").onResult { uri ->
            val inputStream = SafUtils.openInputStream(ctx, uri)
            if (inputStream == null) {
                Toasts.error(ctx, "打开文件失败")
            } else async {
                val data = IoUtils.readFully(inputStream)
                runOnUiThread {
                    updateDarkSplash(data)
                    mHasUnsavedChanges.value = true
                }
            }
        }.commit()
    }

    @SuppressLint("SetTextI18n")
    @UiThread
    private fun updateLightSplash(data: ByteArray) {
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        if (bitmap != null) {
            mLightSplashData = data
            mBinding!!.apply {
                imageViewCustomSplashLight.setImageBitmap(bitmap)
                val h = bitmap.height
                val w = bitmap.width
                val s = BugUtils.getSizeString(data.size.toLong())
                textViewCustomSplashLightStatus.text = "${w}x${h} $s"
            }
        } else {
            Toasts.error(requireContext(), "图片损坏或者为空")
        }
    }

    @SuppressLint("SetTextI18n")
    @UiThread
    private fun updateDarkSplash(data: ByteArray) {
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        if (bitmap != null) {
            mDarkSplashData = data
            mBinding!!.apply {
                imageViewCustomSplashDark.setImageBitmap(bitmap)
                val h = bitmap.height
                val w = bitmap.width
                val s = BugUtils.getSizeString(data.size.toLong())
                textViewCustomSplashDarkStatus.text = "${w}x${h} $s"
            }
        } else {
            Toasts.error(requireContext(), "图片损坏或者为空")
        }
    }

    @UiThread
    private fun checkAndSavePreference() {
        // check options valid
        if (mEnableFunction) {
            if (mUseCustomLightSplash && mLightSplashData == null) {
                Toasts.error(requireContext(), "请先选择自定义的亮色背景图片")
                return
            }
            if (mUseDifferentSplashInDarkMode && mDarkSplashData == null) {
                Toasts.error(requireContext(), "请先选择自定义的暗色背景图片")
                return
            }
        }
        // update options
        val hook = CustomSplash.INSTANCE
        hook.isEnabled = mEnableFunction
        hook.isUseCustomLightSplash = mUseCustomLightSplash
        hook.isUseDifferentDarkSplash = mUseDifferentSplashInDarkMode
        // save file
        try {
            if (mLightSplashData != null) {
                IoUtils.writeFile(hook.lightSplashFile, mLightSplashData!!)
            }
            if (mDarkSplashData != null) {
                IoUtils.writeFile(hook.darkSplashFile, mDarkSplashData!!)
            }
            mHasUnsavedChanges.value = false
            Toasts.success(requireContext(), "保存成功")
            finishFragment()
        } catch (e: Exception) {
            FaultyDialog.show(requireContext(), e)
        }
    }

    private fun checkFilePath(path: String): File? {
        val ctx = requireContext()
        if (path.isEmpty()) {
            Toasts.error(ctx, "请输入文件路径")
            return null
        }
        if (!path.startsWith("/")) {
            Toasts.error(ctx, "文件路径必须以 '/' 开头")
            return null
        }
        val file = File(path)
        if (!file.exists()) {
            Toasts.error(ctx, "文件不存在")
            return null
        }
        if (!file.canRead()) {
            Toasts.error(ctx, "文件无法读取")
            return null
        }
        if (file.length() > 1024 * 1024 * 64) {
            Toasts.error(ctx, "文件过大: ${file.length() / 1024 / 1024}MiB")
            return null
        }
        return file
    }

    private val mOnBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            confirmFinishFragment()
        }
    }

    @UiThread
    private fun confirmFinishFragment() {
        if (!mHasUnsavedChanges.value) {
            finishFragment()
            return
        } else {
            val ctx = requireContext()
            AlertDialog.Builder(ctx)
                .setTitle("提示")
                .setMessage("未保存的更改将会丢失，确定要退出吗？")
                .setCancelable(true)
                .setPositiveButton("不保存") { _, _ -> finishFragment() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .show()
        }
    }
}
