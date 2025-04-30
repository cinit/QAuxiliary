import org.gradle.api.JavaVersion
import org.gradle.api.Project
import java.io.File
import java.util.Properties

object Version {
    val java = JavaVersion.VERSION_11

    const val compileSdkVersion = 35
    val buildToolsVersion = findBuildToolsVersion()
    const val minSdk = 24
    const val targetSdk = 35
    const val versionName = "1.5.8"

    private const val defaultNdkVersion = "27.0.12077973"

    // LSPlant requires CMake 3.28.0+ to build
    private const val defaultCMakeVersion = "3.31.0"

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

    fun getNinjaPathOrNull(project: Project): String? {
        val prop = getLocalProperty(project, "qauxv.override.ninja.path")
        val env = getEnvVariable("QAUXV_OVERRIDE_NINJA_PATH")
        if (!prop.isNullOrEmpty() && !env.isNullOrEmpty()) {
            throw IllegalStateException("Cannot set both QAUXV_OVERRIDE_NINJA_PATH and qauxv.override.ninja.path")
        }
        return prop ?: env
    }

    fun getLocalProperty(project: Project, propertyName: String): String? {
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

    private fun findBuildToolsVersion(): String {
        val defaultBuildToolsVersion = "35.0.0" // AGP 8.2.0 need Build Tools 34.0.0
        return File(System.getenv("ANDROID_HOME"), "build-tools").listFiles()?.filter { it.isDirectory }?.maxOfOrNull { it.name }
            ?.also { println("Using build tools version $it") }
            ?: defaultBuildToolsVersion
    }
}
