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

package cc.hicore.message.bridge;

import android.content.Context;
import android.os.Environment;
import cc.hicore.ReflectUtil.XMethod;
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
            XMethod.clz("com.tencent.mobileqq.activity.ChatActivityFacade").ret( void.class).param(
                    Initiator.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                    Context.class,
                    Initiator.loadClass("com.tencent.mobileqq.activity.aio.SessionInfo"),
                    String.class,
                    ArrayList.class
            ).invoke(QAppUtils.getAppRuntime(), HostInfo.getApplication(), _Session, text, atList);
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
            XMethod.clz("com.tencent.mobileqq.activity.ChatActivityFacade").ret(void.class).param(
                    Initiator.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                    Initiator.loadClass("com.tencent.mobileqq.activity.aio.SessionInfo"),
                    Initiator.loadClass("com.tencent.mobileqq.data.MessageForPic"),
                    int.class
            ).invoke(QAppUtils.getAppRuntime(), _Session, picRecord, 0);
        } catch (Exception e) {
            Log.e(e);
        }
    }

    public static void sendStruct(Object _Session, Object structMsg) {
        try {
            XMethod.clz("com.tencent.mobileqq.activity.ChatActivityFacade").ret(void.class).param(
                    Initiator.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                    Initiator.loadClass("com.tencent.mobileqq.activity.aio.SessionInfo"),
                    Initiator.loadClass("com.tencent.mobileqq.structmsg.AbsStructMsg")
            ).invoke(QAppUtils.getAppRuntime(), _Session, structMsg);
        } catch (Throwable th) {
            Log.e(th);
        }
    }

    public static void sendArkApp(Object _Session, Object arkAppMsg) {
        try {
            XMethod.clz("com.tencent.mobileqq.activity.ChatActivityFacade").ret(boolean.class).param(
                    Initiator.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                    Initiator.loadClass("com.tencent.mobileqq.activity.aio.SessionInfo"),
                    Initiator.loadClass("com.tencent.mobileqq.data.ArkAppMessage")
            ).invoke(QAppUtils.getAppRuntime(), _Session, arkAppMsg);
        } catch (Throwable th) {
            Log.e(th);
        }
    }

    public static void sendVoice(Object _Session, String path) {
        try {
            if (!path.contains(HostInfo.getPackageName() + "/Tencent/MobileQQ/" + QAppUtils.getCurrentUin())) {
                String newPath = Environment.getExternalStorageDirectory() + "/Android/data/" + HostInfo.getPackageName() + "/Tencent/MobileQQ/" + QAppUtils.getCurrentUin()
                        + "/ptt/" + new File(path).getName();
                FileUtils.copy(path, newPath);
                path = newPath;
            }

            XMethod.clz("com.tencent.mobileqq.activity.ChatActivityFacade").ret(long.class).param(
                    Initiator.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                    Initiator.loadClass("com.tencent.mobileqq.activity.aio.SessionInfo"),
                    String.class
            ).invoke(QAppUtils.getAppRuntime(), _Session, path);


        } catch (Exception e) {
            Log.e(e);
        }
    }

    public static void sendMix(Object _Session, Object mixRecord) {
        try {
            Object replyMsgSender;
            if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_0)){
                replyMsgSender = XMethod.clz("com.tencent.mobileqq.replymsg.d").param(Initiator.loadClass("com.tencent.mobileqq.replymsg.d")).invoke();
            }else {
                replyMsgSender = XMethod.clz("com.tencent.mobileqq.replymsg.ReplyMsgSender").param(Initiator.loadClass("com.tencent.mobileqq.replymsg.ReplyMsgSender")).invoke();
            }
            XMethod.obj(replyMsgSender).ret(void.class).param(
                    Initiator.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                    Initiator.loadClass("com.tencent.mobileqq.data.MessageForMixedMsg"),
                    Initiator.loadClass("com.tencent.mobileqq.activity.aio.SessionInfo"),
                    int.class
            ).invoke(QAppUtils.getAppRuntime(), mixRecord, _Session, 0);

        } catch (Exception e) {
            Log.e(e);
        }

    }

    public static void sendReply(Object _Session, Object replyRecord) {
        try {
            Object replyMsgSender;
            if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_0)){
                replyMsgSender = XMethod.clz("com.tencent.mobileqq.replymsg.d").param(Initiator.loadClass("com.tencent.mobileqq.replymsg.d")).invoke();
            }else {
                replyMsgSender = XMethod.clz("com.tencent.mobileqq.replymsg.ReplyMsgSender").param(Initiator.loadClass("com.tencent.mobileqq.replymsg.ReplyMsgSender")).invoke();
            }

            XMethod.obj(replyMsgSender).ret(void.class).param(
                    Initiator.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                    Initiator.loadClass("com.tencent.mobileqq.data.ChatMessage"),
                    Initiator._BaseSessionInfo(),
                    int.class,
                    int.class,
                    boolean.class
            ).invoke(QAppUtils.getAppRuntime(), replyRecord, _Session, 2, 0, false);

        } catch (Exception e) {
            Log.e(e);
        }
    }
    public static void sendAnimation(Object Session, int sevrID) {
        try {

            if (!HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_20)) {
                XMethod.clz("com.tencent.mobileqq.emoticonview.AniStickerSendMessageCallBack").name("sendAniSticker")
                        .ret(boolean.class).param(int.class, Initiator.loadClass("com.tencent.mobileqq.activity.aio.BaseSessionInfo")).invoke( sevrID, Session);
            } else {

                XMethod.clz("com.tencent.mobileqq.emoticonview.AniStickerSendMessageCallBack").name("sendAniSticker")
                        .ret(boolean.class).param(int.class, Initiator._BaseSessionInfo(), int.class).invoke( sevrID, Session, 0);
            }
        } catch (Exception e) {
            Log.e(e);
        }
    }

    public static void QQ_Forward_ShortVideo(Object _SessionInfo, Object ChatMessage) {
        try {
            XMethod.clz("com.tencent.mobileqq.activity.ChatActivityFacade").ret(void.class).param(
                    Initiator.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                    Initiator.loadClass("com.tencent.mobileqq.activity.aio.SessionInfo"),
                    Initiator.loadClass("com.tencent.mobileqq.data.MessageForShortVideo")
            ).invoke(QAppUtils.getAppRuntime(), _SessionInfo, ChatMessage);
        } catch (Exception e) {
            Log.e(e);
        }
    }
    public static void AddAndSendMsg(Object MessageRecord) {
        try {
            Object MessageFacade = XMethod.obj(QAppUtils.getAppRuntime()).name("getMessageFacade").ret(Initiator.loadClass("com.tencent.imcore.message.QQMessageFacade")).invoke();
            XMethod.obj(MessageFacade).ret(void.class).param(
                    Initiator.loadClass("com.tencent.mobileqq.data.MessageRecord"),
                    Initiator.loadClass("com.tencent.mobileqq.app.BusinessObserver"),
                    boolean.class
            ).invoke(MessageRecord, null,false);
        } catch (Exception e) {
        }

    }
}
