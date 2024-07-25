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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
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
import dalvik.system.DexFile;
import dalvik.system.InMemoryDexClassLoader;
import io.github.qauxv.util.IoUtils;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.Objects;

public class MemoryDexLoader {

    private MemoryDexLoader() {
        throw new AssertionError("No instance for you!");
    }

    @NonNull
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
    @NonNull
    private static ClassLoader createClassLoaderWithDexAboveOreo(@NonNull byte[] dexFile, @NonNull ClassLoader parent) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(dexFile);
        return new InMemoryDexClassLoader(byteBuffer, parent);
    }

    @NonNull
    private static native ClassLoader nativeCreateClassLoaderWithDexBelowOreo(@NonNull byte[] dexFile, @NonNull ClassLoader parent);

    /**
     * Create a DexFile instance from a byte array. Applications generally should not create a DexFile directly.
     * <p>
     * It will hurt performance in most cases and will lead to incorrect execution of bytecode in the worst case.
     *
     * @param dexBytes        dex file data
     * @param definingContext the class loader where the dex file will be attached to
     * @param name            optional name for the dex file, may be null
     * @return a DexFile instance
     */
    @NonNull
    public static DexFile createDexFileFormBytes(@NonNull byte[] dexBytes, @NonNull ClassLoader definingContext, @Nullable String name) {
        Objects.requireNonNull(dexBytes, "dexBytes is null");
        Objects.requireNonNull(definingContext, "definingContext is null");
        if (dexBytes.length < 20) {
            throw new IllegalArgumentException("dexBytes is too short");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return createDexFileFormBytesAboveOreo(dexBytes, definingContext, name);
        } else {
            return nativeCreateDexFileFormBytesBelowOreo(dexBytes, definingContext, name);
        }
    }

    @RequiresApi(26)
    @NonNull
    private static DexFile createDexFileFormBytesAboveOreo(@NonNull byte[] dexBytes, @NonNull ClassLoader definingContext, @Nullable String name) {
        // Android 8.0 - 10:  DexFile(ByteBuffer buf) throws IOException;
        // Android 10+: DexFile(ByteBuffer[] bufs, ClassLoader loader, DexPathList.Element[] elements);
        Constructor<DexFile> constructor1 = null;
        Constructor<DexFile> constructor3 = null;
        Class<?> kElementArray;
        try {
            kElementArray = Class.forName("[Ldalvik.system.DexPathList$Element;");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class.forName(\"[Ldalvik.system.DexPathList$Element;\"); fail", e);
        }
        try {
            constructor1 = DexFile.class.getDeclaredConstructor(ByteBuffer.class);
            constructor1.setAccessible(true);
        } catch (NoSuchMethodException ignored) {
        }
        try {
            constructor3 = DexFile.class.getDeclaredConstructor(ByteBuffer[].class, ClassLoader.class, kElementArray);
            constructor3.setAccessible(true);
        } catch (NoSuchMethodException ignored) {
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(dexBytes);
        if (constructor3 != null) {
            ByteBuffer[] byteBuffers = new ByteBuffer[]{byteBuffer};
            try {
                return constructor3.newInstance(byteBuffers, definingContext, null);
            } catch (ReflectiveOperationException e) {
                throw IoUtils.unsafeThrowForIteCause(e);
            }
        } else if (constructor1 != null) {
            try {
                return constructor1.newInstance(byteBuffer);
            } catch (ReflectiveOperationException e) {
                throw IoUtils.unsafeThrowForIteCause(e);
            }
        } else {
            throw new IllegalStateException("DexFile constructor not found, SDK_INT=" + Build.VERSION.SDK_INT);
        }
    }

    @NonNull
    private static native DexFile nativeCreateDexFileFormBytesBelowOreo(@NonNull byte[] dexBytes, @NonNull ClassLoader definingContext, @Nullable String name);

}
