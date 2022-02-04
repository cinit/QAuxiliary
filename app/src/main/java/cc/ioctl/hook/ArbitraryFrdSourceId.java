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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostStyledViewBuilder;
import cc.ioctl.util.LayoutHelper;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.UiThread;
import java.lang.reflect.Method;

@UiItemAgentEntry
@FunctionHookEntry
public class ArbitraryFrdSourceId extends CommonSwitchFunctionHook {

    public static final ArbitraryFrdSourceId INSTANCE = new ArbitraryFrdSourceId();

    private ArbitraryFrdSourceId() {
        super(true);
    }

    @UiThread
    static ViewGroup[] findRlRootAndParent(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        ViewGroup inner1 = (ViewGroup) content.getChildAt(0);
        for (int i = 0; i < inner1.getChildCount(); i++) {
            View v = inner1.getChildAt(i);
            if (v.getClass().getName().contains("BounceScrollView")) {
                ViewGroup bsv = (ViewGroup) v;
                return new ViewGroup[]{(ViewGroup) bsv.getChildAt(0), bsv};
            }
        }
        return null;
    }

    @UiThread
    static void initFunView(Activity ctx) {
        Intent intent = ctx.getIntent();
        Bundle argv = intent.getExtras();
        assert argv != null : "Intent extra for AddFriendVerifyActivity should not be null";
        int uinType = argv.getInt("k_uin_type", 0);
        if (uinType == 4) {
            //Pointless for group entry
            return;
        }
        ViewGroup[] tmp = findRlRootAndParent(ctx);
        RelativeLayout rl_root = (RelativeLayout) tmp[0];
        ViewGroup bsv = tmp[1];
        int __10_ = LayoutHelper.dip2px(ctx, 10);
        LinearLayout wrapper = new LinearLayout(ctx);
        wrapper.setOrientation(LinearLayout.VERTICAL);

        LinearLayout sourceAttrLayout = new LinearLayout(ctx);
        sourceAttrLayout.setOrientation(LinearLayout.VERTICAL);
        sourceAttrLayout.addView(HostStyledViewBuilder.subtitle(ctx, "来源参数"));
        sourceAttrLayout.addView(HostStyledViewBuilder.newListItemDummy(ctx, "SourceId", null,
            String.valueOf(argv.getInt("source_id", 3999))));
        sourceAttrLayout.addView(HostStyledViewBuilder.newListItemDummy(ctx, "SubSourceId", null,
            String.valueOf(argv.getInt("sub_source_id", 0))));
        sourceAttrLayout.addView(HostStyledViewBuilder.newListItemDummy(ctx, "Extra", null,
            String.valueOf(argv.getString("extra"))));
        sourceAttrLayout.addView(
            HostStyledViewBuilder.newListItemDummy(ctx, "Msg", null,
                String.valueOf(argv.getString("msg"))));
        sourceAttrLayout.setPadding(0, __10_, 0, __10_);

        ViewGroup.LayoutParams rl_root_lp = rl_root.getLayoutParams();
        bsv.removeAllViews();
        wrapper.addView(rl_root, MATCH_PARENT, WRAP_CONTENT);
        wrapper.addView(sourceAttrLayout, MATCH_PARENT, WRAP_CONTENT);

        bsv.addView(wrapper, rl_root_lp);
    }

    @Override
    public boolean initOnce() throws Exception {
        Method AddFriendVerifyActivity_doOnCreate = null;
        for (Method m : Initiator.load("com.tencent.mobileqq.activity.AddFriendVerifyActivity")
            .getDeclaredMethods()) {
            if (m.getName().equals("doOnCreate")) {
                AddFriendVerifyActivity_doOnCreate = m;
                break;
            }
        }
        if (AddFriendVerifyActivity_doOnCreate == null) {
            throw new NoSuchMethodException("AddFriendVerifyActivity_doOnCreate not found");
        }
        HookUtils.hookAfterIfEnabled(this, AddFriendVerifyActivity_doOnCreate, param -> {
            Activity ctx = (Activity) param.thisObject;
            initFunView(ctx);
        });
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
