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
package io.github.qauxv.util.dexkit;

import static io.github.qauxv.util.Initiator._BaseChatPie;
import static io.github.qauxv.util.Initiator._ChatMessage;
import static io.github.qauxv.util.Initiator._QQAppInterface;
import static io.github.qauxv.util.Initiator._TroopChatPie;
import static io.github.qauxv.util.Initiator.load;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;
import io.github.qauxv.base.annotation.InternalApi;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * I hadn't obfuscated the source code. I just don't want to name it, leaving it a()
 */
public class DexKit {

    private DexKit() {
    }

    static final String NO_SUCH_CLASS = "Lio/github/qauxv/util/DexKit$NoSuchClass;";
    public static final DexMethodDescriptor NO_SUCH_METHOD = new DexMethodDescriptor(NO_SUCH_CLASS, "a", "()V");
    public static final String KEY_DEX_DEOBFS_BACKEND = "dex_deobfs_backend";
    public static final String DEFAULT_DEX_DEOBFS_BACKEND_DEXBUILDER = LegacyDexDeobfs.INSTANCE.getId();

    //WARN: NEVER change the index!
    public static final int C_DIALOG_UTIL = 1;
    public static final int C_FACADE = 2;
    public static final int C_FLASH_PIC_HELPER = 3;
    public static final int C_BASE_PIC_DL_PROC = 4;
    public static final int C_ItemBuilderFactory = 5;
    public static final int C_AIO_UTILS = 6;
    public static final int C_ABS_GAL_SCENE = 7;
    //public static final int C_FAV_EMO_ROAM_HANDLER = 8;
    public static final int C_FAV_EMO_CONST = 9;
    public static final int C_MessageRecordFactory = 10;
    public static final int C_CONTACT_UTILS = 11;
    //public static final int C_VIP_UTILS = 12;
    public static final int C_ARK_APP_ITEM_BUBBLE_BUILDER = 13;
    public static final int C_PNG_FRAME_UTIL = 14;
    public static final int C_PIC_EMOTICON_INFO = 15;
    public static final int C_SIMPLE_UI_UTIL = 16;
    public static final int C_TROOP_GIFT_UTIL = 17;
    public static final int C_TEST_STRUCT_MSG = 18;
    public static final int C_QZONE_MSG_NOTIFY = 19;
    public static final int C_APP_CONSTANTS = 20;
    public static final int C_CustomWidgetUtil = 21;
    public static final int C_MessageCache = 22;
    public static final int C_ScreenShotHelper = 23;
    public static final int C_TimeFormatterUtils = 24;
    public static final int C_TogetherControlHelper = 25;
    //unknown class name
    public static final int C_GroupAppActivity = 26;
    public static final int C_IntimateDrawer = 27;
    public static final int C_ZipUtils_biz = 28;
    public static final int C_HttpDownloader = 29;
    public static final int C_MultiMsg_Manager = 30;
    public static final int C_ClockInEntryHelper = 31;
    public static final int C_CaptureUtil = 32;
    public static final int C_AvatarUtil = 33;
    //for old version qq NewRoundHead
    public static final int C_FaceManager = 34;
    public static final int C_SmartDeviceProxyMgr = 35;
    public static final int C_AIOPictureView = 36;
    public static final int C_GalleryBaseScene = 37;
    public static final int C_GuildHelperProvider = 38;
    public static final int C_GuildArkHelper = 39;
    public static final int C_ReplyMsgUtils = 40;
    public static final int C_ReplyMsgSender = 41;
    public static final int C_PopOutEmoticonUtil = 42;
    public static final int C_VipStatusManagerImpl = 43;
    public static final int C_SystemMessageProcessor = 44;
    public static final int C_OnlinePushPbPushTransMsg = 45;
    //the last index
    public static final int DEOBF_NUM_C = 45;

    public static final int N_BASE_CHAT_PIE__INIT = 20001;
    public static final int N_BASE_CHAT_PIE__handleNightMask = 20002;
    public static final int N_BASE_CHAT_PIE__updateSession = 20003;
    public static final int N_BASE_CHAT_PIE__createMulti = 20004;
    public static final int N_BASE_CHAT_PIE__chooseMsg = 20005;
    public static final int N_LeftSwipeReply_Helper__reply = 20006;
    public static final int N_AtPanel__showDialogAtView = 20007;
    public static final int N_AtPanel__refreshUI = 20008;
    public static final int N_FriendChatPie_updateUITitle = 20009;
    public static final int N_ProfileCardUtil_getCard = 20010;
    public static final int N_VasProfileTemplateController_onCardUpdate = 20011;
    public static final int N_QQSettingMe_updateProfileBubble = 20012;
    public static final int N_VIP_UTILS_getPrivilegeFlags = 20013;
    public static final int N_TroopChatPie_showNewTroopMemberCount = 20014;
    public static final int N_Conversation_onCreate = 20015;
    public static final int N_QQSettingMe_onResume = 20016;
    public static final int N_BaseChatPie_mosaic = 20017;
    public static final int N_WebSecurityPluginV2_callback = 20018;
    public static final int N_TroopAppShortcutBarHelper_resumeAppShorcutBar = 20019;
    public static final int N_ChatActivityFacade_sendMsgButton = 20020;
    public static final int N_FriendsStatusUtil_isChatAtTop = 20021;
    public static final int N_VipUtils_getUserStatus = 20022;
    public static final int N_PhotoListPanel_resetStatus = 20023;
    public static final int N_ContactUtils_getDiscussionMemberShowName = 20024;
    public static final int N_ContactUtils_getBuddyName = 20025;
    public static final int DEOBF_NUM_N = 25;

    public static final Map<String, DexDeobfsBackend> backends = Map.of(
            LegacyDexDeobfs.INSTANCE.getId(), LegacyDexDeobfs.INSTANCE,
            DexBuilderDexDeobfs.INSTANCE.getId(), DexBuilderDexDeobfs.INSTANCE,
            DexKitDeobfs.INSTANCE.getId(), DexKitDeobfs.INSTANCE
    );

    public static DexDeobfsBackend getCurrentBackend() {
        var id = ConfigManager.getDefaultConfig()
                .getString(KEY_DEX_DEOBFS_BACKEND, DEFAULT_DEX_DEOBFS_BACKEND_DEXBUILDER);
        return backends.get(id);
    }

    /**
     * Run the dex deobfuscation.
     *
     * @param i the dex class index
     * @return true if the dex class is deobfuscated successfully.
     */
    public static boolean prepareFor(int i) {
        if (i / 10000 == 0) {
            return doFindClass(i) != null;
        } else {
            return doFindMethod(i) != null;
        }
    }

    /**
     * Test whether we should run the dex deobfuscation. Note that if a dex class is tried to deobfuscate before, but
     * failed, its failed result will be cached, which means that the same dex class will not be deobfuscated again.
     *
     * @param i the dex class index
     * @return true if time is required to deobfuscate the dex class, false if either the dex class is already
     * found or there was already a failed result.
     */
    public static boolean isRunDexDeobfuscationRequired(int i) {
        if (i / 10000 == 0) {
            // class
            if (loadClassFromCache(i) != null) {
                return false;
            }
            DexMethodDescriptor desc = getMethodDescFromCache(i);
            // either the method is already found or there was a failed result
            return desc == null;
        } else {
            DexMethodDescriptor desc = getMethodDescFromCache(i);
            // either the method is already found or there was a failed result
            return desc == null;
        }
    }

    /**
     * Try to load the obfuscated class from deobfuscation cache. This method does not take much time and may be called
     * in main thread.
     *
     * @param i the dex class index
     * @return null if the dex class is not in deobfuscation cache, otherwise the target class object.
     */
    @Nullable
    public static Class<?> loadClassFromCache(int i) {
        Class<?> ret = Initiator.load(c(i));
        if (ret != null) {
            return ret;
        }
        DexMethodDescriptor m = getMethodDescFromCache(i);
        if (m == null) {
            return null;
        }
        return Initiator.load(m.declaringClass);
    }

    /**
     * Run the dex deobfuscation. This method may take a long time and should only be called in background thread.
     *
     * @param i the dex class index
     * @return the target class object, null if the dex class is not found.
     */
    @Nullable
    public static Class<?> doFindClass(int i) {
        return getCurrentBackend().doFindClass(i);
    }

    /**
     * Try to load the obfuscated method from deobfuscation cache. This method does not take much time and may be called
     * in main thread.
     *
     * @param i the dex method index
     * @return the target method descriptor, null if the target is not found.
     */
    @Nullable
    public static Method getMethodFromCache(int i) {
        if (i / 10000 == 0) {
            throw new IllegalStateException("Index " + i + " attempted to access method!");
        }
        DexMethodDescriptor m = getMethodDescFromCache(i);
        if (m == null || NO_SUCH_METHOD.toString().equals(m.toString())) {
            return null;
        }
        if (m.name.equals("<init>") || m.name.equals("<clinit>")) {
            // TODO: support constructors
            Log.i("getMethodFromCache(" + i + ") methodName == " + m.name + " , return null");
            return null;
        }
        try {
            return m.getMethodInstance(Initiator.getHostClassLoader());
        } catch (NoSuchMethodException e) {
            Log.e(e);
            return null;
        }
    }

    /**
     * Run the dex deobfuscation. This method may take a long time and should only be called in background thread.
     *
     * @param i the dex method index
     * @return target method descriptor, null if the target is not found.
     */
    @Nullable
    public static Method doFindMethod(int i) {
        if (i / 10000 == 0) {
            throw new IllegalStateException("Index " + i + " attempted to access method!");
        }
        return getCurrentBackend().doFindMethod(i);
    }

    /**
     * Try to load the obfuscated method from deobfuscation cache. This method does not take much time and may be called
     * in main thread.
     *
     * @param i the dex method index
     * @return the target method descriptor, null if the target is not in deobfuscation cache.
     */
    @Nullable
    @InternalApi
    public static DexMethodDescriptor getMethodDescFromCache(int i) {
        try {
            ConfigManager cache = ConfigManager.getCache();
            int lastVersion = cache.getIntOrDefault("cache_" + a(i) + "_code", 0);
            if (HostInfo.getVersionCode32() != lastVersion) {
                return null;
            }
            String name = cache.getString("cache_" + a(i) + "_method");
            if (name != null && name.length() > 0) {
                return new DexMethodDescriptor(name);
            }
            return null;
        } catch (Exception e) {
            Log.e(e);
            return null;
        }
    }

    public static String a(int i) {
        return "DexKit.CACHE_" + i;
    }

    /**
     * Get the original class name for a class index.
     *
     * @param i The class index.
     * @return The original class name.
     */
    public static String c(int i) {
        String ret;
        switch (i) {
            case C_DIALOG_UTIL:
                ret = "com/tencent/mobileqq/utils/DialogUtil";
                break;
            case C_FACADE:
            case N_ChatActivityFacade_sendMsgButton:
                ret = "com/tencent/mobileqq/activity/ChatActivityFacade";
                break;
            case C_FLASH_PIC_HELPER:
                ret = "com.tencent.mobileqq.app.FlashPicHelper";
                break;
            case C_BASE_PIC_DL_PROC:
                ret = "com/tencent/mobileqq/transfile/BasePicDownloadProcessor";
                break;
            case C_ItemBuilderFactory:
                ret = "com/tencent/mobileqq/activity/aio/item/ItemBuilderFactory";
                break;
            case C_AIO_UTILS:
                ret = "com.tencent.mobileqq.activity.aio.AIOUtils";
                break;
            case C_ABS_GAL_SCENE:
                ret = "com/tencent/common/galleryactivity/AbstractGalleryScene";
                break;
            case C_FAV_EMO_CONST:
                ret = "com/tencent/mobileqq/emosm/favroaming/FavEmoConstant";
                break;
            case C_MessageRecordFactory:
                ret = "com.tencent.mobileqq.service.message.MessageRecordFactory";
                break;
            case C_CONTACT_UTILS:
                ret = "com/tencent/mobileqq/utils/ContactUtils";
                break;
            case N_VIP_UTILS_getPrivilegeFlags:
                ret = "com/tencent/mobileqq/utils/VipUtils";
                break;
            case C_ARK_APP_ITEM_BUBBLE_BUILDER:
                ret = "com/tencent/mobileqq/activity/aio/item/ArkAppItemBubbleBuilder";
                break;
            case C_PNG_FRAME_UTIL:
                ret = "com.tencent.mobileqq.magicface.drawable.PngFrameUtil";
                break;
            case C_PIC_EMOTICON_INFO:
                ret = "com.tencent.mobileqq.emoticonview.PicEmoticonInfo";
                break;
            case C_SIMPLE_UI_UTIL:
                //dummy, placeholder, just a guess
                ret = "com.tencent.mobileqq.theme.SimpleUIUtil";
                break;
            case C_TROOP_GIFT_UTIL:
                ret = "com/tencent/mobileqq/troop/utils/TroopGiftUtil";
                break;
            case C_TEST_STRUCT_MSG:
                ret = "com/tencent/mobileqq/structmsg/TestStructMsg";
                break;
            case C_QZONE_MSG_NOTIFY:
                ret = "cooperation/qzone/push/MsgNotification";
                break;
            case C_APP_CONSTANTS:
                ret = "com.tencent.mobileqq.app.AppConstants";
                break;
            case C_CustomWidgetUtil:
                ret = "com.tencent.widget.CustomWidgetUtil";
                break;
            case C_MessageCache:
                ret = "com/tencent/mobileqq/service/message/MessageCache";
                break;
            case C_ScreenShotHelper:
                ret = "com.tencent.mobileqq.screendetect.ScreenShotHelper";
                break;
            case C_TimeFormatterUtils:
                ret = "com.tencent.mobileqq.utils.TimeFormatterUtils";
                break;
            case C_TogetherControlHelper:
                //guess
                ret = "com.tencent.mobileqq.aio.helper.TogetherControlHelper";
                break;
            case C_GroupAppActivity:
                ret = "com/tencent/mobileqq/activity/aio/drawer/TroopAppShortcutDrawer";
                break;
            case C_IntimateDrawer:
                ret = "com/tencent/mobileqq/activity/aio/drawer/IntimateInfoChatDrawer";
                break;
            case C_ZipUtils_biz:
                ret = "com/tencent/biz/common/util/ZipUtils";
                break;
            case C_HttpDownloader:
                ret = "com/tencent/mobileqq/transfile/HttpDownloader";
                break;
            case C_MultiMsg_Manager:
                ret = "com/tencent/mobileqq/multimsg/MultiMsgManager";
                break;
            case C_ClockInEntryHelper:
                ret = "com/tencent/mobileqq/activity/aio/helper/ClockInEntryHelper";
                break;
            case C_CaptureUtil:
                ret = "com.tencent.mobileqq.richmedia.capture.util.CaptureUtil";
                break;
            case C_AvatarUtil:
                ret = "com.tencent.mobileqq.avatar.utils.AvatarUtil";
                break;
            case C_SmartDeviceProxyMgr:
                ret = "com.tencent.device.devicemgr.SmartDeviceProxyMgr";
                break;
            case N_LeftSwipeReply_Helper__reply:
                ret = "com/tencent/mobileqq/bubble/LeftSwipeReplyHelper";
                break;
            case C_FaceManager:
                ret = "com.tencent.mobileqq.app.face.FaceManager";
                break;
            case C_AIOPictureView:
                ret = "com.tencent.mobileqq.richmediabrowser.view.AIOPictureView";
                break;
            case C_GalleryBaseScene:
                // guess
                ret = "com.tencent.mobileqq.gallery.view.GalleryBaseScene";
                break;
            case N_BASE_CHAT_PIE__INIT:
            case N_BASE_CHAT_PIE__handleNightMask:
            case N_BASE_CHAT_PIE__updateSession:
            case N_BASE_CHAT_PIE__createMulti:
            case N_BASE_CHAT_PIE__chooseMsg:
            case N_BaseChatPie_mosaic:
                ret = _BaseChatPie().getName();
                break;
            case N_AtPanel__refreshUI:
            case N_AtPanel__showDialogAtView:
                ret = "com/tencent/mobileqq/troop/quickat/ui/AtPanel";
                break;
            case N_FriendChatPie_updateUITitle:
                ret = "com/tencent/mobileqq/activity/aio/core/FriendChatPie";
                break;
            case N_ProfileCardUtil_getCard:
                ret = "com.tencent.mobileqq.util.ProfileCardUtil";
                break;
            case N_VasProfileTemplateController_onCardUpdate:
                ret = "com.tencent.mobileqq.profilecard.vas.VasProfileTemplateController";
                break;
            case N_QQSettingMe_updateProfileBubble:
            case N_QQSettingMe_onResume:
                ret = "com.tencent.mobileqq.activity.QQSettingMe";
                break;
            case N_TroopChatPie_showNewTroopMemberCount:
                ret = "com.tencent.mobileqq.activity.aio.core.TroopChatPie";
                break;
            case N_Conversation_onCreate:
                ret = "com/tencent/mobileqq/activity/home/Conversation";
                break;
            case N_WebSecurityPluginV2_callback:
                ret = "com.tencent.mobileqq.webview.WebSecurityPluginV2$1";
                break;
            case N_TroopAppShortcutBarHelper_resumeAppShorcutBar:
                ret = "com.tencent.mobileqq.activity.aio.helper.TroopAppShortcutBarHelper";
                break;
            case N_FriendsStatusUtil_isChatAtTop:
                ret = "com.tencent.mobileqq.app.utils.FriendsStatusUtil";
                break;
            case N_VipUtils_getUserStatus:
                ret = "com.tencent.mobileqq.utils.VipUtils";
                break;
            case C_GuildHelperProvider:
                ret = "com.tencent.mobileqq.guild.chatpie.GuildHelperProvider";
                break;
            case C_GuildArkHelper:
                ret = "com.tencent.mobileqq.guild.chatpie.helper.GuildArkHelper";
                break;
            case C_ReplyMsgUtils:
                ret = "com.tencent.mobileqq.activity.aio.reply.ReplyMsgUtils";
                break;
            case C_ReplyMsgSender:
                ret = "com.tencent.mobileqq.replymsg.ReplyMsgSender";
                break;
            case N_PhotoListPanel_resetStatus:
                ret = "com.tencent.mobileqq.activity.aio.photo.PhotoListPanel";
                break;
            case C_PopOutEmoticonUtil:
                ret = "com.tencent.mobileqq.popanim.util.PopOutEmoticonUtil";
                break;
            case C_VipStatusManagerImpl:
                ret = "com.tencent.mobileqq.vip.VipStatusManagerImpl";
                break;
            case C_SystemMessageProcessor:
                ret = "com.tencent.mobileqq.app.message.SystemMessageProcessor";
                break;
            case C_OnlinePushPbPushTransMsg:
                ret = "com.tencent.mobileqq.app.handler.receivesuccess.OnlinePushPbPushTransMsg";
                break;
            case N_ContactUtils_getDiscussionMemberShowName:
            case N_ContactUtils_getBuddyName:
                ret = "com.tencent.mobileqq.utils.ContactUtils";
                break;
            default:
                ret = null;
        }
        if (ret != null) {
            return ret.replace("/", ".");
        }
        throw new IndexOutOfBoundsException("No class index for " + i + ",max = " + DEOBF_NUM_C);
    }

    /**
     * Get the keywords of the obfuscated class.
     *
     * @param i The class index.
     * @return The keywords of the class index.
     */
    public static byte[][] b(int i) {
        switch (i) {
            case C_DIALOG_UTIL:
                return new byte[][]{forFiniteString8("android.permission.SEND_SMS", 27)};
            case C_FACADE:
                return new byte[][]{forFiniteString8("reSendEmo", 32)};
            case C_FLASH_PIC_HELPER:
                return new byte[][]{forFiniteString8("FlashPicHelper", 14)};
            case C_BASE_PIC_DL_PROC:
                return new byte[][]{forFiniteString8("BasePicDownl", 44)};
            case C_ItemBuilderFactory:
                return new byte[][]{
                        forFiniteString8("ItemBuilder is: D", 36),
                        forFiniteString8("findItemBuilder: invoked.", 42)
                };
            case C_AIO_UTILS:
                return new byte[][]{forFiniteString8("openAIO by MT", 13)};
            case C_ABS_GAL_SCENE:
                return new byte[][]{forFiniteString8("gallery setColor bl", 22)};
            case C_FAV_EMO_CONST:
                return new byte[][]{
                        forFiniteString8("http://p.qpic.", 17),
                        forFiniteString8("https://p.qpic.", 18)
                };
            case C_MessageRecordFactory:
                return new byte[][]{forFiniteString8("createPicMessage", 44)};
            case C_CONTACT_UTILS:
                return new byte[][]{forFiniteString8(" - WiFi", 7)};
            case C_ARK_APP_ITEM_BUBBLE_BUILDER:
                return new byte[][]{forFiniteString8("debugArkMeta = ", 15)};
            case C_PNG_FRAME_UTIL:
                return new byte[][]{forFiniteString8("func checkRandomPngEx", 42)};
            case C_PIC_EMOTICON_INFO:
                return new byte[][]{forFiniteString8("send emotion + 1:", 32)};
            case C_SIMPLE_UI_UTIL:
                return new byte[][]{forFiniteString8("key_simple_status_s", 21)};
            case C_TROOP_GIFT_UTIL:
                return new byte[][]{
                        forFiniteString8(".troop.send_giftTroopUtils"),
                        forFiniteString8(".troop.send_giftTroopMemberUtil")
                };
            case C_TEST_STRUCT_MSG:
                return new byte[][]{forFiniteString8("TestStructMsg", 13)};
            case C_QZONE_MSG_NOTIFY:
                return new byte[][]{forFiniteString8("use small icon ,exp:", 20)};
            case C_APP_CONSTANTS:
                return new byte[][]{forFiniteString8(".indivAnim/", 11)};
            case C_MessageCache:
                return new byte[][]{forFiniteString8("Q.msg.MessageCache", 18)};
            case C_ScreenShotHelper:
                return new byte[][]{forFiniteString8("onActivityResumeHideFloatView", 29)};
            case C_TimeFormatterUtils:
                return new byte[][]{forFiniteString8("TimeFormatterUtils", 18)};
            case C_TogetherControlHelper:
                return new byte[][]{forFiniteString8("SING together is click", 22)};
            case C_GroupAppActivity:
                return new byte[][]{forFiniteString8("onDrawerStartOpen", 17)};
            case C_IntimateDrawer:
                return new byte[][]{forFiniteString8("onDrawerOpened, needReqIntimateInfo: %s")};
            case C_ZipUtils_biz: {
                return new byte[][]{forFiniteString8(",ZipEntry name: ", 16)};
            }
            case N_BASE_CHAT_PIE__INIT:
                return new byte[][]{
                        forFiniteString8("input set error", 15),
                        forFiniteString8(", mDefautlBtnLeft: ", 19)
                };
            case N_BASE_CHAT_PIE__handleNightMask:
                return new byte[][]{forFiniteString8("#handleNightMask# : inNightMode", 45)};
            case N_BASE_CHAT_PIE__updateSession:
                return new byte[][]{forFiniteString8("AIOTime updateSession end", 25)};
            case N_BASE_CHAT_PIE__createMulti:
                return new byte[][]{forFiniteString8("createMulti")};
            case N_BASE_CHAT_PIE__chooseMsg:
                return new byte[][]{forFiniteString8("set left text from cancel", 25)};
            case C_CustomWidgetUtil:
                // forFiniteString8("NEW", 3), still use hex bytes for high collision reason
                return new byte[][]{new byte[]{0x03, 0x4E, 0x45, 0x57, 0x00}};
            case C_HttpDownloader:
                return new byte[][]{forFiniteString8("[reportHttpsResult] url=", 24)};
            case C_MultiMsg_Manager:
                return new byte[][]{forFiniteString8("[sendMultiMsg]data.length = ", 28)};
            case N_LeftSwipeReply_Helper__reply:
                return new byte[][]{forFiniteString8("0X800A92F", 9)};
            case C_ClockInEntryHelper:
                return new byte[][]{
                        forFiniteString8("isShowTogetherEntry", 31),
                        forFiniteString8("ClockInEntryHelper.helper", 25)};
            case C_CaptureUtil:
                return new byte[][]{forFiniteString8("mediacodec", 31)};
            case C_AvatarUtil:
                return new byte[][]{forFiniteString8("AvatarUtil", 10)};
            case C_FaceManager:
                return new byte[][]{forFiniteString8("FaceManager", 11)};
            case C_SmartDeviceProxyMgr:
                return new byte[][]{
                        forFiniteString8("SmartDeviceProxyMgr create", 26)};
            case N_AtPanel__refreshUI:
                return new byte[][]{forFiniteString8("resultList = null")};
            case N_AtPanel__showDialogAtView:
                return new byte[][]{forFiniteString8("showDialogAtView", 27)};
            case C_AIOPictureView:
                return new byte[][]{forFiniteString8("AIOPictureView", 14),
                        forFiniteString8("AIOGalleryPicView", 17)};
            case N_FriendChatPie_updateUITitle:
                return new byte[][]{
                        forFiniteString8("FriendChatPie updateUI_ti", 65)};
            case N_ProfileCardUtil_getCard:
                return new byte[][]{forFiniteString8("initCard bSuperVipOpen=", 23)};
            case N_VasProfileTemplateController_onCardUpdate:
                return new byte[][]{
                        forFiniteString8("onCardUpdate fail.", 18),
                        forFiniteString8("onCardUpdate: bgId=", 19)
                };
            case N_QQSettingMe_updateProfileBubble:
                return new byte[][]{
                        forFiniteString8("updateProfileBubbleMsgView", 27)};
            case N_VIP_UTILS_getPrivilegeFlags:
                return new byte[][]{forFiniteString8("getPrivilegeFlags Friends is null", 33)};
            case N_TroopChatPie_showNewTroopMemberCount:
                return new byte[][]{forFiniteString8("showNewTroopMemberCount info is null", 36)};
            case N_Conversation_onCreate:
                return new byte[][]{forFiniteString8("Recent_OnCreate")};
            case N_QQSettingMe_onResume:
                return new byte[][]{forFiniteString8("-->onResume!", 12)};
            case N_BaseChatPie_mosaic:
                return new byte[][]{forFiniteString8("enableMosaicEffect", 50)};
            case C_GalleryBaseScene:
                return new byte[][]{forFiniteString8("GalleryBaseScene", 16)};
            case N_WebSecurityPluginV2_callback:
                return new byte[][]{forFiniteString8("check finish jr=", 16)};
            case N_TroopAppShortcutBarHelper_resumeAppShorcutBar:
                return new byte[][]{forFiniteString8("resumeAppShorcutBar")};
            case N_ChatActivityFacade_sendMsgButton:
                return new byte[][]{forFiniteString8(" sendMessage start currenttime:")};
            case N_FriendsStatusUtil_isChatAtTop:
                return new byte[][]{forFiniteString8("isChatAtTop result is: ")};
            case N_VipUtils_getUserStatus:
                return new byte[][]{forFiniteString8("getUserStatus Friends is null")};
            case C_GuildHelperProvider:
                return new byte[][]{forFiniteString8("onFoldStatus beginMoveFoldStatus:")};
            case C_GuildArkHelper:
                return new byte[][]{forFiniteString8("GuildArkHelper")};
            case C_ReplyMsgUtils:
                return new byte[][]{forFiniteString8("generateSourceInfo sender uin exception:")};
            case C_ReplyMsgSender:
                return new byte[][]{forFiniteString8("sendReplyMessage uniseq=0")};
            case N_PhotoListPanel_resetStatus:
                return new byte[][]{forFiniteString8("resetStatus selectSize:")};
            case C_PopOutEmoticonUtil:
                return new byte[][]{forFiniteString8("supportPopOutEmoticon isC2C=")};
            case C_VipStatusManagerImpl:
                return new byte[][]{forFiniteString8("getPrivilegeFlags Friends is null")};
            case C_SystemMessageProcessor:
                return new byte[][]{forFiniteString8("<---handleGetFriendSystemMsgResp : decode pb filtered")};
            case C_OnlinePushPbPushTransMsg:
                return new byte[][]{forFiniteString8("PbPushTransMsg muteGeneralFlag:")};
            case N_ContactUtils_getDiscussionMemberShowName:
                return new byte[][]{forFiniteString8("getDiscussionMemberShowName uin is null")};
            case N_ContactUtils_getBuddyName:
                return new byte[][]{forFiniteString8("getBuddyName()")};
            default:
                throw new IndexOutOfBoundsException("No class index for " + i + ", max = " + DEOBF_NUM_C);
        }
    }

    /**
     * Get the dex index where the target dex class belongs to. Note that this dex index is only used as a hint.
     *
     * @param i the dex index
     * @return the dex indexes where the target class belongs to
     */
    public static int[] d(int i) {
        switch (i) {
            case C_DIALOG_UTIL:
                return new int[]{1, 4, 3};
            case C_FACADE:
            case N_ChatActivityFacade_sendMsgButton:
                return new int[]{2, 6, 3, 7};
            case C_FLASH_PIC_HELPER:
                return new int[]{1, 3};
            case C_BASE_PIC_DL_PROC:
                return new int[]{4, 7, 2};
            case C_ItemBuilderFactory:
                return new int[]{11, 6, 1};
            case C_AIO_UTILS:
                return new int[]{2, 11, 6};
            case C_ABS_GAL_SCENE:
                return new int[]{1};
            case C_FAV_EMO_CONST:
                return new int[]{3, 4, 5};
            case C_MessageRecordFactory:
                return new int[]{15, 4};
            case C_CONTACT_UTILS:
                return new int[]{4};
            case C_ARK_APP_ITEM_BUBBLE_BUILDER:
                return new int[]{2, 11, 6};
            case C_PNG_FRAME_UTIL:
                return new int[]{3, 2};
            case C_PIC_EMOTICON_INFO:
                return new int[]{3, 4};
            case C_SIMPLE_UI_UTIL:
                return new int[]{4, 2};
            case C_TROOP_GIFT_UTIL:
                return new int[]{4, 9, 2};
            case C_TEST_STRUCT_MSG:
                return new int[]{4, 7, 2};
            case C_QZONE_MSG_NOTIFY:
                return new int[]{4, 3};
            case C_APP_CONSTANTS:
                return new int[]{1};
            case C_MessageCache:
                return new int[]{1, 4};
            case C_ScreenShotHelper:
                return new int[]{4, 2};
            case C_TimeFormatterUtils:
                return new int[]{1, 4};
            case C_TogetherControlHelper:
                return new int[]{2, 11, 6};
            case C_GroupAppActivity:
                return new int[]{2, 11, 6};
            case C_IntimateDrawer:
                return new int[]{7, 6};
            case C_ZipUtils_biz:
                return new int[]{1, 9, 5};
            case C_HttpDownloader:
                return new int[]{4, 10, 7, 2};
            case C_MultiMsg_Manager:
                return new int[]{9, 4, 3, 7};
            case N_LeftSwipeReply_Helper__reply:
                return new int[]{8, 3, 2};
            case N_BASE_CHAT_PIE__INIT:
            case N_BASE_CHAT_PIE__handleNightMask:
            case N_BASE_CHAT_PIE__updateSession:
            case N_BASE_CHAT_PIE__chooseMsg:
            case N_BaseChatPie_mosaic:
            case N_BASE_CHAT_PIE__createMulti:
                return new int[]{6, 2, 7, 3};
            case N_AtPanel__refreshUI:
            case N_AtPanel__showDialogAtView:
                return new int[]{12, 10, 4};
            case C_CustomWidgetUtil:
                return new int[]{20, 5, 4, 9};
            case C_ClockInEntryHelper:
                return new int[]{6, 2};
            case C_CaptureUtil:
                return new int[]{10, 4};
            case C_AvatarUtil:
                return new int[]{8};
            case C_FaceManager:
                return new int[]{3};
            case C_SmartDeviceProxyMgr:
                return new int[]{5, 2};
            case C_AIOPictureView:
                return new int[]{10, 4, 2};
            case N_FriendChatPie_updateUITitle:
                return new int[]{4, 6, 2};
            case N_ProfileCardUtil_getCard:
                return new int[]{9, 10, 11, 5, 4, 2};
            case N_VasProfileTemplateController_onCardUpdate:
                return new int[]{7, 6};
            case N_QQSettingMe_updateProfileBubble:
            case N_QQSettingMe_onResume:
                return new int[]{4, 6, 8, 7};
            case N_VIP_UTILS_getPrivilegeFlags:
            case N_VipUtils_getUserStatus:
                return new int[]{16, 11, 12, 14, 4, 2, 3};
            case N_TroopChatPie_showNewTroopMemberCount:
                return new int[]{4, 8, 11, 6};
            case N_Conversation_onCreate:
                return new int[]{5, 7, 8, 6};
            case C_GalleryBaseScene:
                return new int[]{2};
            case N_WebSecurityPluginV2_callback:
                return new int[]{17, 10};
            case N_TroopAppShortcutBarHelper_resumeAppShorcutBar:
                return new int[]{7, 8, 4, 6};
            case N_FriendsStatusUtil_isChatAtTop:
                return new int[]{8};
            case C_GuildHelperProvider:
                return new int[]{9};
            case C_GuildArkHelper:
                return new int[]{9};
            case C_ReplyMsgUtils:
                return new int[]{7};
            case C_ReplyMsgSender:
                return new int[]{13};
            case N_PhotoListPanel_resetStatus:
                return new int[]{7};
            case C_PopOutEmoticonUtil:
                return new int[]{11};
            case C_VipStatusManagerImpl:
                return new int[]{16};
            case C_SystemMessageProcessor:
            case C_OnlinePushPbPushTransMsg:
                return new int[]{8};
            case N_ContactUtils_getDiscussionMemberShowName:
            case N_ContactUtils_getBuddyName:
                return new int[]{16, 15, 14, 4, 10};
            default:
                throw new IndexOutOfBoundsException("No class index for " + i + ", max = " + DEOBF_NUM_C);
        }
    }

    static DexMethodDescriptor verifyTargetMethod(int i, HashSet<DexMethodDescriptor> __methods) {
        switch (i) {
            case C_DIALOG_UTIL:
            case C_FACADE:
            case C_AIO_UTILS:
            case C_CONTACT_UTILS:
            case C_MessageRecordFactory:
            case C_SIMPLE_UI_UTIL:
            case C_TROOP_GIFT_UTIL:
            case C_TEST_STRUCT_MSG:
            case C_TimeFormatterUtils:
            case C_ZipUtils_biz:
            case C_SmartDeviceProxyMgr:
            case C_ReplyMsgUtils:
            case C_PopOutEmoticonUtil:
                a:
                for (DexMethodDescriptor m : __methods) {
                    Class clz = Initiator.load(m.declaringClass);
                    if (Modifier.isAbstract(clz.getModifiers())) {
                        continue;
                    }
                    for (Field f : clz.getDeclaredFields()) {
                        if (!Modifier.isStatic(f.getModifiers())) {
                            continue a;
                        }
                    }
                    return m;
                }
                break;
            case C_FLASH_PIC_HELPER:
            case C_CaptureUtil:
                a:
                for (DexMethodDescriptor m : __methods) {
                    Class clz = Initiator.load(m.declaringClass);
                    if (Modifier.isAbstract(clz.getModifiers())) {
                        continue;
                    }
                    for (Field f : clz.getDeclaredFields()) {
                        if (!Modifier.isStatic(f.getModifiers())) {
                            continue a;
                        }
                    }
                    if (clz.getDeclaredMethods().length > 8) {
                        continue;
                    }
                    return m;
                }
                break;
            case C_BASE_PIC_DL_PROC:
                for (DexMethodDescriptor md : __methods) {
                    Class clz = Initiator.load(md.declaringClass);
                    for (Field f : clz.getDeclaredFields()) {
                        int m = f.getModifiers();
                        if (Modifier.isStatic(m) && Modifier.isFinal(m) && f.getType().equals(Pattern.class)) {
                            return md;
                        }
                    }
                }
                break;
            case C_ItemBuilderFactory: {
                for (DexMethodDescriptor m : __methods) {
                    Class<?> clz = Initiator.load(m.declaringClass);
                    Objects.requireNonNull(clz, "load class '" + m.declaringClass + "' failed");
                    if (clz.getSuperclass() == Object.class && !Modifier.isAbstract(clz.getModifiers())) {
                        return m;
                    }
                }
                break;
            }
            case C_ABS_GAL_SCENE:
                for (DexMethodDescriptor m : __methods) {
                    Class<?> clz = Initiator.load(m.declaringClass);
                    if (!Modifier.isAbstract(clz.getModifiers())) {
                        continue;
                    }
                    for (Field f : clz.getDeclaredFields()) {
                        if (f.getType().equals(View.class)) {
                            return m;
                        }
                    }
                }
                break;
            case C_GalleryBaseScene:
                for (DexMethodDescriptor m : __methods) {
                    Class<?> clz = Initiator.load(m.declaringClass);
                    for (Field f : clz.getDeclaredFields()) {
                        if (f.getType().equals(View.class)) {
                            return m;
                        }
                    }
                }
                break;
            case C_FAV_EMO_CONST:
                a:
                for (DexMethodDescriptor m : __methods) {
                    Class clz = Initiator.load(m.declaringClass);
                    if (Modifier.isAbstract(clz.getModifiers())) {
                        continue;
                    }
                    for (Field f : clz.getDeclaredFields()) {
                        if (!Modifier.isStatic(f.getModifiers())) {
                            continue a;
                        }
                    }
                    if (clz.getDeclaredMethods().length > 3) {
                        continue;
                    }
                    return m;
                }
                break;
            case C_ARK_APP_ITEM_BUBBLE_BUILDER:
                for (DexMethodDescriptor m : __methods) {
                    Class clz = Initiator.load(m.declaringClass);
                    if (Modifier.isAbstract(clz.getModifiers())) {
                        continue;
                    }
                    Class sp = clz.getSuperclass();
                    if (Object.class.equals(sp)) {
                        continue;
                    }
                    if (!Modifier.isAbstract(sp.getModifiers())) {
                        continue;
                    }
                    if (sp.getName().contains("Builder")) {
                        return m;
                    }
                    return m;
                }
                break;
            case C_PNG_FRAME_UTIL:
                for (DexMethodDescriptor md : __methods) {
                    Class clz = Initiator.load(md.declaringClass);
                    for (Method m : clz.getMethods()) {
                        if (m.getName().equals("b")) {
                            continue;
                        }
                        if (!m.getReturnType().equals(int.class)) {
                            continue;
                        }
                        if (!Modifier.isStatic(m.getModifiers())) {
                            continue;
                        }
                        Class[] argt = m.getParameterTypes();
                        if (argt.length == 1 && int.class.equals(argt[0])) {
                            return md;
                        }
                    }
                    return md;
                }
                break;
            case C_PIC_EMOTICON_INFO:
                for (DexMethodDescriptor m : __methods) {
                    Class clz = Initiator.load(m.declaringClass);
                    if (Modifier.isAbstract(clz.getModifiers())) {
                        continue;
                    }
                    Class s = clz.getSuperclass();
                    if (Object.class.equals(s)) {
                        continue;
                    }
                    s = s.getSuperclass();
                    if (Object.class.equals(s)) {
                        continue;
                    }
                    s = s.getSuperclass();
                    if (Object.class.equals(s)) {
                        return m;
                    }
                }
                break;
            case C_QZONE_MSG_NOTIFY:
                for (DexMethodDescriptor md : __methods) {
                    Class clz = Initiator.load(md.declaringClass);
                    if (Modifier.isAbstract(clz.getModifiers())) {
                        continue;
                    }
                    Class s = clz.getSuperclass();
                    if (!Object.class.equals(s)) {
                        continue;
                    }
                    for (Method m : clz.getDeclaredMethods()) {
                        if (!m.getReturnType().equals(void.class)) {
                            continue;
                        }
                        Class<?>[] argt = m.getParameterTypes();
                        if (argt.length > 7 && argt[0].equals(_QQAppInterface())) {
                            return md;
                        }
                    }
                }
                break;
            case C_APP_CONSTANTS:
                for (DexMethodDescriptor m : __methods) {
                    Class clz = Initiator.load(m.declaringClass);
                    if (!Modifier.isInterface(clz.getModifiers())) {
                        continue;
                    }
                    if (clz.getDeclaredFields().length < 50) {
                        continue;
                    }
                    return m;
                }
                break;
            case N_BASE_CHAT_PIE__INIT:
            case N_BASE_CHAT_PIE__handleNightMask:
            case N_BASE_CHAT_PIE__updateSession:
            case N_BaseChatPie_mosaic:
            case N_BASE_CHAT_PIE__chooseMsg:
                for (DexMethodDescriptor m : __methods) {
                    if (m.declaringClass.replace('/', '.').contains(_BaseChatPie().getName())) {
                        return m;
                    }
                }
                break;
            case C_TogetherControlHelper:
                return (DexMethodDescriptor) __methods.toArray()[0];
            case N_LeftSwipeReply_Helper__reply:
            case N_FriendChatPie_updateUITitle:
            case N_VipUtils_getUserStatus:
            case C_VipStatusManagerImpl:
            case C_SystemMessageProcessor:
                // NOTICE: this must only has one result
                if (__methods.size() == 1) {
                    return (DexMethodDescriptor) __methods.toArray()[0];
                } else {
                    // return null to avoid unexpected behavior
                    return null;
                }
            case N_QQSettingMe_updateProfileBubble:
                for (DexMethodDescriptor m : __methods) {
                    Method method;
                    try {
                        method = m.getMethodInstance(Initiator.getHostClassLoader());
                    } catch (Exception e) {
                        continue;
                    }
                    if (!method.getReturnType().equals(void.class)) {
                        continue;
                    }
                    if (m.declaringClass.contains("QQSettingMe")) {
                        return m;
                    }
                }
                break;
            case N_BASE_CHAT_PIE__createMulti:
                for (DexMethodDescriptor m : __methods) {
                    Method method;
                    try {
                        method = m.getMethodInstance(Initiator.getHostClassLoader());
                    } catch (Exception e) {
                        continue;
                    }
                    String name = m.declaringClass.replace('/', '.');
                    if (name.contains("com.tencent.mobileqq.activity.aio.helper.AIOMultiActionHelper")
                            || name.contains(_BaseChatPie().getName())) {
                        if (method.getParameterTypes()[0].equals(_ChatMessage())) {
                            return m;
                        }
                    }
                    Class<?> klass = Initiator.load(m.declaringClass);
                    if (klass != null && View.OnClickListener.class.isAssignableFrom(klass)
                            && klass.getName().contains("com.tencent.mobileqq.activity.aio.helper")) {
                        return m;
                    }
                }
                break;
            case N_ProfileCardUtil_getCard:
                for (DexMethodDescriptor m : __methods) {
                    Method method;
                    try {
                        method = m.getMethodInstance(Initiator.getHostClassLoader());
                    } catch (Exception e) {
                        continue;
                    }
                    if ("Card".equals(method.getReturnType().getSimpleName())) {
                        return m;
                    }
                }
                break;
            case N_VasProfileTemplateController_onCardUpdate:
                for (DexMethodDescriptor m : __methods) {
                    Method method;
                    try {
                        method = m.getMethodInstance(Initiator.getHostClassLoader());
                        Log.d("m=" + method);
                    } catch (Exception e) {
                        continue;
                    }
                    if ("onCardUpdate".equals(method.getName())) {
                        return m;
                    }
                    Class<?> kVasProfileTemplateController = Initiator.load("com.tencent.mobileqq.profilecard.vas.VasProfileTemplateController");
                    if (kVasProfileTemplateController != null && method.getDeclaringClass().isAssignableFrom(kVasProfileTemplateController)) {
                        return m;
                    }
                    if (method.getDeclaringClass().isAssignableFrom(Initiator._FriendProfileCardActivity())) {
                        return m;
                    }
                }
                break;
            case C_CustomWidgetUtil:
                a:
                for (DexMethodDescriptor m : __methods) {
                    if (m.declaringClass.contains("/") && !m.declaringClass.contains("com/tencent/widget")) {
                        continue;
                    }
                    Class<?> clz = Initiator.load(m.declaringClass);
                    if (clz.isEnum()) {
                        continue;
                    }
                    if (Modifier.isAbstract(clz.getModifiers())) {
                        continue;
                    }
                    if (Object.class != clz.getSuperclass()) {
                        continue;
                    }
                    for (Field f : clz.getDeclaredFields()) {
                        if (!Modifier.isStatic(f.getModifiers())) {
                            continue a;
                        }
                    }
                    return m;
                }
                break;
            case C_MessageCache:
                for (DexMethodDescriptor m : __methods) {
                    if ("<clinit>".equals(m.name)) {
                        return m;
                    }
                }
                break;
            case C_ScreenShotHelper:
                for (DexMethodDescriptor m : __methods) {
                    Class clz = Initiator.load(m.declaringClass);
                    if (clz.isEnum()) {
                        continue;
                    }
                    if (Modifier.isAbstract(clz.getModifiers())) {
                        continue;
                    }
                    if (Object.class != clz.getSuperclass()) {
                        continue;
                    }
                    return m;
                }
                break;
            case C_ClockInEntryHelper:
            case C_FaceManager:
            case C_AvatarUtil:
            case N_AtPanel__showDialogAtView:
                for (DexMethodDescriptor m : __methods) {
                    Class clz = Initiator.load(m.declaringClass);
                    if (clz.isEnum()) {
                        continue;
                    }
                    if (Modifier.isAbstract(clz.getModifiers())) {
                        continue;
                    }
                    if (Object.class != clz.getSuperclass()) {
                        continue;
                    }
                    return m;
                }
                break;
            case N_AtPanel__refreshUI:
                for (DexMethodDescriptor m : __methods) {
                    Class<?> clz = Initiator.load(m.declaringClass);
                    if (clz.isEnum()) {
                        continue;
                    }
                    if (Modifier.isAbstract(clz.getModifiers())) {
                        continue;
                    }
                    if (Object.class != clz.getSuperclass()) {
                        continue;
                    }
                    try {
                        if (void.class != m.getMethodInstance(Initiator.getHostClassLoader()).getReturnType()) {
                            continue;
                        }
                    } catch (NoSuchMethodException e) {
                        Log.e(e);
                    }
                    return m;
                }
                break;
            case C_GroupAppActivity:
                //has superclass
                for (DexMethodDescriptor m : __methods) {
                    Class clz = Initiator.load(m.declaringClass);
                    if (clz.isEnum()) {
                        continue;
                    }
                    if (Modifier.isAbstract(clz.getModifiers())) {
                        continue;
                    }
                    if (clz.getSuperclass() == Object.class) {
                        continue;
                    }
                    if (Arrays.stream(clz.getDeclaredFields()).noneMatch(f -> f.getType().getName().endsWith("TroopAppShortcutContainer"))) {
                        continue;
                    }
                    return m;
                }
                break;
            case C_IntimateDrawer:
            case C_HttpDownloader:
            case C_MultiMsg_Manager:
            case C_AIOPictureView:
                //has superclass
                for (DexMethodDescriptor m : __methods) {
                    Class clz = Initiator.load(m.declaringClass);
                    if (clz.isEnum()) {
                        continue;
                    }
                    if (Modifier.isAbstract(clz.getModifiers())) {
                        continue;
                    }
                    if (clz.getSuperclass() == Object.class) {
                        continue;
                    }
                    return m;
                }
                break;
            case N_VIP_UTILS_getPrivilegeFlags:
                for (DexMethodDescriptor m : __methods) {
                    Method method;
                    try {
                        method = m.getMethodInstance(Initiator.getHostClassLoader());
                    } catch (Exception e) {
                        continue;
                    }
                    if (method.getReturnType().equals(int.class)) {
                        if (method.getName().equals("getPrivilegeFlags")) {
                            return m;
                        }
                        Class<?>[] argt = method.getParameterTypes();
                        if (argt.length == 1 && argt[0].equals(String.class)) {
                            return m;
                        }
                        if (argt.length == 2 && argt[0].equals(load("mqq/app/AppRuntime")) && argt[1].equals(String.class)) {
                            return m;
                        }
                    }
                }
                break;
            case N_TroopChatPie_showNewTroopMemberCount:
                for (DexMethodDescriptor m : __methods) {
                    Method method;
                    try {
                        method = m.getMethodInstance(Initiator.getHostClassLoader());
                    } catch (Exception e) {
                        continue;
                    }
                    if (method.getDeclaringClass() == _TroopChatPie()) {
                        Class<?>[] argt = method.getParameterTypes();
                        if (argt.length == 0) {
                            return m;
                        }
                    }
                }
                break;
            case N_Conversation_onCreate:
                for (DexMethodDescriptor m : __methods) {
                    if (m.declaringClass.contains("Conversation")) {
                        return m;
                    }
                }
                break;
            case N_QQSettingMe_onResume:
                for (DexMethodDescriptor m : __methods) {
                    if (m.declaringClass.contains("QQSettingMe")) {
                        return m;
                    }
                }
                break;
            case N_WebSecurityPluginV2_callback:
                for (DexMethodDescriptor m : __methods) {
                    Method method;
                    try {
                        method = m.getMethodInstance(Initiator.getHostClassLoader());
                    } catch (Exception e) {
                        continue;
                    }
                    Class<?>[] argt = method.getParameterTypes();
                    if (argt.length == 1 && argt[0] == Bundle.class) {
                        return m;
                    }
                }
                break;
            case N_TroopAppShortcutBarHelper_resumeAppShorcutBar: {
                // only 1 expected
                if (__methods.size() == 1) {
                    DexMethodDescriptor m = __methods.iterator().next();
                    if (m.declaringClass.contains("TroopAppShortcutBarHelper")
                            || m.declaringClass.contains("ShortcutBarAIOHelper")
                            || m.declaringClass.contains("/aio/helper/")) {
                        return m;
                    }
                }
                return null;
            }
            case N_ChatActivityFacade_sendMsgButton:
                for (DexMethodDescriptor m : __methods) {
                    if (m.declaringClass.contains("ChatActivityFacade")) {
                        Method method;
                        try {
                            method = m.getMethodInstance(Initiator.getHostClassLoader());
                        } catch (Exception e) {
                            continue;
                        }
                        Class<?>[] argt = method.getParameterTypes();
                        if (argt.length == 6) {
                            return m;
                        }
                    }
                }
                break;
            case N_FriendsStatusUtil_isChatAtTop:
                for (DexMethodDescriptor m : __methods) {
                    if (m.declaringClass.contains("FriendsStatusUtil")) {
                        return m;
                    }
                }
                break;
            case C_GuildHelperProvider: {
                // only 1 expected
                if (__methods.size() == 1) {
                    DexMethodDescriptor m = __methods.iterator().next();
                    if (m.declaringClass.contains("com/tencent/mobileqq/guild/chatpie")) {
                        return m;
                    }
                }
                return null;
            }
            case C_GuildArkHelper: {
                // only 1 expected
                if (__methods.size() == 1) {
                    DexMethodDescriptor m = __methods.iterator().next();
                    if ("getTag".equals(m.name)) {
                        return m;
                    }
                }
                return null;
            }
            case C_ReplyMsgSender: {
                // only 1 expected
                if (__methods.size() == 1) {
                    DexMethodDescriptor m = __methods.iterator().next();
                    if (m.declaringClass.contains("com/tencent/mobileqq/replymsg/")) {
                        return m;
                    }
                }
                return null;
            }
            case C_OnlinePushPbPushTransMsg: {
                // only 1 expected
                if (__methods.size() == 1) {
                    DexMethodDescriptor m = __methods.iterator().next();
                    if (m.declaringClass.contains("/receivesuccess/")) {
                        return m;
                    }
                }
                return null;
            }
            case N_PhotoListPanel_resetStatus: {
                // only 1 expected
                if (__methods.size() == 1) {
                    DexMethodDescriptor m = __methods.iterator().next();
                    if ("(Z)V".equals(m.signature)) {
                        return m;
                    }
                }
                return null;
            }
            case N_ContactUtils_getDiscussionMemberShowName: {
                // only 1 expected
                if (__methods.size() == 1) {
                    DexMethodDescriptor m = __methods.iterator().next();
                    try {
                        Method method = m.getMethodInstance(Initiator.getHostClassLoader());
                        if (method.getReturnType() == String.class && Modifier.isStatic(method.getModifiers())
                                && method.getParameterTypes().length == 3) {
                            return m;
                        }
                    } catch (NoSuchMethodException e) {
                        Log.e("N_ContactUtils_getDiscussionMemberShowName: getMethodInstance error", e);
                    }
                }
                return null;
            }
            case N_ContactUtils_getBuddyName: {
                // 2 expected, for same method
                DexMethodDescriptor m = __methods.iterator().next();
                try {
                    Method method = m.getMethodInstance(Initiator.getHostClassLoader());
                    if (method.getReturnType() == String.class && Modifier.isStatic(method.getModifiers())
                            && method.getParameterTypes().length == 3) {
                        return m;
                    }
                } catch (NoSuchMethodException e) {
                    Log.e("N_ContactUtils_getBuddyName: getMethodInstance error", e);
                }
                return null;
            }
            default: {
                //noinspection SwitchStatementWithouthDefaultBranch
            }
        }
        return null;
    }

    @Nullable
    public static byte[] getClassDeclaringDex(String klass, @Nullable int[] qf) {
        ClassLoader loader = Initiator.getHostClassLoader();
        int record = 0;
        if (qf != null) {
            for (int dexi : qf) {
                record |= 1 << dexi;
                try {
                    String name;
                    byte[] buf = new byte[4096];
                    byte[] content;
                    if (dexi == 1) {
                        name = "classes.dex";
                    } else {
                        name = "classes" + dexi + ".dex";
                    }
                    HashSet<URL> urls = new HashSet<>(3);
                    try {
                        Enumeration<URL> eu;
                        eu = (Enumeration<URL>) Reflex.invokeVirtual(loader, "findResources", name, String.class);
                        if (eu != null) {
                            while (eu.hasMoreElements()) {
                                URL url = eu.nextElement();
                                if (url.toString().contains(HostInfo.getPackageName())) {
                                    urls.add(url);
                                }
                            }
                        }
                    } catch (Throwable e) {
                        Log.e(e);
                    }
                    if (!loader.getClass().equals(PathClassLoader.class) && !loader.getClass().equals(DexClassLoader.class)
                            && loader.getParent() != null) {
                        try {
                            Enumeration<URL> eu;
                            eu = (Enumeration<URL>) Reflex.invokeVirtual(loader.getParent(), "findResources", name, String.class);
                            if (eu != null) {
                                while (eu.hasMoreElements()) {
                                    URL url = eu.nextElement();
                                    if (url.toString().contains(HostInfo.getPackageName())) {
                                        urls.add(url);
                                    }
                                }
                            }
                        } catch (Throwable e) {
                            Log.e(e);
                        }
                    }
                    if (urls.size() == 0) {
                        throw new FileNotFoundException(name);
                    }
                    InputStream in;
                    try {
                        for (URL url : urls) {
                            in = url.openStream();
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            int ii;
                            while ((ii = in.read(buf)) != -1) {
                                baos.write(buf, 0, ii);
                            }
                            in.close();
                            content = baos.toByteArray();
                            if (DexFlow.hasClassInDex(content, klass)) {
                                return content;
                            }
                        }
                    } catch (IOException e) {
                        Log.e(e);
                        return null;
                    }
                } catch (FileNotFoundException ignored) {
                }
            }
        }
        int dexi = 1;
        while (true) {
            if ((record & (1 << dexi)) != 0) {
                dexi++;
                continue;
            }
            try {
                String name;
                byte[] buf = new byte[4096];
                byte[] content;
                if (dexi == 1) {
                    name = "classes.dex";
                } else {
                    name = "classes" + dexi + ".dex";
                }
                HashSet<URL> urls = new HashSet<>(3);
                try {
                    Enumeration<URL> eu;
                    eu = (Enumeration<URL>) Reflex.invokeVirtual(loader, "findResources", name, String.class);
                    if (eu != null) {
                        while (eu.hasMoreElements()) {
                            URL url = eu.nextElement();
                            if (url.toString().contains(HostInfo.getPackageName())) {
                                urls.add(url);
                            }
                        }
                    }
                } catch (Throwable e) {
                    Log.e(e);
                }
                if (!loader.getClass().equals(PathClassLoader.class) && !loader.getClass().equals(DexClassLoader.class) && loader.getParent() != null) {
                    try {
                        Enumeration<URL> eu;
                        eu = (Enumeration<URL>) Reflex.invokeVirtual(loader.getParent(), "findResources", name, String.class);
                        if (eu != null) {
                            while (eu.hasMoreElements()) {
                                URL url = eu.nextElement();
                                if (url.toString().contains(HostInfo.getPackageName())) {
                                    urls.add(url);
                                }
                            }
                        }
                    } catch (Throwable e) {
                        Log.e(e);
                    }
                }
                if (urls.size() == 0) {
                    throw new FileNotFoundException(name);
                }
                InputStream in;
                try {
                    for (URL url : urls) {
                        in = url.openStream();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        int ii;
                        while ((ii = in.read(buf)) != -1) {
                            baos.write(buf, 0, ii);
                        }
                        in.close();
                        content = baos.toByteArray();
                        if (DexFlow.hasClassInDex(content, klass)) {
                            return content;
                        }
                    }
                } catch (IOException e) {
                    Log.e(e);
                    return null;
                }
            } catch (FileNotFoundException ignored) {
                return null;
            }
            dexi++;
        }
    }

    @NonNull
    public static byte[] forFiniteString8(@NonNull String str, int len) {
        // dex uses utf-8 encoded utf-16, s.t. even if the str is complete, str.length != str.bytes.length
        if (len < 0 || len > 127 || str.length() > 127) {
            throw new IllegalArgumentException("String too long");
        }
        byte[] u8 = str.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[u8.length + 1];
        result[0] = (byte) len;
        System.arraycopy(u8, 0, result, 1, u8.length);
        return result;
    }

    @NonNull
    public static byte[] forFiniteString8(@NonNull String str) {
        // dex uses utf-8 encoded utf-16
        int len = str.length();
        if (len > 127) {
            throw new IllegalArgumentException("String too long");
        }
        byte[] u8 = str.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[u8.length + 1];
        result[0] = (byte) len;
        System.arraycopy(u8, 0, result, 1, u8.length);
        return result;
    }
}
