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

package cc.hicore.hook;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.bridge.ContactUtils;
import io.github.qauxv.bridge.SessionInfoImpl;
import io.github.qauxv.core.HookInstaller;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.router.decorator.IBaseChatPieInitDecorator;
import io.github.qauxv.router.dispacher.InputButtonHookDispatcher;
import io.github.qauxv.step.Step;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.IoUtils;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.Toasts;
import io.github.qauxv.util.dexkit.CGuildArkHelper;
import io.github.qauxv.util.dexkit.CGuildHelperProvider;
import io.github.qauxv.util.dexkit.CMessageRecordFactory;
import io.github.qauxv.util.dexkit.CReplyMsgSender;
import io.github.qauxv.util.dexkit.CReplyMsgUtils;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.NContactUtils_getBuddyName;
import io.github.qauxv.util.dexkit.NContactUtils_getDiscussionMemberShowName;
import io.github.qauxv.util.dexkit.NPhotoListPanel_resetStatus;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
        super(new DexKitTarget[]{
                CGuildHelperProvider.INSTANCE,
                CGuildArkHelper.INSTANCE,
                CMessageRecordFactory.INSTANCE,
                CReplyMsgUtils.INSTANCE,
                CReplyMsgSender.INSTANCE,
                NPhotoListPanel_resetStatus.INSTANCE,
                NContactUtils_getDiscussionMemberShowName.INSTANCE,
                NContactUtils_getBuddyName.INSTANCE,
        });
    }

    @NonNull
    @Override
    public String getName() {
        return "回复带图";
    }

    @Nullable
    @Override
    public CharSequence getDescription() {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append("回复消息发送时一并把图片带上，在不支持的版本上行为是 undefined behavior. ");
        String warn = "注意: 此功能会导致文本框上方的表情推荐点击无效，介意者慎用。";
        int color = 0xFFFF5252;
        int start = sb.length();
        sb.append(warn);
        sb.setSpan(new android.text.style.ForegroundColorSpan(color), start, sb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY;
    }

    private WeakReference<EditText> mInputEditText = null;
    private WeakReference<Object> mBaseChatPie = null;

    private static Class<?> kHelperProvider = null;
    private static Class<?> kIHelper = null;
    private static Class<?> kMessageRecordFactory = null;
    private static Class<?> kReplyMsgUtils = null;
    private static Class<?> kReplyMsgSender = null;
    private static Method pfnPhotoListPanel_resetStatus = null;
    private static Method sGetHelperMethod = null;
    private static Field sBaseChatPie_HelperProvider = null;
    // guessed name
    private static Method mReplyHelper_getSourceInfo = null;

    @Override
    protected boolean initOnce() throws Exception {
        kHelperProvider = Initiator.load("com.tencent.mobileqq.activity.aio.helper.HelperProvider");
        if (kHelperProvider == null) {
            kHelperProvider = Objects.requireNonNull(DexKit.loadClassFromCache(CGuildHelperProvider.INSTANCE), "CGuildHelperProvider.INSTANCE")
                    .getSuperclass();
            Objects.requireNonNull(kHelperProvider);
        }
        kIHelper = Initiator.load("com.tencent.mobileqq.activity.aio.helper.IHelper");
        if (kIHelper == null) {
            Class<?> kGuildArkHelper = Objects.requireNonNull(DexKit.loadClassFromCache(CGuildArkHelper.INSTANCE), "CGuildArkHelper.INSTANCE");
            // expect 1 interface
            Class<?>[] interfaces = kGuildArkHelper.getInterfaces();
            if (interfaces.length != 1) {
                throw new IllegalStateException("GuildArkHelper must implement only 1 interface: IHelper");
            }
            kIHelper = interfaces[0];
        }
        kMessageRecordFactory = Objects.requireNonNull(DexKit.loadClassFromCache(CMessageRecordFactory.INSTANCE), "CMessageRecordFactory.INSTANCE");
        kReplyMsgUtils = Objects.requireNonNull(DexKit.loadClassFromCache(CReplyMsgUtils.INSTANCE), "CReplyMsgUtils.INSTANCE");
        kReplyMsgSender = Objects.requireNonNull(DexKit.loadClassFromCache(CReplyMsgSender.INSTANCE), "CReplyMsgSender.INSTANCE");
        pfnPhotoListPanel_resetStatus = Objects.requireNonNull(DexKit.loadMethodFromCache(NPhotoListPanel_resetStatus.INSTANCE),
                "NPhotoListPanel_resetStatus.INSTANCE");

        sBaseChatPie_HelperProvider = Reflex.getFirstNSFFieldByType(Initiator._BaseChatPie(), kHelperProvider);
        sBaseChatPie_HelperProvider.setAccessible(true);
        Objects.requireNonNull(sBaseChatPie_HelperProvider, "BaseChatPie.?:HelperProvider not found");

        Class<?> kReplyHelper = Initiator.loadClass("com.tencent.mobileqq.activity.aio.helper.ReplyHelper");

        Class<?> kSourceMsgInfo = Initiator.loadClass("com.tencent.mobileqq.data.MessageForReplyText$SourceMsgInfo");
        for (Method m : kReplyHelper.getDeclaredMethods()) {
            if (m.getReturnType() == kSourceMsgInfo && m.getParameterTypes().length == 0 && m.getModifiers() == Modifier.PUBLIC) {
                if ("a".equals(m.getName()) || "d".equals(m.getName()) || "h".equals(m.getName())) {
                    mReplyHelper_getSourceInfo = m;
                    break;
                }
            }
        }
        Objects.requireNonNull(mReplyHelper_getSourceInfo, "ReplyHelper.getSourceInfo not found");

        for (Method sm : kHelperProvider.getSuperclass().getDeclaredMethods()) {
            if (sm.getParameterTypes().length == 1 && sm.getParameterTypes()[0] == int.class) {
                if (sm.getReturnType() == kIHelper) {
                    sGetHelperMethod = sm;
                    break;
                }
            }
        }
        Objects.requireNonNull(sGetHelperMethod, "HelperProvider.getHelper not found");

        Method sendCustomEmotion = Reflex.findMethod(Initiator.loadClass("com.tencent.mobileqq.emoticonview.sender.CustomEmotionSenderUtil"), void.class,
                "sendCustomEmotion", Initiator._BaseQQAppInterface(), Context.class, Initiator._BaseSessionInfo(),
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

        HookUtils.hookBeforeIfEnabled(this, Reflex.findSingleMethod(
                Initiator.loadClass("com.tencent.mobileqq.activity.aio.photo.PhotoListPanel"),
                boolean.class, false,
                Initiator._BaseChatPie(), List.class, boolean.class), param -> {
            // Log.d("ReplyMsgWithImg PhotoListPanel.a");
            Object chatPie = param.args[0];
            if (chatPie == null) {
                chatPie = mBaseChatPie == null ? null : mBaseChatPie.get();
            }
            if (chatPie == null) {
                Toasts.error(null, "chatPie is null");
                Log.d("ReplyMsgWithImg PhotoListPanel.a chatPie is null");
                return;
            }

            // TODO: NT版本请用getSessionByAIOParam()
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
                        pfnPhotoListPanel_resetStatus.invoke(param.thisObject, true);
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
                var textMsg = new StringBuilder();
                int length = 0;
                for (MessageInfoList mElement : msgInfoList) {
                    if (mElement.messageType == 1) {
                        Object mTextMsgData = buildMessageForText(uin, mElement.message);
                        length += mElement.message.length();
                        textMsg.append(mElement.message);
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

                        textMsg.append(AtText);
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
        // \((final)? QQAppInterface qQAppInterface\, (final)? SessionInfo sessionInfo, String str\) \{
        Method method = Reflex.findSingleMethod(Initiator.loadClass("com.tencent.mobileqq.activity.ChatActivityFacade"),
                Initiator.loadClass("com.tencent.mobileqq.data.ChatMessage"), false,
                Initiator._QQAppInterface(), Initiator._SessionInfo(), String.class);
        Object picMsg = method.invoke(null, AppRuntimeHelper.getQQAppInterface(), sessionInfo, picPath);
        String picMd5 = IoUtils.calculateFileMd5HexString(new File(picPath), true);
        Reflex.setInstanceObject(picMsg, "md5", picMd5);
        Reflex.setInstanceObject(picMsg, "uuid", picMd5 + ".jpg");
        Reflex.setInstanceObject(picMsg, "localUUID", UUID.randomUUID().toString());
        Reflex.invokeVirtual(picMsg, "prewrite", void.class);
        return picMsg;
    }

    public static Object buildMessageForText(String groupUin, String text) throws ReflectiveOperationException {
        Method m = Reflex.findSingleMethod(kMessageRecordFactory,
                Initiator.loadClass("com.tencent.mobileqq.data.MessageForText"), false,
                Initiator.loadClass("com.tencent.common.app.AppInterface"),
                String.class, String.class, String.class, int.class, byte.class, byte.class, short.class, String.class);
        return m.invoke(null, AppRuntimeHelper.getQQAppInterface(), "", groupUin, AppRuntimeHelper.getAccount(), 1, (byte) 0, (byte) 0, (short) 0, text);
    }

    private static Method smMessageRecordFactory_BuildMixedMsg = null;

    public static void buildAndSendMessageForMixedMsg(Object sessionInfo, String uin, List<Object> messageList) throws ReflectiveOperationException {
        if (smMessageRecordFactory_BuildMixedMsg == null) {
            Class<?> kMessageForMixedMsg = Initiator.loadClass("com.tencent.mobileqq.data.MessageForMixedMsg");
            Method m = Reflex.findSingleMethod(kMessageRecordFactory, kMessageForMixedMsg, false,
                    Initiator._QQAppInterface(), String.class, String.class, int.class);
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
        Method method = Reflex.findSingleMethod(kReplyMsgSender, void.class, false,
                Initiator._QQAppInterface(),
                Initiator.loadClass("com.tencent.mobileqq.data.MessageForMixedMsg"),
                Initiator._SessionInfo(), int.class);
        Object instance = Reflex.findSingleMethod(kReplyMsgSender, kReplyMsgSender, false).invoke(null);
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
        Method sourceInfo = Reflex.findSingleMethod(kReplyMsgUtils,
                Initiator.loadClass("com.tencent.mobileqq.data.MessageForReplyText$SourceMsgInfo"), false,
                Initiator._QQAppInterface(), Initiator.loadClass("com.tencent.mobileqq.data.ChatMessage"), int.class, long.class, String.class);
        Object sourceInfoObj = sourceInfo.invoke(null, appInterface, courceMsg, 0, Long.parseLong(uins),
                ContactUtils.getTroopName(Reflex.getInstanceObject(courceMsg, "frienduin", String.class)));
        Method m = Reflex.findSingleMethod(kMessageRecordFactory,
                Initiator.loadClass("com.tencent.mobileqq.data.MessageForReplyText"), false,
                Initiator._QQAppInterface(), String.class, int.class,
                Initiator.loadClass("com.tencent.mobileqq.data.MessageForReplyText$SourceMsgInfo"), String.class);
        Object msgObj = m.invoke(null, appInterface, troopUin, isTroop, sourceInfoObj, messageContent);
        return msgObj;
    }

    public static Object copyReplyMessage(Object source) throws ReflectiveOperationException {
        Method m = Reflex.findSingleMethod(kMessageRecordFactory,
                Initiator.loadClass("com.tencent.mobileqq.data.MessageForReplyText"), false,
                Initiator._QQAppInterface(), String.class, int.class,
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
    public void onInitBaseChatPie(@NonNull Object baseChatPie, @NonNull ViewGroup aioRootView, @Nullable Parcelable session, @NonNull Context ctx,
            @NonNull AppRuntime rt) {
        mBaseChatPie = new WeakReference<>(Objects.requireNonNull(baseChatPie, "baseChatPie is null"));
        EditText input = aioRootView.findViewById(ctx.getResources().getIdentifier("input", "id", ctx.getPackageName()));
        if (input != null) {
            mInputEditText = new WeakReference<>(input);
        }
    }

    // 判断当前输入框是否是正在回复某一条消息的状态
    public static boolean isNowReplying(@NonNull Object baseChatPie) throws ReflectiveOperationException {
        Object helperProvider = sBaseChatPie_HelperProvider.get(baseChatPie);
        // HelperProvider.ID_AIO_REPLY = 119
        Object replyHelper = sGetHelperMethod.invoke(helperProvider, 119);
        Object sourceInfo = mReplyHelper_getSourceInfo.invoke(replyHelper);
        return sourceInfo != null;
    }

    @Nullable
    @Override
    public Step[] makePreparationSteps() {
        return HookInstaller.stepsOf(
                super.makePreparationSteps(),
                InputButtonHookDispatcher.INSTANCE.makePreparationSteps()
        );
    }

    @Override
    public boolean isPreparationRequired() {
        return InputButtonHookDispatcher.INSTANCE.isPreparationRequired() || super.isPreparationRequired();
    }
}
