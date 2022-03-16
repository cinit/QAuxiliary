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
package io.github.qauxv.activity;

import static me.ketal.ui.activity.QFileShareToIpadActivity.ENABLE_SEND_TO_IPAD;
import static me.ketal.ui.activity.QFileShareToIpadActivity.ENABLE_SEND_TO_IPAD_STATUS;
import static me.ketal.ui.activity.QFileShareToIpadActivity.SEND_TO_IPAD_CMD;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.system.Os;
import android.system.StructUtsname;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.res.ResourcesCompat;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.BuildConfig;
import io.github.qauxv.R;
import io.github.qauxv.SyncUtils;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.databinding.MainV2NormalBinding;
import io.github.qauxv.fragment.AboutFragment;
import io.github.qauxv.lifecycle.JumpActivityEntryHook;
import io.github.qauxv.startup.HookEntry;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.Natives;
import io.github.qauxv.util.UiThread;
import io.github.qauxv.util.hookstatus.AbiUtils;
import io.github.qauxv.util.hookstatus.HookStatus;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import me.ketal.ui.activity.QFileShareToIpadActivity;
import me.ketal.util.ComponentUtilKt;
import name.mikanoshi.customiuizer.holidays.HolidayHelper;
import name.mikanoshi.customiuizer.utils.Helpers;
import name.mikanoshi.customiuizer.utils.Helpers.Holidays;

public class ConfigV2Activity extends AppCompatTransferActivity {

    private static final String ALIAS_ACTIVITY_NAME = "io.github.qauxv.activity.ConfigV2ActivityAlias";
    private MainV2NormalBinding mainV2Binding = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (HostInfo.isInHostProcess()) {
            // we have to set the theme before super.onCreate()
            setTheme(getCurrentV2Theme() == 3 ? R.style.Theme_MaiTungTMDesign_Light_Blue : R.style.Theme_MaiTungTMDesign_DayNight);
        } else {
            // we is in module process
            applyV2Theme(getCurrentV2Theme(), false);
        }
        // if in host process, it should already be done by last activity
        super.onCreate(savedInstanceState);
        if (R.string.res_inject_success >>> 24 == 0x7f) {
            throw new RuntimeException("package id must NOT be 0x7f");
        }
        String cmd = getIntent().getStringExtra(SEND_TO_IPAD_CMD);
        if (ENABLE_SEND_TO_IPAD.equals(cmd)) {
            boolean enabled = getIntent().getBooleanExtra(ENABLE_SEND_TO_IPAD_STATUS, false);
            ComponentName componentName = new ComponentName(this, QFileShareToIpadActivity.class);
            ComponentUtilKt.setEnable(componentName, this, enabled);
            finish();
        }
        HookStatus.init(this);
        if (getCurrentV2Theme() == 3) {
            // MaiTung light blue
            ViewGroup root = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.main_v2_light_blue, null);
            mainV2Binding = MainV2NormalBinding.bind(root);
        } else {
            mainV2Binding = MainV2NormalBinding.inflate(LayoutInflater.from(this));
        }
        setContentView(mainV2Binding.getRoot());
        setSupportActionBar(mainV2Binding.topAppBar);
        requestTranslucentStatusBar();
        HolidayHelper.setup(this);
        updateActivationStatus();
        SyncUtils.postDelayed(3000, this::updateActivationStatus);
    }

    public void updateActivationStatus() {
        boolean isHookEnabled = HookStatus.isModuleEnabled();
        boolean isAbiMatch = HookStatus.checkABI();
        LinearLayout frameStatus = mainV2Binding.mainV2ActivationStatusLinearLayout;
        ImageView frameIcon = mainV2Binding.mainV2ActivationStatusIcon;
        TextView statusTitle = mainV2Binding.mainV2ActivationStatusTitle;
        TextView tvStatus = mainV2Binding.mainV2ActivationStatusDesc;
        TextView tvInsVersion = mainV2Binding.mainTextViewVersion;
        if (isAbiMatch) {
            frameStatus.setBackground(ResourcesCompat.getDrawable(getResources(),
                    (isHookEnabled && Helpers.currentHoliday != Holidays.LUNARNEWYEAR)
                            ? R.drawable.bg_green_solid : R.drawable.bg_red_solid, getTheme()));
            frameIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                    isHookEnabled ? R.drawable.ic_success_white :
                            R.drawable.ic_failure_white, getTheme()));
            statusTitle.setText(isHookEnabled ? "已激活" : "未激活");
            tvStatus.setText(HookStatus.getHookProviderName());
        } else {
            frameStatus.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_yellow_solid, getTheme()));
            frameIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_info_white, getTheme()));
            statusTitle.setText(isHookEnabled ? "未完全激活" : "未激活");
            tvStatus.setText("点击处理");
            frameStatus.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("模块原生库与宿主不匹配");
                StringBuilder message = new StringBuilder("当前模块使用的原生库为 " + BuildConfig.FLAVOR);
                if ((AbiUtils.archStringToArchInt(Os.uname().machine) & (AbiUtils.ABI_X86 | AbiUtils.ABI_X86_64)) != 0) {
                    message.append("\n").append("当前系统 uname machine 为 ").append(Os.uname().machine);
                }
                for (String scope : HookStatus.getHostABI().keySet()) {
                    String abi = HookStatus.getHostABI().get(scope);
                    assert abi != null;
                    String requiredVariant = AbiUtils.getSuggestedAbiVariant(AbiUtils.archStringToArchInt(abi));
                    message.append("\n").append(scope).append(" 需要模块使用的原生库为 ").append(requiredVariant);
                }
                message.append("\n\n").append("推荐您将模块更换为使用 ").append(getSuggestedAbiVariant()).append(" 原生库的版本");
                builder.setMessage(message.toString());
                builder.setPositiveButton("去 Telegram 频道下载", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://t.me/QAuxiliary"));
                    startActivity(intent);
                });
                builder.setNegativeButton("去 GitHub 下载", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://github.com/cinit/QAuxiliary/releases/latest"));
                    startActivity(intent);
                });
                builder.setNeutralButton(android.R.string.cancel, null);
                builder.setCancelable(true);
                builder.show();
            });
        }
        tvInsVersion.setText(BuildConfig.VERSION_NAME);
    }

    public void openModuleSettingForHost(View view) {
        String pkg = null;
        switch (view.getId()) {
            case R.id.mainRelativeLayoutButtonOpenQQ: {
                pkg = HookEntry.PACKAGE_NAME_QQ;
                break;
            }
            case R.id.mainRelativeLayoutButtonOpenTIM: {
                pkg = HookEntry.PACKAGE_NAME_TIM;
                break;
            }
            case R.id.mainRelativeLayoutButtonOpenQQLite: {
                pkg = HookEntry.PACKAGE_NAME_QQ_LITE;
                break;
            }
            default: {
            }
        }
        if (pkg != null) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(pkg, "com.tencent.mobileqq.activity.JumpActivity"));
            intent.setAction(Intent.ACTION_VIEW);
            intent.putExtra(JumpActivityEntryHook.JUMP_ACTION_CMD, JumpActivityEntryHook.JUMP_ACTION_SETTING_ACTIVITY);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                new AlertDialog.Builder(this).setTitle("出错啦")
                        .setMessage("拉起模块设置失败, 请确认 " + pkg + " 已安装并启用(没有被关冰箱或被冻结停用)\n" + e)
                        .setCancelable(true).setPositiveButton(android.R.string.ok, null).show();
            }
        }
    }

    public void handleClickEvent(View v) {
        switch (v.getId()) {
            case R.id.mainV2_githubRepo: {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://github.com/cinit/QAuxiliary"));
                startActivity(intent);
                break;
            }
            case R.id.mainV2_help: {
                new AlertDialog.Builder(this)
                        .setMessage("如模块无法使用，EdXp可尝试取消优化+开启兼容模式  "
                                + "ROOT用户可尝试 用幸运破解器-工具箱-移除odex更改 移除QQ与本模块的优化, 太极尝试取消优化")
                        .setCancelable(true).setPositiveButton(android.R.string.ok, null).show();
                break;
            }
            case R.id.mainV2_troubleshoot: {
                new AlertDialog.Builder(this)
                        .setTitle("你想要进入哪个App的故障排除")
                        .setItems(new String[]{"QQ", "TIM", "QQ极速版"}, (dialog, which) -> {
                            String pkg = null;
                            switch (which) {
                                case 0: {
                                    pkg = HookEntry.PACKAGE_NAME_QQ;
                                    break;
                                }
                                case 1: {
                                    pkg = HookEntry.PACKAGE_NAME_TIM;
                                    break;
                                }
                                case 2: {
                                    pkg = HookEntry.PACKAGE_NAME_QQ_LITE;
                                    break;
                                }
                                default: {
                                }
                            }
                            if (pkg != null) {
                                Intent intent = new Intent();
                                intent.setComponent(new ComponentName(pkg, "com.tencent.mobileqq.activity.JumpActivity"));
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.putExtra(JumpActivityEntryHook.JUMP_ACTION_CMD, JumpActivityEntryHook.JUMP_ACTION_START_ACTIVITY);
                                intent.putExtra(JumpActivityEntryHook.JUMP_ACTION_TARGET, SettingsUiFragmentHostActivity.class.getName());
                                try {
                                    startActivity(intent);
                                } catch (ActivityNotFoundException e) {
                                    new AlertDialog.Builder(this).setTitle("出错啦")
                                            .setMessage("拉起模块设置失败, 请确认 " + pkg + " 已安装并启用(没有被关冰箱或被冻结停用)\n" + e.toString())
                                            .setCancelable(true).setPositiveButton(android.R.string.ok, null).show();
                                }
                            }
                        })
                        .setPositiveButton(android.R.string.ok, null).show();
                break;
            }
            default: {
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (HostInfo.isInModuleProcess()) {
            getMenuInflater().inflate(R.menu.main_v2_toolbar, menu);
        } else {
            getMenuInflater().inflate(R.menu.host_main_v2_options, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_debugInfo: {
                new AlertDialog.Builder(ConfigV2Activity.this)
                        .setTitle("调试信息").setPositiveButton(android.R.string.ok, null)
                        .setMessage(getDebugInfo()).show();
                return true;
            }
            case R.id.menu_item_about: {
                SettingsUiFragmentHostActivity.startActivityForFragment(this, AboutFragment.class, null);
                return true;
            }
            case R.id.mainV2_menuItem_toggleDesktopIcon: {
                setLauncherIconEnabled(!isLauncherIconEnabled());
                SyncUtils.postDelayed(this::updateMenuItems, 500);
                return true;
            }
            case R.id.menu_item_changeTheme: {
                showChangeThemeDialog();
                return true;
            }
            case R.id.menu_item_switch_to_module_process: {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(BuildConfig.APPLICATION_ID, ConfigV2Activity.class.getName()));
                intent.setAction(Intent.ACTION_MAIN);
                intent.addCategory("de.robv.android.xposed.category.MODULE_SETTINGS");
                try {
                    startActivity(intent);
                    finish();
                } catch (ActivityNotFoundException e) {
                    new AlertDialog.Builder(this).setTitle("出错啦")
                            .setMessage("拉起模块失败, 请确认 " + BuildConfig.APPLICATION_ID + " 已安装并启用(没有被关冰箱或被冻结停用)\n" + e)
                            .setCancelable(true).setPositiveButton(android.R.string.ok, null).show();
                }
                return true;
            }
            default: {
                return ConfigV2Activity.super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMenuItems();
        HolidayHelper.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HolidayHelper.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        HolidayHelper.onPause();
    }

    private void showChangeThemeDialog() {
        String[] themes = new String[]{"系统默认", "深色", "浅色", "浅蓝限定"};
        new AlertDialog.Builder(this)
                .setTitle("更换主题")
                .setItems(themes, (dialog, which) -> {
                    saveCurrentV2Theme(which);
                    applyV2Theme(which, true);
                })
                .show();
    }

    private int getCurrentV2Theme() {
        return ConfigManager.getDefaultConfig().getIntOrDefault("KEY_DAY_NIGHT_STATUS", 0);
    }

    private void saveCurrentV2Theme(int i) {
        ConfigManager.getDefaultConfig().putInt("KEY_DAY_NIGHT_STATUS", i);
    }

    private void applyV2Theme(int i, boolean allowRecreate) {
        switch (i) {
            case 0: {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                if (allowRecreate) {
                    recreate();
                } else {
                    // just set theme
                    setTheme(R.style.Theme_MaiTungTMDesign_DayNight);
                }
                break;
            }
            case 1: {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                if (allowRecreate) {
                    recreate();
                } else {
                    // just set theme
                    setTheme(R.style.Theme_MaiTungTMDesign_DayNight);
                }
                break;
            }
            case 2: {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                if (allowRecreate) {
                    recreate();
                } else {
                    // just set theme
                    setTheme(R.style.Theme_MaiTungTMDesign_DayNight);
                }
                break;
            }
            case 3: {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                // apply MaiTungTM light blue theme requires a recreate
                if (allowRecreate) {
                    recreate();
                } else {
                    // just set theme
                    setTheme(R.style.Theme_MaiTungTMDesign_Light_Blue);
                }
                break;
            }
            default:
                break;
        }
    }

    void updateMenuItems() {
        if (HostInfo.isInHostProcess()) {
            return;
        }
        Menu menu = mainV2Binding.topAppBar.getMenu();
        if (menu != null) {
            menu.removeItem(R.id.mainV2_menuItem_toggleDesktopIcon);
            menu.add(Menu.CATEGORY_SYSTEM, R.id.mainV2_menuItem_toggleDesktopIcon, 0,
                    isLauncherIconEnabled() ? "隐藏桌面图标" : "显示桌面图标");
        }
    }

    boolean isLauncherIconEnabled() {
        ComponentName componentName = new ComponentName(this, ALIAS_ACTIVITY_NAME);
        return ComponentUtilKt.getEnable(componentName, this);
    }

    @UiThread
    void setLauncherIconEnabled(boolean enabled) {
        ComponentName componentName = new ComponentName(this, ALIAS_ACTIVITY_NAME);
        ComponentUtilKt.setEnable(componentName, this, enabled);
    }

    @Nullable
    private String getSuggestedAbiVariant() {
        StructUtsname uts = Os.uname();
        String sysAbi = uts.machine;
        HashSet<String> requestAbis = new HashSet<>();
        requestAbis.add(AbiUtils.archStringToLibDirName(sysAbi));
        String[] pkgList = new String[]{
                "com.tencent.mobileqq",
//                "com.tencent.mobileqqi",
                "com.tencent.qqlite",
                "com.tencent.tim",
        };
        for (String pkg : pkgList) {
            String activeAbi = AbiUtils.getApplicationActiveAbi(pkg);
            if (activeAbi == null) {
                continue;
            }
            String abi = AbiUtils.archStringToLibDirName(activeAbi);
            if (!requestAbis.contains(abi)) {
                requestAbis.add(abi);
            }
        }
        String[] modulesAbis = AbiUtils.queryModuleAbiList();
        HashSet<String> missingAbis = new HashSet<>();
        // check if modulesAbis contains all requestAbis
        for (String abi : requestAbis) {
            if (!Arrays.asList(modulesAbis).contains(abi)) {
                missingAbis.add(abi);
            }
        }
        if (missingAbis.isEmpty()) {
            return null;
        }
        int abi = 0;
        Log.d("missingAbis, " + Arrays.toString(missingAbis.toArray()));
        for (String name : missingAbis) {
            Log.d("missing abi: " + name);
            abi += AbiUtils.archStringToArchInt(name);
        }
        Log.d("abi: " + abi);
        return AbiUtils.getSuggestedAbiVariant(abi);
    }

    private String getDebugInfo() {
        String dbgInfo = "";
        try {
            long ts = BuildConfig.BUILD_TIMESTAMP;
            dbgInfo += "\nBuild Time: " + (ts > 0 ? new Date(ts).toString() : "unknown") + ", " +
                    "SUPPORTED_ABIS=" + Arrays.toString(Build.SUPPORTED_ABIS) + "\npageSize=" + Natives
                    .getpagesize();
        } catch (Throwable e) {
            dbgInfo += "\n" + e;
        }
        try {
            dbgInfo += "SystemClassLoader: " + ClassLoader.getSystemClassLoader()
                    + "\nActiveModuleVersion: " + BuildConfig.VERSION_NAME
                    + "\nThisVersion: " + BuildConfig.VERSION_NAME
                    + "\nProductFlavors: " + BuildConfig.FLAVOR;
        } catch (Throwable r) {
            dbgInfo += r;
        }
        return dbgInfo;
    }
}
