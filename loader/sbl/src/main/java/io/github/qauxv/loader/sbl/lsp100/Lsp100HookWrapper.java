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

package io.github.qauxv.loader.sbl.lsp100;

import android.os.ParcelUuid;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;
import io.github.qauxv.loader.hookapi.IHookBridge;
import io.github.qauxv.loader.sbl.common.CheckUtils;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Lsp100HookWrapper {

    private Lsp100HookWrapper() {
    }

    public static XposedModule self = null;

    private static final CallbackWrapper[] EMPTY_CALLBACKS = new CallbackWrapper[0];

    private static final AtomicLong sNextHookId = new AtomicLong(1);

    private static final Object sRegistryWriteLock = new Object();

    private static final int LSP100_PRIORITY = XposedInterface.PRIORITY_LOWEST + 1;

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

    public static UnhookHandle hookAndRegisterMethodCallback(@NonNull Member method, @NonNull IHookBridge.IMemberHookCallback callback, int priority) {
        CheckUtils.checkNonNull(method, "method");
        CheckUtils.checkNonNull(callback, "callback");
        CallbackWrapper wrapper = new CallbackWrapper(callback, priority);
        UnhookHandle handle = new UnhookHandle(wrapper, method);
        Class<?> declaringClass = method.getDeclaringClass();
        CallbackListHolder holder;
        synchronized (sRegistryWriteLock) {
            // 1. check if the method is already hooked
            ConcurrentHashMap<Member, CallbackListHolder> callbackList = sCallbackRegistry.get(declaringClass);
            if (callbackList == null) {
                callbackList = new ConcurrentHashMap<>();
                sCallbackRegistry.put(declaringClass, callbackList);
            }
            holder = callbackList.get(method);
            if (holder == null) {
                // 2. tell the underlying framework to hook the method
                if (method instanceof Method) {
                    self.hook((Method) method, LSP100_PRIORITY, Lsp100HookAgent.class);
                } else if (method instanceof Constructor) {
                    self.hook((Constructor<?>) method, LSP100_PRIORITY, Lsp100HookAgent.class);
                } else {
                    throw new IllegalArgumentException("only method and constructor can be hooked, but got " + method);
                }
                // 3. create a new holder
                CallbackListHolder newHolder = new CallbackListHolder();
                callbackList.put(method, newHolder);
                holder = newHolder;
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

    public static void removeMethodCallback(@NonNull Member method, @NonNull CallbackWrapper callback) {
        CheckUtils.checkNonNull(method, "method");
        CheckUtils.checkNonNull(callback, "callback");
        // find the callback holder
        ConcurrentHashMap<Member, CallbackListHolder> callbackList = sCallbackRegistry.get(method.getDeclaringClass());
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
        // find the callback holder
        ConcurrentHashMap<Member, CallbackListHolder> callbackList = sCallbackRegistry.get(method.getDeclaringClass());
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

        // the index of the active callback
        public int index = -1;
        public boolean isAfter = false;
        XposedInterface.BeforeHookCallback before;
        XposedInterface.AfterHookCallback after;
        // sorted by priority, descending
        public CallbackWrapper[] callbacks;
        // create on demand
        public Object[] extras;

        @NonNull
        @Override
        public Member getMember() {
            if (isAfter) {
                return after.getMember();
            } else {
                return before.getMember();
            }
        }

        @Nullable
        @Override
        public Object getThisObject() {
            if (isAfter) {
                return after.getThisObject();
            } else {
                return before.getThisObject();
            }
        }

        @NonNull
        @Override
        public Object[] getArgs() {
            if (isAfter) {
                return after.getArgs();
            } else {
                return before.getArgs();
            }
        }

        @Nullable
        @Override
        public Object getResult() {
            if (isAfter) {
                return after.getResult();
            } else {
                return null;
            }
        }

        @Override
        public void setResult(@Nullable Object result) {
            if (isAfter) {
                after.setResult(result);
            } else {
                before.returnAndSkip(result);
            }
        }

        @Nullable
        @Override
        public Throwable getThrowable() {
            if (isAfter) {
                return after.getThrowable();
            } else {
                return null;
            }
        }

        @Override
        public void setThrowable(@NonNull Throwable throwable) {
            if (isAfter) {
                after.setThrowable(throwable);
            } else {
                before.throwAndSkip(throwable);
            }
        }

        @Nullable
        @Override
        public Object getExtra() {
            if (extras == null) {
                return null;
            }
            return extras[index];
        }

        @Override
        public void setExtra(@Nullable Object extra) {
            if (extras == null) {
                // create on demand
                extras = new Object[callbacks.length];
            }
            extras[index] = extra;
        }
    }

    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<Member, CallbackListHolder>> sCallbackRegistry = new ConcurrentHashMap<>();

    @XposedHooker
    public static class Lsp100HookAgent implements XposedInterface.Hooker {

        @BeforeInvocation
        public static InvocationParamWrapper beforeInvocation(@NonNull XposedInterface.BeforeHookCallback callback) {
            Member member = callback.getMember();
            // lookup callback list
            ConcurrentHashMap<Member, CallbackListHolder> callbackList = sCallbackRegistry.get(member.getDeclaringClass());
            if (callbackList == null) {
                return null;
            }
            CallbackListHolder holder = callbackList.get(member);
            if (holder == null) {
                return null;
            }
            // copy callbacks
            CallbackWrapper[] callbacks = copyCallbacks(holder);
            if (callbacks.length == 0) {
                return null;
            }
            // create invocation holder
            InvocationParamWrapper param = new InvocationParamWrapper();
            param.callbacks = callbacks;
            param.before = callback;
            param.isAfter = false;
            for (int i = 0; i < callbacks.length; i++) {
                param.index = i;
                try {
                    callbacks[i].callback.beforeHookedMember(param);
                } catch (Throwable t) {
                    self.log(t.toString(), t);
                }
            }
            param.index = -1;
            return param;
        }

        @AfterInvocation
        public static void afterInvocation(
                @NonNull XposedInterface.AfterHookCallback callback,
                @Nullable InvocationParamWrapper param
        ) {
            if (param == null) {
                throw new AssertionError("param is null");
            }
            param.isAfter = true;
            param.after = callback;
            // call in reserve order
            for (int i = param.callbacks.length - 1; i >= 0; i--) {
                param.index = i;
                try {
                    param.callbacks[i].callback.afterHookedMember(param);
                } catch (Throwable t) {
                    self.log(t.toString(), t);
                }
            }
            // for gc
            param.callbacks = null;
            param.extras = null;
            param.before = null;
            param.after = null;
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

}
