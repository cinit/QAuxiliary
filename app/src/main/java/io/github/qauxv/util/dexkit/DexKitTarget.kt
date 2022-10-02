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

package io.github.qauxv.util.dexkit

import android.os.Bundle
import android.view.View
import cc.ioctl.util.HostInfo
import com.github.kyuubiran.ezxhelper.utils.isAbstract
import com.github.kyuubiran.ezxhelper.utils.isFinal
import com.github.kyuubiran.ezxhelper.utils.isStatic
import com.github.kyuubiran.ezxhelper.utils.paramCount
import com.livefront.sealedenum.GenSealedEnum
import com.tencent.common.app.AppInterface
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.util.Initiator._BaseChatPie
import io.github.qauxv.util.Initiator._ChatMessage
import io.github.qauxv.util.Initiator._FriendProfileCardActivity
import io.github.qauxv.util.Initiator._QQAppInterface
import io.github.qauxv.util.Initiator._TroopChatPie
import io.github.qauxv.util.Initiator.getHostClassLoader
import io.github.qauxv.util.Initiator.load
import io.github.qauxv.util.Log
import me.ketal.data.ConfigData
import mqq.app.AppRuntime

sealed class DexKitTarget {
    val version = HostInfo.getVersionCode32()

    sealed class UsingStr : DexKitTarget() {
        open val findMethod: Boolean = false
        abstract val traitString: Array<String>
    }

    abstract val declaringClass: String
    open val preferredDexIndexArray: IntArray = intArrayOf()
    abstract val filter: dexkitFilter

    private val descCacheKey by lazy { ConfigData<String>("cache#$name#$version", ConfigManager.getCache()) }
    var descCache: String?
        get() = descCacheKey.value
        set(value) {
            descCacheKey.value = value
        }

    open fun verifyTargetMethod(methods: List<DexMethodDescriptor>): DexMethodDescriptor? {
        return kotlin.runCatching {
            val filter = methods.filter(filter)
            if (filter.size > 1) {
                filter.forEach { Log.e(it.toString()) }
                Log.e("More than one method matched: $name, return the first one")
            }
            // check(filter.size <= 1) { "Too many methods found for $this: ${filter.size}" }
            filter.firstOrNull()
        }.onFailure { Log.e(it) }.getOrNull()
    }

    @GenSealedEnum
    companion object
}

object CDialogUtil : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/utils/DialogUtil"
    override val traitString = arrayOf("android.permission.SEND_SMS")
    override val preferredDexIndexArray = intArrayOf(1, 4, 3)
    override val filter = DexKitFilter.allStaticFields
}

object CFaceDe : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/activity/ChatActivityFacade"
    override val traitString = arrayOf("reSendEmo")
    override val preferredDexIndexArray = intArrayOf(2, 6, 3, 7)
    override val filter = DexKitFilter.allStaticFields
}

object CFlashPicHelper : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.app.FlashPicHelper"
    override val traitString = arrayOf("FlashPicHelper")
    override val preferredDexIndexArray = intArrayOf(2, 6, 3, 7)
    override val filter = DexKitFilter.allStaticFields
}

object CBasePicDlProcessor : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/transfile/BasePicDownloadProcessor"
    override val traitString = arrayOf("BasePicDownl")
    override val preferredDexIndexArray = intArrayOf(4, 7, 2)
    override val filter = filter@{ it: DexMethodDescriptor ->
        val clz = load(it.declaringClass) ?: return@filter false
        clz.declaredFields.any { it.isStatic && it.isFinal && it.type == java.util.regex.Pattern::class.java }
    }
}

object CItemBuilderFactory : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/activity/aio/item/ItemBuilderFactory"
    override val traitString = arrayOf("ItemBuilder is: D", "findItemBuilder: invoked.")
    override val preferredDexIndexArray = intArrayOf(11, 6, 1)
    override val filter = filter@{ it: DexMethodDescriptor ->
        val clz = load(it.declaringClass) ?: return@filter false
        clz.superclass == Any::class.java && !clz.isAbstract
    }
}

object CAIOUtils : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.activity.aio.AIOUtils"
    override val traitString = arrayOf("openAIO by MT")
    override val preferredDexIndexArray = intArrayOf(2, 11, 6)
    override val filter = DexKitFilter.allStaticFields
}

object CAbsGalScene : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/common/galleryactivity/AbstractGalleryScene"
    override val traitString = arrayOf("gallery setColor bl")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val clz = load(it.declaringClass) ?: return@filter false
        clz.isAbstract && clz.declaredFields.any { it.type == View::class.java }
    }
}

object CFavEmoConst : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/emosm/favroaming/FavEmoConstant"
    override val traitString = arrayOf("http://p.qpic.", "https://p.qpic.")
    override val preferredDexIndexArray = intArrayOf(3, 4, 5)
    override val filter = filter@{ it: DexMethodDescriptor ->
        val clz = load(it.declaringClass) ?: return@filter false
        !clz.isAbstract && clz.fields.all { it.isStatic } && clz.declaredMethods.size <= 3
    }
}

object CMessageRecordFactory : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.service.message.MessageRecordFactory"
    override val traitString = arrayOf("createPicMessage")
    override val preferredDexIndexArray = intArrayOf(15, 4)
    override val filter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        m.parameterTypes[0] == AppInterface::class.java
    }
}

object CContactUtils : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/utils/ContactUtils"
    override val traitString = arrayOf(" - WiFi")
    override val preferredDexIndexArray = intArrayOf(4)
    override val filter = DexKitFilter.allStaticFields
}

object CArkAppItemBubbleBuilder : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/activity/aio/item/ArkAppItemBubbleBuilder"
    override val traitString = arrayOf("debugArkMeta = ")
    override val preferredDexIndexArray = intArrayOf(2, 11, 6)
    override val filter = filter@{ it: DexMethodDescriptor ->
        val clz = load(it.declaringClass) ?: return@filter false
        val superClz = clz.superclass
        !clz.isAbstract && superClz != Any::class.java && superClz.isAbstract && "Builder" in superClz.name
    }
}

object CPngFrameUtil : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.magicface.drawable.PngFrameUtil"
    override val traitString = arrayOf("func checkRandomPngEx")
    override val preferredDexIndexArray = intArrayOf(3, 2)
    override val filter = filter@{ it: DexMethodDescriptor ->
        val clz = load(it.declaringClass) ?: return@filter false
        clz.methods.filter { "b" != it.name && it.returnType == Int::class.java && it.isStatic }
            .any { it.parameterTypes.contentEquals(arrayOf(Int::class.java)) }
    }
}

object CPicEmoticonInfo : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.emoticonview.PicEmoticonInfo"
    override val traitString = arrayOf("send emotion + 1:")
    override val preferredDexIndexArray = intArrayOf(3, 4)
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/emoticonview") or
        filter@{ it: DexMethodDescriptor ->
            val clz = load(it.declaringClass) ?: return@filter false
            !clz.isAbstract
                && clz.superclass != Any::class.java
                && clz.superclass.superclass != Any::class.java
                && clz.superclass.superclass.superclass == Any::class.java
        }
}

object CSimpleUiUtil : DexKitTarget.UsingStr() {
    // dummy, placeholder, just a guess
    override val declaringClass = "com.tencent.mobileqq.theme.SimpleUIUtil"
    override val traitString = arrayOf("key_simple_status_s")
    override val preferredDexIndexArray = intArrayOf(4, 2)
    override val filter = DexKitFilter.allStaticFields
}

object CTroopGiftUtil : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/troop/utils/TroopGiftUtil"
    override val traitString = arrayOf(".troop.send_giftTroopUtils", ".troop.send_giftTroopMemberUtil")
    override val preferredDexIndexArray = intArrayOf(4, 9, 2)
    override val filter = DexKitFilter.allStaticFields
}

object CQzoneMsgNotify : DexKitTarget.UsingStr() {
    override val declaringClass = "cooperation/qzone/push/MsgNotification"
    override val traitString = arrayOf("use small icon ,exp:")
    override val preferredDexIndexArray = intArrayOf(4, 3)
    override val filter = filter@{ it: DexMethodDescriptor ->
        val clz = load(it.declaringClass) ?: return@filter false
        !clz.isAbstract
            && clz.superclass == Any::class.java
            && clz.methods.any {
            val argt = it.parameterTypes
            it.returnType == Void.TYPE && argt.size > 7 && argt[0] == _QQAppInterface()
        }
    }
}

object CAppConstants : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.app.AppConstants"
    override val traitString = arrayOf(".indivAnim/")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val clz = load(it.declaringClass) ?: return@filter false
        clz.isInterface && clz.declaredMethods.size >= 50
    }
}

object CCustomWidgetUtil : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.widget.CustomWidgetUtil"
    override val traitString = arrayOf("^NEW$")
    override val preferredDexIndexArray = intArrayOf(20, 5, 4, 9)
    override val filter = DexKitFilter.strInClsName("com/tencent/widget") and
        DexKitFilter.notHasSuper and
        filter@{ it: DexMethodDescriptor ->
            val clz = load(it.declaringClass) ?: return@filter false
            clz.fields.all { it.isStatic }
        }
}

object CMessageCache : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/service/message/MessageCache"
    override val traitString = arrayOf("Q.msg.MessageCache")
    override val preferredDexIndexArray = intArrayOf(1, 4)
    override val filter = filter@{ it: DexMethodDescriptor -> "<clinit>" == it.name }
}

object CScreenShotHelper : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.screendetect.ScreenShotHelper"
    override val traitString = arrayOf("onActivityResumeHideFloatView")
    override val preferredDexIndexArray = intArrayOf(4, 2)
    override val filter = DexKitFilter.notHasSuper
}

object CTimeFormatterUtils : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.utils.TimeFormatterUtils"
    override val traitString = arrayOf("TimeFormatterUtils")
    override val preferredDexIndexArray = intArrayOf(1, 4)
    override val filter = DexKitFilter.allStaticFields
}

object CGroupAppActivity : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/activity/aio/drawer/TroopAppShortcutDrawer"
    override val traitString = arrayOf("onDrawerStartOpen")
    override val preferredDexIndexArray = intArrayOf(2, 11, 6)
    override val filter = DexKitFilter.hasSuper and
        filter@{ it: DexMethodDescriptor ->
            val clz = load(it.declaringClass) ?: return@filter false
            clz.declaredFields.any { it.type.name.endsWith("TroopAppShortcutContainer") }
        }
}

object CIntimateDrawer : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/activity/aio/drawer/IntimateInfoChatDrawer"
    override val traitString = arrayOf("onDrawerOpened, needReqIntimateInfo: %s")
    override val preferredDexIndexArray = intArrayOf(7, 6)
    override val filter = DexKitFilter.hasSuper
}

object CZipUtils : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/biz/common/util/ZipUtils"
    override val traitString = arrayOf(",ZipEntry name: ")
    override val preferredDexIndexArray = intArrayOf(1, 9, 5)
    override val filter = DexKitFilter.allStaticFields
}

object CHttpDownloader : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/transfile/HttpDownloader"
    override val traitString = arrayOf("[reportHttpsResult] url=")
    override val preferredDexIndexArray = intArrayOf(4, 10, 7, 2)
    override val filter = DexKitFilter.notHasSuper
}

object CMultiMsgManager : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/multimsg/MultiMsgManager"
    override val traitString = arrayOf("[sendMultiMsg]data.length = ")
    override val preferredDexIndexArray = intArrayOf(9, 4, 3, 7)
    override val filter = DexKitFilter.hasSuper
}

object CAvatarUtil : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.avatar.utils.AvatarUtil"
    override val traitString = arrayOf("AvatarUtil")
    override val preferredDexIndexArray = intArrayOf(8)
    override val filter = DexKitFilter.notHasSuper
}

object CFaceManager : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.app.face.FaceManager"
    override val traitString = arrayOf("FaceManager")
    override val preferredDexIndexArray = intArrayOf(3)
    override val filter = DexKitFilter.notHasSuper
}

object CAIOPictureView : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.richmediabrowser.view.AIOPictureView"
    override val traitString = arrayOf("AIOPictureView", "AIOGalleryPicView")
    override val preferredDexIndexArray = intArrayOf(10, 4, 2)
    override val filter = DexKitFilter.hasSuper
}

object CGalleryBaseScene : DexKitTarget.UsingStr() {
    // guess
    override val declaringClass = "com.tencent.mobileqq.gallery.view.GalleryBaseScene"
    override val traitString = arrayOf("GalleryBaseScene")
    override val preferredDexIndexArray = intArrayOf(2)
    override val filter = filter@{ it: DexMethodDescriptor ->
        val clz = load(it.declaringClass) ?: return@filter false
        clz.declaredFields.any { it.type == View::class.java }
    }
}

object CGuildHelperProvider : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.guild.chatpie.GuildHelperProvider"
    override val traitString = arrayOf("onFoldStatus beginMoveFoldStatus:")
    override val preferredDexIndexArray = intArrayOf(9)
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/guild/chatpie")
}

object CGuildArkHelper : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.guild.chatpie.helper.GuildArkHelper"
    override val traitString = arrayOf("GuildArkHelper")
    override val preferredDexIndexArray = intArrayOf(9)
    override val filter = filter@{ it: DexMethodDescriptor ->
        val clz = load(it.declaringClass) ?: return@filter false
        clz.methods.any { it.name == "getTag" }
    }
}

object CReplyMsgUtils : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.activity.aio.reply.ReplyMsgUtils"
    override val traitString = arrayOf("generateSourceInfo sender uin exception:")
    override val preferredDexIndexArray = intArrayOf(7)
    override val filter = DexKitFilter.allStaticFields
}

object CReplyMsgSender : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.replymsg.ReplyMsgSender"
    override val traitString = arrayOf("sendReplyMessage uniseq=0")
    override val preferredDexIndexArray = intArrayOf(13)
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/replymsg/")
}

object CPopOutEmoticonUtil : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.popanim.util.PopOutEmoticonUtil"
    override val traitString = arrayOf("supportPopOutEmoticon isC2C=")
    override val preferredDexIndexArray = intArrayOf(11)
    override val filter = DexKitFilter.allStaticFields
}

object CSystemMessageProcessor : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.app.message.SystemMessageProcessor"
    override val traitString = arrayOf("<---handleGetFriendSystemMsgResp : decode pb filtered")
    override val preferredDexIndexArray = intArrayOf(8)
    override val filter = DexKitFilter.allowAll
}

object COnlinePushPbPushTransMsg : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.app.handler.receivesuccess.OnlinePushPbPushTransMsg"
    override val traitString = arrayOf("PbPushTransMsg muteGeneralFlag:")
    override val preferredDexIndexArray = intArrayOf(8)
    override val filter = DexKitFilter.strInClsName("/receivesuccess/")
}

object NBaseChatPie_init : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass: String = _BaseChatPie().name
    override val traitString = arrayOf("input set error", ", mDefautlBtnLeft: ")
    override val preferredDexIndexArray = intArrayOf(6, 2, 7, 3)
    override val filter = DexKitFilter.strInClsName(declaringClass.replace('.', '/'))
}

object NBaseChatPie_createMulti : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass: String = _BaseChatPie().name
    override val traitString = arrayOf("createMulti")
    override val preferredDexIndexArray = intArrayOf(6, 2, 7, 3)
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/activity/aio/helper") or
        DexKitFilter.strInClsName(declaringClass.replace('.', '/')) and
        filter@{ it: DexMethodDescriptor ->
            val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
            m.parameterTypes.first() == _ChatMessage()
        }
}

object NBaseChatPie_chooseMsg : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass: String = _BaseChatPie().name
    override val traitString = arrayOf("set left text from cancel")
    override val preferredDexIndexArray = intArrayOf(6, 2, 7, 3)
    override val filter = DexKitFilter.strInClsName(declaringClass.replace('.', '/'))
}

object NLeftSwipeReplyHelper_reply : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/bubble/LeftSwipeReplyHelper"
    override val traitString = arrayOf("0X800A92F")
    override val preferredDexIndexArray = intArrayOf(8, 3, 2)
    override val filter = DexKitFilter.allowAll
}

object NAtPanel_showDialogAtView : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/troop/quickat/ui/AtPanel"
    override val traitString = arrayOf("showDialogAtView")
    override val preferredDexIndexArray = intArrayOf(12, 10, 4)
    override val filter = DexKitFilter.notHasSuper
}

object NAtPanel_refreshUI : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/troop/quickat/ui/AtPanel"
    override val traitString = arrayOf("resultList = null")
    override val preferredDexIndexArray = intArrayOf(12, 10, 4)
    override val filter = DexKitFilter.notHasSuper and
        filter@{ it: DexMethodDescriptor ->
            val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
            m.returnType == Void.TYPE
        }
}

object NFriendChatPie_updateUITitle : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/activity/aio/core/FriendChatPie"
    override val traitString = arrayOf("FriendChatPie updateUI_ti")
    override val preferredDexIndexArray = intArrayOf(4, 6, 2)
    override val filter = DexKitFilter.allowAll
}

object NProfileCardUtil_getCard : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.util.ProfileCardUtil"
    override val traitString = arrayOf("initCard bSuperVipOpen=")
    override val preferredDexIndexArray = intArrayOf(9, 10, 11, 5, 4, 2)
    override val filter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        "Card" == m.returnType.simpleName
    }
}

object NVasProfileTemplateController_onCardUpdate : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.profilecard.vas.VasProfileTemplateController"
    override val traitString = arrayOf("onCardUpdate fail.", "onCardUpdate: bgId=")
    override val preferredDexIndexArray = intArrayOf(7, 6)
    override val filter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        "onCardUpdate" == m.name
            || m.declaringClass.name == "com.tencent.mobileqq.profilecard.vas.VasProfileTemplateController"
            || m.declaringClass.isAssignableFrom(_FriendProfileCardActivity())
    }
}

object NQQSettingMe_updateProfileBubble : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.activity.QQSettingMe"
    override val traitString = arrayOf("updateProfileBubbleMsgView")
    override val preferredDexIndexArray = intArrayOf(4, 6, 8, 7)
    override val filter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        "QQSettingMe" in it.declaringClass || m.returnType == Void.TYPE
    }
}

object NQQSettingMe_onResume : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.activity.QQSettingMe"
    override val traitString = arrayOf("-->onResume!")
    override val preferredDexIndexArray = intArrayOf(4, 6, 8, 7)
    override val filter = DexKitFilter.strInClsName("QQSettingMe")
}

object NVipUtils_getPrivilegeFlags : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/utils/VipUtils"
    override val traitString = arrayOf("getPrivilegeFlags Friends is null")
    override val preferredDexIndexArray = intArrayOf(16, 11, 12, 14, 4, 2, 3)
    override val filter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        "getPrivilegeFlags" == m.name
            || m.parameterTypes.contentEquals(arrayOf(String::class.java))
            || m.parameterTypes.contentEquals(arrayOf(AppRuntime::class.java, String::class.java))
    }
}

object NTroopChatPie_showNewTroopMemberCount : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass: String = _TroopChatPie().name
    override val traitString = arrayOf("showNewTroopMemberCount info is null")
    override val preferredDexIndexArray = intArrayOf(4, 8, 11, 6)
    override val filter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        m.declaringClass.name == declaringClass && m.paramCount == 0
    }
}

object NConversation_onCreate : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/activity/home/Conversation"
    override val traitString = arrayOf("Recent_OnCreate")
    override val preferredDexIndexArray = intArrayOf(4, 8, 11, 6)
    override val filter = DexKitFilter.strInClsName("Conversation")
}

object NBaseChatPie_mosaic : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass: String = _BaseChatPie().name
    override val traitString = arrayOf("enableMosaicEffect")
    override val preferredDexIndexArray = intArrayOf(6, 2, 7, 3)
    override val filter = DexKitFilter.strInClsName(declaringClass.replace('.', '/'))
}

object NWebSecurityPluginV2_callback : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.webview.WebSecurityPluginV2\$"
    override val traitString = arrayOf("check finish jr=")
    override val preferredDexIndexArray = intArrayOf(17, 10)
    override val filter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        m.parameterTypes.contentEquals(arrayOf(Bundle::class.java))
    }
}

object NTroopAppShortcutBarHelper_resumeAppShorcutBar : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.activity.aio.helper.TroopAppShortcutBarHelper"
    override val traitString = arrayOf("resumeAppShorcutBar")
    override val preferredDexIndexArray = intArrayOf(17, 10)
    override val filter = DexKitFilter.strInClsName("TroopAppShortcutBarHelper") or
        DexKitFilter.strInClsName("ShortcutBarAIOHelper") or
        DexKitFilter.strInClsName("/aio/helper/")
}

object NChatActivityFacade_sendMsgButton : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/activity/ChatActivityFacade"
    override val traitString = arrayOf(" sendMessage start currenttime:")
    override val preferredDexIndexArray = intArrayOf(2, 6, 3, 7)
    override val filter = DexKitFilter.strInClsName("ChatActivityFacade") and
        filter@{ it: DexMethodDescriptor ->
            val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
            m.paramCount == 6
        }
}

object NFriendsStatusUtil_isChatAtTop : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.app.utils.FriendsStatusUtil"
    override val traitString = arrayOf("isChatAtTop result is: ")
    override val preferredDexIndexArray = intArrayOf(8)
    override val filter = DexKitFilter.strInClsName("FriendsStatusUtil")
}

object NVipUtils_getUserStatus : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.utils.VipUtils"
    override val traitString = arrayOf("getUserStatus Friends is null")
    override val preferredDexIndexArray = intArrayOf(16, 11, 12, 14, 4, 2, 3)
    override val filter = DexKitFilter.allowAll
}

object NPhotoListPanel_resetStatus : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.activity.aio.photo.PhotoListPanel"
    override val traitString = arrayOf("resetStatus selectSize:")
    override val preferredDexIndexArray = intArrayOf(7)
    override val filter = DexKitFilter.strInSig("(Z)V")
}

object NContactUtils_getDiscussionMemberShowName : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.utils.ContactUtils"
    override val traitString = arrayOf("getDiscussionMemberShowName uin is null")
    override val preferredDexIndexArray = intArrayOf(16, 15, 14, 4, 10)
    override val filter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        m.isStatic && m.returnType == String::class.java && m.paramCount == 3
    }
}

object NContactUtils_getBuddyName : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.utils.ContactUtils"
    override val traitString = arrayOf("getBuddyName()")
    override val preferredDexIndexArray = intArrayOf(16, 15, 14, 4, 10)
    override val filter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        m.isStatic && m.returnType == String::class.java && m.paramCount == 3
    }
}

object NScene_checkDataRecmdRemarkList : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.troopAddFrd.Scene"
    override val traitString = arrayOf("checkDataRecmdRemarkList cacheInvalid_ts_type_troopUin=%b_%d_%d_%s")
    override val preferredDexIndexArray = intArrayOf(16)
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/troopAddFrd")
}

// TODO 待优化这几种类型
object NTextItemBuilder_setETText : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/activity/aio/item/TextItemBuilder"
    override val traitString = arrayOf("NOT_USED()")
    override val preferredDexIndexArray = intArrayOf(-1)
    override val filter = DexKitFilter.allowAll
}

object NAIOPictureView_setVisibility : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/activity/aio/photo/AIOPictureView"
    override val traitString = arrayOf("NOT_USED()")
    override val preferredDexIndexArray = intArrayOf(-1)
    override val filter = DexKitFilter.allowAll
}

object NAIOPictureView_onDownloadOriginalPictureClick : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/activity/aio/photo/AIOPictureView"
    override val traitString = arrayOf("NOT_USED()")
    override val preferredDexIndexArray = intArrayOf(-1)
    override val filter = DexKitFilter.allowAll
}
