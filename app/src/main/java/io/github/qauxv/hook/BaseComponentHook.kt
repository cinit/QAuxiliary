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

package io.github.qauxv.hook

import io.github.qauxv.base.ITraceableDynamicHook
import io.github.qauxv.base.RuntimeErrorTracer
import io.github.qauxv.step.Step
import io.github.qauxv.util.Log
import io.github.qauxv.util.SyncUtils

abstract class BaseComponentHook : ITraceableDynamicHook {

    private val mErrors: ArrayList<Throwable> = ArrayList()
    private var mInitialized = false
    private var mInitializeResult = false

    override val isInitialized: Boolean
        get() = mInitialized

    override val isInitializationSuccessful: Boolean
        get() = mInitializeResult

    override fun initialize(): Boolean {
        if (mInitialized) {
            return mInitializeResult
        }
        mInitializeResult = try {
            initOnce()
        } catch (e: Throwable) {
            traceError(e)
            if (e is Error && e !is AssertionError && e !is LinkageError) {
                // wtf Throwable
                throw e
            }
            false
        }
        mInitialized = true
        return mInitializeResult
    }

    @Throws(Exception::class)
    protected abstract fun initOnce(): Boolean

    override val runtimeErrors: List<Throwable> = mErrors

    override val targetProcesses = SyncUtils.PROC_MAIN

    override val isTargetProcess by lazy { SyncUtils.isTargetProcess(targetProcesses) }

    override val isAvailable = true

    override val isPreparationRequired = false

    override fun makePreparationSteps(): Array<Step>? = null

    override val isApplicationRestartRequired = false

    override val dependentComponents: List<ITraceableDynamicHook>? = null

    override var isEnabled: Boolean
        get() = false
        set(value) {
            // do nothing, because a component is lazy initialized on demand
        }

    @Synchronized
    override fun traceError(e: Throwable) {
        // check if there is already an error with the same error message and stack trace
        var alreadyLogged = false
        for (error in mErrors) {
            if (error.message == e.message && Log.getStackTraceString(error) == Log.getStackTraceString(e)) {
                alreadyLogged = true
            }
        }
        if (!alreadyLogged) {
            // limit the number of errors to 100
            if (mErrors.size >= 100) {
                mErrors.removeAt(50)
            }
            mErrors.add(e)
        }
        Log.e(e)
    }

    override val runtimeErrorDependentComponents: List<RuntimeErrorTracer>?
        get() = null
}
