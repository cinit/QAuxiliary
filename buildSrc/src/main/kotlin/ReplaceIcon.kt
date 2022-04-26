import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.random.Random

abstract class ReplaceIcon : DefaultTask() {

    @get:Internal
    abstract val projectDir: DirectoryProperty
    @get:Input
    abstract var commitHash: String

    @TaskAction
    fun run() {
        val iconsDir = File(projectDir.asFile.get(), "icons")
        val drawableDir= File(projectDir.asFile.get(), "src/main/res/drawable")
        val adapter = File(projectDir.asFile.get(), "src/main/res/drawable-anydpi-v26/icon.xml")
        val md5 = MessageDigest.getInstance("MD5")
        val arrays = md5.digest(commitHash.toByteArray(Charsets.UTF_8))
        val bigInteger = BigInteger(1, arrays)
        val random = Random(bigInteger.toInt())

        // delete old icons: all files starting with `icon`
        drawableDir
            .listFiles()
            ?.filter { it.isFile && it.name.startsWith("icon") }
            ?.forEach(File::delete)
        if (adapter.exists()) adapter.delete()

        // copy new icons
        val iconFileDirs = listOf(
            File(iconsDir, "classic"),
            //File(iconsDir, "ChineseNewYearIcons")
        )

        val iconFile = iconFileDirs.random(random).listFiles()!!.random(random)
        println("Select Icon: $iconFile")
        if (iconFile.isDirectory) {
            // copy all files in the drawable directory
            iconFile.listFiles()?.forEach {
                val target = File(drawableDir, it.name)
                it.copyTo(target, true)
            }
            File(iconsDir, "icon.xml").copyTo(adapter, true)
        } else {
            iconFile.copyTo(File(drawableDir, "icon.png"), true)
        }
    }
}
