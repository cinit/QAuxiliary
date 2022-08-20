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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "io.github.qauxv.buildLogic"

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.android.tools.build:gradle:7.2.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.10.0.202012080955-r")
}

java {
    targetCompatibility = JavaVersion.VERSION_11
    sourceCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

gradlePlugin {
    plugins {
        register("versionPlugin") {
            id = "io.github.qauxv.version"
            implementationClass = "plugin.VersionPlugin"
        }
        register("androidApplication") {
            id = "io.github.qauxv.application"
            implementationClass = "plugin.ApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "io.github.qauxv.library"
            implementationClass = "plugin.LibraryConventionPlugin"
        }
    }
}
