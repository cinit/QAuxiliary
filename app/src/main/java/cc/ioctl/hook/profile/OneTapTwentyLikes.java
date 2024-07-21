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
package cc.ioctl.hook.profile;

import static cc.ioctl.util.Reflex.getFirstByType;

import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import io.github.qauxv.util.xpcompat.XposedBridge;
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
        return "一键20赞";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.PROFILE_CATEGORY;
    }

    @Override
    public boolean isAvailable() {
        return !HostInfo.isTim();
    }

    @Override
    public boolean initOnce() throws ReflectiveOperationException {
        Class<?> CardProfile = Initiator.loadClass("com.tencent.mobileqq.data.CardProfile");
        Method onClickVote = Reflex.findSingleMethod(Initiator._VoteHelper(), null, false,
                CardProfile, ImageView.class);
        for (Method m : Initiator.loadClass("com.tencent.mobileqq.activity.VisitorsActivity").getDeclaredMethods()) {
            if (m.getName().equals("onClick")) {
                HookUtils.hookBeforeIfEnabled(this, m, param -> {
                    View view = (View) param.args[0];
                    Object profile = view.getTag();
                    if (profile == null || !CardProfile.isInstance(profile)) return;
                    Object voteHelper = getFirstByType(param.thisObject, Initiator._VoteHelper());
                    for (int i = 0; i < 20; i++) {
                        onClickVote.invoke(voteHelper, profile, view);
                    }
                });
            }
        }

        Method onClickOnProfileCard = Reflex.findMethod(Initiator.loadClass("com.tencent.mobileqq.profilecard.base.component.AbsProfileHeaderComponent"),
                "handleVoteBtnClickForGuestProfile",
                Initiator.loadClass("com.tencent.mobileqq.data.Card"));
        HookUtils.hookBeforeIfEnabled(this, onClickOnProfileCard, param -> {
            for (int i = 0; i < 19; i++) {
                XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
            }
        });

        return true;
    }
}
