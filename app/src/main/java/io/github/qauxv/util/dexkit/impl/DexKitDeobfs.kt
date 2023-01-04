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
import io.github.qauxv.util.hostInfo
import io.luckypray.dexkit.DexKitBridge
import io.luckypray.dexkit.builder.BatchFindArgs
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor as MethodDescriptor
import java.util.concurrent.locks.Lock

class DexKitDeobfs private constructor(
    private val mReadLock: Lock,
    private var mDexKitBridge: DexKitBridge?
) : DexDeobfsBackend {

    override val id: String = ID
    override val name: String = NAME
    override val isBatchFindMethodSupported: Boolean = true

    fun getDexKitBridge(): DexKitBridge {
        return mDexKitBridge!!
    }

    override fun doBatchFindMethodImpl(targetArray: Array<DexKitTarget>) {
        ensureOpen()
        mReadLock.lock()
        try {
            val helper = mDexKitBridge!!
            val targets = targetArray.filterIsInstance<DexKitTarget.UsingStr>()
            val methodDescriptorArray = Array(targets.size) {
                DexKit.getMethodDescFromCacheImpl(targets[it])
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

            val resultMap = helper.batchFindMethodsUsingStrings(BatchFindArgs.build {
                queryMap = deobfsMap
            })
            val resultMap2 = mutableMapOf<String, Set<MethodDescriptor>>()
            resultMap.forEach {
                val key = it.key.split("#").first()
                if (resultMap2.containsKey(key)) {
                    resultMap2[key] = resultMap2[key]!! + it.value
                } else {
                    resultMap2[key] = it.value.toSet()
                }
            }

            resultMap2.forEach { (key, valueArr) ->
                val target = DexKitTarget.valueOf(key)
                val ret = target.verifyTargetMethod(valueArr.map { DexMethodDescriptor(it.descriptor) })
                if (ret == null) {
                    valueArr.map { it.descriptor }.forEach(Log::i)
                    Log.e("${valueArr.size} candidates found for " + key + ", none satisfactory, save null.")
                    target.descCache = DexKit.NO_SUCH_METHOD.toString()
                } else {
                    Log.d("save id: $key,method: $ret")
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
            var ret = DexKit.getMethodDescFromCacheImpl(target)
            if (ret != null) {
                return ret
            }
            ensureOpen()
            val helper = mDexKitBridge!!
            val keys = target.traitString
            val methods = keys.map { key ->
                helper.findMethodUsingString {
                    usingString = key
                }
            }.flatMap { desc ->
                desc.map { DexMethodDescriptor(it.descriptor) }
            }
            if (methods.isNotEmpty()) {
                ret = target.verifyTargetMethod(methods)
                if (ret == null) {
                    Log.e("${methods.size} methods found for ${target.name}, none satisfactory, save null.")
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
        check(mDexKitBridge != null) { "closed" }
    }

    @Synchronized
    override fun close() {
        mSharedResourceImpl.decreaseRefCount()
        mDexKitBridge = null
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
            object : SharedRefCountResourceImpl<DexKitBridge>() {
                override fun openResourceInternal(): DexKitBridge {
                    Log.d("open resource: DexKit")
                    return DexKitBridge.create(hostInfo.application.applicationInfo.sourceDir)!!
                }

                override fun closeResourceInternal(res: DexKitBridge) {
                    res.close()
                    Log.d("close resource: DexKit")
                }
            }
        }
    }
}
