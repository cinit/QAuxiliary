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

package io.github.qauxv.base

interface RuntimeErrorTracer {

    val runtimeErrors: List<Throwable>

    val hasRuntimeErrors: Boolean
        get() {
            val deps = runtimeErrorDependentComponents
            if (deps.isNullOrEmpty()) {
                return runtimeErrors.isNotEmpty()
            }
            return runtimeErrors.isNotEmpty() || deps.any { it.hasRuntimeErrors }
        }

    val runtimeErrorDependentComponents: List<RuntimeErrorTracer>?

    fun traceError(e: Throwable)

    companion object {
        @JvmField
        val EMPTY_ARRAY = arrayOf<RuntimeErrorTracer>()
    }

}
