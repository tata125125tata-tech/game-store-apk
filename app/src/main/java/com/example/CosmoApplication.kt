package com.example

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.example.downloader.CosmoDownloadManager
import com.example.installer.InstallResultReceiver
import com.example.settings.SettingsManager
import java.io.File

class CosmoApplication : Application() {

    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val packageName = when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REPLACED -> {
                    intent.data?.schemeSpecificPart
                }
                InstallResultReceiver.ACTION_PACKAGE_INSTALLED_NOTIFY -> {
                    intent.getStringExtra(InstallResultReceiver.EXTRA_PACKAGE_NAME)
                }
                else -> null
            }

            if (!packageName.isNullOrBlank()) {
                Log.d("CosmoApp", "Detected installed package: $packageName")
                val downloadManager = CosmoDownloadManager.getInstance(context)
                downloadManager.markPackageInstalled(packageName)
                downloadManager.refreshInstalledStatus()

                // Check auto-delete setting
                val settings = SettingsManager.getInstance(context)
                if (settings.autoDeleteApk.value) {
                    val matching = downloadManager.downloads.value.filter { it.packageName == packageName }
                    matching.forEach { item ->
                        try {
                            val f = File(item.localFilePath)
                            if (f.exists()) f.delete()
                        } catch (e: Exception) {
                            Log.w("CosmoApp", "Failed to auto-delete file for $packageName", e)
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Register package installation receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(packageChangeReceiver, filter)

        // Custom install notification broadcast
        val internalFilter = IntentFilter(InstallResultReceiver.ACTION_PACKAGE_INSTALLED_NOTIFY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageChangeReceiver, internalFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(packageChangeReceiver, internalFilter)
        }

        // Initialize singletons
        CosmoDownloadManager.getInstance(this)
        SettingsManager.getInstance(this)
    }
}
