/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package io.github.duzhaokun123.util

import android.os.Parcelable
import xyz.nextalone.util.clazz
import xyz.nextalone.util.set

object AlbumResultUtils {
    fun create(path: String): Parcelable {
        val class_LocalMediaInfo = "Lcom/tencent/qqnt/qbasealbum/model/LocalMediaInfo;".clazz!!
        val localMediaInfo = class_LocalMediaInfo.newInstance()
        localMediaInfo.set("_id", -1L)
        localMediaInfo.set("path", path)

        val class_AlbumResult = "Lcom/tencent/qqnt/qbasealbum/model/AlbumResult;".clazz!!
        val albumResult = class_AlbumResult.newInstance()
        albumResult.set("d", listOf(localMediaInfo)) // selectList
        albumResult.set("e", true) // needSend
        return albumResult as Parcelable
    }
}