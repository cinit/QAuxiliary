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

package io.github.qauxv.util.dexkit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtilsKt;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import me.iacn.biliroaming.utils.DexHelper;

public class DexBuilderDexDeobfs implements DexDeobfsBackend {

    public static final DexBuilderDexDeobfs INSTANCE = new DexBuilderDexDeobfs();
    private static DexHelper helper = null;

    private static DexHelper getHelper() {
        if (helper == null) {
            ClassLoader dexClassLoader = HookUtilsKt.findDexClassLoader(Initiator.getHostClassLoader());
            Log.d("new DexHelper");
            helper = new DexHelper(dexClassLoader);
        }
        return helper;
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
        var ret = DexKit.getMethodDescFromCache(i);
        if (ret != null) {
            return ret;
        }
        DexHelper helper = getHelper();
        byte[][] keys = DexKit.b(i);
        HashSet<DexMethodDescriptor> methods = new HashSet<>();
        for (byte[] key : keys) {
            String str = new String(Arrays.copyOfRange(key, 1, key.length));
            Log.d("doFindMethodFromNative: id " + i + ", key:" + str);
            long[] ms = helper.findMethodUsingString(
                    str, true, -1, (short) -1, null, -1,
                    null, null, null, false);
            for (long methodIndex : ms) {
                Executable m = helper.decodeMethodIndex(methodIndex);
                if (m instanceof Method) {
                    methods.add(new DexMethodDescriptor((Method) m));
                    Log.d("doFindMethod: use DexBuilder, id " + i + ", find:" + m);
                } else {
                    Log.i("find Constructor: " + m + ", but not support Constructor currently");
                }
            }
        }
        if (methods.size() != 0) {
            ret = DexKit.verifyTargetMethod(i, methods);
            if (ret == null) {
                Log.i(methods.size() + " classes candidates found for " + i + ", none satisfactory.");
                ret = LegacyDexDeobfs.INSTANCE.doFindMethodImpl(i);
                return ret;
            }
            Log.d("save id: " + i + ",method: " + ret);
            saveDescriptor(i, ret);
        }
        ret = LegacyDexDeobfs.INSTANCE.doFindMethodImpl(i);
        return ret;
    }

    @NonNull
    @Override
    public String getId() {
        return "DexBuilder";
    }

    @NonNull
    @Override
    public String getName() {
        return "DexBuilder(更快)";
    }
}
