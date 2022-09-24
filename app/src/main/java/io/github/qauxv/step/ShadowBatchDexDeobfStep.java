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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.qauxv.util.dexkit.DexDeobfsBackend;
import io.github.qauxv.util.dexkit.DexKitTarget;
import java.util.Objects;

public class ShadowBatchDexDeobfStep implements Step {

    private final DexKitTarget[] targets;
    private final DexDeobfsBackend backend;

    public ShadowBatchDexDeobfStep(DexDeobfsBackend backend, @NonNull DexKitTarget[] indexes) {
        this.backend = Objects.requireNonNull(backend);
        this.targets = Objects.requireNonNull(indexes);
        if (!backend.isBatchFindMethodSupported()) {
            throw new IllegalArgumentException(backend.getClass().getName());
        }
    }

    @Override
    public boolean step() {
        backend.doBatchFindMethodImpl(targets);
        // we actually do not care the result
        return true;
    }

    @Override
    public boolean isDone() {
        return targets.length == 0;
    }

    @Override
    public int getPriority() {
        return 101;
    }

    @Nullable
    @Override
    public String getDescription() {
        return "预处理混淆";
    }
}
