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

package com.hicore.hook;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import com.hicore.ReflectUtil.MMethod;
import de.robv.android.xposed.XC_MethodHook;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.bridge.ContactUtils;
import io.github.qauxv.bridge.SessionInfoImpl;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.router.decorator.IBaseChatPieInitDecorator;
import io.github.qauxv.router.dispacher.InputButtonHookDispatcher;
import io.github.qauxv.step.Step;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.IoUtils;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.Toasts;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import mqq.app.AppRuntime;

@FunctionHookEntry
@UiItemAgentEntry
public class ReplyMsgWithImg extends CommonSwitchFunctionHook implements IBaseChatPieInitDecorator {

    public static final ReplyMsgWithImg INSTANCE = new ReplyMsgWithImg();

    private ReplyMsgWithImg() {
    }

    @NonNull
    @Override
    public String getName() {
        return "回复带图";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "回复消息发送时一并把图片带上，在不支持的版本上行为是 undefined behavior";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY;
    }

    private WeakReference<EditText> mInputEditText = null;
    private WeakReference<Object> mBaseChatPie = null;

    @Override
    protected boolean initOnce() throws Exception {
        Method sendCustomEmotion = Reflex.findMethod(Initiator.loadClass("com.tencent.mobileqq.emoticonview.sender.CustomEmotionSenderUtil"), void.class,
                "sendCustomEmotion", Initiator._BaseQQAppInterface(), Context.class, Initiator.loadClass("com.tencent.mobileqq.activity.aio.BaseSessionInfo"),
                String.class, boolean.class, boolean.class, boolean.class, String.class, Initiator.loadClass("com.tencent.mobileqq.emoticon.StickerInfo"),
                String.class, Bundle.class);
        HookUtils.hookBeforeIfEnabled(this, sendCustomEmotion, param -> {
            Context ctx = (Context) param.args[1];
            Parcelable sessionInfo = (Parcelable) param.args[2];
            Object baseChatPie = mBaseChatPie == null ? null : mBaseChatPie.get();
            Objects.requireNonNull(baseChatPie, "baseChatPie or ref is null");
            int isTroop = SessionInfoImpl.getUinType(sessionInfo);
            if (isTroop == 1 || isTroop == 0) {
                String path = (String) param.args[3];
                EditText inputEditText = mInputEditText == null ? null : mInputEditText.get();
                if (inputEditText == null) {
                    Toasts.error(ctx, "inputEditText is null");
                    return;
                }
                // 1. 如果是回复消息，则总是由模块处理 mixed msg
                // 2. 如果已输入文本，则表情由模块处理，以 mixed msg 发送
                // 其余情况由宿主默认处理
                if (isNowReplying(baseChatPie) || !inputEditText.getText().toString().isEmpty()) {
                    Objects.requireNonNull(inputEditText, "inputEditText or ref is null");
                    addEditText(inputEditText, "[PicUrl=" + path + "]");
                    param.setResult(true);
                }
            }
        });

        HookUtils.hookBeforeIfEnabled(this, Initiator.loadClass("com.tencent.mobileqq.activity.aio.photo.PhotoListPanel")
                .getDeclaredMethod("a", Initiator._BaseChatPie(), List.class, boolean.class), param -> {
            Log.d("ReplyMsgWithImg PhotoListPanel.a");
            Object chatPie = param.args[0];
            if (chatPie == null) {
                chatPie = mBaseChatPie == null ? null : mBaseChatPie.get();
            }
            if (chatPie == null) {
                Toasts.error(null, "chatPie is null");
                Log.d("ReplyMsgWithImg PhotoListPanel.a chatPie is null");
                return;
            }
            Parcelable sessionInfo = InputButtonHookDispatcher.getSessionInfo(chatPie);
            int isTroop = SessionInfoImpl.getUinType(sessionInfo);
            if (isTroop == 1 || isTroop == 0) {
                List<String> l = (List<String>) param.args[1];
                EditText inputEditText = mInputEditText == null ? null : mInputEditText.get();
                if (inputEditText == null) {
                    Toasts.error(null, "inputEditText is null");
                    return;
                }
                // 如果已包含 [PicUrl=xxx] 则总是由模块处理 mixed msg
                if (isNowReplying(chatPie) || inputEditText.getText().toString().contains("[PicUrl=")) {
                    param.setResult(true);
                    for (String str : l) {
                        if (str.toLowerCase(Locale.ROOT).endsWith(".mp4")) {
                            continue;
                        }
                        addEditText(inputEditText, "[PicUrl=" + str + "]");
                        Reflex.invokeVirtual(param.thisObject, "a", true, boolean.class, void.class);
                    }
                }
            }
        });

        HookUtils.hookBeforeIfEnabled(this, Reflex.findMethod(Initiator.loadClass("com.tencent.imcore.message.BaseQQMessageFacade"), void.class, "a",
                        Initiator.loadClass("com.tencent.mobileqq.data.MessageRecord"), Initiator.loadClass("com.tencent.mobileqq.app.BusinessObserver")),
                ReplyMsgWithImg::handleAddAndSendMessage);

        return true;
    }

    private static void handleAddAndSendMessage(@NonNull XC_MethodHook.MethodHookParam param) throws Exception {
        Object message = param.args[0];
        String className = message.getClass().getName();
        int istroop = Reflex.getInstanceObject(message, "istroop", int.class);

        if ((className.contains("MessageForReplyText") || className.contains("MessageForText"))
                && (istroop == 1 || istroop == 0)) {
            boolean isReply = className.contains("MessageForReplyText");
            // 取出原始消息内容
            String text = Reflex.getInstanceObject(message, "msg", String.class);
            if (TextUtils.isEmpty(text)) {
                text = Reflex.getInstanceObject(message, "sb", CharSequence.class) + "";
            }
            // 如果包含特定的图片代码则会解析为Mix消息
            if (text.contains("[PicUrl=") && text.contains("]")) {
                Object sessionInfo;
                String uin = Reflex.getInstanceObject(message, "frienduin", String.class);
                if (istroop == 1) {
                    sessionInfo = SessionInfoImpl.forTroop(uin);
                } else {
                    sessionInfo = SessionInfoImpl.forFriend(uin);
                }

                Reflex.setInstanceObject(message, "msg", " ");
                Reflex.invokeVirtual(message, "prewrite", void.class);

                MessageInfoList[] msgInfoList = initMessageData(text);
                ArrayList<Object> recordList = new ArrayList<>();

                if (isReply) {
                    recordList.add(copyReplyMessage(message));
                }
                ArrayList<Object> atInfoList = new ArrayList<>();
                String textMsg = "";
                int length = 0;
                for (MessageInfoList mElement : msgInfoList) {
                    if (mElement.messageType == 1) {
                        Object mTextMsgData = buildMessageForText(uin, mElement.message);
                        length += mElement.message.length();
                        textMsg = textMsg + mElement.message;
                        //MLogCat.Print_Debug(""+mTextMsgData);
                        recordList.add(mTextMsgData);
                    } else if (mElement.messageType == 2) {
                        Object mPicMsgData = buildMessageForPic(sessionInfo, mElement.message);
                        //MLogCat.Print_Debug(""+mPicMsgData);
                        recordList.add(mPicMsgData);
                    } else if (mElement.messageType == 3) {
                        String AtText = "@" + ContactUtils.getTroopMemberNick(uin, mElement.message) + " ";
                        if (mElement.message.equals("0")) {
                            AtText = "@全体成员 ";
                        }

                        textMsg = textMsg + AtText;
                        Object mTextMsg = buildMessageForText(uin, AtText);
                        atInfoList.add(buildAtInfo(mElement.message, AtText, (short) length));
                        length += AtText.length();
                        recordList.add(mTextMsg);
                    }
                }
                // 发送消息并返回
                buildAndSendMessageForMixedMsg(sessionInfo, uin, recordList);
                param.setResult(null);
            }
        }
    }

    public static class MessageInfoList {

        public String message;
        public int messageType;
        public String extraData;
    }

    public static MessageInfoList[] initMessageData(String messageText) {
        if (!messageText.contains("[")) {
            MessageInfoList[] mList = new MessageInfoList[1];
            mList[0] = new MessageInfoList();
            mList[0].message = messageText;
            mList[0].messageType = 1;
            return mList;
        }
        ArrayList<MessageInfoList> list = new ArrayList<>();
        int index1 = -1;
        int index2 = -1;
        int LastEndPos = 0;
        index1 = messageText.indexOf("[");
        if (index1 != -1) {
            index2 = messageText.indexOf("]", index1 + 1);
        }
        while (index1 != -1) {
            if (index2 != -1) {
                String mTextStr = messageText.substring(LastEndPos, index1);
                if (!TextUtils.isEmpty(mTextStr)) {
                    MessageInfoList mTextInfo = new MessageInfoList();
                    mTextInfo.message = mTextStr;
                    mTextInfo.messageType = 1;
                    list.add(mTextInfo);
                }
                String ExtraData = messageText.substring(index1 + 1, index2);
                LastEndPos = index2 + 1;
                MessageInfoList mInfo = new MessageInfoList();
                if (ExtraData.startsWith("PicUrl=")) {
                    mInfo.messageType = 2;
                    mInfo.message = ExtraData.substring(7);
                    list.add(mInfo);
                } else if (ExtraData.startsWith("AtQQ=")) {
                    mInfo.messageType = 3;
                    mInfo.message = ExtraData.substring(5);
                    list.add(mInfo);
                } else {
                    mInfo.messageType = 1;
                    mInfo.message = "[" + ExtraData + "]";
                    list.add(mInfo);
                }
            } else {
                break;
            }
            index1 = messageText.indexOf("[", LastEndPos);
            if (index1 != -1) {
                index2 = messageText.indexOf("]", index1 + 1);
            }
        }
        if (LastEndPos < messageText.length()) {
            MessageInfoList info = new MessageInfoList();
            info.message = messageText.substring(LastEndPos);
            info.messageType = 1;
            list.add(info);
        }
        return list.toArray(new MessageInfoList[0]);
    }

    public static Object buildMessageForPic(Object sessionInfo, String picPath) throws ReflectiveOperationException, IOException {
        Method method = MMethod.FindMethod(Initiator.loadClass("com.tencent.mobileqq.activity.ChatActivityFacade"), "a",
                Initiator.loadClass("com.tencent.mobileqq.data.ChatMessage"), new Class[]{Initiator._QQAppInterface(), Initiator._SessionInfo(), String.class});
        Object picMsg = method.invoke(null, AppRuntimeHelper.getQQAppInterface(), sessionInfo, picPath);
        String picMd5 = IoUtils.calculateFileMd5HexString(new File(picPath), true);
        Reflex.setInstanceObject(picMsg, "md5", picMd5);
        Reflex.setInstanceObject(picMsg, "uuid", picMd5 + ".jpg");
        Reflex.setInstanceObject(picMsg, "localUUID", UUID.randomUUID().toString());
        Reflex.invokeVirtual(picMsg, "prewrite", void.class);
        return picMsg;
    }

    public static Object buildMessageForText(String groupUin, String text) throws ReflectiveOperationException {
        Method m = MMethod.FindMethod(Initiator.loadClass("com.tencent.mobileqq.service.message.MessageRecordFactory"), "a",
                Initiator.loadClass("com.tencent.mobileqq.data.MessageForText"),
                new Class[]{Initiator.loadClass("com.tencent.common.app.AppInterface"), String.class, String.class, String.class, int.class, byte.class,
                        byte.class, short.class, String.class});
        return m.invoke(null, AppRuntimeHelper.getQQAppInterface(), "", groupUin, AppRuntimeHelper.getAccount(), 1, (byte) 0, (byte) 0, (short) 0, text);
    }

    private static Method smMessageRecordFactory_BuildMixedMsg = null;

    public static void buildAndSendMessageForMixedMsg(Object sessionInfo, String uin, List<Object> messageList) throws ReflectiveOperationException {
        if (smMessageRecordFactory_BuildMixedMsg == null) {
            Class<?> kMessageForMixedMsg = Initiator.loadClass("com.tencent.mobileqq.data.MessageForMixedMsg");
            Class<?> kMessageRecordFactory = Initiator.loadClass("com.tencent.mobileqq.service.message.MessageRecordFactory");
            Method m = HostInfo.getVersionCode() < 5670
                    ? Reflex.findMethodOrNull(kMessageRecordFactory, kMessageForMixedMsg, "a",
                    Initiator._QQAppInterface(), String.class, String.class, int.class)
                    : Reflex.findMethodOrNull(kMessageRecordFactory, kMessageForMixedMsg, "g",
                            Initiator._QQAppInterface(), String.class, String.class, int.class);
            if (m == null) {
                m = Reflex.findMethodOrNull(kMessageRecordFactory, kMessageForMixedMsg, "h",
                        Initiator._QQAppInterface(), String.class, String.class, int.class);
            }
            m.setAccessible(true);
            smMessageRecordFactory_BuildMixedMsg = m;
        }
        Object mixMessageRecord = smMessageRecordFactory_BuildMixedMsg.invoke(null,
                AppRuntimeHelper.getQQAppInterface(), uin, AppRuntimeHelper.getAccount(), 1);
        Reflex.setInstanceObject(mixMessageRecord, "msgElemList", messageList);
        mixMessageRecord = Reflex.invokeVirtual(mixMessageRecord, "rebuildMixedMsg", Initiator._MessageRecord());
        forwardMixedMsg(sessionInfo, mixMessageRecord);
    }

    // 图文消息发送
    public static void forwardMixedMsg(Object sessionInfo, Object messageRecord) throws ReflectiveOperationException {
        Class<?> kReplyMsgSender = Initiator.loadClass("com.tencent.mobileqq.replymsg.ReplyMsgSender");
        Method method = MMethod.FindMethod(kReplyMsgSender, "a", void.class,
                new Class[]{Initiator._QQAppInterface(),
                        Initiator.loadClass("com.tencent.mobileqq.data.MessageForMixedMsg"),
                        Initiator._SessionInfo(), int.class});
        Object instance = Reflex.invokeStatic(kReplyMsgSender, "a", kReplyMsgSender);
        method.invoke(instance, AppRuntimeHelper.getQQAppInterface(), messageRecord, sessionInfo, 0);
    }

    public static Object buildAtInfo(String uin, String text, short startPos) throws ReflectiveOperationException {
        Object at = Reflex.newInstance(Initiator.loadClass("com.tencent.mobileqq.data.AtTroopMemberInfo"));
        if (uin.isEmpty()) {
            return null;
        }
        if ("0".equals(uin)) {
            Reflex.setInstanceObject(at, "flag", (byte) 1);
            Reflex.setInstanceObject(at, "startPos", startPos);
            Reflex.setInstanceObject(at, "textLen", (short) text.length());
        } else {
            Reflex.setInstanceObject(at, "uin", Long.parseLong(uin));
            Reflex.setInstanceObject(at, "startPos", startPos);
            Reflex.setInstanceObject(at, "textLen", (short) text.length());
        }
        return at;
    }

    public static Object buildMessageForReplyText(Object courceMsg, String messageContent, String troopUin) throws ReflectiveOperationException {
        int isTroop = Reflex.getInstanceObject(courceMsg, "istroop", int.class);
        String uins = Reflex.getInstanceObject(courceMsg, "senderuin", String.class);
        Object appInterface = AppRuntimeHelper.getQQAppInterface();
        Method sourceInfo = MMethod.FindMethod(Initiator.loadClass("com.tencent.mobileqq.activity.aio.reply.ReplyMsgUtils"), "a",
                Initiator.loadClass("com.tencent.mobileqq.data.MessageForReplyText$SourceMsgInfo"),
                new Class[]{Initiator._QQAppInterface(), Initiator.loadClass("com.tencent.mobileqq.data.ChatMessage"), int.class, long.class, String.class});
        Object sourceInfoObj = sourceInfo.invoke(null, appInterface, courceMsg, 0, Long.parseLong(uins),
                ContactUtils.getTroopName(Reflex.getInstanceObject(courceMsg, "frienduin", String.class)));
        Method m = MMethod.FindMethod(Initiator.loadClass("com.tencent.mobileqq.service.message.MessageRecordFactory"), "a",
                Initiator.loadClass("com.tencent.mobileqq.data.MessageForReplyText"), new Class[]{Initiator._QQAppInterface(), String.class, int.class,
                        Initiator.loadClass("com.tencent.mobileqq.data.MessageForReplyText$SourceMsgInfo"), String.class});
        Object msgObj = m.invoke(null, appInterface, troopUin, isTroop, sourceInfoObj, messageContent);
        return msgObj;
    }

    public static Object copyReplyMessage(Object source) throws ReflectiveOperationException {
        Method m = Reflex.findMethod(Initiator.loadClass("com.tencent.mobileqq.service.message.MessageRecordFactory"),
                Initiator.loadClass("com.tencent.mobileqq.data.MessageForReplyText"), "a", Initiator._QQAppInterface(), String.class, int.class,
                Initiator.loadClass("com.tencent.mobileqq.data.MessageForReplyText$SourceMsgInfo"), String.class);
        String uin = Reflex.getInstanceObject(source, "frienduin", String.class);
        Object sourceMsg = Reflex.getInstanceObject(source, "mSourceMsgInfo",
                Initiator.loadClass("com.tencent.mobileqq.data.MessageForReplyText$SourceMsgInfo"));
        return m.invoke(null, AppRuntimeHelper.getQQAppInterface(), uin, 2, sourceMsg, "");
    }

    public static void addEditText(@NonNull EditText ed, @NonNull String text) {
        int pos = ed.getSelectionStart();
        Editable e = ed.getText();
        e.insert(pos, text);
        ed.setText(e);
        ed.setSelection(pos + text.length());
    }

    @Override
    public void onInitBaseChatPie(@NonNull Object baseChatPie, @NonNull ViewGroup aioRootView, @NonNull Parcelable session, @NonNull Context ctx,
                                  @NonNull AppRuntime rt) {
        mBaseChatPie = new WeakReference<>(Objects.requireNonNull(baseChatPie, "baseChatPie is null"));
        EditText input = aioRootView.findViewById(ctx.getResources().getIdentifier("input", "id", ctx.getPackageName()));
        if (input != null) {
            mInputEditText = new WeakReference<>(input);
        }
    }

    private static Method sIsNowReplyingMethod = null;
    private static Field sBaseChatPie_HelperProvider = null;

    // 判断当前输入框是否是正在回复某一条消息的状态
    public static boolean isNowReplying(@NonNull Object baseChatPie) throws ReflectiveOperationException {
        Class<?> kHelperProvider = Initiator.loadClass("com.tencent.mobileqq.activity.aio.helper.HelperProvider");
        if (sBaseChatPie_HelperProvider == null) {
            Field f = Reflex.getFirstNSFFieldByType(Initiator._BaseChatPie(), kHelperProvider);
            Objects.requireNonNull(f, "BaseChatPie.?:HelperProvider not found");
            f.setAccessible(true);
            sBaseChatPie_HelperProvider = f;
        }
        Object helperProvider = sBaseChatPie_HelperProvider.get(baseChatPie);
        if (sIsNowReplyingMethod == null) {
            for (Method sm : helperProvider.getClass().getSuperclass().getSuperclass().getDeclaredMethods()) {
                if (sm.getName().equals("a") && sm.getParameterTypes().length == 1 && sm.getParameterTypes()[0] == int.class) {
                    if (sm.getReturnType() != Dialog.class && sm.getReturnType() != void.class && sm.getReturnType() != boolean.class) {
                        sIsNowReplyingMethod = sm;
                        break;
                    }
                }
            }
        }
        Object replyHelper = sIsNowReplyingMethod.invoke(helperProvider, 119);
        Object sourceInfo = Reflex.invokeVirtual(replyHelper, "a", Initiator.loadClass("com.tencent.mobileqq.data.MessageForReplyText$SourceMsgInfo"));
        return sourceInfo != null;
    }

    @Nullable
    @Override
    public Step[] makePreparationSteps() {
        return InputButtonHookDispatcher.INSTANCE.makePreparationSteps();
    }

    @Override
    public boolean isAvailable() {
        return super.isAvailable();
    }

    @Override
    public boolean isPreparationRequired() {
        return InputButtonHookDispatcher.INSTANCE.isPreparationRequired();
    }
}
