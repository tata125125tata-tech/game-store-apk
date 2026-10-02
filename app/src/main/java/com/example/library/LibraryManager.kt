package com.example.library

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.downloader.CosmoDownloadManager
import com.example.model.InstalledGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object LibraryManager {

    private const val TAG = "LibraryManager"

    suspend fun loadInstalledGames(context: Context): List<InstalledGame> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val result = mutableListOf<InstalledGame>()

        try {
            val downloadManager = CosmoDownloadManager.getInstance(context)
            val cosmoPackages = downloadManager.downloads.value
                .mapNotNull { it.packageName }
                .toSet()

            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val launchableApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(launcherIntent, 0)
            }

            val seenPackages = mutableSetOf<String>()

            for (resolveInfo in launchableApps) {
                val pkgName = resolveInfo.activityInfo.packageName
                if (pkgName == context.packageName) continue // Skip self
                if (seenPackages.contains(pkgName)) continue
                seenPackages.add(pkgName)

                try {
                    val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getApplicationInfo(pkgName, PackageManager.ApplicationInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getApplicationInfo(pkgName, 0)
                    }

                    // Check if it's a game or downloaded via Cosmo
                    val isGame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        appInfo.category == ApplicationInfo.CATEGORY_GAME
                    } else {
                        false
                    }

                    val isCosmoApp = cosmoPackages.contains(pkgName)
                    val isUserApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0

                    // Include if it's a game, installed from Cosmo, or a non-system user app
                    if (isGame || isCosmoApp || isUserApp) {
                        val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pm.getPackageInfo(pkgName, PackageManager.PackageInfoFlags.of(0))
                        } else {
                            @Suppress("DEPRECATION")
                            pm.getPackageInfo(pkgName, 0)
                        }

                        val appName = pm.getApplicationLabel(appInfo).toString()
                        val icon = try {
                            pm.getApplicationIcon(appInfo)
                        } catch (e: Exception) {
                            null
                        }

                        val apkSize = try {
                            File(appInfo.sourceDir).length()
                        } catch (e: Exception) {
                            0L
                        }

                        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            pkgInfo.longVersionCode
                        } else {
                            @Suppress("DEPRECATION")
                            pkgInfo.versionCode.toLong()
                        }

                        result.add(
                            InstalledGame(
                                packageName = pkgName,
                                appName = appName,
                                versionName = pkgInfo.versionName,
                                versionCode = versionCode,
                                iconDrawable = icon,
                                firstInstallTime = pkgInfo.firstInstallTime,
                                lastUpdateTime = pkgInfo.lastUpdateTime,
                                isFromCosmo = isCosmoApp,
                                sizeBytes = apkSize
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error fetching package info for $pkgName", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading games", e)
        }

        // Sort: Cosmo installed first, then newest
        result.sortedWith(
            compareByDescending<InstalledGame> { it.isFromCosmo }
                .thenByDescending { it.lastUpdateTime }
        )
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $packageName", e)
            false
        }
    }

    fun uninstallApp(context: Context, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to uninstall $packageName", e)
        }
    }

    fun openAppDetails(context: Context, packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app details for $packageName", e)
        }
    }
}
