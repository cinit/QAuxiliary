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

package me.singleneuron.util

import android.content.Context
import android.util.Base64
import androidx.appcompat.app.AlertDialog

fun processSearchEasterEgg(text: String, context: Context) {
    for (pair in easterEggsMap) {
        for (key in pair.key) {
            if (text.contains(key, true)) {
                AlertDialog.Builder(context)
                    .setTitle(pair.value.first)
                    .setMessage(pair.value.second)
                    .setPositiveButton("OK", null)
                    .create()
                    .show()
                return
            }
        }
    }
}

private val easterEggsMap by lazy {
    hashMapOf(
        arrayOf("\u26A7\uFE0F", "\uD83C\uDF65", "mtf", "mtx", "ftm", "ftx", "transgender") to forSuBanXia,
        arrayOf("喵") to ("喵喵" to "喵喵喵"),
        arrayOf("lgbt", "lgbtq", "lgbtqia", "lgbtqia+", "lesbian", "gay", "bisexual", "queer", "nextalone") to forPride,
    )
}

val forSuBanXia: Pair<String, String> = (String(Base64.decode("Rm9yIHVzIA==", Base64.DEFAULT)) + "\uD83C\uDFF3\uFE0F\u200D\u26A7\uFE0F" to String(
    Base64.decode(
        "5oS/5q+P5LiA5Liq5Lq66YO96IO96Ieq55Sx55qE55Sf5rS75Zyo6Ziz5YWJ5LiL77yM5oS/5oiR55qE6byT5Yqx5LiO5YuH5rCU6ZqPUUF1eGlsaWFyeeS8tOS9oOi6q+aXgeOAggoKCQkJCeKAlOKAlENyeW9saXRpYSwgYW4gZXhvcmRpbmFyeSBkZXZlbG9wZXIsIGFuIG9yZGluYXJ5IE10Rg==",
        Base64.CRLF
    )
))

val forPride: Pair<String, String> = (
    String(Base64.decode("Rm9yIFByaWRlIA==", Base64.DEFAULT)) + "\uD83C\uDFF3\uFE0F\u200D\uD83C\uDF08" to
        String(
            Base64.decode(
                "TG92ZSBpcyBsb3ZlLiAKCkZyb20gbG92ZWx5IE5leHRBbG9uZSwgd2l0aCBsb3ZlLg==",
                Base64.CRLF,
            ),
        )
)
