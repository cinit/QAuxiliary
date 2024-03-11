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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import cc.ioctl.util.LayoutHelper.MATCH_PARENT
import cc.ioctl.util.ui.dsl.RecyclerListViewController
import io.github.qauxv.util.Log
import io.github.qauxv.BuildConfig
import io.github.qauxv.R
import io.github.qauxv.activity.ConfigV2Activity
import io.github.qauxv.activity.SettingsUiFragmentHostActivity
import io.github.qauxv.base.ISwitchCellAgent
import io.github.qauxv.dsl.item.CategoryItem
import io.github.qauxv.dsl.item.DslTMsgListItemInflatable
import io.github.qauxv.dsl.item.TextListItem
import io.github.qauxv.dsl.item.TextSwitchItem
import io.github.qauxv.util.CliOper
import io.github.qauxv.util.data.Licenses
import io.github.qauxv.util.hostInfo
import io.github.qauxv.util.isInHostProcess
import io.github.qauxv.util.isInModuleProcess
import java.util.Locale

class AboutFragment : BaseRootLayoutFragment() {

    private var mDslListViewController: RecyclerListViewController? = null

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        title = "关于"
        val context = inflater.context
        mDslListViewController = RecyclerListViewController(context, lifecycleScope)
        mDslListViewController!!.items = hierarchy
        mDslListViewController!!.initAdapter()
        mDslListViewController!!.initRecyclerListView()
        val recyclerView = mDslListViewController!!.recyclerListView!!
        recyclerView.apply {
            id = R.id.fragmentMainRecyclerView
        }
        val rootView: FrameLayout = FrameLayout(context).apply {
            addView(mDslListViewController!!.recyclerListView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
        rootLayoutView = recyclerView
        return rootView
    }

    private val hierarchy: Array<DslTMsgListItemInflatable> by lazy {
        arrayOf(
            CategoryItem("QAuxiliary") {
                textItem("愿每个人都被这世界温柔以待", value = " :) ")
            },
            CategoryItem("版本") {
                val moduleVersionName = BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + ")"
                textItem("模块版本", value = moduleVersionName) {
                    copyText(moduleVersionName)
                }
                textItem("构建时间", value = getBuildTimeString())
                if (isInHostProcess) {
                    val hostVersionName = getHostFullVersionNameAndVersionCode()
                    textItem(hostInfo.hostName, value = hostVersionName) {
                        copyText(hostVersionName)
                    }
                }
                textItem("Build UUID", summary = BuildConfig.BUILD_UUID) {
                    copyText(BuildConfig.BUILD_UUID)
                }
                if (isInHostProcess) {
                    textItem("模块热更新设置") {
                        SettingsUiFragmentHostActivity.startFragmentWithContext(
                            requireContext(),
                            HotUpdateConfigFragment::class.java
                        )
                    }
                }
            },
            CategoryItem("隐私与协议") {
                textItem("用户协议与隐私政策") {
                    SettingsUiFragmentHostActivity.startFragmentWithContext(
                        context = requireContext(),
                        fragmentClass = EulaFragment::class.java
                    )
                }
                if (!isInModuleProcess) {
                    add(
                        TextSwitchItem(
                            "AppCenter 匿名统计与崩溃收集",
                            summary = "我们使用 Microsoft AppCenter 来匿名地收集崩溃信息和最常被人们使用的功能和一些使用习惯数据来使得 QAuxiliary 变得更加实用",
                            switchAgent = mAllowAppCenterStatics
                        )
                    )
                }
            },
            CategoryItem("群组") {
                textItem("Telegram 频道", value = "@QAuxiliary") {
                    openUrl("https://t.me/QAuxiliary")
                }
                textItem("Telegram 群组", value = "@QAuxiliaryChat") {
                    openUrl("https://t.me/QAuxiliaryChat")
                }
            },
            CategoryItem("源代码") {
                textItem("GitHub", value = "cinit/QAuxiliary") {
                    openUrl(GITHUB_URL)
                }
            },
            CategoryItem("开放源代码许可") {
                notices.forEach { this@CategoryItem.add(noticeToUiItem(it)) }
            }
        )
    }

    private fun getBuildNumberForCurrentTencentHostApplication(): Int {
        val ctx = hostInfo.application
        val pm = ctx.packageManager
        val metaData: Bundle
        try {
            metaData = pm.getApplicationInfo(ctx.packageName, PackageManager.GET_META_DATA).metaData ?: return 0
        } catch (e: PackageManager.NameNotFoundException) {
            // WTF? in container?
            Log.e("getBuildNumberForCurrentTencentHostApplication WTF", e)
            return 0
        }
        val rdmUuid = metaData.getString("com.tencent.rdm.uuid") ?: return 0
        // [0-9]{3,5}_[0-9]{3,5}
        if (!rdmUuid.matches("[0-9]{3,5}_[0-9]{3,5}".toRegex())) return 0
        // buildNumber is first part
        return rdmUuid.split("_")[0].toInt()
    }

    private fun getHostFullVersionNameAndVersionCode(): String {
        val buildNumber = getBuildNumberForCurrentTencentHostApplication()
        val fullVersionName = if (buildNumber == 0) {
            hostInfo.versionName
        } else {
            hostInfo.versionName + "." + buildNumber
        }
        return fullVersionName + "(" + hostInfo.versionCode32 + ")"
    }

    private fun getBuildTimeString(): String {
        // yyyy-MM-dd HH:mm:ss
        val timestamp: Long = BuildConfig.BUILD_TIMESTAMP
        val date = java.util.Date(timestamp)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
        return sdf.format(date)
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        ContextCompat.startActivity(requireContext(), intent, null)
    }

    private val GITHUB_URL = "https://github.com/cinit/QAuxiliary"

    private val mAllowAppCenterStatics: ISwitchCellAgent = object : ISwitchCellAgent {
        override val isCheckable = true
        override var isChecked: Boolean
            get() = CliOper.isAppCenterAllowed()
            set(value) {
                CliOper.setAppCenterAllowed(value)
                if (value) {
                    CliOper.__init__(hostInfo.application)
                }
            }
    }

    private val notices: List<LicenseNotice> by lazy {
        val ret = listOf(
            LicenseNotice(
                "QNotified",
                "https://github.com/ferredoxin/QNotified",
                "Copyright (C) 2019-2021 ferredoxin",
                "AGPL-3.0 License with EULA"
            ),
            LicenseNotice(
                "Android",
                "https://source.android.google.cn/license",
                "Android Open Source Project",
                "Apache License 2.0"
            ),
            LicenseNotice(
                "Xposed",
                "https://github.com/rovo89/XposedBridge",
                "Copyright 2013 rovo89, Tungstwenty",
                "Apache License 2.0"
            ),
            LicenseNotice(
                "LSPosed",
                "https://github.com/LSPosed/LSPosed",
                "Copyright (C) 2021 LSPosed",
                "GPL-3.0 License"
            ),
            LicenseNotice(
                "Dobby",
                "https://github.com/jmpews/Dobby",
                "jmpews",
                "Apache License 2.0"
            ),
            LicenseNotice(
                "{fmt}",
                "https://github.com/fmtlib/fmt",
                "Copyright (c) 2012 - present, Victor Zverovich and {fmt} contributors",
                "MIT License"
            ),
            LicenseNotice(
                "CustoMIUIzer",
                "https://code.highspec.ru/Mikanoshi/CustoMIUIzer",
                "Mikanoshi",
                "GPL-3.0 License"
            ),
            LicenseNotice(
                "MMKV",
                "https://github.com/Tencent/MMKV",
                "Copyright (C) 2018 THL A29 Limited, a Tencent company.",
                "BSD 3-Clause License"
            ),
            LicenseNotice(
                "QQ-Notify-Evolution",
                "https://github.com/ichenhe/QQ-Notify-Evolution",
                "ichenhe",
                "AGPL-3.0 License"
            ),
            LicenseNotice(
                "DexKit",
                "https://github.com/LuckyPray/DexKit",
                "LuckyPray",
                "LGPL-3.0 license"
            ),
            LicenseNotice(
                "justpush",
                "https://github.com/F-19-F/justpush",
                "F-19-F",
                "GPL-3.0 License"
            ),
            LicenseNotice(
                "QQHelper",
                "https://github.com/Qiwu2542284182/QQHelper",
                "祈无",
                "No License"
            ),
            LicenseNotice(
                "noapplet",
                "https://github.com/Alcatraz323/noapplet",
                "Alcatraz323",
                "MIT License"
            ),
            LicenseNotice(
                "QQ净化",
                "https://github.com/zpp0196/QQPurify",
                "zpp0196",
                "Apache License 2.0"
            ),
            LicenseNotice(
                "原生音乐通知",
                "https://github.com/Cryolitia/XposedMusicNotify",
                "singleNeuron",
                "LGPL-3.0 license"
            ),
            LicenseNotice(
                "QQSpeciallyCare",
                "https://github.com/Cryolitia/QQSpeciallyCare",
                "singleNeuron",
                "Apache License 2.0"
            ),
            LicenseNotice(
                "silk-v3-decoder",
                "https://github.com/kn007/silk-v3-decoder",
                "kn007",
                "The MIT License"
            ),
            LicenseNotice(
                "linux-syscall-support",
                "https://chromium.googlesource.com/linux-syscall-support",
                "Google LLC",
                "BSD 3-Clause \"New\" or \"Revised\" License"
            )
        )
        ret + Licenses.list.map {
            LicenseNotice(it.uniqueId, it.website!!, it.getAuthor(), it.licenses.first())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isInHostProcess) {
            setHasOptionsMenu(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (isInHostProcess) {
            inflater.inflate(R.menu.host_about_fragment_options, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_show_config_v2_activity -> {
                val intent = Intent(requireContext(), ConfigV2Activity::class.java)
                startActivity(intent)
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun copyText(str: String) {
        if (str.isNotEmpty()) {
            val ctx = requireContext()
            val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("text", str)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(ctx, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }
    }

    private fun noticeToUiItem(notice: LicenseNotice) = TextListItem(
        title = notice.name,
        summary = notice.license + ", " + notice.copyright,
        onClick = {
            openUrl(notice.url)
        }
    )

    private data class LicenseNotice(
        val name: String,
        val url: String,
        val copyright: String,
        val license: String
    )
}
