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
public class HideChatDatabaseCorruptDialogHook extends CommonSwitchFunctionHook {

    public static final HideChatDatabaseCorruptDialogHook INSTANCE = new HideChatDatabaseCorruptDialogHook();

    private HideChatDatabaseCorruptDialogHook() {
    }

    @NonNull
    @Override
    public String getName() {
        return "屏蔽聊天记录异常对话框";
    }

    @NonNull
    @Override
    public String getDescription() {
        return "导入聊天记录数据库偶遇大粪qQ脑瘫不认库, 每次切换页面关图片弹对话框, 修复未果无法关闭(对话框), 拼尽全力无法憋笑";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Simplify.UI_MISC;
    }

    @Override
    public boolean isAvailable() {
        //暂时无法确定该类是什么时候添加的
        return Initiator.load("com.tencent.mobileqq.database.corrupt.DBFixDialogUI") != null;
    }

    @Override
    public boolean initOnce() throws Exception {
        // 7f113d58
        // Lcom/tencent/mobileqq/database/corrupt/DBFixDialogUI;->v(I)V
        Method mMethod = Initiator.loadClass("com.tencent.mobileqq.database.corrupt.DBFixDialogUI")
                .getDeclaredMethod("v", int.class);

        HookUtils.hookBeforeIfEnabled(this, mMethod, (param) -> {
            param.setResult(null);
        });
        return true;
    }

}
