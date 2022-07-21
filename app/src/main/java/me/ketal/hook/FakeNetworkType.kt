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

@file:Suppress("DEPRECATION")

package me.ketal.hook

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.method
import xyz.nextalone.util.replace
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object FakeNetworkType : CommonSwitchFunctionHook() {

    override val name = "伪装网络类型为移动网络"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MISC_CATEGORY

    override fun initOnce() = throwOrTrue {
        ConnectivityManager::class.java.method("isActiveNetworkMetered")?.replace(this, true)

        NetworkInfo::class.java.method("getType")?.replace(this, ConnectivityManager.TYPE_MOBILE)

        NetworkCapabilities::class.java.method("hasTransport")?.hookBefore(this) {
            when (it.args[0] as Int) {
                NetworkCapabilities.TRANSPORT_WIFI -> {
                    it.result = false
                }
                NetworkCapabilities.NET_CAPABILITY_INTERNET -> {
                    it.result = true
                }
            }
        }
    }
}
