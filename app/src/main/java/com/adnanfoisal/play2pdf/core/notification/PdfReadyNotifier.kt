package com.adnanfoisal.play2pdf.core.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.adnanfoisal.play2pdf.MainActivity
import com.adnanfoisal.play2pdf.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts the "PDF ready" notification when a compile finishes.
 *
 * Per Phase G native-Android checklist: "notification channel for
 * 'PDF ready'". The channel itself is registered in [com.adnanfoisal.play2pdf.Play2PdfApp].
 */
@Singleton
class PdfReadyNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun notify(subject: String) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, context.getString(R.string.notification_channel_id))
            .setSmallIcon(R.drawable.splash_icon) // placeholder until Asset D is delivered
            .setContentTitle("Study guide ready")
            .setContentText("Your \"$subject\" PDF is ready to view.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        mgr.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
