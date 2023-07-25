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

package io.github.qauxv.util.dexkit

import io.github.qauxv.util.dexkit.impl.DexKitDeobfs
import java.util.concurrent.atomic.AtomicInteger

object DexDeobfsProvider {

    private val mDeobfsSection = AtomicInteger(0)

    fun enterDeobfsSection() {
        mDeobfsSection.incrementAndGet()
    }

    fun exitDeobfsSection() {
        mDeobfsSection.decrementAndGet()
    }

    @JvmStatic
    fun checkDeobfuscationAvailable() {
        check(mDeobfsSection.get() > 0) { "dex deobfuscation is not meant to be available now" }
    }

    /**
     * Create a new instance. Call [DexDeobfsBackend.close] when you are done.
     */
    fun getCurrentBackend(): DexKitDeobfs {
        checkDeobfuscationAvailable()
        return DexKitDeobfs.newInstance()
    }

}
