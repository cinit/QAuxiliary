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
package io.github.qauxv.bridge;

import static io.github.qauxv.util.Initiator._QQAppInterface;
import static io.github.qauxv.util.Initiator.load;
import static io.github.qauxv.bridge.AppRuntimeHelper.getQQAppInterface;
import static io.github.qauxv.bridge.ManagerHelper.getTroopManager;

import cc.ioctl.util.Reflex;
import de.robv.android.xposed.XposedHelpers;
import io.github.qauxv.util.DexKit;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import java.lang.reflect.Modifier;

public class ContactUtils {

    public static String getTroopMemberNick(String troopUin, String memberUin) {
        if (troopUin != null && troopUin.length() > 0) {
            try {
                Object mTroopManager = getTroopManager();
                Object troopMemberInfo = Reflex.invokeVirtualDeclaredOrdinal(mTroopManager, 0, 3, false,
                    troopUin, memberUin, String.class, String.class, Initiator._TroopMemberInfo());
                if (troopMemberInfo != null) {
                    String troopnick = (String) XposedHelpers
                        .getObjectField(troopMemberInfo, "troopnick");
                    if (troopnick != null) {
                        String ret = troopnick.replaceAll("\\u202E", "");
                        if (ret.trim().length() > 0) {
                            return ret;
                        }
                    }
                }
            } catch (Throwable e) {
                Log.e(e);
            }
            try {
                String ret;//getDiscussionMemberShowName
                Object nickname = Reflex.invokeStaticDeclaredOrdinalModifier(
                    DexKit.doFindClass(DexKit.C_CONTACT_UTILS),
                    2, 10, false, Modifier.PUBLIC, 0,
                    getQQAppInterface(), troopUin, memberUin, _QQAppInterface(), String.class,
                    String.class);
                if (nickname instanceof String) {
                    if (nickname != null
                        && (ret = ((String) nickname).replaceAll("\\u202E", "")).trim().length() > 0) {
                        return ret;
                    }
                } else {
                    if (nickname != null
                        && (ret = Reflex.getInstanceObject(nickname, "a", String.class)
                        .replaceAll("\\u202E", "")).trim().length() > 0) {
                        return ret;
                    }
                }
            } catch (ReflectiveOperationException e) {
                Log.e(e);
            }
        }
        try {
            String ret;//getBuddyName
            String nickname = null;
            try {
                nickname = (String) Reflex.invokeStaticDeclaredOrdinalModifier(
                    DexKit.doFindClass(DexKit.C_CONTACT_UTILS), 1, 3, true, Modifier.PUBLIC, 0,
                    getQQAppInterface(), memberUin, true, _QQAppInterface(), String.class,
                    boolean.class, String.class);
            } catch (Throwable e2) {
                try {
                    nickname = (String) Reflex.invokeStaticDeclaredOrdinalModifier(
                        DexKit.doFindClass(DexKit.C_CONTACT_UTILS), 1, 4, true, Modifier.PUBLIC, 0,
                        getQQAppInterface(), memberUin, true, _QQAppInterface(), String.class,
                        boolean.class, String.class);
                } catch (Throwable e3) {
                    try {
                        nickname = (String) Reflex.invokeStaticDeclaredOrdinalModifier(
                            DexKit.doFindClass(DexKit.C_CONTACT_UTILS), 1, 2, false,
                            Modifier.PUBLIC,
                            0,
                            getQQAppInterface(), memberUin, true,
                            load("com.tencent.common.app.AppInterface"), String.class,
                            boolean.class, String.class);
                    } catch (Throwable e4) {
                        e2.addSuppressed(e3);
                        e2.addSuppressed(e4);
                        Log.e(e2);
                    }
                }
            }
            if (nickname != null
                && (ret = nickname.replaceAll("\\u202E", "")).trim().length() > 0) {
                return ret;
            }
        } catch (Throwable e) {
            Log.e(e);
        }
        //**sigh**
        return memberUin;
    }
}
