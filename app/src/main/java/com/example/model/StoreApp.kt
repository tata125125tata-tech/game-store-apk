package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StoreApp(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "dev_name") val devName: String? = null,
    @Json(name = "version") val version: String? = null,
    @Json(name = "size") val size: String? = null,
    @Json(name = "main_category_id") val mainCategoryId: Int? = null,
    @Json(name = "sub_category_id") val subCategoryId: Int? = null,
    @Json(name = "icon_url") val iconUrl: String? = null,
    @Json(name = "screenshots") val screenshots: Any? = null,
    @Json(name = "android_download_url") val androidDownloadUrl: String? = null,
    @Json(name = "ios_download_url") val iosDownloadUrl: String? = null,
    @Json(name = "windows_download_url") val windowsDownloadUrl: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "download_count") val downloadCount: Any? = null,
    @Json(name = "real_download_count") val realDownloadCount: Any? = null,
    @Json(name = "rating_avg") val ratingAvg: Double? = 0.0,
    @Json(name = "rating_count") val ratingCount: Int? = 0
) {
    val parsedScreenshots: List<String>
        get() {
            return when (val raw = screenshots) {
                is List<*> -> raw.filterIsInstance<String>()
                is String -> raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                else -> emptyList()
            }
        }

    val displayDownloads: String
        get() {
            val countNum = (realDownloadCount?.toString()?.toDoubleOrNull()
                ?: downloadCount?.toString()?.toDoubleOrNull() ?: 0.0).toLong()
            return when {
                countNum >= 1_000_000 -> "${countNum / 1_000_000}M+"
                countNum >= 1_000 -> "${countNum / 1_000}K+"
                countNum > 0 -> "$countNum+"
                else -> "New"
            }
        }

    val isXapk: Boolean
        get() = androidDownloadUrl?.contains(".xapk", ignoreCase = true) == true
}

@JsonClass(generateAdapter = true)
data class StoreCategory(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "parent_id") val parentId: Int? = null,
    @Json(name = "icon") val icon: String? = null
)
