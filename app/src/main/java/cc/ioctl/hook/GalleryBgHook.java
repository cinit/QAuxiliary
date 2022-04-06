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
import cc.ioctl.util.HookUtils;
import io.github.qauxv.SyncUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.DexKit;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@FunctionHookEntry
@UiItemAgentEntry
public class GalleryBgHook extends CommonSwitchFunctionHook {

    public static final GalleryBgHook INSTANCE = new GalleryBgHook();

    private GalleryBgHook() {
        super(SyncUtils.PROC_PEAK, new int[]{DexKit.C_ABS_GAL_SCENE});
    }

    @Override
    public boolean initOnce() throws Exception {
        Class<?> kAIOGalleryActivity = Initiator.load("com.tencent.mobileqq.richmediabrowser.AIOGalleryActivity");
        if (kAIOGalleryActivity != null) {
            // for QQ >= 8.3.5
            Class<?> kBrowserBaseScene = Initiator.loadClass("com.tencent.richmediabrowser.view.BrowserBaseScene");
            Method onCreate = kBrowserBaseScene.getDeclaredMethod("onCreate");
            Field fBgView = kBrowserBaseScene.getDeclaredField("bgView");
            fBgView.setAccessible(true);
            HookUtils.hookAfterIfEnabled(this, onCreate, param -> {
                View v = (View) fBgView.get(param.thisObject);
                v.setBackgroundColor(0x00000000);
            });
        }
        Class<?> legacyAIOGalleryActivity = Initiator.load("com.tencent.mobileqq.activity.aio.photo.AIOGalleryActivity");
        if (legacyAIOGalleryActivity != null) {
            // for legacy QQ
            // com.tencent.mobileqq.activity.aio.photo.AIOGalleryActivity
            // source code from: ColorQQ by qiwu
            Class<?> kAbstractGalleryScene = DexKit.doFindClass(DexKit.C_ABS_GAL_SCENE);
            Method m = kAbstractGalleryScene.getDeclaredMethod("a", ViewGroup.class);
            Field fv = null;
            for (Field f : kAbstractGalleryScene.getDeclaredFields()) {
                if (f.getType().equals(View.class)) {
                    f.setAccessible(true);
                    fv = f;
                    break;
                }
            }
            if (fv == null) {
                throw new IllegalStateException("GalleryBgHook: targetView is null");
            }
            final Field targetView = fv;
            HookUtils.hookAfterIfEnabled(this, m, param -> {
                View v = (View) targetView.get(param.thisObject);
                v.setBackgroundColor(0x00000000);
            });
        }
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "聊天界面查看图片使用透明背景";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.CHAT_OTHER;
    }
}
