import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class ReplaceIcon : DefaultTask() {
    @TaskAction
    fun run() {
        val iconsDir = File(project.projectDir, "icons")
        val drawableDir= File(project.projectDir, "src/main/res/drawable")
        // delete old icons
        drawableDir
                .listFiles()
                ?.filter { it.isFile && it.name.startsWith("icon.") }
                ?.forEach(File::delete)

        // copy new icons
        val iconFileDirs = listOf(
//            File(iconsDir, "MiStyleIcons"),
            File(iconsDir, "classic"),
            File(iconsDir, "ChineseNewYearIcons")
        )

        val iconFile: File? = iconFileDirs.random().listFiles()?.random()
        val suffix = iconFile?.name?.substringAfterLast(".") ?: "png"
        println("Select Icon: $iconFile")
        iconFile!!.copyTo(File(drawableDir, "icon.$suffix"), true)
    }
}
