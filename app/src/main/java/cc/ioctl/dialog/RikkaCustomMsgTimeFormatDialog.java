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
import cc.ioctl.hook.chat.CustomMsgTimeFormat;
import io.github.qauxv.R;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.ui.CustomDialog;
import io.github.qauxv.util.Toasts;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RikkaCustomMsgTimeFormatDialog {

    private static final String DEFAULT_MSG_TIME_FORMAT = "yyyy年MM月dd日 HH:mm:ss";

    private static final String rq_msg_time_format = "rq_msg_time_format";
    private static final String rq_msg_time_enabled = "rq_msg_time_enabled";

    @Nullable
    private AlertDialog dialog;
    @Nullable
    private LinearLayout vg;

    private String currentFormat;
    private boolean enableMsgTimeFormat;
    private boolean currentFormatValid = false;

    public static boolean IsEnabled() {
        return ConfigManager.getDefaultConfig().getBooleanOrFalse(rq_msg_time_enabled);
    }

    @NonNull
    public static String getCurrentMsgTimeFormat() {
        return ConfigManager.getDefaultConfig().getString(rq_msg_time_format, DEFAULT_MSG_TIME_FORMAT);
    }

    @NonNull
    public static String getTimeFormat() {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        String val = cfg.getString(rq_msg_time_format);
        if (val == null) {
            val = DEFAULT_MSG_TIME_FORMAT;
        }
        return val;
    }

    @SuppressLint("InflateParams")
    public void showDialog(@NonNull Context context) {
        dialog = (AlertDialog) CustomDialog.createFailsafe(context).setTitle("自定义时间格式")
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null).create();
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        final Context ctx = dialog.getContext();
        vg = (LinearLayout) LayoutInflater.from(ctx)
                .inflate(R.layout.rikka_msg_time_formart_dialog, null);
        final TextView preview = vg.findViewById(R.id.textViewMsgTimeFormatPreview);
        final TextView invalid = vg.findViewById(R.id.textViewInvalidMsgTimeFormat);
        final TextView input = vg.findViewById(R.id.editTextMsgTimeFormat);
        final CheckBox enable = vg.findViewById(R.id.checkBoxEnableMsgTimeFormat);
        final LinearLayout panel = vg.findViewById(R.id.layoutMsgTimeFormatPanel);
        enableMsgTimeFormat = ConfigManager.getDefaultConfig()
                .getBooleanOrFalse(rq_msg_time_enabled);
        enable.setChecked(enableMsgTimeFormat);
        panel.setVisibility(enableMsgTimeFormat ? View.VISIBLE : View.GONE);
        currentFormat = ConfigManager.getDefaultConfig().getString(rq_msg_time_format);
        if (currentFormat == null) {
            currentFormat = DEFAULT_MSG_TIME_FORMAT;
        }
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            @SuppressLint("SimpleDateFormat")
            public void afterTextChanged(Editable s) {
                String format = s.toString();
                currentFormat = format;
                try {
                    SimpleDateFormat dsf = new SimpleDateFormat(format);
                    String result = dsf.format(new Date());
                    currentFormatValid = true;
                    invalid.setVisibility(View.GONE);
                    preview.setVisibility(View.VISIBLE);
                    preview.setText(result);
                } catch (Exception e) {
                    currentFormatValid = false;
                    preview.setVisibility(View.GONE);
                    invalid.setVisibility(View.VISIBLE);
                }
            }
        });
        input.setText(currentFormat);
        enable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enableMsgTimeFormat = isChecked;
            panel.setVisibility(enableMsgTimeFormat ? View.VISIBLE : View.GONE);
        });
        dialog.setView(vg);
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    ConfigManager cfg = ConfigManager.getDefaultConfig();
                    boolean done = false;
                    if (!enableMsgTimeFormat) {
                        cfg.putBoolean(rq_msg_time_enabled, false);
                        done = true;
                    } else {
                        if (currentFormatValid) {
                            cfg.putBoolean(rq_msg_time_enabled, true);
                            cfg.putString(rq_msg_time_format, currentFormat);
                            done = true;
                        } else {
                            Toasts.error(ctx, "请输入一个有效的时间格式");
                        }
                    }
                    if (done) {
                        cfg.save();
                        dialog.dismiss();
                        if (enableMsgTimeFormat) {
                            CustomMsgTimeFormat hook = CustomMsgTimeFormat.INSTANCE;
                            if (!hook.isInitialized()) {
                                hook.initialize();
                            }
                        }
                    }
                });
    }

    public boolean isEnabled() {
        return ConfigManager.getDefaultConfig().getBooleanOrFalse(rq_msg_time_enabled);
    }

    public String getName() {
        return "聊天页自定义时间格式";
    }
}
