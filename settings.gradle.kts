pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://api.xposed.info/")
    }
}

include(":app", ":stub", ":ksp", ":mmkv")
val compilerLibsDir: File = File(settingsDir, "libs")
project(":stub").projectDir = File(compilerLibsDir, "stub")
project(":ksp").projectDir = File(compilerLibsDir, "ksp")
project(":mmkv").projectDir = File(compilerLibsDir, "mmkv")
rootProject.name = "QAuxiliary"
