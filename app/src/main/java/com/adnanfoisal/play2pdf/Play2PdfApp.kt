package com.adnanfoisal.play2pdf

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * Registers the notification channel for "PDF ready" notifications and
 * serves as the Hilt application scope.
 *
 * Hilt graph is built lazily on first injection — see [com.adnanfoisal.play2pdf.di]
 * for the modules.
 */
@HiltAndroidApp
class Play2PdfApp : Application() {

    override fun onCreate() {
        super.onCreate()
        registerNotificationChannel()
    }

    /**
     * Channel used by [com.adnanfoisal.play2pdf.core.notification.PdfReadyNotifier]
     * to post the "PDF ready" notification when a compile finishes in the
     * background (or when the user navigates away from the Compiling screen).
     */
    private fun registerNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val desc = getString(R.string.notification_channel_desc)
            val channel = NotificationChannel(
                getString(R.string.notification_channel_id),
                name,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = desc }
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(channel)
        }
    }
}
