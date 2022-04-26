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
package io.github.qauxv.util;

import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.HashMap;
import mqq.app.AppRuntime;

public class Initiator {

    private static ClassLoader sHostClassLoader;
    private static ClassLoader sPluginParentClassLoader;
    private static Class<?> kQQAppInterface = null;
    private static final HashMap<String, Class<?>> sClassCache = new HashMap<>(16);

    private Initiator() {
        throw new AssertionError("No instance for you!");
    }

    public static void init(ClassLoader classLoader) {
        sHostClassLoader = classLoader;
        sPluginParentClassLoader = Initiator.class.getClassLoader();
    }

    public static ClassLoader getPluginClassLoader() {
        return Initiator.class.getClassLoader();
    }

    public static ClassLoader getHostClassLoader() {
        return sHostClassLoader;
    }

    /**
     * Load a class, if the class is not found, null will be returned.
     *
     * @param className The class name.
     * @return The class, or null if not found.
     */
    @Nullable
    public static Class<?> load(String className) {
        if (sPluginParentClassLoader == null || className == null || className.isEmpty()) {
            return null;
        }
        className = className.replace('/', '.');
        if (className.endsWith(";")) {
            if (className.charAt(0) == 'L') {
                className = className.substring(1, className.length() - 1);
            } else {
                className = className.substring(0, className.length() - 1);
            }
        }
        try {
            return sPluginParentClassLoader.loadClass(className);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Load a class, if the class is not found, a ClassNotFoundException will be thrown.
     *
     * @param className The class name.
     * @return The class.
     * @throws ClassNotFoundException If the class is not found.
     */
    @NonNull
    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        Class<?> ret = load(className);
        if (ret == null) {
            throw new ClassNotFoundException(className);
        }
        return ret;
    }

    /**
     * Load a class, if the class is not found, an unsafe ClassNotFoundException will be thrown.
     *
     * @param className The class name.
     * @return The class.
     */
    @NonNull
    public static Class<?> requireClass(@NonNull String className) {
        try {
            return loadClass(className);
        } catch (ClassNotFoundException e) {
            IoUtils.unsafeThrow(e);
            throw new AssertionError("Unreachable code");
        }
    }

    public static Class<?> _QbossADImmersionBannerManager() {
        return findClassWithSynthetics("cooperation.vip.qqbanner.QbossADImmersionBannerManager",
                "cooperation.vip.qqbanner.manager.VasADImmersionBannerManager",
                "cooperation.vip.qqbanner.manager.VasADImmersionBannerManager", 1, 2);
    }

    public static Class<?> _ConversationTitleBtnCtrl() {
        return findClassWithSynthetics("com.tencent.mobileqq.activity.ConversationTitleBtnCtrl", 1, 2, 4, 5, 6);
    }

    public static Class<?> _ConfigHandler() {
        return findClassWithSynthetics("com.tencent.mobileqq.app.ConfigHandler", 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
    }

    public static Class<?> _GdtMvViewController() {
        return findClassWithSynthetics("com.tencent.gdtad.basics.motivevideo.GdtMvViewController",
                "com.tencent.gdtad.api.motivevideo.GdtMvViewController", 6, 8);
    }

    public static Class<?> _GivingHeartItemBuilder() {
        return findClassWithSynthetics("com.tencent.mobileqq.activity.aio.item.GivingHeartItemBuilder", 10, 5);
    }

    public static Class<?> _ColorNickManager() {
        return findClassWithSynthetics("com.tencent.mobileqq.vas.ColorNickManager", 2);
    }

    public static Class<?> _TroopEnterEffectController() {
        return findClassWithSynthetics("com.tencent.mobileqq.troop.enterEffect.TroopEnterEffectController",
                "com.tencent.mobileqq.troop.enterEffect.TroopEnterEffect.Controller", 3);
    }

    public static Class<?> _VoteHelper() {
        return findClassWithSynthetics("com/tencent/mobileqq/profile/vote/VoteHelper", 1, 3, 4);
    }

    public static Class<?> _PicItemBuilder() {
        return findClassWithSynthetics("com.tencent.mobileqq.activity.aio.item.PicItemBuilder", 7, 6, 8, 3);
    }

    public static Class<?> _MixedMsgItemBuilder() {
        return findClassWithSynthetics("com.tencent.mobileqq.activity.aio.item.MixedMsgItemBuilder", 2);
    }

    public static Class<?> _MarketFaceItemBuilder() {
        return findClassWithSynthetics("com.tencent.mobileqq.activity.aio.item.MarketFaceItemBuilder", 8, 10, 14, 15);
    }

    public static Class<?> _TroopGagMgr() {
        return findClassWithSynthetics("com.tencent.mobileqq.troop.utils.TroopGagMgr", 1, 2);
    }

    public static Class<?> _TextItemBuilder() {
        return findClassWithSynthetics("com/tencent/mobileqq/activity/aio/item/TextItemBuilder", 10, 7, 6, 3, 8);
    }

    public static Class<?> _TroopFileUploadMgr() {
        return findClassWithSynthetics("com.tencent.mobileqq.troop.filemanager.upload.TroopFileUploadMgr",
                1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    public static Class<?> _TroopPicEffectsController() {
        return findClassWithSynthetics("com/tencent/mobileqq/trooppiceffects/TroopPicEffectsController", 2);
    }

    public static Class<?> _BannerManager() {
        return findClassWithSynthetics("com.tencent.mobileqq.activity.recent.BannerManager", 38, 39, 40, 41, 42);
    }

    public static Class<?> _PttItemBuilder() {
        return findClassWithSynthetics("com/tencent/mobileqq/activity/aio/item/PttItemBuilder", 2);
    }

    public static Class<?> _ReplyItemBuilder(){return load("com.tencent.mobileqq.activity.aio.item.ReplyTextItemBuilder");}

    public static Class<?> _TroopGiftAnimationController() {
        return findClassWithSynthetics("com.tencent.mobileqq.troopgift.TroopGiftAnimationController", 1);
    }

    public static Class<?> _FavEmoRoamingHandler() {
        return findClassWithSynthetics("com/tencent/mobileqq/app/FavEmoRoamingHandler", 1);
    }

    public static Class<?> _StartupDirector() {
        return findClassWithSynthetics("com/tencent/mobileqq/startup/director/StartupDirector", 1);
    }

    public static Class<?> _BaseQQMessageFacade() {
        return load("com/tencent/imcore/message/BaseQQMessageFacade");
    }

    public static Class<?> _QQMessageFacade() {
        return findClassWithSynthetics("com/tencent/mobileqq/app/message/QQMessageFacade",
                "com/tencent/imcore/message/QQMessageFacade");
    }

    @SuppressWarnings("unchecked")
    public static <T extends Parcelable> Class<T> _SessionInfo() {
        return (Class<T>) load("com/tencent/mobileqq/activity/aio/SessionInfo");
    }

    public static Class<?> _BaseChatPie() {
        return findClassWithSynthetics("com/tencent/mobileqq/activity/aio/core/BaseChatPie", "com.tencent.mobileqq.activity.BaseChatPie");
    }

    public static Class<?> _TroopMemberInfo() {
        return findClassWithSynthetics("com.tencent.mobileqq.data.troop.TroopMemberInfo", "com.tencent.mobileqq.data.TroopMemberInfo");
    }

    public static Class<?> _TroopInfo() {
        return findClassWithSynthetics("com.tencent.mobileqq.data.troop.TroopInfo", "com.tencent.mobileqq.data.TroopInfo");
    }

    public static Class<?> _Conversation() {
        return findClassWithSynthetics("com/tencent/mobileqq/activity/home/Conversation", "com/tencent/mobileqq/activity/Conversation", 5);
    }

    public static Class<?> _ChatMessage() {
        return load("com.tencent.mobileqq.data.ChatMessage");
    }

    public static Class<?> _MessageRecord() {
        return load("com/tencent/mobileqq/data/MessageRecord");
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends AppRuntime> _QQAppInterface() {
        if (kQQAppInterface == null) {
            kQQAppInterface = load("com/tencent/mobileqq/app/QQAppInterface");
            if (kQQAppInterface == null) {
                Class<?> ref = load("com/tencent/mobileqq/app/QQAppInterface$1");
                if (ref != null) {
                    try {
                        kQQAppInterface = ref.getDeclaredField("this$0").getType();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return (Class<? extends AppRuntime>) kQQAppInterface;
    }

    public static Class<?> _BaseQQAppInterface() {
        return load("com.tencent.common.app.business.BaseQQAppInterface");
    }

    public static Class<?> _BaseMessageManager() {
        return findClassWithSynthetics("com/tencent/imcore/message/BaseMessageManager",
                "com/tencent/mobileqq/app/message/BaseMessageManager", 1, 2);
    }

    public static Class<?> _EmoAddedAuthCallback() {
        return findClassWithSynthetics("com/tencent/mobileqq/emosm/favroaming/EmoAddedAuthCallback", 2, 1);
    }

    public static Class<?> _C2CMessageProcessor() {
        return findClassWithSynthetics("com/tencent/mobileqq/app/message/C2CMessageProcessor",
                "com/tencent/imcore/message/C2CMessageProcessor", 4, 6, 1, 5, 7);
    }

    public static Class<?> _C2CMessageManager() {
        return findClassWithSynthetics("com/tencent/imcore/message/C2CMessageManager",
                "com/tencent/mobileqq/app/message/C2CMessageManager", 1, 2);
    }

    public static Class<?> _AllInOne() {
        return findClassWithSynthetics("com/tencent/mobileqq/activity/ProfileActivity$AllInOne",
                "com.tencent.mobileqq.profilecard.data.AllInOne");
    }

    public static Class<?> _FriendProfileCardActivity() {
        return findClassWithSynthetics("com/tencent/mobileqq/activity/FriendProfileCardActivity",
                "com.tencent.mobileqq.profilecard.activity.FriendProfileCardActivity");
    }

    public static Class<?> _ThemeUtil() {
        return findClassWithSynthetics("com/tencent/mobileqq/theme/ThemeUtil", "com.tencent.mobileqq.vas.theme.api.ThemeUtil");
    }

    public static Class<?> _TroopMemberLevelView() {
        return findClassWithSynthetics("com.tencent.mobileqq.troop.troopMemberLevel.TroopMemberNewLevelView",
                "com.tencent.mobileqq.troop.widget.troopmemberlevel.TroopMemberNewLevelView");
    }

    public static Class<?> _TroopChatPie() {
        return findClassWithSynthetics("com.tencent.mobileqq.activity.aio.core.TroopChatPie", "com.tencent.mobileqq.activity.aio.rebuild.TroopChatPie");
    }

    public static Class<?> _ChatActivityFacade() {
        return load("com/tencent/mobileqq/activity/ChatActivityFacade");
    }

    public static Class<?> _BaseSessionInfo() {
        return findClassWithSynthetics("com/tencent/mobileqq/activity/aio/BaseSessionInfo", "com/tencent/mobileqq/activity/aio/SessionInfo");
    }

    public static Class<?> _Emoticon() {
        return load("com.tencent.mobileqq.data.Emoticon");
    }

    public static Class<?> _StickerInfo() {
        return findClassWithSynthetics("com.tencent.mobileqq.emoticon.StickerInfo", "com.tencent.mobileqq.emoticon.EmojiStickerManager$StickerInfo");
    }

    public static Class<?> _TogetherControlHelper() {
        return findClassWithSynthetics("com.tencent.mobileqq.aio.helper.TogetherControlHelper",
                "com.tencent.mobileqq.activity.aio.helper.TogetherControlHelper");
    }

    public static Class<?> _ClockInEntryHelper() {
        return load("com/tencent/mobileqq/activity/aio/helper/ClockInEntryHelper");
    }

    @Nullable
    private static Class<?> findClassWithSyntheticsImpl(@NonNull String className, int... index) {
        Class<?> clazz = load(className);
        if (clazz != null) {
            return clazz;
        }
        if (index != null && index.length > 0) {
            for (int i : index) {
                Class<?> cref = load(className + "$" + i);
                if (cref != null) {
                    try {
                        return cref.getDeclaredField("this$0").getType();
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private static Class<?> findClassWithSynthetics(@NonNull String className, int... index) {
        Class<?> cache = sClassCache.get(className);
        if (cache != null) {
            return cache;
        }
        Class<?> clazz = load(className);
        if (clazz != null) {
            sClassCache.put(className, clazz);
            return clazz;
        }
        clazz = findClassWithSyntheticsImpl(className, index);
        if (clazz != null) {
            sClassCache.put(className, clazz);
            return clazz;
        }
        Log.e("Initiator/E class " + className + " not found");
        return null;
    }

    @Nullable
    public static Class<?> findClassWithSynthetics(@NonNull String className1, @NonNull String className2) {
        Class<?> clazz = load(className1);
        if (clazz != null) {
            return clazz;
        }
        return load(className2);
    }

    @Nullable
    public static Class<?> findClassWithSynthetics(@NonNull String className1, @NonNull String className2, int... index) {
        String cacheKey = className1;
        Class<?> cache = sClassCache.get(cacheKey);
        if (cache != null) {
            return cache;
        }
        Class<?> clazz = findClassWithSyntheticsImpl(className1, index);
        if (clazz != null) {
            sClassCache.put(cacheKey, clazz);
            return clazz;
        }
        clazz = findClassWithSyntheticsImpl(className2, index);
        if (clazz != null) {
            sClassCache.put(cacheKey, clazz);
            return clazz;
        }
        Log.e("Initiator/E class " + className1 + " not found");
        return null;
    }

    @Nullable
    public static Class<?> findClassWithSynthetics(@NonNull String className1, @NonNull String className2,
                                                   @NonNull String className3, int... index) {
        String cacheKey = className1;
        Class<?> cache = sClassCache.get(cacheKey);
        if (cache != null) {
            return cache;
        }
        Class<?> clazz = findClassWithSyntheticsImpl(className1, index);
        if (clazz != null) {
            sClassCache.put(cacheKey, clazz);
            return clazz;
        }
        clazz = findClassWithSyntheticsImpl(className2, index);
        if (clazz != null) {
            sClassCache.put(cacheKey, clazz);
            return clazz;
        }
        clazz = findClassWithSyntheticsImpl(className3, index);
        if (clazz != null) {
            sClassCache.put(cacheKey, clazz);
            return clazz;
        }
        Log.e("Initiator/E class " + className1 + " not found");
        return null;
    }
}
