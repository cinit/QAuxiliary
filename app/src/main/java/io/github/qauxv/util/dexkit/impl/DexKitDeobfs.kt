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

package io.github.qauxv.util.dexkit.impl

import cc.ioctl.util.findDexClassLoader
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log
import io.github.qauxv.util.dexkit.DexDeobfsBackend
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.DexKitTarget
import io.github.qauxv.util.dexkit.DexMethodDescriptor
import io.github.qauxv.util.dexkit.name
import io.github.qauxv.util.dexkit.valueOf
import me.teble.DexKitHelper
import java.util.concurrent.locks.Lock

class DexKitDeobfs private constructor(
    private val mReadLock: Lock,
    private var mDexKitHelper: DexKitHelper?
) : DexDeobfsBackend {

    override val id: String = ID
    override val name: String = NAME
    override val isBatchFindMethodSupported: Boolean = true

    fun getDexKitHelper(): DexKitHelper {
        return mDexKitHelper!!
    }

    override fun doBatchFindMethodImpl(targetArray: Array<DexKitTarget>) {
        ensureOpen()
        mReadLock.lock()
        try {
            val helper = mDexKitHelper!!
            val targets = targetArray.filterIsInstance<DexKitTarget.UsingStr>()
            val methodDescriptorArray = Array(targets.size) {
                DexKit.getMethodDescFromCache(targets[it])
            }
            val deobfsMap = mutableMapOf<String, Set<String>>()
            for (index in methodDescriptorArray.indices) {
                if (methodDescriptorArray[index] == null) {
                    val target = targets[index]
                    val keys = target.traitString
                    keys.forEachIndexed { idx, key ->
                        // 可能存在不同版本的关键词，所以需要区分开来
                        deobfsMap["${target.name}#_#${idx}"] = setOf(key)
                    }
                }
            }

            val resultMap = helper.batchFindMethodsUsedStrings(deobfsMap, true)

            resultMap.forEach { (key, valueArr) ->
                val name = key.split("#_#").first()
                val target = DexKitTarget.valueOf(name)
                val ret = target.verifyTargetMethod(valueArr.map { DexMethodDescriptor(it) })
                if (ret == null) {
                    valueArr.forEach(Log::i)
                    Log.i("${valueArr.size} candidates found for " + name + ", none satisfactory.")
                } else {
                    Log.d("save id: $name,method: $ret")
                    target.descCache = ret.toString()
                }
            }
        } finally {
            mReadLock.unlock()
        }
    }

    override fun doFindMethodImpl(target: DexKitTarget): DexMethodDescriptor? {
        if (target !is DexKitTarget.UsingStr) return null
        ensureOpen()
        mReadLock.lock()
        try {
            var ret = DexKit.getMethodDescFromCache(target)
            if (ret != null) {
                return ret
            }
            ensureOpen()
            val helper = mDexKitHelper!!
            val keys = target.traitString
            val methods = keys.map { key ->
                helper.findMethodUsedString(key, true)
            }.flatMap { desc ->
                desc.map { DexMethodDescriptor(it) }
            }
            if (methods.isNotEmpty()) {
                ret = target.verifyTargetMethod(methods)
                if (ret == null) {
                    Log.i("${methods.size} methods found for ${target.name}, none satisfactory, save null.")
                    ret = DexKit.NO_SUCH_METHOD
                }
                Log.d("save id: ${target.name},method: $ret")
                target.descCache = ret.toString()
            }
            return ret
        } finally {
            mReadLock.unlock()
        }
    }

    @Synchronized
    private fun ensureOpen() {
        check(mDexKitHelper != null) { "closed" }
    }

    @Synchronized
    override fun close() {
        mSharedResourceImpl.decreaseRefCount()
        mDexKitHelper = null
    }

    companion object {

        const val ID = "DexKit"
        const val NAME = "DexKit(默认, 最快)"

        @JvmStatic
        fun newInstance(): DexKitDeobfs {
            val lock = mSharedResourceImpl.increaseRefCount()
            return DexKitDeobfs(lock.readLock(), mSharedResourceImpl.resources!!)
        }

        private val mSharedResourceImpl by lazy {
            object : SharedRefCountResourceImpl<DexKitHelper>() {
                override fun openResourceInternal(): DexKitHelper {
                    Log.d("open resource: DexKit")
                    val dexClassLoader: ClassLoader = Initiator.getHostClassLoader().findDexClassLoader()!!
                    return DexKitHelper(dexClassLoader)
                }

                override fun closeResourceInternal(res: DexKitHelper) {
                    res.release()
                    Log.d("close resource: DexKit")
                }
            }
        }
    }
}
