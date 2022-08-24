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

import cc.ioctl.util.findDexClassLoader
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log
import me.teble.DexKitHelper
import java.util.Arrays

object DexKitDeobfs: DexDeobfsBackend {

    private val dexClassLoader: ClassLoader = Initiator.getHostClassLoader().findDexClassLoader()!!

    val helper: DexKitHelper by lazy {
        Log.d("new DexKitHelper")
        DexKitHelper(dexClassLoader)
    }

    override fun doFindMethodImpl(i: Int): DexMethodDescriptor? {
        var ret = DexKit.getMethodDescFromCache(i)
        if (ret != null) {
            return ret
        }
        val keys = DexKit.b(i)
        val methods = HashSet<DexMethodDescriptor>()
        for (key in keys) {
            val str = String(Arrays.copyOfRange(key, 1, key.size))
            Log.d("doFindMethodFromNative: id $i, key:$str")
            val descArray = helper.findMethodUsedString(str)
            descArray.forEach {
                val desc = DexMethodDescriptor(it)
                if (desc.name == "<init>" || desc.name == "<clinit>") {
                    Log.i("doFindMethod($i) methodName == ${desc.name}, skip")
                } else {
                    methods.add(desc)
                    Log.d("doFindMethodFromNative: id $i, m:$desc")
                }
            }
        }
        if (methods.size != 0) {
            ret = DexKit.verifyTargetMethod(i, methods)
            if (ret == null) {
                Log.i(methods.size.toString() + " classes candidates found for " + i + ", none satisfactory.")
                ret = LegacyDexDeobfs.INSTANCE.doFindMethodImpl(i)
                return ret
            }
            Log.d("save id: $i,method: $ret")
            saveDescriptor(i, ret)
        }
        ret = LegacyDexDeobfs.INSTANCE.doFindMethodImpl(i)
        return ret
    }

    override fun getId() = "DexKit"

    override fun getName() = "DexKit(极速)"

}
