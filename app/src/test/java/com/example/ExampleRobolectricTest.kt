package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.FileType
import com.example.settings.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Cosmo Game Store", appName)
    }

    @Test
    fun `download item progress calculation`() {
        val item = DownloadItem(
            id = "test-1",
            title = "Test Game",
            originalUrl = "https://example.com/game.apk",
            fileType = FileType.APK,
            localFilePath = "/tmp/game.apk",
            totalBytes = 1000L,
            downloadedBytes = 500L,
            status = DownloadStatus.DOWNLOADING
        )

        assertEquals(0.5f, item.progress, 0.01f)
        assertEquals(50, item.progressPercent)
        assertEquals("500 B", item.formattedDownloaded)
        assertEquals("1000 B", item.formattedTotal)
    }

    @Test
    fun `file size formatting helper`() {
        assertEquals("0 B", DownloadItem.formatFileSize(0))
        assertEquals("500 B", DownloadItem.formatFileSize(500))
        assertEquals("1.0 KB", DownloadItem.formatFileSize(1024))
        assertEquals("1.0 MB", DownloadItem.formatFileSize(1024 * 1024))
        assertEquals("1.50 GB", DownloadItem.formatFileSize((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun `settings manager preferences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = SettingsManager.getInstance(context)

        settings.setAutoDeleteApk(true)
        assertTrue(settings.autoDeleteApk.value)

        settings.setDownloadWifiOnly(true)
        assertTrue(settings.downloadWifiOnly.value)

        val downloadDir = settings.getDownloadDir()
        assertNotNull(downloadDir)
    }
}
