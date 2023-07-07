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

package io.github.qauxv.bridge.ntapi;

import com.tencent.mobileqq.qroute.QRoute;
import com.tencent.mobileqq.qroute.QRouteApi;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Method;

public class RelationNTUinAndUidApi {

    private RelationNTUinAndUidApi() {
    }

    private static Object sImpl = null;
    private static Method sGetUidFromUin = null;
    private static Method sGetUinFromUid = null;

    private static Object getRelationNTUinAndUidApiImpl() throws ReflectiveOperationException, LinkageError {
        if (sImpl == null) {
            Class<? extends QRouteApi> klass = (Class<? extends QRouteApi>) Initiator.loadClass("com.tencent.relation.common.api.IRelationNTUinAndUidApi");
            sGetUidFromUin = klass.getMethod("getUidFromUin", String.class);
            sGetUinFromUid = klass.getMethod("getUinFromUid", String.class);
            sImpl = QRoute.api(klass);
        }
        return sImpl;
    }

    public static boolean isAvailable() {
        try {
            getRelationNTUinAndUidApiImpl();
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            return false;
        }
    }

    public static String getUidFromUin(String str) {
        try {
            Object impl = getRelationNTUinAndUidApiImpl();
            return (String) sGetUidFromUin.invoke(impl, str);
        } catch (ReflectiveOperationException | LinkageError e) {
            throw new RuntimeException("RelationNTUinAndUidApi not available", e);
        }
    }


    public static String getUinFromUid(String str) {
        try {
            Object impl = getRelationNTUinAndUidApiImpl();
            return (String) sGetUinFromUid.invoke(impl, str);
        } catch (ReflectiveOperationException | LinkageError e) {
            throw new RuntimeException("RelationNTUinAndUidApi not available", e);
        }
    }

}
