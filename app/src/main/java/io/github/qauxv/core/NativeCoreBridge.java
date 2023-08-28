/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See theqwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package io.github.qauxv.core;

import androidx.annotation.Keep;
import io.github.qauxv.base.RuntimeErrorTracer;
import io.github.qauxv.util.Log;

public class NativeCoreBridge {

    private NativeCoreBridge() {
        throw new AssertionError("No NativeCoreBridge instances for you!");
    }

    public static native void initNativeCore(String packageName, int currentSdkLevel, String versionName, long longVersionCode);

    @Keep
    private static void nativeTraceErrorHelper(Object thiz, Throwable error) {
        if (thiz instanceof RuntimeErrorTracer) {
            RuntimeErrorTracer tracer = (RuntimeErrorTracer) thiz;
            tracer.traceError(error);
        } else {
            Log.e("NativeCoreBridge nativeTraceErrorHelper: thiz is not a RuntimeErrorTracer, got "
                    + thiz.getClass().getName() + ", errorMessage: " + error);
        }
    }

}
