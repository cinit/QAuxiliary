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

package io.github.qauxv.util;

import androidx.annotation.NonNull;

public class Log {

    private Log() {
    }

    private static final String TAG = "QAuxv";

    public static void e(@NonNull String msg) {
        android.util.Log.e(TAG, msg);
    }

    public static void w(@NonNull String msg) {
        android.util.Log.w(TAG, msg);
    }

    public static void i(@NonNull String msg) {
        android.util.Log.i(TAG, msg);
    }

    public static void d(@NonNull String msg) {
        android.util.Log.d(TAG, msg);
    }

    public static void v(@NonNull String msg) {
        android.util.Log.v(TAG, msg);
    }

    public static void e(@NonNull Throwable e) {
        android.util.Log.e(TAG, e.toString(), e);
    }

    public static void w(@NonNull Throwable e) {
        android.util.Log.w(TAG, e.toString(), e);
    }

    public static void i(@NonNull Throwable e) {
        android.util.Log.i(TAG, e.toString(), e);
    }

    public static void d(@NonNull Throwable e) {
        android.util.Log.d(TAG, e.toString(), e);
    }

    public static void e(@NonNull String msg, @NonNull Throwable e) {
        android.util.Log.e(TAG, msg, e);
    }

    public static void w(@NonNull String msg, @NonNull Throwable e) {
        android.util.Log.w(TAG, msg, e);
    }

    public static void i(@NonNull String msg, @NonNull Throwable e) {
        android.util.Log.i(TAG, msg, e);
    }

    public static void d(@NonNull String msg, @NonNull Throwable e) {
        android.util.Log.d(TAG, msg, e);
    }
}
