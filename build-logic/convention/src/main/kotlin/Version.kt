import org.gradle.api.JavaVersion
import org.gradle.api.Project
import java.io.File
import java.util.Properties

object Version {
    const val kotlin = "1.7.20"
    const val ksp = "1.0.6"
    val java = JavaVersion.VERSION_11

    const val compileSdkVersion = 33
    const val buildToolsVersion = "33.0.0"
    const val minSdk = 24
    const val targetSdk = 33
    const val versionName = "1.3.4"

    private const val defaultNdkVersion = "25.0.8775105"
    private const val defaultCMakeVersion = "3.22.1"

    fun getNdkVersion(project: Project): String {
        val prop = getLocalProperty(project, "qauxv.override.ndk.version")
        val env = getEnvVariable("QAUXV_OVERRIDE_NDK_VERSION")
        if (!prop.isNullOrEmpty() && !env.isNullOrEmpty()) {
            throw IllegalStateException("Cannot set both QAUXV_OVERRIDE_NDK_VERSION and qauxv.override.ndk.version")
        }
        return prop ?: env ?: defaultNdkVersion
    }

    fun getCMakeVersion(project: Project): String {
        val prop = getLocalProperty(project, "qauxv.override.cmake.version")
        val env = getEnvVariable("QAUXV_OVERRIDE_CMAKE_VERSION")
        if (!prop.isNullOrEmpty() && !env.isNullOrEmpty()) {
            throw IllegalStateException("Cannot set both QAUXV_OVERRIDE_CMAKE_VERSION and qauxv.override.cmake.version")
        }
        return prop ?: env ?: defaultCMakeVersion
    }

    private fun getLocalProperty(project: Project, propertyName: String): String? {
        val rootProject = project.rootProject
        val localProp = File(rootProject.projectDir, "local.properties")
        if (!localProp.exists()) {
            return null
        }
        val localProperties = Properties()
        localProp.inputStream().use {
            localProperties.load(it)
        }
        return localProperties.getProperty(propertyName, null)
    }

    private fun getEnvVariable(name: String): String? {
        return System.getenv(name)
    }
}
