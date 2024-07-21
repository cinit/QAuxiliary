package io.github.qauxv.util.xpcompat;

import androidx.annotation.NonNull;
import io.github.qauxv.loader.hookapi.IHookBridge;
import io.github.qauxv.poststartup.StartupInfo;
import java.lang.reflect.Member;
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
        IHookBridge.IMemberHookCallback wrappedCallback = new WrappedHookCallback(callback);
        IHookBridge.MemberUnhookHandle unhookHandle = hookBridge.hookMethod(hookMethod, wrappedCallback, priority);
        return new XC_MethodHook.Unhook(unhookHandle, callback);
    }

    private static class WrappedHookParam extends XC_MethodHook.MethodHookParam {

        private WrappedHookParam() {
        }

        private IHookBridge.IMemberHookParam param;

        @Override
        public Object getResult() {
            return param.getResult();
        }

        @Override
        public void setResult(Object result) {
            param.setResult(result);
        }

        @Override
        public Throwable getThrowable() {
            return param.getThrowable();
        }

        @Override
        public boolean hasThrowable() {
            return param.getThrowable() != null;
        }

        @Override
        public void setThrowable(Throwable throwable) {
            param.setThrowable(throwable);
        }

        @Override
        public Object getResultOrThrowable() throws Throwable {
            Throwable throwable = param.getThrowable();
            if (throwable != null) {
                throw throwable;
            }
            return param.getResult();
        }
    }

    private static class WrappedHookCallback implements IHookBridge.IMemberHookCallback {

        private final XC_MethodHook callback;

        public WrappedHookCallback(@NonNull XC_MethodHook callback) {
            this.callback = callback;
        }

        @Override
        public void beforeHookedMember(@NonNull IHookBridge.IMemberHookParam param) throws Throwable {
            WrappedHookParam wrappedParam = new WrappedHookParam();
            wrappedParam.param = param;
            wrappedParam.method = param.getMember();
            wrappedParam.thisObject = param.getThisObject();
            wrappedParam.args = param.getArgs();
            param.setExtra(wrappedParam);
            callback.beforeHookedMethod(wrappedParam);
        }

        @Override
        public void afterHookedMember(@NonNull IHookBridge.IMemberHookParam param) throws Throwable {
            WrappedHookParam wrappedParam = (WrappedHookParam) param.getExtra();
            if (wrappedParam == null) {
                throw new IllegalStateException("beforeHookedMember not called");
            }
            wrappedParam.method = param.getMember();
            wrappedParam.thisObject = param.getThisObject();
            wrappedParam.args = param.getArgs();
            callback.afterHookedMethod(wrappedParam);
        }
    }

    public static void log(String message) {
        StartupInfo.getLoaderInfo().log(message);
    }

    public static void log(Throwable tr) {
        StartupInfo.getLoaderInfo().log(tr);
    }

}
