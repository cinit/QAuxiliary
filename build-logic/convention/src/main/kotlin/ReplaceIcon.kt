import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.random.Random

abstract class ReplaceIcon : Copy() {

    @get:Internal
    abstract val projectDir: DirectoryProperty
    @get:Input
    abstract var commitHash: String

    fun config() {
        into(File(projectDir.asFile.get(), "src/main/res/"))

        val iconsDir = File(projectDir.asFile.get(), "icons")
        val random = Random(commitHash.toSeed())
        // copy new icons
        val iconFileDirs = listOf(
            File(iconsDir, "classic"),
            //File(iconsDir, "ChineseNewYearIcons")
        )
        val iconFile = iconFileDirs.random(random).listFiles()!!.random(random)
        println("Select Icon: $iconFile")
        into("drawable") {
            from(iconFile)
        }
        if (iconFile.isDirectory) {
            into("drawable-anydpi-v26") {
                from(File(iconsDir, "icon.xml"))
            }
        }
    }

    private fun String.toSeed(): Int {
        val md5 = MessageDigest.getInstance("MD5")
        val arrays = md5.digest(this.toByteArray(Charsets.UTF_8))
        return BigInteger(1, arrays).toInt()
    }
}
