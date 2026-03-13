/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
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

package io.github.qauxv.loader.sbl.lsp101;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.qauxv.loader.hookapi.IClassLoaderHelper;
import io.github.qauxv.loader.hookapi.IHookBridge;
import io.github.qauxv.loader.hookapi.ILoaderService;
import io.github.qauxv.loader.sbl.BuildConfig;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

@RequiresApi(26)
public class Lsp101HookImpl implements IHookBridge, ILoaderService {

    public static final Lsp101HookImpl INSTANCE = new Lsp101HookImpl();
    public static XposedModule self = null;
    private static final String DEFAULT_LOG_TAG = "QAuxv";
    // leave it there, although we don't need it
    private IClassLoaderHelper mClassLoaderHelper;

    private Lsp101HookImpl() {
    }

    public static void init(@NonNull XposedModule base) {
        self = base;
        Lsp101HookWrapper.self = base;
    }

    @Override
    public int getApiLevel() {
        return self.getApiVersion();
    }

    @NonNull
    @Override
    public String getFrameworkName() {
        return self.getFrameworkName();
    }

    @NonNull
    @Override
    public String getFrameworkVersion() {
        return self.getFrameworkVersion();
    }

    @Override
    public long getFrameworkVersionCode() {
        return self.getFrameworkVersionCode();
    }

    @NonNull
    @Override
    public MemberUnhookHandle hookMethod(@NonNull Member member, @NonNull IMemberHookCallback callback, int priority) {
        return Lsp101HookWrapper.hookAndRegisterMethodCallback(member, callback, priority);
    }

    @Override
    public boolean isDeoptimizationSupported() {
        return true;
    }

    @Override
    public boolean deoptimize(@NonNull Member member) {
        return self.deoptimize((Executable) member);
    }

    @Nullable
    @Override
    public Object invokeOriginalMethod(@NonNull Method method, @Nullable Object thisObject, @NonNull Object[] args)
            throws NullPointerException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        XposedInterface.Invoker<?, Method> invoker = self.getInvoker(method);
        invoker.setType(XposedInterface.Invoker.Type.ORIGIN);
        return invoker.invoke(thisObject, args);
    }

    @Override
    public <T> void invokeOriginalConstructor(@NonNull Constructor<T> ctor, @NonNull T thisObject, @NonNull Object[] args)
            throws NullPointerException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        XposedInterface.CtorInvoker<T> invoker = self.getInvoker(ctor);
        invoker.setType(XposedInterface.Invoker.Type.ORIGIN);
        // invoke constructor as method, s.t. <init>(args...)V
        invoker.invoke(thisObject, args);
    }

    @NonNull
    @Override
    public <T> T newInstanceOrigin(@NonNull Constructor<T> constructor, @NonNull Object... args)
            throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, InstantiationException {
        XposedInterface.CtorInvoker<T> invoker = self.getInvoker(constructor);
        invoker.setType(XposedInterface.Invoker.Type.ORIGIN);
        return invoker.newInstance(args);
    }

    @Override
    public long getHookCounter() {
        return Lsp101HookWrapper.getHookCounter();
    }

    @Override
    public Set<Member> getHookedMethods() {
        return Collections.unmodifiableSet(Lsp101HookWrapper.getHookedMethodsRaw());
    }

    @NonNull
    @Override
    public String getEntryPointName() {
        return this.getClass().getName();
    }

    @NonNull
    @Override
    public String getLoaderVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public int getLoaderVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    @NonNull
    @Override
    public String getMainModulePath() {
        return self.getModuleApplicationInfo().sourceDir;
    }

    @Override
    public void log(@NonNull String msg) {
        int level = android.util.Log.INFO;
        self.log(level, DEFAULT_LOG_TAG, msg, null);
    }

    @Override
    public void log(@NonNull Throwable tr) {
        int level = android.util.Log.ERROR;
        String msg = tr.getMessage();
        if (msg == null) {
            msg = tr.getClass().getSimpleName();
        }
        self.log(level, DEFAULT_LOG_TAG, msg, tr);
    }

    @Nullable
    @Override
    public Object queryExtension(@NonNull String key, @Nullable Object... args) {
        return Lsp101ExtCmd.handleQueryExtension(key, args);
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

}
