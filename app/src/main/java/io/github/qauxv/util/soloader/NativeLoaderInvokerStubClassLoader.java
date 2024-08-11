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

package io.github.qauxv.util.soloader;

import androidx.annotation.NonNull;
import dalvik.system.BaseDexClassLoader;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

public class NativeLoaderInvokerStubClassLoader extends BaseDexClassLoader {

    private final ClassLoader mReferencedClassLoader;

    public NativeLoaderInvokerStubClassLoader(@NonNull ClassLoader reference, @NonNull ClassLoader parent, @NonNull String librarySearchPath) {
        // parent class loader are deliberately set to the target class loader
        // this class loader is only used to load the native library
        // and does NOT define any class
        super("", null, librarySearchPath, parent);
        mReferencedClassLoader = reference;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return mReferencedClassLoader.loadClass(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return mReferencedClassLoader.loadClass(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return mReferencedClassLoader.getResources(name);
    }

    @Override
    public URL getResource(String name) {
        return mReferencedClassLoader.getResource(name);
    }

}
