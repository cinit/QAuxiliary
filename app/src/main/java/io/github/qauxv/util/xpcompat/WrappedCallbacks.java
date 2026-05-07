package io.github.qauxv.util.xpcompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.qauxv.loader.hookapi.IHookBridge;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/*package*/ class WrappedCallbacks {

    private WrappedCallbacks() {
    }

    @NonNull
    private static Class<?> getWrapperType(@NonNull Class<?> type) {
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == void.class) {
            return Void.class;
        }
        throw new IllegalArgumentException("Not a primitive type: " + type);
    }

    public static class WrappedHookParam extends XC_MethodHook.MethodHookParam {

        private WrappedHookParam() {
        }

        private IHookBridge.IMemberHookParam param;

        @Override
        public Object getResult() {
            return param.getResult();
        }

        private void checkResultCast(@NonNull Member member, @Nullable Object result) throws ClassCastException {
            if (!(member instanceof Method)) {
                // Constructors always return void, so we can skip the check.
                return;
            }
            Class<?> returnType = ((Method) member).getReturnType();
            if (returnType == void.class) {
                return;
            }
            if (returnType.isPrimitive()) {
                if (result == null) {
                    throw new ClassCastException("Cannot return null for a primitive return type");
                }
                Class<?> wrapperType = getWrapperType(returnType);
                wrapperType.cast(result);
            } else {
                if (result != null) {
                    returnType.cast(result);
                }
            }
        }

        @Override
        public void setResult(Object result) {
            checkResultCast(param.getMember(), result);
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

    public static class WrappedHookCallback implements IHookBridge.IMemberHookCallback {

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

}
