package com.example.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class SettingsManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cosmo_settings_prefs", Context.MODE_PRIVATE)

    private val _autoDeleteApk = MutableStateFlow(prefs.getBoolean(KEY_AUTO_DELETE, false))
    val autoDeleteApk: StateFlow<Boolean> = _autoDeleteApk.asStateFlow()

    private val _downloadWifiOnly = MutableStateFlow(prefs.getBoolean(KEY_WIFI_ONLY, false))
    val downloadWifiOnly: StateFlow<Boolean> = _downloadWifiOnly.asStateFlow()

    private val _xapkAutoExtract = MutableStateFlow(prefs.getBoolean(KEY_XAPK_AUTO_EXTRACT, true))
    val xapkAutoExtract: StateFlow<Boolean> = _xapkAutoExtract.asStateFlow()

    private val _cosmoTheme = MutableStateFlow(prefs.getString(KEY_THEME, "cosmic_dark") ?: "cosmic_dark")
    val cosmoTheme: StateFlow<String> = _cosmoTheme.asStateFlow()

    fun setAutoDeleteApk(value: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_DELETE, value).apply()
        _autoDeleteApk.value = value
    }

    fun setDownloadWifiOnly(value: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()
        _downloadWifiOnly.value = value
    }

    fun setXapkAutoExtract(value: Boolean) {
        prefs.edit().putBoolean(KEY_XAPK_AUTO_EXTRACT, value).apply()
        _xapkAutoExtract.value = value
    }

    fun setCosmoTheme(value: String) {
        prefs.edit().putString(KEY_THEME, value).apply()
        _cosmoTheme.value = value
    }

    fun getDownloadDir(): File {
        val external = context.getExternalFilesDir("downloads")
        return external ?: File(context.filesDir, "downloads").apply { mkdirs() }
    }

    fun getDownloadedFilesSize(): Long {
        val dir = getDownloadDir()
        return calculateDirSize(dir)
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        if (!dir.exists()) return 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateDirSize(file) else file.length()
        }
        return size
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

    companion object {
        private const val KEY_AUTO_DELETE = "key_auto_delete_apk"
        private const val KEY_WIFI_ONLY = "key_download_wifi_only"
        private const val KEY_XAPK_AUTO_EXTRACT = "key_xapk_auto_extract"
        private const val KEY_THEME = "key_cosmo_theme"

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
