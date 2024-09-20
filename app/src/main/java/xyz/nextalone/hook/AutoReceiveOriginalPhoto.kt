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
import com.github.kyuubiran.ezxhelper.utils.hookAfter
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
import io.github.qauxv.util.dexkit.OriginalPhotoNT_onInitView
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.requireMinVersion
import org.luckypray.dexkit.result.ClassData
import xyz.nextalone.util.invoke
import xyz.nextalone.util.replace
import java.lang.reflect.Modifier

@FunctionHookEntry
@UiItemAgentEntry
object AutoReceiveOriginalPhoto : CommonSwitchFunctionHook(
    SyncUtils.PROC_ANY,
    arrayOf(CAIOPictureView)
), DexKitFinder {

    override val name: String = "聊天自动接收原图"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

    override fun initOnce(): Boolean {
        if (requireMinQQVersion(QQVersion.QQ_8_9_63_BETA_11345)) {
//            Initiator.loadClass("com.tencent.qqnt.aio.gallery.part.d").declaredMethods.single { method ->
            DexKit.requireMethodFromCache(OriginalPhotoNT_onInitView).declaringClass.declaredMethods.single { method ->
                val params = method.parameterTypes
                params.size == 1 && params[0] == Int::class.java
            }.hookAfter {
                if (it.args[0] == 0) {
                    it.thisObject.invoke("loadOriginImageInner")
                    val listener = it.thisObject.invoke("getMLayerOperateListener")
                    listener!!.invoke("clickShowOriginPicBtn")
                }
            }
            return true
        }
        val kAIOPictureView = DexKit.requireClassFromCache(CAIOPictureView)
        val onDownloadOriginalPictureClick = DexKit.loadMethodFromCache(NAIOPictureView_onDownloadOriginalPictureClick)!!
        val setVisibility = DexKit.requireMethodFromCache(NAIOPictureView_setVisibility)
        setVisibility.replace(this) {
            if (it.args[0] as Boolean) {
                it.thisObject.invoke(onDownloadOriginalPictureClick.name)
            }
        }
        return true
    }

    override val isAvailable: Boolean
        get() = requireMinVersion(
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
        get() = NAIOPictureView_onDownloadOriginalPictureClick.descCache == null || (requireMinQQVersion(QQVersion.QQ_8_9_63_BETA_11345) && OriginalPhotoNT_onInitView.descCache == null)

    override fun doFind(): Boolean {
        getCurrentBackend().use { backend ->
            val dexKit = backend.getDexKitBridge()
            if (requireMinQQVersion(QQVersion.QQ_8_9_63_BETA_11345)) {
                dexKit.findMethod {
                    matcher {
                        name = "onInitView"
                        usingStrings("rootView", "em_bas_view_the_original_picture")
                    }
                }.firstOrNull()?.let {
                    OriginalPhotoNT_onInitView.descCache = it.descriptor
                } ?: return false
            }
            var kAIOPictureView = DexKit.loadClassFromCache(CAIOPictureView)
            if (kAIOPictureView == null) {
                val clazzList = mutableListOf<ClassData>().apply {
                    dexKit.batchFindClassUsingStrings {
                        addSearchGroup("1", listOf("AIOPictureView", "0X800A91E"))
                        addSearchGroup("2", listOf("AIOGalleryPicView", "0X800A91E"))
                    }.values.forEach {
                        addAll(it)
                    }
                }
                Log.d("clazz: $clazzList")
                if (clazzList.size != 1) {
                    return false
                }
                kAIOPictureView = clazzList[0].getInstance(getHostClassLoader())
                // here a method descriptor is used to cache the class descriptor, not a class name
                CAIOPictureView.descCache = getTypeSig(kAIOPictureView) + "-><clinit>()V"
            }
            Log.d("kAIOPictureView: ${kAIOPictureView.name}")
            val onClickMethod = dexKit.findMethod {
                matcher {
                    modifiers = Modifier.PUBLIC
                    declaredClass = kAIOPictureView.name
                    returnType = "void"
                    paramTypes(*arrayOf<Class<*>>())
                    addCall {
                        declaredClass = kAIOPictureView.name
                        name = "onClick"
                    }
                    addInvoke {
                        returnType = "void"
                        paramTypes("long", "int", "int")
                    }
                }
            }.firstOrNull() ?: return false
            Log.d("save: ${onClickMethod.descriptor}")
            NAIOPictureView_onDownloadOriginalPictureClick.descCache = onClickMethod.descriptor

            val setVisibilityMethods = dexKit.findMethod {
                matcher {
                    declaredClass = kAIOPictureView.name
                    returnType = "void"
                    paramTypes("boolean")
                    addInvoke("Landroid/widget/TextView;->setVisibility(I)V")
                }
            }
            Log.d("setVisibilityMethods: $setVisibilityMethods")
            if (setVisibilityMethods.size != 1) {
                return false
            }
            NAIOPictureView_setVisibility.descCache = setVisibilityMethods.first().descriptor
            return true
        }
    }
}
