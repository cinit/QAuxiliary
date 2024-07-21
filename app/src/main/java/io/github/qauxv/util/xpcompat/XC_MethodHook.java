package io.github.qauxv.util.xpcompat;

import io.github.qauxv.loader.hookapi.IHookBridge;
import java.lang.reflect.Member;

/**
 * Callback class for method hooks.
 *
 * <p>Usually, anonymous subclasses of this class are created which override
 * {@link #beforeHookedMethod} and/or {@link #afterHookedMethod}.
 */
public abstract class XC_MethodHook {

    private final int priority;

    /**
     * Creates a new callback with default priority.
     */
    public XC_MethodHook() {
        priority = IHookBridge.PRIORITY_DEFAULT;
    }

    /**
     * Creates a new callback with a specific priority.
     *
     * <p class="note">Note that {@link #afterHookedMethod} will be called in reversed order, i.e.
     * the callback with the highest priority will be called last. This way, the callback has the final control over the return value.
     * {@link #beforeHookedMethod} is called as usual, i.e. highest priority first.
     *
     * @param priority The priority for this callback.
     */
    public XC_MethodHook(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * Called before the invocation of the method.
     *
     * <p>You can use {@link XC_MethodHook.MethodHookParam#setResult} and
     * {@link XC_MethodHook.MethodHookParam#setThrowable} to prevent the original method from being called.
     *
     * <p>Note that implementations shouldn't call {@code super(param)}, it's not necessary.
     *
     * @param param Information about the method call.
     * @throws Throwable Everything the callback throws is caught and logged.
     */
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
    }

    /**
     * Called after the invocation of the method.
     *
     * <p>You can use {@link XC_MethodHook.MethodHookParam#setResult} and
     * {@link XC_MethodHook.MethodHookParam#setThrowable} to modify the return value of the original method.
     *
     * <p>Note that implementations shouldn't call {@code super(param)}, it's not necessary.
     *
     * @param param Information about the method call.
     * @throws Throwable Everything the callback throws is caught and logged.
     */
    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
    }

    /**
     * Wraps information about the method call and allows to influence it.
     */
    public static abstract class MethodHookParam {

        protected MethodHookParam() {
        }

        /**
         * The hooked method/constructor.
         */
        public Member method;

        /**
         * The {@code this} reference for an instance method, or {@code null} for static methods.
         */
        public Object thisObject;

        /**
         * Arguments to the method call.
         */
        public Object[] args;

        /**
         * Returns the result of the method call.
         */
        public abstract Object getResult();

        /**
         * Modify the result of the method call.
         *
         * <p>If called from {@link #beforeHookedMethod}, it prevents the call to the original method.
         */
        public abstract void setResult(Object result);

        /**
         * Returns the {@link Throwable} thrown by the method, or {@code null}.
         */
        public abstract Throwable getThrowable();

        /**
         * Returns true if an exception was thrown by the method.
         */
        public abstract boolean hasThrowable();

        /**
         * Modify the exception thrown of the method call.
         *
         * <p>If called from {@link #beforeHookedMethod}, it prevents the call to the original method.
         */
        public abstract void setThrowable(Throwable throwable);

        /**
         * Returns the result of the method call, or throws the Throwable caused by it.
         */
        public abstract Object getResultOrThrowable() throws Throwable;
    }

    /**
     * An object with which the method/constructor can be unhooked.
     */
    public static class Unhook {

        private final IHookBridge.MemberUnhookHandle unhookHandle;
        private final XC_MethodHook callback;

        /*package*/ Unhook(IHookBridge.MemberUnhookHandle unhookHandle, XC_MethodHook callback) {
            this.unhookHandle = unhookHandle;
            this.callback = callback;
        }

        /**
         * Returns the method/constructor that has been hooked.
         */
        public Member getHookedMethod() {
            return unhookHandle.getMember();
        }

        public XC_MethodHook getCallback() {
            return callback;
        }

        public void unhook() {
            unhookHandle.unhook();
        }

    }
}
