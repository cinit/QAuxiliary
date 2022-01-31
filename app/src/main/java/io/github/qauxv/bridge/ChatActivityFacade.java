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

import static io.github.qauxv.util.Initiator._BaseSessionInfo;
import static io.github.qauxv.util.Initiator._QQAppInterface;
import static io.github.qauxv.util.Initiator._SessionInfo;
import static io.github.qauxv.bridge.AppRuntimeHelper.getQQAppInterface;
import static cc.ioctl.util.Reflex.getShortClassName;

import android.content.Context;
import android.os.Parcelable;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import io.github.qauxv.util.DexKit;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.Toasts;
import java.io.Externalizable;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import mqq.app.AppRuntime;

public class ChatActivityFacade {

    public static long[] sendMessage(AppRuntime qqAppInterface, Context context,
                                     Parcelable sessionInfo, String msg,
                                     ArrayList<?> atInfo, Object sendMsgParams) {
        if (qqAppInterface == null) {
            throw new NullPointerException("qqAppInterface == null");
        }
        if (sessionInfo == null) {
            throw new NullPointerException("sessionInfo == null");
        }
        Class facade = DexKit.doFindClass(DexKit.C_FACADE);
        Class SendMsgParams = null;
        Method m = null;
        for (Method mi : facade.getDeclaredMethods()) {
            if (!mi.getReturnType().equals(long[].class)) {
                continue;
            }
            Class[] argt = mi.getParameterTypes();
            if (argt.length != 6) {
                continue;
            }
            if (argt[1].equals(Context.class)
                && (argt[2].equals(_SessionInfo()) || argt[2].equals(_BaseSessionInfo()))
                && argt[3].equals(String.class) && argt[4].equals(ArrayList.class)) {
                m = mi;
                m.setAccessible(true);
                SendMsgParams = argt[5];
                break;
            }
        }
        try {
            if (sendMsgParams == null) {
                sendMsgParams = SendMsgParams.newInstance();
            }
            return (long[]) m
                .invoke(null, qqAppInterface, context, sessionInfo, msg, atInfo, sendMsgParams);
        } catch (Exception e) {
            Log.e(e);
            return null;
        }
    }

    public static long[] sendMessage(AppRuntime qqAppInterface, Context context,
                                     Parcelable sessionInfo, String msg) {
        if (qqAppInterface == null) {
            throw new NullPointerException("qqAppInterface == null");
        }
        if (sessionInfo == null) {
            throw new NullPointerException("sessionInfo == null");
        }
        if (msg == null) {
            throw new NullPointerException("msg == null");
        }
        Class facade = DexKit.doFindClass(DexKit.C_FACADE);
        Class SendMsgParams = null;
        Method m = null;
        for (Method mi : facade.getDeclaredMethods()) {
            if (!mi.getReturnType().equals(long[].class)) {
                continue;
            }
            Class[] argt = mi.getParameterTypes();
            if (argt.length != 6) {
                continue;
            }
            if (argt[1].equals(Context.class)
                && (argt[2].equals(_SessionInfo()) || argt[2].equals(_BaseSessionInfo()))
                && argt[3].equals(String.class) && argt[4].equals(ArrayList.class)) {
                m = mi;
                m.setAccessible(true);
                SendMsgParams = argt[5];
                break;
            }
        }
        try {
            return (long[]) m
                .invoke(null, qqAppInterface, context, sessionInfo, msg, new ArrayList<>(),
                    SendMsgParams.newInstance());
        } catch (Exception e) {
            Log.e(e);
            return null;
        }
    }

    public static long sendPttMessage(AppRuntime qqAppInterface, Parcelable sessionInfo,
                                      String pttPath) {
        if (qqAppInterface == null) {
            throw new NullPointerException("qqAppInterface == null");
        }
        if (sessionInfo == null) {
            throw new NullPointerException("sessionInfo == null");
        }
        if (pttPath == null) {
            throw new NullPointerException("pttPath == null");
        }
        Method send = null;
        for (Method m : DexKit.doFindClass(DexKit.C_FACADE).getMethods()) {
            if (m.getReturnType().equals(long.class)) {
                Class<?>[] clz = m.getParameterTypes();
                if (clz.length != 3) {
                    continue;
                }
                if (clz[0].equals(_QQAppInterface()) && clz[1].equals(_SessionInfo()) && clz[2]
                    .equals(String.class)) {
                    send = m;
                    break;
                }
            }
        }
        try {
            return (long) send.invoke(null, qqAppInterface, sessionInfo, pttPath);
        } catch (Exception e) {
            Log.e(e);
            return 0;
        }
    }

    public static boolean sendArkAppMessage(AppRuntime qqAppInterface, Parcelable sessionInfo,
                                            Object arkAppMsg) {
        if (qqAppInterface == null) {
            throw new NullPointerException("qqAppInterface == null");
        }
        if (sessionInfo == null) {
            throw new NullPointerException("sessionInfo == null");
        }
        if (arkAppMsg == null) {
            throw new NullPointerException("arkAppMsg == null");
        }
        Method send = null;
        for (Method m : DexKit.doFindClass(DexKit.C_FACADE).getMethods()) {
            if (m.getReturnType().equals(boolean.class)) {
                Class<?>[] clz = m.getParameterTypes();
                if (clz.length != 3) {
                    continue;
                }
                if (clz[0].equals(_QQAppInterface()) && clz[1].equals(_SessionInfo()) && clz[2]
                    .isInstance(arkAppMsg)) {
                    send = m;
                    break;
                }
            }
        }
        try {
            return (boolean) send.invoke(null, qqAppInterface, sessionInfo, arkAppMsg);
        } catch (Exception e) {
            Log.e(e);
            return false;
        }
    }

    public static void sendAbsStructMsg(AppRuntime qqAppInterface, Parcelable sessionInfo,
                                        Externalizable absStructMsg) {
        if (qqAppInterface == null) {
            throw new NullPointerException("qqAppInterface == null");
        }
        if (sessionInfo == null) {
            throw new NullPointerException("sessionInfo == null");
        }
        if (absStructMsg == null) {
            throw new NullPointerException("absStructMsg == null");
        }
        Method send = null;
        for (Method m : DexKit.doFindClass(DexKit.C_FACADE).getMethods()) {
            if (m.getReturnType().equals(void.class)) {
                Class<?>[] clz = m.getParameterTypes();
                if (clz.length != 3) {
                    continue;
                }
                if (clz[0].equals(_QQAppInterface()) && clz[1].equals(_SessionInfo()) && clz[2]
                    .isInstance(absStructMsg)) {
                    send = m;
                    break;
                }
            }
        }
        try {
            send.invoke(null, qqAppInterface, sessionInfo, absStructMsg);
        } catch (Exception e) {
            Log.e(e);
        }
    }

    public static void repeatMessage(AppRuntime app, Parcelable session, Object msg) {
        if (app == null) {
            throw new NullPointerException("app == null");
        }
        if (session == null) {
            throw new NullPointerException("session == null");
        }
        if (msg == null) {
            throw new NullPointerException("msg == null");
        }
        String msgText;
        Class[] argt = null;
        Method m = null;
        switch (getShortClassName(msg)) {
            case "MessageForText":
            case "MessageForFoldMsg":
            case "MessageForLongTextMsg":
                msgText = (String) Reflex.getInstanceObjectOrNull(msg, "msg");
                if (msgText.length() > 3000) {
                    Toasts.error(HostInfo.getApplication(),
                        "暂不支持发送长消息");
                    return;
                }
                ArrayList<?> atInfo = null;
                try {
                    String extStr = (String) Reflex.invokeVirtual(msg, "getExtInfoFromExtStr", "troop_at_info_list",
                        String.class);
                    atInfo = (ArrayList) Reflex.invokeVirtual(msg, "getTroopMemberInfoFromExtrJson", extStr,
                        String.class);
                } catch (Exception e) {
                    // ignore
                }
                if (atInfo == null) {
                    sendMessage(app, HostInfo.getApplication(), session,
                        msgText);
                } else {
                    sendMessage(app, HostInfo.getApplication(), session,
                        msgText, atInfo, null);
                }
                break;
            case "MessageForPic":
                try {
                    for (Method mi : DexKit.doFindClass(DexKit.C_FACADE).getMethods()) {
                        if (!mi.getName().equals("a") && !mi.getName().equals("b")) {
                            continue;
                        }
                        argt = mi.getParameterTypes();
                        if (argt.length < 3) {
                            continue;
                        }
                        if (argt[0].equals(Initiator._QQAppInterface()) && argt[1]
                            .equals(_SessionInfo())
                            && argt[2].isAssignableFrom(msg.getClass()) && mi.getReturnType()
                            .equals(void.class)) {
                            m = mi;
                            break;
                        }
                    }
                    if (argt.length == 3) {
                        m.invoke(null, app, session, msg);
                    } else {
                        m.invoke(null, app, session, msg, 0);
                    }
                } catch (Exception e) {
                    Toasts.error(HostInfo.getApplication(),
                        e.toString().replace("java.lang.", ""));
                    Log.e(e);
                }
                break;
            case "MessageForPtt":
                try {
                    String url = (String) Reflex.invokeVirtual(msg, "getLocalFilePath");
                    File file = new File(url);
                    if (!file.exists()) {
                        Toasts.error(HostInfo.getApplication(),
                            "未找到语音文件");
                        return;
                    }
                    sendPttMessage(getQQAppInterface(), session, url);
                } catch (Exception e) {
                    Toasts.error(HostInfo.getApplication(),
                        e.toString().replace("java.lang.", ""));
                    Log.e(e);
                }
                break;
            default:
                Toasts.error(HostInfo.getApplication(),
                    "Unsupported msg type: " + getShortClassName(msg));
        }
    }
}
