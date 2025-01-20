package io.github.moonleeeaf.hook;

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

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Method;

@FunctionHookEntry
@UiItemAgentEntry
public class FuckOpenContactsActivity extends CommonSwitchFunctionHook {

    public static final FuckOpenContactsActivity INSTANCE = new FuckOpenContactsActivity();

    private FuckOpenContactsActivity() {
    }

    @NonNull
    @Override
    public String getName() {
        return "去你妈的打开通讯录";
    }

    @NonNull
    @Override
    public String getDescription() {
        return "理论支持任意NTQQ, 仅在 TIM NT 4.0.98 测试通过";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Simplify.MAIN_UI_CONTACT;
    }

    @Override
    public boolean isAvailable() {
        // 不知道怎么判定, 就先看看有没有这个类罢
        return Initiator.load("com.tencent.mobileqq.activity.phone.PhoneMatchActivity") != null;
    }

    @Override
    public boolean initOnce() throws Exception {
        // Lcom/tencent/mobileqq/activity/phone/PhoneMatchActivity;->doOnCreate(Landroid/os/Bundle;)Z
        Method _onCreate = Initiator.loadClass("com.tencent.mobileqq.activity.phone.PhoneMatchActivity")
                .getDeclaredMethod("doOnCreate", Bundle.class);

        HookUtils.hookAfterIfEnabled(this, _onCreate, (param) -> {
            Activity self = (Activity) param.thisObject;
            // Hook点太多太复杂 就这样简单粗暴罢(x
            self.finish();
        });
        return true;
    }

}
