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
package io.github.qauxv.util;

import static io.github.qauxv.util.Initiator.load;

import android.content.Context;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HostInfo;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Use custom toast anywhere
 */
public class Toasts {

    public static final int TYPE_PLAIN = -1;
    public static final int TYPE_INFO = 0;
    public static final int TYPE_ERROR = 1;
    public static final int TYPE_SUCCESS = 2;

    public static final int LENGTH_SHORT = 0;
    public static final int LENGTH_LONG = 1;

    private static Method method_Toast_show;
    private static Method method_Toast_makeText;
    private static Class<?> clazz_QQToast;

    /**
     * Make a QQ custom toast.
     *
     * @param context  The context to use.
     * @param type     The type of toast, Either {@link #TYPE_INFO}, {@link #TYPE_ERROR}, {@link #TYPE_SUCCESS} or
     *                 {@link #TYPE_INFO}
     * @param text     The text to show.
     * @param duration How long to display the message.  Either {@link #LENGTH_SHORT} or {@link #LENGTH_LONG}
     */
    public static void showToast(@Nullable Context context, int type,
                                 @NonNull final CharSequence text, int duration) {
        Objects.requireNonNull(text, "text");
        if (context == null) {
            context = HostInfo.getApplication();
        }
        final Context ctx = context;
        SyncUtils.runOnUiThread(() -> {
            if (type == TYPE_PLAIN) {
                Toast.makeText(ctx, text, duration).show();
            } else {
                try {
                    if (clazz_QQToast == null) {
                        clazz_QQToast = load("com/tencent/mobileqq/widget/QQToast");
                    }
                    if (clazz_QQToast == null) {
                        Class<?> clz = load(
                            "com/tencent/mobileqq/activity/aio/doodle/DoodleLayout");
                        if (clz != null) {
                            Field[] fs = clz.getDeclaredFields();
                            for (Field f : fs) {
                                if (View.class.isAssignableFrom(f.getType())) {
                                    continue;
                                }
                                if (f.getType().isPrimitive()) {
                                    continue;
                                }
                                if (f.getType().isInterface()) {
                                    continue;
                                }
                                clazz_QQToast = f.getType();
                            }
                        } else {
                            // for qqlite >= 4.0.0
                            clazz_QQToast = load("com.tencent.qqmini.sdk.core.widget.QQToast");
                        }
                    }
                    if (method_Toast_show == null) {
                        Method[] ms = clazz_QQToast.getMethods();
                        for (Method m : ms) {
                            if (Toast.class.equals(m.getReturnType())
                                && m.getParameterTypes().length == 0) {
                                method_Toast_show = m;
                                break;
                            }
                        }
                    }
                    if (method_Toast_makeText == null) {
                        try {
                            method_Toast_makeText = clazz_QQToast
                                .getMethod("a", Context.class, int.class, CharSequence.class,
                                    int.class);
                        } catch (NoSuchMethodException e) {
                            try {
                                method_Toast_makeText = clazz_QQToast
                                    .getMethod("b", Context.class, int.class, CharSequence.class,
                                        int.class);
                            } catch (NoSuchMethodException e2) {
                                try {
                                    method_Toast_makeText = clazz_QQToast.getMethod("makeText",
                                        Context.class,
                                        int.class, CharSequence.class, int.class);
                                } catch (NoSuchMethodException e3) {
                                    throw e;
                                }
                            }
                        }
                    }
                    Object this_QQToast_does_NOT_extend_a_standard_Toast_so_please_do_NOT_cast_it_to_Toast
                        = method_Toast_makeText.invoke(null, ctx, type, text, duration);
                    method_Toast_show.invoke(
                        this_QQToast_does_NOT_extend_a_standard_Toast_so_please_do_NOT_cast_it_to_Toast);
                    // However, the return value of QQToast.show() is a standard Toast
                } catch (Exception e) {
                    Log.e(e);
                    Toast.makeText(ctx, text, duration).show();
                }
            }
        });
    }

    public static void info(Context ctx, @NonNull CharSequence text, int duration) {
        showToast(ctx, TYPE_INFO, text, duration);
    }

    public static void info(Context ctx, @NonNull CharSequence text) {
        showToast(ctx, TYPE_INFO, text, LENGTH_SHORT);
    }

    public static void success(Context ctx, @NonNull CharSequence text, int duration) {
        showToast(ctx, TYPE_SUCCESS, text, duration);
    }

    public static void success(Context ctx, @NonNull CharSequence text) {
        showToast(ctx, TYPE_SUCCESS, text, LENGTH_SHORT);
    }

    public static void error(Context ctx, @NonNull CharSequence text, int duration) {
        showToast(ctx, TYPE_ERROR, text, duration);
    }

    public static void error(Context ctx, @NonNull CharSequence text) {
        showToast(ctx, TYPE_ERROR, text, LENGTH_SHORT);
    }

    public static void show(Context ctx, @NonNull CharSequence text, int duration) {
        showToast(ctx, TYPE_PLAIN, text, duration);
    }

    public static void show(Context ctx, @NonNull CharSequence text) {
        showToast(ctx, TYPE_PLAIN, text, LENGTH_SHORT);
    }
    public static void show(@NonNull CharSequence text) {
        showToast(null, TYPE_PLAIN, text, LENGTH_SHORT);
    }
}
