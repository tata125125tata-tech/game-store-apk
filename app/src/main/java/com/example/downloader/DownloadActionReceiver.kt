package com.example.downloader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DownloadActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getStringExtra(EXTRA_DOWNLOAD_ID) ?: return
        val downloadManager = CosmoDownloadManager.getInstance(context)

        Log.d(TAG, "Received action ${intent.action} for download $downloadId")

        when (intent.action) {
            ACTION_STOP -> {
                downloadManager.pauseDownload(downloadId)
            }
            ACTION_RESUME -> {
                downloadManager.resumeDownload(downloadId)
            }
            ACTION_CANCEL -> {
                downloadManager.cancelDownload(downloadId, deleteFile = true)
            }
        }
    }

    companion object {
        private const val TAG = "DownloadActionReceiver"
        const val ACTION_STOP = "com.example.downloader.ACTION_STOP"
        const val ACTION_RESUME = "com.example.downloader.ACTION_RESUME"
        const val ACTION_CANCEL = "com.example.downloader.ACTION_CANCEL"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
    }
}
