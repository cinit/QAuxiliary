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

package me.singleneuron.hook

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import com.github.kyuubiran.ezxhelper.utils.tryOrFalse
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XC_MethodReplacement
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.util.xpcompat.XposedHelpers
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.hostInfo
import xyz.nextalone.util.clazz
import xyz.nextalone.util.throwOrTrue
import java.util.concurrent.atomic.AtomicBoolean

//https://github.com/F-19-F/justpush

private const val BRD_CLOSE_SOCKET = "BRD_CLOSE_SOCKET"
private const val BRD_RELEASE_SOCKET = "BRD_RELEASE_SOCKET"

@FunctionHookEntry
@UiItemAgentEntry
object JustPush : CommonSwitchFunctionHook(targetProc = SyncUtils.PROC_ANY) {

    private var toClose: AtomicBoolean? = null
    private var sockets: ArrayList<Any>? = null

    private val isMsf: Boolean
        get() = SyncUtils.isTargetProcess(SyncUtils.PROC_MSF)

    override val name: String = "Just Push"

    override val description: CharSequence = "让QQ收到国内厂商推送时不拉起QQ自身，仅运行通知必要的服务"

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Auxiliary.NOTIFICATION_CATEGORY

    override fun initOnce(): Boolean {
        return throwOrTrue {
            hookServices()
            sockets = ArrayList()
            toClose = AtomicBoolean(false)
            XposedBridge.hookAllConstructors("java.net.Socket".clazz, object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    tryOrFalse {
                        if (toClose?.get() == true) {
                            XposedHelpers.callMethod(param.thisObject, "close")
                            return
                        }
                        if (isMsf) {
                            sockets?.add(param.thisObject)
                        }
                    }
                }
            })
            XposedBridge.hookAllConstructors("java.net.DatagramSocket".clazz, object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    tryOrFalse {
                        if (toClose?.get() == true) {
                            XposedHelpers.callMethod(param.thisObject, "close")
                            return
                        }
                        if (isMsf) {
                            sockets?.add(param.thisObject)
                        }
                    }
                }
            })
            XposedHelpers.findAndHookMethod("com.tencent.mobileqq.activity.SplashActivity".clazz, "doOnResume", object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    hostInfo.application.sendBroadcast(Intent(BRD_RELEASE_SOCKET))
                }
            })
            XposedHelpers.findAndHookMethod("com.tencent.mobileqq.msf.service.MsfService".clazz, "onCreate", object : XC_MethodHook() {
                @SuppressLint("UnspecifiedRegisterReceiverFlag")
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    val intentFilter = IntentFilter()
                    intentFilter.addAction("mqq.intent.action.QQ_BACKGROUND")
                    intentFilter.addAction("mqq.intent.action.QQ_FOREGROUND")
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            when (intent.action) {
                                "mqq.intent.action.QQ_BACKGROUND" -> context.sendBroadcast(Intent(BRD_CLOSE_SOCKET))
                                "mqq.intent.action.QQ_FOREGROUND" -> context.sendBroadcast(Intent(BRD_RELEASE_SOCKET))
                            }
                        }
                    }
                    // Must not use ContextImpl.registerReceiver here.
                    // QQ do not have $packageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        hostInfo.application.registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
                    } else {
                        hostInfo.application.registerReceiver(
                            receiver, intentFilter,
                            SyncUtils.getDynamicReceiverNotExportedPermission(hostInfo.application), null
                        )
                    }
                }
            })
        }
    }

    private fun hookServices() {
        XposedHelpers.findAndHookMethod("com.tencent.mobileqq.qfix.QFixApplication".clazz, "onCreate", object : XC_MethodReplacement() {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            @Throws(Throwable::class)
            override fun replaceHookedMethod(param: MethodHookParam): Any? {
//              进程名为推送服务时不拉起QQ主进程
                if (SyncUtils.getProcessName().endsWith("pushservice")) {
                    return null
                }
                if (isMsf) {
                    val handlerThread = HandlerThread("CloseSocket")
                    handlerThread.start()
                    val handler = Handler(handlerThread.looper)
                    val intentFilter = IntentFilter()
                    intentFilter.addAction(BRD_CLOSE_SOCKET)
                    intentFilter.addAction(BRD_RELEASE_SOCKET)
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            when (intent.action) {
                                BRD_CLOSE_SOCKET -> {
                                    toClose!!.set(true)
                                    for (socket in sockets!!) {
//                                    Log.d("QQ_BRD","close socket");
                                        handler.post { XposedHelpers.callMethod(socket, "close") }
                                    }
                                    sockets!!.clear()
                                }
                                BRD_RELEASE_SOCKET -> toClose!!.set(false)
                            }
                        }
                    }
                    // Must not use ContextImpl.registerReceiver here.
                    // QQ do not have $packageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        hostInfo.application.registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
                    } else {
                        hostInfo.application.registerReceiver(
                            receiver,
                            intentFilter,
                            SyncUtils.getDynamicReceiverNotExportedPermission(hostInfo.application),
                            null
                        )
                    }
                }
                return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
            }
        })
    }

}
