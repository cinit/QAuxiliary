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

package io.github.qauxv.util.dexkit.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.dexkit.DexDeobfsBackend;
import io.github.qauxv.util.dexkit.DexFlow;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexMethodDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;

public class LegacyDexDeobfs implements DexDeobfsBackend {

    private LegacyDexDeobfs() {
    }

    @Nullable
    @Override
    public DexMethodDescriptor doFindMethodImpl(int i) {
        var ret = DexKit.getMethodDescFromCache(i);
        if (ret != null) {
            return ret;
        }
        ret = searchVerifyDexMethodDesc(i);
        if (ret != null) {
            saveDescriptor(i, ret);
        }
        return ret;
    }

    @Override
    public boolean isBatchFindMethodSupported() {
        return false;
    }

    @NonNull
    @Override
    public DexMethodDescriptor[] doBatchFindMethodImpl(@NonNull int[] indexArray) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("not supported by " + this.getClass().getName());
    }

    @Nullable
    private static DexMethodDescriptor searchVerifyDexMethodDesc(int i) {
        ClassLoader loader = Initiator.getHostClassLoader();
        long record = 0;
        int[] qf = DexKit.d(i);
        byte[][] keys = DexKit.b(i);
        for (int dexi : qf) {
            record |= 1L << dexi;
            try {
                for (byte[] k : keys) {
                    HashSet<DexMethodDescriptor> rets = findMethodsByConstString(i, k, dexi, loader);
                    if (rets != null && rets.size() > 0) {
                        // verify
                        DexMethodDescriptor method = DexKit.verifyTargetMethod(i, rets);
                        if (method != null) {
                            return method;
                        }
                    }
                }
            } catch (FileNotFoundException ignored) {
            }
        }
        int dexi = 1;
        while (true) {
            if ((record & (1L << dexi)) != 0) {
                dexi++;
                continue;
            }
            try {
                for (byte[] k : keys) {
                    HashSet<DexMethodDescriptor> ret = findMethodsByConstString(i, k, dexi, loader);
                    if (ret != null && ret.size() > 0) {
                        // verify
                        DexMethodDescriptor method = DexKit.verifyTargetMethod(i, ret);
                        if (method != null) {
                            return method;
                        }
                    }
                }
            } catch (FileNotFoundException ignored) {
                return null;
            }
            dexi++;
        }
    }

    /**
     * get ALL the possible class names
     *
     * @param key    the pattern
     * @param i      C_XXXX
     * @param loader to get dex file
     * @return ["abc","ab"]
     * @throws FileNotFoundException apk has no classesN.dex
     */
    public static HashSet<DexMethodDescriptor> findMethodsByConstString(int id, byte[] key, int i, ClassLoader loader) throws FileNotFoundException {
        String name;
        byte[] buf = new byte[4096];
        byte[] content;
        if (i == 1) {
            name = "classes.dex";
        } else {
            name = "classes" + i + ".dex";
        }
        HashSet<URL> urls = new HashSet<>(3);
        try {
            Enumeration<URL> eu;
            eu = (Enumeration<URL>) Reflex.invokeVirtual(loader, "findResources", name, String.class);
            if (eu != null) {
                while (eu.hasMoreElements()) {
                    URL url = eu.nextElement();
                    if (url.toString().contains(HostInfo.getPackageName())) {
                        urls.add(url);
                    }
                }
            }
        } catch (Throwable e) {
            Log.e(e);
        }
        if (!loader.getClass().equals(PathClassLoader.class) && !loader.getClass().equals(DexClassLoader.class) && loader.getParent() != null) {
            try {
                Enumeration<URL> eu;
                eu = (Enumeration<URL>) Reflex.invokeVirtual(loader.getParent(), "findResources", name, String.class);
                if (eu != null) {
                    while (eu.hasMoreElements()) {
                        URL url = eu.nextElement();
                        if (url.toString().contains(HostInfo.getPackageName())) {
                            urls.add(url);
                        }
                    }
                }
            } catch (Throwable e) {
                Log.e(e);
            }
        }
        //log("dex" + i + ":" + url);
        if (urls.size() == 0) {
            throw new FileNotFoundException(name);
        }
        InputStream in;
        try {
            HashSet<DexMethodDescriptor> rets = new HashSet<>();
            for (URL url : urls) {
                in = url.openStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int ii;
                while ((ii = in.read(buf)) != -1) {
                    baos.write(buf, 0, ii);
                }
                in.close();
                content = baos.toByteArray();
                ArrayList<Integer> opcodeOffsets = a(content, key);
                for (int j = 0; j < opcodeOffsets.size(); j++) {
                    try {
                        DexMethodDescriptor desc = DexFlow.getDexMethodByOpOffset(content, opcodeOffsets.get(j), true);
                        if (desc != null) {
                            Log.d("doFindMethod: use Legacy, id " + id + ", find:" + desc);
                            rets.add(desc);
                        }
                    } catch (InternalError ignored) {
                    }
                }
            }
            return rets;
        } catch (IOException e) {
            Log.e(e);
            return null;
        }
    }

    public static ArrayList<Integer> a(byte[] buf, byte[] target) {
        ArrayList<Integer> rets = new ArrayList<>();
        int[] ret = new int[1];
        ret[0] = DexFlow.arrayIndexOf(buf, target, 0, buf.length);
        ret[0] = DexFlow.arrayIndexOf(buf, DexFlow.int2u4le(ret[0]), 0, buf.length);
        int strIdx = (ret[0] - DexFlow.readLe32(buf, 0x3c)) / 4;
        if (strIdx > 0xFFFF) {
            target = DexFlow.int2u4le(strIdx);
        } else {
            target = DexFlow.int2u2le(strIdx);
        }
        int off = 0;
        while (true) {
            off = DexFlow.arrayIndexOf(buf, target, off + 1, buf.length);
            if (off == -1) {
                break;
            }
            if (buf[off - 2] == (byte) 26/*Opcodes.OP_CONST_STRING*/
                    || buf[off - 2] == (byte) 27)/* Opcodes.OP_CONST_STRING_JUMBO*/ {
                ret[0] = off - 2;
                int opcodeOffset = ret[0];
                if (buf[off - 2] == (byte) 27 && strIdx < 0x10000) {
                    if (DexFlow.readLe32(buf, opcodeOffset + 2) != strIdx) {
                        continue;
                    }
                }
                rets.add(opcodeOffset);
            }
        }
        return rets;
    }

    public static final String ID = "Legacy";
    public static final String NAME = "Legacy(默认)";

    @NonNull
    @Override
    public String getId() {
        return ID;
    }

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void close() {
        // we do nothing because we do not cache anything
        // TODO: 2022-08-26 add dex weak reference cache
    }

    public static LegacyDexDeobfs newInstance() {
        return new LegacyDexDeobfs();
    }
}
