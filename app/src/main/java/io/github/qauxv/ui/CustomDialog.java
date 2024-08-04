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

import static io.github.qauxv.util.Initiator.load;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.Reflex;
import io.github.qauxv.base.annotation.DexDeobfs;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.dexkit.CDialogUtil;
import io.github.qauxv.util.dexkit.DexKit;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class CustomDialog {

    private static Class<?> clz_DialogUtil;
    private static Class<?> clz_CustomDialog;
    private static Method m_DialogUtil_a;
    private Dialog mDialog = null;
    private androidx.appcompat.app.AlertDialog mFailsafeDialog = null;
    private androidx.appcompat.app.AlertDialog.Builder mBuilder = null;
    private boolean failsafe = false;

    @DexDeobfs(CDialogUtil.class)
    public static CustomDialog create(Context ctx) {
        try {
            if (clz_DialogUtil == null) {
                clz_DialogUtil = DexKit.loadClassFromCache(CDialogUtil.INSTANCE);
            }
            if (clz_CustomDialog == null) {
                clz_CustomDialog = load("com/tencent/mobileqq/utils/QQCustomDialog");
                if (clz_CustomDialog == null) {
                    Class clz_Lite = load("com/dataline/activities/LiteActivity");
                    Field[] fs = clz_Lite.getDeclaredFields();
                    for (Field f : fs) {
                        if (Modifier.isPrivate(f.getModifiers()) && Dialog.class.isAssignableFrom(f.getType())) {
                            clz_CustomDialog = f.getType();
                            break;
                        }
                    }
                }
            }
            if (m_DialogUtil_a == null) {
                Method tmpa = null, tmpb = null;
                for (Method m : clz_DialogUtil.getDeclaredMethods()) {
                    if (m.getReturnType().equals(clz_CustomDialog) && (Modifier.isPublic(m.getModifiers()))) {
                        Class<?>[] argt = m.getParameterTypes();
                        if (argt.length != 2) {
                            continue;
                        }
                        if (argt[0].equals(Context.class) && argt[1].equals(int.class)) {
                            if (m.getName().equals("a")) {
                                m_DialogUtil_a = m;
                                break;
                            } else {
                                if (tmpa == null) {
                                    tmpa = m;
                                } else {
                                    tmpb = m;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (m_DialogUtil_a == null && tmpa != null) {
                    if (tmpb == null) {
                        m_DialogUtil_a = tmpa;
                    } else {
                        m_DialogUtil_a = (Reflex.strcmp(tmpa.getName(), tmpb.getName()) > 0) ? tmpb : tmpa;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(e);
        }
        CustomDialog ref = new CustomDialog();
        try {
            ref.mDialog = (Dialog) m_DialogUtil_a.invoke(null, ctx, 230);
        } catch (Exception e) {
            Log.e(e);
        }
        if (ref.mDialog == null) {
            ref.failsafe = true;
            ref.mBuilder = new androidx.appcompat.app.AlertDialog.Builder(
                    CommonContextWrapper.createAppCompatContext(ctx));
        }
        return ref;
    }

    /**
     * @param context the context to create the dialog
     * @return a failsafe dialog builder
     * @deprecated use {@link androidx.appcompat.app.AlertDialog.Builder} with {@link CommonContextWrapper#createAppCompatContext(Context)}  instead
     */
    @Deprecated
    public static CustomDialog createFailsafe(Context context) {
        CustomDialog ref = new CustomDialog();
        // dark/light theme is already handled by CommonContextWrapper.createAppCompatContext(context)
        Context ctx = CommonContextWrapper.createAppCompatContext(context);
        ref.failsafe = true;
        ref.mBuilder = new androidx.appcompat.app.AlertDialog.Builder(ctx);
        return ref;
    }

    public CustomDialog setCancelable(boolean flag) {
        if (!failsafe) {
            mDialog.setCancelable(flag);
            if (flag) {
                mDialog.setCanceledOnTouchOutside(true);
            }
        } else {
            mBuilder.setCancelable(flag);
        }
        return this;
    }

    public CustomDialog setTitle(String title) {
        if (!failsafe) {
            try {
                Reflex.invokeVirtual(mDialog, "setTitle", title, String.class);
            } catch (Exception e) {
                Log.e(e);
            }
        } else {
            if (mFailsafeDialog == null) {
                mBuilder.setTitle(title);
            } else {
                mFailsafeDialog.setTitle(title);
            }
        }
        return this;
    }

    public CustomDialog setMessage(CharSequence msg) {
        if (!failsafe) {
            try {
                Reflex.invokeVirtual(mDialog, "setMessage", msg, CharSequence.class);
            } catch (Exception e) {
                Log.e(e);
            }
        } else {
            if (mFailsafeDialog == null) {
                mBuilder.setMessage(msg);
            } else {
                mFailsafeDialog.setMessage(msg);
            }
        }
        return this;
    }

    public Context getContext() {
        if (!failsafe) {
            return mDialog.getContext();
        } else {
            if (mFailsafeDialog == null) {
                return mBuilder.getContext();
            } else {
                return mFailsafeDialog.getContext();
            }
        }
    }

    public CustomDialog setView(View v) {
        if (!failsafe) {
            try {
                Reflex.invokeVirtual(mDialog, "setView", v, View.class);
            } catch (Exception e) {
                Log.e(e);
            }
        } else {
            if (mFailsafeDialog == null) {
                mBuilder.setView(v);
            } else {
                mFailsafeDialog.setView(v);
            }
        }
        return this;
    }

    @NonNull
    public CustomDialog setPositiveButton(int text, @Nullable DialogInterface.OnClickListener listener) {
        Context ctx;
        if (failsafe) {
            ctx = mBuilder.getContext();
        } else {
            ctx = mDialog.getContext();
        }
        return setPositiveButton(ctx.getString(text), listener);
    }

    @NonNull
    public CustomDialog setNegativeButton(int text, @Nullable DialogInterface.OnClickListener listener) {
        Context ctx;
        if (failsafe) {
            ctx = mBuilder.getContext();
        } else {
            ctx = mDialog.getContext();
        }
        return setNegativeButton(ctx.getString(text), listener);
    }

    @NonNull
    public CustomDialog ok() {
        setPositiveButton(android.R.string.ok, null);
        return this;
    }

    @NonNull
    public CustomDialog setPositiveButton(@NonNull String text, @Nullable DialogInterface.OnClickListener listener) {
        if (!failsafe) {
            if (text != null && listener == null) {
                listener = new DummyCallback();
            }
            try {
                Reflex.invokeVirtual(mDialog, "setPositiveButton", text, listener, String.class, DialogInterface.OnClickListener.class);
            } catch (Exception e) {
                Log.e(e);
            }
        } else {
            mBuilder.setPositiveButton(text, listener);
        }
        return this;
    }

    @NonNull
    public CustomDialog setNeutralButton(@NonNull String text, @Nullable DialogInterface.OnClickListener listener) {
        if (!failsafe) {
            //They don't have a neutral button, sigh...
        } else {
            mBuilder.setNeutralButton(text, listener);
        }
        return this;
    }

    @NonNull
    public CustomDialog setNeutralButton(int text, @Nullable DialogInterface.OnClickListener listener) {
        if (!failsafe) {
            //They don't have a neutral button, sigh...
        } else {
            mBuilder.setNeutralButton(text, listener);
        }
        return this;
    }

    public CustomDialog setNegativeButton(String text, DialogInterface.OnClickListener listener) {
        if (!failsafe) {
            if (text != null && listener == null) {
                listener = new DummyCallback();
            }
            try {
                Reflex.invokeVirtual(mDialog, "setNegativeButton", text, listener, String.class,
                    DialogInterface.OnClickListener.class);
            } catch (Exception e) {
                Log.e(e);
            }
        } else {
            mBuilder.setNegativeButton(text, listener);
        }
        return this;
    }

    public Dialog create() {
        if (!failsafe) {
            return mDialog;
        } else {
            if (mFailsafeDialog == null) {
                mFailsafeDialog = mBuilder.create();
            }
            return mFailsafeDialog;
        }
    }

    public Dialog show() {
        if (!failsafe) {
            mDialog.show();
            return mDialog;
        } else {
            if (mFailsafeDialog == null) {
                mFailsafeDialog = mBuilder.create();
            }
            mFailsafeDialog.show();
            return mFailsafeDialog;
        }
    }

    public void dismiss() {
        if (!failsafe) {
            mDialog.dismiss();
        } else {
            if (mFailsafeDialog != null) {
                mFailsafeDialog.dismiss();
            }
        }
    }

    public boolean isShowing() {
        if (mDialog != null) {
            return mDialog.isShowing();
        }
        if (mFailsafeDialog != null) {
            return mFailsafeDialog.isShowing();
        }
        return false;
    }

    @Nullable
    public TextView getMessageTextView() {
        if (!failsafe) {
            return (TextView) Reflex.getInstanceObjectOrNull(mDialog, "text");
        } else {
            return null;
        }
    }

    public static class DummyCallback implements DialogInterface.OnClickListener {

        public DummyCallback() {
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
        }
    }
}
