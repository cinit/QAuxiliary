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
import cc.ioctl.util.HookUtilsKt;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.dexkit.DexDeobfsBackend;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexMethodDescriptor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import me.iacn.biliroaming.utils.DexHelper;

public class DexBuilderDexDeobfs implements DexDeobfsBackend {

    private DexHelper mHelper;
    private final Lock mReadLock;

    private DexBuilderDexDeobfs() {
        SharedResourceImpl res = SharedResourceImpl.getInstance();
        mReadLock = res.increaseRefCount().readLock();
        mHelper = res.getResources();
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
    @Override
    public DexMethodDescriptor doFindMethodImpl(int i) {
        ensureOpen();
        mReadLock.lock();
        try {
            var ret = DexKit.getMethodDescFromCache(i);
            if (ret != null) {
                return ret;
            }
            DexHelper helper = mHelper;
            ensureOpen();
            byte[][] keys = DexKit.b(i);
            HashSet<DexMethodDescriptor> methods = new HashSet<>();
            for (byte[] key : keys) {
                String str = new String(Arrays.copyOfRange(key, 1, key.length));
                Log.d("DexBuilderDexDeobfs.doFindMethodImpl: id " + i + ", key:" + str);
                long[] ms = helper.findMethodUsingString(
                        str, true, -1, (short) -1, null, -1,
                        null, null, null, false);
                for (long methodIndex : ms) {
                    Executable m = helper.decodeMethodIndex(methodIndex);
                    if (m instanceof Method) {
                        methods.add(new DexMethodDescriptor((Method) m));
                        Log.d("DexBuilderDexDeobfs.doFindMethodImpl: id " + i + ", m:" + m);
                    } else {
                        Log.i("DexBuilderDexDeobfs.doFindMethodImpl find Constructor: " + m + ", but not support Constructor currently");
                    }
                }
            }
            if (methods.size() != 0) {
                ret = DexKit.verifyTargetMethod(i, methods);
                if (ret == null) {
                    Log.i(methods.size() + " classes candidates found for " + i + ", none satisfactory.");
                    try (LegacyDexDeobfs legacy = LegacyDexDeobfs.newInstance()) {
                        ret = legacy.doFindMethodImpl(i);
                        return ret;
                    }
                }
                Log.d("save id: " + i + ",method: " + ret);
                saveDescriptor(i, ret);
            }
            try (LegacyDexDeobfs legacy = LegacyDexDeobfs.newInstance()) {
                ret = legacy.doFindMethodImpl(i);
                return ret;
            }
        } finally {
            mReadLock.unlock();
        }
    }

    public static final String ID = "DexBuilder";
    public static final String NAME = "DexBuilder(更快)";

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

    private synchronized void ensureOpen() {
        if (mHelper == null) {
            throw new IllegalStateException("DexBuilder is closed");
        }
    }

    @Override
    public synchronized void close() {
        if (mHelper != null) {
            SharedResourceImpl.getInstance().decreaseRefCount();
            mHelper = null;
        }
    }

    public static DexBuilderDexDeobfs newInstance() {
        return new DexBuilderDexDeobfs();
    }

    private static class SharedResourceImpl extends SharedRefCountResourceImpl<DexHelper> {

        private static SharedResourceImpl sInstance = null;

        public static synchronized SharedResourceImpl getInstance() {
            if (sInstance == null) {
                sInstance = new SharedResourceImpl();
            }
            return sInstance;
        }

        @NonNull
        @Override
        protected DexHelper openResourceInternal() {
            ClassLoader dexClassLoader = HookUtilsKt.findDexClassLoader(Initiator.getHostClassLoader());
            Log.d("new DexBuilderHelper");
            return new DexHelper(dexClassLoader);
        }

        @Override
        protected void closeResourceInternal(@NonNull DexHelper res) {
            res.close();
            Log.d("close DexBuilderHelper");
        }
    }
}
