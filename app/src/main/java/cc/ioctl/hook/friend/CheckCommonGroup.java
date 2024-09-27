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

package cc.ioctl.hook.friend;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import cc.ioctl.util.LayoutHelper;
import io.github.qauxv.base.IEntityAgent;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonClickableStaticFunctionItem;
import io.github.qauxv.ui.CommonContextWrapper;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Toasts;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;

@UiItemAgentEntry
public class CheckCommonGroup extends CommonClickableStaticFunctionItem {

    public static final CheckCommonGroup INSTANCE = new CheckCommonGroup();

    private CheckCommonGroup() {
    }

    public static void onClick(Context baseContext) {
        Context ctx = CommonContextWrapper.createAppCompatContext(baseContext);
        EditText editText = new EditText(ctx);
        editText.setTextSize(16);
        LinearLayout linearLayout = new LinearLayout(ctx);
        linearLayout.addView(editText, LayoutHelper.newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT));
        AlertDialog alertDialog = new AlertDialog.Builder(ctx)
                .setTitle("输入对方QQ号")
                .setView(linearLayout)
                .setCancelable(true)
                .setPositiveButton("打开", null)
                .setNegativeButton("取消", null)
                .create();
        alertDialog.show();
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String text = editText.getText().toString();
                    if (TextUtils.isEmpty(text)) {
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
                    Class<?> browser;
                    try {
                        browser = Initiator.loadClass("com.tencent.mobileqq.activity.QQBrowserDelegationActivity");
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

    @NonNull
    @Override
    public Function1<IEntityAgent, String> getTitleProvider() {
        return agent -> "查找共同群";
    }

    @Override
    public Function3<IUiItemAgent, Activity, View, Unit> getOnClickListener() {
        return (agent, ctx, view) -> {
            onClick(ctx);
            return Unit.INSTANCE;
        };
    }

    @Nullable
    @Override
    public Function2<IUiItemAgent, Context, String[]> getExtraSearchKeywordProvider() {
        return null;
    }

    @NonNull
    @Override
    public IUiItemAgent getUiItemAgent() {
        return this;
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.FRIEND_CATEGORY;
    }

    @NonNull
    @Override
    public String getItemAgentProviderUniqueIdentifier() {
        return getClass().getName();
    }
}
