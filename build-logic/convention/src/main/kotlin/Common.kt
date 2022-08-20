import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import java.io.File
import java.nio.file.Paths

object Common {
    fun getBuildVersionCode(project: Project): Int {
        val rootProject = project.rootProject
        val projectDir = rootProject.projectDir
        val headFile = File(projectDir, ".git" + File.separator + "HEAD")
        return if (headFile.exists()) {
            FileRepository(rootProject.file(".git")).use { repo ->
                val refId = repo.resolve("HEAD")
                Git(repo).log().add(refId).call().count()
            }
        } else {
            println("WARN: .git/HEAD does NOT exist")
            1
        }
    }

    fun getGitHeadRefsSuffix(project: Project): String {
        val rootProject = project.rootProject
        val headFile = File(rootProject.projectDir, ".git" + File.separator + "HEAD")
        return if (headFile.exists()) {
            FileRepository(rootProject.file(".git")).use { repo ->
                val refId = repo.resolve("HEAD")
                val commitCount = Git(repo).log().add(refId).call().count()
                ".r" + commitCount + "." + refId.name.substring(0, 7)
            }
        } else {
            println("WARN: .git/HEAD does NOT exist")
            ".standalone"
        }
    }

    fun findInPath(executable: String): String? {
        val pathEnv = System.getenv("PATH")
        return pathEnv.split(File.pathSeparator).map { folder ->
            Paths.get("${folder}${File.separator}${executable}${if (OperatingSystem.current().isWindows) ".exe" else ""}")
                .toFile()
        }.firstOrNull { path ->
            path.exists()
        }?.absolutePath
    }

    fun getBuildIdSuffix(): String {
        return try {
            val ciBuildId = System.getenv()["APPCENTER_BUILD_ID"]
            if (ciBuildId != null) ".$ciBuildId"
            else ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun getTimeStamp(): Int {
        return (System.currentTimeMillis() / 1000L).toInt()
    }
}
