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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import io.github.qauxv.lifecycle.Parasitics;
import io.github.qauxv.util.IoUtils;
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
    private LayoutInflater mInflater = null;

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

    public static boolean isAppCompatContext(@NonNull Context context) {
        if (!checkContextClassLoader(context)) {
            return false;
        }
        TypedArray a = context.obtainStyledAttributes(androidx.appcompat.R.styleable.AppCompatTheme);
        try {
            return a.hasValue(androidx.appcompat.R.styleable.AppCompatTheme_windowActionBar);
        } finally {
            a.recycle();
        }
    }

    private static final int[] MATERIAL_CHECK_ATTRS = {com.google.android.material.R.attr.colorPrimaryVariant};

    public static boolean isMaterialDesignContext(@NonNull Context context) {
        if (!isAppCompatContext(context)) {
            return false;
        }
        @SuppressLint("ResourceType") TypedArray a = context.obtainStyledAttributes(MATERIAL_CHECK_ATTRS);
        try {
            return a.hasValue(0);
        } finally {
            a.recycle();
        }
    }

    public static boolean checkContextClassLoader(@NonNull Context context) {
        try {
            ClassLoader cl = context.getClassLoader();
            if (cl == null) {
                return false;
            }
            return cl.loadClass(AppCompatActivity.class.getName()) == AppCompatActivity.class;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @NonNull
    public static Context createAppCompatContext(@NonNull Context base) {
        if (isAppCompatContext(base)) {
            return base;
        }
        return new CommonContextWrapper(base, ModuleThemeManager.getCurrentStyleId(),
            recreateNighModeConfig(base, ResUtils.getNightModeMasked()));
    }

    @NonNull
    public static Context createMaterialDesignContext(@NonNull Context base) {
        if (isMaterialDesignContext(base)) {
            return base;
        }
        // currently all themes by createAppCompatContext are material themes
        // change this if you have a AppCompat theme that is not material theme
        return createAppCompatContext(base);
    }

    @SuppressLint("PrivateApi")
    private static Context getBaseContextImpl(@NonNull Context context) {
        Class<?> kContextImpl;
        try {
            kContextImpl = Class.forName("android.app.ContextImpl");
        } catch (ClassNotFoundException e) {
            throw IoUtils.unsafeThrow(e);
        }
        Context c;
        while ((context instanceof ContextWrapper) && (c = ((ContextWrapper) context).getBaseContext()) != null) {
            context = c;
        }
        if (!kContextImpl.isInstance(context)) {
            throw new UnsupportedOperationException("unable to get base context from " + context.getClass().getName());
        }
        return context;
    }

    @Override
    public Object getSystemService(String name) {
        // QQ has a custom layout_inflater in robot AIO, which will cause androidx context class loader mismatches.
        // E.g. ClassCastException with same class name but different classloaders when showing an androidx dialog.
        if (LAYOUT_INFLATER_SERVICE.equals(name)) {
            if (mInflater == null) {
                mInflater = LayoutInflater.from(getBaseContextImpl(getBaseContext())).cloneInContext(this);
            }
            return mInflater;
        }
        return getBaseContext().getSystemService(name);
    }

}
