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

package me.teble

class DexKitHelper(
    classLoader: ClassLoader
) {
    companion object {
        const val FLAG_GETTING = 1
        const val FLAG_SETTING = 2
        const val FLAG_USING = FLAG_GETTING or FLAG_SETTING
    }

    /**
     * 使用完成后切记记得调用 [release]，否则内存不会释放
     */
    private var token: Long = 0

    init {
        token = initDexKit(classLoader)
    }

    fun release() {
        release(token)
    }

    fun batchFindClassesUsedStrings(
        map: Map<String, Set<String>>,
        advancedMatch: Boolean = true,
        dexPriority: IntArray? = intArrayOf(),
    ): Map<String, Array<String>> {
        return batchFindClassesUsedStrings(token, map, advancedMatch, dexPriority)
    }

    fun batchFindMethodsUsedStrings(
        map: Map<String, Set<String>>,
        advancedMatch: Boolean = true,
        dexPriority: IntArray? = intArrayOf(),
    ): Map<String, Array<String>> {
        return batchFindMethodsUsedStrings(token, map, advancedMatch, dexPriority)
    }

    fun findMethodBeInvoked(
        methodDescriptor: String,
        methodDeclareClass: String,
        methodName: String,
        methodReturnType: String,
        methodParamTypes: Array<String>? = null,
        callerMethodDeclareClass: String,
        callerMethodName: String,
        callerMethodReturnType: String,
        callerMethodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = intArrayOf(),
    ): Array<String> {
        return findMethodBeInvoked(
            token,
            methodDescriptor,
            methodDeclareClass,
            methodName,
            methodReturnType,
            methodParamTypes,
            callerMethodDeclareClass,
            callerMethodName,
            callerMethodReturnType,
            callerMethodParamTypes,
            dexPriority
        )
    }

    fun findMethodInvoking(
        methodDescriptor: String,
        methodDeclareClass: String,
        methodName: String,
        methodReturnType: String,
        methodParamTypes: Array<String>? = null,
        beCalledMethodDeclareClass: String,
        beCalledMethodName: String,
        beCalledMethodReturnType: String,
        beCalledMethodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = intArrayOf(),
    ): Map<String, Array<String>> {
        return findMethodInvoking(
            token,
            methodDescriptor,
            methodDeclareClass,
            methodName,
            methodReturnType,
            methodParamTypes,
            beCalledMethodDeclareClass,
            beCalledMethodName,
            beCalledMethodReturnType,
            beCalledMethodParamTypes,
            dexPriority
        )
    }

    fun findFieldBeUsed(
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
        return findFieldBeUsed(
            token,
            fieldDescriptor,
            fieldDeclareClass,
            fieldName,
            fieldType,
            beUsedFlag,
            callerMethodDeclareClass,
            callerMethodName,
            callerMethodReturnType,
            callerMethodParamTypes,
            dexPriority
        )
    }

    fun findMethodUsedString(
        usedString: String,
        advancedMatch: Boolean = true,
        methodDeclareClass: String = "",
        methodName: String = "",
        methodReturnType: String = "",
        methodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = intArrayOf(),
    ): Array<String> {
        return findMethodUsedString(
            token,
            usedString,
            advancedMatch,
            methodDeclareClass,
            methodName,
            methodReturnType,
            methodParamTypes,
            dexPriority
        )
    }

    fun findMethod(
        methodDeclareClass: String,
        methodName: String,
        methodReturnType: String,
        methodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = intArrayOf(),
    ): Array<String> {
        return findMethod(
            token,
            methodDeclareClass,
            methodName,
            methodReturnType,
            methodParamTypes,
            dexPriority
        )
    }

    fun findSubClasses(
        parentClass: String,
        dexPriority: IntArray? = intArrayOf(),
    ): Array<String> {
        return findSubClasses(token, parentClass, dexPriority)
    }

    fun findMethodOpPrefixSeq(
        opPrefixSeq: IntArray,
        methodDeclareClass: String,
        methodName: String,
        methodReturnType: String,
        methodParamTypes: Array<String>? = null,
        dexPriority: IntArray? = intArrayOf(),
    ): Array<String> {
        return findMethodOpPrefixSeq(
            token,
            opPrefixSeq,
            methodDeclareClass,
            methodName,
            methodReturnType,
            methodParamTypes,
            dexPriority
        )
    }

    private external fun initDexKit(classLoader: ClassLoader): Long

    private external fun release(token: Long)

    private external fun batchFindClassesUsedStrings(
        token: Long,
        map: Map<String, Set<String>>,
        advancedMatch: Boolean,
        dexPriority: IntArray?,
    ): Map<String, Array<String>>

    private external fun batchFindMethodsUsedStrings(
        token: Long,
        map: Map<String, Set<String>>,
        advancedMatch: Boolean,
        dexPriority: IntArray?,
    ): Map<String, Array<String>>

    private external fun findMethodBeInvoked(
        token: Long,
        methodDescriptor: String,
        methodDeclareClass: String,
        methodName: String,
        methodReturnType: String,
        methodParamTypes: Array<String>?,
        callerMethodDeclareClass: String,
        callerMethodName: String,
        callerMethodReturnType: String,
        callerMethodParamTypes: Array<String>?,
        dexPriority: IntArray?,
    ): Array<String>

    private external fun findMethodInvoking(
        token: Long,
        methodDescriptor: String,
        methodDeclareClass: String,
        methodName: String,
        methodReturnType: String,
        methodParamTypes: Array<String>?,
        beCalledMethodDeclareClass: String,
        beCalledMethodName: String,
        beCalledMethodReturnType: String,
        beCalledMethodParamTypes: Array<String>?,
        dexPriority: IntArray?,
    ): Map<String, Array<String>>

    private external fun findFieldBeUsed(
        token: Long,
        fieldDescriptor: String,
        fieldDeclareClass: String,
        fieldName: String,
        fieldType: String,
        beUsedFlag: Int,
        callerMethodDeclareClass: String,
        callerMethodName: String,
        callerMethodReturnType: String,
        callerMethodParamTypes: Array<String>?,
        dexPriority: IntArray?,
    ): Array<String>

    private external fun findMethodUsedString(
        token: Long,
        usedString: String,
        advancedMatch: Boolean,
        methodDeclareClass: String,
        methodName: String,
        methodReturnType: String,
        methodParamTypes: Array<String>?,
        dexPriority: IntArray?,
    ): Array<String>

    private external fun findMethod(
        token: Long,
        methodDeclareClass: String,
        methodName: String,
        methodReturnType: String,
        methodParamTypes: Array<String>?,
        dexPriority: IntArray?,
    ): Array<String>

    private external fun findSubClasses(
        token: Long,
        parentClass: String,
        dexPriority: IntArray?,
    ): Array<String>

    private external fun findMethodOpPrefixSeq(
        token: Long,
        opPrefixSeq: IntArray,
        methodDeclareClass: String,
        methodName: String,
        methodReturnType: String,
        methodParamTypes: Array<String>?,
        dexPriority: IntArray?,
    ): Array<String>
}
