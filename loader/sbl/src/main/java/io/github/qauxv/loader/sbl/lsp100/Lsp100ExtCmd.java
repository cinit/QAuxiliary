/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
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

package io.github.qauxv.loader.sbl.lsp100;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.libxposed.api.XposedInterface;
import io.github.qauxv.loader.sbl.common.CheckUtils;
import io.github.qauxv.loader.sbl.common.ModuleLoader;
import io.github.qauxv.loader.sbl.lsp100.codegen.Lsp100ProxyClassMaker;
import java.lang.reflect.Method;

public class Lsp100ExtCmd {

    private Lsp100ExtCmd() {
    }

    public static Object handleQueryExtension(@NonNull String cmd, @Nullable Object[] arg) {
        CheckUtils.checkNonNull(cmd, "cmd");
        switch (cmd) {
            case "GetXposedInterfaceClass":
                return XposedInterface.class;
            case "GetLoadPackageParam":
                return null;
            case "GetInitZygoteStartupParam":
                return null;
            case "GetInitErrors":
                return ModuleLoader.getInitErrors();
            case "SetLibXposedNewApiByteCodeGeneratorWrapper": {
                Lsp100ProxyClassMaker.setWrapperMethod((Method) arg[0]);
                return Boolean.TRUE;
            }
            default:
                return null;
        }
    }

}
