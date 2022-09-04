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

package cc.ioctl.util;

import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

public class TroopManagerHelper {

    private TroopManagerHelper() {
    }

    public static Object getManager(int index) throws Exception {
        return Reflex.invokeVirtual(AppRuntimeHelper.getQQAppInterface(), "getManager", index, int.class);
    }

    public static Object getTroopManager() throws Exception {
        int troopMgrId = -1;
        Class<?> cl_QQManagerFactory = Initiator.load("com.tencent.mobileqq.app.QQManagerFactory");
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

    public static class TroopInfo implements Comparable {

        public String troopuin;
        public String troopname;
        public CharSequence _troopuin;
        public CharSequence _troopname;
        public int hit;

        public TroopInfo(Object obj) {
            _troopname = troopname = (String) Reflex.getInstanceObjectOrNull(obj, "troopname");
            _troopuin = troopuin = (String) Reflex.getInstanceObjectOrNull(obj, "troopuin");
            hit = 0;
        }

        @Override
        public int compareTo(Object o) {
            TroopInfo t = (TroopInfo) o;
            return t.hit - hit;
        }
    }


    public static ArrayList<TroopInfo> getTroopInfoList() throws Exception {
        ArrayList<?> tx = getTroopInfoListRaw();
        ArrayList<TroopInfo> ret = new ArrayList<>();
        for (Object info : tx) {
            ret.add(new TroopInfo(info));
        }
        return ret;
    }

    public static ArrayList<?> getTroopInfoListRaw() throws Exception {
        Object mTroopManager = getTroopManager();
        ArrayList<?> tx;
        Method m0a = null, m0b = null;
        for (Method m : mTroopManager.getClass().getMethods()) {
            if (m.getReturnType().equals(ArrayList.class) && Modifier.isPublic(m.getModifiers())
                    && m.getParameterTypes().length == 0) {
                if (m.getName().equals("a")) {
                    m0a = m;
                    break;
                } else {
                    if (m0a == null) {
                        m0a = m;
                    } else {
                        m0b = m;
                        break;
                    }
                }
            }
        }
        if (m0b == null) {
            tx = (ArrayList<?>) m0a.invoke(mTroopManager);
        } else {
            tx = (ArrayList<?>) ((Reflex.strcmp(m0a.getName(), m0b.getName()) > 0) ? m0b : m0a)
                    .invoke(mTroopManager);
        }
        return tx;
    }
}
