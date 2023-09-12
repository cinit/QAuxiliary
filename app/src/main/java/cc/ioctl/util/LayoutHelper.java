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

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import cc.hicore.Utils.ContextUtils;
import io.github.qauxv.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LayoutHelper {

    private LayoutHelper() {
    }

    public static final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;
    public static final int MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT;

    /**
     * 根据手机的分辨率从 dip 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static float dip2sp(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density / context.getResources()
            .getDisplayMetrics().scaledDensity;
        return dpValue * scale + 0.5f;
    }

    /**
     * 将px值转换为sp值，保证文字大小不变
     */
    public static int px2sp(Context context, float pxValue) {
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    public static LinearLayout.LayoutParams newLinearLayoutParams(int width, int height, int left, int top, int right,
                                                                  int bottom) {
        LinearLayout.LayoutParams ret = new LinearLayout.LayoutParams(width, height);
        ret.setMargins(left, top, right, bottom);
        return ret;
    }

    public static LinearLayout.LayoutParams newLinearLayoutParams(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    public static LinearLayout.LayoutParams newLinearLayoutParams(int width, int height, int gravity, int left, int top,
                                                                  int right, int bottom) {
        LinearLayout.LayoutParams ret = new LinearLayout.LayoutParams(width, height);
        ret.setMargins(left, top, right, bottom);
        ret.gravity = gravity;
        return ret;
    }

    public static LinearLayout.LayoutParams newLinearLayoutParams(int width, int height, int margins) {
        return newLinearLayoutParams(width, height, margins, margins, margins, margins);
    }

    public static RelativeLayout.LayoutParams newRelativeLayoutParamsM(int width, int height, int left, int top,
                                                                       int right, int bottom, int... verbArgv) {
        RelativeLayout.LayoutParams ret = new RelativeLayout.LayoutParams(width, height);
        ret.setMargins(left, top, right, bottom);
        for (int i = 0; i < verbArgv.length / 2; i++) {
            ret.addRule(verbArgv[i * 2], verbArgv[i * 2 + 1]);
        }
        return ret;
    }

    public static RelativeLayout.LayoutParams newRelativeLayoutParams(int width, int height, int... verbArgv) {
        RelativeLayout.LayoutParams ret = new RelativeLayout.LayoutParams(width, height);
        for (int i = 0; i < verbArgv.length / 2; i++) {
            ret.addRule(verbArgv[i * 2], verbArgv[i * 2 + 1]);
        }
        return ret;
    }

    public static FrameLayout.LayoutParams newFrameLayoutParamsDp(@NonNull Context ctx, int width, int height,
                                                                  int gravity, float left, float top, float right,
                                                                  float bottom, float start, float end) {
        FrameLayout.LayoutParams ret = new FrameLayout.LayoutParams(width > 0 ? dip2px(ctx, width) : width,
            height > 0 ? dip2px(ctx, height) : height);
        ret.setMargins(dip2px(ctx, left), dip2px(ctx, top), dip2px(ctx, right), dip2px(ctx, bottom));
        ret.setMarginStart(dip2px(ctx, start));
        ret.setMarginEnd(dip2px(ctx, end));
        ret.gravity = gravity;
        return ret;
    }

    public static FrameLayout.LayoutParams newFrameLayoutParamsDp(@NonNull Context ctx, int width, int height,
                                                                  int gravity, float left, float top, float right,
                                                                  float bottom) {
        return newFrameLayoutParamsDp(ctx, width, height, gravity, left, top, right, bottom, 0, 0);
    }

    public static FrameLayout.LayoutParams newFrameLayoutParamsAbs(int width, int height, int gravity,
                                                                   int left, int top,int right, int bottom) {
        FrameLayout.LayoutParams ret = new FrameLayout.LayoutParams(width, height);
        ret.setMargins(left, top, right, bottom);
        ret.gravity = gravity;
        return ret;
    }

    public static FrameLayout.LayoutParams newFrameLayoutParamsRel(int width, int height, int gravity,
                                                                   int start, int top, int end, int bottom) {
        FrameLayout.LayoutParams ret = new FrameLayout.LayoutParams(width, height);
        ret.setMargins(0, top, 0, bottom);
        ret.setMarginStart(start);
        ret.setMarginEnd(end);
        ret.gravity = gravity;
        return ret;
    }

    public static int[] getStyleableValues(String str) {
        try {
            return (int[]) Class.forName("com.android.internal.R$styleable").getField(str).get(null);
        } catch (Exception e) {
            return new int[0];
        }
    }

    private static Method sInitializeScrollbars = null;
    private static int[] sViewAttributes = null;

    public static void initializeScrollbars(@NonNull ViewGroup viewGroup) {
        if (sInitializeScrollbars == null) {
            try {
                sInitializeScrollbars = View.class.getDeclaredMethod("initializeScrollbars", TypedArray.class);
                sInitializeScrollbars.setAccessible(true);
            } catch (NoSuchMethodException e) {
                Log.e(e);
                throw new NoSuchMethodError(e.getMessage());
            }
        }
        if (sViewAttributes == null) {
            sViewAttributes = getStyleableValues("View");
        }
        TypedArray obtainStyledAttributes = viewGroup.getContext().obtainStyledAttributes(getStyleableValues("View"));
        try {
            sInitializeScrollbars.invoke(viewGroup, obtainStyledAttributes);
        } catch (IllegalAccessException e) {
            // should not happen
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            Log.e(e);
        } finally {
            obtainStyledAttributes.recycle();
        }
    }
    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }
    public static boolean isSmallWindowNeedPlay(View v) {
        Rect rect = new Rect();
        boolean visibleRect = v.getGlobalVisibleRect(rect);

        if (visibleRect) {
            Point point = new Point();
            Context baseContext = v.getContext();
            if (!(baseContext instanceof Activity) && (baseContext instanceof ContextWrapper)) {
                baseContext = ((ContextWrapper)baseContext).getBaseContext();
            }


            if (baseContext instanceof Activity) {
                ((Activity) baseContext).getWindowManager().getDefaultDisplay().getSize(point);

                return rect.top >= 0 && (rect.top - 100) <= point.y && rect.left >= 0 && rect.left <= point.x;
            }
        }
        return false;
    }
}
