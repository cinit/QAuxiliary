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

package io.github.qauxv.util.dexkit;

import android.app.Application;
import androidx.annotation.Nullable;
import io.github.qauxv.util.HostInfo;
import io.github.qauxv.util.IoUtils;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class HostMainDexHelper {

    private HostMainDexHelper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    private static HashMap<Integer, WeakReference<byte[]>> sCachedDex = new HashMap<>(32);

    @Nullable
    private static byte[] extractDexFromHost(int index) {
        Application app = HostInfo.getHostInfo().getApplication();
        // get path of base.apk
        String apkPath = app.getApplicationInfo().sourceDir;
        String dexName = getNameForIndex(index);
        // FIXME 2023-07-25: on very old QQ/TIM versions, some dex is in assets
        try (ZipFile zipFile = new ZipFile(apkPath)) {
            ZipEntry entry = zipFile.getEntry(dexName);
            if (entry == null) {
                return null;
            }
            return IoUtils.readFully(zipFile.getInputStream(entry));
        } catch (IOException e) {
            IoUtils.unsafeThrow(e);
            // unreachable
            return null;
        }
    }

    public static boolean hasDexIndex(int i) {
        Application app = HostInfo.getHostInfo().getApplication();
        // get path of base.apk
        String apkPath = app.getApplicationInfo().sourceDir;
        String dexName = getNameForIndex(i);
        // FIXME 2023-07-25: on very old QQ/TIM versions, some dex is in assets
        try (ZipFile zipFile = new ZipFile(apkPath)) {
            ZipEntry entry = zipFile.getEntry(dexName);
            return entry != null;
        } catch (IOException e) {
            IoUtils.unsafeThrow(e);
            // unreachable
            return false;
        }
    }

    private static String getNameForIndex(int i) {
        if (i <= 1) {
            return "classes.dex";
        } else {
            return "classes" + i + ".dex";
        }
    }

    @Nullable
    public static byte[] getHostDexIndex(int i) {
        if (i <= 0) {
            return null;
        }
        // load from cache
        WeakReference<byte[]> weakReference = sCachedDex.get(i);
        if (weakReference != null) {
            byte[] bytes = weakReference.get();
            if (bytes != null) {
                return bytes;
            }
        }
        // load from host
        byte[] bytes = extractDexFromHost(i);
        if (bytes != null) {
            sCachedDex.put(i, new WeakReference<>(bytes));
        }
        return bytes;
    }


    public static class DexIterator implements Iterator<byte[]> {

        private int nextIndex = 1;

        private DexIterator() {
        }

        public boolean hasNext() {
            return HostMainDexHelper.hasDexIndex(this.nextIndex);
        }

        public byte[] next() {
            byte[] hostDexIndex = HostMainDexHelper.getHostDexIndex(this.nextIndex);
            if (hostDexIndex != null) {
                this.nextIndex++;
            }
            return hostDexIndex;
        }

        public int getLastIndex() {
            return this.nextIndex - 1;
        }

    }

    public static Iterator<byte[]> getDexIterator() {
        return new DexIterator();
    }

    public static Iterable<byte[]> asIterable() {
        return DexIterator::new;
    }

    @Nullable
    public static byte[] findDexWithClass(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name == null || name.isEmpty()");
        }
        for (byte[] dex : asIterable()) {
            if (DexFlow.hasClassInDex(dex, name)) {
                return dex;
            }
        }
        return null;
    }

    @Nullable
    public static byte[] findDexWithClass(Class<?> klass) {
        Objects.requireNonNull(klass, "klass == null");
        return findDexWithClass(klass.getName());
    }

}
