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
import com.android.build.gradle.internal.dsl.SigningConfig
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.android.tools.build.apkzlib.sign.SigningExtension
import com.android.tools.build.apkzlib.sign.SigningOptions
import com.android.tools.build.apkzlib.zfile.ZFiles
import com.android.tools.build.apkzlib.zip.AlignmentRule
import com.android.tools.build.apkzlib.zip.CompressionMethod
import com.android.tools.build.apkzlib.zip.ZFile
import com.android.tools.build.apkzlib.zip.ZFileOptions
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Locale
import java.util.UUID

plugins {
    id("build-logic.android.application")
    alias(libs.plugins.changelog)
    alias(libs.plugins.ksp)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.serialization)
    alias(libs.plugins.aboutlibraries)
}

// ------ buildscript config ------

val buildAllAbiForDebug = false
val isNewXposedApiEnabled = true
val isNativeFullDebugMode = false

val currentBuildUuid = UUID.randomUUID().toString()
println("Current build ID is $currentBuildUuid")

val ccacheExecutablePath = Common.findInPath("ccache")

if (ccacheExecutablePath != null) {
    println("Found ccache at $ccacheExecutablePath")
} else {
    println("No ccache found.")
}

fun getSignatureKeyDigest(signConfig: SigningConfig?): String? {
    val key1: String? = if (signConfig?.storeFile != null) {
        // extract certificate digest
        val key = signConfig.storeFile
        val keyStore = KeyStore.getInstance(signConfig.storeType ?: KeyStore.getDefaultType())
        FileInputStream(key!!).use {
            keyStore.load(it, signConfig.storePassword!!.toCharArray())
        }
        val cert = keyStore.getCertificate(signConfig.keyAlias!!)
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(cert.encoded)
        digest.joinToString("") { "%02X".format(it) }
    } else null
    val key2: String? = Version.getLocalProperty(project, "qauxv.signature.md5digest")
        ?.uppercase(Locale.ROOT)?.ifEmpty { null }
    // check if key1 and key2 are the same
    if (key1 != null && key2 != null && key1 != key2) {
        error(
            "The signature key digest in the signing config and local.properties are different, " +
                "got $key1 and $key2, please make sure they are the same."
        )
    }
    return (key1 ?: key2)?.also {
        check(it.matches(Regex("[0-9A-F]{32}"))) {
            "Invalid signature key digest: $it"
        }
    }
}

android {
    namespace = "io.github.qauxv"
    ndkVersion = Version.getNdkVersion(rootProject)
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
                Version.getNinjaPathOrNull(rootProject)?.let {
                    arguments += "-DCMAKE_MAKE_PROGRAM=$it"
                }
                arguments += "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON"
                val flags = arrayOf(
                    "-Qunused-arguments",
                    "-fno-rtti",
                    "-fvisibility=protected",
                    "-fvisibility-inlines-hidden",
                    "-fno-omit-frame-pointer",
                    "-Wno-unused-value",
                    "-Wno-unused-variable",
                    "-Wno-unused-command-line-argument",
                    "-DMMKV_DISABLE_CRYPT",
                )
                // do not add -std=c++20 here, it should be added in the CMakeLists.txt where each module is defined
                // some modules uses features that are REMOVED or deprecated in C++20
                cppFlags(*flags)
                cFlags("-std=c18", *flags)
                targets += "qauxv-core0"
            }
        }

        ndk.debugSymbolLevel = "FULL"
    }
    externalNativeBuild {
        cmake {
            path = File(projectDir, "src/main/cpp/CMakeLists.txt")
            version = Version.getCMakeVersion(project)
        }
    }
    buildTypes {
        val signatureDigest: String? = getSignatureKeyDigest(signingConfigs.findByName("release"))
        if (signatureDigest != null) {
            println("Signature Digest: $signatureDigest")
        } else {
            println("No Signature Digest Configured")
        }
        getByName("release") {
            isShrinkResources = false
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
            kotlinOptions.suppressWarnings = true
            val ltoCacheFlags = listOf(
                "-flto=thin",
                "-Wl,--thinlto-cache-policy,cache_size_bytes=300m",
                "-Wl,--thinlto-cache-dir=${buildDir.absolutePath}/.lto-cache",
            )
            var releaseFlags = arrayOf(
                "-ffunction-sections",
                "-fdata-sections",
                "-Wl,--gc-sections",
                "-O3",
                "-Wl,--exclude-libs,ALL",
                "-DNDEBUG",
            )
            if (signatureDigest != null) {
                releaseFlags += "-DMODULE_SIGNATURE=$signatureDigest"
            }
            externalNativeBuild.cmake {
                arguments += "-DQAUXV_VERSION=${defaultConfig.versionName}"
                cFlags += releaseFlags
                cppFlags += releaseFlags
                cFlags += ltoCacheFlags
                cppFlags += ltoCacheFlags
            }
        }
        getByName("debug") {
            ndk {
                if (isNativeFullDebugMode) {
                    isJniDebuggable = true
                } else {
                    if (!buildAllAbiForDebug) {
                        @Suppress("ChromeOsAbiSupport")
                        abiFilters += arrayOf("arm64-v8a", "armeabi-v7a")
                    }
                }
            }
            isCrunchPngs = false
            proguardFiles("proguard-rules.pro")
            var debugFlags = arrayOf<String>(
//                "-DTEST_SIGNATURE",
            )
            if (signatureDigest != null) {
                debugFlags += "-DMODULE_SIGNATURE=$signatureDigest"
            }
            externalNativeBuild.cmake {
                arguments.addAll(
                    arrayOf(
                        "-DQAUXV_VERSION=${Version.versionName}.debug",
                    )
                )
                arguments.addAll(
                    if (isNativeFullDebugMode) arrayOf(
                        "-DCMAKE_CXX_FLAGS_DEBUG=-O0",
                        "-DCMAKE_C_FLAGS_DEBUG=-O0",
                    )
                    else arrayOf(
                        "-DCMAKE_CXX_FLAGS_DEBUG=-Og",
                        "-DCMAKE_C_FLAGS_DEBUG=-Og",
                    )
                )
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
        // libxposed API uses META-INF/xposed
        resources.excludes.addAll(
            arrayOf(
                "kotlin/**",
                "**.bin",
                "kotlin-tooling-metadata.json"
            )
        )
        if (!isNewXposedApiEnabled) {
            resources.excludes.add("META-INF/xposed/**")
        }
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
        tasks.findByName("lintVitalAnalyze${variantCapped}")?.dependsOn(mergeAssetsProvider)
        mergeAssetsProvider.dependsOn(generateEulaAndPrivacy)
    }

    if (isNativeFullDebugMode) {
        packagingOptions.jniLibs {
            // be aware that some SIGSEGVs and SIGBUSes are only reproducible with "useLegacyPackaging = false"
            useLegacyPackaging = true
            keepDebugSymbols += "**/*.so"
        }
    }
    // not use embedded dex
    packagingOptions.dex.useLegacyPackaging = true
}

kotlin {
    sourceSets.configureEach {
        kotlin.srcDir("$buildDir/generated/ksp/$name/kotlin/")
    }
    sourceSets.main {
        kotlin.srcDir(File(rootDir, "libs/ezxhelper/src/main/java"))
    }
}

dependencies {
    // loader
    compileOnly(projects.loader.hookapi)
    runtimeOnly(projects.loader.sbl)
    implementation(projects.loader.startup)
    // ksp
    ksp(projects.libs.ksp)
    // host stub
    compileOnly(projects.libs.stub)
    // libraries
    implementation(projects.libs.mmkv)
    implementation(projects.libs.dexkit)
    implementation(projects.libs.xView)
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
    // festival title
    implementation(libs.confetti)
    implementation(libs.weatherView)
    implementation(libs.appcenter.analytics)
    implementation(libs.appcenter.crashes)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.sealedEnum.runtime)
    implementation(libs.glide)
    implementation(libs.byte.buddy)
    implementation(libs.dalvik.dx)
    ksp(libs.sealedEnum.ksp)
    implementation(libs.google.protobuf.java)
    implementation(libs.dexlib2)
    // I don't know why, but without this, compilation will fail
    implementation(libs.google.guava)
    // for get activation status
    implementation(projects.libs.libxposed.service)
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
        val submoduleContentLines = File(projectDir, ".gitmodules").readText().replace('\r', '\n').split('\n')
        // regex '[submodule "(.+)"]'
        val prefix = "[submodule \""
        val suffix = "\"]"
        val capturedSubmodulePaths = submoduleContentLines
            .filter { it.startsWith(prefix) && it.endsWith(suffix) }
            .map { it.substring(prefix.length, it.length - suffix.length) }
        capturedSubmodulePaths.forEach {
            val submoduleDir = File(projectDir, "$it/.git")
            if (!submoduleDir.exists()) {
                error(
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
            alignmentRule = object : AlignmentRule {
                override fun alignment(path: String): Int {
                    if (path.endsWith(".so")) {
                        if (path.contains("arm64-v8a") || path.contains("x86_64") || path.contains("riscv64")) {
                            // for 64-bit so files, we use 16k alignment in case of 16k page size
                            return 16384
                        } else {
                            // for 32-bit so files, we use 4k alignment
                            // will there be any 16k-page-size devices supporting 32-bit abi?
                            return 4096
                        }
                    } else {
                        // no alignment for other files
                        return AlignmentRule.NO_ALIGNMENT
                    }
                }
            }
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
                val path = "lib/$abi/libqauxv-core0.so"
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
                append("<!DOCTYPE html ><head><meta charset=\"UTF-8\"></head><html><body>")
                html.forEach(::append)
                append("</body></html>")
            }.lines().joinToString("")
            it.writeText(output)
        }
    }
}

// see https://github.com/google/protobuf-gradle-plugin/issues/518
protobuf {
    protoc {
        artifact = libs.google.protobuf.protoc.get().toString()
    }
    plugins {
        generateProtoTasks {
            all().forEach {
                it.builtins {
                    create("java") {
                        option("lite")
                    }
                }
            }
        }
    }
}

// force kotlin to produce java 11 class files
tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

// force javac to produce java 11 class files
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

// javac should be able to read java 17 class files, although we force it to produce java 11 class files for this module
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
