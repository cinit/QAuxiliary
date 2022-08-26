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
import cc.ioctl.util.HostInfo;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import java.lang.reflect.Method;

public interface DexDeobfsBackend {

    @NonNull
    String getId();

    @NonNull
    String getName();

    /**
     * Run the dex deobfuscation. This method may take a long time and should only be called in background thread.
     *
     * @param i the dex method index
     * @return target method descriptor, null if the target is not found.
     */
    @Nullable
    DexMethodDescriptor doFindMethodImpl(int i);

    boolean isBatchFindMethodSupported();

    @NonNull
    DexMethodDescriptor[] doBatchFindMethodImpl(@NonNull int[] indexArray) throws UnsupportedOperationException;

    @Nullable
    default Method doFindMethod(int i) {
        var descriptor = DexKit.getMethodDescFromCache(i);
        if (descriptor == null) {
            descriptor = doFindMethodImpl(i);
            if (descriptor == null) {
                Log.d("not found, save null");
                descriptor = DexKit.NO_SUCH_METHOD;
                saveDescriptor(i, descriptor);
                return null;
            }
        }
        try {
            if (DexKit.NO_SUCH_METHOD.toString().equals(descriptor.toString())) {
                return null;
            }
            if (descriptor.name.equals("<init>") || descriptor.name.equals("<clinit>")) {
                Log.i("doFindMethod(" + i + ") methodName == " + descriptor.name + " , return null");
                return null;
            }
            return descriptor.getMethodInstance(Initiator.getHostClassLoader());
        } catch (NoSuchMethodException e) {
            // ignore
        }
        return null;
    }

    @Nullable
    default Class<?> doFindClass(int i) {
        Class<?> ret = Initiator.load(DexKit.c(i));
        if (ret != null) {
            return ret;
        }
        var descriptor = DexKit.getMethodDescFromCache(i);
        if (descriptor == null) {
            descriptor = doFindMethodImpl(i);
            if (descriptor == null) {
                Log.d("not found, save null");
                descriptor = DexKit.NO_SUCH_METHOD;
                saveDescriptor(i, descriptor);
                return null;
            }
        }
        if (DexKit.NO_SUCH_METHOD.toString().equals(descriptor.toString())) {
            return null;
        }
        if (descriptor.name.equals("<init>") || descriptor.name.equals("<clinit>")) {
            Log.i("doFindMethod(" + i + ") methodName == " + descriptor.name + " , return null");
            return null;
        }
        return Initiator.load(descriptor.declaringClass);

    }

    default void saveDescriptor(int i, DexMethodDescriptor descriptor) {
        var cache = ConfigManager.getCache();
        cache.putString("cache_" + DexKit.a(i) + "_method", descriptor.toString());
        cache.putInt("cache_" + DexKit.a(i) + "_code", HostInfo.getVersionCode32());
        cache.save();
    }
}
