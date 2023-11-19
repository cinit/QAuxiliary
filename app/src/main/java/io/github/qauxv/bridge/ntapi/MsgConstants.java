/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package io.github.qauxv.bridge.ntapi;

public class MsgConstants {

    private MsgConstants() {
    }

    public static final int ARK_STRUCT_ELEMENT_SUB_TYPE_TENCENT_DOC_FROM_MINI_APP = 1;
    public static final int ARK_STRUCT_ELEMENT_SUB_TYPE_TENCENT_DOC_FROM_PLUS_PANEL = 2;
    public static final int ARK_STRUCT_ELEMENT_SUB_TYPE_UNKNOWN = 0;
    public static final int AT_TYPE_ALL = 1;
    public static final int AT_TYPE_CHANNEL = 16;
    public static final int AT_TYPE_ME = 4;
    public static final int AT_TYPE_ONE = 2;
    public static final int AT_TYPE_ONLINE = 64;
    public static final int AT_TYPE_ROLE = 8;
    public static final int AT_TYPE_SUMMON = 32;
    public static final int AT_TYPE_SUMMON_ONLINE = 128;
    public static final int AT_TYPE_SUMMON_ROLE = 256;
    public static final int AT_TYPE_UNKNOWN = 0;
    public static final int CALENDAR_ELEM_SUB_TYPE_COMMON = 3;
    public static final int CALENDAR_ELEM_SUB_TYPE_STRONG = 1;
    public static final int CALENDAR_ELEM_SUB_TYPE_UNKNOWN = 0;
    public static final int CALENDAR_ELEM_SUB_TYPE_WEAK = 2;
    public static final int FACE_BUBBLE_ELEM_SUB_TYPE_NORMAL = 1;
    public static final int FACE_BUBBLE_ELEM_SUB_TYPE_UNKNOWN = 0;
    public static final int FETCH_LONG_MSG_ERR_CODE_MSG_EXPIRED = 196;
    public static final int FILE_ELEM_SUB_TYPE_AI = 16;
    public static final int FILE_ELEM_SUB_TYPE_APP = 11;
    public static final int FILE_ELEM_SUB_TYPE_AUDIO = 3;
    public static final int FILE_ELEM_SUB_TYPE_DOC = 4;
    public static final int FILE_ELEM_SUB_TYPE_EMOTICON = 15;
    public static final int FILE_ELEM_SUB_TYPE_EXCEL = 6;
    public static final int FILE_ELEM_SUB_TYPE_FOLDER = 13;
    public static final int FILE_ELEM_SUB_TYPE_HTML = 10;
    public static final int FILE_ELEM_SUB_TYPE_IPA = 14;
    public static final int FILE_ELEM_SUB_TYPE_NORMAL = 0;
    public static final int FILE_ELEM_SUB_TYPE_PDF = 7;
    public static final int FILE_ELEM_SUB_TYPE_PIC = 1;
    public static final int FILE_ELEM_SUB_TYPE_PPT = 5;
    public static final int FILE_ELEM_SUB_TYPE_PSD = 12;
    public static final int FILE_ELEM_SUB_TYPE_TXT = 8;
    public static final int FILE_ELEM_SUB_TYPE_VIDEO = 2;
    public static final int FILE_ELEM_SUB_TYPE_ZIP = 9;
    public static final int GRAY_TYPE_ELEM_SUB_TYPE_AIO_OP = 15;
    public static final int GRAY_TYPE_ELEM_SUB_TYPE_BLOCK = 14;
    public static final int GRAY_TYPE_ELEM_SUB_TYPE_BUDDY = 5;
    public static final int GRAY_TYPE_ELEM_SUB_TYPE_BUDDY_NOTIFY = 9;
    public static final int GRAY_TYPE_ELEM_SUB_TYPE_EMOJI_REPLY = 3;
    public static final int GRAY_TYPE_ELEM_SUB_TYPE_ESSENCE = 7;
    public static final int GRAY_TYPE_ELEM_SUB_TYPE_FEED = 6;
    public static final int GRAY_TYPE_ELEM_SUB_TYPE_FEED_CHANNEL_MSG = 11;
    public static final int GRAY_TYPE_ELEM_SUB_TYPE_FILE = 10;
    public static final int GRAY_TYPE_ELEM_SUB_TYPE_GROUP = 4;
    public static final int GRAY_TYPE_ELEM_SUB_TYPE_GROUP_NOTIFY = 8;
    public static final int GRAY_TYPE_ELEM_SUB_TYPE_JSON = 17;
    public static final int GRAY_TYPE_ELEM_SUB_TYPE_LOCAL_MSG = 13;
    public static final int GRAY_TYPE_ELEM_SUB_TYPE_PROCLAMATION = 2;
    public static final int GRAY_TYPE_ELEM_SUB_TYPE_REVOKE = 1;
    public static final int GRAY_TYPE_ELEM_SUB_TYPE_UNKNOWN = 0;
    public static final int GRAY_TYPE_ELEM_SUB_TYPE_WALLET = 16;
    public static final int GRAY_TYPE_ELEM_SUB_TYPE_XML_MSG = 12;
    public static final int INPUT_STATUS_TYPE_CANCEL = 2;
    public static final int INPUT_STATUS_TYPE_SPEAK = 3;
    public static final int INPUT_STATUS_TYPE_TEXT = 1;

    public static final int ACTIVITY_MSG = 22;
    public static final int ANONYMOUS_AT_MEMSG_TYPE_IN_MSG_BOX = 1001;
    public static final int ANONYMOUS_FLAG_FROM_OTHER_PEOPLE = 1;
    public static final int ANONYMOUS_FLAG_FROM_OWN = 2;
    public static final int ANONYMOUS_FLAG_INVALID = 0;
    public static final int APP_CHANNEL_MSG = 16;
    public static final int AT_ALL_MSG_TYPE_IN_MSG_BOX = 2000;
    public static final int AT_ME_MSG_TYPE_IN_MSG_BOX = 1000;
    public static final int ATTRIBUTE_TYPE_GROUP_HONOR = 2;
    public static final int ATTRIBUTE_TYPE_KING_HONOR = 3;
    public static final int ATTRIBUTE_TYPE_LONG_MSG = 8;
    public static final int ATTRIBUTE_TYPE_MSG = 0;
    public static final int ATTRIBUTE_TYPE_PERSONAL = 1;
    public static final int ATTRIBUTE_TYPE_PUBLIC_ACCOUNT = 4;
    public static final int ATTRIBUTE_TYPE_SHARED_MSG_INFO = 5;
    public static final int ATTRIBUTE_TYPE_TEMP_CHAT_GAME_SESSION = 6;
    public static final int ATTRIBUTE_TYPE_TO_ROBOT_MSG = 9;
    public static final int ATTRIBUTE_TYPE_UIN_INFO = 7;
    public static final int ATTRIBUTE_TYPE_Z_PLAN = 11;
    public static final int AUTO_REPLY_TEXT_NONE_INDEX = -1;
    public static final int AV_RECORD_MSG = 19;
    public static final int BUSINESS_TYPE_GUILD = 1;
    public static final int BUSINESS_TYPE_NT = 0;
    public static final int CHAT_TYPE_BUDDY_NOTIFY = 5;
    public static final int CHAT_TYPE_C2C = 1;
    public static final int CHAT_TYPE_CIRCLE = 113;
    public static final int CHAT_TYPE_DATA_LINE = 8;
    public static final int CHAT_TYPE_DATA_LINE_MQQ = 134;
    public static final int CHAT_TYPE_DISC = 3;
    public static final int CHAT_TYPE_FAV = 41;
    public static final int CHAT_TYPE_GAME_MESSAGE = 105;
    public static final int CHAT_TYPE_GAME_MESSAGE_FOLDER = 116;
    public static final int CHAT_TYPE_GROUP = 2;
    public static final int CHAT_TYPE_GROUP_BLESS = 133;
    public static final int CHAT_TYPE_GROUP_GUILD = 9;
    public static final int CHAT_TYPE_GROUP_HELPER = 7;
    public static final int CHAT_TYPE_GROUP_NOTIFY = 6;
    public static final int CHAT_TYPE_GUILD = 4;
    public static final int CHAT_TYPE_GUILD_META = 16;
    public static final int CHAT_TYPE_MATCH_FRIEND = 104;
    public static final int CHAT_TYPE_MATCH_FRIEND_FOLDER = 109;
    public static final int CHAT_TYPE_NEARBY = 106;
    public static final int CHAT_TYPE_NEARBY_ASSISTANT = 107;
    public static final int CHAT_TYPE_NEARBY_FOLDER = 110;
    public static final int CHAT_TYPE_NEARBY_HELLO_FOLDER = 112;
    public static final int CHAT_TYPE_NEARBY_INTERACT = 108;
    public static final int CHAT_TYPE_QQ_NOTIFY = 132;
    public static final int CHAT_TYPE_RELATE_ACCOUNT = 131;
    public static final int CHAT_TYPE_SERVICE_ASSISTANT = 118;
    public static final int CHAT_TYPE_SERVICE_ASSISTANT_SUB = 201;
    public static final int CHAT_TYPE_SQUARE_PUBLIC = 115;
    public static final int CHAT_TYPE_SUBSCRIBE_FOLDER = 30;
    public static final int CHAT_TYPE_TEMP_ADDRESS_BOOK = 111;
    public static final int CHAT_TYPE_TEMP_BUSSINESS_CRM = 102;
    public static final int CHAT_TYPE_TEMP_C2C_FROM_GROUP = 100;
    public static final int CHAT_TYPE_TEMP_C2C_FROM_UNKNOWN = 99;
    public static final int CHAT_TYPE_TEMP_FRIEND_VERIFY = 101;
    public static final int CHAT_TYPE_TEMP_NEARBY_PRO = 119;
    public static final int CHAT_TYPE_TEMP_PUBLIC_ACCOUNT = 103;
    public static final int CHAT_TYPE_TEMP_WPA = 117;
    public static final int CHAT_TYPE_UNKNOWN = 0;
    public static final int CHAT_TYPE_WEIYUN = 40;
    public static final int COMMON_RED_ENVELOPE_MSG_TYPE_IN_MSG_BOX = 1007;
    public static final int DOWN_SOURCE_TYPE_AIO_INNER = 1;
    public static final int DOWN_SOURCE_TYPE_BIG_SCREEN = 2;
    public static final int DOWN_SOURCE_TYPE_HISTORY = 3;
    public static final int DOWN_SOURCE_TYPE_UNKNOWN = 0;

    public static final int ELEM_TYPE_ACTIVITY = 25;
    public static final int ELEM_TYPE_ACTIVITY_STATE = 41;
    public static final int ELEM_TYPE_ACTIVITY_SUB_TYPE_CREATE_MOBA_TEAM = 12;
    public static final int ELEM_TYPE_ACTIVITY_SUB_TYPE_DISBAND_MOBA_TEAM = 11;
    public static final int ELEM_TYPE_ACTIVITY_SUB_TYPE_FINISH_GAME = 16;
    public static final int ELEM_TYPE_ACTIVITY_SUB_TYPE_FINISH_MATCH_TEAM = 14;
    public static final int ELEM_TYPE_ACTIVITY_SUB_TYPE_HOT_CHAT = 10000;
    public static final int ELEM_TYPE_ACTIVITY_SUB_TYPE_MINI_GAME = 18;
    public static final int ELEM_TYPE_ACTIVITY_SUB_TYPE_MUSIC_PLAY = 17;
    public static final int ELEM_TYPE_ACTIVITY_SUB_TYPE_NEWS_MOBA = 9;
    public static final int ELEM_TYPE_ACTIVITY_SUB_TYPE_NO_LIVE = 2;
    public static final int ELEM_TYPE_ACTIVITY_SUB_TYPE_NO_SCREEN_SHARE = 7;
    public static final int ELEM_TYPE_ACTIVITY_SUB_TYPE_NO_VOICE = 3;
    public static final int ELEM_TYPE_ACTIVITY_SUB_TYPE_ON_LIVE = 1;
    public static final int ELEM_TYPE_ACTIVITY_SUB_TYPE_ON_SCREEN_SHARE = 6;
    public static final int ELEM_TYPE_ACTIVITY_SUB_TYPE_ON_VOICE = 4;
    public static final int ELEM_TYPE_ACTIVITY_SUB_TYPE_START_MATCH_TEAM = 13;
    public static final int ELEM_TYPE_ACTIVITY_SUB_TYPE_TART_GAME = 15;
    public static final int ELEM_TYPE_ACTIVITY_SUB_TYPE_UNKNOWN = 0;
    public static final int ELEM_TYPE_ARK_STRUCT = 10;
    public static final int ELEM_TYPE_AV_RECORD = 21;
    public static final int ELEM_TYPE_CALENDAR = 19;
    public static final int ELEM_TYPE_FACE = 6;
    public static final int ELEM_TYPE_FACE_BUBBLE = 27;
    public static final int ELEM_TYPE_FEED = 22;
    public static final int ELEM_TYPE_FILE = 3;
    public static final int ELEM_TYPE_GIPHY = 15;
    public static final int ELEM_TYPE_GRAY_TIP = 8;
    public static final int ELEM_TYPE_INLINE_KEYBOARD = 17;
    public static final int ELEM_TYPE_IN_TEXT_GIFT = 18;
    public static final int ELEM_TYPE_LIVE_GIFT = 12;
    public static final int ELEM_TYPE_MARKDOWN = 14;
    public static final int ELEM_TYPE_MARKET_FACE = 11;
    public static final int ELEM_TYPE_MULTI_FORWARD = 16;
    public static final int ELEM_TYPE_ONLINE_FILE = 23;
    public static final int ELEM_TYPE_PIC = 2;
    public static final int ELEM_TYPE_PTT = 4;
    public static final int ELEM_TYPE_REPLY = 7;
    public static final int ELEM_TYPE_SHARE_LOCATION = 28;
    public static final int ELEM_TYPE_STRUCT_LONG_MSG = 13;
    public static final int ELEM_TYPE_TASK_TOP_MSG = 29;
    public static final int ELEM_TYPE_TEXT = 1;
    public static final int ELEM_TYPE_TOFU = 26;
    public static final int ELEM_TYPE_UNKNOWN = 0;
    public static final int ELEM_TYPE_VIDEO = 5;
    public static final int ELEM_TYPE_WALLET = 9;
    public static final int ELEM_TYPE_YOLO_GAME_RESULT = 20;
    public static final int ENTER_AIO = 1;
    public static final int EXIT_AIO = 2;
    public static final int FRIEND_NEW_ADDED_MSG_TYPE_IN_MSG_BOX = 1008;
    public static final int GIFT_AT_ME_MSG_TYPE_IN_MSG_BOX = 1005;
    public static final int GROUP_FILE_AT_ALL_MSG_TYPE_IN_MSG_BOX = 2001;
    public static final int GROUP_KEYWORD_MSG_TYPE_IN_MSG_BOX = 2006;
    public static final int GROUP_ANNOUNCE_AT_ALL_MSG_TYPE_IN_MSG_BOX = 2004;
    public static final int GROUP_TASK_AT_ALL_MSG_TYPE_IN_MSG_BOX = 2003;
    public static final int GROUP_UNREAD_TYPE_IN_MSG_BOX = 2007;
    public static final int HIGHLIGHT_WORD_IN_TEMP_CHAT_TYPE_IN_MSG_BOX = 1009;
    public static final int MARKET_FACE = 17;
    public static final int MINI_PROGRAM_NOTICE = 114;

    public static final int MSG_SUB_TYPE_ARK_GROUP_ANNOUNCE = 3;
    public static final int MSG_SUB_TYPE_ARK_GROUP_ANNOUNCE_CONFIRM_REQUIRED = 4;
    public static final int MSG_SUB_TYPE_ARK_GROUP_GIFT_AT_ME = 5;
    public static final int MSG_SUB_TYPE_ARK_GROUP_TASK_AT_ALL = 6;
    public static final int MSG_SUB_TYPE_ARK_MULTI_MSG = 7;
    public static final int MSG_SUB_TYPE_ARK_NORMAL = 0;
    public static final int MSG_SUB_TYPE_ARK_TENCENT_DOC_FROM_MINI_APP = 1;
    public static final int MSG_SUB_TYPE_ARK_TENCENT_DOC_FROM_PLUS_PANEL = 2;
    public static final int MSG_SUB_TYPE_EMOTICON = 15;
    public static final int MSG_SUB_TYPE_FILE_APP = 11;
    public static final int MSG_SUB_TYPE_FILE_AUDIO = 3;
    public static final int MSG_SUB_TYPE_FILE_DOC = 4;
    public static final int MSG_SUB_TYPE_FILE_EXCEL = 6;
    public static final int MSG_SUB_TYPE_FILE_FOLDER = 13;
    public static final int MSG_SUB_TYPE_FILE_HTML = 10;
    public static final int MSG_SUB_TYPE_FILE_IPA = 14;
    public static final int MSG_SUB_TYPE_FILE_NORMAL = 0;
    public static final int MSG_SUB_TYPE_FILE_PDF = 7;
    public static final int MSG_SUB_TYPE_FILE_PIC = 1;
    public static final int MSG_SUB_TYPE_FILE_PPT = 5;
    public static final int MSG_SUB_TYPE_FILE_PSD = 12;
    public static final int MSG_SUB_TYPE_FILE_TXT = 8;
    public static final int MSG_SUB_TYPE_FILE_VIDEO = 2;
    public static final int MSG_SUB_TYPE_FILE_ZIP = 9;
    public static final int MSG_SUB_TYPE_LINK = 5;
    public static final int MSG_SUB_TYPE_MARKET_FACE = 1;
    public static final int MSG_SUB_TYPE_MIX_EMOTICON = 7;
    public static final int MSG_SUB_TYPE_MIX_FACE = 3;
    public static final int MSG_SUB_TYPE_MIX_MARKET_FACE = 2;
    public static final int MSG_SUB_TYPE_MIX_PIC = 1;
    public static final int MSG_SUB_TYPE_MIX_REPLY = 4;
    public static final int MSG_SUB_TYPE_MIX_TEXT = 0;
    public static final int MSG_SUB_TYPE_TENCENT_DOC = 6;
    public static final int MSG_TYPE_ARK_STRUCT = 11;
    public static final int MSG_TYPE_FACE_BUBBLE = 24;
    public static final int MSG_TYPE_FILE = 3;
    public static final int MSG_TYPE_GIFT = 14;
    public static final int MSG_TYPE_GIPHY = 13;
    public static final int MSG_TYPE_GRAY_TIPS = 5;
    public static final int MSG_TYPE_MIX = 2;
    public static final int MSG_TYPE_MULTI_MSG_FORWARD = 8;
    public static final int MSG_TYPE_NULL = 1;
    public static final int MSG_TYPE_ONLINE_FILE = 21;
    public static final int MSG_TYPE_PTT = 6;
    public static final int MSG_TYPE_REPLY = 9;
    public static final int MSG_TYPE_SHARE_LOCATION = 25;
    public static final int MSG_TYPE_STRUCT = 4;
    public static final int MSG_TYPE_STRUCT_LONG_MSG = 12;
    public static final int MSG_TYPE_TEXT_GIFT = 15;
    public static final int MSG_TYPE_UNKNOWN = 0;
    public static final int MSG_TYPE_VIDEO = 7;
    public static final int MSG_TYPE_WALLET = 10;
    public static final int NEED_CONFIRM_GROUP_ANNOUNCE_AT_ALL_MSG_TYPE_IN_MSG_BOX = 2005;
    public static final int PTT_FORMAT_TYPE_AMR = 0;
    public static final int PTT_FORMAT_TYPE_SILK = 1;
    public static final int PTT_TRANSLATE_STATUS_FAIL = 3;
    public static final int PTT_TRANSLATE_STATUS_SUC = 2;
    public static final int PTT_TRANSLATE_STATUS_TRANSLATING = 1;
    public static final int PTT_TRANSLATE_STATUS_UNKNOWN = 0;
    public static final int PTT_VIP_LEVEL_TYPE_NONE = 0;
    public static final int PTT_VIP_LEVEL_TYPE_QQ_VIP = 0;
    public static final int PTT_VIP_LEVEL_TYPE_SVIP = 0;
    public static final int PTT_VOICE_CHANGE_TYPE_BEAST_MACHINE = 7;
    public static final int PTT_VOICE_CHANGE_TYPE_BOY = 2;
    public static final int PTT_VOICE_CHANGE_TYPE_CATCH_COLD = 13;
    public static final int PTT_VOICE_CHANGE_TYPE_ECHO = 5;
    public static final int PTT_VOICE_CHANGE_TYPE_FAT_GUY = 16;
    public static final int PTT_VOICE_CHANGE_TYPE_FLASHING = 9;
    public static final int PTT_VOICE_CHANGE_TYPE_GIRL = 1;
    public static final int PTT_VOICE_CHANGE_TYPE_HORRIBLE = 3;
    public static final int PTT_VOICE_CHANGE_TYPE_KINDERGARTEN = 6;
    public static final int PTT_VOICE_CHANGE_TYPE_MEDAROT = 15;
    public static final int PTT_VOICE_CHANGE_TYPE_NONE = 0;
    public static final int PTT_VOICE_CHANGE_TYPE_OPTIMUS_PRIME = 8;
    public static final int PTT_VOICE_CHANGE_TYPE_OUT_OF_DATE = 14;
    public static final int PTT_VOICE_CHANGE_TYPE_PAPI = 11;
    public static final int PTT_VOICE_CHANGE_TYPE_QUICK = 4;
    public static final int PTT_VOICE_CHANGE_TYPE_STUTTER = 10;
    public static final int PTT_VOICE_CHANGE_TYPE_TRAPPED_BEAST = 12;
    public static final int PTT_VOICE_TYPE_INTERCOM = 1;
    public static final int PTT_VOICE_TYPE_SOUND_RECORD = 2;
    public static final int PTT_VOICE_TYPE_UNKNOWN = 0;
    public static final int PTT_VOICE_TYPE_VOICE_CHANGE = 3;
    public static final int PUBLIC_ACCOUNT_TIANSHU_HIGHLIGHT_WORD_TYPE_IN_MSG_BOX = 1010;
    public static final int REPLY_ABS_ELEM_TYPE_FACE = 2;
    public static final int REPLY_ABS_ELEM_TYPE_TEXT = 1;
    public static final int REPLY_ABS_ELEM_TYPE_UNKNOWN = 0;
    public static final int REPLY_AT_ME_MSG_TYPE_IN_MSG_BOX = 1002;
    public static final int RM_DOWN_TYPE_ORIG = 1;
    public static final int RM_DOWN_TYPE_THUMB = 2;
    public static final int RM_DOWN_TYPE_UNKNOWN = 0;
    public static final int RM_FILE_THUMB_SIZE_128 = 128;
    public static final int RM_FILE_THUMB_SIZE_320 = 320;
    public static final int RM_FILE_THUMB_SIZE_384 = 384;
    public static final int RM_FILE_THUMB_SIZE_750 = 750;
    public static final int RM_PIC_AIO_THUMB_SIZE = 0;
    public static final int RM_PIC_THUMB_SIZE_198 = 198;
    public static final int RM_PIC_THUMB_SIZE_720 = 720;
    public static final int RM_PIC_TYPE_BMP = 3;
    public static final int RM_PIC_TYPE_CHECK_OTHER = 900;
    public static final int RM_PIC_TYPE_GIF = 2;
    public static final int RM_PIC_TYPE_JPG = 0;
    public static final int RM_PIC_TYPE_NEW_PIC_APNG = 2001;
    public static final int RM_PIC_TYPE_NEW_PIC_BMP = 1005;
    public static final int RM_PIC_TYPE_NEW_PIC_GIF = 2000;
    public static final int RM_PIC_TYPE_NEW_PIC_JPEG = 1000;
    public static final int RM_PIC_TYPE_NEW_PIC_PNG = 1001;
    public static final int RM_PIC_TYPE_NEW_PIC_PROGRESSIVE_JPEG = 1003;
    public static final int RM_PIC_TYPE_NEW_PIC_SHARPP = 1004;
    public static final int RM_PIC_TYPE_NEW_PIC_WEBP = 1002;
    public static final int RM_PIC_TYPE_PNG = 1;
    public static final int RM_PIC_TYPE_UNKNOWN = 0;
    public static final int RM_THUMB_SIZE_ZERO = 0;
    public static final int RM_TRANSFER_STATUS_DOWNLOADING = 3;
    public static final int RM_TRANSFER_STATUS_FAIL = 5;
    public static final int RM_TRANSFER_STATUS_INIT = 1;
    public static final int RM_TRANSFER_STATUS_SUC = 4;
    public static final int RM_TRANSFER_STATUS_UNKNOWN = 0;
    public static final int RM_TRANSFER_STATUS_UPLOADING = 2;
    public static final int RM_TRANSFER_STATUS_USER_CANCEL = 6;

    public static final int SEND_STATUS_FAILED = 0;
    public static final int SEND_STATUS_SENDING = 1;
    public static final int SEND_STATUS_SUCCESS = 2;
    public static final int SEND_STATUS_SUCCESS_NO_SEQ = 3;
    public static final int SEND_TYPE_DROPPED = 6;
    public static final int SEND_TYPE_LOCAL = 3;
    public static final int SEND_TYPE_OTHER_DEVICE = 2;
    public static final int SEND_TYPE_RECV = 0;
    public static final int SEND_TYPE_SELF = 1;
    public static final int SEND_TYPE_SELF_FORWARD = 4;
    public static final int SEND_TYPE_SELF_MULTI_FORWARD = 5;
    public static final int SESSION_TYPE_ADDRESS_BOOK = 5;
    public static final int SESSION_TYPE_C2C = 1;
    public static final int SESSION_TYPE_DISC = 3;
    public static final int SESSION_TYPE_FAV = 41;
    public static final int SESSION_TYPE_GROUP = 2;
    public static final int SESSION_TYPE_GROUP_BLESS = 52;
    public static final int SESSION_TYPE_GUILD = 4;
    public static final int SESSION_TYPE_GUILD_META = 16;
    public static final int SESSION_TYPE_NEARBY_PRO = 54;
    public static final int SESSION_TYPE_QQ_NOTIFY = 51;
    public static final int SESSION_TYPE_RELATE_ACCOUNT = 50;
    public static final int SESSION_TYPE_SERVICE_ASSISTANT = 19;
    public static final int SESSION_TYPE_SUBSCRIBE_FOLDER = 30;
    public static final int SESSION_TYPE_TYPE_BUDDY_NOTIFY = 7;
    public static final int SESSION_TYPE_TYPE_GROUP_HELPER = 9;
    public static final int SESSION_TYPE_TYPE_GROUP_NOTIFY = 8;
    public static final int SESSION_TYPE_UNKNOWN = 0;
    public static final int SESSION_TYPE_WEIYUN = 40;
    public static final int SPECIAL_CARE_MSG_TYPE_IN_MSG_BOX = 1006;
    public static final int SPECIFIED_RED_ENVELOPE_AT_ME_MSG_TYPE_IN_MSG_BOX = 1004;
    public static final int SPECIFIED_RED_ENVELOPE_ONE_MSG_TYPE_IN_MSG_BOX = 1003;
    public static final int TENCENT_DOC_TYPE_ADD_ON = 110;
    public static final int TENCENT_DOC_TYPE_DOC = 0;
    public static final int TENCENT_DOC_TYPE_DRAWING = 89;
    public static final int TENCENT_DOC_TYPE_DRIVE = 101;
    public static final int TENCENT_DOC_TYPE_FILE = 100;
    public static final int TENCENT_DOC_TYPE_FLOW_CHART = 91;
    public static final int TENCENT_DOC_TYPE_FOLDER = 3;
    public static final int TENCENT_DOC_TYPE_FORM = 2;
    public static final int TENCENT_DOC_TYPE_MIND = 90;
    public static final int TENCENT_DOC_TYPE_NOTES = 5;
    public static final int TENCENT_DOC_TYPE_PDF = 6;
    public static final int TENCENT_DOC_TYPE_PROGRAM = 7;
    public static final int TENCENT_DOC_TYPE_SHEET = 1;
    public static final int TENCENT_DOC_TYPE_SLIDE = 4;
    public static final int TENCENT_DOC_TYPE_SMART_CANVAS = 8;
    public static final int TENCENT_DOC_TYPE_SMART_SHEET = 9;
    public static final int TENCENT_DOC_TYPE_SPEECH = 102;
    public static final int TENCENT_DOC_TYPE_UNKNOWN = 10;
    public static final int TOFU_RECORD_MSG = 23;
    public static final int TOP_MSG_TYPE_TASK = 1;
    public static final int TOP_MSG_TYPE_UNKNOWN = 0;
    public static final int TRIGGER_TYPE_AUTO = 1;
    public static final int TRIGGER_TYPE_MANUAL = 0;
    public static final int UNKNOWN_TYPE_IN_MSG_BOX = 0;
    public static final int UNREAD_CNT_UP_TYPE_ALL_DIRECT_SESSION = 4;
    public static final int UNREAD_CNT_UP_TYPE_ALL_FEED_SINGLE_GUILD = 6;
    public static final int UNREAD_CNT_UP_TYPE_ALL_GUILD = 3;
    public static final int UNREAD_CNT_UP_TYPE_CATEGORY = 5;
    public static final int UNREAD_CNT_UP_TYPE_CHANNEL = 1;
    public static final int UNREAD_CNT_UP_TYPE_CONTACT = 0;
    public static final int UNREAD_CNT_UP_TYPE_GUILD = 2;
    public static final int UNREAD_CNT_UP_TYPE_GUILD_GROUP = 7;
    public static final int UNREAD_SHOW_TYPE_GRAY_POINT = 2;
    public static final int UNREAD_SHOW_TYPE_RED_POINT = 1;
    public static final int UNREAD_SHOW_TYPE_SMALL_GRAY_POINT = 4;
    public static final int UNREAD_SHOW_TYPE_SMALL_RED_POINT = 3;
    public static final int UNREAD_SHOW_TYPE_UNKNOWN = 0;
    public static final int VAS_GIFT_COIN_TYPE_COIN = 0;
    public static final int VAS_GIFT_COIN_TYPE_MARKET_COIN = 1;
    public static final int YOLO_GAME_RESULT_MSG = 18;
    public static final int PIC_800_RECOMMENDED = 7;
    public static final int PIC_ALBUM_GIF = 11;
    public static final int PIC_COMMERCIAL_ADVERTISING = 9;
    public static final int PIC_FIND = 10;
    public static final int PIC_HOT = 2;
    public static final int PIC_HOT_EMOJI = 13;
    public static final int PIC_NORMAL = 0;
    public static final int PIC_PK = 3;
    public static final int PIC_QQ_ZONE = 5;
    public static final int PIC_SELFIE_GIF = 8;
    public static final int PIC_SEND_FROM_TAB_SEARCH_BOX = 12;
    public static final int PIC_USER = 1;
    public static final int PIC_WISDOM_FIGURE = 4;
    public static final int REPLY_ORIGINAL_MSG_STATE_HAS_RECALL = 1;
    public static final int REPLY_ORIGINAL_MSG_STATE_UNKNOWN = 0;
    public static final int SHARE_LOCATION_ELEM_SUB_TYPE_NORMAL = 1;
    public static final int SHARE_LOCATION_ELEM_SUB_TYPE_UNKNOWN = 0;
    public static final int TEXT_ELEMENT_SUB_TYPE_LINK = 1;
    public static final int TEXT_ELEMENT_SUB_TYPE_TENCENT_DOC = 2;
    public static final int TEXT_ELEMENT_SUB_TYPE_UNKNOWN = 0;

}

/*
 * Prompt
 * There are some Java constants generated form C++ source, please split words with "_".
 * For example, "public static final int ARKSTRUCTELEMENTSUBTYPETENCENTDOCFROMMINIAPP = 1;" becomes "public static final int
 * ARK_STRUCT_ELEMENT_SUB_TYPE_TENCENT_DOC_FROM_MINIAPP = 1;",
 * and "public static final int ATTYPEALL = 1;" become "public static final int AT_TYPE_ALL = 1;".
 * You may encounter some terms and abbrs, eg, "msg", "ark", "aio", "ptt", "at", "c2c", "fav", "tencent", "uin", "vas".
 * Keep "dataline" as a single word.
 * Please stick to the original text.
 */
