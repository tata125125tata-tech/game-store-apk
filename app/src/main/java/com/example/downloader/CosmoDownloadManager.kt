package com.example.downloader

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.example.installer.PackageInstallerManager
import com.example.library.LibraryStore
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.FileType
import com.example.model.LibraryItem
import com.example.settings.SettingsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.URLDecoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class CosmoDownloadManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val settingsManager = SettingsManager.getInstance(context)
    private val notificationHelper = DownloadNotificationHelper(context)
    private val libraryStore = LibraryStore.getInstance(context)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val downloadJobs = ConcurrentHashMap<String, Job>()

    val activeDownloadsCount: StateFlow<Int> = _downloads.map { list ->
        list.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING }
    }.stateIn(scope, SharingStarted.Eagerly, 0)

    init {
        scanExistingFiles()
    }

    private fun scanExistingFiles() {
        scope.launch {
            try {
                val dir = settingsManager.getDownloadDir()
                if (!dir.exists()) dir.mkdirs()
                val files = dir.listFiles() ?: return@launch
                val scanned = mutableListOf<DownloadItem>()

                for (file in files) {
                    if (file.isFile && (file.name.endsWith(".apk", true) || file.name.endsWith(".xapk", true))) {
                        val isXapk = file.name.endsWith(".xapk", true)
                        val type = if (isXapk) FileType.XAPK else FileType.APK
                        var pkg: String? = null
                        var vName: String? = null
                        var vCode: Long? = null
                        var xapkInfo: com.example.model.XapkInfo? = null

                        if (!isXapk) {
                            pkg = PackageInstallerManager.getPackageNameFromApk(context, file)
                            try {
                                val pi = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
                                vName = pi?.versionName
                                vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi?.longVersionCode else pi?.versionCode?.toLong()
                            } catch (e: Exception) {
                                // Ignore
                            }
                        } else {
                            xapkInfo = PackageInstallerManager.parseXapk(file).getOrNull()
                            pkg = xapkInfo?.packageName
                            vName = xapkInfo?.versionName
                            vCode = xapkInfo?.versionCode
                        }

                        val isInstalled = PackageInstallerManager.isPackageInstalled(context, pkg)
                        val title = formatFileTitle(file.name)

                        val item = DownloadItem(
                            id = file.name,
                            title = title,
                            originalUrl = "",
                            fileType = type,
                            localFilePath = file.absolutePath,
                            totalBytes = file.length(),
                            downloadedBytes = file.length(),
                            status = DownloadStatus.COMPLETED,
                            packageName = pkg,
                            isInstalled = isInstalled,
                            xapkInfo = xapkInfo,
                            createdAt = file.lastModified()
                        )
                        scanned.add(item)

                        // Register into persistent LibraryStore
                        libraryStore.addItem(
                            LibraryItem(
                                id = file.name,
                                title = title,
                                packageName = pkg,
                                versionName = vName,
                                versionCode = vCode,
                                iconUrl = null,
                                localFilePath = file.absolutePath,
                                isXapk = isXapk,
                                hasObb = (xapkInfo?.totalObbs ?: 0) > 0,
                                hasSplitApks = (xapkInfo?.totalApks ?: 1) > 1,
                                totalApks = xapkInfo?.totalApks ?: 1,
                                fileSizeBytes = file.length(),
                                downloadedAt = file.lastModified()
                            )
                        )
                    }
                }

                if (scanned.isNotEmpty()) {
                    _downloads.value = scanned.sortedByDescending { it.createdAt }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error scanning existing downloads", e)
            }
        }
    }

    fun startDownload(
        url: String,
        suggestedTitle: String? = null,
        iconUrl: String? = null
    ): String {
        val fileType = detectFileTypeFromUrl(url)
        val fileName = sanitizeFileName(url, suggestedTitle, fileType)
        val targetFile = File(settingsManager.getDownloadDir(), fileName)

        val existing = _downloads.value.find { it.localFilePath == targetFile.absolutePath || it.originalUrl == url }
        val id = existing?.id ?: UUID.randomUUID().toString()

        val downloadItem = DownloadItem(
            id = id,
            title = suggestedTitle ?: formatFileTitle(fileName),
            originalUrl = url,
            fileType = fileType,
            localFilePath = targetFile.absolutePath,
            totalBytes = existing?.totalBytes ?: -1L,
            downloadedBytes = if (targetFile.exists()) targetFile.length() else 0L,
            status = DownloadStatus.PENDING,
            iconUrl = iconUrl,
            createdAt = System.currentTimeMillis()
        )

        updateOrAddItem(downloadItem)
        notificationHelper.updateProgressNotification(downloadItem)
        enqueueDownload(downloadItem, targetFile)

        return id
    }

    fun resumeDownload(id: String) {
        val item = _downloads.value.find { it.id == id } ?: return
        if (item.status == DownloadStatus.DOWNLOADING) return

        val file = File(item.localFilePath)
        val updated = item.copy(status = DownloadStatus.PENDING, errorMessage = null)
        updateOrAddItem(updated)
        notificationHelper.updateProgressNotification(updated)
        enqueueDownload(updated, file)
    }

    fun pauseDownload(id: String) {
        downloadJobs[id]?.cancel()
        downloadJobs.remove(id)

        updateItem(id) { item ->
            val paused = item.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L)
            notificationHelper.showPausedNotification(paused)
            paused
        }
    }

    fun cancelDownload(id: String, deleteFile: Boolean = true) {
        downloadJobs[id]?.cancel()
        downloadJobs.remove(id)
        notificationHelper.cancelNotification(id)

        val item = _downloads.value.find { it.id == id }
        if (deleteFile && item != null) {
            try {
                val f = File(item.localFilePath)
                if (f.exists()) f.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting file for cancelled download $id", e)
            }
            _downloads.value = _downloads.value.filter { it.id != id }
            libraryStore.removeItem(id)
        } else {
            updateItem(id) { it.copy(status = DownloadStatus.CANCELLED, speedBytesPerSec = 0L) }
        }
    }

    fun deleteDownload(id: String) {
        cancelDownload(id, deleteFile = true)
    }

    fun markPackageInstalled(packageName: String) {
        _downloads.value = _downloads.value.map { item ->
            if (item.packageName == packageName) {
                item.copy(isInstalled = true)
            } else {
                item
            }
        }
        libraryStore.refreshState()
    }

    fun refreshInstalledStatus() {
        _downloads.value = _downloads.value.map { item ->
            if (item.packageName != null) {
                val installed = PackageInstallerManager.isPackageInstalled(context, item.packageName)
                item.copy(isInstalled = installed)
            } else {
                item
            }
        }
        libraryStore.refreshState()
    }

    private fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun enqueueDownload(item: DownloadItem, file: File) {
        downloadJobs[item.id]?.cancel()

        val job = scope.launch {
            var existingLength = if (file.exists()) file.length() else 0L

            try {
                // Check Wi-Fi only constraint
                if (settingsManager.downloadWifiOnly.value && !isWifiConnected()) {
                    throw IllegalStateException("Download blocked: Wi-Fi only is enabled in Settings")
                }

                updateItem(item.id) { it.copy(status = DownloadStatus.DOWNLOADING, errorMessage = null) }

                // Build request with standard mobile browser headers to avoid HTTP 403
                val requestBuilder = Request.Builder()
                    .url(item.originalUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "*/*")
                    .header("Accept-Encoding", "identity")
                    .header("Referer", "https://cosmo-game.pages.dev/")

                if (existingLength > 0) {
                    requestBuilder.header("Range", "bytes=$existingLength-")
                }

                val request = requestBuilder.build()
                val response = executeWithRedirectHandling(request)

                // Real HTTP error checking - no fake success or fake progress!
                when (response.code) {
                    401 -> throw IllegalStateException("HTTP 401 Unauthorized: Authentication required by server")
                    403 -> throw IllegalStateException("HTTP 403 Forbidden: Access denied by download host")
                    404 -> throw IllegalStateException("HTTP 404 Not Found: Download file does not exist")
                    416 -> {
                        // Range Not Satisfiable: File may be already fully downloaded or changed
                        file.delete()
                        existingLength = 0L
                        val cleanRequest = requestBuilder.removeHeader("Range").build()
                        val retryResponse = executeWithRedirectHandling(cleanRequest)
                        if (!retryResponse.isSuccessful) {
                            throw IllegalStateException("HTTP ${retryResponse.code}: ${retryResponse.message}")
                        }
                        handleSuccessfulResponse(item.id, retryResponse, file, 0L)
                        return@launch
                    }
                    in 400..599 -> throw IllegalStateException("HTTP ${response.code}: ${response.message}")
                }

                if (!response.isSuccessful && response.code != 206) {
                    throw IllegalStateException("HTTP ${response.code}: ${response.message}")
                }

                handleSuccessfulResponse(item.id, response, file, existingLength)

            } catch (e: CancellationException) {
                Log.d(TAG, "Download job cancelled for ${item.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Download error for ${item.id}", e)
                val realError = e.message ?: "Download connection failed"
                val failedItem = _downloads.value.find { it.id == item.id }?.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = realError,
                    speedBytesPerSec = 0L
                ) ?: item.copy(status = DownloadStatus.FAILED, errorMessage = realError)

                updateItem(item.id) { failedItem }
                notificationHelper.showDownloadFailedNotification(failedItem)
            } finally {
                downloadJobs.remove(item.id)
            }
        }

        downloadJobs[item.id] = job
    }

    /**
     * Executes the HTTP request and handles 301, 302, 303, 307, 308 redirects with header preservation.
     */
    private fun executeWithRedirectHandling(initialRequest: Request): Response {
        var currentRequest = initialRequest
        var redirectCount = 0
        val maxRedirects = 10

        while (redirectCount < maxRedirects) {
            val response = okHttpClient.newCall(currentRequest).execute()
            val code = response.code

            // Handle HTTP 301, 302, 303, 307, 308
            if (code in listOf(301, 302, 303, 307, 308)) {
                val location = response.header("Location")
                response.close()

                if (location.isNullOrBlank()) {
                    return response
                }

                val newUrl = currentRequest.url.resolve(location) ?: return response
                val newRequestBuilder = currentRequest.newBuilder().url(newUrl)

                if (code == 303) {
                    newRequestBuilder.method("GET", null)
                }

                currentRequest = newRequestBuilder.build()
                redirectCount++
            } else {
                return response
            }
        }

        return okHttpClient.newCall(currentRequest).execute()
    }

    private suspend fun handleSuccessfulResponse(
        id: String,
        response: Response,
        initialFile: File,
        existingBytes: Long
    ) {
        val body = response.body ?: throw IllegalStateException("Empty response body from server")
        val finalUrl = response.request.url.toString()
        val contentDisposition = response.header("Content-Disposition")
        val contentType = response.header("Content-Type")

        // Detect correct file extension (.xapk vs .apk) from final URL and headers
        val detectedFileType = detectFileType(finalUrl, contentDisposition, contentType)
        var actualFile = initialFile

        // Adjust file extension if needed
        val expectedExt = if (detectedFileType == FileType.XAPK) ".xapk" else ".apk"
        if (!actualFile.name.endsWith(expectedExt, ignoreCase = true)) {
            val newName = actualFile.name.substringBeforeLast(".") + expectedExt
            val newFile = File(actualFile.parentFile, newName)
            if (actualFile.exists()) {
                actualFile.renameTo(newFile)
            }
            actualFile = newFile
        }

        val contentLength = body.contentLength()
        val totalBytes = if (contentLength > 0) {
            existingBytes + contentLength
        } else {
            -1L
        }

        val append = existingBytes > 0 && response.code == 206
        val raf = RandomAccessFile(actualFile, "rw")
        if (append) {
            raf.seek(existingBytes)
        } else {
            raf.setLength(0)
        }

        var downloaded = existingBytes
        var lastTime = System.currentTimeMillis()
        var lastBytes = downloaded
        var currentSpeed = 0L
        var lastNotificationTime = System.currentTimeMillis()

        val buffer = ByteArray(8192)
        val inStream: InputStream = body.byteStream()

        inStream.use { input ->
            raf.use { output ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloaded += bytesRead

                    val now = System.currentTimeMillis()
                    val timeDelta = now - lastTime
                    if (timeDelta >= 500) {
                        val bytesDelta = downloaded - lastBytes
                        currentSpeed = ((bytesDelta * 1000.0) / timeDelta).toLong()
                        lastTime = now
                        lastBytes = downloaded

                        val updated = _downloads.value.find { it.id == id }?.copy(
                            localFilePath = actualFile.absolutePath,
                            fileType = detectedFileType,
                            totalBytes = totalBytes,
                            downloadedBytes = downloaded,
                            speedBytesPerSec = currentSpeed
                        )

                        if (updated != null) {
                            updateItem(id) { updated }

                            // Real-time notification update throttled to ~750ms
                            if (now - lastNotificationTime >= 750) {
                                notificationHelper.updateProgressNotification(updated)
                                lastNotificationTime = now
                            }
                        }
                    }
                }
            }
        }

        // Complete Verification & Metadata Extraction
        var parsedPkg: String? = null
        var versionName: String? = null
        var versionCode: Long? = null
        var xapkDetails: com.example.model.XapkInfo? = null

        val currentItem = _downloads.value.find { it.id == id }
        if (detectedFileType == FileType.APK) {
            parsedPkg = PackageInstallerManager.getPackageNameFromApk(context, actualFile)
            try {
                val pi = context.packageManager.getPackageArchiveInfo(actualFile.absolutePath, 0)
                versionName = pi?.versionName
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi?.longVersionCode else pi?.versionCode?.toLong()
            } catch (e: Exception) {
                // Ignore
            }
        } else {
            xapkDetails = PackageInstallerManager.parseXapk(actualFile).getOrNull()
            parsedPkg = xapkDetails?.packageName
            versionName = xapkDetails?.versionName
            versionCode = xapkDetails?.versionCode
        }

        val isInstalled = PackageInstallerManager.isPackageInstalled(context, parsedPkg)

        val completedItem = (currentItem ?: DownloadItem(
            id = id,
            title = formatFileTitle(actualFile.name),
            originalUrl = "",
            fileType = detectedFileType,
            localFilePath = actualFile.absolutePath
        )).copy(
            status = DownloadStatus.COMPLETED,
            fileType = detectedFileType,
            localFilePath = actualFile.absolutePath,
            totalBytes = actualFile.length(),
            downloadedBytes = actualFile.length(),
            speedBytesPerSec = 0L,
            packageName = parsedPkg,
            isInstalled = isInstalled,
            xapkInfo = xapkDetails
        )

        updateItem(id) { completedItem }

        // Automatically persist into My Library
        val libraryItem = LibraryItem(
            id = id,
            title = completedItem.title,
            packageName = parsedPkg,
            versionName = versionName,
            versionCode = versionCode,
            iconUrl = completedItem.iconUrl,
            localFilePath = actualFile.absolutePath,
            isXapk = detectedFileType == FileType.XAPK,
            hasObb = (xapkDetails?.totalObbs ?: 0) > 0,
            hasSplitApks = (xapkDetails?.totalApks ?: 1) > 1,
            totalApks = xapkDetails?.totalApks ?: 1,
            fileSizeBytes = actualFile.length(),
            downloadedAt = System.currentTimeMillis(),
            originalUrl = completedItem.originalUrl
        )
        libraryStore.addItem(libraryItem)

        // Show real Android system notification: "Download complete" with "Open My Library" action
        notificationHelper.showDownloadCompleteNotification(completedItem)
    }

    private fun updateItem(id: String, update: (DownloadItem) -> DownloadItem) {
        _downloads.value = _downloads.value.map { item ->
            if (item.id == id) update(item) else item
        }
    }

    private fun updateOrAddItem(newItem: DownloadItem) {
        val current = _downloads.value
        val index = current.indexOfFirst { it.id == newItem.id }
        if (index >= 0) {
            _downloads.value = current.mapIndexed { i, it -> if (i == index) newItem else it }
        } else {
            _downloads.value = listOf(newItem) + current
        }
    }

    private fun detectFileTypeFromUrl(url: String): FileType {
        val cleanUrl = url.substringBefore("?").lowercase()
        return when {
            cleanUrl.endsWith(".xapk") -> FileType.XAPK
            cleanUrl.endsWith(".apk") -> FileType.APK
            url.contains(".xapk", ignoreCase = true) -> FileType.XAPK
            url.contains(".apk", ignoreCase = true) -> FileType.APK
            else -> FileType.APK
        }
    }

    private fun detectFileType(url: String, contentDisposition: String?, contentType: String?): FileType {
        // 1. Check Content-Disposition filename
        if (!contentDisposition.isNullOrBlank()) {
            val filename = extractFilenameFromContentDisposition(contentDisposition)
            if (filename != null) {
                if (filename.endsWith(".xapk", ignoreCase = true)) return FileType.XAPK
                if (filename.endsWith(".apk", ignoreCase = true)) return FileType.APK
            }
        }

        // 2. Check URL path
        val cleanUrl = url.substringBefore("?").lowercase()
        if (cleanUrl.endsWith(".xapk") || url.contains(".xapk", ignoreCase = true)) return FileType.XAPK
        if (cleanUrl.endsWith(".apk") || url.contains(".apk", ignoreCase = true)) return FileType.APK

        return FileType.APK
    }

    private fun extractFilenameFromContentDisposition(contentDisposition: String): String? {
        try {
            val pattern = Pattern.compile("filename[*]?=['\"]?(?:UTF-8'')?([^;'\"]+)", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(contentDisposition)
            if (matcher.find()) {
                val raw = matcher.group(1) ?: return null
                return URLDecoder.decode(raw, "UTF-8")
            }
        } catch (e: Exception) {
            // Ignore
        }
        return null
    }

    private fun sanitizeFileName(url: String, title: String?, fileType: FileType): String {
        return try {
            val decoded = URLDecoder.decode(url, "UTF-8")
            val baseName = decoded.substringBefore("?").substringAfterLast("/")
            if (baseName.isNotBlank() && (baseName.endsWith(".apk", true) || baseName.endsWith(".xapk", true))) {
                baseName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            } else {
                val safeTitle = (title ?: "game_download").replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                val ext = if (fileType == FileType.XAPK) ".xapk" else ".apk"
                "${safeTitle}_${System.currentTimeMillis() % 10000}$ext"
            }
        } catch (e: Exception) {
            val ext = if (fileType == FileType.XAPK) ".xapk" else ".apk"
            "download_${System.currentTimeMillis()}$ext"
        }
    }

    private fun formatFileTitle(fileName: String): String {
        return fileName
            .removeSuffix(".apk")
            .removeSuffix(".xapk")
            .replace("_", " ")
            .replace("-", " ")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    companion object {
        private const val TAG = "CosmoDownloadMgr"

        @Volatile
        private var INSTANCE: CosmoDownloadManager? = null

        fun getInstance(context: Context): CosmoDownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CosmoDownloadManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
