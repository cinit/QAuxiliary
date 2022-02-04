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

package cc.ioctl.hook;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static cc.ioctl.util.LayoutHelper.dip2px;
import static cc.ioctl.util.LayoutHelper.newLinearLayoutParams;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.core.view.ViewCompat;
import cc.ioctl.util.ui.drawable.HighContrastBorder;
import io.github.qauxv.MainHook;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.ui.CustomDialog;
import io.github.qauxv.util.Toasts;

@UiItemAgentEntry
public class OpenProfileCard {

    // TODO: 2022-02-04 Add UI item interface

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
                .setNeutralButton("打开QQ群", null)
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
                    MainHook.openProfileCard(ctx, uin);
                });
        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setOnClickListener(v -> {
                    String text = editText.getText().toString();
                    if (text.equals("")) {
                        Toasts.error(ctx, "请输入QQ群号");
                        return;
                    }
                    long uin = 0;
                    try {
                        uin = Long.parseLong(text);
                    } catch (NumberFormatException ignored) {
                    }
                    if (uin < 10000) {
                        Toasts.error(ctx, "请输入有效的QQ群号");
                        return;
                    }
                    alertDialog.dismiss();
                    openTroopCard(ctx, Long.toString(uin));
                });
    }

    public static void openTroopCard(Context ctx, String troop) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.tencent.mobileqq",
                "com.tencent.mobileqq.activity.PublicFragmentActivity"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        intent.putExtra("fling_action_key", 2);
        intent.putExtra("preAct", "QRJumpActivity");
        intent.putExtra("leftViewText", "返回");
        intent.putExtra("keyword", (String) null);
        intent.putExtra("authKey", (String) null);
        intent.putExtra("preAct_time", System.currentTimeMillis());
        intent.putExtra("preAct_elapsedRealtime", System.nanoTime());
        intent.putExtra("troop_info_from", 14);
        intent.putExtra("troop_uin", troop);
        intent.putExtra("vistor_type", 2);
        intent.putExtra("public_fragment_class",
                "com.tencent.mobileqq.troop.troopCard.VisitorTroopCardFragment");
        ctx.startActivity(intent);
    }

}
