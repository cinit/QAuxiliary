package io.github.qauxv.loader.sbl.lsp100.dyn;

import androidx.annotation.Keep;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;
import io.github.qauxv.loader.sbl.lsp100.Lsp100HookWrapper;

@Keep
public class Lsp100CallbackProxy {

    private Lsp100CallbackProxy() {
    }

    /**
     * The default priority for Xposed hooks.
     */
    @Keep
    @XposedHooker
    public static class P0000000050 implements XposedInterface.Hooker {

        public static final int tag = 50;

        @Keep
        @BeforeInvocation
        public static Lsp100HookWrapper.InvocationParamWrapper before(XposedInterface.BeforeHookCallback callback) {
            return Lsp100HookWrapper.Lsp100HookAgent.handleBeforeHookedMethod(callback, tag);
        }

        @Keep
        @AfterInvocation
        public static void after(XposedInterface.AfterHookCallback callback, Lsp100HookWrapper.InvocationParamWrapper param) {
            Lsp100HookWrapper.Lsp100HookAgent.handleAfterHookedMethod(callback, param, tag);
        }

    }

    /**
     * LoadDex hook uses priority 51.
     */
    @Keep
    @XposedHooker
    public static class P0000000051 implements XposedInterface.Hooker {

        public static final int tag = 51;

        @Keep
        @BeforeInvocation
        public static Lsp100HookWrapper.InvocationParamWrapper before(XposedInterface.BeforeHookCallback callback) {
            return Lsp100HookWrapper.Lsp100HookAgent.handleBeforeHookedMethod(callback, tag);
        }

        @Keep
        @AfterInvocation
        public static void after(XposedInterface.AfterHookCallback callback, Lsp100HookWrapper.InvocationParamWrapper param) {
            Lsp100HookWrapper.Lsp100HookAgent.handleAfterHookedMethod(callback, param, tag);
        }

    }

}
