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

package cc.ioctl.hook;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.LayoutHelper;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.qauxv.SyncUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.ui.ResUtils;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@UiItemAgentEntry
@FunctionHookEntry
public class OptXListViewScrollBar extends CommonSwitchFunctionHook {

    public static final OptXListViewScrollBar INSTANCE = new OptXListViewScrollBar();

    private OptXListViewScrollBar() {
        super(SyncUtils.PROC_ANY);
    }

    @NonNull
    @Override
    public String getName() {
        return "替换滑条样式";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "替换 XListView 滑条安卓 2.3 样式";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.UI_MISC;
    }

    @Override
    protected boolean initOnce() throws Exception {
        Class<?> kAbsListView = Initiator.loadClass("com.tencent.widget.AbsListView");
        Constructor<?> ctor1 = kAbsListView.getConstructor(Context.class);
        Constructor<?> ctor3;
        try {
            ctor3 = kAbsListView.getConstructor(Context.class, AttributeSet.class, int.class);
        } catch (NoSuchMethodException ignored) {
            ctor3 = null;
        }
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws ReflectiveOperationException {
                ViewGroup vg = (ViewGroup) param.thisObject;
                if (isEnabled()) {
                    patchAbsListViewScrollBars(vg);
                }
            }
        };
        XposedBridge.hookMethod(ctor1, hook);
        if (ctor3 != null) {
            XposedBridge.hookMethod(ctor3, hook);
        }
        return true;
    }

    private static Drawable createScrollBarDrawable(@NonNull Context ctx) {
        int color = ResUtils.isInNightMode() ? 0x40A0A0A0 : 0x403C4043;
        return new ColorDrawable(color);
    }

    private static Field fScrollCache = null;
    private static Field fScrollBar = null;
    private static Method sSetVerticalThumbDrawable = null;

    private static void patchAbsListViewScrollBars(@NonNull ViewGroup listView) throws ReflectiveOperationException {
        Context ctx = listView.getContext();
        int scrollBarSize = LayoutHelper.dip2px(ctx, 4);
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            listView.setVerticalScrollbarThumbDrawable(createScrollBarDrawable(ctx));
            listView.setScrollBarSize(scrollBarSize);
        } else {
            // Android is always hiding f*** APIs and not exposing them
            // use reflection to make it work
            // XListView should have already initialized the scrollbars
            if (fScrollCache == null) {
                fScrollCache = View.class.getDeclaredField("mScrollCache");
                fScrollCache.setAccessible(true);
            }
            if (fScrollBar == null) {
                fScrollBar = fScrollCache.getType().getDeclaredField("scrollBar");
                fScrollBar.setAccessible(true);
            }
            if (sSetVerticalThumbDrawable == null) {
                Class<?> kScrollBarDrawable = fScrollBar.getType();
                sSetVerticalThumbDrawable = kScrollBarDrawable.getDeclaredMethod("setVerticalThumbDrawable", Drawable.class);
                sSetVerticalThumbDrawable.setAccessible(true);
            }
            Object scrollCache = fScrollCache.get(listView);
            if (scrollCache != null) {
                Object scrollBar = fScrollBar.get(scrollCache);
                if (scrollBar != null) {
                    sSetVerticalThumbDrawable.invoke(scrollBar, createScrollBarDrawable(ctx));
                    listView.setScrollBarSize(scrollBarSize);
                }
            }
        }
    }
}
