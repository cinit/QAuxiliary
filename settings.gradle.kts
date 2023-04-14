@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
// enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google() {
            mavenContent {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com.google.*")
                includeGroupByRegex("com.android.*")
            }
        }
        maven("https://jitpack.io") {
            mavenContent {
                includeGroup("com.github.livefront.sealed-enum")
                includeGroup("com.github.MatteoBattilana")
                includeGroup("com.github.plattysoft")
            }
        }
        maven("https://api.xposed.info/") {
            mavenContent {
                includeGroup("de.robv.android.xposed")
            }
        }
    }
}

plugins {
    `gradle-enterprise`
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlwaysIf(System.getenv("GITHUB_ACTIONS") == "true")
        publishOnFailure()
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
)
