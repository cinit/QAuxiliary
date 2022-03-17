import com.android.build.gradle.BaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

plugins {
    id("com.android.application") version "7.1.2" apply false
    id("com.android.library") version "7.1.2" apply false
    id("org.jetbrains.kotlin.android") version Version.kotlin apply false
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}

val apiCode by extra(93)
val verCode = Common.getBuildVersionCode(rootProject)
// versionName = major.minor.bugfix.rev.commit
val verName = "1.1.3" + (Common.getGitHeadRefsSuffix(rootProject))
val androidTargetSdkVersion by extra(32)
val androidMinSdkVersion by extra(21)
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

            externalNativeBuild {
                cmake {
                    arguments += listOf(
                        "-DQAUXV_VERSION=$versionName",
                        "-DCMAKE_C_COMPILER_LAUNCHER=ccache",
                        "-DCMAKE_CXX_COMPILER_LAUNCHER=ccache",
                        "-DNDK_CCACHE=ccache"
                    )
                    targets += "qauxv"
                }
            }
        }

        compileOptions {
            sourceCompatibility = Version.java
            targetCompatibility = Version.java
        }
    }

    extensions.findByName("kotlinOptions")?.run {
        println("kotlinOptions")
        (this as KotlinJvmOptions).jvmTarget = Version.java.toString()
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
