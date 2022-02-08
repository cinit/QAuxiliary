import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.gradle.api.Project
import java.io.File

object Common {

    @JvmStatic
    fun getBuildVersionCode(project: Project): Int {
        // .git/HEAD描述当前目录所指向的分支信息，内容示例："ref: refs/heads/master\n"
        val headFile = File(project.rootProject.projectDir, ".git" + File.separator + "HEAD")
        if (headFile.exists()) {
            var commitHash: String
            val strings = headFile.readText(Charsets.UTF_8).split(" ")
            if (strings.size > 1) {
                val refFilePath = ".git" + File.separator + strings[1];
                // 根据HEAD读取当前指向的hash值，路径示例为：".git/refs/heads/master"
                val refFile = File(project.rootProject.projectDir, refFilePath.replace("\n", "").replace("\r", ""));
                // 索引文件内容为hash值+"\n"，
                commitHash = refFile.readText(Charsets.UTF_8)
            } else {
                commitHash = strings[0]
            }
            commitHash = commitHash.trim()
            val repo = FileRepository(project.rootProject.file(".git"))
            val refId = repo.resolve(commitHash)
            val iterator = Git(repo).log().add(refId).call().iterator()
            var commitCount = 0
            while (iterator.hasNext()) {
                commitCount++
                iterator.next()
            }
            repo.close()
            return commitCount
        } else {
            println("WARN: .git/HEAD does NOT exist")
            return 1
        }
    }

    @JvmStatic
    fun getGitHeadRefsSuffix(project: Project): String {
        // .git/HEAD描述当前目录所指向的分支信息，内容示例："ref: refs/heads/master\n"
        val headFile = File(project.rootProject.projectDir, ".git" + File.separator + "HEAD")
        if (headFile.exists()) {
            var commitHash: String
            val strings = headFile.readText(Charsets.UTF_8).split(" ")
            if (strings.size > 1) {
                val refFilePath = ".git" + File.separator + strings[1];
                // 根据HEAD读取当前指向的hash值，路径示例为：".git/refs/heads/master"
                val refFile = File(project.rootProject.projectDir, refFilePath.replace("\n", "").replace("\r", ""));
                // 索引文件内容为hash值+"\n"，
                commitHash = refFile.readText(Charsets.UTF_8)
            } else {
                commitHash = strings[0]
            }
            commitHash = commitHash.trim()
            val repo = FileRepository(project.rootProject.file(".git"))
            val refId = repo.resolve(commitHash)
            val iterator = Git(repo).log().add(refId).call().iterator()
            var commitCount = 0
            while (iterator.hasNext()) {
                commitCount++
                iterator.next()
            }
            repo.close()
            return ".r" + commitCount + "." + commitHash.substring(0, 7)
        } else {
            println("WARN: .git/HEAD does NOT exist")
            return ".standalone"
        }
    }

    @JvmStatic
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

    @JvmStatic
    fun getTimeStamp(): Int {
        return (System.currentTimeMillis() / 1000L).toInt()
    }
}
