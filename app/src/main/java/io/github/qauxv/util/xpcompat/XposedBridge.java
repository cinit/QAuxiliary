package io.github.qauxv.util.xpcompat;

import androidx.annotation.NonNull;
import io.github.qauxv.loader.hookapi.IHookBridge;
import io.github.qauxv.poststartup.StartupInfo;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * The XposedBridge compatibility layer.
 */
public class XposedBridge {

    private XposedBridge() {
    }

    /**
     * Hooks all methods with a certain name that were declared in the specified class. Inherited methods and constructors are not considered. For constructors,
     * use {@link #hookAllConstructors} instead.
     *
     * @param hookClass  The class to check for declared methods.
     * @param methodName The name of the method(s) to hook.
     * @param callback   The callback to be executed when the hooked methods are called.
     * @return A set containing one object for each found method which can be used to unhook it.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> unhooks = new HashSet<XC_MethodHook.Unhook>();
        for (Member method : hookClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                unhooks.add(hookMethod(method, callback));
            }
        }
        return unhooks;
    }

    /**
     * Hook all constructors of the specified class.
     *
     * @param hookClass The class to check for constructors.
     * @param callback  The callback to be executed when the hooked constructors are called.
     * @return A set containing one object for each found constructor which can be used to unhook it.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> hookClass, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> unhooks = new HashSet<XC_MethodHook.Unhook>();
        for (Member constructor : hookClass.getDeclaredConstructors()) {
            unhooks.add(hookMethod(constructor, callback));
        }
        return unhooks;
    }

    private static IHookBridge requireHookBridge() {
        IHookBridge hookBridge = StartupInfo.getHookBridge();
        if (hookBridge == null) {
            throw new IllegalStateException("Hook bridge not available");
        }
        return hookBridge;
    }

    /**
     * Hook any method (or constructor) with the specified callback. See below for some wrappers that make it easier to find a method/constructor in one step.
     *
     * @param hookMethod The method to be hooked.
     * @param callback   The callback to be executed when the hooked method is called.
     * @return An object that can be used to remove the hook.
     * @see XposedHelpers#findAndHookMethod(String, ClassLoader, String, Object...)
     * @see XposedHelpers#findAndHookMethod(Class, String, Object...)
     * @see #hookAllMethods
     * @see XposedHelpers#findAndHookConstructor(String, ClassLoader, Object...)
     * @see XposedHelpers#findAndHookConstructor(Class, Object...)
     * @see #hookAllConstructors
     */
    public static XC_MethodHook.Unhook hookMethod(Member hookMethod, XC_MethodHook callback) {
        if (hookMethod == null) {
            throw new IllegalArgumentException("hookMethod must not be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        int priority = callback.getPriority();
        IHookBridge hookBridge = requireHookBridge();
        IHookBridge.IMemberHookCallback wrappedCallback = new WrappedCallbacks.WrappedHookCallback(callback);
        IHookBridge.MemberUnhookHandle unhookHandle = hookBridge.hookMethod(hookMethod, wrappedCallback, priority);
        return new XC_MethodHook.Unhook(unhookHandle, callback);
    }

    /**
     * Basically the same as {@link Method#invoke}, but calls the original method as it was before the interception by Xposed. Also, access permissions are not
     * checked.
     *
     * <p class="caution">There are very few cases where this method is needed. A common mistake is
     * to replace a method and then invoke the original one based on dynamic conditions. This creates overhead and skips further hooks by other modules.
     * Instead, just hook (don't replace) the method and call {@code param.setResult(null)} in {@link XC_MethodHook#beforeHookedMethod} if the original method
     * should be skipped.
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
    public static Object invokeOriginalMethod(Member method, Object thisObject, Object[] args)
            throws NullPointerException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (args == null) {
            args = ArrayUtils.EMPTY_OBJECT_ARRAY;
        }
        if (method == null) {
            throw new IllegalArgumentException("method must not be null");
        }
        IHookBridge hookBridge = requireHookBridge();
        if (method instanceof Method) {
            if (!Modifier.isStatic(method.getModifiers()) && thisObject == null) {
                throw new IllegalArgumentException("receiver == null for a non-static method");
            }
            return hookBridge.invokeOriginalMethod((Method) method, thisObject, args);
        } else if (method instanceof Constructor) {
            if (thisObject == null) {
                throw new NullPointerException("receiver == null for a constructor");
            }
            hookBridge.invokeOriginalConstructor((Constructor) method, thisObject, args);
            return null;
        } else {
            throw new IllegalArgumentException("method must be a method or constructor");
        }
    }

    /**
     * Deoptimize the specified member.
     * <p>
     * Note: Not all Xposed frameworks support this feature. If the framework does not support it, this method will return {@code false}.
     *
     * @param member a method or constructor
     * @return {@code true} if the member was deoptimized, or already was deoptimized, {@code false} otherwise
     */
    public static boolean deoptimizeMethod(@NonNull Member member) {
        if (!(member instanceof Method) && !(member instanceof Constructor)) {
            throw new IllegalArgumentException("member must be method or constructor");
        }
        return requireHookBridge().deoptimize(member);
    }

    public static void log(String message) {
        StartupInfo.getLoaderService().log(message);
    }

    public static void log(Throwable tr) {
        StartupInfo.getLoaderService().log(tr);
    }

}
