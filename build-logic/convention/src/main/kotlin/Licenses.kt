import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object Licenses {
    @Serializable
    data class RawLibraryLicense(
        @SerialName("project")
        val libraryName: String,
        val developers: List<String>,
        val url: String?,
        val licenses: List<License>
    ) {
        @Serializable
        data class License(
            val license: String,
            val license_url: String,
        )

        val jumpUrl = url ?: licenses.first().license_url
        val license = licenses.first().license
        val author = developers.joinToString(" & ")
    }

    @Serializable
    data class LibraryLicense(
        val libraryName: String,
        val jumpUrl: String,
        val license: String,
        val author: String,
    )

    fun transform(input: String): String {
        val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
        val libs = json.decodeFromString<List<RawLibraryLicense>>(input)
            .sortedBy { it.libraryName }
            .map {
                LibraryLicense(
                    libraryName = it.libraryName,
                    jumpUrl = it.jumpUrl,
                    author = it.author,
                    license = it.license
                )
            }
        return json.encodeToString(libs)
    }
}
