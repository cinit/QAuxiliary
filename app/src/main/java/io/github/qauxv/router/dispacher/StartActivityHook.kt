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
package io.github.qauxv.router.dispacher

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import com.github.kyuubiran.ezxhelper.utils.getStaticObject
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.hook.BaseHookDispatcher
import io.github.qauxv.router.decorator.IStartActivityHookDecorator
import io.github.qauxv.BuildConfig
import io.github.qauxv.base.annotation.EntityAgentEntry
import me.singleneuron.hook.decorator.DisableQzoneSlideCamera
import me.singleneuron.hook.decorator.ForceSystemAlbum
import me.singleneuron.hook.decorator.ForceSystemFile
import me.singleneuron.hook.decorator.FxxkQQBrowser

@EntityAgentEntry
@FunctionHookEntry
object StartActivityHook : BaseHookDispatcher<IStartActivityHookDecorator>(null) {

    // add start activity hooks here
    override val decorators: Array<IStartActivityHookDecorator>
        get() {
            val ret: Array<IStartActivityHookDecorator> = arrayOf(
                DisableQzoneSlideCamera,
                FxxkQQBrowser,
                ForceSystemAlbum,
                ForceSystemFile
            )
            return if (BuildConfig.DEBUG) {
                val debugDump = StartActivityHook::class.java.classLoader.loadClass("hook.DebugDump")
                    .getStaticObject("INSTANCE") as IStartActivityHookDecorator
                ret + debugDump
            } else ret
        }

    override val targetProcesses = SyncUtils.PROC_ANY

    @Throws(Throwable::class)
    override fun initOnce(): Boolean {
        //dump startActivity
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                // @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
                // public void startActivityForResult(String who, Intent intent, int requestCode, @Nullable Bundle options);
                val intent: Intent = if (param.args[0] is Intent) {
                    param.args[0] as Intent
                } else {
                    param.args[1] as Intent
                }
                for (decorator in decorators) {
                    try {
                        if (decorator.isEnabled && decorator.onStartActivityIntent(intent, param)) {
                            return
                        }
                    } catch (e: Throwable) {
                        decorator.traceError(e)
                    }
                }
            }
        }
        XposedBridge.hookAllMethods(ContextWrapper::class.java, "startActivity", hook)
        XposedBridge.hookAllMethods(ContextWrapper::class.java, "startActivityForResult", hook)
        XposedBridge.hookAllMethods(Activity::class.java, "startActivity", hook)
        XposedBridge.hookAllMethods(Activity::class.java, "startActivityForResult", hook)
        return true
    }
}
