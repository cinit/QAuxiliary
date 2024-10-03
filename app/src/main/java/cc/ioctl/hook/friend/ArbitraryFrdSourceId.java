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
package cc.ioctl.hook.friend;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static cc.ioctl.util.HostStyledViewBuilder.newListItemDummy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostStyledViewBuilder;
import cc.ioctl.util.LayoutHelper;
import cc.ioctl.util.Reflex;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.UiThread;
import java.lang.reflect.Method;
import java.util.Objects;

@UiItemAgentEntry
@FunctionHookEntry
public class ArbitraryFrdSourceId extends CommonSwitchFunctionHook {

    public static final ArbitraryFrdSourceId INSTANCE = new ArbitraryFrdSourceId();

    private ArbitraryFrdSourceId() {
        super(true);
    }

    @Nullable
    @UiThread
    static ViewGroup findRlRootRecursive(@NonNull ViewGroup root) {
        if (root.getClass().getName().contains("BounceScrollView")) {
            ViewGroup bsv = root;
            return (ViewGroup) bsv.getChildAt(0);
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            View v = (root).getChildAt(i);
            if (v instanceof ViewGroup) {
                ViewGroup r = findRlRootRecursive((ViewGroup) v);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    @Nullable
    @UiThread
    static ViewGroup findRlRootLegacy(@NonNull Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        ViewGroup inner1 = (ViewGroup) content.getChildAt(0);
        for (int i = 0; i < inner1.getChildCount(); i++) {
            View v = inner1.getChildAt(i);
            if (v.getClass().getName().contains("BounceScrollView")) {
                ViewGroup bsv = (ViewGroup) v;
                return (ViewGroup) bsv.getChildAt(0);
            }
        }
        return null;
    }

    @UiThread
    static void initFunView(@NonNull Context ctx, @NonNull Intent intent, @NonNull ViewGroup relativeRoot) {
        Bundle argv = intent.getExtras();
        assert argv != null : "Intent extra for AddFriendVerifyActivity should not be null";
        int uinType = argv.getInt("k_uin_type", 0);
        // 1: user, 4: group
        if (uinType == 4) {
            //Pointless for group entry
            return;
        }
        ViewGroup bsv = (ViewGroup) relativeRoot.getParent();
        int dp10 = LayoutHelper.dip2px(ctx, 10);
        LinearLayout wrapper = new LinearLayout(ctx);
        wrapper.setOrientation(LinearLayout.VERTICAL);

        LinearLayout sourceAttrLayout = new LinearLayout(ctx);
        sourceAttrLayout.setOrientation(LinearLayout.VERTICAL);
        sourceAttrLayout.addView(HostStyledViewBuilder.subtitle(ctx, "来源参数"));
        sourceAttrLayout.addView(newListItemDummy(ctx, "Uin", null,
                String.valueOf(argv.getString("uin"))));
        sourceAttrLayout.addView(newListItemDummy(ctx, "SourceId", null,
                String.valueOf(argv.getInt("source_id", 3999))));
        sourceAttrLayout.addView(newListItemDummy(ctx, "SubSourceId", null,
                String.valueOf(argv.getInt("sub_source_id", 0))));
        sourceAttrLayout.addView(newListItemDummy(ctx, "Extra", null,
                String.valueOf(argv.getString("extra"))));
        sourceAttrLayout.addView(newListItemDummy(ctx, "Msg", null,
                String.valueOf(argv.getString("msg"))));
        sourceAttrLayout.setPadding(0, dp10, 0, dp10);

        ViewGroup.LayoutParams rlRootLp = relativeRoot.getLayoutParams();
        bsv.removeAllViews();
        wrapper.addView(relativeRoot, MATCH_PARENT, WRAP_CONTENT);
        wrapper.addView(sourceAttrLayout, MATCH_PARENT, WRAP_CONTENT);

        bsv.addView(wrapper, rlRootLp);
    }

    @Override
    public boolean initOnce() throws Exception {
        Class<?> KAddFriendVerifyFragmentForNT = Initiator.load("com.tencent.mobileqq.addfriend.ui.fornt.verify.AddFriendVerifyFragmentForNT");
        if (KAddFriendVerifyFragmentForNT != null) {
            Method doOnCreateView = KAddFriendVerifyFragmentForNT.getDeclaredMethod("doOnCreateView",
                    LayoutInflater.class, ViewGroup.class, Bundle.class);
            HookUtils.hookAfterIfEnabled(this, doOnCreateView, param -> {
                ViewGroup contentView = (ViewGroup) Reflex.getInstanceObject(param.thisObject, "mContentView", View.class);
                Objects.requireNonNull(contentView, "AddFriendVerifyFragmentForNT.this@QIphoneTitleBarFragment.mContentView should not be null");
                ViewGroup rlRoot = findRlRootRecursive(contentView);
                Objects.requireNonNull(rlRoot, "rl_root should not be null");
                Activity activity = (Activity) Reflex.invokeVirtual(param.thisObject, "getActivity");
                Objects.requireNonNull(activity, "activity should not be null");
                initFunView(activity, activity.getIntent(), rlRoot);
            });
        }
        Class<?> kAddFriendVerifyFragment = Initiator.load("com.tencent.mobileqq.addfriend.ui.AddFriendVerifyFragment");
        if (kAddFriendVerifyFragment != null) {
            Method doOnCreateView = kAddFriendVerifyFragment.getDeclaredMethod("doOnCreateView",
                    LayoutInflater.class, ViewGroup.class, Bundle.class);
            HookUtils.hookAfterIfEnabled(this, doOnCreateView, param -> {
                ViewGroup contentView = (ViewGroup) Reflex.getInstanceObject(param.thisObject, "mContentView", View.class);
                Objects.requireNonNull(contentView, "AddFriendVerifyFragment.this@QIphoneTitleBarFragment.mContentView should not be null");
                ViewGroup rlRoot = findRlRootRecursive(contentView);
                Objects.requireNonNull(rlRoot, "rl_root should not be null");
                Activity activity = (Activity) Reflex.invokeVirtual(param.thisObject, "getActivity");
                Objects.requireNonNull(activity, "activity should not be null");
                initFunView(activity, activity.getIntent(), rlRoot);
            });
        }
        Class<?> kABTestAddFriendVerifyFragment = Initiator.load("com.tencent.mobileqq.abtest.ABTestAddFriendVerifyFragment");
        if (kABTestAddFriendVerifyFragment != null) {
            Method doOnCreateView = kABTestAddFriendVerifyFragment.getDeclaredMethod("doOnCreateView",
                    LayoutInflater.class, ViewGroup.class, Bundle.class);
            HookUtils.hookAfterIfEnabled(this, doOnCreateView, param -> {
                ViewGroup contentView = (ViewGroup) Reflex.getInstanceObject(param.thisObject, "mContentView", View.class);
                Objects.requireNonNull(contentView, "ABTestAddFriendVerifyFragment.this@QIphoneTitleBarFragment.mContentView should not be null");
                ViewGroup rlRoot = findRlRootRecursive(contentView);
                Objects.requireNonNull(rlRoot, "rl_root should not be null");
                Activity activity = (Activity) Reflex.invokeVirtual(param.thisObject, "getActivity");
                Objects.requireNonNull(activity, "activity should not be null");
                initFunView(activity, activity.getIntent(), rlRoot);
            });
        }
        Class<?> kAddFriendVerifyActivity = Initiator.load("com.tencent.mobileqq.activity.AddFriendVerifyActivity");
        if (kAddFriendVerifyActivity != null) {
            Method AddFriendVerifyActivity_doOnCreate = null;
            for (Method m : kAddFriendVerifyActivity.getDeclaredMethods()) {
                if ("doOnCreate".equals(m.getName())) {
                    AddFriendVerifyActivity_doOnCreate = m;
                    break;
                }
            }
            if (AddFriendVerifyActivity_doOnCreate == null) {
                throw new NoSuchMethodException("AddFriendVerifyActivity_doOnCreate not found");
            }
            HookUtils.hookAfterIfEnabled(this, AddFriendVerifyActivity_doOnCreate, param -> {
                Activity ctx = (Activity) param.thisObject;
                initFunView(ctx, ctx.getIntent(), Objects.requireNonNull(findRlRootLegacy(ctx), "rl_root not found"));
            });
        }
        return true;
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.FRIEND_CATEGORY;
    }

    @NonNull
    @Override
    public String getName() {
        return "显示加好友来源参数";
    }
}
