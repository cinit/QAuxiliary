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

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import io.github.qauxv.SyncUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.DexKit;
import java.lang.reflect.Field;

@FunctionHookEntry
@UiItemAgentEntry
public class GalleryBgHook extends CommonSwitchFunctionHook {

    private static final GalleryBgHook INSTANCE = new GalleryBgHook();

    private GalleryBgHook() {
        super(SyncUtils.PROC_PEAK, new int[]{DexKit.C_ABS_GAL_SCENE});
    }

    @Override
    public boolean initOnce() throws Exception {
        XposedHelpers.findAndHookMethod(DexKit.doFindClass(DexKit.C_ABS_GAL_SCENE),
                "a", ViewGroup.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!isEnabled()) {
                            return;
                        }
                        for (Field f : param.method.getDeclaringClass().getDeclaredFields()) {
                            if (f.getType().equals(View.class)) {
                                f.setAccessible(true);
                                View v = (View) f.get(param.thisObject);
                                v.setBackgroundColor(0x00000000);
                                return;
                            }
                        }
                    }
                });
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "聊天界面查看图片使用透明背景";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "[仅限QQ8.1.0以下版本]来源于ColorQQ";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.CHAT_OTHER;
    }
}
