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

package io.github.qauxv.isolated.soloader;

import android.annotation.SuppressLint;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;

/**
 * Keep this class simple and clean.
 */
@Keep
public class LoadLibraryInvoker {

    private LoadLibraryInvoker() {
        throw new AssertionError("No instances for you!");
    }

    private static volatile boolean sLoaded = false;
    private static volatile boolean sPrimaryNativeLibraryAttached = false;

    /**
     * Load a shared library from the specified absolute path.
     * <p>
     * This method is intended to be called by reflection.
     *
     * @param path The absolute path of the shared library to load.
     */
    @Keep
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    public static void invokeLoadLibrary(@NonNull String path) {
        if (sLoaded) {
            throw new IllegalStateException("Library has already been loaded");
        }
        System.load(path);
        sLoaded = true;
    }

    @Keep
    public static void invokeAttachClassLoader(ClassLoader agent) {
        if (sPrimaryNativeLibraryAttached) {
            throw new IllegalStateException("Primary native library has already been attached");
        }
        if (agent == null) {
            throw new NullPointerException("agent");
        }
        nativePrimaryNativeLibraryAttachClassLoader(agent);
        sPrimaryNativeLibraryAttached = true;
    }

    private static native void nativePrimaryNativeLibraryAttachClassLoader(@NonNull ClassLoader agent);

    /**
     * Attach the specified class loader to the secondary native library.
     * <p>
     * This method is intended to be called by the primary native library with native bridge, not from Java.
     *
     * @param agent    The class loader to attach, must not be null.
     * @param initInfo The initialization information, a plain-old-data structure native pointer.
     * @return a non-negative value if successful, otherwise negative or exception.
     */
    private static native int nativeSecondaryNativeLibraryAttachClassLoader(@NonNull ClassLoader agent, long[] initInfo);

    /**
     * Call android_dlopen_ext() with the specified file descriptor, library name and offset.
     * <p>
     * Note: This method is intended to be called by the primary native library with native bridge, not from Java.
     * <p>
     * The file descriptor will NOT be closed after the library is loaded.
     *
     * @param fd     the file descriptor of the shared library to load, must be valid.
     * @param name   the name of the shared library to load, must not be null or empty.
     * @param offset the offset of the shared library to load, must be valid.
     * @return a handle to the shared library, or null if an error occurred.
     * @throws RuntimeException if an error occurred.
     */
    private static native long nativeCallAndroidDlopenExt(int fd, @NonNull String name, long offset) throws RuntimeException;

}
