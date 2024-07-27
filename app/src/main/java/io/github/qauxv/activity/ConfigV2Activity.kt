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
package io.github.qauxv.activity

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cc.ioctl.fragment.DebugTestFragment
import cc.ioctl.fragment.JunkCodeFragment
import cc.ioctl.fragment.Pcm2SilkTestFragment
import cc.ioctl.util.HostInfo
import io.github.libxposed.service.XposedService
import io.github.qauxv.BuildConfig
import io.github.qauxv.R
import io.github.qauxv.activity.SettingsUiFragmentHostActivity.Companion.startActivityForFragment
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.config.SafeModeManager
import io.github.qauxv.databinding.MainV2NormalBinding
import io.github.qauxv.fragment.AboutFragment
import io.github.qauxv.fragment.CheckAbiVariantFragment
import io.github.qauxv.fragment.CheckAbiVariantModel
import io.github.qauxv.lifecycle.JumpActivityEntryHook
import io.github.qauxv.util.PackageConstants
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.UiThread
import io.github.qauxv.util.hookstatus.AbiUtils
import io.github.qauxv.util.hookstatus.HookStatus
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.ketal.ui.activity.QFileShareToIpadActivity
import me.ketal.util.getEnable
import me.ketal.util.setEnable
import name.mikanoshi.customiuizer.holidays.HolidayHelper
import name.mikanoshi.customiuizer.utils.Helpers
import name.mikanoshi.customiuizer.utils.Helpers.Holidays
import xyz.nextalone.util.SystemServiceUtils.copyToClipboard

class ConfigV2Activity : AppCompatTransferActivity() {

    private var mainV2Binding: MainV2NormalBinding? = null
    private var mHintLongPressed = false

    private val mHostAppPackages = setOf(
        PackageConstants.PACKAGE_NAME_QQ,
        PackageConstants.PACKAGE_NAME_TIM,
        PackageConstants.PACKAGE_NAME_QQ_LITE,
        PackageConstants.PACKAGE_NAME_QQ_HD,
        // not add QQ international, is it still usable now?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        if (HostInfo.isInHostProcess()) {
            // we have to set the theme before super.onCreate()
            setTheme(if (currentV2Theme == 3) R.style.Theme_MaiTungTMDesign_Light_Blue else R.style.Theme_MaiTungTMDesign_DayNight)
        } else {
            // we are in module process
            applyV2Theme(currentV2Theme, false)
        }
        // if in host process, it should already be done by last activity
        super.onCreate(savedInstanceState)
        if (R.string.res_inject_success ushr 24 == 0x7f) {
            throw AssertionError("package id must NOT be 0x7f")
        }
        val cmd = intent.getStringExtra(QFileShareToIpadActivity.SEND_TO_IPAD_CMD)
        if (QFileShareToIpadActivity.ENABLE_SEND_TO_IPAD == cmd) {
            val enabled = intent.getBooleanExtra(QFileShareToIpadActivity.ENABLE_SEND_TO_IPAD_STATUS, false)
            val componentName = ComponentName(this, QFileShareToIpadActivity::class.java)
            componentName.setEnable(this, enabled)
            finish()
        }
        HookStatus.init(this)
        if (currentV2Theme == 3) {
            // MaiTung light blue
            val root = LayoutInflater.from(this).inflate(R.layout.main_v2_light_blue, null) as ViewGroup
            mainV2Binding = MainV2NormalBinding.bind(root)
        } else {
            mainV2Binding = MainV2NormalBinding.inflate(LayoutInflater.from(this))
        }
        setContentView(mainV2Binding!!.root)
        setSupportActionBar(mainV2Binding!!.topAppBar)
        requestTranslucentStatusBar()
        HolidayHelper.setup(this)
        updateActivationStatus()
        SyncUtils.postDelayed(3000) { this.updateActivationStatus() }
        mainV2Binding!!.mainV2Help.setOnLongClickListener { v: View? ->
            if (!mHintLongPressed) {
                mHintLongPressed = true
            } else {
                startActivityForFragment(this, JunkCodeFragment::class.java, null)
            }
            true
        }
        mainV2Binding!!.mainV2Troubleshoot.setOnLongClickListener { v: View? ->
            startActivityForFragment(this, DebugTestFragment::class.java, null)
            true
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                HookStatus.getXposedService().onEach {
                    updateActivationStatus()
                }
            }
        }
    }

    fun updateActivationStatus() {
        val isHookEnabledByLegacyApi = HookStatus.isModuleEnabled() || HostInfo.isInHostProcess()
        val xposedService: XposedService? = HookStatus.getXposedService().value
        val isHookEnabledByLibXposedApi = if (xposedService != null) {
            val scope = xposedService.scope.toSet()
            // check intersection
            mHostAppPackages.intersect(scope).isNotEmpty()
        } else false
        val isHookEnabled = isHookEnabledByLegacyApi || isHookEnabledByLibXposedApi
        var isAbiMatch = CheckAbiVariantModel.collectAbiInfo(this).isAbiMatch
        if ((isHookEnabled && HostInfo.isInModuleProcess() && !HookStatus.isZygoteHookMode()
                && HookStatus.isTaiChiInstalled(this)) && HookStatus.getHookType() == HookStatus.HookType.APP_PATCH && "armAll" != AbiUtils.getModuleFlavorName()
        ) {
            isAbiMatch = false
        }
        val frameStatus = mainV2Binding!!.mainV2ActivationStatusLinearLayout
        val frameIcon = mainV2Binding!!.mainV2ActivationStatusIcon
        val statusTitle = mainV2Binding!!.mainV2ActivationStatusTitle
        val tvStatus = mainV2Binding!!.mainV2ActivationStatusDesc
        val tvInsVersion = mainV2Binding!!.mainTextViewVersion
        if (isAbiMatch) {
            frameStatus.background = ResourcesCompat.getDrawable(
                resources,
                if ((isHookEnabled && Helpers.currentHoliday != Holidays.LUNARNEWYEAR)
                ) R.drawable.bg_green_solid else R.drawable.bg_red_solid, theme
            )
            frameIcon.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    if (isHookEnabled) R.drawable.ic_success_white else R.drawable.ic_failure_white, theme
                )
            )
            statusTitle.text = if (isHookEnabled) "已激活" else "未激活"
            if (HostInfo.isInHostProcess()) {
                tvStatus.text = HostInfo.getPackageName()
            } else {
                tvStatus.text = if (isHookEnabledByLibXposedApi) {
                    val xp = xposedService!!
                    xp.frameworkName + " " + xp.frameworkVersion + " (" + xp.frameworkVersionCode + "), API " + xp.apiVersion
                } else {
                    HookStatus.getHookProviderNameForLegacyApi()
                }
            }
        } else {
            frameStatus.background = ResourcesCompat.getDrawable(resources, R.drawable.bg_yellow_solid, theme)
            frameIcon.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_info_white, theme))
            statusTitle.text = if (isHookEnabled) "未完全激活" else "未激活"
            tvStatus.text = "点击处理"
            frameStatus.setOnClickListener { v: View? -> startActivityForFragment(this, CheckAbiVariantFragment::class.java, null) }
        }
        tvInsVersion.text = BuildConfig.VERSION_NAME
    }

    fun openModuleSettingForHost(view: View) {
        var pkg: String? = null
        val id = view.id
        if (id == R.id.mainRelativeLayoutButtonOpenQQ) {
            pkg = PackageConstants.PACKAGE_NAME_QQ
        } else if (id == R.id.mainRelativeLayoutButtonOpenTIM) {
            pkg = PackageConstants.PACKAGE_NAME_TIM
        } else if (id == R.id.mainRelativeLayoutButtonOpenQQLite) {
            pkg = PackageConstants.PACKAGE_NAME_QQ_LITE
        }
        if (pkg != null) {
            val intent = Intent()
            intent.setComponent(ComponentName(pkg, "com.tencent.mobileqq.activity.JumpActivity"))
            intent.setAction(Intent.ACTION_VIEW)
            intent.putExtra(JumpActivityEntryHook.JUMP_ACTION_CMD, JumpActivityEntryHook.JUMP_ACTION_SETTING_ACTIVITY)
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                AlertDialog.Builder(this).setTitle("出错啦")
                    .setMessage("拉起模块设置失败, 请确认 $pkg 已安装并启用(没有被关冰箱或被冻结停用)\n$e")
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
    }

    fun handleClickEvent(v: View) {
        val id = v.id
        if (id == R.id.mainV2_githubRepo) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setData(Uri.parse("https://github.com/cinit/QAuxiliary"))
            startActivity(intent)
        } else if (id == R.id.mainV2_help) {
            AlertDialog.Builder(this)
                .setMessage(
                    "如模块无法使用，EdXposed 用户可尝试取消优化+开启兼容模式  "
                        + "root 用户可尝试 用幸运破解器-工具箱-移除 odex 更改 移除 QQ/TIM 的优化, 太极用户请尝试取消优化"
                )
                .setCancelable(true).setPositiveButton(android.R.string.ok, null).show()
        } else if (id == R.id.mainV2_troubleshoot) {
            AlertDialog.Builder(this)
                .setTitle("你想要进入哪个App的故障排除")
                .setItems(arrayOf("QQ", "TIM", "QQ极速版", "QQ HD")) { dialog: DialogInterface?, which: Int ->
                    var pkg: String? = null
                    when (which) {
                        0 -> pkg = PackageConstants.PACKAGE_NAME_QQ
                        1 -> pkg = PackageConstants.PACKAGE_NAME_TIM
                        2 -> pkg = PackageConstants.PACKAGE_NAME_QQ_LITE
                        3 -> pkg = PackageConstants.PACKAGE_NAME_QQ_HD
                        else -> {}
                    }
                    if (pkg != null) {
                        val intent = Intent()
                        intent.setComponent(ComponentName(pkg, "com.tencent.mobileqq.activity.JumpActivity"))
                        intent.setAction(Intent.ACTION_VIEW)
                        intent.putExtra(
                            JumpActivityEntryHook.JUMP_ACTION_CMD,
                            JumpActivityEntryHook.JUMP_ACTION_TROUBLE_SHOOTING_ACTIVITY
                        )
                        try {
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            AlertDialog.Builder(this).setTitle("出错啦")
                                .setMessage("拉起模块设置失败, 请确认 $pkg 已安装并启用(没有被关冰箱或被冻结停用)\n$e")
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                        }
                    }
                }
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton("无法进入？") { dialog: DialogInterface?, which: Int ->
                    AlertDialog.Builder(this).setTitle("手动启用安全模式")
                        .setMessage(
                            """
    如果模块已经激活但无法进入故障排除界面，或在点击进入故障排除后卡死，你可以手动在以下位置建立一个空文件来强制启用 QAuxiliary 的安全模式。

    ${Environment.getExternalStorageDirectory().absolutePath}/Android/data/包名(例如 QQ 是 com.tencent.mobileqq)/${SafeModeManager.SAFE_MODE_FILE_NAME}

    请注意这个位置在 Android 11 及以上的系统是无法直接访问的，你可以使用一些支持访问 Android/data 的第三方文件管理器来操作，例如 MT 管理器。
    """.trimIndent()
                        )
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton("复制文件名") { dialog1: DialogInterface?, which1: Int ->
                            copyToClipboard(this, SafeModeManager.SAFE_MODE_FILE_NAME)
                            Toasts.info(this, "复制成功")
                        }.show()
                }.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        if (HostInfo.isInModuleProcess()) {
            menuInflater.inflate(R.menu.main_v2_toolbar, menu)
            updateMenuItems()
        } else {
            menuInflater.inflate(R.menu.host_main_v2_options, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.menu_item_nativeLibVariantInfo) {
            startActivityForFragment(this, CheckAbiVariantFragment::class.java, null)
        } else if (id == R.id.menu_item_about) {
            startActivityForFragment(this, AboutFragment::class.java, null)
        } else if (id == R.id.menu_item_test_pcm2silk) {
            startActivityForFragment(this, Pcm2SilkTestFragment::class.java, null)
        } else if (id == R.id.mainV2_menuItem_toggleDesktopIcon) {
            isLauncherIconEnabled = !isLauncherIconEnabled
            SyncUtils.postDelayed({ this.updateMenuItems() }, 500)
        } else if (id == R.id.menu_item_changeTheme) {
            showChangeThemeDialog()
        } else if (id == R.id.menu_item_switch_to_module_process) {
            val intent = Intent()
            intent.setComponent(ComponentName(BuildConfig.APPLICATION_ID, ConfigV2Activity::class.java.name))
            intent.setAction(Intent.ACTION_MAIN)
            intent.addCategory("de.robv.android.xposed.category.MODULE_SETTINGS")
            try {
                startActivity(intent)
                finish()
            } catch (e: ActivityNotFoundException) {
                AlertDialog.Builder(this).setTitle("出错啦")
                    .setMessage(
                        """拉起模块失败, 请确认 ${BuildConfig.APPLICATION_ID} 已安装并启用(没有被关冰箱或被冻结停用)
$e"""
                    )
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        } else {
            return super@ConfigV2Activity.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        updateMenuItems()
        updateActivationStatus()
        HolidayHelper.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        HolidayHelper.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        HolidayHelper.onPause()
    }

    private fun showChangeThemeDialog() {
        val themes = arrayOf("系统默认", "深色", "浅色", "浅蓝限定")
        AlertDialog.Builder(this)
            .setTitle("更换主题")
            .setItems(themes) { dialog: DialogInterface?, which: Int ->
                saveCurrentV2Theme(which)
                applyV2Theme(which, true)
            }
            .show()
    }

    private val currentV2Theme: Int
        get() = ConfigManager.getDefaultConfig().getIntOrDefault("KEY_DAY_NIGHT_STATUS", 0)

    private fun saveCurrentV2Theme(i: Int) {
        ConfigManager.getDefaultConfig().putInt("KEY_DAY_NIGHT_STATUS", i)
    }

    private fun applyV2Theme(i: Int, allowRecreate: Boolean) {
        when (i) {
            0 -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                if (allowRecreate) {
                    recreate()
                } else {
                    // just set theme
                    setTheme(R.style.Theme_MaiTungTMDesign_DayNight)
                }
            }

            1 -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                if (allowRecreate) {
                    recreate()
                } else {
                    // just set theme
                    setTheme(R.style.Theme_MaiTungTMDesign_DayNight)
                }
            }

            2 -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                if (allowRecreate) {
                    recreate()
                } else {
                    // just set theme
                    setTheme(R.style.Theme_MaiTungTMDesign_DayNight)
                }
            }

            3 -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                // apply MaiTungTM light blue theme requires a recreate
                if (allowRecreate) {
                    recreate()
                } else {
                    // just set theme
                    setTheme(R.style.Theme_MaiTungTMDesign_Light_Blue)
                }
            }

            else -> {}
        }
    }

    fun updateMenuItems() {
        if (HostInfo.isInHostProcess()) {
            return
        }
        val menu = mainV2Binding!!.topAppBar.menu
        if (menu != null) {
            menu.removeItem(R.id.mainV2_menuItem_toggleDesktopIcon)
            menu.add(
                Menu.CATEGORY_SYSTEM, R.id.mainV2_menuItem_toggleDesktopIcon, 0,
                if (isLauncherIconEnabled) "隐藏桌面图标" else "显示桌面图标"
            )
        }
    }

    @set:UiThread
    @get:UiThread
    var isLauncherIconEnabled: Boolean
        get() {
            val componentName = ComponentName(this, ALIAS_ACTIVITY_NAME)
            return componentName.getEnable(this)
        }
        set(enabled) {
            val componentName = ComponentName(this, ALIAS_ACTIVITY_NAME)
            componentName.setEnable(this, enabled)
        }

    companion object {
        private const val ALIAS_ACTIVITY_NAME = "io.github.qauxv.activity.ConfigV2ActivityAlias"
    }
}
