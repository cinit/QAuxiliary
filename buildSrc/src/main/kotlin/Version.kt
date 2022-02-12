import org.gradle.api.JavaVersion
import org.gradle.api.Project
import java.io.File
import java.util.*

object Version {
    const val kotlin = "1.5.31"
    const val ksp = "1.0.1"
    val java = JavaVersion.VERSION_11

    private const val defaultNdkVersion = "23.1.7779620"
    private const val defaultCMakeVersion = "3.18.1"

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
        localProperties.load(localProp.inputStream())
        return localProperties.getProperty(propertyName, null)
    }

    private fun getEnvVariable(name: String): String? {
        return System.getenv(name)
    }
}
