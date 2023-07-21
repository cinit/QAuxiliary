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

package io.github.qauxv.router.decorator

import android.content.Context
import android.os.Parcelable
import android.view.ViewGroup
import mqq.app.AppRuntime

interface IBaseChatPieInitDecorator : IBaseChatPieDecorator {
    @Throws(Throwable::class)
    /**
     * On NT Version, param session is null, get it by calling InputButtonHookDispatcher.INSTANCE.getSessionByAIOParam().
     * DO NOT call it right after onInitBaseChatPie, for it is not initialized before entering an AIO.
     */
    fun onInitBaseChatPie(baseChatPie: Any, aioRootView: ViewGroup, session: Parcelable?, ctx: Context, rt: AppRuntime)
}
