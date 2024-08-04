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

package io.github.qauxv.util.hookimpl.lsplant;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class LsplantBridge {

    private LsplantBridge() {
        throw new AssertionError("No instance for you!");
    }

    static native void nativeInitializeLsplant() throws RuntimeException;

    // return backup method if success, or null if failed
    // LSPlant backup is always a method, regardless of the target type being method or constructor
    @Nullable
    static native Method nativeHookMethod(@NonNull Member target, @NonNull Member callback, @NonNull Object context) throws RuntimeException;

    static native boolean nativeIsMethodHooked(@NonNull Member target) throws RuntimeException;

    static native boolean nativeUnhookMethod(@NonNull Member target) throws RuntimeException;

    static native boolean nativeDeoptimizeMethod(@NonNull Member target) throws RuntimeException;

}
