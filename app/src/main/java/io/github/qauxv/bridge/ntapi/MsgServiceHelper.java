/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package io.github.qauxv.bridge.ntapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.Reflex;
import com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService;
import io.github.qauxv.bridge.kernelcompat.KernelMsgServiceCompat;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Method;
import mqq.app.AppRuntime;
import mqq.app.api.IRuntimeService;

public class MsgServiceHelper {

    private MsgServiceHelper() {
    }

    @NonNull
    public static Object getMsgService(@NonNull AppRuntime app) throws ReflectiveOperationException, LinkageError {
        // IMsgService msgService = ((IKernelService) app.getRuntimeService(IKernelService.class, "")).getMsgService();
        Class<? extends IRuntimeService> kIKernelService = (Class<? extends IRuntimeService>) Initiator.loadClass("com.tencent.qqnt.kernel.api.IKernelService");
        IRuntimeService kernelService = app.getRuntimeService(kIKernelService, "");
        Method getMsgService = kernelService.getClass().getMethod("getMsgService");
        return getMsgService.invoke(kernelService);
    }

    @Nullable
    public static IKernelMsgService getKernelMsgServiceRaw(@NonNull AppRuntime app) throws ReflectiveOperationException, LinkageError {
        Object msgService = getMsgService(app);
        IKernelMsgService service;
        try {
            // 8.9.78起
            service = (IKernelMsgService) msgService.getClass().getMethod("getService").invoke(msgService);
        } catch (Exception unused) {
            Method getKMsgSvc = Reflex.findSingleMethod(msgService.getClass(), IKernelMsgService.class, false);
            service = (IKernelMsgService) getKMsgSvc.invoke(msgService);
        }
        return service;
    }

    @Nullable
    public static KernelMsgServiceCompat getKernelMsgService(@NonNull AppRuntime app) throws ReflectiveOperationException, LinkageError {
        IKernelMsgService service = getKernelMsgServiceRaw(app);
        if (service != null) {
            return new KernelMsgServiceCompat(service);
        } else {
            return null;
        }
    }

}
