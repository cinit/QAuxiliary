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

    override fun doBatchFindMethodImpl(indexArray: IntArray) {
        ensureOpen()
        mReadLock.lock()
        try {
            val helper = mDexKitHelper!!
            val methodDescriptorArray = Array(indexArray.size) {
                DexKit.getMethodDescFromCache(indexArray[it])
            }
            val deobfsMap = mutableMapOf<String, Set<String>>()
            for (index in methodDescriptorArray.indices) {
                if (methodDescriptorArray[index] == null) {
                    val id = indexArray[index]
                    val keys = DexKit.b(id).map {
                        String(Arrays.copyOfRange(it, 1, it.size))
                    }
                    keys.forEachIndexed { idx, key ->
                        // 可能存在不同版本的关键词，所以需要区分开来
                        deobfsMap["${id}_${idx}"] = setOf(key)
                    }
                }
            }

            val resultMap = helper.batchFindMethodsUsedStrings(deobfsMap, true)

            resultMap.forEach { (key, valueArr) ->
                val id = key.split("_").first().toInt()
                val ret = DexKit.verifyTargetMethod(id, valueArr.map { DexMethodDescriptor(it) }.toHashSet())
                if (ret == null) {
                    Log.i("${valueArr.size} candidates found for " + id + ", none satisfactory.")
                } else {
                    Log.d("save id: $id,method: $ret")
                    saveDescriptor(id, ret)
                }
            }
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

    fun doFindMethodUsedField(
        fieldDescriptor: String,
        fieldDeclareClass: String,
        fieldName: String,
        fieldType: String,
        beUsedFlag: Int,
        callerMethodDeclareClass: String,
        callerMethodName: String,
        callerMethodReturnType: String,
        callerMethodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = intArrayOf(),
    ): Array<String> {
        return mDexKitHelper!!.findFieldBeUsed(
            fieldDescriptor,
            fieldDeclareClass,
            fieldName,
            fieldType,
            beUsedFlag,
            callerMethodDeclareClass,
            callerMethodName,
            callerMethodReturnType,
            callerMethodParamTypes,
            dexPriority,
        )
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

    override fun getId() = ID

    override fun getName() = NAME

}
