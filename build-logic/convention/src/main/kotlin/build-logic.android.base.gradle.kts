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

@file:Suppress("UnstableApiUsage")

import com.android.build.api.dsl.CommonExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

plugins {
    id("com.android.base")
}

extensions.findByType(CommonExtension::class)?.run {
    compileSdk {
        version = release(Version.compileSdkVersion)
    }
    buildToolsVersion = Version.buildToolsVersion
    ndkVersion = Version.getNdkVersion(project)

    defaultConfig.apply {
        minSdk = Version.minSdk
    }

    compileOptions.apply {
        sourceCompatibility = Version.java
        targetCompatibility = Version.java
    }

    packaging.jniLibs.useLegacyPackaging = false
}

extensions.findByType(KotlinAndroidProjectExtension::class)?.run {
    compilerOptions {
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(Version.java.toString())
    }
}
