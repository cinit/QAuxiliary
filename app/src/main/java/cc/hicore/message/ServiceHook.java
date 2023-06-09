/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package cc.hicore.message;

import com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import io.github.qauxv.util.Initiator;

public class ServiceHook {
    public static IKernelMsgService kernel_service;
    public static IKernelMsgService get_kernel_service(){
        return kernel_service;
    }
    public static void requireHook(){
        if (kernel_service == null) {
            XposedHelpers.findAndHookConstructor("com.tencent.qqnt.kernel.api.impl.MsgService", Initiator.getHostClassLoader(),
                    com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService.class, "com.tencent.qqnt.kernel.api.impl.ServiceContent", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            kernel_service = (IKernelMsgService) param.args[0];
                        }
                    });

        }
    }

}
