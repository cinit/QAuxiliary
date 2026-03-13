plugins {
    id("com.android.library")
}

android {
    namespace = "io.github.qauxv.loader.hookapi"

    compileSdk {
        // "36.1" -> major=36, minor=1
        version = release(Version.compileSdkVersion.substringBefore('.').toInt()) {
            minorApiLevel = Version.compileSdkVersion.substringAfter('.').toIntOrNull()
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    compileOnly(libs.androidx.annotation)
}
