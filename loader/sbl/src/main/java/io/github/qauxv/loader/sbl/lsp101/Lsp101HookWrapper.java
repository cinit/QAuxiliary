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
import io.github.libxposed.api.annotations.XposedApiMin;
import io.github.qauxv.loader.hookapi.IHookBridge;
import io.github.qauxv.loader.sbl.common.CheckUtils;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RequiresApi(26)
@XposedApiMin(101)
public class Lsp101HookWrapper {

    private Lsp101HookWrapper() {
        throw new AssertionError("No instance for you!");
    }

    public static XposedModule self = null;

    private static final CallbackWrapper[] EMPTY_CALLBACKS = new CallbackWrapper[0];
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private static final AtomicLong sNextHookId = new AtomicLong(1);
    private static final Set<Member> sHookedMethods = ConcurrentHashMap.newKeySet();

    private static final Object sRegistryWriteLock = new Object();

    private static final int DEFAULT_PRIORITY = 50;

    public interface Hooker extends XposedInterface.Hooker {

        /**
         * This is the actual hook callback interface for libxposed 101. See link io.github.libxposed.api.XposedInterface.Hooker#intercept
         */
        Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable;

    }

    public static class CallbackWrapper {

        public final IHookBridge.IMemberHookCallback callback;
        public final long hookId = sNextHookId.getAndIncrement();
        public final int priority;

        public CallbackWrapper(IHookBridge.IMemberHookCallback callback, int priority) {
            this.callback = callback;
            this.priority = priority;
        }
    }

    public static class CallbackListHolder {

        public final Object lock = new Object();
        // sorted by priority, descending
        public CallbackWrapper[] callbacks = EMPTY_CALLBACKS;

    }

    public static UnhookHandle hookAndRegisterMethodCallback(
            final @NonNull Member method,
            final @NonNull IHookBridge.IMemberHookCallback callback,
            final int priority
    ) {
        CheckUtils.checkNonNull(method, "method");
        CheckUtils.checkNonNull(callback, "callback");
        CallbackWrapper wrapper = new CallbackWrapper(callback, priority);
        UnhookHandle handle = new UnhookHandle(wrapper, method);
        Class<?> declaringClass = method.getDeclaringClass();
        CallbackListHolder holder;
        synchronized (sRegistryWriteLock) {
            // 1. select the callback list by priority
            ConcurrentHashMap<Class<?>, ConcurrentHashMap<Member, CallbackListHolder>> taggedCallbackRegistry = sCallbackRegistry.get(priority);
            if (taggedCallbackRegistry == null) {
                taggedCallbackRegistry = new ConcurrentHashMap<>();
                sCallbackRegistry.put(priority, taggedCallbackRegistry);
            }
            // 2. check if the method is already hooked
            ConcurrentHashMap<Member, CallbackListHolder> callbackList = taggedCallbackRegistry.get(declaringClass);
            if (callbackList == null) {
                callbackList = new ConcurrentHashMap<>();
                taggedCallbackRegistry.put(declaringClass, callbackList);
            }
            holder = callbackList.get(method);
            if (holder == null) {
                // 3. tell the underlying framework to hook the method
                if (method instanceof Executable) {
                    Lsp101HookDispatchAgent agent = new Lsp101HookDispatchAgent(priority);
                    XposedInterface.HookHandle hookHandle = self.hook((Executable) method).setPriority(priority).intercept(agent);
                    agent.setFrameworkHookHandle(hookHandle);
                } else {
                    throw new IllegalArgumentException("only method and constructor can be hooked, but got " + method);
                }
                // 4. create a new holder
                CallbackListHolder newHolder = new CallbackListHolder();
                callbackList.put(method, newHolder);
                holder = newHolder;
                // add to hooked methods set
                sHookedMethods.add(method);
            }
        }
        // 4. add the callback to the holder
        synchronized (holder.lock) {
            // add and sort descending
            int newSize = holder.callbacks == null ? 1 : holder.callbacks.length + 1;
            CallbackWrapper[] newCallbacks = new CallbackWrapper[newSize];
            if (holder.callbacks != null) {
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
            } else {
                newCallbacks[0] = wrapper;
            }
            holder.callbacks = newCallbacks;
        }
        return handle;
    }

    public static void removeMethodCallback(@NonNull Member method, final @NonNull CallbackWrapper callback) {
        CheckUtils.checkNonNull(method, "method");
        CheckUtils.checkNonNull(callback, "callback");
        // find the callback holder
        final int tag = callback.priority;
        ConcurrentHashMap<Class<?>, ConcurrentHashMap<Member, CallbackListHolder>> taggedCallbackRegistry = sCallbackRegistry.get(tag);
        if (taggedCallbackRegistry == null) {
            return;
        }
        ConcurrentHashMap<Member, CallbackListHolder> callbackList = taggedCallbackRegistry.get(method.getDeclaringClass());
        if (callbackList == null) {
            return;
        }
        CallbackListHolder holder = callbackList.get(method);
        if (holder == null) {
            return;
        }
        // remove the callback
        synchronized (holder.lock) {
            ArrayList<CallbackWrapper> newCallbacks = new ArrayList<>();
            for (CallbackWrapper cb : holder.callbacks) {
                if (cb != callback) {
                    newCallbacks.add(cb);
                }
            }
            holder.callbacks = newCallbacks.toArray(new CallbackWrapper[0]);
        }
    }

    public static boolean isMethodCallbackRegistered(@NonNull Member method, @NonNull CallbackWrapper callback) {
        CheckUtils.checkNonNull(method, "method");
        CheckUtils.checkNonNull(callback, "callback");
        final int tag = callback.priority;
        ConcurrentHashMap<Class<?>, ConcurrentHashMap<Member, CallbackListHolder>> taggedCallbackRegistry = sCallbackRegistry.get(tag);
        if (taggedCallbackRegistry == null) {
            return false;
        }
        // find the callback holder
        ConcurrentHashMap<Member, CallbackListHolder> callbackList = taggedCallbackRegistry.get(method.getDeclaringClass());
        if (callbackList == null) {
            return false;
        }
        CallbackListHolder holder = callbackList.get(method);
        if (holder == null) {
            return false;
        }
        // read only, not need to lock
        CallbackWrapper[] callbacks = holder.callbacks;
        for (CallbackWrapper cb : callbacks) {
            if (cb == callback) {
                return true;
            }
        }
        return false;
    }

    private static CallbackWrapper[] copyCallbacks(@Nullable CallbackListHolder holder) {
        if (holder == null) {
            return EMPTY_CALLBACKS;
        }
        synchronized (holder.lock) {
            if (holder.callbacks != null) {
                return holder.callbacks.clone();
            }
        }
        return EMPTY_CALLBACKS;
    }

    public static class InvocationParamWrapper implements IHookBridge.IMemberHookParam {

        public int index = -1;
        public boolean isAfter = false;
        // sorted by priority, descending
        public CallbackWrapper[] callbacks;
        // create on demand, per-callback extras
        public Object[] extras;

        // control flags shared with API 101 Hooker
        public boolean skipOriginal;
        public Object result;
        public Throwable throwable;
        public Object thisObjectCompat;
        public Object[] argsCompat;
        public Member member;

        @NonNull
        @Override
        public Member getMember() {
            return member;
        }

        @Nullable
        @Override
        public Object getThisObject() {
            return thisObjectCompat;
        }

        @NonNull
        @Override
        public Object[] getArgs() {
            return argsCompat;
        }

        @Nullable
        @Override
        public Object getResult() {
            return result;
        }

        @Override
        public void setResult(@Nullable Object result) {
            this.result = result;
            this.skipOriginal = true;
        }

        @Nullable
        @Override
        public Throwable getThrowable() {
            return throwable;
        }

        @Override
        public void setThrowable(@NonNull Throwable throwable) {
            this.throwable = throwable;
            this.skipOriginal = true;
        }

        @Nullable
        @Override
        public Object getExtra() {
            if (extras == null || index < 0 || index >= extras.length) {
                return null;
            }
            return extras[index];
        }

        @Override
        public void setExtra(@Nullable Object extra) {
            if (callbacks == null || index < 0) {
                return;
            }
            if (extras == null) {
                extras = new Object[callbacks.length];
            }
            extras[index] = extra;
        }

    }

    // WARNING: This will only work for Android 7.0 and above.
    // Since SDK 24, Method.equals() and Method.hashCode() can correctly compare hooked methods.
    // Before SDK 24, equals() uses AbstractMethod which is not safe for hooked methods.
    // If you need to support lower versions, go and read cs.android.com.
    private static final ConcurrentHashMap<Integer, ConcurrentHashMap<Class<?>, ConcurrentHashMap<Member, CallbackListHolder>>> sCallbackRegistry = new ConcurrentHashMap<>();

    static class Lsp101HookDispatchAgent implements Lsp101HookWrapper.Hooker {

        private final int mPriority;
        private XposedInterface.HookHandle mHandle;

        Lsp101HookDispatchAgent(int priority) {
            mPriority = priority;
        }

        void setFrameworkHookHandle(@NonNull XposedInterface.HookHandle hookHandle) {
            if (mHandle != null && mHandle != hookHandle) {
                throw new IllegalStateException("Hook handle already set");
            }
            mHandle = hookHandle;
        }

        @Override
        public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
            Executable executable = chain.getExecutable();
            // lookup callbacks by priority and member
            ConcurrentHashMap<Class<?>, ConcurrentHashMap<Member, CallbackListHolder>> taggedCallbackRegistry = sCallbackRegistry.get(mPriority);
            if (taggedCallbackRegistry == null) {
                // no callbacks for this priority, just proceed
                return chain.proceed();
            }
            ConcurrentHashMap<Member, CallbackListHolder> callbackList = taggedCallbackRegistry.get(executable.getDeclaringClass());
            if (callbackList == null) {
                return chain.proceed();
            }
            CallbackListHolder holder = callbackList.get(executable);
            if (holder == null) {
                return chain.proceed();
            }
            CallbackWrapper[] callbacks = copyCallbacks(holder);
            if (callbacks.length == 0) {
                return chain.proceed();
            }

            InvocationParamWrapper param = new InvocationParamWrapper();
            Object[] argsCompat = chain.getArgs().toArray();
            param.callbacks = callbacks;

            Object result = null;
            Throwable throwable = null;

            // before callbacks
            for (int i = 0; i < callbacks.length; i++) {
                param.index = i;
                try {
                    callbacks[i].callback.beforeHookedMember(param);
                } catch (Throwable t) {
                    Lsp101HookImpl.INSTANCE.log(t);
                }
            }
            param.index = -1;

            if (!param.skipOriginal) {
                // synchronize args to chain
                try {
                    result = chain.proceed(argsCompat);
                } catch (Throwable t) {
                    throwable = t;
                }
            } else {
                result = param.result;
                throwable = param.throwable;
            }

            // after callbacks in reverse order
            param.isAfter = true;
            param.result = result;
            param.throwable = throwable;
            for (int i = callbacks.length - 1; i >= 0; i--) {
                param.index = i;
                try {
                    callbacks[i].callback.afterHookedMember(param);
                } catch (Throwable t) {
                    Lsp101HookImpl.INSTANCE.log(t);
                }
            }

            result = param.result;
            throwable = param.throwable;

            param.callbacks = null;
            param.extras = null;

            if (throwable != null) {
                throw throwable;
            }
            return result;
        }
    }

    public static class UnhookHandle implements IHookBridge.MemberUnhookHandle {

        private final CallbackWrapper callback;
        private final Member method;

        public UnhookHandle(@NonNull CallbackWrapper callback, @NonNull Member method) {
            this.callback = callback;
            this.method = method;
        }

        @NonNull
        @Override
        public Member getMember() {
            return method;
        }

        @NonNull
        @Override
        public IHookBridge.IMemberHookCallback getCallback() {
            return callback.callback;
        }

        @Override
        public boolean isHookActive() {
            return isMethodCallbackRegistered(method, callback);
        }

        @Override
        public void unhook() {
            removeMethodCallback(method, callback);
        }

    }

    public static int getHookCounter() {
        return (int) (sNextHookId.get() - 1);
    }

    public static Set<Member> getHookedMethodsRaw() {
        return sHookedMethods;
    }

}
