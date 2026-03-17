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
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

/**
 * Xposed interface for modules to operate on application processes.
 */
@SuppressWarnings("unused")
public interface XposedInterface {

    /* API 101 */

    /**
     * The API version of this <b>library</b>. This is a static value for the framework.
     * Modules should use {@link #getApiVersion()} to check the API version at runtime.
     */
    @XposedApiMin(101)
    int LIB_API = 101;

    /**
     * The framework has the capability to hook system_server and other system processes.
     */
    @XposedApiMin(101)
    long PROP_CAP_SYSTEM = 1L;
    /**
     * The framework provides remote preferences and remote files support.
     */
    @XposedApiMin(101)
    long PROP_CAP_REMOTE = 1L << 1;
    /**
     * The framework disallows accessing Xposed API via reflection or dynamically loaded code.
     */
    @XposedApiMin(101)
    long PROP_RT_API_PROTECTION = 1L << 2;

    /**
     * The default hook priority.
     */
    int PRIORITY_DEFAULT = 50;
    /**
     * Execute at the end of the interception chain.
     */
    int PRIORITY_LOWEST = Integer.MIN_VALUE;
    /**
     * Execute at the beginning of the interception chain.
     */
    int PRIORITY_HIGHEST = Integer.MAX_VALUE;

    /* For API 100 */

    /**
     * SDK API version.
     */
    @XposedApiExact(100)
    int API = 100;

    /**
     * Indicates that the framework is running as root.
     */
    @XposedApiExact(100)
    int FRAMEWORK_PRIVILEGE_ROOT = 0;
    /**
     * Indicates that the framework is running in a container with a fake system_server.
     */
    @XposedApiExact(100)
    int FRAMEWORK_PRIVILEGE_CONTAINER = 1;
    /**
     * Indicates that the framework is running as a different app, which may have at most shell permission.
     */
    @XposedApiExact(100)
    int FRAMEWORK_PRIVILEGE_APP = 2;
    /**
     * Indicates that the framework is embedded in the hooked app,
     * which means {@link #getRemotePreferences} will be null and remote file is unsupported.
     */
    @XposedApiExact(100)
    int FRAMEWORK_PRIVILEGE_EMBEDDED = 3;

    /**
     * Contextual interface for before invocation callbacks.
     */
    @XposedApiExact(100)
    interface BeforeHookCallback {
        /**
         * Gets the method / constructor to be hooked.
         */
        @NonNull
        Member getMember();

        /**
         * Gets the {@code this} object, or {@code null} if the method is static.
         */
        @Nullable
        Object getThisObject();

        /**
         * Gets the arguments passed to the method / constructor. You can modify the arguments.
         */
        @NonNull
        Object[] getArgs();

        /**
         * Sets the return value of the method and skip the invocation. If the procedure is a constructor,
         * the {@code result} param will be ignored.
         * Note that the after invocation callback will still be called.
         *
         * @param result The return value
         */
        void returnAndSkip(@Nullable Object result);

        /**
         * Throw an exception from the method / constructor and skip the invocation.
         * Note that the after invocation callback will still be called.
         *
         * @param throwable The exception to be thrown
         */
        void throwAndSkip(@Nullable Throwable throwable);
    }

    /**
     * Contextual interface for after invocation callbacks.
     */
    @XposedApiExact(100)
    interface AfterHookCallback {
        /**
         * Gets the method / constructor to be hooked.
         */
        @NonNull
        Member getMember();

        /**
         * Gets the {@code this} object, or {@code null} if the method is static.
         */
        @Nullable
        Object getThisObject();

        /**
         * Gets all arguments passed to the method / constructor.
         */
        @NonNull
        Object[] getArgs();

        /**
         * Gets the return value of the method or the before invocation callback. If the procedure is a
         * constructor, a void method or an exception was thrown, the return value will be {@code null}.
         */
        @Nullable
        Object getResult();

        /**
         * Gets the exception thrown by the method / constructor or the before invocation callback. If the
         * procedure call was successful, the return value will be {@code null}.
         */
        @Nullable
        Throwable getThrowable();

        /**
         * Gets whether the invocation was skipped by the before invocation callback.
         */
        boolean isSkipped();

        /**
         * Sets the return value of the method and skip the invocation. If the procedure is a constructor,
         * the {@code result} param will be ignored.
         *
         * @param result The return value
         */
        void setResult(@Nullable Object result);

        /**
         * Sets the exception thrown by the method / constructor.
         *
         * @param throwable The exception to be thrown.
         */
        void setThrowable(@Nullable Throwable throwable);
    }

    /**
     * Interface for method / constructor hooking. Xposed modules should define their own hooker class
     * and implement this interface. Normally, a hooker class corresponds to a method / constructor, but
     * there could also be a single hooker class for all of them. By this way you can implement an interface
     * like the old API.
     *
     * <p>
     * Classes implementing this interface should be annotated with {@link XposedHooker} and should provide
     * two public static methods that are annotated with {@link BeforeInvocation} and {@link AfterInvocation},
     * respectively.
     * </p>
     *
     * <p>
     * The before invocation method should have the following signature:<br/>
     * Param {@code callback}: The {@link BeforeHookCallback} of the procedure call.<br/>
     * Return value: If you want to save contextual information of one procedure call between the before
     * and after callback, it could be a self-defined class, otherwise it should be {@code void}.
     * </p>
     *
     * <p>
     * The after invocation method should have the following signature:<br/>
     * Param {@code callback}: The {@link AfterHookCallback} of the procedure call.<br/>
     * Param {@code context} (optional): The contextual object returned by the before invocation.
     * </p>
     *
     * <p>Example usage:</p>
     *
     * <pre>{@code
     *   @XposedHooker
     *   public class ExampleHooker implements Hooker {
     *
     *       @BeforeInvocation
     *       public static void before(@NonNull BeforeHookCallback callback) {
     *           // Pre-hooking logic goes here
     *       }
     *
     *       @AfterInvocation
     *       public static void after(@NonNull AfterHookCallback callback) {
     *           // Post-hooking logic goes here
     *       }
     *   }
     *
     *   @XposedHooker
     *   public class ExampleHookerWithContext implements Hooker {
     *
     *       @BeforeInvocation
     *       public static MyContext before(@NonNull BeforeHookCallback callback) {
     *           // Pre-hooking logic goes here
     *           return new MyContext();
     *       }
     *
     *       @AfterInvocation
     *       public static void after(@NonNull AfterHookCallback callback, MyContext context) {
     *           // Post-hooking logic goes here
     *       }
     *   }
     * }</pre>
     */
    interface Hooker {
        // for API 100, the hooker interface is just a marker interface.
        // The actual hook logic is implemented in the static methods annotated with @BeforeInvocation and @AfterInvocation.
        // for API 101, the hooker interface is defined by modules and the actual hook logic is implemented in the intercept method.
    }

    /**
     * Interface for canceling a hook.
     *
     * @param <T> {@link Method} or {@link Constructor}
     */
    @XposedApiExact(100)
    interface MethodUnhooker<T> {
        /**
         * Gets the method or constructor being hooked.
         */
        @NonNull
        T getOrigin();

        /**
         * Cancels the hook. The behavior of calling this method multiple times is undefined.
         */
        void unhook();
    }

    /* API 101 callbacks aka. invokers */

    /**
     * Invoker for a method or constructor.
     */
    @XposedApiMin(101)
    interface Invoker<T extends Invoker<T, U>, U extends Executable> {
        /**
         * Type of the invoker, which determines the hook chain to be invoked
         */
        interface Type {
            /**
             * A convenience constant for {@link Origin}.
             */
            Origin ORIGIN = new Origin();

            /**
             * Invokes the original executable, skipping all hooks.
             */
            final class Origin implements Type {
                Origin() { }
            }

            /**
             * Invokes the executable starting from the middle of the hook chain, skipping all
             * hooks with priority higher than the given value.
             */
            final class Chain implements Type {
                /**
                 * Invoking the executable with full hook chain.
                 */
                public static final Chain FULL = new Chain(PRIORITY_HIGHEST);

                private final int maxPriority;

                /**
                 * @param maxPriority The maximum priority of hooks to include in the chain
                 */
                public Chain(int maxPriority) {
                    this.maxPriority = maxPriority;
                }

                public int maxPriority() {
                    return maxPriority;
                }
            }
        }

        /**
         * Sets the type of the invoker, which determines the hook chain to be invoked
         */
        T setType(@NonNull Type type);

        /**
         * Invokes the method (or the constructor as a method) through the hook chain determined by
         * the invoker's type.
         *
         * @param thisObject For non-static calls, the {@code this} pointer, otherwise {@code null}
         * @param args       The arguments used for the method call
         * @return The result returned from the invoked method
         * <p>For void methods and constructors, always returns {@code null}.</p>
         * @see Method#invoke(Object, Object...)
         */
        Object invoke(Object thisObject, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException;

        /**
         * Invokes the special (non-virtual) method (or the constructor as a method) on a given object
         * instance, similar to the functionality of {@code CallNonVirtual<type>Method} in JNI, which invokes
         * an instance (nonstatic) method on a Java object. This method is useful when you need to call
         * a specific method on an object, bypassing any overridden methods in subclasses and
         * directly invoking the method defined in the specified class.
         *
         * <p>This method is useful when you need to call {@code super.xxx()} in a hooked constructor.</p>
         *
         * @param thisObject The {@code this} pointer
         * @param args       The arguments used for the method call
         * @return The result returned from the invoked method
         * <p>For void methods and constructors, always returns {@code null}.</p>
         * @see Method#invoke(Object, Object...)
         */
        Object invokeSpecial(@NonNull Object thisObject, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException;
    }

    /**
     * Invoker for a constructor.
     *
     * @param <T> The type of the constructor
     */
    @XposedApiMin(101)
    interface CtorInvoker<T> extends Invoker<CtorInvoker<T>, Constructor<T>> {
        /**
         * Creates a new instance through the hook chain determined by the invoker's type.
         *
         * @param args The arguments used for the construction
         * @return The instance created and initialized by the constructor
         * @see Constructor#newInstance(Object...)
         */
        @NonNull
        T newInstance(Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, InstantiationException;

        /**
         * Creates a new instance of the given subclass, but initializes it with a parent constructor. This could
         * leave the object in an invalid state, where the subclass constructor is not called and the fields
         * of the subclass are not initialized.
         *
         * <p>This method is useful when you need to initialize some fields in the subclass by yourself.</p>
         *
         * @param <U>      The type of the subclass
         * @param subClass The subclass to create a new instance
         * @param args     The arguments used for the construction
         * @return The instance of subclass initialized by the constructor
         * @see Constructor#newInstance(Object...)
         */
        @NonNull
        <U> U newInstanceSpecial(@NonNull Class<U> subClass, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, InstantiationException;
    }

    /**
     * Interceptor chain for a method or constructor.
     */
    @XposedApiMin(101)
    interface Chain {
        /**
         * Gets the method / constructor being hooked.
         */
        @RequiresApi(26)
        @NonNull
        Executable getExecutable();

        /**
         * Gets the {@code this} pointer for the call, or {@code null} for static methods.
         */
        Object getThisObject();

        /**
         * Gets the arguments. The returned list is immutable. If you want to change the arguments, you
         * should call {@code proceed(Object...)} or {@code proceedWith(Object, Object...)} with the new
         * arguments.
         */
        @NonNull
        List<Object> getArgs();

        /**
         * Gets the argument at the given index.
         *
         * @param index The argument index
         * @return The argument at the given index
         * @throws IndexOutOfBoundsException if index is out of bounds
         * @throws ClassCastException        if the argument cannot be cast to the expected type
         */
        Object getArg(int index) throws IndexOutOfBoundsException, ClassCastException;

        /**
         * Proceeds to the next interceptor in the chain with the same arguments and {@code this} pointer.
         *
         * @return The result returned from next interceptor or the original executable if current
         * interceptor is the last one in the chain.
         * <p>For void methods and constructors, always returns {@code null}.</p>
         * @throws Throwable if any interceptor or the original executable throws an exception
         */
        Object proceed() throws Throwable;

        /**
         * Proceeds to the next interceptor in the chain with the given arguments and the same {@code this} pointer.
         *
         * @param args The arguments used for the call
         * @return The result returned from next interceptor or the original executable if current
         * interceptor is the last one in the chain.
         * <p>For void methods and constructors, always returns {@code null}.</p>
         * @throws Throwable if any interceptor or the original executable throws an exception
         */
        Object proceed(@NonNull Object[] args) throws Throwable;

        /**
         * Proceeds to the next interceptor in the chain with the same arguments and given {@code this} pointer.
         * Static method interceptors should not call this.
         *
         * @param thisObject The {@code this} pointer for the call
         * @return The result returned from next interceptor or the original executable if current
         * interceptor is the last one in the chain.
         * <p>For void methods and constructors, always returns {@code null}.</p>
         * @throws Throwable if any interceptor or the original executable throws an exception
         */
        Object proceedWith(@NonNull Object thisObject) throws Throwable;

        /**
         * Proceeds to the next interceptor in the chain with the given arguments and {@code this} pointer.
         * Static method interceptors should not call this.
         *
         * @param thisObject The {@code this} pointer for the call
         * @param args       The arguments used for the call
         * @return The result returned from next interceptor or the original executable if current
         * interceptor is the last one in the chain.
         * <p>For void methods and constructors, always returns {@code null}.</p>
         * @throws Throwable if any interceptor or the original executable throws an exception
         */
        Object proceedWith(@NonNull Object thisObject, @NonNull Object[] args) throws Throwable;
    }

    /*
     * Hooker for a method or constructor.
     *
    interface Hooker {
        /**
         * Intercepts a method / constructor call.
         *
         * @param chain The interceptor chain for the call
         * @return The result to be returned from the interceptor. If the hooker does not want to
         * change the result, it should call {@code chain.proceed()} and return its result.
         * <p>For void methods and constructors, the return value is ignored by the framework.</p>
         * @throws Throwable Throw any exception from the interceptor. The exception will
         *                   propagate to the caller if not caught by any interceptor.
         *
        Object intercept(@NonNull Chain chain) throws Throwable;
    }
    */

    /**
     * Handle for a hook.
     */
    @XposedApiMin(101)
    interface HookHandle {
        /**
         * Gets the method / constructor being hooked.
         */
        @RequiresApi(26)
        @NonNull
        Executable getExecutable();

        /**
         * Cancels the hook. This method is idempotent. It is safe to call this method multiple times.
         */
        void unhook();
    }

    /**
     * Exception handling mode for hookers. This determines how the framework handles exceptions
     * thrown by hookers. The default mode is {@link ExceptionMode#DEFAULT}.
     */
    enum ExceptionMode {
        /**
         * Follows the global exception mode configured in {@code module.prop}. Defaults to {@link #PROTECTIVE}
         * if not specified.
         */
        DEFAULT,

        /**
         * Any exception thrown by the <b>hooker</b> will be caught and logged, and the call will proceed as
         * if no hook exists. This mode is recommended for most cases, as it can prevent crashes caused by
         * hook errors.
         * <p>
         * If the exception is thrown before {@link Chain#proceed()}, the framework will
         * continue the chain without the hook; if the exception is thrown after proceed, the framework
         * will return the value / exception proceeded as the result.
         * </p>
         * <p>Exceptions thrown by proceed will always be propagated.</p>
         */
        PROTECTIVE,

        /**
         * Any exception thrown by the hooker will be propagated to the caller as usual. This mode is
         * recommended for debugging purposes, as it can help you find and fix errors in your hooks.
         */
        PASSTHROUGH,
    }

    /**
     * Builder for configuring a hook.
     */
    @XposedApiMin(101)
    interface HookBuilder {
        /**
         * Sets the priority of the hook. Hooks with higher priority will be called before hooks with lower
         * priority. The default priority is {@link XposedInterface#PRIORITY_DEFAULT}.
         *
         * @param priority The priority of the hook
         * @return The builder itself for chaining
         */
        HookBuilder setPriority(int priority);

        /**
         * Sets the exception handling mode for the hook. The default mode is {@link ExceptionMode#DEFAULT}.
         *
         * @param mode The exception handling mode
         * @return The builder itself for chaining
         */
        HookBuilder setExceptionMode(@NonNull ExceptionMode mode);

        /**
         * Sets the hooker for the method / constructor and builds the hook.
         *
         * @param hooker The hooker object
         * @return The handle for the hook
         * @throws IllegalArgumentException if origin is framework internal or {@link Constructor#newInstance},
         *                                  or hooker is invalid
         * @throws io.github.libxposed.api.error.HookFailedError  if hook fails due to framework internal error
         */
        @NonNull
        HookHandle intercept(@NonNull Hooker hooker);
    }

    /**
     * Gets the runtime Xposed API version. Framework implementations must <b>not</b> override this method.
     */
    default int getApiVersion() {
        return LIB_API;
    }

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
     * Gets the Xposed framework privilege of current implementation.
     *
     * @return Framework privilege
     */
    @XposedApiExact(100)
    int getFrameworkPrivilege();

    /**
     * Gets the Xposed framework properties.
     * Properties with prefix {@code PROP_RT_} may change among launches.
     */
    @XposedApiMin(101)
    long getFrameworkProperties();

    /**
     * Hook a method with default priority.
     *
     * @param origin The method to be hooked
     * @param hooker The hooker class
     * @return Unhooker for canceling the hook
     * @throws IllegalArgumentException if origin is abstract, framework internal or {@link Method#invoke},
     *                                  or hooker is invalid
     * @throws io.github.libxposed.api.errors.HookFailedError  if hook fails due to framework internal error
     */
    @XposedApiExact(100)
    @NonNull
    MethodUnhooker<Method> hook(@NonNull Method origin, @NonNull Class<? extends Hooker> hooker);

    /**
     * Hook a method with specified priority.
     *
     * @param origin   The method to be hooked
     * @param priority The hook priority
     * @param hooker   The hooker class
     * @return Unhooker for canceling the hook
     * @throws IllegalArgumentException if origin is abstract, framework internal or {@link Method#invoke},
     *                                  or hooker is invalid
     * @throws io.github.libxposed.api.errors.HookFailedError  if hook fails due to framework internal error
     */
    @XposedApiExact(100)
    @NonNull
    MethodUnhooker<Method> hook(@NonNull Method origin, int priority, @NonNull Class<? extends Hooker> hooker);

    /**
     * Hook a constructor with default priority.
     *
     * @param <T>    The type of the constructor
     * @param origin The constructor to be hooked
     * @param hooker The hooker class
     * @return Unhooker for canceling the hook
     * @throws IllegalArgumentException if origin is abstract, framework internal or {@link Method#invoke},
     *                                  or hooker is invalid
     * @throws io.github.libxposed.api.errors.HookFailedError  if hook fails due to framework internal error
     */
    @XposedApiExact(100)
    @NonNull
    <T> MethodUnhooker<Constructor<T>> hook(@NonNull Constructor<T> origin, @NonNull Class<? extends Hooker> hooker);

    /**
     * Hook a constructor with specified priority.
     *
     * @param <T>      The type of the constructor
     * @param origin   The constructor to be hooked
     * @param priority The hook priority
     * @param hooker   The hooker class
     * @return Unhooker for canceling the hook
     * @throws IllegalArgumentException if origin is abstract, framework internal or {@link Method#invoke},
     *                                  or hooker is invalid
     * @throws io.github.libxposed.api.errors.HookFailedError  if hook fails due to framework internal error
     */
    @XposedApiExact(100)
    @NonNull
    <T> MethodUnhooker<Constructor<T>> hook(@NonNull Constructor<T> origin, int priority, @NonNull Class<? extends Hooker> hooker);

    /**
     * Hook the static initializer of a class with default priority.
     * <p>
     * Note: If the class is initialized, the hook will never be called.
     * </p>
     *
     * @param origin The class to be hooked
     * @param hooker The hooker class
     * @return Unhooker for canceling the hook
     * @throws IllegalArgumentException if class has no static initializer or hooker is invalid
     * @throws io.github.libxposed.api.errors.HookFailedError  if hook fails due to framework internal error
     */
    @XposedApiExact(100)
    @NonNull
    <T> MethodUnhooker<Constructor<T>> hookClassInitializer(@NonNull Class<T> origin, @NonNull Class<? extends Hooker> hooker);

    /**
     * Hook the static initializer of a class with specified priority.
     * <p>
     * Note: If the class is initialized, the hook will never be called.
     * </p>
     *
     * @param origin   The class to be hooked
     * @param priority The hook priority
     * @param hooker   The hooker class
     * @return Unhooker for canceling the hook
     * @throws IllegalArgumentException if class has no static initializer or hooker is invalid
     * @throws io.github.libxposed.api.errors.HookFailedError  if hook fails due to framework internal error
     */
    @XposedApiExact(100)
    @NonNull
    <T> MethodUnhooker<Constructor<T>> hookClassInitializer(@NonNull Class<T> origin, int priority, @NonNull Class<? extends Hooker> hooker);

    /**
     * Deoptimizes a method in case hooked callee is not called because of inline.
     *
     * <p>By deoptimizing the method, the method will back all callee without inlining.
     * For example, when a short hooked method B is invoked by method A, the callback to B is not invoked
     * after hooking, which may mean A has inlined B inside its method body. To force A to call the hooked B,
     * you can deoptimize A and then your hook can take effect.</p>
     *
     * <p>Generally, you need to find all the callers of your hooked callee and that can be hardly achieve
     * (but you can still search all callers by using DexParser). Use this method if you are sure
     * the deoptimized callers are all you need. Otherwise, it would be better to change the hook point or
     * to deoptimize the whole app manually (by simply reinstalling the app without uninstall).</p>
     *
     * @param method The method to deoptimize
     * @return Indicate whether the deoptimizing succeed or not
     */
    @XposedApiExact(100)
    boolean deoptimize(@NonNull Method method);

    /**
     * Deoptimizes a constructor in case hooked callee is not called because of inline.
     *
     * @param <T>         The type of the constructor
     * @param constructor The constructor to deoptimize
     * @return Indicate whether the deoptimizing succeed or not
     * @see #deoptimize(Method)
     */
    @XposedApiExact(100)
    <T> boolean deoptimize(@NonNull Constructor<T> constructor);

    /**
     * Basically the same as {@link Method#invoke(Object, Object...)}, but calls the original method
     * as it was before the interception by Xposed.
     *
     * @param method     The method to be called
     * @param thisObject For non-static calls, the {@code this} pointer, otherwise {@code null}
     * @param args       The arguments used for the method call
     * @return The result returned from the invoked method
     * @see Method#invoke(Object, Object...)
     */
    @XposedApiExact(100)
    @Nullable
    Object invokeOrigin(@NonNull Method method, @Nullable Object thisObject, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException;

    /**
     * Basically the same as {@link Constructor#newInstance(Object...)}, but calls the original constructor
     * as it was before the interception by Xposed.
     * @param constructor The constructor to create and initialize a new instance
     * @param thisObject  The instance to be constructed
     * @param args        The arguments used for the construction
     * @param <T>         The type of the instance
     * @see Constructor#newInstance(Object...)
     */
    @XposedApiExact(100)
    <T> void invokeOrigin(@NonNull Constructor<T> constructor, @NonNull T thisObject, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException;

    /**
     * Invokes a special (non-virtual) method on a given object instance, similar to the functionality of
     * {@code CallNonVirtual<type>Method} in JNI, which invokes an instance (nonstatic) method on a Java
     * object. This method is useful when you need to call a specific method on an object, bypassing any
     * overridden methods in subclasses and directly invoking the method defined in the specified class.
     *
     * <p>This method is useful when you need to call {@code super.xxx()} in a hooked constructor.</p>
     *
     * @param method     The method to be called
     * @param thisObject For non-static calls, the {@code this} pointer, otherwise {@code null}
     * @param args       The arguments used for the method call
     * @return The result returned from the invoked method
     * @see Method#invoke(Object, Object...)
     */
    @Nullable
    @XposedApiExact(100)
    Object invokeSpecial(@NonNull Method method, @NonNull Object thisObject, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException;

    /**
     * Invokes a special (non-virtual) method on a given object instance, similar to the functionality of
     * {@code CallNonVirtual<type>Method} in JNI, which invokes an instance (nonstatic) method on a Java
     * object. This method is useful when you need to call a specific method on an object, bypassing any
     * overridden methods in subclasses and directly invoking the method defined in the specified class.
     *
     * <p>This method is useful when you need to call {@code super.xxx()} in a hooked constructor.</p>
     *
     * @param constructor The constructor to create and initialize a new instance
     * @param thisObject  The instance to be constructed
     * @param args        The arguments used for the construction
     * @see Constructor#newInstance(Object...)
     */
    @XposedApiExact(100)
    <T> void invokeSpecial(@NonNull Constructor<T> constructor, @NonNull T thisObject, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException;

    /**
     * Basically the same as {@link Constructor#newInstance(Object...)}, but calls the original constructor
     * as it was before the interception by Xposed.
     *
     * @param <T>         The type of the constructor
     * @param constructor The constructor to create and initialize a new instance
     * @param args        The arguments used for the construction
     * @return The instance created and initialized by the constructor
     * @see Constructor#newInstance(Object...)
     */
    @NonNull
    @XposedApiExact(100)
    <T> T newInstanceOrigin(@NonNull Constructor<T> constructor, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, InstantiationException;

    /**
     * Creates a new instance of the given subclass, but initialize it with a parent constructor. This could
     * leave the object in an invalid state, where the subclass constructor are not called and the fields
     * of the subclass are not initialized.
     *
     * <p>This method is useful when you need to initialize some fields in the subclass by yourself.</p>
     *
     * @param <T>         The type of the parent constructor
     * @param <U>         The type of the subclass
     * @param constructor The parent constructor to initialize a new instance
     * @param subClass    The subclass to create a new instance
     * @param args        The arguments used for the construction
     * @return The instance of subclass initialized by the constructor
     * @see Constructor#newInstance(Object...)
     */
    @NonNull
    @XposedApiExact(100)
    <T, U> U newInstanceSpecial(@NonNull Constructor<T> constructor, @NonNull Class<U> subClass, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, InstantiationException;


    /**
     * Hook a method / constructor.
     *
     * @param origin The executable to be hooked
     * @return The builder for the hook
     */
    @XposedApiMin(101)
    @RequiresApi(26)
    @NonNull
    HookBuilder hook(@NonNull Executable origin);

    /**
     * Hook the static initializer ({@code <clinit>}) of a class.
     *
     * <p>The static initializer is treated as a regular {@code static void()} method with no parameters.
     * Accordingly, in the {@link Chain} passed to the hooker:</p>
     * <ul>
     *     <li>{@link Chain#getExecutable()} returns a synthetic {@link Method} representing
     *     the static initializer.</li>
     *     <li>{@link Chain#getThisObject()} always returns {@code null}.</li>
     *     <li>{@link Chain#getArgs()} returns an empty list.</li>
     *     <li>{@link Chain#proceed()} returns {@code null}.</li>
     * </ul>
     *
     * <p>Note: If the class is already initialized, the hook will never be called.</p>
     *
     * @param origin The class whose static initializer is to be hooked
     * @return The builder for the hook
     */
    @NonNull
    @XposedApiMin(101)
    HookBuilder hookClassInitializer(@NonNull Class<?> origin);

    /**
     * Deoptimizes a method / constructor in case hooked callee is not called because of inline.
     *
     * <p>By deoptimizing the method, the runtime will fall back to calling all callees without inlining.
     * For example, when a short hooked method B is invoked by method A, the callback to B is not invoked
     * after hooking, which may mean A has inlined B inside its method body. To force A to call the hooked B,
     * you can deoptimize A and then your hook can take effect.</p>
     *
     * <p>Generally, you need to find all the callers of your hooked callee, and that can hardly be achieved
     * (but you can still search all callers by using <a href="https://github.com/LuckyPray/DexKit">DexKit</a>).
     * Use this method if you are sure the deoptimized callers are all you need. Otherwise, it would be better to
     * change the hook point or to deoptimize the whole app manually (by simply reinstalling the app without uninstall).</p>
     *
     * @param executable The method / constructor to deoptimize
     * @return Indicate whether the deoptimizing succeed or not
     */
    @RequiresApi(26)
    @XposedApiMin(101)
    boolean deoptimize(@NonNull Executable executable);

    /**
     * Get a method invoker for the given method. The default type of the invoker is
     * {@link Invoker.Type.Chain#FULL}.
     *
     * @param method The method to get the invoker for
     * @return The method invoker
     */
    @XposedApiMin(101)
    @NonNull
    Invoker<?, Method> getInvoker(@NonNull Method method);

    /**
     * Get a constructor invoker for the given constructor. The default type of the invoker is
     * {@link Invoker.Type.Chain#FULL}.
     *
     * @param constructor The constructor to get the invoker for
     * @param <T>         The type of the constructor
     * @return The constructor invoker
     */
    @XposedApiMin(101)
    @NonNull
    <T> CtorInvoker<T> getInvoker(@NonNull Constructor<T> constructor);

    /**
     * Writes a message to the Xposed log.
     *
     * @param message The log message
     */
    @XposedApiExact(100)
    void log(@NonNull String message);

    /**
     * Writes a message with a stack trace to the Xposed log.
     *
     * @param message   The log message
     * @param throwable The Throwable object for the stack trace
     */
    @XposedApiExact(100)
    void log(@NonNull String message, @NonNull Throwable throwable);

    /**
     * Writes a message to the Xposed log.
     *
     * @param priority The log priority, see {@link android.util.Log}
     * @param tag      The log tag
     * @param msg      The log message
     */
    @XposedApiMin(101)
    void log(int priority, @Nullable String tag, @NonNull String msg);

    /**
     * Writes a message to the Xposed log.
     *
     * @param priority The log priority, see {@link android.util.Log}
     * @param tag      The log tag
     * @param msg      The log message
     * @param tr       An exception to log
     */
    @XposedApiMin(101)
    void log(int priority, @Nullable String tag, @NonNull String msg, @Nullable Throwable tr);

    /**
     * Gets the application info of the module.
     */
    @NonNull
    @XposedApiExact(100)
    ApplicationInfo getApplicationInfo();

    /**
     * Gets the application info of the module.
     */
    @NonNull
    @XposedApiMin(101)
    ApplicationInfo getModuleApplicationInfo();

    /**
     * Gets remote preferences stored in Xposed framework. Note that those are read-only in hooked apps.
     *
     * @param group Group name
     * @return The preferences
     * @throws UnsupportedOperationException If the framework is embedded
     */
    @NonNull
    SharedPreferences getRemotePreferences(@NonNull String group);

    /**
     * List all files in the module's shared data directory.
     *
     * @return The file list
     * @throws UnsupportedOperationException If the framework is embedded
     */
    @NonNull
    String[] listRemoteFiles();

    /**
     * Open a file in the module's shared data directory. The file is opened in read-only mode.
     *
     * @param name File name, must not contain path separators and . or ..
     * @return The file descriptor
     * @throws FileNotFoundException         If the file does not exist or the path is forbidden
     * @throws UnsupportedOperationException If the framework is embedded
     */
    @NonNull
    ParcelFileDescriptor openRemoteFile(@NonNull String name) throws FileNotFoundException;
}
