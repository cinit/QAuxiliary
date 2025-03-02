package io.github.qauxv.loader.hookapi;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Set;

@Keep
public interface IHookBridge {

    /**
     * The default hook priority.
     */
    int PRIORITY_DEFAULT = 50;

    /**
     * Execute the hook callback late.
     */
    int PRIORITY_LOWEST = -10000;

    /**
     * Execute the hook callback early.
     */
    int PRIORITY_HIGHEST = 10000;

    interface IMemberHookCallback {

        void beforeHookedMember(@NonNull IMemberHookParam param) throws Throwable;

        void afterHookedMember(@NonNull IMemberHookParam param) throws Throwable;

    }

    interface IMemberHookParam {

        /**
         * Gets the method or constructor being hooked.
         *
         * @return The method or constructor
         */
        @NonNull
        Member getMember();

        /**
         * Gets the `this` reference for an instance method, or null for a static method or constructor.
         *
         * @return The `this` reference
         */
        @Nullable
        Object getThisObject();

        /**
         * Gets the arguments passed to the method or constructor. You may modify the arguments.
         *
         * @return The arguments
         */
        @NonNull
        Object[] getArgs();

        /**
         * Gets the return value of the method or constructor.
         *
         * @return The return value
         */
        @Nullable
        Object getResult();

        /**
         * Sets the return value of the method or constructor. If called in beforeHookedMember, the original method or constructor will not be called.
         *
         * @param result The new return value
         */
        void setResult(@Nullable Object result);

        /**
         * Gets the throwable thrown by the method or constructor, or null if it didn't throw anything.
         *
         * @return The throwable
         */
        @Nullable
        Throwable getThrowable();

        /**
         * Sets the throwable to be thrown by the method or constructor. If called in beforeHookedMember, the original method or constructor will not be
         * called.
         *
         * @param throwable The throwable to throw
         */
        void setThrowable(@NonNull Throwable throwable);

        /**
         * Get the extra data for the current IMemberHookParam. The IMemberHookParam lifecycle is the same as the hooked member invocation. That is one
         * IMemberHookParam instance per hooked member invocation. Any data can be stored here.
         *
         * @return The extra data
         */
        @Nullable
        Object getExtra();

        /**
         * Set the extra data for the current IMemberHookParam.
         *
         * @param extra The extra data
         */
        void setExtra(@Nullable Object extra);

    }

    interface MemberUnhookHandle {

        /**
         * Gets the method or constructor being hooked.
         *
         * @return The method or constructor
         */
        @NonNull
        Member getMember();

        /**
         * Gets the callback for the member.
         *
         * @return The callback
         */
        @NonNull
        IMemberHookCallback getCallback();

        /**
         * Checks if the hook for the member is still active.
         *
         * @return True if the hook is still active, false otherwise
         */
        boolean isHookActive();

        /**
         * Removes the hook for the member.
         */
        void unhook();

    }

    /**
     * Gets the API level of the current implementation. eg, 51-100
     *
     * @return API level
     */
    int getApiLevel();

    /**
     * Gets the Xposed framework name of current implementation.
     *
     * @return Framework name
     */
    @NonNull
    String getFrameworkName();

    /**
     * Gets the Xposed framework version of current implementation.
     *
     * @return Framework version
     */
    @NonNull
    String getFrameworkVersion();

    /**
     * Gets the Xposed framework version code of current implementation.
     *
     * @return Framework version code
     */
    long getFrameworkVersionCode();

    /**
     * Hook a method or constructor. A member can be hooked multiple times with different callbacks. If hook fails, it will throw an exception.
     *
     * @param member   The method or constructor to hook
     * @param callback The callback to be invoked
     * @param priority The priority of the callback
     * @return A handle that can be used to unhook the method or constructor
     * @throws IllegalArgumentException if origin is abstract, framework internal or {@link Method#invoke} or hooker is invalid
     * @throws RuntimeException         if something goes wrong
     */
    @NonNull
    MemberUnhookHandle hookMethod(@NonNull Member member, @NonNull IMemberHookCallback callback, int priority);

    /**
     * Check if the current implementation supports optimization.
     *
     * @return true if deoptimization is supported, false otherwise
     */
    boolean isDeoptimizationSupported();

    /**
     * Deoptimize the specified method or constructor.
     * <p>
     * Deoptimization is an optional feature that only a few implementations support. It is used to undo the effects of optimization, which can be useful for
     * hooking an inlined method or constructor.
     * <p>
     * If the current implementation does not support deoptimization, this method should return false without doing anything.
     *
     * @param member The method or constructor to deoptimize
     * @return true if the method or constructor was deoptimized or if it was already deoptimized, false otherwise
     */
    boolean deoptimize(@NonNull Member member);

    /**
     * Basically the same as {@link Method#invoke}, but calls the original method as it was before the interception by Xposed. Also, access permissions are not
     * checked.
     *
     * <p class="caution">There are very few cases where this method is needed. A common mistake is
     * to replace a method and then invoke the original one based on dynamic conditions. This creates overhead and skips further hooks by other modules.
     * Instead, just hook (don't replace) the method and call {@code param.setResult(null)} in XC_MethodHook.beforeHookedMethod if the original method should be
     * skipped.
     *
     * @param method     The method to be called.
     * @param thisObject For non-static calls, the "this" pointer, otherwise {@code null}.
     * @param args       Arguments for the method call as Object[] array.
     * @return The result returned from the invoked method.
     * @throws NullPointerException      if {@code receiver == null} for a non-static method
     * @throws IllegalAccessException    if this method is not accessible (see {@link AccessibleObject})
     * @throws IllegalArgumentException  if the number of arguments doesn't match the number of parameters, the receiver is incompatible with the declaring
     *                                   class, or an argument could not be unboxed or converted by a widening conversion to the corresponding parameter type
     * @throws InvocationTargetException if an exception was thrown by the invoked method
     */
    @Nullable
    Object invokeOriginalMethod(@NonNull Method method, @Nullable Object thisObject, @NonNull Object[] args)
            throws NullPointerException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    <T> void invokeOriginalConstructor(@NonNull Constructor<T> ctor, @NonNull T thisObject, @NonNull Object[] args)
            throws NullPointerException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    /**
     * Basically the same as {@link Constructor#newInstance(Object...)}, but calls the original constructor as it was before the interception by Xposed.
     *
     * @param <T>         The type of the constructor
     * @param constructor The constructor to create and initialize a new instance
     * @param args        The arguments used for the construction
     * @return The instance created and initialized by the constructor
     * @see Constructor#newInstance(Object...)
     */
    @NonNull
    <T> T newInstanceOrigin(@NonNull Constructor<T> constructor, @NonNull Object... args)
            throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, InstantiationException;

    /**
     * Get the hook counter.
     *
     * @return The hook counter, or -1 if not supported
     */
    long getHookCounter();

    /**
     * Get the hooked methods. Caller should not modify the returned set.
     * <p>
     * Hooked methods are methods that have been hooked by this module. This method is useful for debugging and logging purposes.
     *
     * @return The hooked methods
     */
    Set<Member> getHookedMethods();

}
