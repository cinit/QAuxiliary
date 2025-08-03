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
package io.github.qauxv.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CustomDialog {

    private androidx.appcompat.app.AlertDialog mFailsafeDialog = null;
    private androidx.appcompat.app.AlertDialog.Builder mBuilder = null;

    public static CustomDialog create(Context ctx) {
        CustomDialog ref = new CustomDialog();
        // dark/light theme is already handled by CommonContextWrapper.createAppCompatContext(context)
        ref.mBuilder = new androidx.appcompat.app.AlertDialog.Builder(CommonContextWrapper.createAppCompatContext(ctx));
        return ref;
    }

    /**
     * @param context the context to create the dialog
     * @return a failsafe dialog builder
     * @deprecated use {@link androidx.appcompat.app.AlertDialog.Builder} with {@link CommonContextWrapper#createAppCompatContext(Context)}  instead
     */
    @Deprecated
    public static CustomDialog createFailsafe(Context context) {
        // deprecated.
        return create(context);
    }

    public CustomDialog setCancelable(boolean flag) {
        mBuilder.setCancelable(flag);
        return this;
    }

    public CustomDialog setTitle(String title) {
        if (mFailsafeDialog == null) {
            mBuilder.setTitle(title);
        } else {
            mFailsafeDialog.setTitle(title);
        }
        return this;
    }

    public CustomDialog setMessage(CharSequence msg) {
        if (mFailsafeDialog == null) {
            mBuilder.setMessage(msg);
        } else {
            mFailsafeDialog.setMessage(msg);
        }
        return this;
    }

    public Context getContext() {
        if (mFailsafeDialog == null) {
            return mBuilder.getContext();
        } else {
            return mFailsafeDialog.getContext();
        }
    }

    public CustomDialog setView(View v) {
        if (mFailsafeDialog == null) {
            mBuilder.setView(v);
        } else {
            mFailsafeDialog.setView(v);
        }
        return this;
    }

    @NonNull
    public CustomDialog setPositiveButton(int text, @Nullable DialogInterface.OnClickListener listener) {
        Context ctx;
        ctx = mBuilder.getContext();
        setPositiveButton(ctx.getString(text), listener);
        return this;
    }

    @NonNull
    public CustomDialog setNegativeButton(int text, @Nullable DialogInterface.OnClickListener listener) {
        Context ctx;
        ctx = mBuilder.getContext();
        return setNegativeButton(ctx.getString(text), listener);
    }

    @NonNull
    public CustomDialog ok() {
        setPositiveButton(android.R.string.ok, null);
        return this;
    }

    @NonNull
    public CustomDialog setPositiveButton(@NonNull String text, @Nullable DialogInterface.OnClickListener listener) {
        mBuilder.setPositiveButton(text, listener);
        return this;
    }

    @NonNull
    public CustomDialog setNeutralButton(@NonNull String text, @Nullable DialogInterface.OnClickListener listener) {
        mBuilder.setNeutralButton(text, listener);
        return this;
    }

    @NonNull
    public CustomDialog setNeutralButton(int text, @Nullable DialogInterface.OnClickListener listener) {
        mBuilder.setNeutralButton(text, listener);
        return this;
    }

    public CustomDialog setNegativeButton(String text, DialogInterface.OnClickListener listener) {
        mBuilder.setNegativeButton(text, listener);
        return this;
    }

    public Dialog create() {
        if (mFailsafeDialog == null) {
            mFailsafeDialog = mBuilder.create();
        }
        return mFailsafeDialog;
    }

    public Dialog show() {
        if (mFailsafeDialog == null) {
            mFailsafeDialog = mBuilder.create();
        }
        mFailsafeDialog.show();
        return mFailsafeDialog;
    }

    public void dismiss() {
        if (mFailsafeDialog != null) {
            mFailsafeDialog.dismiss();
        }
    }

    public boolean isShowing() {
        if (mFailsafeDialog != null) {
            return mFailsafeDialog.isShowing();
        }
        return false;
    }

    public static class DummyCallback implements DialogInterface.OnClickListener {

        public DummyCallback() {
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
        }
    }
}
