plugins {
    id("build-logic.android.library")
}

android {
    defaultConfig {
        buildConfigField("String", "FLAVOR", "\"StaticCpp\"")
    }

    buildFeatures {
        buildConfig = true
    }

    sourceSets {
        val main by getting
        main.apply {
            manifest.srcFile("MMKV/Android/MMKV/mmkv/src/main/AndroidManifest.xml")
            java.setSrcDirs(listOf("MMKV/Android/MMKV/mmkv/src/main/java"))
            aidl.setSrcDirs(listOf("MMKV/Android/MMKV/mmkv/src/main/aidl"))
            res.setSrcDirs(listOf("MMKV/Android/MMKV/mmkv/src/main/res"))
            assets.setSrcDirs(listOf("MMKV/Android/MMKV/mmkv/src/main/assets"))
        }
    }

    dependencies {
        compileOnly("androidx.annotation:annotation:1.4.0")
    }
}
