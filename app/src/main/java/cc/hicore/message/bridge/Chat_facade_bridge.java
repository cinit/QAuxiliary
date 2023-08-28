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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See theqwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.hicore.message.bridge;

import android.content.Context;
import android.os.Environment;
import cc.hicore.ReflectUtil.MClass;
import cc.hicore.ReflectUtil.MMethod;
import cc.ioctl.util.HostInfo;
import cc.hicore.QApp.QAppUtils;
import cc.hicore.Utils.FileUtils;
import cc.hicore.message.common.MsgBuilder;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.QQVersion;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class Chat_facade_bridge {

    public static void sendText(Object _Session, String text, ArrayList atList) {
        try {
            Method CallMethod = MMethod.FindMethod("com.tencent.mobileqq.activity.ChatActivityFacade", null, void.class, new Class[]{
                    MClass.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                    Context.class,
                    MClass.loadClass("com.tencent.mobileqq.activity.aio.SessionInfo"),
                    String.class,
                    ArrayList.class
            });
            CallMethod.invoke(null, QAppUtils.getAppRuntime(), HostInfo.getApplication(), _Session, text, atList);
        } catch (Exception e) {
            Log.e(e);
        }
    }

    public static void sendPic(Object _Session, File pic) {
        try {
            var picRecord = MsgBuilder.build_pic(_Session, pic.getAbsolutePath());
            sendPic(_Session, picRecord);
        } catch (Exception e) {
            Log.e(e);
        }
    }

    public static void sendPic(Object _Session, Object picRecord) {
        try {
            Method hookMethod = MMethod.FindMethod("com.tencent.mobileqq.activity.ChatActivityFacade", null, void.class, new Class[]{
                    MClass.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                    MClass.loadClass("com.tencent.mobileqq.activity.aio.SessionInfo"),
                    MClass.loadClass("com.tencent.mobileqq.data.MessageForPic"),
                    int.class
            });
            hookMethod.invoke(null,
                    QAppUtils.getAppRuntime(), _Session, picRecord, 0
            );
        } catch (Exception e) {
            Log.e(e);
        }
    }

    public static void sendStruct(Object _Session, Object structMsg) {
        try {
            Method CallMethod = MMethod.FindMethod("com.tencent.mobileqq.activity.ChatActivityFacade", null,
                    void.class, new Class[]{
                            MClass.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                            MClass.loadClass("com.tencent.mobileqq.activity.aio.SessionInfo"),
                            MClass.loadClass("com.tencent.mobileqq.structmsg.AbsStructMsg")
                    });
            CallMethod.invoke(null, QAppUtils.getAppRuntime(), _Session, structMsg);
        } catch (Throwable th) {
            Log.e(th);
        }
    }

    public static void sendArkApp(Object _Session, Object arkAppMsg) {
        try {
            Method CallMethod = MMethod.FindMethod("com.tencent.mobileqq.activity.ChatActivityFacade", null,
                    boolean.class, new Class[]{
                            MClass.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                            MClass.loadClass("com.tencent.mobileqq.activity.aio.SessionInfo"),
                            MClass.loadClass("com.tencent.mobileqq.data.ArkAppMessage")
                    });
            CallMethod.invoke(null, QAppUtils.getAppRuntime(), _Session, arkAppMsg);
        } catch (Throwable th) {
            Log.e(th);
        }
    }

    public static void sendVoice(Object _Session, String path) {
        try {
            if (!path.contains("com.tencent.mobileqq/Tencent/MobileQQ/" + QAppUtils.getCurrentUin())) {
                String newPath = Environment.getExternalStorageDirectory() + "/Android/data/com.tencent.mobileqq/Tencent/MobileQQ/" + QAppUtils.getCurrentUin()
                        + "/ptt/" + new File(path).getName();
                FileUtils.copy(path, newPath);
                path = newPath;
            }
            Method CallMethod =
                    !HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_11) ?
                            MMethod.FindMethod("com.tencent.mobileqq.activity.ChatActivityFacade", "a", long.class,
                                    new Class[]{MClass.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                                            MClass.loadClass("com.tencent.mobileqq.activity.aio.SessionInfo"), String.class}) :
                            MMethod.FindMethod("com.tencent.mobileqq.activity.ChatActivityFacade", null, long.class,
                                    new Class[]{MClass.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                                            MClass.loadClass("com.tencent.mobileqq.activity.aio.SessionInfo"), String.class});
            CallMethod.invoke(null, QAppUtils.getAppRuntime(), _Session, path);
        } catch (Exception e) {
            Log.e(e);
        }
    }

    public static void sendMix(Object _Session, Object mixRecord) {
        try {

            if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_0)){
                Method mMethod = MMethod.FindMethod("com.tencent.mobileqq.replymsg.d", null, void.class, new Class[]{
                        MClass.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                        MClass.loadClass("com.tencent.mobileqq.data.MessageForMixedMsg"),
                        MClass.loadClass("com.tencent.mobileqq.activity.aio.SessionInfo"),
                        int.class
                });
                Object Call = MMethod.CallStaticMethodNoParam(MClass.loadClass("com.tencent.mobileqq.replymsg.d"), null,
                        MClass.loadClass("com.tencent.mobileqq.replymsg.d"));
                mMethod.invoke(Call, QAppUtils.getAppRuntime(), mixRecord, _Session, 0);
            }else {
                Method mMethod = MMethod.FindMethod("com.tencent.mobileqq.replymsg.ReplyMsgSender", null, void.class, new Class[]{
                        MClass.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                        MClass.loadClass("com.tencent.mobileqq.data.MessageForMixedMsg"),
                        MClass.loadClass("com.tencent.mobileqq.activity.aio.SessionInfo"),
                        int.class
                });
                Object Call = MMethod.CallStaticMethodNoParam(MClass.loadClass("com.tencent.mobileqq.replymsg.ReplyMsgSender"), null,
                        MClass.loadClass("com.tencent.mobileqq.replymsg.ReplyMsgSender"));
                mMethod.invoke(Call, QAppUtils.getAppRuntime(), mixRecord, _Session, 0);
            }

        } catch (Exception e) {
            Log.e(e);
        }

    }

    public static void sendReply(Object _Session, Object replyRecord) {
        try {

            if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_0)){
                Object Call = MMethod.CallStaticMethodNoParam(MClass.loadClass("com.tencent.mobileqq.replymsg.d"), null,
                        MClass.loadClass("com.tencent.mobileqq.replymsg.d"));
                Method mMethod = MMethod.FindMethod("com.tencent.mobileqq.replymsg.d", null, void.class, new Class[]{
                        MClass.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                        MClass.loadClass("com.tencent.mobileqq.data.ChatMessage"),
                        Initiator._BaseSessionInfo(),
                        int.class,
                        int.class,
                        boolean.class
                });
                mMethod.invoke(Call, QAppUtils.getAppRuntime(), replyRecord, _Session, 2, 0, false);
            }else {
                Object Call = MMethod.CallStaticMethodNoParam(MClass.loadClass("com.tencent.mobileqq.replymsg.ReplyMsgSender"), null,
                        MClass.loadClass("com.tencent.mobileqq.replymsg.ReplyMsgSender"));
                Method mMethod = MMethod.FindMethod("com.tencent.mobileqq.replymsg.ReplyMsgSender", null, void.class, new Class[]{
                        MClass.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                        MClass.loadClass("com.tencent.mobileqq.data.ChatMessage"),
                        MClass.loadClass("com.tencent.mobileqq.activity.aio.BaseSessionInfo"),
                        int.class,
                        int.class,
                        boolean.class
                });
                mMethod.invoke(Call, QAppUtils.getAppRuntime(), replyRecord, _Session, 2, 0, false);
            }

        } catch (Exception e) {
            Log.e(e);
        }
    }
    public static void sendAnimation(Object Session, int sevrID) {
        try {

            if (!HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_20)) {
                MMethod.CallMethod(null, MClass.loadClass("com.tencent.mobileqq.emoticonview.AniStickerSendMessageCallBack"), "sendAniSticker",
                        boolean.class, new Class[]{int.class, MClass.loadClass("com.tencent.mobileqq.activity.aio.BaseSessionInfo")}, sevrID, Session);
            } else {
                MMethod.CallMethod(null, MClass.loadClass("com.tencent.mobileqq.emoticonview.AniStickerSendMessageCallBack"), "sendAniSticker",
                        boolean.class, new Class[]{int.class, Initiator._BaseSessionInfo(), int.class}, sevrID,
                        Session, 0);
            }
        } catch (Exception e) {
            Log.e(e);
        }
    }

    public static void QQ_Forward_ShortVideo(Object _SessionInfo, Object ChatMessage) {
        try {
            MMethod.CallStaticMethod(MClass.loadClass("com.tencent.mobileqq.activity.ChatActivityFacade"), null, void.class, new Class[]{
                    MClass.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                    MClass.loadClass("com.tencent.mobileqq.activity.aio.SessionInfo"),
                    MClass.loadClass("com.tencent.mobileqq.data.MessageForShortVideo")
            }, QAppUtils.getAppRuntime(), _SessionInfo, ChatMessage);
        } catch (Exception e) {
            Log.e(e);
        }
    }
    public static void AddAndSendMsg(Object MessageRecord) {
        try {
            Object MessageFacade = MMethod.CallMethodNoParam(QAppUtils.getAppRuntime(), "getMessageFacade",
                    MClass.loadClass("com.tencent.imcore.message.QQMessageFacade"));
            Method mMethod = MMethod.FindMethod("com.tencent.imcore.message.BaseQQMessageFacade", null, void.class, new Class[]{
                    MClass.loadClass("com.tencent.mobileqq.data.MessageRecord"),
                    MClass.loadClass("com.tencent.mobileqq.app.BusinessObserver"),boolean.class
            });
            mMethod.invoke(MessageFacade, MessageRecord, null,false);
        } catch (Exception e) {
        }

    }
}
