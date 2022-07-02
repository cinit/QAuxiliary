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
import cc.ioctl.fragment.CustomSplashConfigFragment;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.activity.SettingsUiFragmentHostActivity;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonConfigFunctionHook;
import io.github.qauxv.ui.ResUtils;
import io.github.qauxv.util.IoUtils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import kotlin.Unit;
import kotlin.jvm.functions.Function3;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlowKt;

//自定义启动图
@FunctionHookEntry
@UiItemAgentEntry
public class CustomSplash extends CommonConfigFunctionHook {

    public static final CustomSplash INSTANCE = new CustomSplash();

    public static final String DIR_NANE_CONFIG_MISC = "qa_misc";
    public static final String FILE_NAME_SPLASH_LIGHT = "splash_light.png";
    public static final String FILE_NAME_SPLASH_DARK = "splash_dark.png";

    private static final String CFG_KEY_CUSTOM_LIGHT_SPLASH = "custom_light_splash";
    private static final String CFG_KEY_CUSTOM_DIFFERENT_DARK_SPLASH = "custom_different_dark_splash";

    private MutableStateFlow<String> mStateFlowStatus = null;

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
        if (mStateFlowStatus == null) {
            updateStateFlow();
        }
        return mStateFlowStatus;
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
            SettingsUiFragmentHostActivity.startFragmentWithContext(activity, CustomSplashConfigFragment.class);
            return Unit.INSTANCE;
        };
    }

    @Override
    public boolean initOnce() throws Exception {
        Method open = AssetManager.class.getDeclaredMethod("open", String.class, int.class);
        HookUtils.hookBeforeIfEnabled(this, open, 53, param -> {
            String fileName = (String) param.args[0];
            if ("splash.jpg".equals(fileName)
                    || "splash.png".equals(fileName)
                    || "splash_big.jpg".equals(fileName)
                    || "splash/splash_simple.png".equals(fileName)
                    || "splash/splash_big_simple.png".equals(fileName)) {
                boolean isNowDark = ResUtils.isInNightMode();
                InputStream is;
                if (isNowDark) {
                    is = openSplashDarkIfOverride();
                } else {
                    is = openSplashLightIfOverride();
                }
                if (is != null) {
                    param.setResult(is);
                }
            } else if ("splash_logo.png".equals(fileName)) {
                param.setResult(new ByteArrayInputStream(TRANSPARENT_PNG));
            }
        });
        return true;
    }

    private void updateStateFlow() {
        String state = isEnabled() ? "已开启" : "禁用";
        if (mStateFlowStatus == null) {
            mStateFlowStatus = StateFlowKt.MutableStateFlow(state);
        } else {
            mStateFlowStatus.setValue(state);
        }
    }

    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);
        updateStateFlow();
    }

    @Nullable
    public InputStream openSplashInputStream(@NonNull String which) throws IOException {
        File f = new File(HostInfo.getApplication().getFilesDir(), DIR_NANE_CONFIG_MISC + File.separator + which);
        if (f.exists() && f.isFile()) {
            return new FileInputStream(f);
        }
        return null;
    }

    @Nullable
    public InputStream openSplashLightIfOverride() throws IOException {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        if (isEnabled() && cfg.getBoolean(CFG_KEY_CUSTOM_LIGHT_SPLASH, false)) {
            return openSplashInputStream(FILE_NAME_SPLASH_LIGHT);
        }
        return null;
    }

    @Nullable
    public InputStream openSplashDarkIfOverride() throws IOException {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        if (isEnabled() && cfg.getBoolean(CFG_KEY_CUSTOM_DIFFERENT_DARK_SPLASH, false)) {
            return openSplashInputStream(FILE_NAME_SPLASH_DARK);
        }
        // for those who use a same splash for both light and dark
        if (isEnabled() && cfg.getBoolean(CFG_KEY_CUSTOM_LIGHT_SPLASH, false)) {
            return openSplashInputStream(FILE_NAME_SPLASH_LIGHT);
        }
        return null;
    }

    @NonNull
    public File getDarkSplashFile() {
        File dir = new File(HostInfo.getApplication().getFilesDir(), DIR_NANE_CONFIG_MISC);
        return new File(IoUtils.mkdirsOrThrow(dir), FILE_NAME_SPLASH_DARK);
    }

    @NonNull
    public File getLightSplashFile() {
        File dir = new File(HostInfo.getApplication().getFilesDir(), DIR_NANE_CONFIG_MISC);
        return new File(IoUtils.mkdirsOrThrow(dir), FILE_NAME_SPLASH_LIGHT);
    }

    public void setUseCustomLightSplash(boolean use) {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        cfg.putBoolean(CFG_KEY_CUSTOM_LIGHT_SPLASH, use);
    }

    public boolean isUseCustomLightSplash() {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        return cfg.getBoolean(CFG_KEY_CUSTOM_LIGHT_SPLASH, false);
    }

    public void setUseDifferentDarkSplash(boolean use) {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        cfg.putBoolean(CFG_KEY_CUSTOM_DIFFERENT_DARK_SPLASH, use);
    }

    public boolean isUseDifferentDarkSplash() {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        return cfg.getBoolean(CFG_KEY_CUSTOM_DIFFERENT_DARK_SPLASH, false);
    }
}
