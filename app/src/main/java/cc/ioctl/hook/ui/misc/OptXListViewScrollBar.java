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

package cc.ioctl.hook.ui.misc;

import android.app.Activity;
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
import androidx.appcompat.app.AlertDialog;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.LayoutHelper;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedBridge;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.core.HookInstaller;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonConfigFunctionHook;
import io.github.qauxv.ui.ResUtils;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.Toasts;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import kotlin.Unit;
import kotlin.jvm.functions.Function3;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlowKt;

@UiItemAgentEntry
@FunctionHookEntry
public class OptXListViewScrollBar extends CommonConfigFunctionHook {

    public static final OptXListViewScrollBar INSTANCE = new OptXListViewScrollBar();
    private static final String[] SWITCH_ITEMS = {"默认", "隐藏", "半透明"};
    private static final String[] SWITCH_ITEMS_DETAIL = {"默认 (2.3 样式)", "隐藏滑条", "半透明滑条"};
    private static final String KEY_SCROLL_BAR_OPT_TYPE = "scroll_bar_opt_type";
    private MutableStateFlow<String> mStateFlow = null;

    private OptXListViewScrollBar() {
        super(SyncUtils.PROC_MAIN | SyncUtils.PROC_QZONE | SyncUtils.PROC_TOOL);
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

    @NonNull
    @Override
    public Function3<IUiItemAgent, Activity, View, Unit> getOnUiItemClickListener() {
        return (agent, activity, view) -> {
            showDialog(activity);
            return Unit.INSTANCE;
        };
    }

    private void showDialog(Activity ctx) {
        int current = getCurrentValue();
        new AlertDialog.Builder(ctx)
                .setTitle("修改滑条样式")
                .setSingleChoiceItems(SWITCH_ITEMS_DETAIL, current, (dialog, which) -> {
                    setCurrentValue(which);
                    if (current != which) {
                        Toasts.info(ctx, "重启" + HostInfo.getAppName() + "生效");
                    }
                    dialog.dismiss();
                    if (!isInitialized() && which != 0) {
                        HookInstaller.initializeHookForeground(ctx, OptXListViewScrollBar.INSTANCE);
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    @Nullable
    @Override
    public MutableStateFlow<String> getValueState() {
        if (mStateFlow == null) {
            updateStateValue();
        }
        return mStateFlow;
    }

    private void updateStateValue() {
        int i = getCurrentValue();
        if (mStateFlow == null) {
            mStateFlow = StateFlowKt.MutableStateFlow(SWITCH_ITEMS[i]);
        } else {
            mStateFlow.setValue(SWITCH_ITEMS[i]);
        }
    }

    @Override
    protected boolean initOnce() throws Exception {
        Class<?> kAbsListView = Initiator.loadClass("com.tencent.widget.AbsListView");
        Class<?> kScrollView = Initiator.loadClass("com.tencent.widget.ScrollView");
        Constructor<?> ctor1 = kAbsListView.getConstructor(Context.class);
        Constructor<?> ctorScrollView = kScrollView.getConstructor(Context.class, AttributeSet.class, int.class);
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
        XposedBridge.hookMethod(ctorScrollView, hook);
        if (ctor3 != null) {
            XposedBridge.hookMethod(ctor3, hook);
        }
        Class<?> kRecyclerView = Initiator.load("androidx.recyclerview.widget.RecyclerView");
        if (kRecyclerView != null) {
            XposedBridge.hookAllConstructors(kRecyclerView, hook);
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
        int i = INSTANCE.getCurrentValue();
        if (i == 1) {
            // hide scrollbar
            listView.setVerticalScrollBarEnabled(false);
        } else if (i == 2) {
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

    public int getCurrentValue() {
        int i = ConfigManager.getDefaultConfig().getInt(KEY_SCROLL_BAR_OPT_TYPE, 0);
        if (i < 0 || i >= SWITCH_ITEMS.length) {
            i = 0;
        }
        return i;
    }

    public void setCurrentValue(int value) {
        ConfigManager.getDefaultConfig().putInt(KEY_SCROLL_BAR_OPT_TYPE, value);
        updateStateValue();
    }

    @Override
    public void setEnabled(boolean value) {
        // no-op
    }

    @Override
    public boolean isEnabled() {
        return getCurrentValue() != 0;
    }
}
