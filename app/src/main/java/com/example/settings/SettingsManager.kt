package com.example.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class ThemeMode(val title: String) {
    SYSTEM("System Default"),
    DARK("Dark"),
    LIGHT("Light")
}

class SettingsManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cosmo_settings_prefs", Context.MODE_PRIVATE)

    // Theme
    private val _themeMode = MutableStateFlow(
        try {
            ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // Downloads
    private val _autoDeleteApk = MutableStateFlow(prefs.getBoolean(KEY_AUTO_DELETE, false))
    val autoDeleteApk: StateFlow<Boolean> = _autoDeleteApk.asStateFlow()

    private val _autoRetry = MutableStateFlow(prefs.getBoolean(KEY_AUTO_RETRY, true))
    val autoRetry: StateFlow<Boolean> = _autoRetry.asStateFlow()

    private val _downloadNotificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_DOWNLOAD_NOTIF, true))
    val downloadNotificationsEnabled: StateFlow<Boolean> = _downloadNotificationsEnabled.asStateFlow()

    // Network
    private val _downloadWifiOnly = MutableStateFlow(prefs.getBoolean(KEY_WIFI_ONLY, false))
    val downloadWifiOnly: StateFlow<Boolean> = _downloadWifiOnly.asStateFlow()

    private val _allowMobileData = MutableStateFlow(prefs.getBoolean(KEY_ALLOW_MOBILE, true))
    val allowMobileData: StateFlow<Boolean> = _allowMobileData.asStateFlow()

    // Browser / WebView
    private val _javaScriptEnabled = MutableStateFlow(prefs.getBoolean(KEY_JS_ENABLED, true))
    val javaScriptEnabled: StateFlow<Boolean> = _javaScriptEnabled.asStateFlow()

    private val _domStorageEnabled = MutableStateFlow(prefs.getBoolean(KEY_DOM_STORAGE, true))
    val domStorageEnabled: StateFlow<Boolean> = _domStorageEnabled.asStateFlow()

    private val _deepLinkEnabled = MutableStateFlow(prefs.getBoolean(KEY_DEEP_LINK, true))
    val deepLinkEnabled: StateFlow<Boolean> = _deepLinkEnabled.asStateFlow()

    // Installation
    private val _autoOpenAfterInstall = MutableStateFlow(prefs.getBoolean(KEY_AUTO_OPEN, false))
    val autoOpenAfterInstall: StateFlow<Boolean> = _autoOpenAfterInstall.asStateFlow()

    private val _xapkAutoExtract = MutableStateFlow(prefs.getBoolean(KEY_XAPK_AUTO_EXTRACT, true))
    val xapkAutoExtract: StateFlow<Boolean> = _xapkAutoExtract.asStateFlow()

    // Setters
    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setAutoDeleteApk(value: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_DELETE, value).apply()
        _autoDeleteApk.value = value
    }

    fun setAutoRetry(value: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RETRY, value).apply()
        _autoRetry.value = value
    }

    fun setDownloadNotificationsEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_DOWNLOAD_NOTIF, value).apply()
        _downloadNotificationsEnabled.value = value
    }

    fun setDownloadWifiOnly(value: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()
        _downloadWifiOnly.value = value
    }

    fun setAllowMobileData(value: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_MOBILE, value).apply()
        _allowMobileData.value = value
    }

    fun setJavaScriptEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_JS_ENABLED, value).apply()
        _javaScriptEnabled.value = value
    }

    fun setDomStorageEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_DOM_STORAGE, value).apply()
        _domStorageEnabled.value = value
    }

    fun setDeepLinkEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_DEEP_LINK, value).apply()
        _deepLinkEnabled.value = value
    }

    fun setAutoOpenAfterInstall(value: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_OPEN, value).apply()
        _autoOpenAfterInstall.value = value
    }

    fun setXapkAutoExtract(value: Boolean) {
        prefs.edit().putBoolean(KEY_XAPK_AUTO_EXTRACT, value).apply()
        _xapkAutoExtract.value = value
    }

    // Storage Management
    fun getDownloadDir(): File {
        val external = context.getExternalFilesDir("downloads")
        return external ?: File(context.filesDir, "downloads").apply { mkdirs() }
    }

    fun getDownloadedFilesSize(): Long {
        return calculateDirSize(getDownloadDir())
    }

    fun getAppCacheSize(): Long {
        var size = calculateDirSize(context.cacheDir)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            size += calculateDirSize(context.codeCacheDir)
        }
        val externalCache = context.externalCacheDir
        if (externalCache != null) {
            size += calculateDirSize(externalCache)
        }
        return size
    }

    fun getAvailableStorageBytes(): Long {
        return try {
            val stat = StatFs(context.filesDir.path)
            stat.availableBytes
        } catch (e: Exception) {
            0L
        }
    }

    fun getTotalStorageBytes(): Long {
        return try {
            val stat = StatFs(context.filesDir.path)
            stat.totalBytes
        } catch (e: Exception) {
            0L
        }
    }

    fun clearAppCache(): Long {
        val initialSize = getAppCacheSize()
        deleteDirContents(context.cacheDir)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            deleteDirContents(context.codeCacheDir)
        }
        context.externalCacheDir?.let { deleteDirContents(it) }
        return initialSize
    }

    fun clearDownloadedFiles(): Boolean {
        val dir = getDownloadDir()
        var allDeleted = true
        dir.listFiles()?.forEach { file ->
            val deleted = file.deleteRecursively()
            if (!deleted) allDeleted = false
        }
        return allDeleted
    }

    private fun calculateDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateDirSize(file) else file.length()
        }
        return size
    }

    private fun deleteDirContents(dir: File?): Boolean {
        if (dir == null || !dir.exists()) return true
        var allDeleted = true
        dir.listFiles()?.forEach { file ->
            val deleted = file.deleteRecursively()
            if (!deleted) allDeleted = false
        }
        return allDeleted
    }

    // Device Info
    val deviceModel: String
        get() = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

    val androidVersion: String
        get() = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    val deviceAbi: String
        get() = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_AUTO_DELETE = "key_auto_delete_apk"
        private const val KEY_AUTO_RETRY = "key_auto_retry"
        private const val KEY_DOWNLOAD_NOTIF = "key_download_notifications"
        private const val KEY_WIFI_ONLY = "key_download_wifi_only"
        private const val KEY_ALLOW_MOBILE = "key_allow_mobile_data"
        private const val KEY_JS_ENABLED = "key_js_enabled"
        private const val KEY_DOM_STORAGE = "key_dom_storage"
        private const val KEY_DEEP_LINK = "key_deep_link_enabled"
        private const val KEY_AUTO_OPEN = "key_auto_open_after_install"
        private const val KEY_XAPK_AUTO_EXTRACT = "key_xapk_auto_extract"

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
