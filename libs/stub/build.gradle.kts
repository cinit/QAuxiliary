plugins {
    id("build-logic.android.library")
}

android {
    namespace = "me.teble.xposed.stub"
    sourceSets {
        val main by getting
        main.apply {
            manifest.srcFile("AndroidManifest.xml")
            java.setSrcDirs(listOf("qq-stub/src/main/java"))
            res.setSrcDirs(listOf("qq-stub/src/main/res"))
        }
    }

    dependencies {
        // androidx nullability stubs
        compileOnly("androidx.annotation:annotation:1.8.0")
    }
}
