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

package cc.microblock.hook

import cc.hicore.QApp.QAppUtils
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.dexkit.AIOTextElementCtor
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.requireMinTimVersion
import xyz.nextalone.util.get
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.set
import java.util.regex.Pattern

val CJK = "\u2e80-\u2eff\u2f00-\u2fdf\u3040-\u309f\u30a0-\u30fa\u30fc-\u30ff\u3100-\u312f\u3200-\u32ff\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff"

val ANY_CJK = Pattern.compile("[$CJK]")

val CONVERT_TO_FULLWIDTH_CJK_SYMBOLS_CJK = Pattern.compile("([$CJK])[ ]*(\\:+|\\.)[ ]*([$CJK])")
val CONVERT_TO_FULLWIDTH_CJK_SYMBOLS = Pattern.compile("([$CJK])[ ]*([~\\!;,\\?]+)[ ]*")
val DOTS_CJK = Pattern.compile("([\\.]{2,}|\\u2026)([$CJK])")
val FIX_CJK_COLON_ANS = Pattern.compile("([$CJK])\\:([A-Z0-9\\(\\)])")

val CJK_QUOTE = Pattern.compile("([$CJK])([\\`\"\\u05f4])")
val QUOTE_CJK = Pattern.compile("([\\`\"\\u05f4])([$CJK])")
val FIX_QUOTE_ANY_QUOTE = Pattern.compile("([`\"\\u05f4]+)[ ]*(.+?)[ ]*([`\"\\u05f4]+)")

val CJK_SINGLE_QUOTE_BUT_POSSESSIVE = Pattern.compile("([$CJK])('[^s])")
val SINGLE_QUOTE_CJK = Pattern.compile("(')([$CJK])")
val FIX_POSSESSIVE_SINGLE_QUOTE = Pattern.compile("([A-Za-z0-9${CJK}])( )('s)")

val HASH_ANS_CJK_HASH = Pattern.compile("([$CJK])(#)([$CJK]+)(#)([$CJK])")
val CJK_HASH = Pattern.compile("([$CJK])(#([^ ]))")
val HASH_CJK = Pattern.compile("(([^ ])#)([$CJK])")

val CJK_OPERATOR_ANS = Pattern.compile("([$CJK])([\\+\\-\\*\\/=&\\|<>])([A-Za-z0-9])")
val ANS_OPERATOR_CJK = Pattern.compile("([A-Za-z0-9])([\\+\\-\\*\\/=&\\|<>])([$CJK])")

val FIX_SLASH_AS = Pattern.compile("([/]) ([a-z\\-\\_\\./]+)")
val FIX_SLASH_AS_SLASH = Pattern.compile("([/\\.])([A-Za-z\\-\\_\\./]+) ([/])")

val CJK_LEFT_BRACKET = Pattern.compile("([$CJK])([\\(\\[\\{<>\\u201c])")
val RIGHT_BRACKET_CJK = Pattern.compile("([\\)\\]\\}>\\u201d])([$CJK])")
val FIX_LEFT_BRACKET_ANY_RIGHT_BRACKET = Pattern.compile("([\\(\\[\\{<\\u201c]+)[ ]*(.+?)[ ]*([\\)\\]\\}>\u201d]+)")
val ANS_CJK_LEFT_BRACKET_ANY_RIGHT_BRACKET = Pattern.compile("([A-Za-z0-9${CJK}])[ ]*([\\u201c])([A-Za-z0-9${CJK}\\-_ ]+)([\\u201d])")
val LEFT_BRACKET_ANY_RIGHT_BRACKET_ANS_CJK = Pattern.compile("([\\u201c])([A-Za-z0-9${CJK}\\-_ ]+)([\\u201d])[ ]*([A-Za-z0-9${CJK}])")

val AN_LEFT_BRACKET = Pattern.compile("([A-Za-z0-9])([\\(\\[\\{])")
val RIGHT_BRACKET_AN = Pattern.compile("([\\)\\]\\}])([A-Za-z0-9])")

val CJK_ANS = Pattern.compile("([$CJK])([A-Za-z\\u0370-\\u03ff0-9@\$%\\^&\\*\\-\\+\\\\=\\|/\\u00a1-\\u00ff\\u2150-\\u218f\\u2700—\\u27bf])")
val ANS_CJK = Pattern.compile("([A-Za-z\\u0370-\\u03ff0-9~\\\$%\\^&\\*\\-\\+\\\\=\\|/!;:,\\.\\?\\u00a1-\\u00ff\\u2150-\\u218f\\u2700—\\u27bf])([$CJK])")

val S_A = Pattern.compile("(%)([A-Za-z])")

val MIDDLE_DOT = Pattern.compile("([ ]*)([\\u00b7\\u2022\\u2027])([ ]*)")

fun convertToFullwidth(symbols: String): String {
    return symbols
        .replace("~", "～")
        .replace("!", "！")
        .replace(";", "；")
        .replace(":", "：")
        .replace(",", "，")
        .replace(".", "。")
        .replace("?", "？")
}

// This is an incomplete implementation of https://github.com/vinta/pangu.js/blob/master/src/shared/core.js
// https://github.com/vinta/pangu.js/blob/6107055384b99e6f30a49f5d1b85aa0b78251dc2/src/shared/core.js#L118-L126
// These lines are implemented, work in the playground, but are commented in the actual code, as they are causing compile errors
// As I'm not so familiar with Kotlin, if someone is able to figure it out and make it a complete impl, I'll appreciate it.
fun pangu_spacing(text: String): String {
    if (text.length <= 1 || !ANY_CJK.matcher(text).find()) {
        return text
    }

    var newText = text
    val fixSlash = !text.startsWith("/ ")

    /*

    // Corresponding lines: https://github.com/vinta/pangu.js/blob/6107055384b99e6f30a49f5d1b85aa0b78251dc2/src/shared/core.js#L118-L126

         newText = CONVERT_TO_FULLWIDTH_CJK_SYMBOLS_CJK.matcher(newText).replaceAll { matchResult ->
            val leftCjk = matchResult.group(1)
            val symbols = matchResult.group(2)
            val rightCjk = matchResult.group(3)
            val fullwidthSymbols = convertToFullwidth(symbols)
            "$leftCjk$fullwidthSymbols$rightCjk"
        }

        newText = CONVERT_TO_FULLWIDTH_CJK_SYMBOLS.matcher(newText).replaceAll { matchResult ->
            val cjk = matchResult.group(1)
            val symbols = matchResult.group(2)
            val fullwidthSymbols = convertToFullwidth(symbols)
            "$cjk$fullwidthSymbols"
        }
    */

    newText = DOTS_CJK.matcher(newText).replaceAll("$1 $2")
    newText = FIX_CJK_COLON_ANS.matcher(newText).replaceAll("$1：$2")

    newText = CJK_QUOTE.matcher(newText).replaceAll("$1 $2")
    newText = QUOTE_CJK.matcher(newText).replaceAll("$1 $2")
    newText = FIX_QUOTE_ANY_QUOTE.matcher(newText).replaceAll("$1$2$3")

    newText = CJK_SINGLE_QUOTE_BUT_POSSESSIVE.matcher(newText).replaceAll("$1 $2")
    newText = SINGLE_QUOTE_CJK.matcher(newText).replaceAll("$1 $2")
    newText = FIX_POSSESSIVE_SINGLE_QUOTE.matcher(newText).replaceAll("$1's")

    newText = HASH_ANS_CJK_HASH.matcher(newText).replaceAll("$1 $2$3$4 $5")
    newText = CJK_HASH.matcher(newText).replaceAll("$1 $2")
    newText = HASH_CJK.matcher(newText).replaceAll("$1 $3")

    newText = CJK_OPERATOR_ANS.matcher(newText).replaceAll("$1 $2 $3")
    newText = ANS_OPERATOR_CJK.matcher(newText).replaceAll("$1 $2 $3")

    newText = FIX_SLASH_AS.matcher(newText).replaceAll("$1$2")
    newText = FIX_SLASH_AS_SLASH.matcher(newText).replaceAll("$1$2$3")

    newText = CJK_LEFT_BRACKET.matcher(newText).replaceAll("$1 $2")
    newText = RIGHT_BRACKET_CJK.matcher(newText).replaceAll("$1 $2")
    newText = FIX_LEFT_BRACKET_ANY_RIGHT_BRACKET.matcher(newText).replaceAll("$1$2$3")
    newText = ANS_CJK_LEFT_BRACKET_ANY_RIGHT_BRACKET.matcher(newText).replaceAll("$1 $2$3$4")
    newText = LEFT_BRACKET_ANY_RIGHT_BRACKET_ANS_CJK.matcher(newText).replaceAll("$1$2$3 $4")

    newText = AN_LEFT_BRACKET.matcher(newText).replaceAll("$1 $2")
    newText = RIGHT_BRACKET_AN.matcher(newText).replaceAll("$1 $2")

    newText = CJK_ANS.matcher(newText).replaceAll("$1 $2")
    newText = ANS_CJK.matcher(newText).replaceAll("$1 $2")

    newText = S_A.matcher(newText).replaceAll("$1 $2")

    newText = MIDDLE_DOT.matcher(newText).replaceAll("・")

    if (fixSlash && newText.startsWith("/ ")) {
        newText = "/" + newText.substring(2)
    }

    return newText
}


@FunctionHookEntry
@UiItemAgentEntry
object SendPangu : CommonSwitchFunctionHook("sendMsgPangu", arrayOf(AIOTextElementCtor)) {
    override val name = "发送消息自动 Pangu"
    override val description = "自动在中英文间加上空格，以美化排版\n若消息以,,或，，开头，则不会进行处理"

    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_OTHER
    override val isAvailable = QAppUtils.isQQnt()
    override fun initOnce(): Boolean {
        DexKit.requireMethodFromCache(AIOTextElementCtor)
            .hookBefore(this) {
                val inputStrFieldName = when {
                    requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA) -> "d"
                    requireMinQQVersion(QQVersion.QQ_9_0_56) -> "e"
                    else -> "a"
                }
                val content = it.args[0].get(inputStrFieldName) as String
                if (!content.startsWith("，，") && !content.startsWith(",,"))
                    it.args[0].set(inputStrFieldName, processPangu(content))
                else
                    it.args[0].set(inputStrFieldName, content.substring(2))
            }
        return true
    }

    private fun processPangu(message: String): String {
        return pangu_spacing(message)
    }
}
