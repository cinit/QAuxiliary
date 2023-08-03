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

package cc.ioctl.util;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static cc.ioctl.util.LayoutHelper.dip2px;
import static cc.ioctl.util.LayoutHelper.dip2sp;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.ui.drawable.HostStyleCommonItemBackground;
import cc.ioctl.util.ui.widget.FunctionDummy;
import io.github.qauxv.R;
import io.github.qauxv.ui.ResUtils;
import io.github.qauxv.util.Toasts;
import xyz.nextalone.util.SystemServiceUtils;

public class HostStyledViewBuilder {

    private HostStyledViewBuilder() {
    }

    public static FunctionDummy newListItemDummy(@NonNull Context ctx, @NonNull CharSequence title,
            @Nullable CharSequence desc, @Nullable CharSequence value) {
        FunctionDummy root = new FunctionDummy(ctx);
        root.getTitle().setText(title);
        if (!TextUtils.isEmpty(desc)) {
            root.getDesc().setText(desc);
        }
        if (!TextUtils.isEmpty(value)) {
            root.getValue().setText(value);
            root.getValue().setTextIsSelectable(true);
        }
        return root;
    }

    public static FunctionDummy newListItemButton(@NonNull Context ctx, @NonNull String title,
            @Nullable String desc, @Nullable String value, @Nullable View.OnClickListener listener) {
        FunctionDummy root = new FunctionDummy(ctx);
        root.getTitle().setText(title);
        if (!TextUtils.isEmpty(desc)) {
            root.getDesc().setText(desc);
        }
        if (!TextUtils.isEmpty(value)) {
            root.getValue().setText(value);
        }
        root.setOnClickListener(listener);
        return root;
    }

    public static LinearLayout subtitle(Context ctx, CharSequence title) {
        return subtitle(ctx, title, 0);
    }

    public static LinearLayout subtitle(Context ctx, CharSequence title, int color) {
        return subtitle(ctx, title, color, false);
    }

    public static LinearLayout subtitle(Context ctx, CharSequence title, int color, boolean isSelectable) {
        LinearLayout ll = new LinearLayout(ctx);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setGravity(Gravity.CENTER_VERTICAL);
        TextView tv = new TextView(ctx);
        tv.setTextIsSelectable(isSelectable);
        tv.setText(title);
        tv.setTextSize(dip2sp(ctx, 13));
        if (color == 0) {
            tv.setTextColor(getColorSkinBlack());
        } else {
            tv.setTextColor(color);
        }
        tv.setLayoutParams(new ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        ll.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        int m = dip2px(ctx, 14);
        tv.setPadding(m, m / 5, m / 5, m / 5);
        ll.addView(tv);
        return ll;
    }

    public static LinearLayout newDialogClickableItemClickToCopy(final Context ctx, String title,
            String value, ViewGroup vg, boolean attach, View.OnClickListener l) {
        return newDialogClickableItem(ctx, title, value, l, v -> {
            Context c = v.getContext();
            String msg = ((TextView) v).getText().toString();
            if (msg.length() > 0) {
                SystemServiceUtils.copyToClipboard(ctx, msg);
                Toasts.info(c, "已复制文本");
            }
            return true;
        }, vg, attach);
    }

    public static LinearLayout newDialogClickableItem(final Context ctx, String title, String value,
            View.OnClickListener l, View.OnLongClickListener ll, ViewGroup vg, boolean attach) {
        LinearLayout root = (LinearLayout) LayoutInflater.from(ctx)
                .inflate(R.layout.dialog_clickable_item, vg, false);
        TextView t = root.findViewById(R.id.dialogClickableItemTitle);
        TextView v = root.findViewById(R.id.dialogClickableItemValue);
        t.setText(title);
        v.setText(value);
        if (l != null) {
            v.setOnClickListener(l);
        }
        if (ll != null) {
            v.setOnLongClickListener(ll);
        }
        if (attach) {
            vg.addView(root);
        }
        return root;
    }

    public static int getColorSkinBlack() {
        return ResUtils.isInNightMode() ? 0xFFFFFFFF : 0xFF000000;
    }

    public static int getColorSkinGray3() {
        //noinspection java:S3400
        return 0xFF808080;
    }

    public static Drawable getListItemBackground() {
        return new HostStyleCommonItemBackground();
    }
}
