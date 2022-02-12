import java.util.UUID

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp") version "${Version.kotlin}-${Version.ksp}"
}

val currentBuildUuid = UUID.randomUUID().toString()
println("Current build ID is $currentBuildUuid")


android {
    compileSdk = 31
    ndkVersion = Version.detectNdkVersion()
    defaultConfig {
        applicationId = "io.github.qauxv"
        minSdk = 21
        targetSdk = 32
        versionCode = Common.getBuildVersionCode(rootProject)
        // versionName = major.minor.bugfix.rev.commit
        versionName = "0.1.0" + (Common.getGitHeadRefsSuffix(rootProject))
        buildConfigField("String", "BUILD_UUID", "\"$currentBuildUuid\"")
        buildConfigField("long", "BUILD_TIMESTAMP", "${System.currentTimeMillis()}L")
        multiDexEnabled = false
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
        externalNativeBuild {
            cmake {
                arguments("-DQAUXV_VERSION=$versionName")
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
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }
    buildTypes {
        getByName("release") {
            isShrinkResources = false
            isMinifyEnabled = false
            proguardFiles("proguard-rules.pro")
            if (System.getenv("KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            tasks.forEach {
                if (it.name.contains("lint")) {
                    it.enabled = false
                }
            }
            kotlinOptions.suppressWarnings = true
        }
        getByName("debug") {
            isShrinkResources = false
            isMinifyEnabled = false
            proguardFiles("proguard-rules.pro")
        }
        create("CI") {
            initWith(getByName("debug"))
            isShrinkResources = false
            isMinifyEnabled = false
            signingConfig = null
            ndk {
                abiFilters.clear()
                abiFilters.add("arm64-v8a")
            }
            matchingFallbacks += listOf("debug")
            tasks.forEach {
                if (it.name.contains("lint")) {
                    it.enabled = false
                }
            }
            kotlinOptions.suppressWarnings = true
        }
    }
    androidResources {
        additionalParameters("--allow-reserved-package-id", "--package-id", "0x39")
    }
    compileOptions {
        sourceCompatibility = Version.java
        targetCompatibility = Version.java
    }
    kotlinOptions {
        jvmTarget = Version.java.toString()
        compileOptions {
            kotlinOptions.freeCompilerArgs += "-Xmulti-platform"
        }
    }
    // Encapsulates your external native build configurations.
    externalNativeBuild {
        // Encapsulates your CMake build configurations.
        cmake {
            // Provides a relative path to your CMake build script.
            path = File(projectDir, "src/main/cpp/CMakeLists.txt")
            version = "3.18.1"
        }
    }
    buildFeatures {
        viewBinding = true
    }
    applicationVariants.all {
        val outputFileName = "QAuxv-v${defaultConfig.versionName}-${this.buildType.name}.apk"
        outputs.all {
            val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output?.outputFileName = outputFileName
        }
    }
}

kotlin {
    sourceSets.debug {
        kotlin.srcDir("build/generated/ksp/debug/kotlin")
    }
    sourceSets.release {
        kotlin.srcDir("build/generated/ksp/release/kotlin")
    }
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "lib", "include" to listOf("*.jar"))))
    compileOnly(project(":stub"))
    implementation(project(":mmkv"))
    ksp(project(":ksp"))
    compileOnly("de.robv.android.xposed:api:82")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    // androidx
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-testing:2.4.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("androidx.browser:browser:1.4.0")
    implementation("com.google.android.material:material:1.5.0")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.afollestad.material-dialogs:core:3.3.0")
    implementation("com.afollestad.material-dialogs:input:3.3.0")
    implementation("com.github.kyuubiran:EzXHelper:0.6.1")
}

dependencies {
    val appCenterSdkVersion = "4.4.2"
    implementation("com.microsoft.appcenter:appcenter-analytics:${appCenterSdkVersion}")
    implementation("com.microsoft.appcenter:appcenter-crashes:${appCenterSdkVersion}")
}

dependencies {
    val lifecycleVersion = "2.4.1"
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")
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
}

tasks.register<ReplaceIcon>("replaceIcon")
tasks.getByName("preBuild").dependsOn(tasks.getByName("replaceIcon"))

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
    doLast {
        val path = "libs/mmkv/MMKV/Core".replace('/', File.separatorChar)
        val submoduleDir = File(rootProject.projectDir, path)
        if (!submoduleDir.exists()) {
            throw IllegalStateException("submodule dir not found: $submoduleDir" +
                    "\nPlease run 'git submodule init' and 'git submodule update' manually.")
        }
    }
}
tasks.getByName("preBuild").dependsOn(tasks.getByName("checkGitSubmodule"))
