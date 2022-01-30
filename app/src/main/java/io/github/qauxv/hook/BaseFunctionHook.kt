/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
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

import io.github.qauxv.SyncUtils
import io.github.qauxv.base.IDynamicHook
import io.github.qauxv.base.IUiItemAgentProvider
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.util.Log
import java.util.*

abstract class BaseFunctionHook(
    hookKey: String? = null,
    defaultEnabled: Boolean = true
) : IDynamicHook, IUiItemAgentProvider {

    private val mErrors: ArrayList<Throwable> = ArrayList()
    private var mInitialized = false
    private var mInitializeResult = false
    private val mHookKey: String = hookKey ?: this::class.java.name
    private val mDefaultEnabled: Boolean = defaultEnabled

    override val isInitialized: Boolean
        get() = mInitialized

    override fun initialize(): Boolean {
        if (mInitialized) {
            return mInitializeResult
        }
        mInitializeResult = try {
            initOnce()
        } catch (e: Throwable) {
            traceError(e)
            if (e !is Exception && e !is LinkageError && e !is AssertionError) {
                // wtf Throwable
                throw e
            }
            false
        }
        mInitialized = true
        return mInitializeResult
    }

    @Throws(Exception::class)
    abstract fun initOnce(): Boolean

    override val runtimeErrors = mErrors

    override val targetProcesses = SyncUtils.PROC_MAIN

    override val isTargetProcess by lazy { SyncUtils.isTargetProcess(targetProcesses) }

    override val isAvailable = true

    override val isPreparationRequired = false

    override fun makePreparations() = true

    override val isApplicationRestartRequired = false

    override var isEnabled: Boolean
        get() = ConfigManager.getDefaultConfig().getBooleanOrDefault("$mHookKey.enabled", mDefaultEnabled)
        set(value) {
            ConfigManager.getDefaultConfig().putBoolean("$mHookKey.enabled", value)
        }

    open fun traceError(e: Throwable) {
        // check if there is already an error with the same error message and stack trace
        var alreadyLogged = false
        for (error in mErrors) {
            if (error.message == e.message && Arrays.equals(error.stackTrace, e.stackTrace)) {
                alreadyLogged = true
            }
        }
        if (!alreadyLogged) {
            mErrors.add(e)
        }
        Log.e(e)
    }
}
