import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class ReplaceIcon : DefaultTask() {

    @get:Internal
    abstract val projectDir: DirectoryProperty

    @TaskAction
    fun run() {
        val iconsDir = File(projectDir.asFile.get(), "icons")
        val drawableDir= File(projectDir.asFile.get(), "src/main/res/drawable")
        val adapter = File(projectDir.asFile.get(), "src/main/res/drawable-anydpi-v26/icon.xml")

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

        val iconFile = iconFileDirs.random().listFiles()!!.random()
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
