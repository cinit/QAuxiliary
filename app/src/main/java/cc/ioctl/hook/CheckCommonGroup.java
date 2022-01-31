/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
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

package cc.ioctl.hook;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static io.github.qauxv.ui.ViewBuilder.newLinearLayoutParams;
import static cc.ioctl.util.LayoutUtils.dip2px;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.core.view.ViewCompat;
import io.github.qauxv.ui.CustomDialog;
import cc.ioctl.util.ui.drawable.HighContrastBorder;
import io.github.qauxv.util.Toasts;

public class CheckCommonGroup {

    public static void onClick(Context ctx) {
        CustomDialog dialog = CustomDialog.createFailsafe(ctx);
        EditText editText = new EditText(ctx);
        editText.setTextSize(16);
        int _5 = dip2px(ctx, 5);
        editText.setPadding(_5, _5, _5, _5);
        ViewCompat.setBackground(editText, new HighContrastBorder());
        LinearLayout linearLayout = new LinearLayout(ctx);
        linearLayout
            .addView(editText, newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT, _5 * 2));
        AlertDialog alertDialog = (AlertDialog) dialog.setTitle("输入对方QQ号")
            .setView(linearLayout)
            .setCancelable(true)
            .setPositiveButton("打开QQ号", null)
            .setNegativeButton("取消", null)
            .create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener(v -> {
                String text = editText.getText().toString();
                if (text.equals("")) {
                    Toasts.error(ctx, "请输入QQ号");
                    return;
                }
                long uin = 0;
                try {
                    uin = Long.parseLong(text);
                } catch (NumberFormatException ignored) {
                }
                if (uin < 10000) {
                    Toasts.error(ctx, "请输入有效的QQ号");
                    return;
                }
                alertDialog.dismiss();
                Class<?> browser = null;
                try {
                    browser = Class
                        .forName("com.tencent.mobileqq.activity.QQBrowserDelegationActivity");
                    Intent intent = new Intent(ctx, browser);
                    intent.putExtra("fling_action_key", 2);
                    intent.putExtra("fling_code_key", ctx.hashCode());
                    intent.putExtra("useDefBackText", true);
                    intent.putExtra("param_force_internal_browser", true);
                    intent.putExtra("url", "https://ti.qq.com/friends/recall?uin=" + uin);
                    ctx.startActivity(intent);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });
    }

}
