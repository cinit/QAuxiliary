plugins {
    id("build-logic.android.library")
}

android {
    namespace = "me.teble.xposed.stub"
    sourceSets {
        val main by getting
        main.apply {
            manifest.srcFile("AndroidManifest.xml")
            java.directories += "qq-stub/src/main/java"
            res.directories += "qq-stub/src/main/res"
        }
    }

    dependencies {
        // androidx nullability stubs
        compileOnly(libs.androidx.annotation)
    }
}
