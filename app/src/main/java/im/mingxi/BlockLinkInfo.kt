package im.mingxi

import cc.ioctl.util.hookBeforeIfEnabled
import com.github.kyuubiran.ezxhelper.utils.paramCount
import com.tencent.qqnt.kernel.nativeinterface.LinkInfo
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge

@FunctionHookEntry
@UiItemAgentEntry
object BlockLinkInfo : CommonSwitchFunctionHook() {

    override val name = "屏蔽链接信息"

    override val description = "去除链接下方的信息卡片，也可以用来防耗流量链接消息"

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

    override fun initOnce(): Boolean {
        val linkInfoClass = Initiator.loadClass("com.tencent.qqnt.kernel.nativeinterface.LinkInfo")
        XposedBridge.hookAllConstructors(linkInfoClass, object: XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = null
            }
        })

        val msgExtClass = Initiator.loadClass("com.tencent.qqnt.msg.MsgExtKt")
        hookBeforeIfEnabled(msgExtClass.declaredMethods.single {
            it.name == (if (requireMinQQVersion(QQVersion.QQ_9_1_70)) "S" else "T")
        }) { it.result = false }

        val txtMsgClass = Initiator.loadClass("com.tencent.mobileqq.aio.msg.TextMsgContent")
        hookBeforeIfEnabled(txtMsgClass.declaredMethods.first {
            it.returnType == Void.TYPE && it.paramCount > 1 && it.parameterTypes[1] == LinkInfo::class.java
        }) { it.result = null }

        return true
    }
}
