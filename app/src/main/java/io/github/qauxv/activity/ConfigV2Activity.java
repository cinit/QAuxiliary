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
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.res.ResourcesCompat;
import cc.ioctl.fragment.JunkCodeFragment;
import cc.ioctl.fragment.Pcm2SilkTestFragment;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.BuildConfig;
import io.github.qauxv.R;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.config.SafeModeManager;
import io.github.qauxv.databinding.MainV2NormalBinding;
import io.github.qauxv.fragment.AboutFragment;
import io.github.qauxv.fragment.CheckAbiVariantFragment;
import io.github.qauxv.fragment.CheckAbiVariantModel;
import io.github.qauxv.lifecycle.JumpActivityEntryHook;
import io.github.qauxv.startup.HookEntry;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.Toasts;
import io.github.qauxv.util.UiThread;
import io.github.qauxv.util.hookstatus.AbiUtils;
import io.github.qauxv.util.hookstatus.HookStatus;
import me.ketal.ui.activity.QFileShareToIpadActivity;
import me.ketal.util.ComponentUtilKt;
import name.mikanoshi.customiuizer.holidays.HolidayHelper;
import name.mikanoshi.customiuizer.utils.Helpers;
import name.mikanoshi.customiuizer.utils.Helpers.Holidays;
import xyz.nextalone.util.SystemServiceUtils;

public class ConfigV2Activity extends AppCompatTransferActivity {

    private static final String ALIAS_ACTIVITY_NAME = "io.github.qauxv.activity.ConfigV2ActivityAlias";
    private MainV2NormalBinding mainV2Binding = null;
    private boolean mHintLongPressed = false;

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
            throw new AssertionError("package id must NOT be 0x7f");
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
        mainV2Binding.mainV2Help.setOnLongClickListener(v -> {
            if (!mHintLongPressed) {
                mHintLongPressed = true;
            } else {
                SettingsUiFragmentHostActivity.startActivityForFragment(this, JunkCodeFragment.class, null);
            }
            return true;
        });
    }

    public void updateActivationStatus() {
        boolean isHookEnabled = HookStatus.isModuleEnabled() || HostInfo.isInHostProcess();
        boolean isAbiMatch = CheckAbiVariantModel.collectAbiInfo(this).isAbiMatch;
        if (isHookEnabled && HostInfo.isInModuleProcess() && !HookStatus.isZygoteHookMode()
                && HookStatus.isTaiChiInstalled(this)
                && HookStatus.getHookType() == HookStatus.HookType.APP_PATCH
                && !"armAll".equals(AbiUtils.getModuleFlavorName())) {
            isAbiMatch = false;
        }
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
            if (HostInfo.isInHostProcess()) {
                tvStatus.setText(HostInfo.getPackageName());
            } else {
                tvStatus.setText(HookStatus.getHookProviderName());
            }
        } else {
            frameStatus.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_yellow_solid, getTheme()));
            frameIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_info_white, getTheme()));
            statusTitle.setText(isHookEnabled ? "未完全激活" : "未激活");
            tvStatus.setText("点击处理");
            frameStatus.setOnClickListener(v -> SettingsUiFragmentHostActivity.startActivityForFragment(this, CheckAbiVariantFragment.class, null));
        }
        tvInsVersion.setText(BuildConfig.VERSION_NAME);
    }

    public void openModuleSettingForHost(View view) {
        String pkg = null;
        var id = view.getId();
        if (id == R.id.mainRelativeLayoutButtonOpenQQ) {
            pkg = HookEntry.PACKAGE_NAME_QQ;
        } else if (id == R.id.mainRelativeLayoutButtonOpenTIM) {
            pkg = HookEntry.PACKAGE_NAME_TIM;
        } else if (id == R.id.mainRelativeLayoutButtonOpenQQLite) {
            pkg = HookEntry.PACKAGE_NAME_QQ_LITE;
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
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        }
    }

    public void handleClickEvent(View v) {
        var id = v.getId();
        if (id == R.id.mainV2_githubRepo) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://github.com/cinit/QAuxiliary"));
            startActivity(intent);
        } else if (id == R.id.mainV2_help) {
            new AlertDialog.Builder(this)
                    .setMessage("如模块无法使用，EdXp可尝试取消优化+开启兼容模式  "
                            + "ROOT用户可尝试 用幸运破解器-工具箱-移除odex更改 移除QQ与本模块的优化, 太极尝试取消优化")
                    .setCancelable(true).setPositiveButton(android.R.string.ok, null).show();
        } else if (id == R.id.mainV2_troubleshoot) {
            new AlertDialog.Builder(this)
                    .setTitle("你想要进入哪个App的故障排除")
                    .setItems(new String[]{"QQ", "TIM", "QQ极速版", "QQ HD"}, (dialog, which) -> {
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
                            case 3: {
                                pkg = HookEntry.PACKAGE_NAME_QQ_HD;
                                break;
                            }
                            default: {
                            }
                        }
                        if (pkg != null) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName(pkg, "com.tencent.mobileqq.activity.JumpActivity"));
                            intent.setAction(Intent.ACTION_VIEW);
                            intent.putExtra(JumpActivityEntryHook.JUMP_ACTION_CMD,
                                    JumpActivityEntryHook.JUMP_ACTION_TROUBLE_SHOOTING_ACTIVITY);
                            try {
                                startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                new AlertDialog.Builder(this).setTitle("出错啦")
                                        .setMessage("拉起模块设置失败, 请确认 " + pkg + " 已安装并启用(没有被关冰箱或被冻结停用)\n" + e)
                                        .setPositiveButton(android.R.string.ok, null)
                                        .show();
                            }
                        }
                    })
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton("无法进入？", (dialog, which) -> {
                        new AlertDialog.Builder(this).setTitle("手动启用安全模式")
                                .setMessage("如果模块已经激活但无法进入故障排除界面，或在点击进入故障排除后卡死，"
                                        + "你可以手动在以下位置建立一个空文件来强制启用 QAuxiliary 的安全模式。\n\n" +
                                        Environment.getExternalStorageDirectory().getAbsolutePath() +
                                        "/Android/data/包名(例如 QQ 是 com.tencent.mobileqq)/" +
                                        SafeModeManager.SAFE_MODE_FILE_NAME + "\n\n"
                                        + "请注意这个位置在 Android 11 及以上的系统是无法直接访问的，"
                                        + "你可以使用一些支持访问 Android/data 的第三方文件管理器来操作，例如 MT 管理器。")
                                .setPositiveButton(android.R.string.ok, null)
                                .setNegativeButton("复制文件名", (dialog1, which1) -> {
                                    SystemServiceUtils.copyToClipboard(this, SafeModeManager.SAFE_MODE_FILE_NAME);
                                    Toasts.info(this, "复制成功");
                                }).show();
                    }).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (HostInfo.isInModuleProcess()) {
            getMenuInflater().inflate(R.menu.main_v2_toolbar, menu);
            updateMenuItems();
        } else {
            getMenuInflater().inflate(R.menu.host_main_v2_options, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        var id = item.getItemId();
        if (id == R.id.menu_item_nativeLibVariantInfo) {
            SettingsUiFragmentHostActivity.startActivityForFragment(this, CheckAbiVariantFragment.class, null);
        } else if (id == R.id.menu_item_about) {
            SettingsUiFragmentHostActivity.startActivityForFragment(this, AboutFragment.class, null);
        } else if (id == R.id.menu_item_test_pcm2silk) {
            SettingsUiFragmentHostActivity.startActivityForFragment(this, Pcm2SilkTestFragment.class, null);
        } else if (id == R.id.mainV2_menuItem_toggleDesktopIcon) {
            setLauncherIconEnabled(!isLauncherIconEnabled());
            SyncUtils.postDelayed(this::updateMenuItems, 500);
        } else if (id == R.id.menu_item_changeTheme) {
            showChangeThemeDialog();
        } else if (id == R.id.menu_item_switch_to_module_process) {
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
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        } else {
            return ConfigV2Activity.super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMenuItems();
        updateActivationStatus();
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
}
