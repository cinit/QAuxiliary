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
package cc.ioctl.util

object MsgRecordUtil {
    val MSG = object : HashBiMap<String, Int>() {
        init {
            put("@全体消息", 0)
            put("EXTRA_STREAM_PTT_FLAG", 10001)
            put("MIN_VERSION_CODE_SUPPORT_IMAGE_MD5_TRANS", 2)
            put("MSG_TYPE_0X7F", -2006)
            put("MSG_TYPE_ACTIVATE_FRIENDS", -5003)
            put("MSG_TYPE_ACTIVITY", -4002)
            put("MSG_TYPE_AIO_FOR_LOCATION_SHARE", -2076)
            put("MSG_TYPE_AIO_FOR_STORY_VIDEO", -2074)
            put("MSG_TYPE_AI_SPECIAL_GUIDE", -1052)
            put("MSG_TYPE_APPROVAL_GRAY_TIPS", -2041)
            put("MSG_TYPE_APPROVAL_MSG", -2040)
            put("MSG_TYPE_ARK_APP", -5008)
            put("MSG_TYPE_ARK_BABYQ_REPLY", -5016)
            put("MSG_TYPE_ARK_SDK_SHARE", -5017)
            put("MSG_TYPE_AUTHORIZE_FAILED", -4005)
            put("MSG_TYPE_AUTOREPLY", -10000)
            put("MSG_TYPE_BAT_PROCESS_FILE", -3013)
            put("MSG_TYPE_BIRTHDAY_NOTICE", -7007)
            put("MSG_TYPE_BIZ_DATA", -2023)
            put("MSG_TYPE_C2C_CHAT_FREQ_CALL_TIP", -1014)
            put("MSG_TYPE_C2C_KEYWORD_CALL_TIP", -1015)
            put("MSG_TYPE_C2C_MIXED", -30002)
            put("MSG_TYPE_CMGAME_TIPS", -7004)
            put("MSG_TYPE_COLOR_RING_TIPS", -3012)
            put("MSG_TYPE_COMMON_HOBBY_FOR_AIO_SHOW", -2023)
            put("MSG_TYPE_CONFESS_CARD", -2066)
            put("MSG_TYPE_CONFESS_NEWS", -2065)
            put("MSG_TYPE_CONFIGURABLE_GRAY_TIPS", 2024)
            put("MSG_TYPE_CONFIGURABLE_TAB_VISIBLE_GRAY_TIPS", -2042)
            put("MSG_TYPE_DAREN_ASSISTANT", -2068)
            put("MSG_TYPE_DATE_FEED", -1042)
            put("MSG_TYPE_DEVICE_CLOSEGROUPCHAT", -4506)
            put("MSG_TYPE_DEVICE_DISMISSBIND", -4507)
            put("MSG_TYPE_DEVICE_FILE", -4500)
            put("MSG_TYPE_DEVICE_LITTLE_VIDEO", -4509)
            put("MSG_TYPE_DEVICE_OPENGROUPCHAT", -4505)
            put("MSG_TYPE_DEVICE_PTT", -4501)
            put("MSG_TYPE_DEVICE_SHORT_VIDEO", -4503)
            put("MSG_TYPE_DEVICE_SINGLESTRUCT", -4502)
            put("MSG_TYPE_DEVICE_TEXT", -4508)
            put("MSG_TYPE_DINGDONG_SCHEDULE_MSG", -5010)
            put("MSG_TYPE_DING_DONG_GRAY_TIPS", -2034)
            put("MSG_TYPE_DISCUSS_PUSH", -1004)
            put("MSG_TYPE_DISCUSS_UPGRADE_TO_GROUP_TIPS", -1050)
            put("MSG_TYPE_DISC_CREATE_CALL_TIP", -1016)
            put("MSG_TYPE_DISC_PTT_FREQ_CALL_TIP", -1017)
            put("MSG_TYPE_ENTER_TROOP", -4003)
            put("MSG_TYPE_FAILED_MSG", -2013)
            put("MSG_TYPE_FAKE_EMOTION", -7008)
            put("MSG_TYPE_FILE_RECEIPT", -3008)
            put("MSG_TYPE_FLASH_CHAT", -5013)
            put("MSG_TYPE_FOLD_MSG_GRAY_TIPS", -5011)
            put("MSG_TYPE_FORWARD_IMAGE", -20000)
            put("MSG_TYPE_FRIEND_SYSTEM_STRUCT_MSG", -2050)
            put("MSG_TYPE_FU_DAI", -2072)
            put("MSG_TYPE_GAME_INVITE", -3004)
            put("MSG_TYPE_GAME_PARTY_GRAY_TIPS", -2049)
            put("MSG_TYPE_GAME_SHARE", -3005)
            put("MSG_TYPE_GRAY_DATALINE_TIM_TIPS", -5041)
            put("MSG_TYPE_GRAY_TIPS", -5000)
            put("MSG_TYPE_GRAY_TIPS_TAB_VISIBLE", -5001)
            put("MSG_TYPE_GROUPDISC_FILE", -2014)
            put("MSG_TYPE_HIBOOM", -5014)
            put("MSG_TYPE_HOMEWORK_PRAISE", -2043)
            put("MSG_TYPE_HONGBAO_KEYWORDS_TIPS", -1045)
            put("MSG_TYPE_HOT_CHAT_TO_SEE_TIP", 1018)
            put("MSG_TYPE_HR_INFO", -7003)
            put("MSG_TYPE_INCOMPATIBLE_GRAY_TIPS", -5002)
            put("MSG_TYPE_INTERACT_AND_FOLLOW", -2055)
            put("MSG_TYPE_LIFEONLINEACCOUNT", -5004)
            put("MSG_TYPE_LIGHTALK_MSG", -2026)
            put("MSG_TYPE_LIMIT_CHAT_CONFIRM", -7005)
            put("MSG_TYPE_LIMIT_CHAT_TOPIC", -4023)
            put("MSG_TYPE_LIMIT_CHAT_TOPIC_RECEIVER", -4024)
            put("MSG_TYPE_LOCAL_COMMON", -4000)
            put("MSG_TYPE_LOCAL_URL", -4001)
            put("MSG_TYPE_LONG_MIX", -1036)
            put("MSG_TYPE_LONG_TEXT", -1037)
            put("MSG_TYPE_MASTER_UIN_NAVIGATION", -2064)
            put("MSG_TYPE_MEDAL_NEWS", -2062)
            put("MSG_TYPE_MEDIA_EMO", -2001)
            put("MSG_TYPE_MEDIA_FILE", -2005)
            put("MSG_TYPE_MEDIA_FUNNY_FACE", -2010)
            put("MSG_TYPE_MEDIA_LIGHTVIDEO", -2071)
            put("MSG_TYPE_MEDIA_MARKFACE", -2007)
            put("MSG_TYPE_MEDIA_MULTI09", -2003)
            put("MSG_TYPE_MEDIA_MULTI513", -2004)
            put("MSG_TYPE_MEDIA_PIC", -2000)
            put("MSG_TYPE_MEDIA_PTT", -2002)
            put("MSG_TYPE_MEDIA_SECRETFILE", -2008)
            put("MSG_TYPE_MEDIA_SHORTVIDEO", -2022)
            put("MSG_TYPE_MEDIA_VIDEO", -2009)
            put("MSG_TYPE_MEETING_NOTIFY", -5006)
            put("MSG_TYPE_MIX", -1035)
            put("MSG_TYPE_MULTI_TEXT_VIDEO", -4008)
            put("MSG_TYPE_MULTI_VIDEO", -2016)
            put("MSG_TYPE_MY_ENTER_TROOP", -4004)
            put("MSG_TYPE_NEARBY_DATING_SAFETY_TIP", -1028)
            put("MSG_TYPE_NEARBY_DATING_TIP", -1024)
            put("MSG_TYPE_NEARBY_FLOWER_TIP", -2037)
            put("MSG_TYPE_NEARBY_LIVE_TIP", -2053)
            put("MSG_TYPE_NEARBY_MARKET", -2027)
            put("MSG_TYPE_NEARBY_RECOMMENDER", -4011)
            put("MSG_TYPE_NEW_FRIEND_TIPS", -1013)
            put("MSG_TYPE_NEW_FRIEND_TIPS_GAME_ADDEE", -1019)
            put("MSG_TYPE_NEW_FRIEND_TIPS_GAME_ADDER", -1018)
            put("MSG_TYPE_NULL", -999)
            put("MSG_TYPE_ONLINE_FILE_REQ", -3007)
            put("MSG_TYPE_OPERATE_TIPS", -1041)
            put("MSG_TYPE_PA_PHONE_MSG_TIPS", -1048)
            put("MSG_TYPE_PC_PUSH", -3001)
            put("MSG_TYPE_PIC_AND_TEXT_MIXED", -3000)
            put("MSG_TYPE_PIC_QSECRETARY", -1032)
            put("MSG_TYPE_PL_NEWS", -2060)
            put("MSG_TYPE_POKE_EMO_MSG", -5018)
            put("MSG_TYPE_POKE_MSG", -5012)
            put("MSG_TYPE_PSTN_CALL", -2046)
            put("MSG_TYPE_PTT_QSECRETARY", -1031)
            put("MSG_TYPE_PUBLIC_ACCOUNT", -3006)
            put("MSG_TYPE_QCIRCLE_NEWEST_FEED", -2077)
            put("MSG_TYPE_QLINK_AP_CREATE_SUC_TIPS", -3011)
            put("MSG_TYPE_QLINK_FILE_TIPS", -3009)
            put("MSG_TYPE_QLINK_SEND_FILE_TIPS", -3010)
            put("MSG_TYPE_QQSTORY", -2051)
            put("MSG_TYPE_QQSTORY_COMMENT", -2052)
            put("MSG_TYPE_QQSTORY_LATEST_FEED", -2061)
            put("MSG_TYPE_QQWALLET_MSG", -2025)
            put("MSG_TYPE_QQWALLET_TIPS", -2029)
            put("MSG_TYPE_QZONE_NEWEST_FEED", -2015)
            put("MSG_TYPE_RECOMMAND_TIPS", -5007)
            put("MSG_TYPE_RED_PACKET_TIPS", -1044)
            put("MSG_TYPE_RENEWAL_TAIL_TIP", -4020)
            put("MSG_TYPE_REPLY_TEXT", -1049)
            put("MSG_TYPE_REVOKE_GRAY_TIPS", -2031)
            put("MSG_TYPE_SCRIBBLE_MSG", -7001)
            put("MSG_TYPE_SENSITIVE_MSG_MASK_TIPS", -1046)
            put("MSG_TYPE_SHAKE_WINDOW", -2020)
            put("MSG_TYPE_SHARE_HOT_CHAT_GRAY_TIPS", -2033)
            put("MSG_TYPE_SHARE_LBS_PUSH", -4010)
            put("MSG_TYPE_SHIELD_MSG", -2012)
            put("MSG_TYPE_SINGLE_WAY_FRIEND_ADD_ALLOW_ALL_MSG", -7006)
            put("MSG_TYPE_SINGLE_WAY_FRIEND_MSG", -2019)
            put("MSG_TYPE_SOUGOU_INPUT_TIPS", -1043)
            put("MSG_TYPE_SPECIALCARE_TIPS", -5005)
            put("MSG_TYPE_SPLIT_LINE_GRAY_TIPS", -4012)
            put("MSG_TYPE_STICKER_MSG", -2058)
            put("MSG_TYPE_STRUCT_LONG_TEXT", -1051)
            put("MSG_TYPE_STRUCT_MSG", -2011)
            put("MSG_TYPE_STRUCT_TROOP_NOTIFICATION", -2021)
            put("MSG_TYPE_SYSTEM_STRUCT_MSG", -2018)
            put("MSG_TYPE_TEAM_WORK_FILE_IMPORT_SUCCESS_TIPS", -2063)
            put("MSG_TYPE_TEAM_WORK_FILE_IMPORT_SUCCESS_TIPS_DL", -2073)
            put("MSG_TYPE_TEXT", -1000)
            put("MSG_TYPE_TEXT_FRIEND_FEED", -1034)
            put("MSG_TYPE_TEXT_GROUPMAN_ACCEPT", -1021)
            put("MSG_TYPE_TEXT_GROUPMAN_ADDREQUEST", -1020)
            put("MSG_TYPE_TEXT_GROUPMAN_INVITE", -1023)
            put("MSG_TYPE_TEXT_GROUPMAN_REFUSE", -1022)
            put("MSG_TYPE_TEXT_GROUP_CREATED", -1047)
            put("MSG_TYPE_TEXT_QSECRETARY", -1003)
            put("MSG_TYPE_TEXT_RECOMMEND_CIRCLE", -1033)
            put("MSG_TYPE_TEXT_RECOMMEND_CONTACT", -1030)
            put("MSG_TYPE_TEXT_RECOMMEND_TROOP", -1039)
            put("MSG_TYPE_TEXT_RECOMMEND_TROOP_BUSINESS", -1040)
            put("MSG_TYPE_TEXT_SAFE", -1002)
            put("MSG_TYPE_TEXT_SYSTEM_ACCEPT", -1008)
            put("MSG_TYPE_TEXT_SYSTEM_ACCEPTANDADD", -1007)
            put("MSG_TYPE_TEXT_SYSTEM_ADDREQUEST", -1006)
            put("MSG_TYPE_TEXT_SYSTEM_ADDSUCCESS", -1010)
            put("MSG_TYPE_TEXT_SYSTEM_OLD_VERSION_ADDREQUEST", -1011)
            put("MSG_TYPE_TEXT_SYSTEM_REFUSE", -1009)
            put("MSG_TYPE_TEXT_VIDEO", -1001)
            put("MSG_TYPE_TIM_AIOMSG_TIPS", -3016)
            put("MSG_TYPE_TIM_DOUFU_GUIDE", -3015)
            put("MSG_TYPE_TIM_GUIDE", -3014)
            put("MSG_TYPE_TRIBE_SHORT_VIDEO", -7002)
            put("MSG_TYPE_TROOP_CONFESS", -2067)
            put("MSG_TYPE_TROOP_DELIVER_GIFT", -2035)
            put("MSG_TYPE_TROOP_DELIVER_GIFT_OBJ", -2038)
            put("MSG_TYPE_TROOP_EFFECT_PIC", -5015)
            put("MSG_TYPE_TROOP_FEE", -2036)
            put("MSG_TYPE_TROOP_GAP_GRAY_TIPS", -2030)
            put("MSG_TYPE_TROOP_MIXED", -30003)
            put("MSG_TYPE_TROOP_NEWER_POBING", -2059)
            put("MSG_TYPE_TROOP_OBJ_MSG", -2017)
            put("MSG_TYPE_TROOP_REWARD", -2048)
            put("MSG_TYPE_TROOP_SIGN", -2054)
            put("MSG_TYPE_TROOP_STAR_LEAGUE", -2069)
            put("MSG_TYPE_TROOP_STORY", -2057)
            put("MSG_TYPE_TROOP_TIPS_ADD_MEMBER", -1012)
            put("MSG_TYPE_TROOP_UNREAD_TIPS", -4009)
            put("MSG_TYPE_TROOP_WANT_GIFT_MSG", -2056)
            put("MSG_TYPE_UNCOMMONLY_USED_CONTACTS", -1026)
            put("MSG_TYPE_UNCOMMONLY_USED_CONTACTS_CANCEL_SET", -1027)
            put("MSG_TYPE_UNITE_GRAY_HISTORY_INVI", -5021)
            put("MSG_TYPE_UNITE_GRAY_NORMAL", -5040)
            put("MSG_TYPE_UNITE_GRAY_TAB_INVI", -5020)
            put("MSG_TYPE_UNITE_TAB_DB_INVI", -5022)
            put("MSG_TYPE_UNITE_TAB_HISTORI_INVI", -5023)
            put("MSG_TYPE_VAS_APOLLO", -2039)
            put("MSG_TYPE_VIP_AIO_SEND_TIPS", -4022)
            put("MSG_TYPE_VIP_DONATE", -2047)
            put("MSG_TYPE_VIP_KEYWORD", -4021)
            put("MSG_TYPE_VIDEO_EMOTICON", -2079)
            put("MSG_TYPE_VIP_VIDEO", -2045)
            put("MSG_TYPE_YANZHI", -2070)
            put("MSG_TYPE_ZPLAN", -2078)
            put("MSG_TYPE_GUILD_APP_CHANNEL", -4051)
            put("MSG_TYPE_GUILD_GIFT", -5009)
            put("MSG_TYPE_GUILD_LIVE_GIFT", -4070)
            put("MSG_TYPE_GUILD_MARK_DOWN", -4052)
            put("MSG_TYPE_GUILD_REVOKE_GRAY_TIP", -4050)
            put("MSG_TYPE_GUILD_ROBOT_WELCOME_TIPS", -4090)
            put("MSG_TYPE_GUILD_WELCOME_TIPS", -4028)
            put("MSG_TYPE_GUILD_YOLO_GAME_RESULT", -4029)
            put("MSG_TYPE_GUILD_YOLO_SYSTEM", -4030)
            put("MSG_TYPE_GUILD_YOLO_TEAM", -4031)
        }
    }
    val DESC = object : HashBiMap<String, String>() {
        init {
            put("MSG_TYPE_GUILD_APP_CHANNEL", "频道消息")
            put("MSG_TYPE_GUILD_GIFT", "频道礼物")
            put("MSG_TYPE_GUILD_LIVE_GIFT", "频道直播礼物")
            put("MSG_TYPE_GUILD_REVOKE_GRAY_TIP", "频道撤回礼物")
            put("MSG_TYPE_GUILD_WELCOME_TIPS", "频道欢迎提示")
            put("MSG_TYPE_TEXT", "文本消息")
            put("MSG_TYPE_TEXT_VIDEO", "小视频")
            put("MSG_TYPE_TROOP_TIPS_ADD_MEMBER", "加群消息")
            put("MSG_TYPE_TEXT_FRIEND_FEED", "好友签名卡片消息")
            put("MSG_TYPE_MIX", "混合消息")
            put("MSG_TYPE_REPLY_TEXT", "回复消息")
            put("MSG_TYPE_MEDIA_PIC", "图片消息")
            put("MSG_TYPE_MEDIA_PTT", "语音消息")
            put("MSG_TYPE_MEDIA_FILE", "文件")
            put("MSG_TYPE_MEDIA_MARKFACE", "表情消息[从QQ表情商店下载的表情]")
            put("MSG_TYPE_MEDIA_VIDEO", "语音通话/视频通话")
            put("MSG_TYPE_STRUCT_MSG", "卡片消息[分享/签到/转发消息等]")
            put("MSG_TYPE_ARK_APP", "小程序分享消息")
            put("MSG_TYPE_POKE_MSG", "戳一戳")
            put("MSG_TYPE_POKE_EMO_MSG", "另类戳一戳")
            put("MSG_TYPE_UNITE_GRAY_NORMAL", "灰字消息")
            put("MSG_TYPE_SHAKE_WINDOW", "窗口抖动")
            put("MSG_TYPE_QQWALLET_MSG", "红包消息")
        }
    }

    // com.tencent.mobileqq.aio.msglist.holder.component.nick.block.AbsNickBlock -> com.tencent.mobileqq.aio.msglist.holder.component.nick.block.c
    // public class ? extends com.tencent.mobileqq.aio.msglist.holder.component.nick.block.AbsNickBlock
    val NICK_BLOCKS = object : HashBiMap<String, String>() {
        init {
            // extends AbsNickBlockProvider
            // com.tencent.mobileqq.vas.vipicon.VasNickBlockProvider
            put("VIP图标", "AIOVipIconProcessor")// com.tencent.mobileqq.vas.vipicon.AIOVipIconProcessor
            put("额外VIP图标", "AIOVipIconExProcessor")// com.tencent.mobileqq.vas.vipicon.AIOVipIconExProcessor
            put("游戏图标", "AIOGameIconProcessor")// com.tencent.mobileqq.vas.vipicon.AIOGameIconProcessor

            // com.tencent.mobileqq.aio.msglist.holder.component.nick.block.NickBlockProvider
            put("昵称", "MainNickNameBlock")// com.tencent.mobileqq.aio.msglist.holder.component.nick.block.MainNickNameBlock
            put("机器人图标", "NickNameRobotBlock")// com.tencent.mobileqq.aio.msglist.holder.component.nick.robot.NickNameRobotBlock

            // com.tencent.qqnt.aio.nick.ExtNickBlockProvider
            put("群等级图标", "AIOTroopMemberLevelBlock")// com.tencent.qqnt.aio.nick.memberlevel.AIOTroopMemberLevelBlock
            put("群数字等级图标", "AIOTroopMemberGradeLevelBlock")// com.tencent.qqnt.aio.gradelevel.AIOTroopMemberGradeLevelBlock
            put("群荣耀图标", "AIOTroopHonorNickBlock")// com.tencent.qqnt.aio.mutualmark.AIOTroopHonorNickBlock
            put("群匿名图标", "AIOTroopAnonymousNickBlock")// com.tencent.qqnt.aio.anonymous.AIOTroopAnonymousNickBlock

            // com.tencent.mobileqq.activity.qcircle.c
            put("群QQ圈图标", "QCircleTroopIconProcessor")// com.tencent.mobileqq.activity.qcircle.QCircleTroopIconProcessor

        }
    }

    val MSG_WITH_DESC = object : HashBiMap<String, Int>() {
        init {
            MSG.forEach {
                put(getDesc(it.key), it.value)
            }
        }
    }

    /**
     * 将像 MSG_TYPE_YANZHI 这样的东西转换为 int 值方便判断
     *
     * @param activeItems 列表
     * @return 列表
     */
    fun parse(activeItems: List<String>): List<Int> {
        val items: MutableList<Int> = ArrayList()
        for (item in activeItems) {
            if (MSG_WITH_DESC.containsKey(item)) {
                items.add(MSG_WITH_DESC[item]!!)
            }
        }
        return items
    }

    /**
     * 通过 "MSG_TYPE_TEXT" 这样的转换为 "文本类消息" 这样的
     *
     * @param s "MSG_TYPE_TEXT" 这样的
     * @return "文本类消息" 这样的
     */
    fun getDesc(s: String?): String {
//        Toasts.info(Utils.getQQAppInterface().getApplication(), s);
        return if (DESC.containsKey(s)) {
//            Toasts.info(Utils.getQQAppInterface().getApplication(), DESC.get(s));
            DESC[s]!! + '\n' + s!!
        } else s!!
    }

    /**
     * 通过 "文本类消息" 这样的转换为 "MSG_TYPE_TEXT" 这样的
     *
     *
     * *******留着未来使用*******
     *
     * @param cache "文本类消息" 这样的
     * @return "MSG_TYPE_TEXT" 这样的
     */
    fun getKeys(cache: List<String?>): List<String> {
        val ls: MutableList<String> = ArrayList()
        for (d in ls) {
            ls.add(getKey(d))
        }
        return ls
    }

    /**
     * 通过 "文本类消息" 这样的转换为 "MSG_TYPE_TEXT" 这样的
     *
     *
     * *******留着未来使用*******
     *
     * @param cache "文本类消息" 这样的
     * @return "MSG_TYPE_TEXT" 这样的
     */
    fun getKey(cache: String): String {
        val map = DESC.reverseMap
        return if (map.containsKey(cache)) {
            map[cache]!!
        } else cache
    }

    fun parseNickBlocks(activeItems: List<String>): List<String> {
        val items: MutableList<String> = ArrayList()
        for (item in activeItems) {
            if (NICK_BLOCKS.containsKey(item)) {
                items.add(NICK_BLOCKS[item]!!)
            }
        }
        return items
    }
}
