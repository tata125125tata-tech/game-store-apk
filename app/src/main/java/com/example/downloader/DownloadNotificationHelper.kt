package com.example.downloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.model.DownloadItem
import com.example.model.FileType

class DownloadNotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel = NotificationChannel(
                CHANNEL_PROGRESS_ID,
                "Active Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time download progress for APK and XAPK packages"
                setShowBadge(false)
            }

            val completedChannel = NotificationChannel(
                CHANNEL_COMPLETE_ID,
                "Completed Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for completed and failed game downloads"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(completedChannel)
        }
    }

    private fun getNotificationId(downloadId: String): Int {
        return downloadId.hashCode() and 0x7FFFFFFF
    }

    fun updateProgressNotification(item: DownloadItem) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val typeText = if (item.fileType == FileType.XAPK) "XAPK" else "APK"
        val progressText = if (item.formattedSpeed.isNotBlank()) {
            "${item.progressPercent}% · ${item.formattedDownloaded} / ${item.formattedTotal} · ${item.formattedSpeed}"
        } else {
            "${item.progressPercent}% · ${item.formattedDownloaded} / ${item.formattedTotal}"
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, DESTINATION_DOWNLOADS)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            getNotificationId(item.id),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_PROGRESS_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("${item.title} ($typeText)")
            .setContentText(progressText)
            .setProgress(100, item.progressPercent, item.totalBytes <= 0)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        notificationManager.notify(getNotificationId(item.id), builder.build())
    }

    fun showDownloadCompleteNotification(item: DownloadItem) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        // Dismiss progress notification
        notificationManager.cancel(getNotificationId(item.id))

        val typeText = if (item.fileType == FileType.XAPK) "XAPK" else "APK"

        val openLibraryIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, DESTINATION_LIBRARY)
        }
        val openLibraryPendingIntent = PendingIntent.getActivity(
            context,
            getNotificationId(item.id) + 1,
            openLibraryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_COMPLETE_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("${item.title} ($typeText)")
            .setContentText("Download complete · ${item.formattedTotal} · Tap to install")
            .setContentIntent(openLibraryPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_agenda,
                "Open My Library",
                openLibraryPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(getNotificationId(item.id), builder.build())
    }

    fun showDownloadFailedNotification(item: DownloadItem, onRetryPendingIntent: PendingIntent? = null) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        notificationManager.cancel(getNotificationId(item.id))

        val typeText = if (item.fileType == FileType.XAPK) "XAPK" else "APK"

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, DESTINATION_DOWNLOADS)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            getNotificationId(item.id) + 2,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_COMPLETE_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("${item.title} ($typeText)")
            .setContentText("Download failed: ${item.errorMessage ?: "Network error"}")
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (onRetryPendingIntent != null) {
            builder.addAction(
                android.R.drawable.ic_popup_sync,
                "Retry",
                onRetryPendingIntent
            )
        }

        notificationManager.notify(getNotificationId(item.id), builder.build())
    }

    fun cancelNotification(id: String) {
        notificationManager.cancel(getNotificationId(id))
    }

    companion object {
        const val CHANNEL_PROGRESS_ID = "cosmo_downloads_progress"
        const val CHANNEL_COMPLETE_ID = "cosmo_downloads_complete"
        const val EXTRA_DESTINATION = "extra_cosmo_destination"
        const val DESTINATION_LIBRARY = "library"
        const val DESTINATION_DOWNLOADS = "downloads"
    }
}
