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
package cc.ioctl.hook.ui.profile

import android.app.Activity
import android.content.Intent
import cc.ioctl.util.HookUtils
import cc.ioctl.util.HostInfo
import cc.ioctl.util.Reflex
import com.github.kyuubiran.ezxhelper.utils.field
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.NVasProfileTemplateController_onCardUpdate
import io.github.qauxv.util.isTim
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.get
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.method
import xyz.nextalone.util.set
import xyz.nextalone.util.throwOrTrue
import java.lang.reflect.Method
import java.lang.reflect.Modifier

@FunctionHookEntry
@UiItemAgentEntry
object RemoveDiyCard : CommonSwitchFunctionHook(
    "kr_remove_diy_card",
    arrayOf(NVasProfileTemplateController_onCardUpdate)
) {

    override val name = "屏蔽 DIY 名片"

    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_PROFILE

    override fun initOnce() = throwOrTrue {
        DexKit.requireMethodFromCache(NVasProfileTemplateController_onCardUpdate)
            .hookBefore(this) {
                when (it.thisObject) {
                    is Activity -> {
                        val card = it.args[0]
                        copeCard(card)
                    }

                    else -> {
                        if (requireMinQQVersion(QQVersion.QQ_8_6_0)) {
                            val card = it.args[1].get("card")
                            copeCard(card!!)
                            return@hookBefore
                        }
                        it.result = true
                    }
                }
            }
        for (m in Initiator._FriendProfileCardActivity().declaredMethods) {
            val argt = m.parameterTypes
            if (HostInfo.getVersionCode32() <= QQVersion.QQ_8_3_6) {
                if (m.name == "a" && !Modifier.isStatic(m.modifiers) && m.returnType == Void.TYPE) {
                    if (argt.size != 2) {
                        continue
                    }
                    if (argt[1] != Boolean::class.javaPrimitiveType) {
                        continue
                    }
                    if (argt[0].superclass != Any::class.java) {
                        continue
                    }
                } else {
                    continue
                }
            } else {
                if (m.name == "b" && !Modifier.isStatic(m.modifiers) && m.returnType == Void.TYPE) {
                    if (argt.size != 1) {
                        continue
                    }
                    if (argt[0].superclass == Intent::class.java) {
                        continue
                    }
                    if (argt[0].superclass != Any::class.java) {
                        continue
                    }
                } else {
                    continue
                }
            }
            HookUtils.hookBeforeIfEnabled(this, m) { param ->
                val _ProfileCardInfo = (param.method as Method).parameterTypes[0]
                val info = Reflex.getInstanceObjectOrNull(param.thisObject, "a", _ProfileCardInfo)
                if (info != null) {
                    val _Card = Initiator.loadClass("com.tencent.mobileqq.data.Card")
                    val card = Reflex.getInstanceObjectOrNull(info, "a", _Card)
                    if (card != null) {
                        val f = _Card.getField("lCurrentStyleId")
                        if (f.getLong(card) == 22L || f.getLong(card) == 21L) {
                            f.setLong(card, 0)
                        }
                    } else {
                        Log.e("IgnoreDiyCard/W but info.<Card> == null")
                    }
                } else {
                    Log.e("IgnoreDiyCard/W but info == null")
                }
            }
        }

        // 上面是旧的代码，不报错就不去动他
        try {
            "Lcom/tencent/mobileqq/profilecard/processor/TempProfileBusinessProcessor;->updateCardTemplate(Lcom/tencent/mobileqq/data/Card;Ljava/lang/String;LSummaryCardTaf/SSummaryCardRsp;)V".method.hookAfter { param ->
                val card = param.args[0]
                card.field("lCurrentStyleId", false, Long::class.java).set(card, 0L)
            }
        } catch (e: Exception) {
            Log.e(e)
        }
    }

    private fun copeCard(card: Any) {
        val id = card.get("lCurrentStyleId", Long::class.java)
        if ((21L == id) or (22L == id))
            card.set("lCurrentStyleId", 0)
    }

    override val isAvailable: Boolean get() = !isTim()
}
