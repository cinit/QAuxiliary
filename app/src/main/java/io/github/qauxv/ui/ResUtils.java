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

import static io.github.qauxv.bridge.AppRuntimeHelper.getAppRuntime;
import static io.github.qauxv.util.Initiator._ThemeUtil;
import static io.github.qauxv.util.Initiator.load;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.NinePatch;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import io.github.qauxv.util.xpcompat.XposedHelpers;
import io.github.qauxv.util.Log;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ResUtils {

    private ResUtils() {
    }

    private static final Map<String, Drawable> sCachedDrawable = new HashMap<>();

    public static InputStream openAsset(String name) {
        return Objects.requireNonNull(ResUtils.class.getClassLoader()).getResourceAsStream("assets/" + name);
    }

    public static Drawable loadDrawableFromAsset(String name, Context mContext) {
        if (mContext != null) {
            return loadDrawableFromAsset(name, mContext.getResources(), mContext);
        } else {
            return loadDrawableFromAsset(name, null, null);
        }
    }

    public static Drawable loadDrawableFromStream(InputStream in, String name, @Nullable Resources res) {
        Drawable ret;
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            // qq has xhdpi
            bitmap.setDensity(320);
            byte[] chunk = bitmap.getNinePatchChunk();
            if (NinePatch.isNinePatchChunk(chunk)) {
                Class<?> clz = load("com/tencent/theme/SkinnableNinePatchDrawable");
                ret = (Drawable) XposedHelpers.findConstructorBestMatch(clz, Resources.class, Bitmap.class,
                        byte[].class, Rect.class, String.class)
                    .newInstance(res, bitmap, chunk, new Rect(), name);
            } else {
                ret = new BitmapDrawable(res, bitmap);
            }
            return ret.mutate();
        } catch (Exception e) {
            Log.e(e);
        }
        return null;
    }

    public static Drawable loadDrawableFromAsset(String name, @Nullable Resources res, Context mContext) {
        Drawable ret;
        if ((ret = sCachedDrawable.get(name)) != null) {
            return ret;
        }
        try {
            if (res == null && mContext != null) {
                res = mContext.getResources();
            }
            InputStream fin = openAsset(name);
            ret = loadDrawableFromStream(fin, name, res);
            sCachedDrawable.put(name, ret);
            return ret;
        } catch (Exception e) {
            Log.e(e);
        }
        return null;
    }

    public static boolean isInNightMode() {
        try {
            String themeId = (String) Reflex.invokeStatic(_ThemeUtil(),
                "getUserCurrentThemeId", getAppRuntime(), load("mqq/app/AppRuntime"));
            return "1103".endsWith(themeId) || "2920".endsWith(themeId) || "2963".endsWith(themeId);
        } catch (Exception e) {
            if (HostInfo.isTim() || HostInfo.isQQHD()) {
                return false;
            }
            Log.e(e);
            return false;
        }
    }

    public static int getNightModeMasked() {
        return isInNightMode() ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO;
    }
}
