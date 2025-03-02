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

package io.github.qauxv.loader.sbl.xp51;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.qauxv.loader.hookapi.IClassLoaderHelper;
import io.github.qauxv.loader.hookapi.IHookBridge;
import io.github.qauxv.loader.hookapi.ILoaderService;
import io.github.qauxv.loader.sbl.common.CheckUtils;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

public class Xp51HookImpl implements IHookBridge, ILoaderService {

    public static final Xp51HookImpl INSTANCE = new Xp51HookImpl();

    private IClassLoaderHelper mClassLoaderHelper;

    @Override
    public int getApiLevel() {
        return XposedBridge.getXposedVersion();
    }

    @NonNull
    @Override
    public String getFrameworkName() {
        return "Xposed";
    }

    @NonNull
    @Override
    public String getFrameworkVersion() {
        return String.valueOf(XposedBridge.getXposedVersion());
    }

    @Override
    public long getFrameworkVersionCode() {
        return XposedBridge.getXposedVersion();
    }

    @NonNull
    @Override
    public MemberUnhookHandle hookMethod(@NonNull Member member, @NonNull IMemberHookCallback callback, int priority) {
        CheckUtils.checkNonNull(member, "member");
        CheckUtils.checkNonNull(callback, "callback");
        // check member is method or constructor
        if (!(member instanceof java.lang.reflect.Method) && !(member instanceof java.lang.reflect.Constructor)) {
            throw new IllegalArgumentException("member must be method or constructor");
        }
        Xp51HookWrapper.Xp51HookCallback cb = new Xp51HookWrapper.Xp51HookCallback(callback, priority);
        XC_MethodHook.Unhook unhook = XposedBridge.hookMethod(member, cb);
        if (unhook == null) {
            throw new UnsupportedOperationException("XposedBridge.hookMethod return null for member: " + member);
        }
        // add to hooked methods set
        Xp51HookWrapper.getHookedMethodsRaw().add(member);
        return new Xp51HookWrapper.Xp51UnhookHandle(unhook, member, cb);
    }

    @Nullable
    public Object invokeOriginalMethod(@NonNull Method method, @Nullable Object thisObject, @NonNull Object[] args)
            throws NullPointerException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        CheckUtils.checkNonNull(method, "method");
        CheckUtils.checkNonNull(args, "args");
        return XposedBridge.invokeOriginalMethod(method, thisObject, args);
    }

    @Override
    public <T> void invokeOriginalConstructor(@NonNull Constructor<T> ctor, @NonNull T thisObject, @NonNull Object[] args)
            throws NullPointerException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        CheckUtils.checkNonNull(ctor, "ctor");
        CheckUtils.checkNonNull(thisObject, "thisObject");
        CheckUtils.checkNonNull(args, "args");
        XposedBridge.invokeOriginalMethod(ctor, thisObject, args);
    }

    @NonNull
    @Override
    public <T> T newInstanceOrigin(@NonNull Constructor<T> constructor, @NonNull Object... args)
            throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, InstantiationException {
        // TODO: 2024-07-22 allocate instance
        throw new UnsupportedOperationException("allocate instance is not supported");
    }

    @Override
    public boolean isDeoptimizationSupported() {
        return false;
    }

    @Override
    public boolean deoptimize(@NonNull Member member) {
        return false;
    }

    @Nullable
    @Override
    public Object queryExtension(@NonNull String key, @Nullable Object... args) {
        return Xp51ExtCmd.handleQueryExtension(key, args);
    }

    @NonNull
    @Override
    public String getEntryPointName() {
        return this.getClass().getName();
    }

    @NonNull
    @Override
    public String getLoaderVersionName() {
        return io.github.qauxv.loader.sbl.BuildConfig.VERSION_NAME;
    }

    @Override
    public int getLoaderVersionCode() {
        return io.github.qauxv.loader.sbl.BuildConfig.VERSION_CODE;
    }

    @NonNull
    @Override
    public String getMainModulePath() {
        return Xp51HookEntry.getModulePath();
    }

    @Override
    public void log(@NonNull String msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        XposedBridge.log(msg);
    }

    @Override
    public void log(@NonNull Throwable tr) {
        XposedBridge.log(tr);
    }

    @Override
    public void setClassLoaderHelper(@Nullable IClassLoaderHelper helper) {
        mClassLoaderHelper = helper;
    }

    @Nullable
    @Override
    public IClassLoaderHelper getClassLoaderHelper() {
        return mClassLoaderHelper;
    }

    @Override
    public long getHookCounter() {
        return Xp51HookWrapper.getHookCounter();
    }

    @Override
    public Set<Member> getHookedMethods() {
        // return a read-only set
        return Collections.unmodifiableSet(Xp51HookWrapper.getHookedMethodsRaw());
    }
}
