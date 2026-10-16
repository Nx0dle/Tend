package com.tend.app.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.tend.app.data.BudgetAlertEvent

class BudgetAlertNotifier(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun canNotify(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return notificationManager.areNotificationsEnabled()
    }

    fun notify(event: BudgetAlertEvent) {
        createChannelIfNeeded()
        if (event.limitReached) {
            show(
                id = "${event.monthKey}:${event.categoryId}:100".hashCode(),
                title = "Osiągnięto limit",
                message = "${event.categoryName}: wykorzystano cały miesięczny budżet.",
            )
        } else if (event.warningReached) {
            show(
                id = "${event.monthKey}:${event.categoryId}:80".hashCode(),
                title = "Zbliżasz się do limitu",
                message = "${event.categoryName}: wykorzystano co najmniej 80% miesięcznego budżetu.",
            )
        }
    }

    private fun show(id: Int, title: String, message: String) {
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build()
        try {
            notificationManager.notify(id, notification)
        } catch (_: SecurityException) {
            // Permission can be revoked between the check and delivery.
        }
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Limity budżetu", NotificationManager.IMPORTANCE_LOW).apply {
                    setSound(null, null)
                    enableVibration(false)
                },
            )
        }
    }

    private companion object {
        const val CHANNEL_ID = "budget_limits"
    }
}
