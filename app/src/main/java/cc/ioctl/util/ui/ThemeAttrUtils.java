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
import android.util.TypedValue;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

public class ThemeAttrUtils {

    private ThemeAttrUtils() {
    }

    private static final TypedValue sValue = new TypedValue();

    @Nullable
    public static TypedValue resolveAttribute(@NonNull Context context, @AttrRes int attr) {
        if (!context.getTheme().resolveAttribute(attr, sValue, true)) {
            return null;
        }
        return sValue;
    }

    @NonNull
    public static TypedValue resolveAttrOrError(@NonNull Context context, @AttrRes int attr) {
        if (!context.getTheme().resolveAttribute(attr, sValue, true)) {
            throw new IllegalArgumentException("Could not resolve attribute " + attr);
        }
        return sValue;
    }

    @ColorInt
    public static int resolveColorOrDefaultColorRes(@NonNull Context context, int attr, @ColorRes int defaultValue) {
        if (!context.getTheme().resolveAttribute(attr, sValue, true)) {
            return ResourcesCompat.getColor(context.getResources(), defaultValue, context.getTheme());
        }
        return sValue.data;
    }

    @ColorInt
    public static int resolveColorOrDefaultColorInt(@NonNull Context context, int attr, @ColorInt int defaultValue) {
        if (!context.getTheme().resolveAttribute(attr, sValue, true)) {
            return defaultValue;
        }
        return sValue.data;
    }
}
