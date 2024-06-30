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

import java.io.Serializable;

public final class ContactCompat {

    int chatType;
    String guildId;
    String peerUid;
    long serialVersionUID;

    public ContactCompat() {
        this.serialVersionUID = 1L;
        this.peerUid = "";
        this.guildId = "";
    }

    public ContactCompat(int chatType, String peerUid, String guildId) {
        this.serialVersionUID = 1L;
        this.chatType = chatType;
        this.peerUid = peerUid;
        this.guildId = guildId;
    }

    public Serializable toKernelObject() {
        // TODO: 2024-06-30 Check whether R8 optimizer will reorganize the const-class instruction
        try {
            Class.forName("com.tencent.qqnt.kernel.nativeinterface.Contact");
            return new com.tencent.qqnt.kernel.nativeinterface.Contact(chatType, peerUid, guildId);
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("com.tencent.qqnt.kernelpublic.nativeinterface.Contact");
            return new com.tencent.qqnt.kernelpublic.nativeinterface.Contact(chatType, peerUid, guildId);
        } catch (ClassNotFoundException ignored) {
        }
        KernelObjectHelper.throwKernelObjectNotSupported("Contact");
        return null;
    }

    public static ContactCompat fromKernelObject(Serializable kernelObject) {
        String className = kernelObject.getClass().getName();
        try {
            if (className.equals("com.tencent.qqnt.kernel.nativeinterface.Contact")) {
                com.tencent.qqnt.kernel.nativeinterface.Contact contact = (com.tencent.qqnt.kernel.nativeinterface.Contact) kernelObject;
                return new ContactCompat(contact.getChatType(), contact.getPeerUid(), contact.getGuildId());
            }
        } finally {
            // prevent R8 const-class optimization
        }
        try {
            if (className.equals("com.tencent.qqnt.kernelpublic.nativeinterface.Contact")) {
                com.tencent.qqnt.kernelpublic.nativeinterface.Contact contact = (com.tencent.qqnt.kernelpublic.nativeinterface.Contact) kernelObject;
                return new ContactCompat(contact.getChatType(), contact.getPeerUid(), contact.getGuildId());
            }
        } finally {
            // prevent R8 const-class optimization
        }
        KernelObjectHelper.throwKernelObjectNotSupported(className);
        return null;
    }

    public int getChatType() {
        return this.chatType;
    }

    public String getGuildId() {
        return this.guildId;
    }

    public String getPeerUid() {
        return this.peerUid;
    }

    public void setChatType(int chatType) {
        this.chatType = chatType;
    }

    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    public void setPeerUid(String peerUid) {
        this.peerUid = peerUid;
    }

    public String toString() {
        return "Contact{chatType=" + this.chatType + ",peerUid=" + this.peerUid + ",guildId=" + this.guildId + ",}";
    }

}
