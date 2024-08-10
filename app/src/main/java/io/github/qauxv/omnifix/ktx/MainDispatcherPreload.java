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

package io.github.qauxv.omnifix.ktx;

import io.github.qauxv.util.IoUtils;
import io.github.qauxv.util.Log;
import java.lang.reflect.Field;

public class MainDispatcherPreload {

    private MainDispatcherPreload() {
        throw new AssertionError("No instance for you!");
    }

    public static void preload() {
        try {
            Class<?> klass = Class.forName("kotlinx.coroutines.internal.MainDispatcherLoader");
            Field dispatcherField = klass.getDeclaredField("dispatcher");
            dispatcherField.setAccessible(true);
            // deliberately get the reflection object before we check whether manual invention is needed
            // so that if anything goes wrong we can find it as soon as possible
            String oldValue = System.getProperty("kotlinx.coroutines.fast.service.loader");
            if (oldValue != null) {
                Log.w("'kotlinx.coroutines.fast.service.loader' is already set to " + oldValue + ", this is unexpected");
            }
            boolean useFastLoader = Boolean.parseBoolean(System.getProperty("kotlinx.coroutines.fast.service.loader", "true"));
            if (!useFastLoader) {
                System.setProperty("kotlinx.coroutines.fast.service.loader", "true");
                try {
                    // do preload
                    Object dispatcher = dispatcherField.get(null);
                    Log.d("preload MainDispatcherLoader: " + dispatcher);
                } finally {
                    // set the system property back
                    if (oldValue == null) {
                        System.clearProperty("kotlinx.coroutines.fast.service.loader");
                    } else {
                        System.setProperty("kotlinx.coroutines.fast.service.loader", oldValue);
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw IoUtils.unsafeThrowForIteCause(e);
        }
    }

}
