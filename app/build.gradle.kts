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

import android.databinding.tool.ext.capitalizeUS
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.android.tools.build.apkzlib.sign.SigningExtension
import com.android.tools.build.apkzlib.sign.SigningOptions
import com.android.tools.build.apkzlib.zfile.ZFiles
import com.android.tools.build.apkzlib.zip.AlignmentRules
import com.android.tools.build.apkzlib.zip.CompressionMethod
import com.android.tools.build.apkzlib.zip.ZFile
import com.android.tools.build.apkzlib.zip.ZFileOptions
import org.jetbrains.changelog.markdownToHTML
import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.UUID

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("build-logic.android.application")
    alias(libs.plugins.changelog)
    alias(libs.plugins.ksp)
    alias(libs.plugins.license)
    alias(libs.plugins.serialization)
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
    namespace = "io.github.qauxv"
    ndkVersion = Version.getNdkVersion(project)
    defaultConfig {
        applicationId = "io.github.qauxv"
        buildConfigField("String", "BUILD_UUID", "\"$currentBuildUuid\"")
        buildConfigField("long", "BUILD_TIMESTAMP", "${System.currentTimeMillis()}L")

        externalNativeBuild {
            cmake {
                ccacheExecutablePath?.let {
                    arguments += listOf(
                        "-DCMAKE_C_COMPILER_LAUNCHER=$it",
                        "-DCMAKE_CXX_COMPILER_LAUNCHER=$it",
                        "-DNDK_CCACHE=$it",
                        "-DANDROID_CCACHE=$it",
                    )
                }
                val flags = arrayOf(
                    "-Qunused-arguments",
                    "-fno-rtti",
                    "-fvisibility=hidden",
                    "-fvisibility-inlines-hidden",
                    "-fno-omit-frame-pointer",
                    "-Wno-unused-value",
                    "-Wno-unused-variable",
                    "-Wno-unused-command-line-argument",
                    "-DMMKV_DISABLE_CRYPT",
                )
                cppFlags("-std=c++17", *flags)
                cFlags("-std=c18", *flags)
                targets += "qauxv"
            }
        }
    }
    externalNativeBuild {
        cmake {
            path = File(projectDir, "src/main/cpp/CMakeLists.txt")
            version = Version.getCMakeVersion(project)
        }
    }
    buildTypes {
        getByName("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
            kotlinOptions.suppressWarnings = true
            val ltoCacheFlags = listOf(
                "-flto=thin",
                "-Wl,--thinlto-cache-policy,cache_size_bytes=300m",
                "-Wl,--thinlto-cache-dir=${buildDir.absolutePath}/.lto-cache",
            )
            val releaseFlags = arrayOf(
                "-ffunction-sections",
                "-fdata-sections",
                "-Wl,--gc-sections",
                "-Oz",
                "-Wl,--exclude-libs,ALL",
                "-DNDEBUG",
            )
            externalNativeBuild.cmake {
                arguments += "-DQAUXV_VERSION=${defaultConfig.versionName}"
                cFlags += releaseFlags
                cppFlags += releaseFlags
                cFlags += ltoCacheFlags
                cppFlags += ltoCacheFlags
            }
        }
        getByName("debug") {
            @Suppress("ChromeOsAbiSupport")
            ndk.abiFilters += arrayOf("arm64-v8a", "armeabi-v7a")
            isCrunchPngs = false
            proguardFiles("proguard-rules.pro")
            val debugFlags = arrayOf<String>(
//                "-DMODULE_SIGNATURE=E7A8AEB0A1431D12EB04BF1B7FC31960",
//                "-DTEST_SIGNATURE",
            )
            externalNativeBuild.cmake {
                arguments += "-DQAUXV_VERSION=${Version.versionName}.debug"
                cFlags += debugFlags
                cppFlags += debugFlags
            }
        }
    }
    androidResources {
        additionalParameters += arrayOf(
            "--allow-reserved-package-id",
            "--package-id", "0x39"
        )
    }
    packaging {
        resources.excludes.addAll(arrayOf(
            "META-INF/**",
            "kotlin/**",
            "**.bin",
            "kotlin-tooling-metadata.json"
        ))
    }

    buildFeatures {
        aidl = true
        buildConfig = true
        viewBinding = true
    }
    lint {
        checkDependencies = true
    }
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-Xno-call-assertions",
            "-Xno-receiver-assertions",
            "-Xno-param-assertions",
        )
    }
    applicationVariants.all {
        val variantCapped = name.capitalizeUS()
        val mergeAssets = tasks.getByName("merge${variantCapped}Assets")
        mergeAssets.dependsOn(generateEulaAndPrivacy)
        mergeAssets.dependsOn("data${variantCapped}Descriptor")
    }
}

kotlin {
    sourceSets.configureEach {
        kotlin.srcDir("$buildDir/generated/ksp/$name/kotlin/")
    }
}

licenseReport {
    generateCsvReport = false
    generateHtmlReport = false
    generateJsonReport = true
    generateTextReport = false

    copyCsvReportToAssets = false
    copyHtmlReportToAssets = false
}

dependencies {
    compileOnly(projects.libs.stub)
    implementation(projects.libs.mmkv)
    implementation(projects.libs.dexkit)
    ksp(projects.libs.ksp)
    compileOnly(libs.xposed)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.browser)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.common)
    implementation(libs.lifecycle.runtime)
    implementation(libs.hiddenapibypass)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.material)
    implementation(libs.flexbox)
    implementation(libs.colorpicker)
    implementation(libs.material.dialogs.core)
    implementation(libs.material.dialogs.input)
    implementation(libs.ezXHelper)
    // festival title
    implementation(libs.confetti)
    implementation(libs.weatherView)
    implementation(libs.appcenter.analytics)
    implementation(libs.appcenter.crashes)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.sealedEnum.runtime)
    ksp(libs.sealedEnum.ksp)
}

val adb: String = androidComponents.sdkComponents.adb.get().asFile.absolutePath
val packageName = "com.tencent.mobileqq"
val killQQ = tasks.register<Exec>("killQQ") {
    group = "qauxv"
    commandLine(adb, "shell", "am", "force-stop", packageName)
    isIgnoreExitValue = true
}

val openQQ = tasks.register<Exec>("openQQ") {
    group = "qauxv"
    commandLine(adb, "shell", "am", "start", "$(pm resolve-activity --components $packageName)")
    isIgnoreExitValue = true
}

val restartQQ = tasks.register<Exec>("restartQQ") {
    group = "qauxv"
    commandLine(adb, "shell", "am", "start", "$(pm resolve-activity --components $packageName)")
    isIgnoreExitValue = true
}.dependsOn(killQQ)

androidComponents.onVariants { variant ->
    val variantCapped = variant.name.capitalizeUS()
    task("install${variantCapped}AndRestartQQ") {
        group = "qauxv"
        dependsOn(":app:install$variantCapped")
        finalizedBy(restartQQ)
    }
    task("data${variantCapped}Descriptor") {
        inputs.file("${buildDir}/reports/licenses/license${variantCapped}Report.json")
        outputs.file("${projectDir}/src/main/assets/open_source_licenses.json")
        dependsOn("license${variantCapped}Report")

        doFirst {
            val input = inputs.files.singleFile
            val output = outputs.files.singleFile
            this.runCatching {
                output.writeText(Licenses.transform(input.readText()))
            }
        }
    }
}

tasks.register<task.ReplaceIcon>("replaceIcon") {
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

tasks.register("checkGitSubmodule") {
    group = "qauxv"
    val projectDir = rootProject.projectDir
    doLast {
        listOf(
            "libs/mmkv/MMKV/Core",
            "libs/stub/qq-stub",
            "libs/dexkit/DexKit/Core",
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
        logger.error(msg)
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
        require(srcApkDir.exists()) { "srcApkDir not found: $srcApkDir" }
        // srcApkDir should have one apk file
        val srcApkFiles = srcApkDir.listFiles()?.filter { it.isFile && it.name.endsWith(".apk") } ?: emptyList()
        require(srcApkFiles.size == 1) { "input apk should have one apk file, but found ${srcApkFiles.size}" }
        val inputApk = srcApkFiles.single()
        val startTime = System.currentTimeMillis()
        ZFile.openReadOnly(inputApk).use { srcApk ->
            // check whether all required abis are in the apk
            requiredAbiList.forEach { abi ->
                val path = "lib/$abi/libqauxv.so"
                require(srcApk.get(path) != null) { "input apk should contain $path, but not found" }
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
                        } else if (name.startsWith("META-INF/com/android/")) {
                            // drop gradle version
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

val generateEulaAndPrivacy by tasks.registering {
    inputs.files("${rootDir}/LICENSE.md", "${rootDir}/PRIVACY_LICENSE.md")
    outputs.file("${projectDir}/src/main/assets/eulaAndPrivacy.html")

    doFirst {
        val html = inputs.files.map { markdownToHTML(it.readText()) }
        outputs.files.forEach {
            val output = buildString {
                append("<!DOCTYPE html><head><meta charset=\"UTF-8\"></head><body><html>")
                html.forEach(::append)
                append("</body></html>")
            }.lines().joinToString("")
            it.writeText(output)
        }
    }
}
