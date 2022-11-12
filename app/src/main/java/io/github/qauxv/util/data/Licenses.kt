package io.github.qauxv.util.data

import io.github.qauxv.ui.ResUtils
import io.github.qauxv.util.decodeToDataClass
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object Licenses {

    @Serializable
    data class LibraryLicense(
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

    private var list: List<LibraryLicense>? = null

    fun getAll(): Result<List<LibraryLicense>> = runCatching {
        list?.let { return@runCatching it }
        val content = ResUtils.openAsset(licensesJSON)!!.bufferedReader().use { x -> x.readText() }
        list = content.decodeToDataClass()
        list = list!!.sortedBy {
            it.libraryName
        }
        list!!
    }

    private const val licensesJSON = "open_source_licenses.json"
}
