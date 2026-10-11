/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
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

package cc.ioctl.fragment

import android.util.Log
import androidx.test.InstrumentationRegistry
import cc.ioctl.util.DebugTestRunner
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugTestFragmentInstrumentedTest {

    @Test
    fun runXposedHookTests() {
        assertSuccess(DebugTestRunner.runXposedHookTests())
    }

    @Test
    fun checkForConflictClassForHost() {
        val context = InstrumentationRegistry.getTargetContext().applicationContext
        assertSuccess(DebugTestRunner.checkForConflictClassForHost(context))
    }

    private fun assertSuccess(result: DebugTestRunner.RunResult) {
        result.output.lineSequence()
            .filter { it.isNotEmpty() }
            .forEach { Log.i(TAG, it) }
        assertTrue(result.output, result.success)
    }

    private companion object {
        private const val TAG = "QAuxv"
    }
}
