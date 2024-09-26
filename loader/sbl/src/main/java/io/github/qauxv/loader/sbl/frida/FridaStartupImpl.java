/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package io.github.qauxv.loader.sbl.frida;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.qauxv.loader.hookapi.IClassLoaderHelper;
import io.github.qauxv.loader.hookapi.ILoaderService;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class FridaStartupImpl implements ILoaderService {

    public static FridaStartupImpl INSTANCE = new FridaStartupImpl();

    private File mModulePath;
    private File mHostDataDir;
    @Nullable
    private Map<String, Method> mXblService;
    private IClassLoaderHelper mClassLoaderHelper;

    /*package*/ void setModulePath(@NonNull File modulePath) {
        mModulePath = modulePath;
    }

    /*package*/ void setHostDataDir(@NonNull File hostDataDir) {
        mHostDataDir = hostDataDir;
    }

    /*package*/ void setXblService(@Nullable Map<String, Method> xblService) {
        mXblService = xblService;
    }

    private FridaStartupImpl() {
    }

    private Object unsafeInvokeXblService(@NonNull Method m, @Nullable Object... args) {
        try {
            return m.invoke(null, args);
        } catch (ReflectiveOperationException e) {
            throw unsafeThrowForIteCause(e);
        }
    }

    @NonNull
    @Override
    public String getEntryPointName() {
        Method m;
        if (mXblService != null && (m = mXblService.get("GetEntryPointName")) != null) {
            return (String) unsafeInvokeXblService(m);
        }
        return FridaInjectEntry.class.getName();
    }

    @NonNull
    @Override
    public String getLoaderVersionName() {
        Method m;
        if (mXblService != null && (m = mXblService.get("GetLoaderVersionName")) != null) {
            return (String) unsafeInvokeXblService(m);
        }
        return io.github.qauxv.loader.sbl.BuildConfig.VERSION_NAME;
    }

    @Override
    public int getLoaderVersionCode() {
        Method m;
        if (mXblService != null && (m = mXblService.get("GetLoaderVersionCode")) != null) {
            return (int) unsafeInvokeXblService(m);
        }
        return io.github.qauxv.loader.sbl.BuildConfig.VERSION_CODE;
    }

    @NonNull
    @Override
    public String getMainModulePath() {
        return mModulePath.getAbsolutePath();
    }

    @Override
    public void log(@NonNull String msg) {
        android.util.Log.i("QAuxv", msg);
    }

    @Override
    public void log(@NonNull Throwable tr) {
        android.util.Log.e("QAuxv", tr.toString(), tr);
    }

    @Nullable
    @Override
    public Object queryExtension(@NonNull String key, @Nullable Object... args) {
        Method m;
        if (mXblService != null && (m = mXblService.get("QueryExtension")) != null) {
            return unsafeInvokeXblService(m, key, args);
        }
        return null;
    }

    @Nullable
    @Override
    public IClassLoaderHelper getClassLoaderHelper() {
        return mClassLoaderHelper;
    }

    @Override
    public void setClassLoaderHelper(@Nullable IClassLoaderHelper helper) {
        mClassLoaderHelper = helper;
    }

    private static <T extends Throwable> AssertionError unsafeThrow(Throwable e) throws T {
        throw (T) e;
    }

    private static AssertionError unsafeThrowForIteCause(Throwable e) {
        if (e instanceof InvocationTargetException) {
            Throwable cause = e.getCause();
            if (cause != null) {
                return unsafeThrow(cause);
            }
        }
        return unsafeThrow(e);
    }

}
