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
package cc.ioctl.hook;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.setObjectField;
import static io.github.qauxv.bridge.AppRuntimeHelper.getLongAccountUin;
import static io.github.qauxv.util.Initiator._C2CMessageProcessor;
import static io.github.qauxv.util.Initiator._QQMessageFacade;

import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import io.github.qauxv.SyncUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.bridge.ContactUtils;
import io.github.qauxv.bridge.QQMessageFacade;
import io.github.qauxv.bridge.RevokeMsgInfoImpl;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.DexKit;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.QQVersion;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author fkzhang Created by fkzhang on 1/20/2016.
 * <p>
 * Changes by cinit:
 * <p>
 * 2020/03/08 Sun.20:33 Minor changes at GreyTip
 * <p>
 * 2020/04/08 Tue.23:21 Use RevokeMsgInfoImpl for ease, wanna cry
 */
@FunctionHookEntry
@UiItemAgentEntry
public class RevokeMsgHook extends CommonSwitchFunctionHook {

    public static final RevokeMsgHook INSTANCE = new RevokeMsgHook();
    private Object mQQMsgFacade = null;

    private RevokeMsgHook() {
        //FIXME: is MSF really necessary?
        super(SyncUtils.PROC_MAIN | SyncUtils.PROC_MSF, new int[]{
                DexKit.C_MessageRecordFactory, DexKit.C_CONTACT_UTILS,
                DexKit.N_ContactUtils_getDiscussionMemberShowName,
                DexKit.N_ContactUtils_getBuddyName
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

    @Override
    public boolean initOnce() throws Exception {
        Method revokeMsg = null;
        for (Method m : _QQMessageFacade().getDeclaredMethods()) {
            if (m.getReturnType().equals(void.class)) {
                Class<?>[] argt = m.getParameterTypes();
                if (argt.length == 2 && argt[0].equals(ArrayList.class) && argt[1]
                        .equals(boolean.class)) {
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
                    onRevokeMsg(revokeMsgInfo);
                } catch (Throwable t) {
                    Log.e(t);
                }
            }
            list.clear();
        });
        return true;
    }

    private void onRevokeMsg(Object revokeMsgInfo) throws Exception {
        RevokeMsgInfoImpl info = new RevokeMsgInfoImpl((Parcelable) revokeMsgInfo);
        String entityUin = info.friendUin;
        String revokerUin = info.fromUin;
        String authorUin = info.authorUin;
        int istroop = info.istroop;
        long msgUid = info.msgUid;
        long shmsgseq = info.shmsgseq;
        long time = info.time;
        /*
        String selfUin = "" + getLongAccountUin();
        if (selfUin.equals(revokerUin)) {
            return;
        }
         */
        String uin = istroop == 0 ? revokerUin : entityUin;
        Object msgObject = getMessage(uin, istroop, shmsgseq, msgUid);
        long id = getMessageUid(msgObject);
        if (Reflex.isCallingFrom(_C2CMessageProcessor().getName())) {
            return;
        }
        boolean isGroupChat = istroop != 0;
        long newMsgUid;
        if (msgUid != 0) {
            newMsgUid = msgUid + new Random().nextInt();
        } else {
            newMsgUid = 0;
        }
        Object revokeGreyTip;
        if (isGroupChat) {
            if (authorUin == null || revokerUin.equals(authorUin)) {
                //自己撤回
                String revokerNick = ContactUtils.getTroopMemberNick(entityUin, revokerUin);
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
                    if (!hasMsgInfo) {
                        greyMsg += ", shmsgseq: " + shmsgseq;
                    }
                } else {
                    greyMsg += "撤回了一条消息(没收到), shmsgseq: " + shmsgseq;
                }
                revokeGreyTip = createBareHighlightGreyTip(entityUin, istroop, revokerUin, time + 1,
                        greyMsg, newMsgUid, shmsgseq);
                addHightlightItem(revokeGreyTip, 1, 1 + revokerNick.length(),
                        createTroopMemberHighlightItem(revokerUin));
            } else {
                //被权限狗撤回(含管理,群主)
                String revokerNick = ContactUtils.getTroopMemberNick(entityUin, revokerUin);
                String authorNick = ContactUtils.getTroopMemberNick(entityUin, authorUin);
                if (msgObject == null) {
                    String greyMsg = "\"" + revokerNick + "\u202d\"撤回了\"" + authorNick + "\u202d\"的消息(没收到)"
                            + ", shmsgseq: " + shmsgseq;
                    revokeGreyTip = createBareHighlightGreyTip(entityUin, istroop, revokerUin,
                            time + 1, greyMsg, newMsgUid, shmsgseq);
                    addHightlightItem(revokeGreyTip, 1, 1 + revokerNick.length(),
                            createTroopMemberHighlightItem(revokerUin));
                    addHightlightItem(revokeGreyTip, 1 + revokerNick.length() + 1 + 5,
                            1 + revokerNick.length() + 1 + 5 + authorNick.length(),
                            createTroopMemberHighlightItem(authorUin));
                } else {
                    String greyMsg =
                            "\"" + revokerNick + "\u202d\"尝试撤回\"" + authorNick + "\u202d\"的消息";
                    String message = getMessageContentStripped(msgObject);
                    int msgtype = getMessageType(msgObject);
                    boolean hasMsgInfo = false;
                    if (msgtype == -1000 /*text msg*/) {
                        if (!TextUtils.isEmpty(message)) {
                            greyMsg += ": " + message;
                            hasMsgInfo = true;
                        }
                    }
                    if (!hasMsgInfo) {
                        greyMsg += ", shmsgseq: " + shmsgseq;
                    }
                    revokeGreyTip = createBareHighlightGreyTip(entityUin, istroop, revokerUin,
                            time + 1, greyMsg, newMsgUid, shmsgseq);
                    addHightlightItem(revokeGreyTip, 1, 1 + revokerNick.length(),
                            createTroopMemberHighlightItem(revokerUin));
                    addHightlightItem(revokeGreyTip, 1 + revokerNick.length() + 1 + 6,
                            1 + revokerNick.length() + 1 + 6 + authorNick.length(),
                            createTroopMemberHighlightItem(authorUin));
                }
            }
        } else {
            String greyMsg;
            if (msgObject == null) {
                greyMsg = "对方撤回了一条消息(没收到), shmsgseq: " + shmsgseq;
            } else {
                String message = getMessageContentStripped(msgObject);
                int msgtype = getMessageType(msgObject);
                boolean hasMsgInfo = false;
                greyMsg = "对方尝试撤回一条消息";
                if (msgtype == -1000 /*text msg*/) {
                    if (!TextUtils.isEmpty(message)) {
                        greyMsg += ": " + message;
                    }
                }
                if (!hasMsgInfo) {
                    greyMsg += ", shmsgseq: " + shmsgseq;
                }
            }
            revokeGreyTip = createBarePlainGreyTip(revokerUin, istroop, revokerUin, time + 1,
                    greyMsg, newMsgUid, shmsgseq);
        }
        List<Object> list = new ArrayList<>();
        list.add(revokeGreyTip);
        QQMessageFacade.commitMessageRecordList(list, AppRuntimeHelper.getAccount());
    }

    private Bundle createTroopMemberHighlightItem(String memberUin) {
        Bundle bundle = new Bundle();
        bundle.putInt("key_action", 5);
        bundle.putString("troop_mem_uin", memberUin);
        bundle.putBoolean("need_update_nick", true);
        return bundle;
    }

    private Object createBareHighlightGreyTip(String entityUin, int istroop, String fromUin,
                                              long time, String msg, long msgUid, long shmsgseq)
            throws Exception {
        int msgtype = -2030;// MessageRecord.MSG_TYPE_TROOP_GAP_GRAY_TIPS
        Object messageRecord = Reflex.invokeStaticDeclaredOrdinalModifier(
                DexKit.doFindClass(DexKit.C_MessageRecordFactory), 0, 1, true, Modifier.PUBLIC, 0, msgtype,
                int.class);
        callMethod(messageRecord, "init", AppRuntimeHelper.getAccount(), entityUin, fromUin, msg,
                time,
                msgtype, istroop, time);
        setObjectField(messageRecord, "msgUid", msgUid);
        setObjectField(messageRecord, "shmsgseq", shmsgseq);
        setObjectField(messageRecord, "isread", true);
        return messageRecord;
    }

    private Object createBarePlainGreyTip(String entityUin, int istroop, String fromUin, long time,
                                          String msg, long msgUid, long shmsgseq) throws Exception {
        int msgtype = -2031;// MessageRecord.MSG_TYPE_REVOKE_GRAY_TIPS
        Object messageRecord = Reflex.invokeStaticDeclaredOrdinalModifier(
                DexKit.doFindClass(DexKit.C_MessageRecordFactory), 0, 1, true, Modifier.PUBLIC, 0, msgtype,
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

    private Object getMessage(String uin, int istroop, long shmsgseq, long msgUid) {
        List<?> list = null;
        try {
            // message is query by shmsgseq, not by time ---> queryMessagesByShmsgseqFromDB
            if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_0)) {
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
