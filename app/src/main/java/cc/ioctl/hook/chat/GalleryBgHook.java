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
package cc.ioctl.hook.chat;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.HostInfo;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.dexkit.CAbsGalScene;
import io.github.qauxv.util.dexkit.CGalleryBaseScene;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@FunctionHookEntry
@UiItemAgentEntry
public class GalleryBgHook extends CommonSwitchFunctionHook {

    public static final GalleryBgHook INSTANCE = new GalleryBgHook();

    private GalleryBgHook() {
        super(SyncUtils.PROC_PEAK | SyncUtils.PROC_MAIN, new DexKitTarget[]{CAbsGalScene.INSTANCE, CGalleryBaseScene.INSTANCE});
    }

    @Override
    public boolean initOnce() throws Exception {
        // for QQ NT
        Class<?> kRFWLayerAnimPart = Initiator.load("com.tencent.richframework.gallery.part.RFWLayerAnimPart");
        if (kRFWLayerAnimPart != null) {
            Method m = kRFWLayerAnimPart.getDeclaredMethod("initStartAnim", ImageView.class);
            HookUtils.hookAfterIfEnabled(this, m, param -> {
                Object mDragLayout = Reflex.getInstanceObject(param.thisObject,
                        HostInfo.requireMinQQVersion(QQVersion.QQ_9_0_56) ? "dragLayout" : "mDragLayout",
                        null);
                Reflex.setInstanceObject(mDragLayout, "mWindowBgDrawable", new ColorDrawable(Color.TRANSPARENT));
            });
            Method m2 = kRFWLayerAnimPart.getDeclaredMethod("updateBackgroundAlpha", int.class);
            HookUtils.hookBeforeIfEnabled(this, m2, param -> {
                param.args[0] = 0;
            });
        }
        // for QQ >= 8.3.5
        Class<?> kBrowserBaseScene = DexKit.loadClassFromCache(CGalleryBaseScene.INSTANCE);
        if (kBrowserBaseScene != null) {
            Method m;
            try {
                m = kBrowserBaseScene.getDeclaredMethod("a", ViewGroup.class);
            } catch (NoSuchMethodException e) {
                m = kBrowserBaseScene.getDeclaredMethod("onCreate");
            }
            Field fv = null;
            for (Field f : kBrowserBaseScene.getDeclaredFields()) {
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
        Class<?> legacyAIOGalleryActivity = Initiator.load("com.tencent.mobileqq.activity.aio.photo.AIOGalleryActivity");
        if (legacyAIOGalleryActivity != null) {
            // for legacy QQ
            // com.tencent.mobileqq.activity.aio.photo.AIOGalleryActivity
            // source code from: ColorQQ by qiwu
            Class<?> kAbstractGalleryScene = DexKit.requireClassFromCache(CAbsGalScene.INSTANCE);
            Method m = Reflex.findSingleMethod(kAbstractGalleryScene, void.class, false, ViewGroup.class);
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
