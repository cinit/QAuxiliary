plugins {
    id("com.android.library")
}

android {
    namespace = "io.github.qauxv.loader.sbl"

    compileSdk {
        // "36.1" -> major=36, minor=1
        version = release(Version.compileSdkVersion.substringBefore('.').toInt()) {
            minorApiLevel = Version.compileSdkVersion.substringAfter('.').toIntOrNull()
        }
    }
    defaultConfig {
        minSdk = Version.minSdk

        buildConfigField("String", "VERSION_NAME", "\"${Common.getBuildVersionName(rootProject)}\"")
        buildConfigField("int", "VERSION_CODE", "${Common.getBuildVersionCode(rootProject)}")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // Xposed API 89
    compileOnly(libs.xposed.api)
    // LSPosed API 100
    compileOnly(projects.libs.libxposed.api)
    compileOnly(libs.androidx.annotation)
    implementation(projects.loader.hookapi)
}
