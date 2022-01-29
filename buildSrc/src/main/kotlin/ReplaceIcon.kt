import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.*

abstract class ReplaceIcon : DefaultTask() {
    @TaskAction
    fun run() {
        val iconsDir = File(project.projectDir, "icons")
        val iconFileDirs = listOf(
            File(iconsDir, "MiStyleIcons"),
            File(iconsDir, "classic"),
            //File(projectDir ,"ChineseNewYearIcons")
        )
        val fileCount = iconFileDirs.fold(0) { i: Int, file: File ->
            i + file.listFiles()!!.size
        }
        var number = Random().nextInt(fileCount)

        //for (aNumber in 0..fileCount) {
        //var number = aNumber
        var iconFile: File? = null
        for (iconFileDir in iconFileDirs) {
            if (number < iconFileDir.listFiles()!!.size) {
                iconFile = iconFileDir.listFiles()!![number]
                break
            }
            number -= iconFileDir.listFiles()!!.size
        }
        println("Select Icon: $iconFile")
        iconFile!!.copyTo(File(project.projectDir, "src/main/res/drawable/icon.png"), true)
        //}
    }
}
