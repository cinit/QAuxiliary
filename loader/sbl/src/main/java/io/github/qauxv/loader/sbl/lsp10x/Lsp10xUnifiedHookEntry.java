/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package io.github.qauxv.loader.sbl.lsp10x;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.annotations.XposedApiExact;
import io.github.libxposed.api.annotations.XposedApiMin;
import io.github.qauxv.loader.sbl.lsp100.Lsp100HookEntry;
import io.github.qauxv.loader.sbl.lsp101.Lsp101HookEntry;

/**
 * The unified entry point for libxpsoed API 100 and 101 (typically LSPosed).
 * <p>
 * Keep this class as simple as possible, and do not add any code that may cause NoClassDefFoundError when running on API 100 or 101.
 * <p>
 * Any fields appear here should be carefully reviewed to ensure they are both API 100 and 101 compatible.
 */
@Keep
public class Lsp10xUnifiedHookEntry extends XposedModule {

    private final Lsp10xHookEntryHandler mHandler;

    /* --- start of API 100 --- */

    @XposedApiExact(100)
    public Lsp10xUnifiedHookEntry(@NonNull XposedInterface base, @NonNull ModuleLoadedParam param) {
        super(base, param);
        // This is the early initialization constructor for API 100,
        // which is equivalent to the old onInitZygote method.
        mHandler = new Lsp100HookEntry(this, param);
    }

    /* --- end of API 100 --- */

    @Override
    public void onPackageLoaded(@NonNull XposedModule.PackageLoadedParam param) {
        mHandler.onPackageLoaded(param);
    }

    /* --- start of API 101 --- */

    @RequiresApi(26)
    @XposedApiMin(101)
    public Lsp10xUnifiedHookEntry() {
        super();
        // The libxposed spec says module should not perform any initialization before onModuleLoaded is called.
        mHandler = new Lsp101HookEntry(this);
    }

    @XposedApiMin(101)
    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        ((Lsp101HookEntry) mHandler).onModuleLoaded(param);
    }

    @XposedApiMin(101)
    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        ((Lsp101HookEntry) mHandler).onPackageReady(param);
    }

    /* --- end of API 101 --- */

}
