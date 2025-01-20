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
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.os.Bundle;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.dexkit.ChatSettingForTroop_InitUI_TIM;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.FormItem_TIM;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import kotlin.collections.ArraysKt;

@FunctionHookEntry
@UiItemAgentEntry
public class FuckOpenContactsActivity extends CommonSwitchFunctionHook {
    private Class<?> clz = Initiator.loadClass('com.tencent.mobileqq.activity.phone.PhoneMatchActivity');
  
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
        return !(clz == null);
    }

    @Override
    public boolean initOnce() throws Exception {
        Method _onCreate =  clz.getDeclaredMethod('onCreate', Bundle.class);

        HookUtils.hookAfterIfEnabled(this, _onCreate, (param) -> {
            Activity self = (Activity) param.thisObject;
            // Hook点太多太复杂 就这样简单粗暴罢(x
            self.finish();
        });
        return true;
    }

}
