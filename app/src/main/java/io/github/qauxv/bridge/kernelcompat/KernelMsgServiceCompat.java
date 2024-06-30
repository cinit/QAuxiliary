/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package io.github.qauxv.bridge.kernelcompat;

import androidx.annotation.NonNull;
import com.tencent.qqnt.kernel.nativeinterface.IAddJsonGrayTipMsgCallback;
import com.tencent.qqnt.kernel.nativeinterface.IForwardOperateCallback;
import com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService;
import com.tencent.qqnt.kernel.nativeinterface.IMsgOperateCallback;
import com.tencent.qqnt.kernel.nativeinterface.IOperateCallback;
import com.tencent.qqnt.kernel.nativeinterface.MsgAttributeInfo;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import java.util.ArrayList;
import java.util.HashMap;

public class KernelMsgServiceCompat {

    private IKernelMsgService msgService;

    public KernelMsgServiceCompat(@NonNull IKernelMsgService service) {
        this.msgService = service;
    }

    public void sendMsg(long msgId, ContactCompat peer, ArrayList<MsgElement> msgElements, HashMap<Integer,
            MsgAttributeInfo> msgAttrs, IOperateCallback callback) {
        try {
            Class.forName("com.tencent.qqnt.kernel.nativeinterface.Contact");
            msgService.sendMsg(msgId, (com.tencent.qqnt.kernel.nativeinterface.Contact) peer.toKernelObject(), msgElements, msgAttrs, callback);
            return;
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("com.tencent.qqnt.kernelpublic.nativeinterface.Contact");
            msgService.sendMsg(msgId, (com.tencent.qqnt.kernelpublic.nativeinterface.Contact) peer.toKernelObject(), msgElements, msgAttrs, callback);
            return;
        } catch (ClassNotFoundException ignored) {
        }
        KernelObjectHelper.throwKernelObjectNotSupported("IKernelMsgService.sendMsg,Contact");
    }

    /**
     * Deleted since QQ 9.0.30
     */
    public long getMsgUniqueId(long serverTimeMillis) {
        return msgService.getMsgUniqueId(serverTimeMillis);
    }

    public long generateMsgUniqueId(int chatType, long serverTimeMillis) {
        return msgService.generateMsgUniqueId(chatType, serverTimeMillis);
    }

    public void getMsgsByMsgId(ContactCompat peer, ArrayList<Long> msgIds, IMsgOperateCallback callback) {
        try {
            Class.forName("com.tencent.qqnt.kernel.nativeinterface.Contact");
            msgService.getMsgsByMsgId((com.tencent.qqnt.kernel.nativeinterface.Contact) peer.toKernelObject(), msgIds, callback);
            return;
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("com.tencent.qqnt.kernelpublic.nativeinterface.Contact");
            msgService.getMsgsByMsgId((com.tencent.qqnt.kernelpublic.nativeinterface.Contact) peer.toKernelObject(), msgIds, callback);
            return;
        } catch (ClassNotFoundException ignored) {
        }
        KernelObjectHelper.throwKernelObjectNotSupported("IKernelMsgService.getMsgsByMsgId,Contact");
    }

    public void getMsgsBySeqList(ContactCompat peer, ArrayList<Long> seqList, IMsgOperateCallback callback) {
        try {
            Class.forName("com.tencent.qqnt.kernel.nativeinterface.Contact");
            msgService.getMsgsBySeqList((com.tencent.qqnt.kernel.nativeinterface.Contact) peer.toKernelObject(), seqList, callback);
            return;
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("com.tencent.qqnt.kernelpublic.nativeinterface.Contact");
            msgService.getMsgsBySeqList((com.tencent.qqnt.kernelpublic.nativeinterface.Contact) peer.toKernelObject(), seqList, callback);
            return;
        } catch (ClassNotFoundException ignored) {
        }
        KernelObjectHelper.throwKernelObjectNotSupported("IKernelMsgService.getMsgsBySeqList,Contact");
    }

    public void getMsgsBySeqRange(ContactCompat peer, long beginSeq, long endSeq, IMsgOperateCallback callback) {
        try {
            Class.forName("com.tencent.qqnt.kernel.nativeinterface.Contact");
            msgService.getMsgsBySeqRange((com.tencent.qqnt.kernel.nativeinterface.Contact) peer.toKernelObject(), beginSeq, endSeq, callback);
            return;
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("com.tencent.qqnt.kernelpublic.nativeinterface.Contact");
            msgService.getMsgsBySeqRange((com.tencent.qqnt.kernelpublic.nativeinterface.Contact) peer.toKernelObject(), beginSeq, endSeq, callback);
            return;
        } catch (ClassNotFoundException ignored) {
        }
        KernelObjectHelper.throwKernelObjectNotSupported("IKernelMsgService.getMsgsBySeqRange,Contact");
    }

    public void getMsgs(ContactCompat peer, long msgId, int cnt, boolean queryOrder, IMsgOperateCallback callback) {
        try {
            Class.forName("com.tencent.qqnt.kernel.nativeinterface.Contact");
            msgService.getMsgs((com.tencent.qqnt.kernel.nativeinterface.Contact) peer.toKernelObject(), msgId, cnt, queryOrder, callback);
            return;
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("com.tencent.qqnt.kernelpublic.nativeinterface.Contact");
            msgService.getMsgs((com.tencent.qqnt.kernelpublic.nativeinterface.Contact) peer.toKernelObject(), msgId, cnt, queryOrder, callback);
            return;
        } catch (ClassNotFoundException ignored) {
        }
        KernelObjectHelper.throwKernelObjectNotSupported("IKernelMsgService.getMsgs,Contact");
    }

    public void getSingleMsg(ContactCompat peer, long msgSeq, IMsgOperateCallback callback) {
        try {
            Class.forName("com.tencent.qqnt.kernel.nativeinterface.Contact");
            msgService.getSingleMsg((com.tencent.qqnt.kernel.nativeinterface.Contact) peer.toKernelObject(), msgSeq, callback);
            return;
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("com.tencent.qqnt.kernelpublic.nativeinterface.Contact");
            msgService.getSingleMsg((com.tencent.qqnt.kernelpublic.nativeinterface.Contact) peer.toKernelObject(), msgSeq, callback);
            return;
        } catch (ClassNotFoundException ignored) {
        }
        KernelObjectHelper.throwKernelObjectNotSupported("IKernelMsgService.getSingleMsg,Contact");
    }

    public void recallMsg(ContactCompat contact, ArrayList<Long> arrayList, IOperateCallback iOperateCallback) {
        try {
            Class.forName("com.tencent.qqnt.kernel.nativeinterface.Contact");
            msgService.recallMsg((com.tencent.qqnt.kernel.nativeinterface.Contact) contact.toKernelObject(), arrayList, iOperateCallback);
            return;
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("com.tencent.qqnt.kernelpublic.nativeinterface.Contact");
            msgService.recallMsg((com.tencent.qqnt.kernelpublic.nativeinterface.Contact) contact.toKernelObject(), arrayList, iOperateCallback);
            return;
        } catch (ClassNotFoundException ignored) {
        }
        KernelObjectHelper.throwKernelObjectNotSupported("IKernelMsgService.recallMsg,Contact");
    }

    public void addLocalJsonGrayTipMsg(ContactCompat peer, Object jsonGrayElement, boolean needStore, boolean needRecentContact,
            IAddJsonGrayTipMsgCallback callback) {
        try {
            // below 9.0.70(exclusive)
            Class.forName("com.tencent.qqnt.kernel.nativeinterface.Contact");
            Class.forName("com.tencent.qqnt.kernel.nativeinterface.JsonGrayElement");
            msgService.addLocalJsonGrayTipMsg((com.tencent.qqnt.kernel.nativeinterface.Contact) peer.toKernelObject(),
                    (com.tencent.qqnt.kernel.nativeinterface.JsonGrayElement) jsonGrayElement, needStore, needRecentContact, callback);
            return;
        } catch (ClassNotFoundException ignored) {
        }
        try {
            // 9.0.70(inclusive) and above
            Class.forName("com.tencent.qqnt.kernelpublic.nativeinterface.Contact");
            Class.forName("com.tencent.qqnt.kernelpublic.nativeinterface.JsonGrayElement");
            msgService.addLocalJsonGrayTipMsg((com.tencent.qqnt.kernelpublic.nativeinterface.Contact) peer.toKernelObject(),
                    (com.tencent.qqnt.kernelpublic.nativeinterface.JsonGrayElement) jsonGrayElement, needStore, needRecentContact, callback);
            return;
        } catch (ClassNotFoundException ignored) {
        }
        KernelObjectHelper.throwKernelObjectNotSupported("IKernelMsgService.addLocalJsonGrayTipMsg,Contact,JsonGrayElement");
    }

    public void forwardMsg(ArrayList<Long> msgIds, ContactCompat srcContact, ArrayList<ContactCompat> dstContacts, HashMap<Integer, MsgAttributeInfo> hashMap,
            IForwardOperateCallback callback) {
        try {
            Class.forName("com.tencent.qqnt.kernel.nativeinterface.Contact");
            ArrayList<com.tencent.qqnt.kernel.nativeinterface.Contact> dstContactsKernel = null;
            if (dstContacts != null) {
                dstContactsKernel = new ArrayList<>();
                for (ContactCompat contact : dstContacts) {
                    dstContactsKernel.add((com.tencent.qqnt.kernel.nativeinterface.Contact) contact.toKernelObject());
                }
            }
            msgService.forwardMsg(msgIds, (com.tencent.qqnt.kernel.nativeinterface.Contact) srcContact.toKernelObject(),
                    dstContactsKernel, hashMap, callback);
            return;
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("com.tencent.qqnt.kernelpublic.nativeinterface.Contact");
            ArrayList<com.tencent.qqnt.kernelpublic.nativeinterface.Contact> dstContactsKernel = null;
            if (dstContacts != null) {
                dstContactsKernel = new ArrayList<>();
                for (ContactCompat contact : dstContacts) {
                    dstContactsKernel.add((com.tencent.qqnt.kernelpublic.nativeinterface.Contact) contact.toKernelObject());
                }
            }
            msgService.forwardMsg(msgIds, (com.tencent.qqnt.kernelpublic.nativeinterface.Contact) srcContact.toKernelObject(),
                    dstContactsKernel, hashMap, callback);
            return;
        } catch (ClassNotFoundException ignored) {
        }
        KernelObjectHelper.throwKernelObjectNotSupported("IKernelMsgService.forwardMsg,Contact");
    }

}
