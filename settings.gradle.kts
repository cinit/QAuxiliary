@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
// enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

pluginManagement {
    includeBuild("build-logic")
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

rootProject.name = "QAuxiliary"
include(
    ":app",
    ":libs:qq-stub",
    ":libs:ksp",
    ":libs:mmkv"
)
