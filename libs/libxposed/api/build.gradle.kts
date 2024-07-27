plugins {
    id("build-logic.android.library")
}

android {
    namespace = "io.github.libxposed.api"
    sourceSets {
        val main by getting
        main.apply {
            manifest.srcFile("AndroidManifest.xml")
            java.setSrcDirs(listOf("api/api/src/main/java"))
        }
    }

    defaultConfig {
        minSdk = Version.minSdk
        targetSdk = Version.targetSdk
        buildToolsVersion = Version.buildToolsVersion
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    dependencies {
        // androidx nullability stubs
        compileOnly(libs.androidx.annotation)
    }

}
