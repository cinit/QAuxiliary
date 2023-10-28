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

import io.github.qauxv.util.Log
import io.github.qauxv.util.dexkit.DexDeobfsBackend
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.DexKitTarget
import io.github.qauxv.util.dexkit.DexMethodDescriptor
import io.github.qauxv.util.dexkit.name
import io.github.qauxv.util.dexkit.valueOf
import io.github.qauxv.util.hostInfo
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.MethodData
import java.util.concurrent.locks.Lock
import kotlin.concurrent.withLock

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
        data class TargetHolder(
            val target: DexKitTarget,
            val traitStringVectors: Array<Set<String>>
        )

        ensureOpen()
        mReadLock.withLock {
            val helper = mDexKitBridge!!
            val targets: MutableList<TargetHolder> = ArrayList()
            targetArray.forEach { target ->
                if (DexKit.getMethodDescFromCacheImpl(target) == null) {
                    if (target is DexKitTarget.UsingStringVector) {
                        // v2, Array<Array<String>> -> Array<Set<String>>
                        targets += TargetHolder(target, target.traitStringVectors.map { it.toSet() }.toTypedArray())
                    } else if (target is DexKitTarget.UsingStr) {
                        // v1, Array<String> -> Array<setOf<String>>
                        targets += TargetHolder(target, target.traitString.map { setOf(it) }.toTypedArray())
                    }
                }
            }
            val deobfsMap = mutableMapOf<String, Set<String>>()
            for (index in targets.indices) {
                val target = targets[index]
                val keys = target.traitStringVectors
                keys.forEachIndexed { idx, key ->
                    // 可能存在不同版本的关键词，所以需要区分开来
                    deobfsMap["${target.target.name}#_#${idx}"] = key
                }
            }

            val resultMap = helper.batchFindMethodUsingStrings {
                groups(deobfsMap, StringMatchType.SimilarRegex)
            }
            val resultMap2 = mutableMapOf<String, Set<MethodData>>()
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
        }
    }

    override fun doFindMethodImpl(target: DexKitTarget): DexMethodDescriptor? {
        if (target !is DexKitTarget.UsingStr && target !is DexKitTarget.UsingStringVector) {
            return null
        }
        ensureOpen()
        mReadLock.withLock {
            val cached = DexKit.getMethodDescFromCacheImpl(target)
            if (cached != null) {
                return if (DexKit.NO_SUCH_METHOD.toString() == cached.toString()) null else cached
            }
            val helper = mDexKitBridge!!
            val keys: Array<Set<String>> = if (target is DexKitTarget.UsingStringVector) {
                target.traitStringVectors.map { it.toSet() }.toTypedArray()
            } else if (target is DexKitTarget.UsingStr) {
                target.traitString.map { setOf(it) }.toTypedArray()
            } else {
                return null
            }
            val resultMap = helper.batchFindMethodUsingStrings {
                val map = keys.mapIndexed { index, set -> "${target.name}#_#${index}" to set }.toMap()
                groups(map, StringMatchType.SimilarRegex)
            }
            if(resultMap.isEmpty()){
                Log.e("no result found for ${target.name}")
                target.descCache = DexKit.NO_SUCH_METHOD.toString()
                return null
            }
            val resultSet = resultMap.values.map { it as List<MethodData> }.reduce { acc, set -> acc + set }
            // verify
            val ret = target.verifyTargetMethod(resultSet.map { DexMethodDescriptor(it.descriptor) })
            if (ret == null) {
                resultSet.map { it.descriptor }.forEach(Log::i)
                Log.e("${resultSet.size} candidates found for " + target.name + ", none satisfactory, save null.")
                target.descCache = DexKit.NO_SUCH_METHOD.toString()
                return null
            } else {
                Log.d("save id: ${target.name},method: $ret")
                target.descCache = ret.toString()
                return ret
            }
        }
    }

    @Synchronized
    private fun ensureOpen() {
        check(mDexKitBridge != null) { "closed" }
    }

    @Synchronized
    override fun close() {
        if (mDexKitBridge != null) {
            mSharedResourceImpl.decreaseRefCount()
            mDexKitBridge = null
        }
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
