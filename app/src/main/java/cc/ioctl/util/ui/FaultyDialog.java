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

package cc.ioctl.util.ui;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import cc.ioctl.util.Reflex;
import io.github.qauxv.SyncUtils;
import io.github.qauxv.ui.CommonContextWrapper;
import io.github.qauxv.util.Log;

public class FaultyDialog {

    private FaultyDialog() {
        throw new AssertionError("No " + getClass().getName() + " instances for you!");
    }

    public static void show(@NonNull Context ctx, @NonNull Throwable e) {
        show(ctx, null, e, true);
    }

    public static void show(@NonNull Context ctx, @Nullable String title, @NonNull Throwable e) {
        show(ctx, title, e, true);
    }

    public static void show(@NonNull Context ctx, @Nullable String title, @NonNull Throwable e, boolean cancelable) {
        Log.e(e);
        if (!SyncUtils.isMainProcess()) {
            // only show in main process
            return;
        }
        String t = TextUtils.isEmpty(title) ? Reflex.getShortClassName(e) : title;
        assert t != null;
        SyncUtils.runOnUiThread(() -> showImpl(ctx, t, e, cancelable));
    }

    public static void show(@NonNull Context ctx, @NonNull String title, @NonNull String msg) {
        show(ctx, title, msg, false);
    }

    public static void show(@NonNull Context ctx, @NonNull String title, @NonNull String msg, boolean cancelable) {
        SyncUtils.runOnUiThread(() -> showImpl(ctx, title, msg, cancelable));
    }

    private static void showImpl(@NonNull Context ctx, @NonNull String title, @NonNull Throwable e, boolean cancelable) {
        Context c = CommonContextWrapper.createAppCompatContext(ctx);
        new AlertDialog.Builder(c)
                .setTitle(title)
                .setMessage(Log.getStackTraceString(e))
                .setCancelable(cancelable)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private static void showImpl(@NonNull Context ctx, @NonNull String title, @NonNull CharSequence msg, boolean cancelable) {
        Context c = CommonContextWrapper.createAppCompatContext(ctx);
        new AlertDialog.Builder(c)
                .setTitle(title)
                .setMessage(msg)
                .setCancelable(cancelable)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
