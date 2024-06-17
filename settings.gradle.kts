@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
// enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven("https://jitpack.io") {
            content {
                includeGroup("com.github.livefront.sealed-enum")
                includeGroup("com.github.MatteoBattilana")
                includeGroup("com.github.plattysoft")
            }
        }
        maven("https://api.xposed.info/") {
            content {
                includeGroup("de.robv.android.xposed")
            }
        }
        mavenCentral()
    }
}

includeBuild("build-logic")

plugins {
    id("com.gradle.develocity") version "3.17.5"
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
        val isOffline = providers.provider { gradle.startParameter.isOffline }.getOrElse(false)
        val ci = System.getenv("GITHUB_ACTIONS") == "true"
        publishing {
            onlyIf { System.getenv("GITHUB_ACTIONS") == "true" }
            onlyIf { !isOffline && (it.buildResult.failures.isNotEmpty() || ci) }
        }
    }
}

rootProject.name = "QAuxiliary"
include(
    ":app",
    ":libs:stub",
    ":libs:ksp",
    ":libs:mmkv",
    ":libs:dexkit",
    ":libs:ezXHelper",
    ":libs:xView",
)
