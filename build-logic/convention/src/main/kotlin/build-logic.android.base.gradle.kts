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

import com.android.build.gradle.BaseExtension

plugins {
    id("com.android.base")
    kotlin("android")
}

extensions.findByType(BaseExtension::class)?.run {
    compileSdkVersion(Version.compileSdkVersion)
    buildToolsVersion = Version.buildToolsVersion
    ndkVersion = Version.getNdkVersion(project)

    defaultConfig {
        minSdk = Version.minSdk
        targetSdk = Version.targetSdk
        versionCode = Common.getBuildVersionCode(rootProject)
        versionName = Common.getBuildVersionName(rootProject)
        resourceConfigurations += listOf("zh", "en")
    }

    compileOptions {
        sourceCompatibility = Version.java
        targetCompatibility = Version.java
    }

    packagingOptions.jniLibs.useLegacyPackaging = false
}

kotlin {
    jvmToolchain(Version.java.toString().toInt())
    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
        }
    }
}
