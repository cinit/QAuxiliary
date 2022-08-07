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

import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.android.tools.build.apkzlib.sign.SigningExtension
import com.android.tools.build.apkzlib.sign.SigningOptions
import com.android.tools.build.apkzlib.zfile.ZFiles
import com.android.tools.build.apkzlib.zip.CompressionMethod
import com.android.tools.build.apkzlib.zip.AlignmentRules
import com.android.tools.build.apkzlib.zip.ZFile
import com.android.tools.build.apkzlib.zip.ZFileOptions
import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.UUID

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "${Version.kotlin}-${Version.ksp}"
    kotlin("plugin.serialization") version Version.kotlin
}

val currentBuildUuid = UUID.randomUUID().toString()
println("Current build ID is $currentBuildUuid")

val ccacheExecutablePath = Common.findInPath("ccache")

if (ccacheExecutablePath != null) {
    println("Found ccache at $ccacheExecutablePath")
} else {
    println("No ccache found.")
}

android {
    defaultConfig {
        applicationId = "io.github.qauxv"
        buildConfigField("String", "BUILD_UUID", "\"$currentBuildUuid\"")
        buildConfigField("long", "BUILD_TIMESTAMP", "${System.currentTimeMillis()}L")

        resourceConfigurations += listOf("zh", "en")

        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DQAUXV_VERSION=$versionName"
                )
                ccacheExecutablePath?.let {
                    arguments += listOf(
                        "-DCMAKE_C_COMPILER_LAUNCHER=$it",
                        "-DCMAKE_CXX_COMPILER_LAUNCHER=$it",
                        "-DNDK_CCACHE=$it",
                        "-DANDROID_CCACHE=$it"
                    )
                }
                targets += "qauxv"
            }
        }
    }
    if (System.getenv("KEYSTORE_PATH") != null) {
        signingConfigs {
            create("release") {
                storeFile = file(System.getenv("KEYSTORE_PATH"))
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }
    buildTypes {
        getByName("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
            if (System.getenv("KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            kotlinOptions.suppressWarnings = true
            val ltoCacheFlags = listOf(
                "-flto=thin",
                "-Wl,--thinlto-cache-policy,cache_size_bytes=300m",
                "-Wl,--thinlto-cache-dir=${buildDir.absolutePath}/.lto-cache",
            )
            externalNativeBuild.cmake {
                cFlags += ltoCacheFlags
                cppFlags += ltoCacheFlags
            }
        }
        getByName("debug") {
            isShrinkResources = false
            isMinifyEnabled = false
            isCrunchPngs = false
            proguardFiles("proguard-rules.pro")
        }
    }
    androidResources {
        additionalParameters("--allow-reserved-package-id", "--package-id", "0x39")
    }
    kotlinOptions.jvmTarget = Version.java.toString()

    externalNativeBuild {
        cmake {
            path = File(projectDir, "src/main/cpp/CMakeLists.txt")
            version = Version.getCMakeVersion(project)
        }
    }
    buildFeatures {
        viewBinding = true
    }
    lint {
        checkDependencies = true
    }
    namespace = "io.github.qauxv"
}

dependencies {
    compileOnly(projects.libs.qqStub)
    implementation(projects.libs.mmkv)
    ksp(projects.libs.ksp)
    // androidx
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.browser:browser:1.4.0")
    val lifecycleVersion = "2.4.1"
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")

    compileOnly("de.robv.android.xposed:api:82")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.google.android.material:material:1.6.1")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.afollestad.material-dialogs:core:3.3.0")
    implementation("com.afollestad.material-dialogs:input:3.3.0")
    implementation("com.jaredrummler:colorpicker:1.1.0")
    implementation("com.github.kyuubiran:EzXHelper:1.0.0")
    // festival title
    implementation("com.github.jinatonic.confetti:confetti:1.1.2")
    implementation("com.github.MatteoBattilana:WeatherView:3.0.0")
    val appCenterSdkVersion = "4.4.5"
    implementation("com.microsoft.appcenter:appcenter-analytics:${appCenterSdkVersion}")
    implementation("com.microsoft.appcenter:appcenter-crashes:${appCenterSdkVersion}")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0-RC")
}

val adb: String = androidComponents.sdkComponents.adb.get().asFile.absolutePath
val killQQ = tasks.register<Exec>("killQQ") {
    group = "qauxv"
    commandLine(adb, "shell", "am", "force-stop", "com.tencent.mobileqq")
    isIgnoreExitValue = true
}

val openQQ = tasks.register<Exec>("openQQ") {
    group = "qauxv"
    commandLine(adb, "shell", "am", "start", "$(pm resolve-activity --components com.tencent.mobileqq)")
    isIgnoreExitValue = true
}

tasks.register<Exec>("openTroubleShooting") {
    group = "qauxv"
    commandLine(adb, "shell", "am", "start",
        "-e", "qa_jump_action_cmd", "io.github.qauxv.TROUBLE_SHOOTING_ACTIVITY",
        "com.tencent.mobileqq/.activity.JumpActivity")
    isIgnoreExitValue = true
}

androidComponents.onVariants { variant ->
    val variantCapped = variant.name.capitalize()
    tasks.register("checkTargetNativeLibs$variantCapped") {
        dependsOn(":app:externalNativeBuild$variantCapped")
        doLast {
            val targetAbi = listOf("arm64-v8a", "armeabi-v7a")
            val soName = "libqauxv.so"
            val libPath = "app/build/intermediates/cmake/debug/obj"
            for (abi in targetAbi) {
                var tmpPath = "$libPath/$abi/$soName"
                if ("/" != File.separator) {
                    tmpPath = tmpPath.replace('/', File.separatorChar)
                }
                val f = File(rootProject.projectDir, tmpPath)
                if (!f.exists()) {
                    throw IllegalStateException("Native library missing for the target abi: $abi. Please run gradle task ':app:externalNativeBuild$variantCapped' manually to force android gradle plugin to satisfy all required ABIs.")
                }
            }
        }
    }

    task("installRestart${variantCapped}") {
        group = "qauxv"
        dependsOn(":app:install$variantCapped", killQQ)
        finalizedBy(openQQ)
    }
}

tasks.register<ReplaceIcon>("replaceIcon") {
    group = "qauxv"
    projectDir.set(project.projectDir)
    commitHash = Common.getGitHeadRefsSuffix(rootProject)
    config()
}.also { tasks.preBuild.dependsOn(it) }

tasks.register<Delete>("cleanCxxIntermediates") {
    group = "qauxv"
    delete(file(".cxx"))
}.also { tasks.clean.dependsOn(it) }

tasks.register<Delete>("cleanOldIcon") {
    group = "qauxv"
    val drawableDir = File(projectDir, "src/main/res/drawable")
    drawableDir
        .listFiles()
        ?.filter { it.isFile && it.name.startsWith("icon") }
        ?.forEach(::delete)
    delete(file("src/main/res/drawable-anydpi-v26/icon.xml"))
}.also { tasks.clean.dependsOn(it) }

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    if (name.contains("release", true)) {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-Xno-call-assertions",
                "-Xno-receiver-assertions",
                "-Xno-param-assertions",
            )
        }
    }
}

tasks.register("checkGitSubmodule") {
    group = "qauxv"
    val projectDir = rootProject.projectDir
    doLast {
        listOf(
            "libs/mmkv/MMKV/Core",
            "libs/qq-stub",
            "app/src/main/cpp/dex_builder",
            "app/src/main/cpp/dex_builder/external/abseil"
        ).forEach {
            val submoduleDir = File(projectDir, it.replace('/', File.separatorChar))
            if (!submoduleDir.exists()) {
                throw IllegalStateException(
                    "submodule dir not found: $submoduleDir" +
                        "\nPlease run 'git submodule init' and 'git submodule update' manually."
                )
            }
        }
    }
}.also { tasks.preBuild.dependsOn(it) }


val synthesizeDistReleaseApksCI by tasks.registering {
    group = "build"
    // use :app:assembleRelease output apk as input
    dependsOn(":app:packageRelease")
    inputs.files(tasks.named("packageRelease").get().outputs.files)
    val srcApkDir = File(project.buildDir, "outputs" + File.separator + "apk" + File.separator + "release")
    if (srcApkDir !in tasks.named("packageRelease").get().outputs.files) {
        val msg = "srcApkDir should be in packageRelease outputs, srcApkDir: $srcApkDir, " +
            "packageRelease outputs: ${tasks.named("packageRelease").get().outputs.files.files}"
        throw IllegalStateException(msg)
    }
    // output name format: "QAuxv-v${defaultConfig.versionName}-${productFlavors.first().name}.apk"
    val outputAbiVariants = mapOf(
        "arm32" to arrayOf("armeabi-v7a"),
        "arm64" to arrayOf("arm64-v8a"),
        "armAll" to arrayOf("armeabi-v7a", "arm64-v8a"),
        "universal" to arrayOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    )
    val versionName = android.defaultConfig.versionName
    val outputDir = File(project.buildDir, "outputs" + File.separator + "ci")
    // declare output files
    outputAbiVariants.forEach { (variant, _) ->
        val outputName = "QAuxv-v${versionName}-${variant}.apk"
        outputs.file(File(outputDir, outputName))
    }
    val signConfig = android.signingConfigs.findByName("release")
    val minSdk = android.defaultConfig.minSdk!!
    doLast {
        if (signConfig == null) {
            logger.error("Task :app:synthesizeDistReleaseApksCI: No release signing config found, skip signing")
        }
        val requiredAbiList = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        outputDir.mkdir()
        val options = ZFileOptions().apply {
            alignmentRule = AlignmentRules.constantForSuffix(".so", 4096)
            noTimestamps = true
            autoSortFiles = true
        }
        if (!srcApkDir.exists()) {
            throw IllegalStateException("input apk not found: ${srcApkDir.absolutePath}")
        }
        // srcApkDir should have one apk file
        val srcApkFiles = srcApkDir.listFiles()?.filter { it.isFile && it.name.endsWith(".apk") } ?: emptyList()
        if (srcApkFiles.size != 1) {
            throw IllegalStateException("input apk should have one apk file, but found ${srcApkFiles.size}")
        }
        val inputApk = srcApkFiles.single()
        val startTime = System.currentTimeMillis()
        ZFile.openReadOnly(inputApk).use { srcApk ->
            // check whether all required abis are in the apk
            requiredAbiList.forEach { abi ->
                val path = "lib/$abi/libqauxv.so"
                if (srcApk.get(path) == null) {
                    throw IllegalStateException("input apk should contain $path, but not found")
                }
            }
            outputAbiVariants.forEach { (variant, abis) ->
                val outputApk = File(outputDir, "QAuxv-v${versionName}-${variant}.apk")
                if (outputApk.exists()) {
                    outputApk.delete()
                }
                ZFiles.apk(outputApk, options).use { dstApk ->
                    if (signConfig != null) {
                        val keyStore = KeyStore.getInstance(signConfig.storeType ?: KeyStore.getDefaultType())
                        FileInputStream(signConfig.storeFile!!).use {
                            keyStore.load(it, signConfig.storePassword!!.toCharArray())
                        }
                        val protParam = KeyStore.PasswordProtection(signConfig.keyPassword!!.toCharArray())
                        val keyEntry = keyStore.getEntry(signConfig.keyAlias!!, protParam)
                        val privateKey = keyEntry as KeyStore.PrivateKeyEntry
                        val signingOptions = SigningOptions.builder()
                            .setMinSdkVersion(minSdk)
                            .setV1SigningEnabled(minSdk < 24)
                            .setV2SigningEnabled(true)
                            .setKey(privateKey.privateKey)
                            .setCertificates(privateKey.certificate as X509Certificate)
                            .setValidation(SigningOptions.Validation.ASSUME_INVALID)
                            .build()
                        SigningExtension(signingOptions).register(dstApk)
                    }
                    // add input apk to the output apk
                    srcApk.entries().forEach { entry ->
                        val cdh = entry.centralDirectoryHeader
                        val name = cdh.name
                        val isCompressed = cdh.compressionInfoWithWait.method != CompressionMethod.STORE
                        if (name.startsWith("lib/")) {
                            val abi = name.substring(4).split('/').first()
                            if (abis.contains(abi)) {
                                dstApk.add(name, entry.open(), isCompressed)
                            }
                        } else {
                            // add all other entries to the output apk
                            dstApk.add(name, entry.open(), isCompressed)
                        }
                    }
                    dstApk.update()
                }
            }
        }
        val endTime = System.currentTimeMillis()
        logger.info("Task :app:synthesizeDistReleaseApksCI: completed in ${endTime - startTime}ms")
    }
}
