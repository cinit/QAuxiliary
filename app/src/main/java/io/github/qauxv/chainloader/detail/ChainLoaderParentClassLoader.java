/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
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

package io.github.qauxv.chainloader.detail;

import android.content.Context;

public class ChainLoaderParentClassLoader extends ClassLoader {

    public static final ChainLoaderParentClassLoader INSTANCE = new ChainLoaderParentClassLoader();

    private ChainLoaderParentClassLoader() {
        super(Context.class.getClassLoader());
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.startsWith("io.github.qauxv.chainloader.") || name.startsWith("io.github.qauxv.loader.hookapi.")) {
            // search the class in the name of the module
            return Class.forName(name);
        }
        return super.findClass(name);
    }

}
