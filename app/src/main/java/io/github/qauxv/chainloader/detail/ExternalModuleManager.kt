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

package io.github.qauxv.chainloader.detail

import cc.ioctl.util.HostInfo
import java.io.File
import java.io.IOException
import kotlin.jvm.Throws

object ExternalModuleManager {

    data class ExternalModuleInfo(
        val enable: Boolean, // 1 for enable, 0 for disable
        val packageName: String,
        // lower case
        val certificateSha256HexLowerCharsArray: Array<String>
    )

    // format
    // enable,packageName,[certificateSha256HexLowerChars1:certificateSha256HexLowerChars2:...];\n
    // e.g.
    // 1,com.example.android.app,e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855:01ba4719c80b6fe911b091a7c05124b64eeece964e09c058ef8f9805daca546b;

    @Throws(IllegalArgumentException::class)
    fun parseExternalModuleInfoList(input: String): Array<ExternalModuleInfo> {
        // split with '\n'
        val lines = input.split("\n").filter { it.isNotBlank() }
        val regex = Regex("""([01]),([a-zA-Z0-9_.]+),((?:[a-f0-9]{64}:?)+);""")
        return lines.map { line ->
            val matchResult = regex.matchEntire(line)
                ?: throw IllegalArgumentException("Invalid line format: $line")
            val (enableStr, packageName, certs) = matchResult.destructured
            val enable = enableStr == "1"
            val certificateSha256HexLowerChars = certs.split(":").map { it.lowercase() }.toTypedArray()
            ExternalModuleInfo(enable, packageName, certificateSha256HexLowerChars)
        }.toTypedArray()
    }

    fun serializeExternalModuleInfoList(modules: Array<ExternalModuleInfo>): String {
        // format
        // enable,packageName,[certificateSha256HexLowerChars1:certificateSha256HexLowerChars2:...];\n
        return modules.joinToString("\n") { module ->
            val certs = module.certificateSha256HexLowerCharsArray.joinToString(":").lowercase()
            "${if (module.enable) "1" else "0"},${module.packageName},$certs;"
        } + "\n"
    }

    private val mExternalModuleInfoListFile: File by lazy {
        val miscDir = File(HostInfo.getApplication().filesDir, "qa_misc")
        File(miscDir, "qa_external_module_info.list")
    }

    @Throws(IOException::class)
    fun saveExternalModuleInfoList(modules: Array<ExternalModuleInfo>) {
        val serialized = serializeExternalModuleInfoList(modules)
        // validate the serialized content
        parseExternalModuleInfoList(serialized)
        mExternalModuleInfoListFile.parentFile?.mkdirs()
        if (mExternalModuleInfoListFile.exists()) {
            mExternalModuleInfoListFile.setWritable(true, true)
        }
        mExternalModuleInfoListFile.writeText(serialized)
        mExternalModuleInfoListFile.setWritable(false, false)
    }

    @Throws(IOException::class, IllegalArgumentException::class)
    fun loadExternalModuleInfoList(): Array<ExternalModuleInfo> {
        if (!mExternalModuleInfoListFile.exists()) {
            return emptyArray()
        }
        // reject if writable, it should be read-only
        if (mExternalModuleInfoListFile.canWrite()) {
            throw IOException("External module info list file is writable, it should be read-only")
        }
        val content = mExternalModuleInfoListFile.readText()
        if (content.isBlank()) {
            return emptyArray()
        }
        return parseExternalModuleInfoList(content)
    }

}
