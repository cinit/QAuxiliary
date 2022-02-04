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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.DexKit;
import io.github.qauxv.util.Log;
import java.util.List;

@FunctionHookEntry
@UiItemAgentEntry
public class FakeVipHook extends CommonSwitchFunctionHook {

    public static final FakeVipHook INSTANCE = new FakeVipHook();

    private FakeVipHook() {
        super(new int[]{DexKit.N_VIP_UTILS_getPrivilegeFlags});
    }

    @Override
    public boolean initOnce() {
        try {
            XposedBridge.hookMethod(
                    DexKit.doFindMethod(DexKit.N_VIP_UTILS_getPrivilegeFlags),
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            int ret;
                            //null is self
                            Object uin = param.args[param.args.length - 1];
                            if (uin == null) {
                                ret = (int) param.getResult();
                                param.setResult(2 | 4 | 8 | ret);//vip + svip + 大会员
                            }
                        }
                    });
            return true;
        } catch (Throwable e) {
            Log.e(e);
            return false;
        }
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.CHAT_CATEGORY;
    }

    @NonNull
    @Override
    public String getName() {
        return "非会员贴表情";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "此功能会导致误判SVIP，请谨慎使用";
    }

    @Nullable
    @Override
    public List<String> getExtraSearchKeywords() {
        return List.of("VIP", "SVIP", "贴表情", "贴收藏表情");
    }
}
