import com.android.build.gradle.BaseExtension

plugins {
    id("com.android.application") version "7.1.3" apply false
    id("com.android.library") version "7.1.3" apply false
    id("org.jetbrains.kotlin.android") version Version.kotlin apply false
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}

val apiCode by extra(93)
val verCode = Common.getBuildVersionCode(rootProject)
// versionName = major.minor.bugfix.rev.commit
val verName = "1.2.1" + (Common.getGitHeadRefsSuffix(rootProject))
val androidTargetSdkVersion by extra(32)
val androidMinSdkVersion by extra(24)
val androidCompileSdkVersion by extra(32)
val androidBuildToolsVersion by extra("32.0.0")
val androidCompileNdkVersion = Version.getNdkVersion(project)

fun Project.configureBaseExtension() {
    extensions.findByType(BaseExtension::class)?.run {
        compileSdkVersion(androidCompileSdkVersion)
        ndkVersion = androidCompileNdkVersion
        buildToolsVersion = androidBuildToolsVersion

        defaultConfig {
            minSdk = androidMinSdkVersion
            targetSdk = androidTargetSdkVersion
            versionCode = verCode
            versionName = verName
        }

        compileOptions {
            sourceCompatibility = Version.java
            targetCompatibility = Version.java
        }

        packagingOptions.jniLibs.useLegacyPackaging = false
    }
}

subprojects {
    plugins.withId("com.android.application") {
        configureBaseExtension()
    }
    plugins.withId("com.android.library") {
        configureBaseExtension()
    }
}
