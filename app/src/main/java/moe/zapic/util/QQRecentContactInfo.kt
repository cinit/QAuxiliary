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

package moe.zapic.util

import io.github.qauxv.util.Log
import xyz.nextalone.util.get


class QQRecentContactInfo(obj: Any) {
    var abstractContent: ArrayList<Any>? = null
    var anonymousFlag: Int? = null
    var atType: Int? = null
    var avatarPath: String? = null
    var avatarUrl: String? = null
    var chatType: Int? = null
    var contactId: Long? = null
    var draftFlag: Byte? = null
    var draftTime: Long? = null
    var hiddenFlag: Int? = null
    var isBeat: Boolean? = null
    var isBlock: Boolean? = null
    var isMsgDisturb: Boolean? = null
    var isOnlineMsg: Boolean? = null
    var keepHiddenFlag: Int? = null
    var listOfSpecificEventTypeInfosInMsgBox: ArrayList<Any>? = null
    var msgId: Long? = null
    var msgSeq: Long? = null
    var msgTime: Long? = null
    var nestedSortedContactList: ArrayList<Long>? = null
    var notifiedType: Int? = null
    var peerName: String? = null
    var peerUid: String? = null
    var peerUin: Long? = null
    var remark: String? = null
    var sendMemberName: String? = null
    var sendNickName: String? = null
    var sendRemarkName: String? = null
    var sendStatus: Int? = null
    var senderUid: String? = null
    var senderUin: Long? = null
    var sessionType: Int? = null
    var shieldFlag: Long? = null
    var sortField: Long? = null
    var specialCareFlag: Byte? = null
    var topFlag: Byte? = null
    var topFlagTime: Long? = null
    var unreadChatCnt: Int? = null
    var unreadCnt: Long? = null
    var unreadFlag: Long? = null


    init {
        this::class.java.declaredFields.forEach {
            try {
                it.set(this, obj.get(it.name))
            } catch (e: Throwable) {
                Log.e(e)
            }
        }
    }

    fun getUserName(): String? {
        return if (!sendMemberName.isNullOrEmpty()) {
            sendMemberName
        } else if (!sendRemarkName.isNullOrEmpty()) {
            sendRemarkName
        } else if (!sendNickName.isNullOrEmpty()) {
            sendNickName
        } else {
            null
        }
    }

}
