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
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.DexKitTargetSealedEnum;

public class DexDeobfStep implements Step {

    private final DexKitTarget target;

    public DexDeobfStep(String id) {
        this.target = DexKitTargetSealedEnum.INSTANCE.valueOf(id);
    }

    public DexDeobfStep(DexKitTarget target) {
        this.target = target;
    }

    public String getId() {
        return DexKitTargetSealedEnum.INSTANCE.nameOf(target);
    }

    @Override
    public boolean step() {
        try {
            if (target instanceof DexKitTarget) {
                var t = (DexKitTarget) target;
                if (t.getFindMethod()) {
                    DexKit.doFindMethod(t);
                } else {
                    DexKit.doFindClass(t);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isDone() {
        return !DexKit.isRunDexDeobfuscationRequired(target);
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public String getDescription() {
        if (target instanceof DexKitTarget) {
            var t = (DexKitTarget) target;
            if (t.getFindMethod()) {
                return "定位被混淆方法: " + getId();
            } else {
                return "定位被混淆类: " + getId();
            }
        }
        return "Unsupported Type";
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
        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
