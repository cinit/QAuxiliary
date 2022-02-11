import org.gradle.api.JavaVersion
import java.io.File
import java.util.*

object Version {
    const val kotlin = "1.5.31"
    const val ksp = "1.0.1"
    val java = JavaVersion.VERSION_11

    fun detectNdkVersion(): String? {
        val version = "23.1.7779620"
        val androidHome = System.getenv("ANDROID_HOME") ?: return version
        if (File(androidHome, "ndk/$version").isDirectory) return version
        val versionFile = File(androidHome, "ndk-bundle/source.properties")
        if (!versionFile.isFile) return version
        val versionProperties = Properties()
        versionProperties.load(versionFile.inputStream())
        return versionProperties.getProperty("Pkg.Revision", version)
    }
}
