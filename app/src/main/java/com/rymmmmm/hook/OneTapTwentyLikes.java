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
package com.rymmmmm.hook;

import static cc.ioctl.util.Reflex.getFirstByType;

import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Method;

//回赞界面一键20赞
@FunctionHookEntry
@UiItemAgentEntry
public class OneTapTwentyLikes extends CommonSwitchFunctionHook {

    public static final OneTapTwentyLikes INSTANCE = new OneTapTwentyLikes();

    private OneTapTwentyLikes() {
    }

    @NonNull
    @Override
    public String getName() {
        return "回赞界面一键20赞";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.PROFILE_CATEGORY;
    }

    @Override
    public boolean initOnce() {
        for (Method m : Initiator.load("com.tencent.mobileqq.activity.VisitorsActivity").getDeclaredMethods()) {
            if (m.getName().equals("onClick")) {
                HookUtils.hookBeforeIfEnabled(this, m, param -> {
                    View view = (View) param.args[0];
                    Object tag = view.getTag();
                    Object likeClickListener = getFirstByType(param.thisObject, Initiator._VoteHelper());
                    Method onClick = likeClickListener.getClass().getDeclaredMethod("a",
                        tag.getClass(), ImageView.class);
                    for (int i = 0; i < 20; i++) {
                        onClick.invoke(likeClickListener, tag, (ImageView) view);
                    }
                });
            }
        }
        return true;
    }
}
