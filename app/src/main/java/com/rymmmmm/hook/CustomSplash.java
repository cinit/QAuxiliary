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

import android.app.Activity;
import android.content.res.AssetManager;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.dialog.RikkaCustomSplash;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonConfigFunctionHook;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import kotlin.Unit;
import kotlin.jvm.functions.Function3;
import kotlinx.coroutines.flow.MutableStateFlow;

//自定义启动图
@FunctionHookEntry
@UiItemAgentEntry
public class CustomSplash extends CommonConfigFunctionHook {

    public static final CustomSplash INSTANCE = new CustomSplash();

    private static final byte[] TRANSPARENT_PNG = new byte[]{
            (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0D, (byte) 0x49, (byte) 0x48, (byte) 0x44, (byte) 0x52,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0x08, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1F, (byte) 0x15, (byte) 0xC4,
            (byte) 0x89, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0B, (byte) 0x49, (byte) 0x44, (byte) 0x41,
            (byte) 0x54, (byte) 0x08, (byte) 0xD7, (byte) 0x63, (byte) 0x60, (byte) 0x00, (byte) 0x02, (byte) 0x00,
            (byte) 0x00, (byte) 0x05, (byte) 0x00, (byte) 0x01, (byte) 0xE2, (byte) 0x26, (byte) 0x05, (byte) 0x9B,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x49, (byte) 0x45, (byte) 0x4E, (byte) 0x44,
            (byte) 0xAE, (byte) 0x42, (byte) 0x60, (byte) 0x82};

    private CustomSplash() {
        super();
    }

    @NonNull
    @Override
    public String getName() {
        return "自定义启动图";
    }

    @Nullable
    @Override
    public MutableStateFlow<String> getValueState() {
        return null;
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.MAIN_UI_MISC;
    }

    @NonNull
    @Override
    public Function3<IUiItemAgent, Activity, View, Unit> getOnUiItemClickListener() {
        return (agent, activity, view) -> {
            RikkaCustomSplash dialog = new RikkaCustomSplash();
            dialog.showDialog(activity);
            return Unit.INSTANCE;
        };
    }

    @Override
    public boolean initOnce() throws Exception {
        Method open = AssetManager.class.getDeclaredMethod("open", String.class, int.class);
        HookUtils.hookBeforeIfEnabled(this, open, 53, param -> {
            String fileName = (String) param.args[0];
            if ("splash.jpg".equals(fileName)
                    || "splash_big.jpg".equals(fileName)
                    || "splash/splash_simple.png".equals(fileName)
                    || "splash/splash_big_simple.png".equals(fileName)) {
                String customPath = RikkaCustomSplash.getCurrentSplashPath();
                if (customPath == null) {
                    return;
                }
                File f = new File(customPath);
                if (f.exists() && f.isFile() && f.canRead()) {
                    param.setResult(new FileInputStream(f));
                } else {
                    byte[] bytes = RikkaCustomSplash.getCurrentSplashData();
                    if (bytes != null) {
                        param.setResult(new ByteArrayInputStream(bytes));
                    }
                }
            }
            if ("splash_logo.png".equals(fileName)) {
                param.setResult(new ByteArrayInputStream(TRANSPARENT_PNG));
            }
        });
        return true;
    }

    @Override
    public boolean isEnabled() {
        return RikkaCustomSplash.IsEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        //not supported.
    }
}
