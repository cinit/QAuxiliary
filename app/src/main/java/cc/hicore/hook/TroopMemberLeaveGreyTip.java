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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.bridge.ContactUtils;
import io.github.qauxv.bridge.GreyTipBuilder;
import io.github.qauxv.bridge.QQMessageFacade;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.dexkit.CMessageRecordFactory;
import io.github.qauxv.util.dexkit.COnlinePushPbPushTransMsg;
import io.github.qauxv.util.dexkit.CSystemMessageProcessor;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.NContactUtils_getBuddyName;
import io.github.qauxv.util.dexkit.NContactUtils_getDiscussionMemberShowName;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

@FunctionHookEntry
@UiItemAgentEntry
public class TroopMemberLeaveGreyTip extends CommonSwitchFunctionHook {

    public static final TroopMemberLeaveGreyTip INSTANCE = new TroopMemberLeaveGreyTip();

    private TroopMemberLeaveGreyTip() {
        super(SyncUtils.PROC_MAIN, new DexKitTarget[]{
                CMessageRecordFactory.INSTANCE,
                CSystemMessageProcessor.INSTANCE,
                COnlinePushPbPushTransMsg.INSTANCE,
                NContactUtils_getDiscussionMemberShowName.INSTANCE,
                NContactUtils_getBuddyName.INSTANCE,
        });
        // The MSF process is not required to be hooked, said by the original author.
    }

    @NonNull
    @Override
    public String getName() {
        return "群成员退群添加灰字提示";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "可能很耗电";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY;
    }

    @Override
    protected boolean initOnce() throws Exception {
        HookUtils.hookBeforeIfEnabled(this,
                Objects.requireNonNull(DexKit.loadClassFromCache(COnlinePushPbPushTransMsg.INSTANCE), "OnlinePushPbPushTransMsg")
                        .getDeclaredMethod("a", Initiator.loadClass("com.tencent.mobileqq.app.MessageHandler"),
                                Initiator.loadClass("com.tencent.qphone.base.remote.ToServiceMsg"),
                                Initiator.loadClass("com.tencent.qphone.base.remote.FromServiceMsg")),
                param -> onOnlinePushPbPushTransMsg(param.args[0], param.args[1], param.args[2])
        );
        HookUtils.hookBeforeIfEnabled(this,
                Reflex.findSingleMethod(
                        Objects.requireNonNull(DexKit.loadClassFromCache(CSystemMessageProcessor.INSTANCE), "C_SystemMessageProcessor"),
                        void.class, false,
                        int.class, Initiator.loadClass("tencent.mobileim.structmsg.structmsg$StructMsg"), int.class),
                param -> onProcessGroupSystemMsg((int) param.args[0], param.args[1], (int) param.args[2])
        );

        return true;
    }

    private static void onProcessGroupSystemMsg(int msgPosUnused, Object structMsg, int msgTypeUnused) throws ReflectiveOperationException {
        Object msg = Reflex.getInstanceObject(structMsg, "msg", null);
        Object actionUinObj = Reflex.getInstanceObject(msg, "action_uin", null);
        long actionUin = Reflex.getInstanceObject(actionUinObj, "value", long.class);
        Object reqUinObj = Reflex.getInstanceObject(structMsg, "req_uin", null);
        long userUin = Reflex.getInstanceObject(reqUinObj, "value", long.class);
        Object groupCodeObj = Reflex.getInstanceObject(msg, "group_code", null);
        long troopUin = Reflex.getInstanceObject(groupCodeObj, "value", long.class);
        Object msgTypeObj = Reflex.getInstanceObject(structMsg, "msg_type", null);
        int msgType = Reflex.getInstanceObject(msgTypeObj, "value", int.class);

//        if (!TroopManager.IsInTroop(String.valueOf(troopUin), String.valueOf(userUin))) {
//            return;
//        }
//        if (actionUin > 0) {
//            if (!TroopManager.IsInTroop(String.valueOf(troopUin), String.valueOf(actionUin))) {
//                actionUin = 1;
//            }
//        }
//        Log.d("onOnlinePushPbPushTransMsg actionUin=" + actionUin + ", userUin=" + userUin + ", troopUin=" + troopUin + ", msgType=" + msgType);
        commitTroopMemberLeaveGreyTip(troopUin, userUin, 3, actionUin);
    }

    private static void onOnlinePushPbPushTransMsg(Object msgHandler, Object toServiceMsg, Object fromServiceMsg) throws ReflectiveOperationException {
        String cmdline = (String) Reflex.invokeVirtual(toServiceMsg, "getServiceCmd", String.class);
        if (!"OnlinePush.PbPushTransMsg".equalsIgnoreCase(cmdline)) {
            return;
        }
        Object pbMsgInfo = Initiator.loadClass("com.tencent.pb.onlinepush.OnlinePushTrans$PbMsgInfo").newInstance();
        byte[] wupBuffer = (byte[]) Reflex.invokeVirtual(toServiceMsg, "getWupBuffer", byte[].class);
        if (wupBuffer == null || wupBuffer.length == 0) {
            return;
        }
        byte[] body = new byte[wupBuffer.length - 4];
        System.arraycopy(wupBuffer, 4, body, 0, body.length);
        Reflex.invokeVirtual(pbMsgInfo, "mergeFrom", body, byte[].class, Initiator.loadClass("com.tencent.mobileqq.pb.MessageMicro"));
        int msgType = (int) decodePBLongField(Reflex.getInstanceObject(pbMsgInfo, "msg_type", null));
        if (msgType == 34) {
            byte[] msgData = Reflex.getInstanceObject(
                    Reflex.getInstanceObject(
                            Reflex.getInstanceObject(pbMsgInfo, "msg_data", null),
                            "value", null),
                    "bytes", byte[].class);
            Objects.requireNonNull(msgData, "msgData is null");

            long troopUin = getLongData(msgData, 0);
            long memberUin = getLongData(msgData, 5);
            byte opType = msgData[9];
            long opUin = getLongData(msgData, 10);

//            Log.d("onProcessGroupSystemMsg troopUin=" + troopUin + ", memberUin=" + memberUin + ", opType=" + opType + ", opUin=" + opUin);
            commitTroopMemberLeaveGreyTip(troopUin, memberUin, opType, opUin);
        }
    }

    public static long decodePBLongField(Object obj) throws ReflectiveOperationException {
        for (Method m : obj.getClass().getDeclaredMethods()) {
            if (m.getName().equals("get")) {
                Object v = m.invoke(obj);
                if (v == null) {
                    throw new NullPointerException("v is null");
                }
                if (v instanceof Number) {
                    return ((Number) v).longValue();
                }
                throw new IllegalArgumentException("expected Number, but got " + v.getClass());
            }
        }
        throw new IllegalArgumentException("no get method");
    }

    public static long getLongData(byte[] data, int i) {
        if (data == null) {
            return 0;
        }
        return ((((long) data[i]) & 255) << 24) + ((((long) data[i + 1]) & 255) << 16) + ((((long) data[i + 2]) & 255) << 8) + ((((long) data[i + 3]) & 255));
    }

    public static void commitTroopMemberLeaveGreyTip(long troopUin, long memberUin,
                                                     int type, long operatorUin) throws ReflectiveOperationException {
        Random r = new Random();
        long time = System.currentTimeMillis() / 1000L;
        long msgUid = r.nextInt();
        long msgseq = time;
        long shmsgseq = Math.abs((long) r.nextInt());
        if (type == 3 && operatorUin > 0) {
            String memberName = ContactUtils.getTroopMemberNick(troopUin, memberUin);
            String opName = ContactUtils.getTroopMemberNick(troopUin, operatorUin);
            Object greyTip = GreyTipBuilder.create(GreyTipBuilder.MSG_TYPE_TROOP_GAP_GRAY_TIPS)
                    .append("成员")
                    .appendTroopMember(String.valueOf(memberUin), memberName)
                    .append("被")
                    .appendTroopMember(String.valueOf(operatorUin), opName)
                    .append("移出了群")
                    .build(String.valueOf(troopUin), 1, String.valueOf(memberUin), time, msgUid, msgseq, shmsgseq);
            ArrayList<Object> list = new ArrayList<>(1);
            list.add(greyTip);
            QQMessageFacade.commitMessageRecordList(list);
        } else {
            String memberName = ContactUtils.getTroopMemberNick(troopUin, memberUin);
            Object greyTip = GreyTipBuilder.create(GreyTipBuilder.MSG_TYPE_TROOP_GAP_GRAY_TIPS)
                    .append("成员")
                    .appendTroopMember(String.valueOf(memberUin), memberName)
                    .append("退出了群")
                    .build(String.valueOf(troopUin), 1, String.valueOf(memberUin), time, msgUid, msgseq, shmsgseq);
            ArrayList<Object> list = new ArrayList<>(1);
            list.add(greyTip);
            QQMessageFacade.commitMessageRecordList(list);
        }
    }
}
