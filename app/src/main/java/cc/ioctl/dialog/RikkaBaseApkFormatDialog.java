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
package cc.ioctl.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import cc.ioctl.util.HostInfo;
import cc.ioctl.hook.file.BaseApk;
import io.github.qauxv.BuildConfig;
import io.github.qauxv.R;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.ui.CustomDialog;
import io.github.qauxv.util.Toasts;
import java.util.Locale;

public class RikkaBaseApkFormatDialog {

    private static final String DEFAULT_BASE_APK_REGEX = ".*\\.apk";
    private static final String DEFAULT_BASE_APK_FORMAT = "%n_%v.APK";

    private static final String rq_base_apk_regex = "rq_base_apk_regex";
    private static final String rq_base_apk_format = "rq_base_apk_format";
    private static final String rq_base_apk_enabled = "rq_base_apk_enabled";

    @Nullable
    private AlertDialog dialog;
    @Nullable
    private LinearLayout vg;

    private String currentRegex;
    private String currentFormat;
    private boolean enableBaseApk;

    public static boolean IsEnabled() {
        return ConfigManager.getDefaultConfig().getBooleanOrFalse(rq_base_apk_enabled);
    }

    @Nullable
    public static String getCurrentBaseApkRegex() {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        if (cfg.getBooleanOrFalse(rq_base_apk_enabled)) {
            String val = cfg.getString(rq_base_apk_regex);
            if (val == null) {
                val = DEFAULT_BASE_APK_REGEX;
            }
            return val;
        }
        return null;
    }

    @Nullable
    public static String getCurrentBaseApkFormat() {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        if (cfg.getBooleanOrFalse(rq_base_apk_enabled)) {
            String val = cfg.getString(rq_base_apk_format);
            if (val == null) {
                val = DEFAULT_BASE_APK_FORMAT;
            }
            return val;
        }
        return null;
    }

    @SuppressLint("InflateParams")
    public void showDialog(@NonNull Context context) {
        dialog = (AlertDialog) CustomDialog.createFailsafe(context).setTitle("BaseApk")
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null).create();
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        final Context ctx = dialog.getContext();
        vg = (LinearLayout) LayoutInflater.from(ctx).inflate(R.layout.rikka_base_apk_dialog, null);
        final TextView preview = vg.findViewById(R.id.textViewBaseApkPreview);
        final TextView regex = vg.findViewById(R.id.editTextBaseApkRegex);
        final TextView format = vg.findViewById(R.id.editTextBaseApkFormat);
        final CheckBox enable = vg.findViewById(R.id.checkBoxEnableBaseApk);
        final LinearLayout panel = vg.findViewById(R.id.layoutBaseApkPanel);
        enableBaseApk = ConfigManager.getDefaultConfig().getBooleanOrFalse(rq_base_apk_enabled);
        enable.setChecked(enableBaseApk);
        panel.setVisibility(enableBaseApk ? View.VISIBLE : View.GONE);
        currentRegex = ConfigManager.getDefaultConfig().getString(rq_base_apk_regex);
        if (currentRegex == null) {
            currentRegex = DEFAULT_BASE_APK_REGEX;
        }
        currentFormat = ConfigManager.getDefaultConfig().getString(rq_base_apk_format);
        if (currentFormat == null) {
            currentFormat = DEFAULT_BASE_APK_FORMAT;
        }
        regex.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                currentRegex = s.toString();
            }
        });
        format.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String format = s.toString();
                currentFormat = format;
                String result = format
                        .replace("%n", "QAuxiliary")
                        .replace("%p", HostInfo.PACKAGE_NAME_SELF)
                        .replace("%v", BuildConfig.VERSION_NAME)
                        .replace("%c", String.valueOf(BuildConfig.VERSION_CODE));
                if (!format.toLowerCase(Locale.ROOT).contains(".apk")) {
                    result += "\n提示:你还没有输入.apk后缀哦";
                }
                preview.setText(result);
            }
        });
        regex.setText(currentRegex);
        format.setText(currentFormat);
        enable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableBaseApk = isChecked;
            panel.setVisibility(enableBaseApk ? View.VISIBLE : View.GONE);
        });
        dialog.setView(vg);
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v1 -> {
                    ConfigManager cfg = ConfigManager.getDefaultConfig();
                    boolean done = false;
                    if (!enableBaseApk) {
                        cfg.putBoolean(rq_base_apk_enabled, false);
                        done = true;
                    } else {
                        if (currentFormat != null && !currentFormat.isEmpty() &&
                                currentRegex != null && !currentRegex.isEmpty() &&(
                                currentFormat.contains("%n") || currentFormat.contains("%p"))) {
                            cfg.putBoolean(rq_base_apk_enabled, true);
                            cfg.putString(rq_base_apk_regex, currentRegex);
                            cfg.putString(rq_base_apk_format, currentFormat);
                            done = true;
                        } else {
                            Toasts.error(ctx, "请输入一个有效的格式");
                        }
                    }
                    if (done) {
                        cfg.save();
                        dialog.dismiss();
                        if (enableBaseApk) {
                            BaseApk hook = BaseApk.INSTANCE;
                            if (!hook.isInitialized()) {
                                hook.initialize();
                            }
                        }
                    }
                });
    }

    public boolean isEnabled() {
        return ConfigManager.getDefaultConfig().getBooleanOrFalse(rq_base_apk_enabled);
    }

    public String getName() {
        return "群上传重命名base.apk";
    }
}
