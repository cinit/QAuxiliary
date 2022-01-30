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
package io.github.qauxv.step;

import androidx.annotation.Nullable;

public interface Step extends Comparable<Step> {

    /**
     * Execute this step, de-obfuscate the dex file. This method takes a long time, so it should not be called in the
     * main thread.
     *
     * @return true if the step is done successfully, false otherwise
     */
    boolean step();

    boolean isDone();

    int getPriority();

    @Nullable
    String getDescription();

    @Override
    default int compareTo(Step o) {
        return this.getPriority() - o.getPriority();
    }
}
