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
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.rymmmmm.hook.CustomMsgTimeFormat;
import io.github.qauxv.R;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.ui.CustomDialog;
import io.github.qauxv.util.Toasts;
import java.io.File;

public class RikkaCustomSplash {

    private static final String DEFAULT_SPLASH_PATH = "";

    private static final String rq_splash_path = "rq_splash_path";
    private static final String rq_splash_enabled = "rq_splash_enabled";

    @Nullable
    private AlertDialog dialog;
    @Nullable
    private LinearLayout vg;

    private String currentPath;
    private boolean enableSplash;

    public static boolean IsEnabled() {
        return ConfigManager.getDefaultConfig().getBooleanOrFalse(rq_splash_enabled);
    }

    @Nullable
    public static String getCurrentSplashPath() {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        if (cfg.getBooleanOrFalse(rq_splash_enabled)) {
            String val = cfg.getString(rq_splash_path);
            if (val == null) {
                val = DEFAULT_SPLASH_PATH;
            }
            return val;
        }
        return null;
    }

    @SuppressLint("InflateParams")
    public void showDialog(@NonNull Context context) {
        dialog = (AlertDialog) CustomDialog.createFailsafe(context).setTitle("自定义启动图")
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null).create();
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        final Context ctx = dialog.getContext();
        vg = (LinearLayout) LayoutInflater.from(ctx)
                .inflate(R.layout.rikka_select_splash_dialog, null);
        final TextView input = vg.findViewById(R.id.selectSplash_editTextPicLocation);
        final CheckBox enable = vg.findViewById(R.id.checkBoxEnableCustomStartupPic);
        final RelativeLayout panel = vg.findViewById(R.id.layoutSplashPanel);
        enableSplash = ConfigManager.getDefaultConfig().getBooleanOrFalse(rq_splash_enabled);
        enable.setChecked(enableSplash);
        panel.setVisibility(enableSplash ? View.VISIBLE : View.GONE);
        currentPath = ConfigManager.getDefaultConfig().getString(rq_splash_path);
        if (currentPath == null) {
            currentPath = DEFAULT_SPLASH_PATH;
        }
        input.setText(currentPath);
        enable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableSplash = isChecked;
            panel.setVisibility(enableSplash ? View.VISIBLE : View.GONE);
        });
        dialog.setView(vg);
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            ConfigManager cfg = ConfigManager.getDefaultConfig();
            if (!enableSplash) {
                cfg.putBoolean(rq_splash_enabled, false);
            } else {
                currentPath = input.getText().toString();
                if (currentPath.length() == 0) {
                    Toasts.error(ctx, "请输入图片路径");
                    return;
                }
                File file = new File(currentPath);
                if (!file.exists() || !file.isFile()) {
                    Toasts.error(ctx, "路径不存在或者有误");
                    return;
                }
                if (!file.canRead()) {
                    Toasts.error(ctx, "无法读取图片 请检查权限");
                    return;
                }
                Bitmap bitmap = BitmapFactory.decodeFile(currentPath);
                if (bitmap == null) {
                    Toasts.error(ctx, "无法加载图片 请检图片是否损坏");
                    return;
                }
                cfg.putBoolean(rq_splash_enabled, true);
                cfg.putString(rq_splash_path, currentPath);
            }
            cfg.save();
            dialog.dismiss();
            if (enableSplash) {
                CustomMsgTimeFormat hook = CustomMsgTimeFormat.INSTANCE;
                if (!hook.isInitialized()) {
                    hook.initialize();
                }
            }
        });
    }

    public boolean isEnabled() {
        return ConfigManager.getDefaultConfig().getBooleanOrFalse(rq_splash_enabled);
    }

    public String getName() {
        return "自定义启动图";
    }
}
