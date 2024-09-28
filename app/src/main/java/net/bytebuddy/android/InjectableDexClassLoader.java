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

package net.bytebuddy.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dalvik.system.BaseDexClassLoader;
import io.github.qauxv.util.hookimpl.InMemoryClassLoaderHelper;
import java.io.File;
import java.util.Objects;

public class InjectableDexClassLoader extends BaseDexClassLoader implements IAndroidInjectableClassLoader {

    /**
     * Constructs an instance. Note that all the *.jar and *.apk files from {@code dexPath} might be first extracted in-memory before the code is loaded. This
     * can be avoided by passing raw dex files (*.dex) in the {@code dexPath}.
     *
     * @param dexPath            the list of jar/apk files containing classes and resources, delimited by {@code File.pathSeparator}, which defaults to
     *                           {@code ":"} on Android.
     * @param optimizedDirectory this parameter is deprecated and has no effect since API level 26.
     * @param librarySearchPath  the list of directories containing native libraries, delimited by {@code File.pathSeparator}; may be {@code null}
     * @param parent             the parent class loader
     */
    public InjectableDexClassLoader(String dexPath, File optimizedDirectory, String librarySearchPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent == null ? System.class.getClassLoader() : parent);
    }

    public InjectableDexClassLoader(ClassLoader parent) {
        super("", null, null, parent);
    }

    @Override
    public void injectDex(@NonNull byte[] dexBytes, @Nullable String dexName) throws IllegalArgumentException {
        Objects.requireNonNull(dexBytes, "dexBytes");
        InMemoryClassLoaderHelper.INSTANCE.injectDexToClassLoader(this, dexBytes, dexName);
    }

}
