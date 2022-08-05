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

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import io.github.qauxv.util.Log
import io.github.qauxv.util.decodeToDataClass
import me.singleneuron.base.bridge.CardMsgList
import me.singleneuron.data.CardMsgCheckResult
import java.io.BufferedReader
import java.io.File
import java.io.IOException

fun ViewGroup.addViewConditionally(view: View, condition: Boolean) {
    if (condition) {
        this.addView(view)
    }
}

@Throws(IOException::class)
fun readFile(file: File): String {
    return file.readText()
}

@Throws(IOException::class)
fun readFromBufferedReader(bufferedReader: BufferedReader): String {
    return bufferedReader.readText()
}

fun Intent.dump() {
    dumpIntent(this)
}

fun dumpIntent(intent: Intent) {
    Log.d(intent.toString())
    Log.d(intent.extras.toString())
    Log.d(Log.getStackTraceString(Throwable()))
}

fun checkCardMsg(originString: String): CardMsgCheckResult {
    try {
        Log.d("origin string: $originString")
        val string = decodePercent(originString)
        Log.d("decode string: $string")
        val blackListString = CardMsgList.getInstance().invoke()
        val blackList = blackListString.decodeToDataClass<Map<String, String>>()
        for (rule in blackList) {
            if (Regex(
                    rule.value,
                    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
                ).containsMatchIn(string)
            ) {
                return CardMsgCheckResult(false, rule.key)
            }
        }
        return CardMsgCheckResult(true)
    } catch (e: Exception) {
        Log.e(e)
        return CardMsgCheckResult(false, "Failed: $e")
    }
}

private fun decodePercent(string: String): String {
    var produceString = string
    val regex = Regex("""%[\da-fA-F]{2}""", RegexOption.IGNORE_CASE)
    while (true) {
        if (!regex.containsMatchIn(produceString)) return produceString
        produceString = regex.replace(produceString) { matchResult ->
            val hex = matchResult.value.substring(1)
            try {
                val char = Integer.valueOf(hex, 16).toChar().toString()
                Log.d("replace $hex -> $char")
                return@replace char
            } catch (e: Exception) {
                Log.e(e)
                return@replace hex
            }
        }
        Log.d("processing string: $produceString")
    }
}
