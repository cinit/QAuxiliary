plugins {
    id("com.android.library")
}

android {
    namespace = "io.github.qauxv.loader.hookapi"
    compileSdk = Version.compileSdkVersion

    defaultConfig {
        minSdk = Version.minSdk
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    compileOnly(libs.androidx.annotation)
}
