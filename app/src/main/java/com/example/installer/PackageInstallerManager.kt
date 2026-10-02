package com.example.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.model.XapkComponent
import com.example.model.XapkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipFile

object PackageInstallerManager {

    private const val TAG = "PackageInstallerMgr"

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openUnknownSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun installApk(context: Context, apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists()) {
                return Result.failure(IllegalArgumentException("APK file does not exist: ${apkFile.absolutePath}"))
            }

            if (!canRequestPackageInstalls(context)) {
                openUnknownSourcesSettings(context)
                return Result.failure(SecurityException("Please grant permission to install unknown apps for Cosmo Game Store"))
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
            Result.failure(e)
        }
    }

    suspend fun parseXapk(xapkFile: File): Result<XapkInfo> = withContext(Dispatchers.IO) {
        try {
            if (!xapkFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("File does not exist"))
            }

            val zip = ZipFile(xapkFile)
            var manifestPackageName: String? = null
            var manifestVersionName: String? = null
            var manifestVersionCode: Long? = null
            var manifestMinSdk: Int? = null

            // 1. Look for manifest.json
            val manifestEntry = zip.getEntry("manifest.json")
            if (manifestEntry != null) {
                zip.getInputStream(manifestEntry).bufferedReader().use { reader ->
                    val text = reader.readText()
                    try {
                        val json = JSONObject(text)
                        manifestPackageName = json.optString("package_name").takeIf { it.isNotBlank() }
                        manifestVersionName = json.optString("version_name").takeIf { it.isNotBlank() }
                        manifestVersionCode = json.optLong("version_code").takeIf { it > 0 }
                        manifestMinSdk = json.optInt("min_sdk_version").takeIf { it > 0 }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse manifest.json inside XAPK", e)
                    }
                }
            }

            val components = mutableListOf<XapkComponent>()
            var apkCount = 0
            var obbCount = 0
            var totalSize = 0L

            for (entry in zip.entries()) {
                val name = entry.name
                val size = entry.size
                totalSize += if (size > 0) size else 0L

                val isApk = name.endsWith(".apk", ignoreCase = true)
                val isObb = name.endsWith(".obb", ignoreCase = true)

                if (isApk) apkCount++
                if (isObb) obbCount++

                if (isApk || isObb) {
                    components.add(
                        XapkComponent(
                            entryName = name,
                            isApk = isApk,
                            isObb = isObb,
                            sizeBytes = size
                        )
                    )
                }
            }
            zip.close()

            Result.success(
                XapkInfo(
                    packageName = manifestPackageName,
                    versionName = manifestVersionName,
                    versionCode = manifestVersionCode,
                    minSdkVersion = manifestMinSdk,
                    components = components,
                    totalApks = apkCount,
                    totalObbs = obbCount,
                    totalUncompressedSize = totalSize
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse XAPK", e)
            Result.failure(e)
        }
    }

    suspend fun installXapk(
        context: Context,
        xapkFile: File,
        onProgress: ((String) -> Unit)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!canRequestPackageInstalls(context)) {
                withContext(Dispatchers.Main) {
                    openUnknownSourcesSettings(context)
                }
                return@withContext Result.failure(SecurityException("Please grant permission to install unknown apps"))
            }

            onProgress?.invoke("Inspecting XAPK contents...")
            val xapkInfo = parseXapk(xapkFile).getOrNull()
            val packageName = xapkInfo?.packageName

            // Extract OBB if present
            onProgress?.invoke("Checking OBB expansion files...")
            extractObbIfPresent(context, xapkFile, packageName)

            // If only one APK exists inside, we can extract and install via FileProvider,
            // OR if multiple APKs exist, use PackageInstaller.Session
            val zip = ZipFile(xapkFile)
            val apkEntries = zip.entries().asSequence()
                .filter { it.name.endsWith(".apk", ignoreCase = true) }
                .toList()

            if (apkEntries.isEmpty()) {
                zip.close()
                return@withContext Result.failure(IllegalStateException("No .apk found inside XAPK"))
            }

            if (apkEntries.size == 1) {
                // Single APK: extract to cache and launch official installer
                onProgress?.invoke("Extracting APK...")
                val entry = apkEntries[0]
                val cacheDir = File(context.cacheDir, "extracted_apks").apply { mkdirs() }
                val tempApk = File(cacheDir, entry.name.substringAfterLast("/"))
                zip.getInputStream(entry).use { input ->
                    FileOutputStream(tempApk).use { output ->
                        input.copyTo(output)
                    }
                }
                zip.close()

                withContext(Dispatchers.Main) {
                    installApk(context, tempApk)
                }
                Result.success(Unit)
            } else {
                // Multi-APK (split APKs): use official Android PackageInstaller Session
                onProgress?.invoke("Opening official Package Installer session...")
                val packageInstaller = context.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                if (!packageName.isNullOrBlank()) {
                    params.setAppPackageName(packageName)
                }

                val sessionId = packageInstaller.createSession(params)
                val session = packageInstaller.openSession(sessionId)

                apkEntries.forEachIndexed { index, entry ->
                    val cleanName = "split_${index}_${entry.name.substringAfterLast("/").replace("[^a-zA-Z0-9_.]".toRegex(), "_")}"
                    onProgress?.invoke("Staging ${entry.name.substringAfterLast("/")}...")
                    val out = session.openWrite(cleanName, 0, entry.size)
                    zip.getInputStream(entry).use { inStream ->
                        inStream.copyTo(out)
                    }
                    session.fsync(out)
                    out.close()
                }
                zip.close()

                val intent = Intent(context, InstallResultReceiver::class.java).apply {
                    action = InstallResultReceiver.ACTION_INSTALL_RESULT
                    putExtra(InstallResultReceiver.EXTRA_SESSION_ID, sessionId)
                    putExtra(InstallResultReceiver.EXTRA_PACKAGE_NAME, packageName)
                }
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    intent,
                    flags
                )

                onProgress?.invoke("Triggering official confirmation...")
                session.commit(pendingIntent.intentSender)
                session.close()

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install XAPK", e)
            Result.failure(e)
        }
    }

    private fun extractObbIfPresent(context: Context, xapkFile: File, packageName: String?) {
        try {
            val zip = ZipFile(xapkFile)
            val obbEntries = zip.entries().asSequence()
                .filter { it.name.endsWith(".obb", ignoreCase = true) }
                .toList()

            if (obbEntries.isNotEmpty()) {
                val targetPkg = packageName ?: "common"
                // Preferred external obb path
                val extObbDir = File(Environment.getExternalStorageDirectory(), "Android/obb/$targetPkg")
                val fallbackObbDir = File(context.getExternalFilesDir(null), "obb/$targetPkg")
                val destDir = if (extObbDir.canWrite() || extObbDir.mkdirs()) extObbDir else fallbackObbDir
                destDir.mkdirs()

                for (obb in obbEntries) {
                    val destFile = File(destDir, obb.name.substringAfterLast("/"))
                    if (!destFile.exists()) {
                        zip.getInputStream(obb).use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
            zip.close()
        } catch (e: Exception) {
            Log.w(TAG, "OBB extraction notice: ${e.message}")
        }
    }

    fun isPackageInstalled(context: Context, packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun openApp(context: Context, packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app $packageName", e)
            false
        }
    }

    fun getPackageNameFromApk(context: Context, apkFile: File): String? {
        return try {
            val pi = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            pi?.packageName
        } catch (e: Exception) {
            null
        }
    }
}
