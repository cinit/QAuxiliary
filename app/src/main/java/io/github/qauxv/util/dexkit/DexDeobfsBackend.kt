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
package io.github.qauxv.util.dexkit

import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log
import java.io.Closeable
import java.lang.reflect.Method

interface DexDeobfsBackend : Closeable {
    val id: String
    val name: String
    val isBatchFindMethodSupported: Boolean

    /**
     * Run the dex deobfuscation. This method may take a long time and should only be called in background thread.
     *
     * @param target the dex method target
     * @return target method descriptor, null if the target is not found.
     */
    fun doFindMethodImpl(target: DexKitTarget): DexMethodDescriptor?

    @Throws(UnsupportedOperationException::class)
    fun doBatchFindMethodImpl(targetArray: Array<DexKitTarget>)

    /**
     * Close the backend, memory will be release when ref-count decrease to 0.
     *
     *
     * No other method should be called after this method is called.
     */
    override fun close()
    fun doFindMethod(target: DexKitTarget): Method? {
        var descriptor = DexKit.getMethodDescFromCache(target)
        if (descriptor == null) {
            descriptor = doFindMethodImpl(target)
            if (descriptor == null) {
                Log.d("not found, save null")
                descriptor = DexKit.NO_SUCH_METHOD
                target.descCache = descriptor.toString()
                return null
            }
        }
        try {
            if (DexKit.NO_SUCH_METHOD.toString() == descriptor.toString()) {
                return null
            }
            if (descriptor.name == "<init>" || descriptor.name == "<clinit>") {
                Log.i("doFindMethod(" + target.name + ") methodName == " + descriptor.name + " , return null")
                return null
            }
            return descriptor.getMethodInstance(Initiator.getHostClassLoader())
        } catch (e: NoSuchMethodException) {
            // ignore
        }
        return null
    }

    fun doFindClass(target: DexKitTarget): Class<*>? {
        val ret = Initiator.load(target.declaringClass)
        if (ret != null) {
            return ret
        }
        var descriptor = DexKit.getMethodDescFromCache(target)
        if (descriptor == null) {
            descriptor = doFindMethodImpl(target)
            if (descriptor == null) {
                Log.d("not found, save null")
                descriptor = DexKit.NO_SUCH_METHOD
                target.descCache = descriptor.toString()
                return null
            }
        }
        if (DexKit.NO_SUCH_METHOD.toString() == descriptor.toString()) {
            return null
        }
        if (descriptor.name == "<init>" || descriptor.name == "<clinit>") {
            Log.i("doFindMethod(${target.name}" + ") methodName == " + descriptor.name + " , return null")
            return null
        }
        return Initiator.load(descriptor.declaringClass)
    }
}
