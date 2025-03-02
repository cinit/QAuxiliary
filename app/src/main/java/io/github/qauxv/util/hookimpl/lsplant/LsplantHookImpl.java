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

package io.github.qauxv.util.hookimpl.lsplant;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.Reflex;
import io.github.qauxv.loader.hookapi.IHookBridge;
import io.github.qauxv.poststartup.StartupInfo;
import io.github.qauxv.util.IoUtils;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.Natives;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class LsplantHookImpl {

    private static boolean isInitialized = false;

    private LsplantHookImpl() {
    }

    public static void initializeLsplantHookBridge() {
        if (StartupInfo.getHookBridge() != null) {
            throw new IllegalStateException("Hook bridge already initialized");
        }
        // if we reach here, it's obvious that LSPlant is required
        // without a hook bridge, we can't do anything
        // if that fails, just want to give up
        initializeLsplantInternal();
        StartupInfo.setHookBridge(LsplantHookBridge.INSTANCE);
        Log.d("LSPlant initialization done.");
    }

    private static class LsplantHookBridge implements IHookBridge {

        private LsplantHookBridge() {
        }

        public static final LsplantHookBridge INSTANCE = new LsplantHookBridge();

        @Override
        public int getApiLevel() {
            // because we do not support Xposed standard, so we return 0
            return 0;
        }

        @NonNull
        @Override
        public String getFrameworkName() {
            return "LSPlant";
        }

        @NonNull
        @Override
        public String getFrameworkVersion() {
            // update this when LSPlant is updated
            return "6.4";
        }

        @Override
        public long getFrameworkVersionCode() {
            return 1;
        }

        @NonNull
        @Override
        public MemberUnhookHandle hookMethod(@NonNull Member member, @NonNull IMemberHookCallback callback, int priority) {
            return hookMethodImpl(member, callback, priority);
        }

        @Override
        public boolean isDeoptimizationSupported() {
            return true;
        }

        @Override
        public boolean deoptimize(@NonNull Member member) {
            checkMemberValid(member);
            return LsplantBridge.nativeDeoptimizeMethod(member);
        }

        @Nullable
        @Override
        public Object invokeOriginalMethod(@NonNull Method method, @Nullable Object thisObject, @NonNull Object[] args)
                throws NullPointerException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            return invokeOriginalMemberImpl(method, thisObject, args);
        }

        @Override
        public <T> void invokeOriginalConstructor(@NonNull Constructor<T> ctor, @NonNull T thisObject, @NonNull Object[] args)
                throws NullPointerException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            invokeOriginalMemberImpl(ctor, thisObject, args);
        }

        @NonNull
        @Override
        public <T> T newInstanceOrigin(@NonNull Constructor<T> constructor, @NonNull Object... args)
                throws InvocationTargetException, IllegalArgumentException, IllegalAccessException {
            checkMemberValid(constructor);
            T instance = (T) Natives.allocateInstanceImpl(constructor.getDeclaringClass());
            invokeOriginalMemberImpl(constructor, instance, args);
            return instance;
        }

        @Override
        public long getHookCounter() {
            return sHookCounter.get() - 1;
        }

        @Override
        public Set<Member> getHookedMethods() {
            // return a read-only set
            return Collections.unmodifiableSet(sHookedMethods);
        }
    }

    private static Member sCallbackMethod;
    private static AtomicLong sHookCounter = new AtomicLong(0);
    private static final Set<Member> sHookedMethods = ConcurrentHashMap.newKeySet();

    private static synchronized void initializeLsplantInternal() {
        if (isInitialized) {
            return;
        }
        try {
            sCallbackMethod = LsplantCallbackToken.class.getDeclaredMethod("callback", Object[].class);
        } catch (NoSuchMethodException e) {
            // should not happen, unless R8 is doing something wired
            throw IoUtils.unsafeThrow(e);
        }
        LsplantBridge.nativeInitializeLsplant();
        isInitialized = true;
    }

    private static void checkMemberValid(@NonNull Member target) {
        // must be method or constructor
        if (!(target instanceof Method) && !(target instanceof Constructor)) {
            throw new IllegalArgumentException("Only method or constructor is supported, got " + target);
        }
        if (target instanceof Method) {
            // non abstract
            if ((target.getModifiers() & Modifier.ABSTRACT) != 0) {
                throw new IllegalArgumentException("method must not be abstract");
            }
        }
    }

    private static HashSet<Member> sExemptMembers = null;
    private static HashSet<Class<?>> sExemptClasses = null;

    private static HashSet<Member> getExemptMembers() {
        if (sExemptMembers == null) {
            synchronized (LsplantHookImpl.class) {
                if (sExemptMembers == null) {
                    try {
                        HashSet<Member> h = new HashSet<>(16);
                        h.add(System.class.getDeclaredMethod("load", String.class));
                        h.add(System.class.getDeclaredMethod("loadLibrary", String.class));
                        // add more if you need
                        sExemptMembers = h;
                    } catch (NoSuchMethodException e) {
                        throw IoUtils.unsafeThrow(e);
                    }
                }
            }
        }
        return sExemptMembers;
    }

    private static HashSet<Class<?>> getExemptClasses() {
        if (sExemptClasses == null) {
            synchronized (LsplantHookImpl.class) {
                if (sExemptClasses == null) {
                    HashSet<Class<?>> h = new HashSet<>(16);
                    // add more if you need
                    h.add(Runtime.class);
                    sExemptClasses = h;
                }
            }
        }
        return sExemptClasses;
    }

    private static void checkHookTarget(@NonNull Member target) {
        checkMemberValid(target);
        Class<?> clazz = target.getDeclaringClass();
        if (clazz.getClassLoader() == Runnable.class.getClassLoader()) {
            if (getExemptClasses().contains(clazz)) {
                return;
            }
            if (getExemptMembers().contains(target)) {
                return;
            }
            if (clazz.getName().startsWith("java.lang.")) {
                throw new IllegalArgumentException("Cannot hook java.lang.* classes");
            } else if (clazz.getName().startsWith("java.util.")) {
                throw new IllegalArgumentException("Cannot hook java.util.* classes");
            }
        }
        if (clazz.getName().startsWith("io.github.qauxv.")) {
            throw new IllegalArgumentException("Cannot hook io.github.qauxv.* classes");
        }
    }

    public static class CallbackWrapper implements IHookBridge.MemberUnhookHandle {

        private CallbackWrapper(@NonNull IHookBridge.IMemberHookCallback callback, @NonNull Member target, int priority) {
            this.callback = callback;
            this.target = target;
            this.priority = priority;
        }

        public final IHookBridge.IMemberHookCallback callback;
        public final Member target;
        public final int priority;
        public final long hookId = sHookCounter.getAndIncrement();
        private boolean active = true;

        @NonNull
        @Override
        public Member getMember() {
            return target;
        }

        @NonNull
        @Override
        public IHookBridge.IMemberHookCallback getCallback() {
            return callback;
        }

        @Override
        public boolean isHookActive() {
            return active;
        }

        @Override
        public void unhook() {
            unhookMethodImpl(this);
            active = false;
        }
    }

    // -------------- implementation dividing line --------------

    private static final CallbackWrapper[] EMPTY_CALLBACKS = new CallbackWrapper[0];

    // WARNING: This will only work for Android 7.0 and above.
    // Since SDK 24, Method.equals() and Method.hashCode() can correctly compare hooked methods.
    // Before SDK 24, equals() uses AbstractMethod which is not safe for hooked methods.
    // If you need to support lower versions, go and read cs.android.com.
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Member, CallbackListHolder>> sCallbackRegistry = new ConcurrentHashMap<>();
    private static final Object sRegistryWriteLock = new Object();

    public static class CallbackListHolder {

        @NonNull
        public final Object lock = new Object();
        // sorted by priority, descending
        @NonNull
        public CallbackWrapper[] callbacks = EMPTY_CALLBACKS;
        // token for LSPlant, after method is hooked, this will be set
        @Nullable
        public LsplantCallbackToken token = null;

    }

    private static IHookBridge.MemberUnhookHandle hookMethodImpl(@NonNull Member member, @NonNull IHookBridge.IMemberHookCallback callback, int priority) {
        checkHookTarget(member);
        Objects.requireNonNull(callback);
        CallbackWrapper wrapper = new CallbackWrapper(callback, member, priority);
        Class<?> declaringClass = member.getDeclaringClass();
        CallbackListHolder holder;
        synchronized (sRegistryWriteLock) {
            ConcurrentHashMap<Member, CallbackListHolder> map = sCallbackRegistry.get(declaringClass);
            if (map == null) {
                map = new ConcurrentHashMap<>(2);
                sCallbackRegistry.put(declaringClass, map);
            }
            holder = map.get(member);
            if (holder == null) {
                holder = new CallbackListHolder();
                map.put(member, holder);
            }
        }
        synchronized (holder.lock) {
            // step 1. check if the method is already hooked
            if (holder.token == null) {
                // underlying ArtMethod is not hooked, we need to hook it before adding callback
                LsplantCallbackToken token = new LsplantCallbackToken(member);
                // perform hook
                Method backup = LsplantBridge.nativeHookMethod(member, sCallbackMethod, token);
                if (backup == null) {
                    throw new UnsupportedOperationException("LSPlant failed to hook method: " + member);
                }
                backup.setAccessible(true);
                // hook success, set backup method
                token.setBackupMember(backup);
                // add token to holder
                holder.token = token;
                // add to hooked methods set
                sHookedMethods.add(member);
            }
            // step 2. add callback to list, descending order by priority
            int newSize = holder.callbacks.length + 1;
            CallbackWrapper[] newCallbacks = new CallbackWrapper[newSize];
            int i = 0;
            for (; i < holder.callbacks.length; i++) {
                if (holder.callbacks[i].priority > priority) {
                    newCallbacks[i] = holder.callbacks[i];
                } else {
                    break;
                }
            }
            newCallbacks[i] = wrapper;
            for (; i < holder.callbacks.length; i++) {
                newCallbacks[i + 1] = holder.callbacks[i];
            }
            holder.callbacks = newCallbacks;
        }
        return wrapper;
    }

    private static void unhookMethodImpl(@NonNull CallbackWrapper callback) {
        Member target = callback.target;
        Class<?> declaringClass = target.getDeclaringClass();
        CallbackListHolder holder;
        // ConcurrentHashMap is thread-safe, so we don't need to synchronize here
        ConcurrentHashMap<Member, CallbackListHolder> map1 = sCallbackRegistry.get(declaringClass);
        if (map1 == null) {
            return;
        }
        holder = map1.get(target);
        if (holder == null) {
            return;
        }
        synchronized (holder.lock) {
            // remove callback from list
            int newSize = holder.callbacks.length - 1;
            if (newSize == 0) {
                holder.callbacks = EMPTY_CALLBACKS;
            } else {
                CallbackWrapper[] newCallbacks = new CallbackWrapper[newSize];
                int j = 0;
                for (int i = 0; i < holder.callbacks.length; i++) {
                    if (holder.callbacks[i] != callback) {
                        newCallbacks[j++] = holder.callbacks[i];
                    }
                }
                holder.callbacks = newCallbacks;
            }
            // if no more callbacks, unhook the method
            if (holder.callbacks.length == 0) {
                LsplantBridge.nativeUnhookMethod(target);
                holder.token = null;
            }
        }
    }

    private static Object invokeOriginalMemberImpl(@NonNull Member method, @Nullable Object thisObject, @NonNull Object[] args)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(args, "args");
        checkMemberValid(method);
        // Constructors may have a static modifier, for <clinit>()V, if anyone is trying to hook it.
        boolean hasThis = !Modifier.isStatic(method.getModifiers());
        if (hasThis && thisObject == null) {
            throw new NullPointerException("thisObject is null for method " + method);
        }
        if (Modifier.isAbstract(method.getModifiers())) {
            // Anyone trying to call invokeOriginalMethod on an abstract method?
            // CHA lookup in case anyone really does that
            method = Reflex.virtualMethodLookup(method, thisObject);
        }
        // perform a lookup
        Class<?> declaringClass = method.getDeclaringClass();
        declaringClass.cast(thisObject);
        LsplantCallbackToken token = null;
        ConcurrentHashMap<Member, CallbackListHolder> map1 = sCallbackRegistry.get(declaringClass);
        if (map1 != null) {
            CallbackListHolder holder = map1.get(method);
            if (holder != null) {
                synchronized (holder.lock) {
                    token = holder.token;
                }
            }
        }
        if (token != null) {
            Method backup = token.getBackupMember();
            return backup.invoke(thisObject, args);
        } else {
            // method is not hooked, invoke the original method/constructor directly
            return Natives.invokeNonVirtualArtMethodNoDeclaringClassCheck(method, declaringClass, thisObject, args);
        }
    }

    @NonNull
    public static CallbackWrapper[] getActiveHookCallbacks(@NonNull Member method) {
        Objects.requireNonNull(method, "method");
        Class<?> declaringClass = method.getDeclaringClass();
        ConcurrentHashMap<Member, CallbackListHolder> map1 = sCallbackRegistry.get(declaringClass);
        if (map1 == null) {
            return EMPTY_CALLBACKS;
        }
        CallbackListHolder holder = map1.get(method);
        if (holder == null) {
            return EMPTY_CALLBACKS;
        }
        synchronized (holder.lock) {
            // perform a copy
            return holder.callbacks.clone();
        }
    }

}
