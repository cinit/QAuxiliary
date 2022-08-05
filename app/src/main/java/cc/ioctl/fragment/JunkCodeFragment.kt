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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.core.view.doOnLayout
import cc.ioctl.util.JunkCodeUtils
import io.github.qauxv.databinding.FragmentJunkCodeBinding
import io.github.qauxv.fragment.BaseRootLayoutFragment
import xyz.nextalone.util.SystemServiceUtils
import java.text.SimpleDateFormat

class JunkCodeFragment : BaseRootLayoutFragment(), Runnable {

    override fun getTitle(): String = "动态验证码"

    private val mTimeFormat = SimpleDateFormat("HH:mm:ss", java.util.Locale.ROOT)
    private var mLasttc: Int = 0
    private var mBinding: FragmentJunkCodeBinding? = null

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mBinding = FragmentJunkCodeBinding.inflate(inflater, container, false).apply {
            junkCodeCopy.setOnClickListener {
                val code = getCodeForTime(System.currentTimeMillis())
                SystemServiceUtils.copyToClipboard(it.context, code)
                Toast.makeText(it.context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
            }
        }
        rootLayoutView = mBinding?.root
        return mBinding!!.root
    }

    @UiThread
    private fun updateStatus(binding: FragmentJunkCodeBinding) {
        val now = System.currentTimeMillis()
        val tc = (now / 1000L / 30L).toInt()
        val expireTime = (tc + 1) * 30L * 1000L
        val code = JunkCodeUtils.getJunkCode(tc)
        val timeRemainingMs = expireTime - now
        val remainPercentage = timeRemainingMs.toFloat() / (30L * 1000L).toFloat()
        if (tc != mLasttc) {
            // six digits
            val codeStr = code.toString().padStart(6, '0')
            binding.junkCodeCode.text = codeStr
        }
        binding.junkCodeInvalidateTime.text = "过期时间: " + mTimeFormat.format(expireTime) +
            " (剩余 " + (timeRemainingMs / 1000L).coerceAtLeast(1) + " 秒)"
        mLasttc = tc
        binding.junkCodeProgressBar.layoutParams.apply {
            val parentWidth = (binding.junkCodeProgressBar.parent as ViewGroup).width
            width = (parentWidth * remainPercentage).toInt()
        }
        binding.junkCodeProgressBar.requestLayout()
    }

    override fun run() {
        if (isResumed) {
            mBinding?.let {
                updateStatus(it)
                it.junkCodeInvalidateTime.postDelayed(this, 1000L)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mBinding?.let { binding ->
            binding.rootMainLayout.doOnLayout {
                updateStatus(binding)
            }
            // schedule a job to update the time every second
            binding.junkCodeInvalidateTime.postDelayed(this, 1000L)
        }
    }

    private fun getCodeForTime(time: Long): String {
        val tc = (time / 1000L / 30L).toInt()
        val code = JunkCodeUtils.getJunkCode(tc)
        return code.toString().padStart(6, '0')
    }
}
