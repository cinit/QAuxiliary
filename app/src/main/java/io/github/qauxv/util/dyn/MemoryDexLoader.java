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

package io.github.qauxv.util.dyn;

import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import dalvik.system.InMemoryDexClassLoader;
import java.nio.ByteBuffer;

public class MemoryDexLoader {

    private MemoryDexLoader() {
        throw new AssertionError("No instance for you!");
    }

    public static ClassLoader createClassLoaderWithDex(@NonNull byte[] dexFile, @Nullable ClassLoader parent) {
        if (dexFile.length == 0) {
            throw new IllegalArgumentException("dexFile is empty");
        }
        if (parent == null) {
            parent = Runtime.class.getClassLoader();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return createClassLoaderWithDexAboveOreo(dexFile, parent);
        } else {
            return nativeCreateClassLoaderWithDexBelowOreo(dexFile, parent);
        }
    }

    @RequiresApi(26)
    private static ClassLoader createClassLoaderWithDexAboveOreo(@NonNull byte[] dexFile, @NonNull ClassLoader parent) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(dexFile);
        return new InMemoryDexClassLoader(byteBuffer, parent);
    }

    private static native ClassLoader nativeCreateClassLoaderWithDexBelowOreo(@NonNull byte[] dexFile, @NonNull ClassLoader parent);

}
