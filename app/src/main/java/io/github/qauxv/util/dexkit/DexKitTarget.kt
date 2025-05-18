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
import android.view.ViewGroup
import cc.ioctl.util.HostInfo
import com.github.kyuubiran.ezxhelper.utils.isAbstract
import com.github.kyuubiran.ezxhelper.utils.isFinal
import com.github.kyuubiran.ezxhelper.utils.isStatic
import com.github.kyuubiran.ezxhelper.utils.paramCount
import com.livefront.sealedenum.GenSealedEnum
import com.tencent.common.app.AppInterface
import com.tencent.mobileqq.app.QQAppInterface
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.util.Initiator._BaseChatPie
import io.github.qauxv.util.Initiator._ChatMessage
import io.github.qauxv.util.Initiator._FriendProfileCardActivity
import io.github.qauxv.util.Initiator._QQAppInterface
import io.github.qauxv.util.Initiator._TroopChatPie
import io.github.qauxv.util.Initiator.getHostClassLoader
import io.github.qauxv.util.Initiator.load
import io.github.qauxv.util.Log
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import me.ketal.data.ConfigData
import mqq.app.AppRuntime

sealed class DexKitTarget {
    val version = HostInfo.getVersionCode32()

    sealed class UsingStr : DexKitTarget() {
        // with 'OR' relationship
        abstract val traitString: Array<String>
    }

    sealed class UsingStringVector : DexKitTarget() {
        // relationship: ((v[0][0] && v[0][1] && ..) || (v[1][0] && v[1][1] && ..) || ..)
        abstract val traitStringVectors: Array<Array<String>>
    }

    sealed class UsingDexkit : DexKitTarget()

    abstract val declaringClass: String
    open val findMethod: Boolean = false
    abstract val filter: dexkitFilter

    private val descCacheKey by lazy { ConfigData<String>("cache#$name#$version", ConfigManager.getCache()) }
    var descCache: String?
        get() = descCacheKey.value
        set(value) {
            if (!value.isNullOrEmpty()) {
                // check if the value is valid
                DexMethodDescriptor(value)
            }
            descCacheKey.value = value
        }

    open fun verifyTargetMethod(methods: List<DexMethodDescriptor>): DexMethodDescriptor? {
        return kotlin.runCatching {
            val filter = methods.filter(filter)
            if (filter.size > 1) {
                filter.forEach { Log.e(it.toString()) }
                if (!findMethod) {
                    val sameClass = filter.distinctBy { it.declaringClass }.size == 1
                    if (sameClass) {
                        Log.w("More than one method matched: $name, but has same class")
                        return filter.first()
                    }
                }
                Log.e("More than one method matched: $name, return none for safety")
                return null
            }
            filter.firstOrNull()
        }.onFailure { Log.e(it) }.getOrNull()
    }

    @GenSealedEnum
    companion object
}

data object CDialogUtil : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/utils/DialogUtil"
    override val traitString = arrayOf("android.permission.SEND_SMS")
    override val filter = DexKitFilter.allStaticFields and DexKitFilter.clinit
}

data object CFaceDe : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/activity/ChatActivityFacade"
    override val traitString = arrayOf("reSendEmo")
    override val filter = DexKitFilter.allStaticFields
}

data object CFlashPicHelper : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.app.FlashPicHelper"
    override val traitString = arrayOf("FlashPicHelper")
    override val filter = DexKitFilter.allStaticFields
}

data object CBasePicDlProcessor : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/transfile/BasePicDownloadProcessor"
    override val traitString = arrayOf("BasePicDownloadProcessor.onSuccess():Delete ")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val clz = load(it.declaringClass) ?: return@filter false
        clz.declaredFields.any { it.isStatic && it.isFinal && it.type == java.util.regex.Pattern::class.java }
    }
}

data object CItemBuilderFactory : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/activity/aio/item/ItemBuilderFactory"
    override val traitString = arrayOf("ItemBuilder is: D", "findItemBuilder: invoked.")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val clz = load(it.declaringClass) ?: return@filter false
        clz.superclass == Any::class.java && !clz.isAbstract
    }
}

data object CAIOUtils : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.activity.aio.AIOUtils"
    override val traitString = arrayOf("openAIO by MT")
    override val filter = DexKitFilter.allStaticFields
}

data object CAbsGalScene : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/common/galleryactivity/AbstractGalleryScene"
    override val traitString = arrayOf("gallery setColor bl")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val clz = load(it.declaringClass) ?: return@filter false
        clz.isAbstract && clz.declaredFields.any { it.type == View::class.java }
    }
}

data object CFavEmoConst : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/emosm/favroaming/FavEmoConstant"
    override val traitString = arrayOf("http://p.qpic.", "https://p.qpic.")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val clz = load(it.declaringClass) ?: return@filter false
        !clz.isAbstract && clz.fields.all { it.isStatic } && clz.declaredMethods.size <= 3
    }
}

data object CMessageRecordFactory : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.service.message.MessageRecordFactory"
    override val traitString = arrayOf("createPicMessage")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        m.parameterTypes[0] == AppInterface::class.java || m.parameterTypes[0] == QQAppInterface::class.java
    }
}

data object CArkAppItemBubbleBuilder : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/activity/aio/item/ArkAppItemBubbleBuilder"
    override val traitString = arrayOf("debugArkMeta = ")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val clz = load(it.declaringClass) ?: return@filter false
        val superClz = clz.superclass
        !clz.isAbstract && superClz != Any::class.java && superClz.isAbstract && "Builder" in superClz.name
    }
}

data object CPngFrameUtil : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.magicface.drawable.PngFrameUtil"
    override val traitString = arrayOf("func checkRandomPngEx")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val clz = load(it.declaringClass) ?: return@filter false
        clz.methods.filter { "b" != it.name && it.returnType == Int::class.java && it.isStatic }
            .any { it.parameterTypes.contentEquals(arrayOf(Int::class.java)) }
    }
}

data object CPicEmoticonInfo : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.emoticonview.PicEmoticonInfo"
    override val traitString = arrayOf("send emotion + 1:")
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/emoticonview") or
        filter@{ it: DexMethodDescriptor ->
            val clz = load(it.declaringClass) ?: return@filter false
            !clz.isAbstract
                && clz.superclass != Any::class.java
                && clz.superclass.superclass != Any::class.java
                && clz.superclass.superclass.superclass == Any::class.java
        }
}

data object CSimpleUiUtil : DexKitTarget.UsingStr() {
    // dummy, placeholder, just a guess
    override val declaringClass = "com.tencent.mobileqq.theme.SimpleUIUtil"
    override val traitString = arrayOf("key_simple_status_s")
    override val filter = DexKitFilter.allStaticFields
}

data object CTroopGiftUtil : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/troop/utils/TroopGiftUtil"
    override val traitString = arrayOf(".troop.send_giftTroopUtils", ".troop.send_giftTroopMemberUtil")
    override val filter = DexKitFilter.allStaticFields
}

data object CQzoneMsgNotify : DexKitTarget.UsingStr() {
    override val declaringClass = "cooperation/qzone/push/MsgNotification"
    override val traitString = arrayOf("use small icon ,exp:")
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

data object CAppConstants : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.app.AppConstants"
    override val traitString = arrayOf(".indivAnim/")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val clz = load(it.declaringClass) ?: return@filter false
        clz.isInterface && clz.declaredMethods.size >= 50
    }
}

data object CMessageCache : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/service/message/MessageCache"
    override val traitString = arrayOf("Q.msg.MessageCache")
    override val filter = filter@{ it: DexMethodDescriptor -> "<clinit>" == it.name }
}

data object CScreenShotHelper : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.screendetect.ScreenShotHelper"
    override val traitString = arrayOf("onActivityResumeHideFloatView")
    override val filter = DexKitFilter.notHasSuper
}

data object CTimeFormatterUtils : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.utils.TimeFormatterUtils"

    // old: arrayOf("TimeFormatterUtils")
    override val traitString = arrayOf("^EEEE$")
    override val filter = DexKitFilter.allStaticFields
}

data object CGroupAppActivity : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/activity/aio/drawer/TroopAppShortcutDrawer"
    override val traitString = arrayOf("onDrawerStartOpen")
    override val filter = DexKitFilter.hasSuper and
        filter@{ it: DexMethodDescriptor ->
            val clz = load(it.declaringClass) ?: return@filter false
            clz.declaredFields.any { it.type.name.endsWith("TroopAppShortcutContainer") }
        }
}

data object CIntimateDrawer : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/activity/aio/drawer/IntimateInfoChatDrawer"
    override val traitString = arrayOf("onDrawerOpened, needReqIntimateInfo: %s")
    override val filter = DexKitFilter.hasSuper
}

data object CZipUtils : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/biz/common/util/ZipUtils"
    override val traitString = arrayOf(",ZipEntry name: ")
    override val filter = DexKitFilter.allStaticFields
}

data object CHttpDownloader : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/transfile/HttpDownloader"
    override val traitString = arrayOf("[reportHttpsResult] url=")
    override val filter = DexKitFilter.notHasSuper
}

data object CMultiMsgManager : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/multimsg/MultiMsgManager"
    override val traitString = arrayOf("[sendMultiMsg]data.length = ")
    override val filter = DexKitFilter.filterByParams {
        it.first() == _QQAppInterface()
    }
}

data object CAvatarUtil : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.avatar.utils.AvatarUtil"
    override val traitString = arrayOf("===getDiscussionUinFromPstn pstnDiscussionUin is null ===")
    override val filter = DexKitFilter.notHasSuper
}

data object CFaceManager : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.app.face.FaceManager"
    override val traitString = arrayOf("FaceManager")
    override val filter = DexKitFilter.notHasSuper
}

data object CAIOPictureView : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.richmediabrowser.view.AIOPictureView"
    override val traitString = arrayOf("AIOPictureView", "AIOGalleryPicView")
    override val filter = DexKitFilter.hasSuper
}

data object CGalleryBaseScene : DexKitTarget.UsingStr() {
    // guess
    override val declaringClass = "com.tencent.mobileqq.gallery.view.GalleryBaseScene"
    override val traitString = arrayOf("GalleryBaseScene")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val clz = load(it.declaringClass) ?: return@filter false
        clz.declaredFields.any { it.type == View::class.java }
    }
}

data object CGuildHelperProvider : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.guild.chatpie.GuildHelperProvider"
    override val traitString = arrayOf("^GuildHelperProvider$")
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/guild/chatpie")
}

data object CGuildArkHelper : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.guild.chatpie.helper.GuildArkHelper"
    override val traitString = arrayOf("GuildArkHelper")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val clz = load(it.declaringClass) ?: return@filter false
        clz.methods.any { it.name == "getTag" }
    }
}

data object CReplyMsgUtils : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.activity.aio.reply.ReplyMsgUtils"
    override val traitString = arrayOf("generateSourceInfo sender uin exception:")
    override val filter = DexKitFilter.filterByParams {
        it.first() == _QQAppInterface()
    }
}

data object CReplyMsgSender : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.replymsg.ReplyMsgSender"
    override val traitString = arrayOf("sendReplyMessage uniseq=0")
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/replymsg/") or DexKitFilter.defpackage
}

data object CPopOutEmoticonUtil : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.popanim.util.PopOutEmoticonUtil"
    override val traitString = arrayOf("supportPopOutEmoticon isC2C=")
    override val filter = DexKitFilter.allStaticFields
}

data object CTestStructMsg : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/structmsg/TestStructMsg"
    override val traitString = arrayOf("TestStructMsg")
    override val filter = DexKitFilter.allStaticFields
}

data object CSystemMessageProcessor : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.app.message.SystemMessageProcessor"
    override val traitString = arrayOf("<---handleGetFriendSystemMsgResp : decode pb filtered")
    override val filter = DexKitFilter.allowAll
}

data object COnlinePushPbPushTransMsg : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.app.handler.receivesuccess.OnlinePushPbPushTransMsg"
    override val traitString = arrayOf("PbPushTransMsg muteGeneralFlag:")
    override val filter = if (requireMinQQVersion(QQVersion.QQ_9_1_30)) DexKitFilter.allowAll else DexKitFilter.strInClsName("/receivesuccess/")
}

data object CFrameControllerInjectImpl : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.mobileqq.activity.framebusiness.controllerinject.FrameControllerInjectImpl"
    override val traitString = arrayOf("FrameControllerInjectImpl.recoverTabBluer")
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/activity/framebusiness/controllerinject/")
}

data object ForwardSendPicUtil : DexKitTarget.UsingStr() {
    override val declaringClass = "com/tencent/mobileqq/utils/ForwardSendPicUtil"
    override val traitString = arrayOf("ForwardSendPicUtil")
    override val filter = DexKitFilter.allowAll
}

data object AbstractQQCustomMenuItem : DexKitTarget.UsingStr() {
    override val declaringClass = "com.tencent.qqnt.aio.menu.ui.AbstractQQCustomMenuItem"
    override val traitString = arrayOf("QQCustomMenuItem{title='")
    override val filter = DexKitFilter.strInClsName("com/tencent/qqnt/aio/menu/ui")
}

data object VasAttrBuilder : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.vas.p"
    override val traitString = arrayOf("attrs")
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/vas")
}

data object Guild_Emo_Btn_Create_QQNT : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val traitString = arrayOf("mEmojiLayout.findViewByI…id.guild_aio_emoji_image)")
    override val declaringClass = "Guild_Emo_Btn_Create_QQNT"
    override val filter = DexKitFilter.allowAll
}

data object NBaseChatPie_init : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass: String = _BaseChatPie()?.name ?: "com.tencent.mobileqq.activity.BaseChatPie"
    override val traitString = arrayOf("input set error", ", mDefautlBtnLeft: ")
    override val filter = DexKitFilter.strInClsName(declaringClass.replace('.', '/'))
}

data object NBaseChatPie_createMulti : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass: String = _BaseChatPie()?.name ?: "com.tencent.mobileqq.activity.BaseChatPie"
    override val traitString = arrayOf("^createMulti$")
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/activity/aio/helper") or
        DexKitFilter.defpackage or
        DexKitFilter.strInClsName(declaringClass.replace('.', '/')) and
        filter@{ it: DexMethodDescriptor ->
            val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
            m.parameterTypes.first() == _ChatMessage()
        }
}

data object NBaseChatPie_chooseMsg : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass: String = _BaseChatPie()?.name ?: "com.tencent.mobileqq.activity.BaseChatPie"
    override val traitString = arrayOf("set left text from cancel")
    override val filter = DexKitFilter.strInClsName(declaringClass.replace('.', '/'))
}

data object NLeftSwipeReplyHelper_reply : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/bubble/LeftSwipeReplyHelper"
    override val traitString = arrayOf("0X800A92F")
    override val filter = DexKitFilter.allowAll
}

data object NAtPanel_showDialogAtView : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/troop/quickat/ui/AtPanel"
    override val traitString = arrayOf("showDialogAtView")
    override val filter = DexKitFilter.notHasSuper
}

data object NAtPanel_refreshUI : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/troop/quickat/ui/AtPanel"
    override val traitString = arrayOf("resultList = null")
    override val filter = DexKitFilter.notHasSuper and
        filter@{ it: DexMethodDescriptor ->
            val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
            m.returnType == Void.TYPE
        }
}

data object NFriendChatPie_updateUITitle : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/activity/aio/core/FriendChatPie"
    override val traitString = arrayOf("FriendChatPie updateUI_ti")
    override val filter = DexKitFilter.allowAll
}

data object NProfileCardUtil_getCard : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.util.ProfileCardUtil"
    override val traitString = arrayOf("initCard bSuperVipOpen=")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        "Card" == m.returnType.simpleName
    }
}

data object NVasProfileTemplateController_onCardUpdate : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.profilecard.vas.VasProfileTemplateController"
    override val traitString = arrayOf("onCardUpdate fail.", "onCardUpdate: bgId=")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        "onCardUpdate" == m.name
            || m.declaringClass.name == "com.tencent.mobileqq.profilecard.vas.VasProfileTemplateController"
            || m.declaringClass.isAssignableFrom(_FriendProfileCardActivity())
    }
}

data object NQQSettingMe_updateProfileBubble : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.activity.QQSettingMe"
    override val traitString = arrayOf("updateProfileBubbleMsgView")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        ("QQSettingMe" in it.declaringClass || m.returnType == Void.TYPE) && "bizParts" !in it.declaringClass
    }
}

data object NQQSettingMe_onResume : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.activity.QQSettingMe"
    override val traitString = arrayOf("-->onResume!")
    override val filter = filter@{ it: DexMethodDescriptor ->
        it.declaringClass.contains("QQSettingMe") && !it.declaringClass.contains("V9")
        // 暂不支持V9侧滑栏，排除
    }
}

data object NVipUtils_getPrivilegeFlags : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/utils/VipUtils"
    override val traitString = arrayOf("getPrivilegeFlags Friends is null")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        "getPrivilegeFlags" == m.name
            || m.parameterTypes.contentEquals(arrayOf(String::class.java))
            || m.parameterTypes.contentEquals(arrayOf(AppRuntime::class.java, String::class.java))
    }
}

data object NTroopChatPie_showNewTroopMemberCount : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass: String = _TroopChatPie()?.name ?: "com.tencent.mobileqq.activity.aio.core.TroopChatPie"
    override val traitString = arrayOf("showNewTroopMemberCount info is null")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        m.declaringClass.name == declaringClass && m.paramCount == 0
    }
}

data object NConversation_onCreate : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/activity/home/Conversation"
    override val traitString = arrayOf("Recent_OnCreate")
    override val filter = DexKitFilter.strInClsName("Conversation")
}

data object NBaseChatPie_mosaic : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass: String = _BaseChatPie()?.name ?: "com.tencent.mobileqq.activity.aio.core.BaseChatPie"
    override val traitString = arrayOf("enableMosaicEffect")
    override val filter = DexKitFilter.strInClsName(declaringClass.replace('.', '/'))
}

data object NWebSecurityPluginV2_callback : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.webview.WebSecurityPluginV2\$"
    override val traitString = arrayOf("check finish jr=")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        m.parameterTypes.contentEquals(arrayOf(Bundle::class.java))
    }
}

data object NTroopAppShortcutBarHelper_resumeAppShorcutBar : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.activity.aio.helper.TroopAppShortcutBarHelper"
    override val traitString = arrayOf("resumeAppShorcutBar")
    override val filter = DexKitFilter.strInClsName("TroopAppShortcutBarHelper") or
        DexKitFilter.strInClsName("ShortcutBarAIOHelper") or
        DexKitFilter.strInClsName("/aio/helper/") or
        DexKitFilter.defpackage
}

data object NChatActivityFacade_sendMsgButton : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/activity/ChatActivityFacade"
    override val traitString = arrayOf(" sendMessage start currenttime:")
    override val filter = DexKitFilter.strInClsName("ChatActivityFacade") and
        filter@{ it: DexMethodDescriptor ->
            val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
            m.paramCount == 6
        }
}

data object NFriendsStatusUtil_isChatAtTop : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.app.utils.FriendsStatusUtil"
    override val traitString = arrayOf("isChatAtTop result is: ")
    override val filter = DexKitFilter.strInClsName("FriendsStatusUtil")
}

data object NVipUtils_getUserStatus : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.utils.VipUtils"
    override val traitString = arrayOf("getUserStatus Friends is null")
    override val filter = DexKitFilter.allowAll
}

data object NPhotoListPanel_resetStatus : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.activity.aio.photo.PhotoListPanel"
    override val traitString = arrayOf("resetStatus selectSize:")
    override val filter = DexKitFilter.strInSig("(Z)V")
}

data object NContactUtils_getDiscussionMemberShowName : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.utils.ContactUtils"
    override val traitString = arrayOf("getDiscussionMemberShowName uin is null")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        m.isStatic && m.returnType == String::class.java && m.paramCount == 3
    }
}

data object NContactUtils_getBuddyName : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.utils.ContactUtils"
    override val traitString = arrayOf("getBuddyName()")
    override val filter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        m.isStatic && m.returnType == String::class.java && m.paramCount == 3
    }
}

data object NScene_checkDataRecmdRemarkList : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.troopAddFrd.Scene"
    override val traitString = arrayOf("checkDataRecmdRemarkList cacheInvalid_ts_type_troopUin=%b_%d_%d_%s")
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/") or DexKitFilter.defpackage
}

data object NCustomWidgetUtil_updateCustomNoteTxt : DexKitTarget.UsingStr() {
    // guess
    override val declaringClass = "com.tencent.widget.CustomWidgetUtil"
    override val traitString = arrayOf("^NEW$")
    override val filter = DexKitFilter.strInClsName("com/tencent/widget") or DexKitFilter.defpackage and DexKitFilter.notHasSuper
}

data object AIOTitleVB_updateLeftTopBack_NT : DexKitTarget.UsingStr() {
    // guess
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.aio.title.AIOTitleVB"
    override val traitString = arrayOf("99+")
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/aio/title/")
}

data object CCustomWidgetUtil_updateCustomNoteTxt_NT : DexKitTarget.UsingStr() {
    // guess
    override val declaringClass = "com.tencent.widget.CustomWidgetUtil"
    override val traitString = arrayOf("fixTextViewLayout wrong: params wrong")
    override val filter = DexKitFilter.strInClsName("com/tencent/qqnt/chats")
}

data object NPadUtil_initDeviceType : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.common.config.PadUtil"
    override val traitString = arrayOf("initDeviceType type = ")
    override val filter = DexKitFilter.allowAll
}

// TODO 待优化这几种类型
data object NTextItemBuilder_setETText : DexKitTarget.UsingDexkit() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/activity/aio/item/TextItemBuilder"
    override val filter = DexKitFilter.allowAll
}

data object NAIOPictureView_setVisibility : DexKitTarget.UsingDexkit() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/activity/aio/photo/AIOPictureView"
    override val filter = DexKitFilter.allowAll
}

data object NAIOPictureView_onDownloadOriginalPictureClick : DexKitTarget.UsingDexkit() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/activity/aio/photo/AIOPictureView"
    override val filter = DexKitFilter.allowAll
}

data object PaiYiPaiHandler_canSendReq : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com/tencent/mobileqq/paiyipai/PaiYiPaiHandler"
    override val traitString = arrayOf("pai_yi_pai_user_double_tap_timestamp_")
    override val filter = DexKitFilter.allowAll
}

data object TroopGuildChatPie_flingRToL : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.troop.guild.TroopGuildChatPie"
    override val traitString = arrayOf("[flingRToL] isMultiSelectState:")
    override val filter = DexKitFilter.allowAll
}

data object BaseListenTogetherPanel_onUIModuleNeedRefresh : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.listentogether.ui.BaseListenTogetherPanel"
    override val traitString = arrayOf("onUIModuleNeedRefresh, checkSession is false")
    override val filter = DexKitFilter.allowAll
}

data object EmotcationConstants : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = false
    override val declaringClass = "com.tencent.mobileqq.text.EmotcationConstants"
    override val traitString = arrayOf("setEmojiMap emoji's file is null")
    override val filter = DexKitFilter.allowAll
}

data object GroupSpecialCare_getCareTroopMemberMsgText : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.util.notification.NotifyIdManager"
    override val traitString = arrayOf("getCareTroopMemberMsgText: invoked.  troopMemberIsCared: ")
    override val filter = DexKitFilter.allowAll
}

data object ChatPanel_InitPanel_QQNT : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val traitString = arrayOf("funBtnLayout.findViewById(R.id.fun_btn)")
    override val declaringClass = "ChatPanel_InitPanel_QQNT"
    override val filter = DexKitFilter.allowAll
}

data object AIO_Create_QQNT : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val traitString = arrayOf("rootVMBuild")
    override val declaringClass = "AIO_Create_QQNT"
    override val filter = DexKitFilter.allowAll
}

data object AIO_Destroy_QQNT : DexKitTarget.UsingStringVector() {
    override val findMethod: Boolean = true
    override val traitStringVectors = arrayOf(arrayOf("ChatPie", "onDestroy "))
    override val declaringClass = "AIO_Create_QQNT"
    override val filter = DexKitFilter.strInClsName("Lcom/tencent/aio/base/chat/ChatPie;", true)
}

data object AIO_InputRootInit_QQNT : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val traitString = arrayOf("inputRoot.findViewById(R.id.send_btn)")
    override val declaringClass = ""
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/aio/input")
}

data object EmoMsgUtils_isSingleLottie_QQNT : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val traitString = arrayOf("is Valid EmojiFaceId")
    override val declaringClass = ""
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/aio/utils")
    // "com/tencent/guild/aio/util" 是频道的
}

data object Reply_At_QQNT : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val traitString = arrayOf("msgItem.msgRecord.senderUid")
    override val declaringClass = ""
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/aio/input")
}

data object TroopSendFile_QQNT : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val traitString = arrayOf("send local to troop file use nt, filePath:")
    override val declaringClass = ""
    override val filter = DexKitFilter.allowAll
}

data object TroopEnterEffect_QQNT : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val traitString = arrayOf("playAnimaions: isSimpleUISwitch = true")
    override val declaringClass = ""
    override val filter = DexKitFilter.allowAll
    // 理论上非NT也能用，但祖法不可变
}

data object NQZMoment_EntranceEnabled : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val traitString = arrayOf("KEY_OPEN_QZMOMENT_ENTRANCE")
    override val declaringClass = ""
    override val filter = DexKitFilter.strInClsName("com/qzone/reborn/qzmoment/util")
}

data object DefaultFileModel : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = false
    override val traitString = arrayOf("onVideoPlayerError : file entity is null")
    override val declaringClass = "com.tencent.mobileqq.filemanager.fileviewer.model.DefaultFileModel"
    override val filter = DexKitFilter.strInClsName("com.tencent.mobileqq.filemanager.fileviewer.model")
}

data object FileBrowserActivity_InnerClass_onItemClick : DexKitTarget.UsingStringVector() {
    override val findMethod: Boolean = true
    override val traitStringVectors: Array<Array<String>> = arrayOf(arrayOf("GeneralFileBrowserActivity", "reportShareActionSheetClick"))
    override val declaringClass = ""
    override val filter = DexKitFilter.strInClsName("FileBrowserActivity")
}

data object Multiforward_Avatar_setListener_NT : DexKitTarget.UsingDexkit() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.aio.msglist.holder.component.avatar.AIOAvatarContentComponent"
    override val filter = DexKitFilter.allowAll
}

data object AIOTextElementCtor : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.aio.msg.AIOMsgElement.AIOTextElementCtor"
    override val traitString = arrayOf("textElement")
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/aio/msg")
}

data object AIOPicElementType : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = false
    override val declaringClass = "com.tencent.mobileqq.aio.msg.AIOMsgElementType.PicElement"
    override val traitString = arrayOf("PicElement(origPath=")
    override val filter = DexKitFilter.strInClsName("com/tencent/qqnt/aio/")
}

data object MultiSelectToBottomIntent : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = false
    override val declaringClass = "com.tencent.mobileqq.aio.input.multiselect.c.toBottomIntent"
    override val traitString = arrayOf("SelectToBottom(dividingLineTop=")
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/aio/input/multiselect")
}

data object MultiSelectBarVM : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = false
    override val declaringClass = ""
    override val traitString = arrayOf("MultiSelectBarVM")
    override val filter = DexKitFilter.allowAll
}

data object AIOSendMsg : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val declaringClass = "com.tencent.mobileqq.aio.input.sendmsg.AIOSendMsgVMDelegate.sendMsg"
    override val traitString = arrayOf("[sendMsg] elements is empty")
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/aio/input/sendmsg/AIOSendMsgVMDelegate")
}

data object AIODelegate_ISwipeListener : DexKitTarget.UsingStringVector() {
    override val findMethod: Boolean = false
    override val declaringClass = "com.tencent.qqnt.aio.activity"
    override val traitStringVectors = arrayOf(arrayOf("aio_disappear_type", "close_aio"))
    override val filter = DexKitFilter.strInClsName("com/tencent/qqnt/aio/activity")
}

data object NT_SysAndEmojiResInfo : DexKitTarget.UsingStringVector() {
    override val findMethod: Boolean = false
    override val traitStringVectors = arrayOf(arrayOf("NT_SysAndEmojiResInfo", "reloadDrawable restartDownload:"))
    override val declaringClass = ""
    override val filter = DexKitFilter.allowAll
}

data object X5_Properties_conf : DexKitTarget.UsingStr() {
    override val traitString: Array<String> = arrayOf("setting_forceUseSystemWebview", "result_systemWebviewForceUsed", "debug.conf")
    override val declaringClass: String = "com.tencent.smtt.utils.LoadPropertiesUtils"
    override val filter = DexKitFilter.allowAll
}

data object EmotionDownloadEnableSwitch : DexKitTarget.UsingStringVector() {
    override val findMethod = true
    override val traitStringVectors = arrayOf(arrayOf("emotion_download_disable_8980_887036489", "…le_8980_887036489"))
    override val declaringClass: String = ""
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/emotionintegrate/")
}


data object QQ_SETTING_ME_CONFIG_CLASS : DexKitTarget.UsingStringVector() {
    override val findMethod: Boolean = false
    override val traitStringVectors: Array<Array<String>> = arrayOf(
        arrayOf(
            "超级QQ秀",
            "我的视频",
            "我的文件",
            "我的收藏",
            "我的相册",
            //"我的小游戏", // removed since 9.0.90
            "免流量",
            "我的个性装扮",
            "财富小金库",
            "我的QQ钱包",
            "开通会员",
            "我的小世界",
            //"直播" // removed since 9.0.90
        )
    )
    override val declaringClass: String = ""
    override val filter = DexKitFilter.allowAll
}

data object TextMsgItem_getText : DexKitTarget.UsingDexkit() {
    override val findMethod: Boolean = true
    override val declaringClass: String = "com.tencent.mobileqq.aio.msg.TextMsgItem"
    override val filter: dexkitFilter = DexKitFilter.allowAll
}

data object ChatSettingForTroop_InitUI_TIM : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val traitString: Array<String> = arrayOf("initUI: time = ")
    override val declaringClass: String = ""
    override val filter: dexkitFilter = DexKitFilter.strInClsName("ChatSettingForTroop")
}

data object FormItem_TIM : DexKitTarget.UsingStringVector() {
    override val findMethod: Boolean = false
    override val traitStringVectors: Array<Array<String>> = arrayOf(arrayOf("RobotMemberFormItem", "setRobotRedDot"))
    override val declaringClass: String = ""
    override val filter: dexkitFilter = DexKitFilter.allowAll
}

data object QQSettingMeABTestHelper_isZPlanExpGroup_New : DexKitTarget.UsingStringVector() {
    override val findMethod: Boolean = true
    override val traitStringVectors: Array<Array<String>> = arrayOf(arrayOf("isZPlanExpGroup: ", "QQSettingMeABTestHelper"))
    override val declaringClass: String = ""
    override val filter: dexkitFilter = DexKitFilter.allowAll
}

data object QQSettingMeABTestHelper_isZplanExpGroup_Old : DexKitTarget.UsingStringVector() {
    override val findMethod: Boolean = true
    override val traitStringVectors: Array<Array<String>> = arrayOf(arrayOf("isZplanExpGroup: ", "QQSettingMeABTestHelper"))
    override val declaringClass: String = ""
    override val filter: dexkitFilter = DexKitFilter.allowAll
}

data object QQSettingMeABTestHelper_isV9ExpGroup : DexKitTarget.UsingStringVector() {
    override val findMethod: Boolean = true
    override val traitStringVectors: Array<Array<String>> = arrayOf(arrayOf("isV9ExpGroup: ", "QQSettingMeABTestHelper"))
    override val declaringClass: String = ""
    override val filter: dexkitFilter = DexKitFilter.allowAll
}

data object QQValueMethod : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val traitString: Array<String> = arrayOf("能量值:")
    override val declaringClass: String = "com/tencent/mobileqq/vas/qqvaluecard/view/QQValuePagView"
    override val filter: dexkitFilter = DexKitFilter.allowAll
}

data object QZoneFeedxTopEntranceMethod : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val traitString: Array<String> = arrayOf("findViewById(R.id.qzone_feedx_top_entrance_view)")
    override val declaringClass: String = "com/qzone/reborn/feedx/widget/entrance/QZoneFeedxTopEntranceManagerView"
    override val filter: dexkitFilter = DexKitFilter.allowAll
}

data object CopyPromptHelper_handlePrompt : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val traitString: Array<String> = arrayOf("handlePrompt content : ")
    override val declaringClass: String = ""
    override val filter: dexkitFilter = DexKitFilter.allowAll
    // There may be at most 4 strings, but they should be in the same method
}

data object PushNotificationManager_judgeAndAddGrayTips : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val traitString: Array<String> = arrayOf("getmTodayHadShowCount > showCount")
    override val declaringClass: String = "com/tencent/mobileqq/managers/PushNotificationManager"
    override val filter: dexkitFilter = DexKitFilter.allowAll
    // only one result expected
}

data object RecentPopup_onClickAction : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = true
    override val traitString: Array<String> = arrayOf("jiahao.fukuan.click")
    override val declaringClass = ""
    override val filter: dexkitFilter = DexKitFilter.allowAll
}

data object TroopInfoCardPageABConfig : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = false
    override val traitString: Array<String> = arrayOf("enableNewPageFromTroopSettingSwitch=")
    override val declaringClass = ""
    override val filter: dexkitFilter = DexKitFilter.allowAll
}

data object PlusPanel_PanelAdapter : DexKitTarget.UsingStr() {
    override val findMethod: Boolean = false
    override val traitString: Array<String> = arrayOf("appDataLists.subList(startIndex, endIndex)")
    override val declaringClass = ""
    override val filter: dexkitFilter = DexKitFilter.allowAll
}

data object Hd_FakePhone_Method : DexKitTarget.UsingStringVector() {
    override val findMethod = true
    override val traitStringVectors = arrayOf(arrayOf("status", "wording", "target_desc", "target_name"))
    override val declaringClass = ""
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/app/")
}

data object Hd_RemoveRedPackSkin_Class : DexKitTarget.UsingStr() {
    override val findMethod = false
    override val traitString = arrayOf("红包封皮")
    override val declaringClass = ""
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/qwallet/hb/panel/")
}

data object Hd_HandleQQSomeFunExit_fixFileView_Method : DexKitTarget.UsingStr() {
    override val findMethod = true
    override val traitString = arrayOf("(fileElement.fileSize)")
    override val declaringClass = "Lcom/tencent/mobileqq/aio/msglist/holder/component/file/AIOFileViewer"
    override val filter = DexKitFilter.allowAll
}

data object Hd_AutoSendOriginalPhoto_guildPicker_Method : DexKitTarget.UsingStr() {
    override val findMethod = true
    override val traitString = arrayOf("isRaw")
    override val declaringClass = "Lcom/tencent/qqnt/qbasealbum/album/view/PickerBottomBarPart"
    override val filter = DexKitFilter.strInClsName("com/tencent/qqnt/qbasealbum/album/view/")
}

data object Hd_AutoSendOriginalPhoto_photoListPanel_Method : DexKitTarget.UsingStr() {
    override val findMethod = true
    override val traitString = arrayOf("resetStatus selectSize:")
    override val declaringClass = "Lcom/tencent/mobileqq/activity/aio/photo/PhotoListPanel"
    override val filter = DexKitFilter.allowAll
}

data object Hd_DisableGrowHalfLayer_Method1 : DexKitTarget.UsingStringVector() {
    override val findMethod = true
    override val traitStringVectors = arrayOf(arrayOf("grow_half_layer_info", "grow_half_layer_tech_info"))
    override val declaringClass = "cooperation.vip.ad.GrowHalfLayerHelper"
    override val filter = DexKitFilter.strInClsName("cooperation/vip/ad/") and filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        m.returnType == Void.TYPE && m.paramCount == 3
    }
}

data object Hd_DisableGrowHalfLayer_Method2 : DexKitTarget.UsingStr() {
    override val findMethod = true
    override val traitString = arrayOf("start showVasADBanner")
    override val declaringClass = "cooperation.vip.qqbanner.manager.VasADBannerManager"
    override val filter = DexKitFilter.allowAll
}

data object Hd_GagInfoDisclosure_Method : DexKitTarget.UsingStr() {
    override val findMethod = true
    override val traitString = arrayOf("<---0x2dc push  groupCode:")
    override val declaringClass = "com.tencent.imcore.message"
    override val filter = DexKitFilter.strInClsName("com/tencent/imcore/message/")
}

data object OriginalPhotoNT_onInitView : DexKitTarget.UsingDexkit() {
    override val findMethod: Boolean = true
    override val declaringClass = ""
    override val filter: dexkitFilter = DexKitFilter.allowAll
}

data object RemoveAudioTransitionMethod : DexKitTarget.UsingStr() {
    override val findMethod = true
    override val traitString = arrayOf("getDrawable onCompositionLoaded lottieComposition is null or mIsDestroyed")
    override val declaringClass = "Lcom/tencent/mobileqq/activity/aio/audiopanel/AudioTransitionAnimManager;"
    override val filter = DexKitFilter.allowAll
}

data object Hd_HideShortcutBar_Method_TroopApp : DexKitTarget.UsingStr() {
    override val findMethod = true
    override val traitString = arrayOf("isShortcutBarVisibleOrGoingToBeVisible return false for AIOIceBreakViewShowing")
    override val declaringClass = "Lcom/tencent/mobileqq/activity/aio/helper/TroopAppShortcutBarHelper;"
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/activity/aio/helper/")
}

data object Hd_HideShortcutBar_Method_Troop : DexKitTarget.UsingStr() {
    override val findMethod = true
    override val traitString = arrayOf(",isShowingCustomShortcut:")
    override val declaringClass = "Lcom/tencent/mobileqq/troop/shortcut/aio/TroopShortcutVB;"
    override val filter = DexKitFilter.allowAll
}

data object UnlockTroopNameLimitClass : DexKitTarget.UsingStr() {
    override val findMethod = false
    override val traitString = arrayOf("[☀-⟿]")
    override val declaringClass = "Lcom/tencent/mobileqq/activity/editservice/EditTroopMemberNickService\$EmojiFilter;"
    override val filter = DexKitFilter.strInClsName("com/tencent/mobileqq/activity/editservice/EditTroopMemberNickService")
}

data object DisableLightInteractionMethod : DexKitTarget.UsingStr() {
    override val findMethod = true
    override val traitString = arrayOf("em_bas_shortcut_bar_above_c2c_input_box")
    override val declaringClass = "Lcom/tencent/mobileqq/aio/bottombar/c2c/LiteActionBottomBar;"
    override val filter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(getHostClassLoader()) }.getOrNull() ?: return@filter false
        m.returnType == View::class.java && m.paramCount == 2 && m.parameterTypes[1] == ViewGroup::class.java
    }
}

data object Hd_HideTipsBar_Method : DexKitTarget.UsingStr() {
    override val findMethod = true
    override val traitString = arrayOf("initBannerView | banner = ")
    override val declaringClass = "Lcom/tencent/mobileqq/banner/BannerManager;"
    override val filter = DexKitFilter.allowAll
}

data object Hd_DisableFekitToAppDialog_Method : DexKitTarget.UsingStringVector() {
    override val findMethod = true
    override val traitStringVectors = arrayOf(arrayOf("DTAPIImpl.FekitToApp", "onSecDispatchToAppEvent show dialog"))
    override val declaringClass = "Lcom/tencent/mobileqq/dt/api/impl/DTAPIImpl;"
    override val filter = DexKitFilter.allowAll
}