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

package io.github.qauxv.bridge.kernelcompat;

import org.jetbrains.annotations.NotNull;

public class KernelObjectHelper {

    private KernelObjectHelper() {
    }

    public static void throwKernelObjectNotSupported(@NotNull Class<?> clazz) {
        String shortName = clazz.getSimpleName();
        throw new UnsupportedOperationException("Kernel object " + shortName + " is not supported");
    }

    public static void throwKernelObjectNotSupported(@NotNull String name) {
        throw new UnsupportedOperationException("Kernel object " + name + " is not supported");
    }

}
