package com.example.model

import com.squareup.moshi.JsonClass
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class LibraryItem(
    val id: String,
    val title: String,
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
    val iconUrl: String?,
    val localFilePath: String,
    val isXapk: Boolean,
    val hasObb: Boolean = false,
    val hasSplitApks: Boolean = false,
    val totalApks: Int = 1,
    val fileSizeBytes: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis(),
    val originalUrl: String? = null
) {
    val fileExists: Boolean
        get() = try {
            File(localFilePath).exists()
        } catch (e: Exception) {
            false
        }

    val formattedSize: String
        get() = DownloadItem.formatFileSize(fileSizeBytes)

    val formattedDate: String
        get() = try {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            sdf.format(Date(downloadedAt))
        } catch (e: Exception) {
            "Downloaded"
        }

    val typeLabel: String
        get() = if (isXapk) "XAPK" else "APK"

    val packageDetailsLabel: String
        get() = when {
            isXapk && hasObb && hasSplitApks -> "XAPK with OBB data and Split APKs ($totalApks APKs)"
            isXapk && hasObb -> "XAPK with OBB expansion data"
            isXapk && hasSplitApks -> "XAPK with Split APKs ($totalApks APKs)"
            isXapk -> "XAPK Package"
            else -> "Standard Android APK"
        }
}
