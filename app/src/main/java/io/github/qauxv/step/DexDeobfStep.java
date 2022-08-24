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
package io.github.qauxv.step;

import io.github.qauxv.util.dexkit.DexKit;

public class DexDeobfStep implements Step {

    private final int id;

    public DexDeobfStep(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean step() {
        return DexKit.prepareFor(id);
    }

    @Override
    public boolean isDone() {
        return !DexKit.isRunDexDeobfuscationRequired(id);
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public String getDescription() {
        if (id / 10000 == 0) {
            return "定位被混淆类: " + DexKit.c(id);
        } else {
            return "定位被混淆方法: " + DexKit.c(id);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DexDeobfStep that = (DexDeobfStep) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
