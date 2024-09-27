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

package cc.ioctl.hook.profile;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static cc.ioctl.util.LayoutHelper.newLinearLayoutParams;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import cc.ioctl.hook.misc.QSecO3AddRiskRequestMitigation;
import cc.ioctl.util.Reflex;
import io.github.qauxv.base.IEntityAgent;
import io.github.qauxv.base.ISwitchCellAgent;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonClickableStaticFunctionItem;
import io.github.qauxv.ui.CommonContextWrapper;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.Toasts;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import kotlinx.coroutines.flow.MutableStateFlow;

@UiItemAgentEntry
public class OpenProfileCard extends CommonClickableStaticFunctionItem {

    public static final OpenProfileCard INSTANCE = new OpenProfileCard();

    private OpenProfileCard() {
    }

    public static void onClick(Context baseContext) {
        Context ctx = CommonContextWrapper.createAppCompatContext(baseContext);
        EditText editText = new EditText(ctx);
        editText.setTextSize(16);
        LinearLayout linearLayout = new LinearLayout(ctx);
        linearLayout.addView(editText, newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT));
        AlertDialog alertDialog = new AlertDialog.Builder(ctx)
                .setTitle("输入对方QQ号")
                .setView(linearLayout)
                .setCancelable(true)
                .setPositiveButton("打开QQ号", null)
                .setNeutralButton("打开QQ群", null)
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
                    openUserProfileCard(ctx, uin);
                });
        alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL)
                .setOnClickListener(v -> {
                    String text = editText.getText().toString();
                    if (TextUtils.isEmpty(text)) {
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
                    openTroopProfileActivity(ctx, Long.toString(uin));
                });
    }

    public static void openTroopProfileActivity(@NonNull Context context, @NonNull String troopUin) {
        if (TextUtils.isEmpty(troopUin)) {
            return;
        }
        try {
            QSecO3AddRiskRequestMitigation.INSTANCE.initialize();
        } catch (Exception | LinkageError e) {
            Log.e("QSecO3AddRiskRequestMitigation init fail", e);
        }
        String knPublicFragmentActivity = "com.tencent.mobileqq.activity.PublicFragmentActivity";
        Class<?> kVisitorTroopCardFragment = Initiator.load("com.tencent.mobileqq.troop.troopCard.VisitorTroopCardFragment");
        if (kVisitorTroopCardFragment == null) {
            kVisitorTroopCardFragment = Initiator.load("com.tencent.mobileqq.troop.troopcard.ui.VisitorTroopCardFragment");
            knPublicFragmentActivity = "com.tencent.mobileqq.activity.QPublicFragmentActivity";
        }
        if (kVisitorTroopCardFragment == null || Initiator.load(knPublicFragmentActivity) == null) {
            Toasts.error(context, "接口错误: troop = " + troopUin);
            return;
        }
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context, knPublicFragmentActivity));
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        intent.putExtra("fling_action_key", 2);
        intent.putExtra("keyword", (String) null);
        intent.putExtra("authKey", (String) null);
        // TODO: 2023-01-28 check whether troop_info_from is appropriate
        intent.putExtra("troop_info_from", 14);
        intent.putExtra("troop_uin", troopUin);
        intent.putExtra("vistor_type", 2);
        intent.putExtra("public_fragment_class", kVisitorTroopCardFragment.getName());
        // TODO: 2023-01-28 warn user if authSig is missing
        intent.putExtra(QSecO3AddRiskRequestMitigation.KEY_UIN_IS_FROM_VOID, true);
        context.startActivity(intent);
    }

    public static void openUserProfileCard(@NonNull Context ctx, long uin) {
        openUserProfileCard(ctx, uin, 0);
    }

    public static void openUserProfileCard(@NonNull Context ctx, long uin, long troopUin) {
        try {
            QSecO3AddRiskRequestMitigation.INSTANCE.initialize();
        } catch (Exception | LinkageError e) {
            Log.e("QSecO3AddRiskRequestMitigation init fail", e);
        }
        try {
            Parcelable allInOne = (Parcelable) Reflex.newInstance(
                    Initiator._AllInOne(), "" + uin, troopUin != 0 ? 20 : 35,
                    String.class, int.class);
            Intent intent = new Intent(ctx, Initiator._FriendProfileCardActivity());
            intent.putExtra("AllInOne", allInOne);
            if (troopUin != 0) {
                intent.putExtra("memberUin", "" + uin);
                intent.putExtra("troopUin", "" + troopUin);
            }
            intent.putExtra(QSecO3AddRiskRequestMitigation.KEY_UIN_IS_FROM_VOID, true);
            ctx.startActivity(intent);
        } catch (Exception e) {
            Toasts.error(ctx, e.toString().replace("java.lang.", ""));
            Log.e(e);
        }
    }

    @NonNull
    @Override
    public Function1<IEntityAgent, String> getTitleProvider() {
        return (agent) -> "打开资料卡";
    }

    @Nullable
    @Override
    public MutableStateFlow<String> getValueState() {
        return null;
    }

    @Nullable
    @Override
    public Function1<IUiItemAgent, Boolean> getValidator() {
        return null;
    }

    @Nullable
    @Override
    public ISwitchCellAgent getSwitchProvider() {
        return null;
    }

    @Nullable
    @Override
    public Function3<IUiItemAgent, Activity, View, Unit> getOnClickListener() {
        return (agent, activity, view) -> {
            onClick(activity);
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
        return Auxiliary.PROFILE_CATEGORY;
    }

    @NonNull
    @Override
    public String getItemAgentProviderUniqueIdentifier() {
        return getClass().getName();
    }
}
