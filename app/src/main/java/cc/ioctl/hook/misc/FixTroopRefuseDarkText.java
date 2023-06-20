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

package cc.ioctl.hook.misc;

import android.os.Bundle;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.hicore.QApp.QAppUtils;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.ui.ResUtils;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.SyncUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@FunctionHookEntry
@UiItemAgentEntry
public class FixTroopRefuseDarkText extends CommonSwitchFunctionHook {

    public static final FixTroopRefuseDarkText INSTANCE = new FixTroopRefuseDarkText();

    private FixTroopRefuseDarkText() {
        super(SyncUtils.PROC_MAIN);
    }

    @NonNull
    @Override
    public String getName() {
        return "修复拒绝加群原因文本背景";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "修复夜间模式下群申请拒绝消息中的拒绝原因看不清的问题";
    }

    @Override
    public boolean isAvailable() {
        return !QAppUtils.isQQnt();
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.MISC_CATEGORY;
    }

    @Override
    protected boolean initOnce() throws Exception {
        Class<?> kTroopRequestRefuseActivity = Initiator.load("com.tencent.mobileqq.activity.TroopRequestRefuseActivity");
        if (kTroopRequestRefuseActivity == null) {
            kTroopRequestRefuseActivity = Initiator.load("com.tencent.mobileqq.troop.troopnotification.activity.TroopRequestRefuseActivity");
        }
        if (kTroopRequestRefuseActivity == null) {
            throw new ClassNotFoundException("com.tencent.mobileqq.**.TroopRequestRefuseActivity");
        }
        Method doOnCreate = kTroopRequestRefuseActivity.getDeclaredMethod("doOnCreate", Bundle.class);
        Field field = null;
        for (Field f : kTroopRequestRefuseActivity.getDeclaredFields()) {
            if (f.getType() == EditText.class) {
                field = f;
                break;
            }
        }
        if (field == null) {
            throw new IllegalStateException("unable to find refuse reason edit text");
        }
        Field fRefuseReason = field;
        fRefuseReason.setAccessible(true);
        HookUtils.hookAfterIfEnabled(this, doOnCreate, param -> {
            // only for night mode
            if (!ResUtils.isInNightMode()) {
                return;
            }
            EditText refuseReason = (EditText) fRefuseReason.get(param.thisObject);
            if (refuseReason != null) {
                // set background to 50% black and text color to white
                refuseReason.setBackgroundColor(0x80000000);
                refuseReason.setTextColor(0xFFFFFFFF);
            }
        });
        return true;
    }
}
