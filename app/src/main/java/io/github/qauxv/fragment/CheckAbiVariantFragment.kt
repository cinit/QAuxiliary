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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.system.Os
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import io.github.qauxv.BuildConfig
import io.github.qauxv.databinding.FragmentAbiVariantInfoBinding
import io.github.qauxv.databinding.ItemHostStatusBinding
import io.github.qauxv.fragment.CheckAbiVariantModel.AbiInfo
import io.github.qauxv.util.hookstatus.AbiUtils
import io.github.qauxv.util.hookstatus.HookStatus
import io.github.qauxv.util.hostInfo
import io.github.qauxv.util.isInModuleProcess

class CheckAbiVariantFragment : BaseRootLayoutFragment() {

    private var mBinding: FragmentAbiVariantInfoBinding? = null

    override fun getTitle() = "εηεΊ ABI"

    override fun doOnCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentAbiVariantInfoBinding.inflate(inflater, container, false).apply {
            btnDownloadFromGitHub.setOnClickListener {
                openUri("https://github.com/cinit/QAuxiliary/releases/latest")
            }
            btnDownloadFromTelegram.setOnClickListener {
                openUri("https://t.me/QAuxiliary")
            }
            itemHostQQ.itemHostIgnoreButton.setOnClickListener(onIgnoreClickListener)
            itemHostTIM.itemHostIgnoreButton.setOnClickListener(onIgnoreClickListener)
            itemHostQQLite.itemHostIgnoreButton.setOnClickListener(onIgnoreClickListener)
        }
        updateView()
        rootLayoutView = mBinding!!.root
        return mBinding!!.root
    }

    @UiThread
    private fun updateView() {
        mBinding?.apply {
            val abiStatus = CheckAbiVariantModel.collectAbiInfo(requireContext())
            val isTaiChiYin = isInModuleProcess && HookStatus.isTaiChiInstalled(requireContext()) && !HookStatus.isZygoteHookMode()
            if (isTaiChiYin && abiStatus.suggestedApkAbiVariant != "universal") {
                abiStatus.suggestedApkAbiVariant = "armAll"
            }
            if (!abiStatus.isAbiMatch || (isTaiChiYin && abiStatus.suggestedApkAbiVariant != "universal")) {
                warnAbiMismatchBar.visibility = View.VISIBLE
                recommendedModuleAbiVariant.text = "ζ¨θζ¨ε°ζ¨‘εζ΄ζ’δΈΊδ½Ώη¨ ${abiStatus.suggestedApkAbiVariant} εηεΊηηζ¬" +
                    (if (isTaiChiYin) "\nε€ͺζη¨ζ·θ―·δ½Ώη¨ armAll εηεΊοΌεΆδ»ηζ¬ε°δΈδΌηζ" else "")
            } else {
                warnAbiMismatchBar.visibility = View.GONE
            }
            val message = StringBuilder("ε½εζ¨‘εδ½Ώη¨ηεηεΊδΈΊ " + BuildConfig.FLAVOR)
            if (AbiUtils.archStringToArchInt(Os.uname().machine) and (AbiUtils.ABI_X86 or AbiUtils.ABI_X86_64) != 0) {
                message.append("\n").append("ε½εη³»η» uname machine δΈΊ ").append(Os.uname().machine)
            }
            currentModuleAbiVariant.text = message.toString()
            itemHostNotFound.visibility = if (abiStatus.packages.isEmpty()) View.VISIBLE else View.GONE
            updateHostItem(itemHostQQ, abiStatus.packages[CheckAbiVariantModel.HOST_PACKAGES[0]])
            updateHostItem(itemHostTIM, abiStatus.packages[CheckAbiVariantModel.HOST_PACKAGES[1]])
            updateHostItem(itemHostQQLite, abiStatus.packages[CheckAbiVariantModel.HOST_PACKAGES[2]])
        }
    }

    @UiThread
    private fun updateHostItem(binding: ItemHostStatusBinding, pkg: AbiInfo.Package?) {
        if (pkg == null) {
            binding.root.visibility = View.GONE
            return
        }
        binding.root.visibility = View.VISIBLE
        binding.hostDescription.text = AbiUtils.archIntToNames(pkg.abi)
        binding.hostTitle.text = pkg.packageName
        binding.itemHostIgnoreButton.tag = pkg.packageName
        if (hostInfo.packageName == pkg.packageName) {
            binding.itemHostIgnoreButton.apply {
                text = "ε½εεΊη¨"
                isClickable = false
            }
        } else {
            binding.itemHostIgnoreButton.apply {
                text = if (pkg.ignored) "ε·²εΏ½η₯" else "εΏ½η₯"
                isClickable = true
            }
        }
    }

    private val onIgnoreClickListener = View.OnClickListener { v ->
        val pkg = v.tag as String
        CheckAbiVariantModel.setPackageIgnored(pkg, !CheckAbiVariantModel.isPackageIgnored(pkg))
        updateView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }

    override fun onResume() {
        super.onResume()
        updateView()
    }

    private fun openUri(uri: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        startActivity(intent)
    }
}
