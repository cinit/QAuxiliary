package io.github.qauxv.util.data

import io.github.qauxv.ui.ResUtils
import io.github.qauxv.util.decodeToDataClass
import kotlinx.serialization.Serializable

object Licenses {

    @Serializable
    data class LibraryLicense(
        val license: String? = null,
        val normalizedLicense: String,
        val url: String? = null,
        val copyrightHolder: String = "",
        val libraryName: String,
    )

    private var parsed: Map<String, List<LibraryLicense>>? = null
    private var list: List<LibraryLicense>? = null

    fun getAll(): Result<List<LibraryLicense>> = runCatching {
        list?.let { return@runCatching it }
        val content = ResUtils.openAsset(licensesJSON)!!.bufferedReader().use { x -> x.readText() }
        parsed = content.decodeToDataClass()
        parsed?.let {
            list = it["libraries"]!!
                .filter { x -> x.url != null && x.license != null }
                .sortedBy { x -> x.libraryName }
        }
        list!!
    }

    private const val licensesJSON = "licenses.json"
}
