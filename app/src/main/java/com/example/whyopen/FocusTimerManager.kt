package com.example.whyopen

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object FocusTimerManager {
    private const val CHANNEL_ID = "focus_timer_channel"

    fun getOrCreateChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Timer",
                NotificationManager.IMPORTANCE_LOW // Low for constant monitoring
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildMonitoringNotification(context: Context, packageName: String): android.app.Notification {
        getOrCreateChannel(context)
        val appName = try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) { packageName }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Focusing on $appName")
            .setContentText("Your session is being timed. Stay intentional!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    fun showTimeExpiredNotification(context: Context, packageName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        getOrCreateChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Time's Up!")
            .setContentText("Your scheduled time for $packageName has ended. Stay focused!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, notification)
    }
}
