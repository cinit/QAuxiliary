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

import static cc.ioctl.util.Reflex.getShortClassName;
import static io.github.qauxv.bridge.AppRuntimeHelper.getQQAppInterface;
import static io.github.qauxv.util.Initiator._BaseSessionInfo;
import static io.github.qauxv.util.Initiator._ChatMessage;
import static io.github.qauxv.util.Initiator._QQAppInterface;
import static io.github.qauxv.util.Initiator._SessionInfo;

import android.content.Context;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.Objects;
import mqq.app.AppRuntime;

public class ChatActivityFacade {

    private ChatActivityFacade() {
        throw new AssertionError("no instance for you");
    }

    /**
     * Send a text message to the specified chat session with extra parameters.
     * <p>
     * If you just want to send a plain text message, use {@link #sendMessage(AppRuntime, Context, Parcelable, String)} for convenience.
     *
     * @param qqAppInterface The QQAppInterface instance, see {@link AppRuntimeHelper#getQQAppInterface()} for details.
     * @param context        The context.
     * @param sessionInfo    The target chat session, see {@link SessionInfoImpl} for details.
     * @param msg            The message to be sent.
     * @param atInfo         The @at info.
     * @param sendMsgParams  The extra parameters.
     * @return The message id, or null if failed.
     */
    public static long[] sendMessage(@NonNull AppRuntime qqAppInterface, @NonNull Context context, @NonNull Parcelable sessionInfo,
                                     @NonNull String msg, @Nullable ArrayList<?> atInfo, @Nullable Object sendMsgParams) {
        Objects.requireNonNull(qqAppInterface, "qqAppInterface == null");
        Objects.requireNonNull(context, "context == null");
        Objects.requireNonNull(sessionInfo, "sessionInfo == null");
        Objects.requireNonNull(msg, "msg == null");
        Class<?> facade = DexKit.doFindClass(DexKit.C_FACADE);
        Class<?> kSendMsgParams = null;
        Method m = null;
        for (Method mi : facade.getDeclaredMethods()) {
            if (!mi.getReturnType().equals(long[].class)) {
                continue;
            }
            Class<?>[] argt = mi.getParameterTypes();
            if (argt.length != 6) {
                continue;
            }
            if (argt[1].equals(Context.class) && (argt[2].equals(_SessionInfo()) || argt[2].equals(_BaseSessionInfo()))
                    && argt[3].equals(String.class) && argt[4].equals(ArrayList.class)) {
                m = mi;
                m.setAccessible(true);
                kSendMsgParams = argt[5];
                break;
            }
        }
        try {
            if (atInfo == null) {
                atInfo = new ArrayList<>();
            }
            if (sendMsgParams == null) {
                sendMsgParams = kSendMsgParams.newInstance();
            }
            return (long[]) m.invoke(null, qqAppInterface, context, sessionInfo, msg, atInfo, sendMsgParams);
        } catch (ReflectiveOperationException e) {
            Log.e(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Send a text message to the specified chat session.
     *
     * @param qqAppInterface The QQAppInterface instance, see {@link AppRuntimeHelper#getQQAppInterface()} for details.
     * @param context        The context.
     * @param sessionInfo    The target chat session, see {@link SessionInfoImpl} for details.
     * @param msg            The message to be sent.
     * @return The message id, or null if failed.
     */
    public static long[] sendMessage(AppRuntime qqAppInterface, Context context, Parcelable sessionInfo, String msg) {
        return sendMessage(qqAppInterface, context, sessionInfo, msg, null, null);
    }

    /**
     * Send a PTT message to a chat session.
     *
     * @param qqAppInterface see {@link AppRuntimeHelper#getQQAppInterface()}
     * @param sessionInfo    the chat session, see {@link SessionInfoImpl} for details
     * @param pttPath        the path of the PTT file
     * @return the message id, or 0 if failed
     */
    public static long sendPttMessage(@NonNull AppRuntime qqAppInterface, @NonNull Parcelable sessionInfo,
                                      @NonNull String pttPath) {
        Objects.requireNonNull(qqAppInterface, "qqAppInterface == null");
        Objects.requireNonNull(sessionInfo, "sessionInfo == null");
        Objects.requireNonNull(pttPath, "pttPath == null");
        Method send = null;
        for (Method m : DexKit.doFindClass(DexKit.C_FACADE).getMethods()) {
            if (m.getReturnType().equals(long.class)) {
                Class<?>[] clz = m.getParameterTypes();
                if (clz.length != 3) {
                    continue;
                }
                if (clz[0].equals(_QQAppInterface()) && clz[1].equals(_SessionInfo()) && clz[2].equals(String.class)) {
                    send = m;
                    break;
                }
            }
        }
        try {
            return (long) send.invoke(null, qqAppInterface, sessionInfo, pttPath);
        } catch (ReflectiveOperationException e) {
            Log.e(e);
            throw new RuntimeException(e);
        }
    }

    public static boolean sendArkAppMessage(@NonNull AppRuntime qqAppInterface, @NonNull Parcelable sessionInfo,
                                            @NonNull Object arkAppMsg) {
        Objects.requireNonNull(qqAppInterface, "qqAppInterface == null");
        Objects.requireNonNull(sessionInfo, "sessionInfo == null");
        Objects.requireNonNull(arkAppMsg, "arkAppMsg == null");
        Method send = null;
        for (Method m : DexKit.doFindClass(DexKit.C_FACADE).getMethods()) {
            if (m.getReturnType().equals(boolean.class)) {
                Class<?>[] clz = m.getParameterTypes();
                if (clz.length != 3) {
                    continue;
                }
                if (clz[0].equals(_QQAppInterface()) && clz[1].equals(_SessionInfo()) && clz[2].isInstance(arkAppMsg)) {
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

    public static void sendAbsStructMsg(@NonNull AppRuntime qqAppInterface, @NonNull Parcelable sessionInfo,
                                        @NonNull Externalizable absStructMsg) {
        Objects.requireNonNull(qqAppInterface, "qqAppInterface == null");
        Objects.requireNonNull(sessionInfo, "sessionInfo == null");
        Objects.requireNonNull(absStructMsg, "absStructMsg == null");
        Method send = null;
        for (Method m : DexKit.doFindClass(DexKit.C_FACADE).getMethods()) {
            if (m.getReturnType().equals(void.class)) {
                Class<?>[] clz = m.getParameterTypes();
                if (clz.length != 3) {
                    continue;
                }
                if (clz[0].equals(_QQAppInterface()) && clz[1].equals(_SessionInfo()) && clz[2].isInstance(absStructMsg)) {
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
    public static void sendReplyMsg(@NonNull AppRuntime qqAppInterface, @NonNull Parcelable sessionInfo,
            @NonNull Object replyMsg){
        Objects.requireNonNull(qqAppInterface, "qqAppInterface == null");
        Objects.requireNonNull(sessionInfo, "sessionInfo == null");
        Objects.requireNonNull(replyMsg, "absStructMsg == null");
        try{
            Object ReplyMsgSender = Reflex.invokeStatic(Initiator.load("com.tencent.mobileqq.replymsg.ReplyMsgSender"),"a",Initiator.load("com.tencent.mobileqq.replymsg.ReplyMsgSender"));
            Method invokeMethod = Reflex.findMethod(Initiator.load("com.tencent.mobileqq.replymsg.ReplyMsgSender"),void.class,"a", _QQAppInterface(),
                    _ChatMessage(),
                    _BaseSessionInfo(),
                    int.class,
                    int.class,
                    boolean.class);
            invokeMethod.invoke(ReplyMsgSender,qqAppInterface,replyMsg,sessionInfo,0,0,false);
        }catch (Exception e){
            Log.e(e);
        }
    }
    public static void sendMixedMsg(@NonNull AppRuntime qqAppInterface, @NonNull Parcelable sessionInfo,
            @NonNull Object mixedMsg){
        Objects.requireNonNull(qqAppInterface, "qqAppInterface == null");
        Objects.requireNonNull(sessionInfo, "sessionInfo == null");
        Objects.requireNonNull(mixedMsg, "absStructMsg == null");
        try{
            Object ReplyMsgSender = Reflex.invokeStatic(Initiator.load("com.tencent.mobileqq.replymsg.ReplyMsgSender"),"a",Initiator.load("com.tencent.mobileqq.replymsg.ReplyMsgSender"));
            Method invokeMethod = Reflex.findMethod(Initiator.load("com.tencent.mobileqq.replymsg.ReplyMsgSender"),void.class,"a",
                    _QQAppInterface(),
                    Initiator.load("com.tencent.mobileqq.data.MessageForMixedMsg"),
                    _SessionInfo(),
                    int.class);
            invokeMethod.invoke(ReplyMsgSender,qqAppInterface,mixedMsg,sessionInfo,0);
        }catch (Exception e){
            Log.e(e);
        }
    }

    public static void repeatMessage(@NonNull AppRuntime app, @NonNull Parcelable session, @NonNull Object msg) {
        Objects.requireNonNull(app, "app == null");
        Objects.requireNonNull(session, "session == null");
        Objects.requireNonNull(msg, "msg == null");
        String msgText;
        Class<?>[] argt = null;
        Method m = null;
        switch (getShortClassName(msg)) {
            case "MessageForText":
            case "MessageForFoldMsg":
            case "MessageForLongTextMsg":
                msgText = (String) Reflex.getInstanceObjectOrNull(msg, "msg");
                if (msgText.length() > 3000) {
                    Toasts.error(HostInfo.getApplication(), "暂不支持发送长消息");
                    return;
                }
                ArrayList<?> atInfo = null;
                try {
                    String extStr = (String) Reflex.invokeVirtual(msg, "getExtInfoFromExtStr", "troop_at_info_list", String.class);
                    atInfo = (ArrayList) Reflex.invokeVirtual(msg, "getTroopMemberInfoFromExtrJson", extStr, String.class);
                } catch (Exception e) {
                    // ignore
                }
                if (atInfo == null) {
                    sendMessage(app, HostInfo.getApplication(), session, msgText);
                } else {
                    sendMessage(app, HostInfo.getApplication(), session, msgText, atInfo, null);
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
                        if (argt[0].equals(Initiator._QQAppInterface()) && argt[1].equals(_SessionInfo())
                                && argt[2].isAssignableFrom(msg.getClass()) && mi.getReturnType().equals(void.class)) {
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
                    Toasts.error(HostInfo.getApplication(), e.toString().replace("java.lang.", ""));
                    Log.e(e);
                }
                break;
            case "MessageForPtt":
                try {
                    String url = (String) Reflex.invokeVirtual(msg, "getLocalFilePath");
                    File file = new File(url);
                    if (!file.exists()) {
                        Toasts.error(HostInfo.getApplication(), "未找到语音文件");
                        return;
                    }
                    sendPttMessage(getQQAppInterface(), session, url);
                } catch (Exception e) {
                    Toasts.error(HostInfo.getApplication(), e.toString().replace("java.lang.", ""));
                    Log.e(e);
                }
                break;
            case "MessageForReplyText":
                sendReplyMsg(app,session,msg);
                break;
            case "MessageForMixedMsg":
                sendMixedMsg(app,session,msg);
                break;
            default:
                Toasts.error(HostInfo.getApplication(), "Unsupported msg type: " + getShortClassName(msg));
        }
    }
}
