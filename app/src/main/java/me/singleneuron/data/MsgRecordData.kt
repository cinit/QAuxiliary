/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
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
package me.singleneuron.data

import cc.ioctl.util.Reflex
import com.github.kyuubiran.ezxhelper.utils.getObjectAs
import com.github.kyuubiran.ezxhelper.utils.getObjectOrNull
import com.github.kyuubiran.ezxhelper.utils.putObject
import io.github.qauxv.util.Initiator
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.*

data class MsgRecordData(val msgRecord: Any) {

    companion object {
        const val MSG_TYPE_TEXT = -1000                       //文本消息
        const val MSG_TYPE_TEXT_VIDEO = -1001                 //小视频
        const val MSG_TYPE_TROOP_TIPS_ADD_MEMBER = -1012      //加群消息
        const val MSG_TYPE_TEXT_FRIEND_FEED = -1034           //好友签名卡片消息(json)
        const val MSG_TYPE_MIX = -1035                        //混合消息
        const val MSG_TYPE_REPLY_TEXT = -1049                 //回复消息
        const val MSG_TYPE_MEDIA_PIC = -2000                  //图片消息
        const val MSG_TYPE_MEDIA_PTT = -2002                  //语音消息
        const val MSG_TYPE_MEDIA_FILE = -2005                 //文件
        const val MSG_TYPE_MEDIA_MARKFACE = -2007             //表情消息[并非"我的收藏" 而是从QQ表情商店下载的表情]
        const val MSG_TYPE_MEDIA_VIDEO = -2009                //QQ语音/视频通话
        const val MSG_TYPE_STRUCT_MSG = -2011                 //卡片消息[分享/签到/转发消息等]
        const val MSG_TYPE_ARK_APP = -5008                    //小程序分享消息
        const val MSG_TYPE_POKE_MSG = -5012                   //戳一戳
        const val MSG_TYPE_POKE_EMO_MSG = -5018               //另类戳一戳
        const val MSG_TYPE_UNITE_GRAY_NORMAL = -5040          //灰字消息(暂时未知)

        val MSG_TYPE_MAP = mapOf(
            MSG_TYPE_TEXT to "文本消息",
            MSG_TYPE_TEXT_VIDEO to "小视频",
            MSG_TYPE_TROOP_TIPS_ADD_MEMBER to "加群消息",
            MSG_TYPE_TEXT_FRIEND_FEED to "好友签名卡片消息",
            MSG_TYPE_MIX to "混合消息",
            MSG_TYPE_REPLY_TEXT to "回复消息",
            MSG_TYPE_MEDIA_PIC to "图片消息",
            MSG_TYPE_MEDIA_PTT to "语音消息",
            MSG_TYPE_MEDIA_FILE to "文件",
            MSG_TYPE_MEDIA_MARKFACE to "表情消息[并非\"我的收藏\" 而是从QQ表情商店下载的表情]",
            MSG_TYPE_MEDIA_VIDEO to "QQ语音/视频通话",
            MSG_TYPE_STRUCT_MSG to "卡片消息[分享/签到/转发消息等]",
            MSG_TYPE_ARK_APP to "小程序分享消息",
            MSG_TYPE_POKE_MSG to "戳一戳",
            MSG_TYPE_POKE_EMO_MSG to "另类戳一戳",
            MSG_TYPE_UNITE_GRAY_NORMAL to "灰字消息"
        )

        private var _msg: Field? = null
        private var _msg2: Field? = null
        private var _msgUid: Field? = null
        private var _friendUin: Field? = null
        private var _senderUin: Field? = null
        private var _selfUin: Field? = null
        private var _msgType: Field? = null
        private var _extraFlag: Field? = null
        private var _extStr: Field? = null
        private var _time: Field? = null
        private var _isRead: Field? = null
        private var _isSend: Field? = null
        private var _isTroop: Field? = null
        private var _msgSeq: Field? = null
        private var _shMsgSeq: Field? = null
        private var _uniseq: Field? = null
        private var _msgData: Field? = null
        private var _getExtInfoFromExtStr: Method? = null
    }

    //消息文本
    val msg: String?
        @Throws(NullPointerException::class)
        get() {
            if (_msg == null) {
                _msg = Reflex.findField(msgRecord.javaClass, String::class.java, "msg")
            }
            return _msg!!.get(msgRecord) as String?
        }

    //也是消息文本
    val msg2: String?
        get() {
            if (_msg2 == null) {
                _msg2 = Reflex.findField(msgRecord.javaClass, String::class.java, "msg2")
            }
            return _msg2!!.get(msgRecord) as String?
        }

    //消息id
    //@Deprecated
    val msgId: Long?
        get() = msgRecord.getObjectOrNull("msgId", Long::class.java) as Long?

    //消息uid
    val msgUid: Long
        get() {
            if (_msgUid == null) {
                _msgUid = Reflex.findField(msgRecord.javaClass, Long::class.java, "msgUid")
            }
            return _msgUid!!.getLong(msgRecord)
        }

    //好友QQ [当为群聊聊天时 则为QQ群号]
    val friendUin: String?
        get() {
            if (_friendUin == null) {
                _friendUin = Reflex.findField(msgRecord.javaClass, String::class.java, "frienduin")
            }
            return _friendUin!!.get(msgRecord) as String?
        }

    //发送人QQ
    val senderUin: String?
        get() {
            if (_senderUin == null) {
                _senderUin = Reflex.findField(msgRecord.javaClass, String::class.java, "senderuin")
            }
            return _senderUin!!.get(msgRecord) as String?
        }

    //自己QQ
    val selfUin: String?
        get() {
            if (_selfUin == null) {
                _selfUin = Reflex.findField(msgRecord.javaClass, String::class.java, "selfuin")
            }
            return _selfUin!!.get(msgRecord) as String?
        }

    //消息类型
    val msgType: Int
        get() {
            if (_msgType == null) {
                _msgType = Reflex.findField(msgRecord.javaClass, Int::class.java, "msgtype")
            }
            return _msgType!!.getInt(msgRecord)
        }

    val readableMsgType: String
        get() = msgType.let { if (MSG_TYPE_MAP[it] == null) msgType.toString() else MSG_TYPE_MAP[it] + "($msgType)" }

    //额外flag extraflag
    val extraFlag: Int
        get() {
            if (_extraFlag == null) {
                _extraFlag = Reflex.findField(msgRecord.javaClass, Int::class.java, "extraflag")
            }
            return _extraFlag!!.getInt(msgRecord)
        }

    //额外字符串 extStr
    val extStr: String?
        get() {
            if (_extStr == null) {
                _extStr = Reflex.findField(msgRecord.javaClass, String::class.java, "extStr")
            }
            return _extStr!!.get(msgRecord) as String?
        }

    //时间戳
    val time: Long
        get() {
            if (_time == null) {
                _time = Reflex.findField(msgRecord.javaClass, Long::class.java, "time")
            }
            return _time!!.getLong(msgRecord)
        }
    val readableTime: String
        get() = time.let {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date(it * 1000))
        }

    //是否已读
    val isRead: Boolean
        get() {
            if (_isRead == null) {
                _isRead = Reflex.findField(msgRecord.javaClass, Boolean::class.java, "isread")
            }
            return _isRead!!.getBoolean(msgRecord)
        }

    //是否发送
    val isSend: Int
        get() {
            if (_isSend == null) {
                _isSend = Reflex.findField(msgRecord.javaClass, Int::class.java, "issend")
            }
            return _isSend!!.getInt(msgRecord)
        }

    //是否群组
    val isTroop: Int
        get() {
            if (_isTroop == null) {
                _isTroop = Reflex.findField(msgRecord.javaClass, Int::class.java, "istroop")
            }
            return _isTroop!!.getInt(msgRecord)
        }

    //未知 msgseq
    val msgSeq: Long
        get() {
            if (_msgSeq == null) {
                _msgSeq = Reflex.findField(msgRecord.javaClass, Long::class.java, "msgseq")
            }
            return _msgSeq!!.getLong(msgRecord)
        }

    //未知 shmsgseq
    val shMsgSeq: Long
        get() {
            if (_shMsgSeq == null) {
                _shMsgSeq = Reflex.findField(msgRecord.javaClass, Long::class.java, "shmsgseq")
            }
            return _shMsgSeq!!.getLong(msgRecord)
        }

    //未知 uinseq
    val uniseq: Long
        get() {
            if (_uniseq == null) {
                _uniseq = Reflex.findField(msgRecord.javaClass, Long::class.java, "uniseq")
            }
            return _uniseq!!.getLong(msgRecord)
        }

    //消息data msgData
    val msgData: ByteArray?
        get() {
            if (_msgData == null) {
                _msgData = Reflex.findField(msgRecord.javaClass, ByteArray::class.java, "msgData")
            }
            return _msgData!!.get(msgRecord) as ByteArray?
        }

    inline fun <reified T> get(fieldName: String): T {
        return msgRecord.getObjectAs(fieldName, T::class.java)
    }

    fun <T> set(fieldName: String, value: T) where T : Any {
        msgRecord.putObject(fieldName, value)
    }

    fun getExtInfoFromExtStr(key: String): String {
        if (_getExtInfoFromExtStr == null) {
            _getExtInfoFromExtStr = Reflex.findMethod(Initiator._MessageRecord(), String::class.java, "getExtInfoFromExtStr", String::class.java)
        }
        return _getExtInfoFromExtStr!!.invoke(msgRecord, key) as String
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.apply {
            append("消息文本: $msg\n")
            msg2?.let { append("也是消息文本: $msg2\n") }
            append("消息uid: $msgUid\n")
            friendUin?.let { append("好友/群号:$friendUin\n") }
            senderUin?.let { append("发送人QQ: $senderUin\n") }
            selfUin?.let { append("自己QQ: $selfUin\n") }
            append("消息类型: $readableMsgType\n")
            append("额外flag: ${extraFlag.toString(16)}\n")
            append("时间戳: $readableTime\n")
            append("是否已读: $isRead\n")
            append("是否发送: $isSend\n")
            append("是否群组: $isTroop\n")
            append("msgSeq：$msgSeq\n")
            append("shMsgSeq: $shMsgSeq\n")
            append("uinSeq: $uniseq\n")
        }
        return stringBuilder.toString()
    }
}
