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
package cc.ioctl.hook.msg;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static io.github.qauxv.util.Initiator._C2CMessageProcessor;
import static io.github.qauxv.util.Initiator._QQMessageFacade;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import cc.hicore.QApp.QAppUtils;
import cc.ioctl.fragment.RevokeMsgConfigFragment;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord;
import io.github.qauxv.activity.SettingsUiFragmentHostActivity;
import io.github.qauxv.base.IUiItemAgent;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.bridge.ContactUtils;
import io.github.qauxv.bridge.QQMessageFacade;
import io.github.qauxv.bridge.RevokeMsgInfoImpl;
import io.github.qauxv.bridge.kernelcompat.ContactCompat;
import io.github.qauxv.bridge.kernelcompat.KernelMsgServiceCompat;
import io.github.qauxv.bridge.ntapi.ChatTypeConstants;
import io.github.qauxv.bridge.ntapi.MsgServiceHelper;
import io.github.qauxv.bridge.ntapi.NtGrayTipHelper;
import io.github.qauxv.bridge.ntapi.RelationNTUinAndUidApi;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonConfigFunctionHook;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.dexkit.CMessageRecordFactory;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.NContactUtils_getBuddyName;
import io.github.qauxv.util.dexkit.NContactUtils_getDiscussionMemberShowName;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import kotlin.Unit;
import kotlin.jvm.functions.Function3;
import kotlin.jvm.internal.Intrinsics;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import mqq.app.AppRuntime;

/**
 * @author fkzhang Created by fkzhang on 1/20/2016.
 * <p>
 * Changes by cinit:
 * <p>
 * 2020/03/08 Sun.20:33 Minor changes at GreyTip
 * <p>
 * 2020/04/08 Tue.23:21 Use RevokeMsgInfoImpl for ease, wanna cry
 * <p>
 * 2023-06-16 Fri.12:40 Initial support for NT kernel.
 * <p>
 * 2023-07-12 Mon.23:12 Basic support for NT kernel.
 */
@FunctionHookEntry
@UiItemAgentEntry
public class RevokeMsgHook extends CommonConfigFunctionHook {

    public static final RevokeMsgHook INSTANCE = new RevokeMsgHook();
    private Object mQQMsgFacade = null;

    private MutableStateFlow<String> mState = null;
    private static final String KEY_KEEP_SELF_REVOKE_MSG = "RevokeMsgHook.KEY_KEEP_SELF_REVOKE_MSG";
    private static final String KEY_SHOW_SHMSGSEQ = "RevokeMsgHook.KEY_SHOW_SHMSGSEQ";

    private RevokeMsgHook() {
        //FIXME: is MSF really necessary?
        super(SyncUtils.PROC_MAIN | SyncUtils.PROC_MSF, new DexKitTarget[]{
                CMessageRecordFactory.INSTANCE,
                NContactUtils_getDiscussionMemberShowName.INSTANCE,
                NContactUtils_getBuddyName.INSTANCE
        });
    }

    @NonNull
    @Override
    public String getName() {
        return "防撤回";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.MESSAGE_CATEGORY;
    }

    @NonNull
    @Override
    public MutableStateFlow<String> getValueState() {
        if (mState == null) {
            mState = StateFlowKt.MutableStateFlow(getStateText());
        }
        Intrinsics.checkExpressionValueIsNotNull(mState, "mState ?: MutableStateFlow(getStateText())");
        return mState;
    }

    private String getStateText() {
        if (isEnabled()) {
            return "已启用";
        } else {
            return "禁用";
        }
    }

    @NonNull
    @Override
    public Function3<IUiItemAgent, Activity, View, Unit> getOnUiItemClickListener() {
        return (agent, activity, view) -> {
            SettingsUiFragmentHostActivity.startFragmentWithContext(activity, RevokeMsgConfigFragment.class, null);
            return Unit.INSTANCE;
        };
    }

    public boolean isKeepSelfMsgEnabled() {
        return false;
        // ConfigManager cfg = ConfigManager.getDefaultConfig();
        // return cfg.getBoolean(KEY_KEEP_SELF_REVOKE_MSG, false);
    }

    public void setKeepSelfMsgEnabled(boolean enabled) {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        cfg.putBoolean(KEY_KEEP_SELF_REVOKE_MSG, enabled);
    }

    public boolean isShowShmsgseqEnabled() {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        return cfg.getBoolean(KEY_SHOW_SHMSGSEQ, true);
    }

    public void setShowShmsgseqEnabled(boolean enabled) {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        cfg.putBoolean(KEY_SHOW_SHMSGSEQ, enabled);
    }

    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);
        if (mState != null) {
            mState.setValue(getStateText());
        }
    }

    @Override
    public boolean initOnce() throws Exception {
        boolean isSuccess = true;
        if (QAppUtils.isQQnt()) {
            isSuccess = nativeInitNtKernelRecallMsgHook();
        }
        // The method is still there, even on NT.
        // I decided to hook them as long as they are there.
        // There should only be one method with such signature.
        Method revokeMsg = null;
        for (Method m : _QQMessageFacade().getDeclaredMethods()) {
            if (m.getReturnType().equals(void.class)) {
                Class<?>[] argt = m.getParameterTypes();
                if (argt.length == 2 && argt[0].equals(ArrayList.class) && argt[1].equals(boolean.class)) {
                    revokeMsg = m;
                    break;
                }
            }
        }
        HookUtils.hookBeforeIfEnabled(this, revokeMsg, -10086, param -> {
            mQQMsgFacade = param.thisObject;
            ArrayList<?> list = (ArrayList<?>) param.args[0];
            param.setResult(null);
            if (list == null || list.isEmpty()) {
                return;
            }
            for (Object revokeMsgInfo : list) {
                try {
                    onRevokeMsgLegacy(revokeMsgInfo);
                } catch (Exception | LinkageError | AssertionError t) {
                    traceError(t);
                }
            }
            list.clear();
        });
        return isSuccess;
    }

    private native boolean nativeInitNtKernelRecallMsgHook();

    private void onRevokeMsgLegacy(Object revokeMsgInfo) throws Exception {
        RevokeMsgInfoImpl info = new RevokeMsgInfoImpl((Parcelable) revokeMsgInfo);
        // 1. C2C chat session, istroop=0, RevokeMsgInfo is always the same on both side. e.g.
        // istroop=0, shmsgseq=***(valid),
        // friendUin=***(counterpart of revoker), msgUid=***(valid), fromUin=***(revoker), time=***(valid),
        // sendUin='0', authorUin='null', opType=0
        // 2. For troop chat session. RevokeMsgInfo is always the same on 3 sides(revoker, revokee, participants).
        // 2.1 Troop spontaneous revocation(revoker is msg sender, opType=0). e.g.
        // istroop=1, shmsgseq=***(valid), friendUin=***(troop), msgUid=0, fromUin=***(revoker), time=***(valid),
        // sendUin='null', authorUin=***(original msg sender), opType=0
        // 2.2 Troop revocation by admin(revoker is troop admin, opType=1). e.g.
        // istroop=1, shmsgseq=***(valid), friendUin=***(troop), msgUid=0, fromUin=***(revoker), time=***(valid),
        // sendUin='null', authorUin=***(original msg sender), opType=1
        String selfUin = AppRuntimeHelper.getAccount();
        String revokerUin = info.fromUin;
        int istroop = info.istroop;
        long shmsgseq = info.shmsgseq;
        long time = info.time;
        String aioSessionUin;
        AppRuntimeHelper.checkUinValid(selfUin);
        AppRuntimeHelper.checkUinValid(revokerUin);
        if (istroop == 0) {
            // C2C PM
            if (selfUin.equals(info.friendUin)) {
                aioSessionUin = revokerUin;
            } else {
                aioSessionUin = info.friendUin;
            }
        } else if (istroop == 1) {
            // Troop
            aioSessionUin = info.friendUin;
        } else {
            // XXX: maybe confession chat? 3000: temp C2C chat? guild?
            // XXX: ??? 1000 ???
            throw new IllegalStateException("onRevokeMsg, istroop=" + istroop);
        }

        if (!isKeepSelfMsgEnabled() && selfUin.equals(revokerUin)) {
            return;
        }

        Object msgObject = getMessageLegacy(aioSessionUin, istroop, shmsgseq, info.msgUid);
        // long id = getMessageUid(msgObject);
        long newMsgUid;
        if (info.msgUid != 0) {
            newMsgUid = info.msgUid + new Random().nextInt();
        } else {
            newMsgUid = 0;
        }

        Object revokeGreyTip = null;
        if (istroop == 0) {
            // C2C PM
            String friendUin = aioSessionUin;
            String greyMsg;
            String revokerPron = selfUin.equals(revokerUin) ? "你" : "对方";
            if (msgObject == null) {
                if (isShowShmsgseqEnabled()) {
                    greyMsg = revokerPron + "撤回了一条消息（没收到）, shmsgseq: " + shmsgseq;
                } else {
                    greyMsg = revokerPron + "撤回了一条消息（没收到）";
                }
            } else if (Reflex.isCallingFrom(_C2CMessageProcessor().getName())) {
                return;
            } else {
                String message = getMessageContentStripped(msgObject);
                int msgtype = getMessageType(msgObject);
                boolean hasMsgInfo = false;
                greyMsg = revokerPron + "尝试撤回一条消息";
                if (msgtype == -1000 /*text msg*/) {
                    if (!TextUtils.isEmpty(message)) {
                        greyMsg += ": " + message;
                    }
                }
                if (!hasMsgInfo && isShowShmsgseqEnabled()) {
                    greyMsg += ", shmsgseq: " + shmsgseq;
                }
            }
            revokeGreyTip = createBarePlainGreyTip(friendUin, istroop, revokerUin, time + 1, greyMsg, newMsgUid, shmsgseq);
        } else if (istroop == 1) {
            String authorUin = info.authorUin;
            AppRuntimeHelper.checkUinValid(authorUin);
            String troopUin = aioSessionUin;
            if (revokerUin.equals(authorUin)) {
                // Troop spontaneous revocation
                String revokerNick = ContactUtils.getTroopMemberNick(troopUin, revokerUin);
                String greyMsg = "\"" + revokerNick + "\u202d\"";
                if (msgObject != null) {
                    greyMsg += "尝试撤回一条消息";
                    String message = getMessageContentStripped(msgObject);
                    int msgtype = getMessageType(msgObject);
                    boolean hasMsgInfo = false;
                    if (msgtype == -1000 /*text msg*/) {
                        if (!TextUtils.isEmpty(message)) {
                            greyMsg += ": " + message;
                            hasMsgInfo = true;
                        }
                    }
                    if (!hasMsgInfo && isShowShmsgseqEnabled()) {
                        greyMsg += ", shmsgseq: " + shmsgseq;
                    }
                } else {
                    if (isShowShmsgseqEnabled()) {
                        greyMsg += "撤回了一条消息（没收到）, shmsgseq: " + shmsgseq;
                    } else {
                        greyMsg += "撤回了一条消息（没收到）";
                    }
                }
                revokeGreyTip = createBareHighlightGreyTip(aioSessionUin, istroop, revokerUin, time + 1, greyMsg, newMsgUid, shmsgseq);
                addHightlightItem(revokeGreyTip, 1, 1 + revokerNick.length(), createTroopMemberHighlightItem(revokerUin));
            } else {
                // Troop revocation by admin or owner
                String revokerNick = ContactUtils.getTroopMemberNick(troopUin, revokerUin);
                String authorNick = ContactUtils.getTroopMemberNick(troopUin, authorUin);
                if (msgObject == null) {
                    String greyMsg = "\"" + revokerNick + "\u202d\"撤回了\"" + authorNick + "\u202d\"的消息(没收到)";
                    if (isShowShmsgseqEnabled()) {
                        greyMsg += ", shmsgseq: " + shmsgseq;
                    }
                    revokeGreyTip = createBareHighlightGreyTip(aioSessionUin, istroop, revokerUin, time + 1, greyMsg, newMsgUid, shmsgseq);
                    addHightlightItem(revokeGreyTip, 1, 1 + revokerNick.length(), createTroopMemberHighlightItem(revokerUin));
                    addHightlightItem(revokeGreyTip, 1 + revokerNick.length() + 1 + 5,
                            1 + revokerNick.length() + 1 + 5 + authorNick.length(),
                            createTroopMemberHighlightItem(authorUin));
                } else {
                    String greyMsg = "\"" + revokerNick + "\u202d\"尝试撤回\"" + authorNick + "\u202d\"的消息";
                    String message = getMessageContentStripped(msgObject);
                    int msgtype = getMessageType(msgObject);
                    boolean hasMsgInfo = false;
                    if (msgtype == -1000 /*text msg*/) {
                        if (!TextUtils.isEmpty(message)) {
                            greyMsg += ": " + message;
                            hasMsgInfo = true;
                        }
                    }
                    if (!hasMsgInfo && isShowShmsgseqEnabled()) {
                        greyMsg += ", shmsgseq: " + shmsgseq;
                    }
                    revokeGreyTip = createBareHighlightGreyTip(aioSessionUin, istroop, revokerUin, time + 1, greyMsg, newMsgUid, shmsgseq);
                    addHightlightItem(revokeGreyTip, 1, 1 + revokerNick.length(),
                            createTroopMemberHighlightItem(revokerUin));
                    addHightlightItem(revokeGreyTip, 1 + revokerNick.length() + 1 + 6,
                            1 + revokerNick.length() + 1 + 6 + authorNick.length(),
                            createTroopMemberHighlightItem(authorUin));
                }
            }
        }
        if (revokeGreyTip != null) {
            List<Object> list = new ArrayList<>(1);
            list.add(revokeGreyTip);
            QQMessageFacade.commitMessageRecordList(list, selfUin);
        }
    }

    /**
     * Called NtRecallMsgHook.cc from native.
     * <p>
     * We are not allowed to throw any exception in this method.
     */
    @Keep
    private static void handleRecallSysMsgFromNtKernel(int chatType, String fromUid, String recallOpUid, String msgAuthorUid,
            String toUid, long random64, long timeSeconds, long msgUid, long msgSeq, int msgClientSeq) {
        SyncUtils.async(() -> {
            try {
                String selfUid = RelationNTUinAndUidApi.getUidFromUin(AppRuntimeHelper.getAccount());
                if (TextUtils.isEmpty(selfUid)) {
                    Log.e("onRecallSysMsgForNT fatal: selfUid is empty");
                    return;
                }
                String peerUid;
                if (selfUid.equals(fromUid)) {
                    // msg is revoked by myself
                    peerUid = toUid;
                } else {
                    peerUid = fromUid;
                }
                RevokeMsgHook.INSTANCE.onRecallSysMsgForNT(chatType, peerUid, recallOpUid, msgAuthorUid, toUid,
                        random64, timeSeconds, msgUid, msgSeq, msgClientSeq);
            } catch (Exception | LinkageError | AssertionError e) {
                RevokeMsgHook.INSTANCE.traceError(e);
            }
        });
    }

    private void onRecallSysMsgForNT(int chatType, String peerUid, String recallOpUid, String msgAuthorUid, String toUid,
            long random64, long timeSeconds, long msgUid, long msgSeq, int msgClientSeq) throws ReflectiveOperationException {
        if (TextUtils.isEmpty(peerUid)) {
            Log.e("onRecallSysMsgForNT fatal: peerUid is empty");
            return;
        }
        if (chatType != 1 && chatType != 2) {
            Log.e("onRecallSysMsgForNT fatal: chatType is not c2c or troop");
            return;
        }
        // for debug log
//        Log.d("onRecallSysMsgForNT: chatType=" + chatType + ", peerUid=" + peerUid + ", recallOpUid=" + recallOpUid
//                + ", msgAuthorUid=" + msgAuthorUid + ", toUid=" + toUid + ", random64=" + random64 + ", timeSeconds=" + timeSeconds
//                + ", msgUid=" + msgUid + ", msgSeq=" + msgSeq + ", msgClientSeq=" + msgClientSeq);
        // operatorUin may be empty, in the case when in a group chat, someone recalled a message by an admin or owner,
        // but your NT kernel are not so familiar with the admin or owner, and it doesn't know the uin of the admin or owner.
        String selfUin = AppRuntimeHelper.getAccount();
        String selfUid = RelationNTUinAndUidApi.getUidFromUin(selfUin);
        if (TextUtils.isEmpty(selfUid)) {
            Log.e("onRecallSysMsgForNT fatal: selfUid is empty");
            return;
        }
        if (TextUtils.isEmpty(msgAuthorUid)) {
            Log.e("onRecallSysMsgForNT fatal: msgAuthorUid is empty");
            return;
        }
        if (chatType == 2 && !Objects.equals(peerUid, toUid)) {
            Log.w("!!! onRecallSysMsgForNT potential bug: chatType=" + chatType + ", peerUid=" + peerUid + ", toUid=" + toUid + ", selfUid=" + selfUid);
        }
        ContactCompat contact = new ContactCompat(chatType, peerUid, "");
        AppRuntime app = AppRuntimeHelper.getAppRuntime();
        KernelMsgServiceCompat kmsgSvc = MsgServiceHelper.getKernelMsgService(app);
        // I don't know why, but...
        // IKernelMsgService.getMsgsByMsgId callback: result=0, errMsg=null, msgList=[](empty list)
        // IKernelMsgService.getMsgsBySeqList does not invoke callback at all, and no log
        // Only kmsgSvc.getSingleMsg works.
        kmsgSvc.getSingleMsg(contact, msgSeq, ((queryResult, errMsg, msgList) -> {
            try {
                MsgRecord msgObject = null;
                if (queryResult == 0 && msgList != null && !msgList.isEmpty()) {
                    msgObject = msgList.get(0);
                } else if (queryResult == 0) {
                    Log.d("onRecallSysMsgForNT: msg not found, msgSeq=" + msgSeq);
                } else {
                    Log.e("onRecallSysMsgForNT error: getMsgsByMsgId failed, result=" + queryResult + ", errMsg=" + errMsg);
                }
                NtGrayTipHelper.NtGrayTipJsonBuilder builder = new NtGrayTipHelper.NtGrayTipJsonBuilder();
                String summary;
                if (chatType == ChatTypeConstants.C2C) {
                    String revokerPron = selfUid.equals(recallOpUid) ? "你" : "对方";
                    if (msgObject != null && !(msgObject.getMsgType() == 5 && msgObject.getSubMsgType() == 4)) {
                        builder.appendText(revokerPron + "尝试撤回");
                        builder.append(new NtGrayTipHelper.NtGrayTipJsonBuilder.MsgRefItem("一条消息", msgSeq));
                        if (isShowShmsgseqEnabled()) {
                            builder.appendText(" [seq=" + msgSeq + "]");
                        }
                        summary = revokerPron + "尝试撤回一条消息";
                    } else if (msgObject != null && (msgObject.getMsgType() == 5 && msgObject.getSubMsgType() == 4)) {
                        // Case 1:
                        // C2C only: msg not actually received, system message: "对方撤回了一条消息"
                        // We don't need to do anything here, otherwise we will have duplicated gray tip.
                        // Case 2:
                        // The current user itself have revoked a msg. No extra action is required.
                        return;
                    } else {
                        builder.appendText(revokerPron + "撤回了一条消息(没收到) [msgId=" + msgUid + ", seq=" + msgSeq + ", cseq=" + msgClientSeq + "]");
                        summary = revokerPron + "撤回了一条消息(没收到)";
                    }
                } else if (chatType == ChatTypeConstants.GROUP) {
                    String operatorName = ContactUtils.getDisplayNameForUid(recallOpUid, peerUid);
                    String operatorUin = RelationNTUinAndUidApi.getUinFromUid(recallOpUid);
                    // note: operatorUin may be empty, in the case when in a group chat, NT kernel are not so familiar with the one
                    // do we have the original message?
                    if (msgObject != null && msgObject.getMsgType() != 1 && !(msgObject.getMsgType() == 5 && msgObject.getSubMsgType() == 4)) {
                        // good, we have the original message
                        // then, is the original message sent by the operator?
                        long msgAuthorUin = msgObject.getSenderUin();
                        String msgAuthorName = getMsgSenderReferenceName(msgObject, true);
                        if (recallOpUid.equals(msgAuthorUid)) {
                            // by themselves
                            builder.append(new NtGrayTipHelper.NtGrayTipJsonBuilder.UserItem(String.valueOf(operatorUin), recallOpUid, operatorName));
                            builder.appendText("尝试撤回");
                            builder.append(new NtGrayTipHelper.NtGrayTipJsonBuilder.MsgRefItem("一条消息", msgSeq));
                            if (isShowShmsgseqEnabled()) {
                                builder.appendText(" [seq=" + msgSeq + "]");
                            }
                            summary = operatorName + "尝试撤回一条消息";
                        } else {
                            builder.append(new NtGrayTipHelper.NtGrayTipJsonBuilder.UserItem(String.valueOf(operatorUin), recallOpUid, operatorName));
                            builder.appendText("尝试撤回");
                            builder.append(new NtGrayTipHelper.NtGrayTipJsonBuilder.UserItem(String.valueOf(msgAuthorUin), msgAuthorUid, msgAuthorName));
                            builder.appendText("的");
                            builder.append(new NtGrayTipHelper.NtGrayTipJsonBuilder.MsgRefItem("一条消息", msgSeq));
                            if (isShowShmsgseqEnabled()) {
                                builder.appendText(" [seq=" + msgSeq + "]");
                            }
                            summary = operatorName + "尝试撤回" + msgAuthorName + "的一条消息";
                        }
                    } else {
                        // we don't have the original message
                        if (selfUid.equals(recallOpUid) && (msgObject.getMsgType() == 5 && msgObject.getSubMsgType() == 4)) {
                            // the message is recalled by self, and the gray tip is already there
                            // we don't need to do anything here, otherwise we will have duplicated gray tip.
                            // Control Flow Termination.
                            return;
                        }
                        String msgAuthorName = ContactUtils.getDisplayNameForUid(msgAuthorUid, peerUid);
                        String msgAuthorUin = RelationNTUinAndUidApi.getUinFromUid(msgAuthorUid);
                        // msgAuthorUin may be empty, in the case when in a group chat, NT kernel are not so familiar with the one
                        if (recallOpUid.equals(msgAuthorUid)) {
                            builder.append(new NtGrayTipHelper.NtGrayTipJsonBuilder.UserItem(String.valueOf(operatorUin), recallOpUid, operatorName));
                            builder.appendText("撤回了一条消息(没收到) [seq=" + msgSeq + "]");
                            summary = operatorName + "撤回了一条消息(没收到)";
                        } else {
                            builder.append(new NtGrayTipHelper.NtGrayTipJsonBuilder.UserItem(String.valueOf(operatorUin), recallOpUid, operatorName));
                            builder.appendText("撤回了");
                            builder.append(new NtGrayTipHelper.NtGrayTipJsonBuilder.UserItem(String.valueOf(msgAuthorUin), msgAuthorUid, msgAuthorName));
                            builder.appendText("的一条消息(没收到) [seq=" + msgSeq + "]");
                            summary = operatorName + "撤回了" + msgAuthorName + "的一条消息(没收到)";
                        }
                    }
                } else {
                    throw new AssertionError("onRecallSysMsgForNT chatType=" + chatType);
                }
                String jsonStr = builder.build().toString();
                int busiId = (chatType == ChatTypeConstants.C2C) ? NtGrayTipHelper.AIO_AV_C2C_NOTICE : NtGrayTipHelper.AIO_AV_GROUP_NOTICE;
                Object jsonGrayElement = NtGrayTipHelper.createLocalJsonElement(busiId, jsonStr, summary);
                NtGrayTipHelper.addLocalJsonGrayTipMsg(AppRuntimeHelper.getAppRuntime(), contact, jsonGrayElement, true, true, (result, uin) -> {
                    if (result != 0) {
                        Log.e("onRecallSysMsgForNT error: addLocalJsonGrayTipMsg failed, result=" + result);
                    }
                });
            } catch (Exception | LinkageError e) {
                // dump all args to logcat
                Log.e("error: onRecallSysMsgForNT failed, chatType=" + chatType + ", peerUid=" + peerUid + ", msgUid=" + msgUid + ", msgSeq=" + msgSeq
                        + ", msgClientSeq=" + msgClientSeq + ", recallOpUid=" + recallOpUid + ", selfUid=" + selfUid, e);
                traceError(e);
            }
        }));
    }

    private static String getMsgSenderReferenceName(@NonNull MsgRecord msgRecord, boolean isInGroup) {
        if (isInGroup) {
            String memberName = msgRecord.getSendMemberName();
            if (!TextUtils.isEmpty(memberName)) {
                return memberName;
            }
        }
        String remarkName = msgRecord.getSendRemarkName();
        if (!TextUtils.isEmpty(remarkName)) {
            return remarkName;
        }
        String nickName = msgRecord.getSendNickName();
        if (!TextUtils.isEmpty(nickName)) {
            return nickName;
        }
        long uin = msgRecord.getSenderUin();
        if (uin > 0) {
            return String.valueOf(uin);
        }
        // wtf?
        return msgRecord.getSenderUid();
    }

    private Bundle createTroopMemberHighlightItem(String memberUin) {
        Bundle bundle = new Bundle();
        bundle.putInt("key_action", 5);
        bundle.putString("troop_mem_uin", memberUin);
        bundle.putBoolean("need_update_nick", true);
        return bundle;
    }

    private Object createBareHighlightGreyTip(String entityUin, int istroop, String fromUin, long time, String msg, long msgUid, long shmsgseq)
            throws ReflectiveOperationException {
        int msgtype = -2030;// MessageRecord.MSG_TYPE_TROOP_GAP_GRAY_TIPS
        Object messageRecord = Reflex.invokeStaticDeclaredOrdinalModifier(
                DexKit.requireClassFromCache(CMessageRecordFactory.INSTANCE), 0, 1, true, Modifier.PUBLIC, 0, msgtype,
                int.class);
        callMethod(messageRecord, "init", AppRuntimeHelper.getAccount(), entityUin, fromUin, msg,
                time,
                msgtype, istroop, time);
        setObjectField(messageRecord, "msgUid", msgUid);
        setObjectField(messageRecord, "shmsgseq", shmsgseq);
        setObjectField(messageRecord, "isread", true);
        return messageRecord;
    }

    private Object createBarePlainGreyTip(String entityUin, int istroop, String fromUin, long time, String msg, long msgUid, long shmsgseq)
            throws ReflectiveOperationException {
        int msgtype = -2031;// MessageRecord.MSG_TYPE_REVOKE_GRAY_TIPS
        Object messageRecord = Reflex.invokeStaticDeclaredOrdinalModifier(
                DexKit.requireClassFromCache(CMessageRecordFactory.INSTANCE), 0, 1, true, Modifier.PUBLIC, 0, msgtype,
                int.class);
        callMethod(messageRecord, "init", AppRuntimeHelper.getAccount(), entityUin, fromUin, msg,
                time,
                msgtype, istroop, time);
        setObjectField(messageRecord, "msgUid", msgUid);
        setObjectField(messageRecord, "shmsgseq", shmsgseq);
        setObjectField(messageRecord, "isread", true);
        return messageRecord;
    }

    private void addHightlightItem(Object msgForGreyTip, int start, int end, Bundle bundle) {
        try {
            Reflex.invokeVirtual(msgForGreyTip, "addHightlightItem", start, end, bundle, int.class,
                    int.class, Bundle.class);
        } catch (Exception e) {
            traceError(e);
        }
    }

    private Object getMessageLegacy(String uin, int istroop, long shmsgseq, long msgUid) {
        List<?> list = null;
        try {
            // message is query by shmsgseq, not by time ---> queryMessagesByShmsgseqFromDB
            // 定位方法名 ---> queryMsgItemByShmsgseq
            if (HostInfo.requireMinQQVersion(QQVersion.QQ_9_0_60)) { // 9.0.60~9.0.68
                list = (List<?>) Reflex.invokeVirtual(mQQMsgFacade, "t0",
                        uin, istroop, shmsgseq, msgUid,
                        String.class, int.class, long.class, long.class,
                        List.class);
            } else if (HostInfo.requireMinQQVersion(QQVersion.QQ_9_0_50)) { // 9.0.50
                list = (List<?>) Reflex.invokeVirtual(mQQMsgFacade, "u2",
                        uin, istroop, shmsgseq, msgUid,
                        String.class, int.class, long.class, long.class,
                        List.class);
            } else if (HostInfo.requireMinQQVersion(QQVersion.QQ_9_0_25)) { // 9.0.25~9.0.35
                list = (List<?>) Reflex.invokeVirtual(mQQMsgFacade, "u0",
                        uin, istroop, shmsgseq, msgUid,
                        String.class, int.class, long.class, long.class,
                        List.class);
            } else if (HostInfo.requireMinQQVersion(QQVersion.QQ_9_0_0)) { // 9.0.0~9.0.20
                list = (List<?>) Reflex.invokeVirtual(mQQMsgFacade, "v0",
                        uin, istroop, shmsgseq, msgUid,
                        String.class, int.class, long.class, long.class,
                        List.class);
            } else if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_88)) { // 8.9.88~8.9.93
                list = (List<?>) Reflex.invokeVirtual(mQQMsgFacade, "w0",
                        uin, istroop, shmsgseq, msgUid,
                        String.class, int.class, long.class, long.class,
                        List.class);
            } else if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_85)) { // 8.9.85
                list = (List<?>) Reflex.invokeVirtual(mQQMsgFacade, "x0",
                        uin, istroop, shmsgseq, msgUid,
                        String.class, int.class, long.class, long.class,
                        List.class);
            } else if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_70)) { // 8.9.70~8.9.76
                list = (List<?>) Reflex.invokeVirtual(mQQMsgFacade, "y0",
                        uin, istroop, shmsgseq, msgUid,
                        String.class, int.class, long.class, long.class,
                        List.class);
            } else if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_55)) {
                list = (List<?>) Reflex.invokeVirtual(mQQMsgFacade, "J0",
                        uin, istroop, shmsgseq, msgUid,
                        String.class, int.class, long.class, long.class,
                        List.class);
            } else if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_53)) {
                list = (List<?>) Reflex.invokeVirtual(mQQMsgFacade, "I0",
                        uin, istroop, shmsgseq, msgUid,
                        String.class, int.class, long.class, long.class,
                        List.class);
            } else if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_28)) {
                list = (List<?>) Reflex.invokeVirtual(mQQMsgFacade, "H0",
                        uin, istroop, shmsgseq, msgUid,
                        String.class, int.class, long.class, long.class,
                        List.class);
            } else if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_25)) {
                list = (List<?>) Reflex.invokeVirtual(mQQMsgFacade, "I0",
                        uin, istroop, shmsgseq, msgUid,
                        String.class, int.class, long.class, long.class,
                        List.class);
            } else if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_0)) {
                list = (List<?>) Reflex.invokeVirtual(mQQMsgFacade, "G0",
                        uin, istroop, shmsgseq, msgUid,
                        String.class, int.class, long.class, long.class,
                        List.class);
            } else if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_93)) {
                list = (List<?>) Reflex.invokeVirtual(mQQMsgFacade, "D0",
                        uin, istroop, shmsgseq, msgUid,
                        String.class, int.class, long.class, long.class,
                        List.class);
            } else if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_11)) {
                list = (List<?>) Reflex.invokeVirtual(mQQMsgFacade, "b",
                        uin, istroop, shmsgseq, msgUid,
                        String.class, int.class, long.class, long.class,
                        List.class);
            } else if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_6_0)) {
                list = (List<?>) Reflex.invokeVirtual(mQQMsgFacade, "a",
                        uin, istroop, shmsgseq, msgUid,
                        String.class, int.class, long.class, long.class,
                        List.class);
            } else {
                list = (List<?>) Reflex.invokeVirtualDeclaredOrdinal(mQQMsgFacade, 0, 2, false,
                        uin, istroop, shmsgseq, msgUid, String.class, int.class, long.class, long.class, List.class);
            }
        } catch (Exception e) {
            traceError(e);
        }
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    private String getMessageContentStripped(Object msgObject) {
        String msg = (String) Reflex.getInstanceObjectOrNull(msgObject, "msg");
        if (msg != null) {
            msg = msg.replace('\n', ' ').replace('\r', ' ').replace("\u202E", "");
            if (msg.length() > 103) {
                msg = msg.substring(0, 100) + "...";
            }
        }
        return msg;
    }

    private long getMessageUid(Object msgObject) {
        if (msgObject == null) {
            return 0;
        }
        return (long) Reflex.getInstanceObjectOrNull(msgObject, "msgUid");
    }

    private int getMessageType(Object msgObject) {
        if (msgObject == null) {
            return -1;
        }
        return (int) Reflex.getInstanceObjectOrNull(msgObject, "msgtype");
    }
}
