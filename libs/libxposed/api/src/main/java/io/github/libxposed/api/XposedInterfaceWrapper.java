package io.github.libxposed.api;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.annotation.RequiresApi;
import io.github.libxposed.api.annotations.XposedApiExact;
import io.github.libxposed.api.annotations.XposedApiMin;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wrapper of {@link XposedInterface} used by modules to shield framework implementation details.
 */
public class XposedInterfaceWrapper implements XposedInterface {

    private volatile XposedInterface mBase;

    /**
     * Attaches the framework interface to the module. Modules should never call this method.
     *
     * @param base The framework interface
     */
    @SuppressWarnings("unused")
    @XposedApiMin(101)
    public final void attachFramework(@NonNull XposedInterface base) {
        if (mBase != null) {
            throw new IllegalStateException("Framework already attached");
        }
        mBase = base;
    }

    @XposedApiMin(101)
    private void ensureAttached() {
        if (mBase == null) {
            throw new IllegalStateException("Framework not attached");
        }
    }

    @XposedApiMin(101)
    @Override
    public final int getApiVersion() {
        ensureAttached();
        return XposedInterface.super.getApiVersion();
    }

    @NonNull
    @Override
    public final String getFrameworkName() {
        ensureAttached();
        return mBase.getFrameworkName();
    }

    @NonNull
    @Override
    public final String getFrameworkVersion() {
        ensureAttached();
        return mBase.getFrameworkVersion();
    }

    @Override
    public final long getFrameworkVersionCode() {
        ensureAttached();
        return mBase.getFrameworkVersionCode();
    }

    @XposedApiExact(100)
    @Override
    public final int getFrameworkPrivilege() {
        return mBase.getFrameworkPrivilege();
    }

    @XposedApiMin(101)
    @Override
    public final long getFrameworkProperties() {
        ensureAttached();
        return mBase.getFrameworkProperties();
    }

    @XposedApiMin(101)
    @RequiresApi(26)
    @NonNull
    @Override
    public final HookBuilder hook(@NonNull Executable origin) {
        ensureAttached();
        return mBase.hook(origin);
    }

    @XposedApiMin(101)
    @NonNull
    @Override
    public final HookBuilder hookClassInitializer(@NonNull Class<?> origin) {
        ensureAttached();
        return mBase.hookClassInitializer(origin);
    }

    @XposedApiMin(101)
    @RequiresApi(26)
    @Override
    public final boolean deoptimize(@NonNull Executable executable) {
        ensureAttached();
        return mBase.deoptimize(executable);
    }

    @XposedApiMin(101)
    @NonNull
    @Override
    public final Invoker<?, Method> getInvoker(@NonNull Method method) {
        ensureAttached();
        return mBase.getInvoker(method);
    }

    @XposedApiMin(101)
    @NonNull
    @Override
    public final <T> CtorInvoker<T> getInvoker(@NonNull Constructor<T> constructor) {
        ensureAttached();
        return mBase.getInvoker(constructor);
    }

    @XposedApiExact(100)
    @NonNull
    @Override
    public final MethodUnhooker<Method> hook(@NonNull Method origin, @NonNull Class<? extends Hooker> hooker) {
        return mBase.hook(origin, hooker);
    }

    @XposedApiExact(100)
    @NonNull
    @Override
    public <T> MethodUnhooker<Constructor<T>> hookClassInitializer(@NonNull Class<T> origin, @NonNull Class<? extends Hooker> hooker) {
        return mBase.hookClassInitializer(origin, hooker);
    }

    @XposedApiExact(100)
    @NonNull
    @Override
    public <T> MethodUnhooker<Constructor<T>> hookClassInitializer(@NonNull Class<T> origin, int priority, @NonNull Class<? extends Hooker> hooker) {
        return mBase.hookClassInitializer(origin, priority, hooker);
    }

    @XposedApiExact(100)
    @NonNull
    @Override
    public final MethodUnhooker<Method> hook(@NonNull Method origin, int priority, @NonNull Class<? extends Hooker> hooker) {
        return mBase.hook(origin, priority, hooker);
    }

    @XposedApiExact(100)
    @NonNull
    @Override
    public final <T> MethodUnhooker<Constructor<T>> hook(@NonNull Constructor<T> origin, @NonNull Class<? extends Hooker> hooker) {
        return mBase.hook(origin, hooker);
    }

    @XposedApiExact(100)
    @NonNull
    @Override
    public final <T> MethodUnhooker<Constructor<T>> hook(@NonNull Constructor<T> origin, int priority, @NonNull Class<? extends Hooker> hooker) {
        return mBase.hook(origin, priority, hooker);
    }

    @XposedApiExact(100)
    @Override
    public final boolean deoptimize(@NonNull Method method) {
        return mBase.deoptimize(method);
    }

    @XposedApiExact(100)
    @Override
    public final <T> boolean deoptimize(@NonNull Constructor<T> constructor) {
        return mBase.deoptimize(constructor);
    }

    @XposedApiExact(100)
    @Nullable
    @Override
    public final Object invokeOrigin(@NonNull Method method, @Nullable Object thisObject, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException {
        return mBase.invokeOrigin(method, thisObject, args);
    }

    @XposedApiExact(100)
    @Override
    public <T> void invokeOrigin(@NonNull Constructor<T> constructor, @NonNull T thisObject, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException {
        mBase.invokeOrigin(constructor, thisObject, args);
    }

    @XposedApiExact(100)
    @Nullable
    @Override
    public final Object invokeSpecial(@NonNull Method method, @NonNull Object thisObject, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException {
        return mBase.invokeSpecial(method, thisObject, args);
    }

    @XposedApiExact(100)
    @Override
    public <T> void invokeSpecial(@NonNull Constructor<T> constructor, @NonNull T thisObject, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException {
        mBase.invokeSpecial(constructor, thisObject, args);
    }

    @XposedApiExact(100)
    @NonNull
    @Override
    public final <T> T newInstanceOrigin(@NonNull Constructor<T> constructor, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, InstantiationException {
        return mBase.newInstanceOrigin(constructor, args);
    }

    @XposedApiExact(100)
    @NonNull
    @Override
    public final <T, U> U newInstanceSpecial(@NonNull Constructor<T> constructor, @NonNull Class<U> subClass, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, InstantiationException {
        return mBase.newInstanceSpecial(constructor, subClass, args);
    }

    @XposedApiExact(100)
    @Override
    public final void log(@NonNull String message) {
        mBase.log(message);
    }

    @XposedApiExact(100)
    @Override
    public final void log(@NonNull String message, @NonNull Throwable throwable) {
        mBase.log(message, throwable);
    }

    @XposedApiMin(101)
    @Override
    public final void log(int priority, @Nullable String tag, @NonNull String msg) {
        ensureAttached();
        mBase.log(priority, tag, msg);
    }

    @XposedApiMin(101)
    @Override
    public final void log(int priority, @Nullable String tag, @NonNull String msg, @Nullable Throwable tr) {
        ensureAttached();
        mBase.log(priority, tag, msg, tr);
    }

    @NonNull
    @Override
    public final SharedPreferences getRemotePreferences(@NonNull String name) {
        ensureAttached();
        return mBase.getRemotePreferences(name);
    }

    @XposedApiMin(101)
    @NonNull
    @Override
    public final ApplicationInfo getModuleApplicationInfo() {
        ensureAttached();
        return mBase.getModuleApplicationInfo();
    }

    @XposedApiExact(100)
    @NonNull
    @Override
    public ApplicationInfo getApplicationInfo() {
        return mBase.getApplicationInfo();
    }

    @NonNull
    @Override
    public final String[] listRemoteFiles() {
        ensureAttached();
        return mBase.listRemoteFiles();
    }

    @NonNull
    @Override
    public final ParcelFileDescriptor openRemoteFile(@NonNull String name) throws FileNotFoundException {
        ensureAttached();
        return mBase.openRemoteFile(name);
    }
}
