package com.example.model

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class FileType {
    APK,
    XAPK,
    UNKNOWN
}

data class XapkComponent(
    val entryName: String,
    val isApk: Boolean,
    val isObb: Boolean,
    val sizeBytes: Long
)

data class XapkInfo(
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
    val minSdkVersion: Int?,
    val components: List<XapkComponent> = emptyList(),
    val totalApks: Int = 0,
    val totalObbs: Int = 0,
    val totalUncompressedSize: Long = 0L
)

data class DownloadItem(
    val id: String,
    val title: String,
    val originalUrl: String,
    val fileType: FileType,
    val localFilePath: String,
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val speedBytesPerSec: Long = 0L,
    val errorMessage: String? = null,
    val packageName: String? = null,
    val iconUrl: String? = null,
    val isInstalled: Boolean = false,
    val xapkInfo: XapkInfo? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()

    val formattedDownloaded: String
        get() = formatFileSize(downloadedBytes)

    val formattedTotal: String
        get() = if (totalBytes > 0) formatFileSize(totalBytes) else "Unknown"

    val formattedSpeed: String
        get() = if (status == DownloadStatus.DOWNLOADING && speedBytesPerSec > 0) {
            "${formatFileSize(speedBytesPerSec)}/s"
        } else {
            ""
        }

    companion object {
        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}
