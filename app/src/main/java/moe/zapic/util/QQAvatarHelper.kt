/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package moe.zapic.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.LruCache
import androidx.core.graphics.drawable.IconCompat
import io.github.qauxv.bridge.FaceImpl
import io.github.qauxv.util.hostInfo
import xyz.nextalone.util.clazz
import java.io.File

class QQAvatarHelper {

    private val toMD5Method = "com.tencent.qphone.base.util.MD5".clazz!!.getMethod("toMD5", String::class.java)
    private val avatarCache: LruCache<String, IconCompat> = LruCache(50)

    private val avatarCachePath = File(
            hostInfo.application.getExternalFilesDir(null)?.parent,
        "Tencent/MobileQQ/head/_hd"
        ).absolutePath

    private fun toMD5(uin: String): String {
        return toMD5Method.invoke(null, uin) as String
    }

    private fun getCroppedBitmap(bm: Bitmap): Bitmap {
        var w: Int = bm.width
        var h: Int = bm.height

        val radius = if (w < h) w else h
        w = radius
        h = radius

        val bmOut = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmOut)

        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = -0xbdbdbe

        val rect = Rect(0, 0, w, h)
        val rectF = RectF(rect)

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(
            rectF.left + rectF.width() / 2, rectF.top + rectF.height() / 2,
            (radius / 2).toFloat(), paint
        )

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bm, rect, rect, paint)

        return bmOut
    }

    private fun getAvatarFromFile(uin: String): Bitmap? {
        val md5 = toMD5(toMD5(toMD5(uin) + uin) + uin)
        val file = File(avatarCachePath, "$md5.jpg_")
        if (file.isFile) {
            return getCroppedBitmap(BitmapFactory.decodeFile(file.absolutePath))
        }
        return null
    }
    fun getAvatar(uin: String): IconCompat? {
        if (avatarCache[uin] == null) {
            var cached = getAvatarFromFile(uin)
            if (cached == null) {
                val face = FaceImpl.getInstance()
                cached = face.getBitmapFromCache(1, uin)
                if (cached == null) {
                    face.requestDecodeFace(1, uin)
                    return null
                }
            }
            avatarCache.put(uin, IconCompat.createWithBitmap(cached))
        }
        return avatarCache[uin]
    }
}
