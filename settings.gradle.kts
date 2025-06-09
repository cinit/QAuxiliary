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
                // includeGroup("com.github.plattysoft")
            }
        }
        maven("https://api.xposed.info/") {
            content {
                includeGroup("de.robv.android.xposed")
            }
        }
        mavenCentral()
        maven("https://maven.tmpfs.dev/repository/maven-public/") {
            // I have no idea why sometimes jitpack.io is not working for
            // "com.github.plattysoft:Leonids:1746429"
            // So I added this repo as a backup.
            // I have encountered this wired issue twice in 2024.
            // The jitpack.io says "Not found" or "File not found. Build ok".
            content {
                includeGroup("com.github.plattysoft")
            }
        }
    }
}

includeBuild("build-logic")

plugins {
    id("com.gradle.develocity") version "4.0.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
        val isOffline = providers.provider { gradle.startParameter.isOffline }.getOrElse(false)
        val ci = System.getenv("GITHUB_ACTIONS") == "true"
        publishing {
            onlyIf { System.getenv("GITHUB_ACTIONS") == "true" }
            // onlyIf { !isOffline && (it.buildResult.failures.isNotEmpty() || ci) }
        }
    }
}

rootProject.name = "QAuxiliary"
include(
    ":app",
    ":loader:startup",
    ":loader:sbl",
    ":loader:hookapi",
    ":libs:stub",
    ":libs:ksp",
    ":libs:mmkv",
    ":libs:dexkit",
    ":libs:ezXHelper",
    ":libs:xView",
    ":libs:libxposed:api",
    ":libs:libxposed:service",
)
