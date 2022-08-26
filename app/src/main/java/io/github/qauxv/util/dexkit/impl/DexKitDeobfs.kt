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
import io.github.qauxv.util.dexkit.DexMethodDescriptor
import me.teble.DexKitHelper
import java.util.Arrays
import java.util.concurrent.locks.Lock

class DexKitDeobfs private constructor(
    private val mReadLock: Lock,
    private var mDexKitHelper: DexKitHelper?
) : DexDeobfsBackend {

    override fun isBatchFindMethodSupported(): Boolean = true

    override fun doBatchFindMethodImpl(indexArray: IntArray): Array<DexMethodDescriptor?> {
        ensureOpen()
        mReadLock.lock()
        try {
            val helper = mDexKitHelper!!
            val resultArray = Array(indexArray.size) {
                DexKit.getMethodDescFromCache(indexArray[it])
            }
            val stringKeyToArrayIndexes = HashMap<String, HashSet<Int>>(4)
            for (index in resultArray.indices) {
                if (resultArray[index] == null) {
                    val id = indexArray[index]
                    val keys = DexKit.b(id)
                    keys.forEach { kbytes ->
                        val str = String(Arrays.copyOfRange(kbytes, 1, kbytes.size))
                        stringKeyToArrayIndexes.getOrPut(str) { HashSet(1) }.add(index)
                    }
                }
            }
            val stringMappedKeys = stringKeyToArrayIndexes.keys.toTypedArray()
            val designatorArray = Array(stringMappedKeys.size) {
                "noref_sid_$it"
            }
            // use designatorArray to map string keys to indexes
            for (i in stringMappedKeys.indices) {
                val key = stringMappedKeys[i]
                val requiringIndex = stringKeyToArrayIndexes[key]
                if (!requiringIndex.isNullOrEmpty()) {
                    designatorArray[i] = requiringIndex.first().toString() + "_sid_$i"
                }
            }
            val resultArrays = helper.batchFindMethodUsedString(designatorArray, stringMappedKeys)
            val shadowTmpCandidateMethodDescriptors = HashMap<Int, HashSet<String>>(indexArray.size)
            // fill candidate method descriptors
            for (key in stringKeyToArrayIndexes.keys) {
                val requiringIndex = stringKeyToArrayIndexes[key]!!
                val indexOfKey = stringMappedKeys.indexOf(key)
                requiringIndex.forEach {
                    shadowTmpCandidateMethodDescriptors.getOrPut(it) { HashSet(1) }.addAll(resultArrays[indexOfKey])
                }
            }
            // run user-defined filter for each index
            for (arrayIndex in resultArray.indices) {
                if (resultArray[arrayIndex] == null) {
                    val candidates = shadowTmpCandidateMethodDescriptors[arrayIndex]
                    if (candidates.isNullOrEmpty()) {
                        continue
                    }
                    val id = indexArray[arrayIndex]
                    val ret = DexKit.verifyTargetMethod(id, candidates.map { DexMethodDescriptor(it) }.toHashSet())
                    if (ret == null) {
                        Log.i(candidates.size.toString() + " candidates found for " + id + ", none satisfactory.")
                    } else {
                        Log.d("save id: $id,method: $ret")
                        saveDescriptor(id, ret)
                        resultArray[arrayIndex] = ret
                    }
                }
            }
            return resultArray
        } finally {
            mReadLock.unlock()
        }
    }

    override fun doFindMethodImpl(i: Int): DexMethodDescriptor? {
        ensureOpen()
        mReadLock.lock()
        try {
            var ret = DexKit.getMethodDescFromCache(i)
            if (ret != null) {
                return ret
            }
            ensureOpen()
            val helper = mDexKitHelper!!
            val keys = DexKit.b(i)
            val methods = HashSet<DexMethodDescriptor>()
            for (key in keys) {
                val str = String(Arrays.copyOfRange(key, 1, key.size))
                Log.d("DexKitDeobfs.doFindMethodImpl: id $i, key:$str")
                val descArray = helper.findMethodUsedString(str)
                descArray.forEach {
                    val desc = DexMethodDescriptor(it)
                    methods.add(desc)
                }
            }
            if (methods.size != 0) {
                ret = DexKit.verifyTargetMethod(i, methods)
                if (ret == null) {
                    Log.i(methods.size.toString() + " classes candidates found for " + i + ", none satisfactory.")
                    LegacyDexDeobfs.newInstance().use { legacy ->
                        ret = legacy.doFindMethodImpl(i)
                        return ret
                    }
                }
                Log.d("save id: $i,method: $ret")
                saveDescriptor(i, ret)

            }
            LegacyDexDeobfs.newInstance().use { legacy ->
                ret = legacy.doFindMethodImpl(i)
                return ret
            }
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
                    res.close()
                    Log.d("close resource: DexKit")
                }
            }
        }
    }

    override fun getId() = ID

    override fun getName() = NAME

}
