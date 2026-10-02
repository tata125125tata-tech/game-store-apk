package com.example.model

import android.graphics.drawable.Drawable

data class InstalledGame(
    val packageName: String,
    val appName: String,
    val versionName: String?,
    val versionCode: Long,
    val iconDrawable: Drawable?,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isFromCosmo: Boolean = false,
    val sizeBytes: Long = 0L
)
