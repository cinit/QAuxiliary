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

package io.github.qauxv.bridge;

import static io.github.qauxv.util.Initiator.load;

import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.QQVersion;
import java.lang.reflect.Method;
import mqq.app.AppRuntime;

public class ManagerHelper {

    private ManagerHelper() {
    }

    public static Object getTroopManager() throws Exception {
        int troopMgrId = -1;
        Class<?> cl_QQManagerFactory = load("com.tencent.mobileqq.app.QQManagerFactory");
        try {
            if (cl_QQManagerFactory != null) {
                troopMgrId = (int) cl_QQManagerFactory.getField("TROOP_MANAGER").get(null);
            }
        } catch (Throwable e) {
            Log.e(e);
        }
        if (troopMgrId != -1) {
            // >=8.4.10
            return getManager(troopMgrId);
        } else {
            // 8.4.8 or earlier
            Object mTroopManager = getManager(51);
            if (!mTroopManager.getClass().getName().contains("TroopManager")) {
                mTroopManager = getManager(52);
            }
            return mTroopManager;
        }
    }

    public static Object getQQMessageFacade() throws ReflectiveOperationException {
        AppRuntime app = AppRuntimeHelper.getQQAppInterface();
        return Reflex.invokeVirtualAny(app, Initiator._QQMessageFacade());
    }

    public static Object getManager(int index) throws ReflectiveOperationException {
        return Reflex.invokeVirtual(AppRuntimeHelper.getQQAppInterface(), "getManager", index, int.class);
    }

    public static Object getFriendListHandler() {
        if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_5_0)) {
            try {
                Class cl_bh = load("com/tencent/mobileqq/app/BusinessHandler");
                Class cl_flh = load("com/tencent/mobileqq/app/FriendListHandler");
                if (cl_bh == null) {
                    assert cl_flh != null;
                    cl_bh = cl_flh.getSuperclass();
                }
                Object appInterface = AppRuntimeHelper.getQQAppInterface();
                return Reflex.invokeVirtual(appInterface, "getBusinessHandler", cl_flh.getName(),
                    String.class, cl_bh);
            } catch (Exception e) {
                Log.e(e);
                return null;
            }
        } else {
            try {
                Class cl_bh = load("com/tencent/mobileqq/app/BusinessHandler");
                if (cl_bh == null) {
                    Class cl_flh = load("com/tencent/mobileqq/app/FriendListHandler");
                    assert cl_flh != null;
                    cl_bh = cl_flh.getSuperclass();
                }
                Object appInterface = AppRuntimeHelper.getQQAppInterface();
                try {
                    return Reflex.invokeVirtual(appInterface, "a", 1, int.class, cl_bh);
                } catch (NoSuchMethodException e) {
                    try {
                        Method m = appInterface.getClass()
                            .getMethod("getBusinessHandler", int.class);
                        m.setAccessible(true);
                        return m.invoke(appInterface, 1);
                    } catch (Exception e2) {
                        e.addSuppressed(e2);
                    }
                    throw e;
                }
            } catch (Exception e) {
                Log.e(e);
                return null;
            }
        }
    }

    @Deprecated
    public static Object getBusinessHandler(int type) {
        try {
            Class cl_bh = load("com/tencent/mobileqq/app/BusinessHandler");
            if (cl_bh == null) {
                Class cl_flh = load("com/tencent/mobileqq/app/FriendListHandler");
                assert cl_flh != null;
                cl_bh = cl_flh.getSuperclass();
            }
            Object appInterface = AppRuntimeHelper.getQQAppInterface();
            try {
                return Reflex.invokeVirtual(appInterface, "a", type, int.class, cl_bh);
            } catch (NoSuchMethodException e) {
                Method m = appInterface.getClass().getMethod("getBusinessHandler", int.class);
                m.setAccessible(true);
                return m.invoke(appInterface, type);
            }
        } catch (Exception e) {
            Log.e(e);
            return null;
        }
    }
}
