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

public class NativeLoaderInvokerClassLoader extends BaseDexClassLoader {

    private final ClassLoader mReferencedClassLoader;

    public NativeLoaderInvokerClassLoader(@NonNull String dexPath, @NonNull String librarySearchPath, @NonNull ClassLoader reference) {
        // make sure that both the parent class loader and the native library path are NOT bridged
        super(dexPath, null, librarySearchPath, ClassLoader.class.getClassLoader());
        mReferencedClassLoader = reference;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name != null && name.startsWith("io.github.qauxv.isolated.soloader.")) {
            // this will define the target class
            return super.findClass(name);
        } else {
            // reference
            return mReferencedClassLoader.loadClass(name);
        }
    }

}
