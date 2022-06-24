/*
 * QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 dmca@ioctl.cc
 * https://github.com/ferredoxin/QNotified
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/ferredoxin/QNotified/blob/master/LICENSE.md>.
 */

package cc.chenhe.qqnotifyevo.utils

import androidx.annotation.IntDef
import java.util.concurrent.TimeUnit

const val ICON_AUTO = 0
const val ICON_QQ = 1

@Retention(AnnotationRetention.SOURCE)
@IntDef(ICON_AUTO, ICON_QQ)
annotation class Icon

@Icon
fun getIconMode(): Int = ICON_AUTO

fun showSpecialPrefix(): Boolean = true

/**
 * 特别关注的群消息通知渠道。
 *
 * @return `true` 为特别关心渠道，`false` 为群消息渠道。
 */
fun specialGroupMsgChannel(): Boolean =  false

fun getAvatarCachePeriod(): Long = TimeUnit.DAYS.toMillis(1)
