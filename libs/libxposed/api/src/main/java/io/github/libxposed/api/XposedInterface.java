package io.github.libxposed.api;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;
import io.github.libxposed.api.errors.HookFailedError;
import io.github.libxposed.api.utils.DexParser;

/**
 * Xposed interface for modules to operate on application processes.
 */
@SuppressWarnings("unused")
public interface XposedInterface {
    /**
     * SDK API version.
     */
    int API = 100;

    /**
     * Indicates that the framework is running as root.
     */
    int FRAMEWORK_PRIVILEGE_ROOT = 0;
    /**
     * Indicates that the framework is running in a container with a fake system_server.
     */
    int FRAMEWORK_PRIVILEGE_CONTAINER = 1;
    /**
     * Indicates that the framework is running as a different app, which may have at most shell permission.
     */
    int FRAMEWORK_PRIVILEGE_APP = 2;
    /**
     * Indicates that the framework is embedded in the hooked app,
     * which means {@link #getRemotePreferences} will be null and remote file is unsupported.
     */
    int FRAMEWORK_PRIVILEGE_EMBEDDED = 3;

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

    /**
     * Contextual interface for before invocation callbacks.
     */
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
    }

    /**
     * Interface for canceling a hook.
     *
     * @param <T> {@link Method} or {@link Constructor}
     */
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
    int getFrameworkPrivilege();

    /**
     * Hook a method with default priority.
     *
     * @param origin The method to be hooked
     * @param hooker The hooker class
     * @return Unhooker for canceling the hook
     * @throws IllegalArgumentException if origin is abstract, framework internal or {@link Method#invoke},
     *                                  or hooker is invalid
     * @throws HookFailedError          if hook fails due to framework internal error
     */
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
     * @throws HookFailedError          if hook fails due to framework internal error
     */
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
     * @throws HookFailedError          if hook fails due to framework internal error
     */
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
     * @throws HookFailedError          if hook fails due to framework internal error
     */
    @NonNull
    <T> MethodUnhooker<Constructor<T>> hook(@NonNull Constructor<T> origin, int priority, @NonNull Class<? extends Hooker> hooker);

    /**
     * Deoptimizes a method in case hooked callee is not called because of inline.
     *
     * <p>By deoptimizing the method, the method will back all callee without inlining.
     * For example, when a short hooked method B is invoked by method A, the callback to B is not invoked
     * after hooking, which may mean A has inlined B inside its method body. To force A to call the hooked B,
     * you can deoptimize A and then your hook can take effect.</p>
     *
     * <p>Generally, you need to find all the callers of your hooked callee and that can be hardly achieve
     * (but you can still search all callers by using {@link DexParser}). Use this method if you are sure
     * the deoptimized callers are all you need. Otherwise, it would be better to change the hook point or
     * to deoptimize the whole app manually (by simply reinstalling the app without uninstall).</p>
     *
     * @param method The method to deoptimize
     * @return Indicate whether the deoptimizing succeed or not
     */
    boolean deoptimize(@NonNull Method method);

    /**
     * Deoptimizes a constructor in case hooked callee is not called because of inline.
     *
     * @param <T>         The type of the constructor
     * @param constructor The constructor to deoptimize
     * @return Indicate whether the deoptimizing succeed or not
     * @see #deoptimize(Method)
     */
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
    <T, U> U newInstanceSpecial(@NonNull Constructor<T> constructor, @NonNull Class<U> subClass, Object... args) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, InstantiationException;

    /**
     * Writes a message to the Xposed log.
     *
     * @param message The log message
     */
    void log(@NonNull String message);

    /**
     * Writes a message with a stack trace to the Xposed log.
     *
     * @param message   The log message
     * @param throwable The Throwable object for the stack trace
     */
    void log(@NonNull String message, @NonNull Throwable throwable);

    /**
     * Parse a dex file in memory.
     *
     * @param dexData            The content of the dex file
     * @param includeAnnotations Whether to include annotations
     * @return The {@link DexParser} of the dex file
     * @throws IOException if the dex file is invalid
     */
    @Nullable
    DexParser parseDex(@NonNull ByteBuffer dexData, boolean includeAnnotations) throws IOException;

    /**
     * Gets the application info of the module.
     */
    @NonNull
    ApplicationInfo getApplicationInfo();

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
