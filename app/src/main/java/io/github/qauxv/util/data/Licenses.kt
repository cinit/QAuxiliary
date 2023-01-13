package io.github.qauxv.util.data

import io.github.qauxv.ui.ResUtils
import io.github.qauxv.util.decodeToDataClass
import kotlinx.serialization.Serializable

object Licenses {

    @Serializable
    data class LibraryLicense(
        val libraryName: String,
        val jumpUrl: String,
        val license: String,
        val author: String,
    )

    val list: List<LibraryLicense> by lazy {
        val content = ResUtils.openAsset(licensesJSON)!!.bufferedReader().use { x -> x.readText() }
        content.decodeToDataClass()
    }

    private const val licensesJSON = "open_source_licenses.json"
}
