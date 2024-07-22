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

    dependencies {
        // androidx nullability stubs
        compileOnly(libs.androidx.annotation)
    }

}
