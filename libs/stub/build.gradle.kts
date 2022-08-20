plugins {
    id("io.github.qauxv.library")
}

android {
    sourceSets {
        val main by getting
        main.apply {
            manifest.srcFile("qq-stub/src/main/AndroidManifest.xml")
            java.setSrcDirs(listOf("qq-stub/src/main/java"))
            res.setSrcDirs(listOf("qq-stub/src/main/res"))
        }
    }
}
