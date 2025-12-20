package com.likejson.hook


import android.app.Activity
import android.content.pm.PackageManager
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedHelpers
import java.util.Arrays

@FunctionHookEntry
@UiItemAgentEntry
object PermissionMaskingHook : CommonSwitchFunctionHook() {
    override val name = "伪装权限"
    override val description = "让QQ认为自己获取了所有权限\n配合 使用系统相册/文件 使用"
    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY
    override fun initOnce(): Boolean {
        // beforeHookIfEnabled 会报 NoSuchMethod 拼尽全力无法战胜，反正现在代码能跑（）
        XposedHelpers.findAndHookMethod(
            Activity::class.java,
            "onRequestPermissionsResult",
            Int::class.javaPrimitiveType,
            Array<String>::class.java,
            IntArray::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    val permissions = param.args[1] as Array<String?>
                    val grantResults = param.args[2] as IntArray
                    for (i in permissions.indices) {
                        grantResults[i] = PackageManager.PERMISSION_GRANTED
                    }
                    val modifiedResults = IntArray(grantResults.size)
                    Arrays.fill(modifiedResults, PackageManager.PERMISSION_GRANTED)
                    param.args[2] = modifiedResults
                }
            }
        )
        return true
    }

}


