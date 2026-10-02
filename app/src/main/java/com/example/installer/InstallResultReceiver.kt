package com.example.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log

class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_INSTALL_RESULT) {
            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
            val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
            val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)

            when (status) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                    if (confirmIntent != null) {
                        confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(confirmIntent)
                    }
                }
                PackageInstaller.STATUS_SUCCESS -> {
                    Log.d("InstallReceiver", "Package installed successfully: $packageName")
                    notifyInstallSuccess(context, packageName)
                }
                else -> {
                    Log.e("InstallReceiver", "Package installation failed ($status): $message")
                }
            }
        }
    }

    private fun notifyInstallSuccess(context: Context, packageName: String?) {
        val notifyIntent = Intent(ACTION_PACKAGE_INSTALLED_NOTIFY).apply {
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            setPackage(context.packageName)
        }
        context.sendBroadcast(notifyIntent)
    }

    companion object {
        const val ACTION_INSTALL_RESULT = "com.aistudio.cosmogamestore.INSTALL_RESULT"
        const val ACTION_PACKAGE_INSTALLED_NOTIFY = "com.aistudio.cosmogamestore.PACKAGE_INSTALLED"
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }
}
