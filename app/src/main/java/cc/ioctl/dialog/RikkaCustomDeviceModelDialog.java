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
import cc.ioctl.hook.chat.CustomMsgTimeFormat;
import io.github.qauxv.R;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.ui.CustomDialog;
import io.github.qauxv.util.Toasts;
import java.io.File;

public class RikkaCustomDeviceModelDialog {

    private static final String DEFAULT_DEVICE_MANUFACTURER = "小米";
    private static final String DEFAULT_DEVICE_MODEL = "小米10 Pro";

    private static final String rq_custom_device_manufacturer = "rq_custom_device_manufacturer";
    private static final String rq_custom_device_model = "rq_custom_device_model";

    private static final String rq_custom_device_model_enabled = "rq_custom_device_model_enabled";

    @Nullable
    private AlertDialog dialog;
    @Nullable
    private LinearLayout vg;

    private String currentDeviceManufacturer;
    private String currentDeviceModel;
    private boolean enableCustomDeviceModel;

    public static boolean IsEnabled() {
        return ConfigManager.getDefaultConfig().getBooleanOrFalse(rq_custom_device_model_enabled);
    }

    @Nullable
    public static String getCurrentDeviceModel() {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        if (cfg.getBooleanOrFalse(rq_custom_device_model_enabled)) {
            String val = cfg.getString(rq_custom_device_model);
            if (val == null) {
                val = DEFAULT_DEVICE_MODEL;
            }
            return val;
        }
        return null;
    }

    @Nullable
    public static String getCurrentDeviceManufacturer() {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        if (cfg.getBooleanOrFalse(rq_custom_device_model_enabled)) {
            String val = cfg.getString(rq_custom_device_manufacturer);
            if (val == null) {
                val = DEFAULT_DEVICE_MANUFACTURER;
            }
            return val;
        }
        return null;
    }

    @SuppressLint("InflateParams")
    public void showDialog(@NonNull Context context) {
        dialog = (AlertDialog) CustomDialog.createFailsafe(context).setTitle("自定义机型")
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null).create();
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        final Context ctx = dialog.getContext();
        vg = (LinearLayout) LayoutInflater.from(ctx)
                .inflate(R.layout.rikka_custom_device_model_dialog, null);

        final TextView previewManufacturer = vg
                .findViewById(R.id.textViewCustomDeviceManufacturerPreview);
        final TextView previewModel = vg.findViewById(R.id.textViewDeviceModelPreview);

        final TextView inputManufacturer = vg.findViewById(R.id.editTextCustomDeviceManufacturer);
        final TextView inputModel = vg.findViewById(R.id.editTextCustomDeviceModel);

        final CheckBox enable = vg.findViewById(R.id.checkBoxEnableCustomDeviceModel);

        final LinearLayout panel = vg.findViewById(R.id.layoutCustomDeviceModelPreview);

        enableCustomDeviceModel = ConfigManager.getDefaultConfig()
                .getBooleanOrFalse(rq_custom_device_model_enabled);
        enable.setChecked(enableCustomDeviceModel);

        panel.setVisibility(enableCustomDeviceModel ? View.VISIBLE : View.GONE);

        currentDeviceManufacturer = ConfigManager.getDefaultConfig()
                .getString(rq_custom_device_manufacturer);
        currentDeviceModel = ConfigManager.getDefaultConfig().getString(rq_custom_device_model);

        if (currentDeviceManufacturer == null) {
            currentDeviceManufacturer = DEFAULT_DEVICE_MANUFACTURER;
        }
        if (currentDeviceModel == null) {
            currentDeviceModel = DEFAULT_DEVICE_MODEL;
        }

        inputManufacturer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                currentDeviceManufacturer = s.toString();
                previewManufacturer.setText(currentDeviceManufacturer);
            }
        });
        inputManufacturer.setText(currentDeviceManufacturer);
        enable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableCustomDeviceModel = isChecked;
            panel.setVisibility(enableCustomDeviceModel ? View.VISIBLE : View.GONE);
        });

        inputModel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                currentDeviceModel = s.toString();
                previewModel.setText(currentDeviceModel);
            }
        });
        inputModel.setText(currentDeviceModel);
        enable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableCustomDeviceModel = isChecked;
            panel.setVisibility(enableCustomDeviceModel ? View.VISIBLE : View.GONE);
        });

        dialog.setView(vg);
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    ConfigManager cfg = ConfigManager.getDefaultConfig();
                    if (!enableCustomDeviceModel) {
                        cfg.putBoolean(rq_custom_device_model_enabled, false);
                    } else if (currentDeviceManufacturer.length() == 0
                            || currentDeviceModel.length() == 0) {
                        Toasts.error(ctx, "厂商或机型不能为空!");
                        return;
                    } else {
                        cfg.putBoolean(rq_custom_device_model_enabled, true);
                        cfg.putString(rq_custom_device_manufacturer, currentDeviceManufacturer);
                        cfg.putString(rq_custom_device_model, currentDeviceModel);
                    }
                    //移除缓存文件
                    new File(ctx.getCacheDir().getParent()+"/app_x5webview/Default/Local Storage/leveldb","MANIFEST-000001").delete();
                    cfg.save();
                    Toasts.success(ctx, "重启" + HostInfo.getAppName() + "生效!");
                    dialog.dismiss();
                    if (enableCustomDeviceModel) {
                        CustomMsgTimeFormat hook = CustomMsgTimeFormat.INSTANCE;
                        if (!hook.isInitialized()) {
                            hook.initialize();
                        }
                    }
                });
    }

    public boolean isEnabled() {
        return ConfigManager.getDefaultConfig().getBooleanOrFalse(rq_custom_device_model_enabled);
    }

    public String getName() {
        return "自定义机型[需要重启" + HostInfo.getAppName() + "]";
    }
}
