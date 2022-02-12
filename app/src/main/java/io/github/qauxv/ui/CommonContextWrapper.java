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

package io.github.qauxv.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.ContextThemeWrapper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.qauxv.R;
import io.github.qauxv.lifecycle.Parasitics;
import io.github.qauxv.util.SavedInstanceStatePatchedClassReferencer;
import java.util.Objects;

/**
 * If you just want to create a MaterialDialog or AppCompatDialog, see {@link #createMaterialDesignContext(Context)} and
 * {@link #createAppCompatContext(Context)}
 **/
public class CommonContextWrapper extends ContextThemeWrapper {

    /**
     * Creates a new context wrapper with the specified theme with correct module ClassLoader.
     *
     * @param base  the base context
     * @param theme the resource ID of the theme to be applied on top of the base context's theme
     */
    public CommonContextWrapper(@NonNull Context base, int theme) {
        this(base, theme, null);
    }

    /**
     * Creates a new context wrapper with the specified theme with correct module ClassLoader.
     *
     * @param base          the base context
     * @param theme         the resource ID of the theme to be applied on top of the base context's theme
     * @param configuration the configuration to override the base one
     */
    public CommonContextWrapper(@NonNull Context base, int theme,
                                @Nullable Configuration configuration) {
        super(base, theme);
        if (configuration != null) {
            mOverrideResources = base.createConfigurationContext(configuration).getResources();
        }
        Parasitics.injectModuleResources(getResources());
    }

    private ClassLoader mXref = null;
    private Resources mOverrideResources;

    @NonNull
    @Override
    public ClassLoader getClassLoader() {
        if (mXref == null) {
            mXref = new SavedInstanceStatePatchedClassReferencer(
                CommonContextWrapper.class.getClassLoader());
        }
        return mXref;
    }

    @Nullable
    private static Configuration recreateNighModeConfig(@NonNull Context base, int uiNightMode) {
        Objects.requireNonNull(base, "base is null");
        Configuration baseConfig = base.getResources().getConfiguration();
        if ((baseConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK) == uiNightMode) {
            // config for base context is already what we want,
            // just return null to avoid unnecessary override
            return null;
        }
        Configuration conf = new Configuration();
        conf.uiMode = uiNightMode | (baseConfig.uiMode & ~Configuration.UI_MODE_NIGHT_MASK);
        return conf;
    }

    @NonNull
    @Override
    public Resources getResources() {
        if (mOverrideResources == null) {
            return super.getResources();
        } else {
            return mOverrideResources;
        }
    }

    @NonNull
    public static CommonContextWrapper createAppCompatContext(@NonNull Context base) {
        return new CommonContextWrapper(base, androidx.appcompat.R.style.Theme_AppCompat_DayNight,
            recreateNighModeConfig(base, ResUtils.getNightModeMasked()));
    }

    @NonNull
    public static CommonContextWrapper createMaterialDesignContext(@NonNull Context base) {
        return new CommonContextWrapper(base, R.style.Theme_MaiTungTMDesign,
            recreateNighModeConfig(base, ResUtils.getNightModeMasked()));
    }
}
