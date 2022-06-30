/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package io.github.qauxv.util;

import androidx.annotation.NonNull;
import cc.ioctl.util.HostInfo;
import java.io.IOException;
import java.util.Objects;

public class MemoryFileUtils {

    private MemoryFileUtils() {
        throw new AssertionError("no instance");
    }

    static boolean sInitialized = false;

    public static int createMemoryFile(@NonNull String name, int size) throws IOException {
        if (!sInitialized) {
            int rc = nativeInitializeTmpDir(HostInfo.getApplication().getCacheDir().getAbsolutePath());
            if (rc != 0) {
                throw new IOException("nativeInitializeTmpDir failed: " + rc);
            }
            sInitialized = true;
        }
        Objects.requireNonNull(name, "name is null");
        if ("".equals(name)) {
            throw new IllegalArgumentException("name cannot be empty");
        }
        if (".".equals(name) || "..".equals(name) || name.contains("/")) {
            throw new IllegalArgumentException("invalid name: '" + name + "'");
        }
        if (size < 0) {
            throw new IllegalArgumentException("size must be >= 0");
        }
        int result = nativeCreateMemoryFile0(name, size);
        if (result < 0) {
            throw new IOException("nativeCreateMemoryFile0 failed, errno=" + result);
        }
        return result;
    }

    private static native int nativeCreateMemoryFile0(String name, int size);

    private static native int nativeInitializeTmpDir(String cacheDir);
}
