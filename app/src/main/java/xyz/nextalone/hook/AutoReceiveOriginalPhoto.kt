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
package xyz.nextalone.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.step.Step
import io.github.qauxv.util.Initiator.getHostClassLoader
import io.github.qauxv.util.PlayQQVersion
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.dexkit.CAIOPictureView
import io.github.qauxv.util.dexkit.DexDeobfsProvider.getCurrentBackend
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.DexKitFinder
import io.github.qauxv.util.dexkit.DexMethodDescriptor.getTypeSig
import io.github.qauxv.util.dexkit.NAIOPictureView_onDownloadOriginalPictureClick
import io.github.qauxv.util.dexkit.NAIOPictureView_setVisibility
import io.github.qauxv.util.dexkit.impl.DexKitDeobfs
import io.github.qauxv.util.requireMinVersion
import io.luckypray.dexkit.descriptor.member.DexClassDescriptor
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor
import xyz.nextalone.util.invoke
import xyz.nextalone.util.isPublic
import xyz.nextalone.util.replace
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object AutoReceiveOriginalPhoto : CommonSwitchFunctionHook(
    SyncUtils.PROC_PEAK,
    arrayOf(CAIOPictureView)
), DexKitFinder {

    override val name: String = "聊天自动接收原图"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

    override fun initOnce() = throwOrTrue {
        Log.d("AutoReceiveOriginalPhoto initOnce")
        val kAIOPictureView = DexKit.loadClassFromCache(CAIOPictureView)!!
        val onDownloadOriginalPictureClick = DexKit.loadMethodFromCache(NAIOPictureView_onDownloadOriginalPictureClick)!!
        val setVisibility = DexKit.loadMethodFromCache(NAIOPictureView_setVisibility)!!
        setVisibility.replace(this) {
            if (it.args[0] as Boolean) {
                it.thisObject.invoke(onDownloadOriginalPictureClick.name)
            }
        }
    }

    override val isAvailable: Boolean get() = requireMinVersion(
        QQVersionCode = QQVersion.QQ_8_3_5,
        PlayQQVersionCode = PlayQQVersion.PlayQQ_8_2_11
    )

    private val mStep: Step = object : Step {
        override fun step(): Boolean {
            return doFind()
        }

        override fun isDone(): Boolean {
            return !isNeedFind
        }

        override fun getPriority(): Int {
            return 0
        }

        override fun getDescription(): String {
            return "聊天自动接收原图相关类查找中"
        }
    }

    override fun makePreparationSteps(): Array<Step> {
        return arrayOf(mStep)
    }

    override val isNeedFind: Boolean
        get() = NAIOPictureView_onDownloadOriginalPictureClick.descCache == null

    override fun doFind(): Boolean {
        val deobfs = getCurrentBackend() as? DexKitDeobfs ?: return false
        val dexKit = deobfs.getDexKitBridge()
        var kAIOPictureView = DexKit.loadClassFromCache(CAIOPictureView)
        if (kAIOPictureView == null) {
            val clazzList = mutableListOf<DexClassDescriptor>().apply {
                dexKit.batchFindClassesUsingStrings {
                    addQuery("1", setOf("AIOPictureView", "0X800A91E"))
                    addQuery("2", setOf("AIOGalleryPicView", "0X800A91E"))
                }.values.forEach {
                    addAll(it)
                }
            }
            Log.d("clazz: $clazzList")
            if (clazzList.size != 1) {
                return false
            }
            kAIOPictureView = clazzList[0].getClassInstance(getHostClassLoader())
            // here a method descriptor is used to cache the class descriptor, not a class name
            CAIOPictureView.descCache = getTypeSig(kAIOPictureView) + "-><clinit>()V"
        }
        Log.d("kAIOPictureView: ${kAIOPictureView.name}")
        val onClickInvokingMethods = dexKit.findMethodInvoking {
            methodDeclareClass = kAIOPictureView.name
            methodName = "onClick"
            beInvokedMethodDeclareClass = kAIOPictureView.name
            beInvokedMethodReturnType = "V"
            beInvokedMethodParameterTypes = emptyArray()
        }
        Log.d("onClickInvokingMethods: $onClickInvokingMethods")
        if (onClickInvokingMethods.size != 1) {
            return false
        }
        val calledMethods = onClickInvokingMethods.values.first().toSet()
        Log.d("calledMethods: $calledMethods")
        val invokingMethods = dexKit.findMethodInvoking {
            methodDeclareClass = kAIOPictureView.name
            methodReturnType = "V"
            methodParameterTypes = emptyArray()
            beInvokedMethodReturnType = "V"
            beInvokedMethodParameterTypes = arrayOf("J", "I", "I")
        }.map { it.key }.filter { calledMethods.contains(it) }
        Log.d("invokingMethods: $invokingMethods")
        if (invokingMethods.size == 1) {
            NAIOPictureView_onDownloadOriginalPictureClick.descCache = invokingMethods.first().descriptor
        } else {
            val filterMethods = invokingMethods
                .map { it.getMethodInstance(getHostClassLoader()) }
                .filter { it.isPublic }
            if (filterMethods.size != 1) {
                return false
            }
            Log.d("save: ${filterMethods.first()}")
            NAIOPictureView_onDownloadOriginalPictureClick.descCache = DexMethodDescriptor(filterMethods.first()).descriptor
        }
        val setVisibilityMethods = dexKit.findMethodCaller {
            methodDescriptor = "Landroid/widget/TextView;->setVisibility(I)V"
            callerMethodDeclareClass = kAIOPictureView.name
            callerMethodReturnType = "V"
            callerMethodParameterTypes = arrayOf("Z")
        }
        Log.d("setVisibilityMethods: $setVisibilityMethods")
        if (setVisibilityMethods.size != 1) {
            return false
        }
        NAIOPictureView_setVisibility.descCache = setVisibilityMethods.first().descriptor
        return true
    }
}
