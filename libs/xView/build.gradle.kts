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

plugins {
    id("build-logic.android.library")
}

android {
    namespace = "com.lxj.xpopup"
    sourceSets {
        val main by getting
        main.apply {
            manifest.srcFile("AndroidManifest.xml")
            java.setSrcDirs(listOf("/XPopup/library/src/main/java","EasyAdapter/easy-adapter/src/main/java"))
            res.setSrcDirs(listOf("EasyAdapter/easy-adapter/src/main/res","/XPopup/library/src/main/res"))
        }
    }
}
dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation("com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0")
}
