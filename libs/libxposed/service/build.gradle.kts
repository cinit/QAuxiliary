plugins {
    id("build-logic.android.library")
}

android {
    namespace = "io.github.libxposed.service"
    sourceSets {
        val main by getting
        main.apply {
            manifest.srcFile("service/service/src/main/AndroidManifest.xml")
            java.setSrcDirs(listOf("service/service/src/main/java"))
            aidl.setSrcDirs(listOf("service/interface/src/main/aidl"))
        }
    }

    defaultConfig {
        minSdk = Version.minSdk
        targetSdk = Version.targetSdk
        buildToolsVersion = Version.buildToolsVersion
    }

    // Java 17 is required by libxposed-service
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = false
        resValues = false
        aidl = true
    }

    dependencies {
        compileOnly(libs.androidx.annotation)
    }

}

// I don't know why but this is required to make the AGP use JDK 17 to compile the source code.
// On my machine, even if I set the sourceCompatibility and targetCompatibility to JavaVersion.VERSION_17,
// and run Gradle with JDK 17, the AGP still uses JDK 11 to compile the source code.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
