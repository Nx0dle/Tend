package com.tend.app.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.tend.app.MainActivity
import com.tend.app.data.BillReminderOccurrence
import java.text.NumberFormat
import java.util.Locale

class BillReminderNotifier(private val context: Context) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun notify(reminder: BillReminderOccurrence) {
        if (!canNotify()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Przypomnienia o rachunkach", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Powiadomienia o rachunkach wymagających zatwierdzenia"
                },
            )
        }
        val openBills = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_BILLS, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            "${reminder.billId}:${reminder.monthKey}".hashCode(),
            openBills,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle("Rachunek do zatwierdzenia")
            .setContentText("${reminder.name} · ${formatMoney(reminder.amountCents)}")
            .setStyle(Notification.BigTextStyle().bigText("${reminder.name}: ${formatMoney(reminder.amountCents)}. Otwórz Tend, aby dodać wydatek albo pominąć przypomnienie."))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify("${reminder.billId}:${reminder.monthKey}".hashCode(), notification)
        } catch (_: SecurityException) {
            // Notification access may be revoked while this reminder is delivered.
        }
    }

    private fun canNotify(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return manager.areNotificationsEnabled()
    }

    private fun formatMoney(cents: Long): String =
        NumberFormat.getCurrencyInstance(Locale("pl", "PL")).format(cents / 100.0)

    private companion object {
        const val CHANNEL_ID = "bill_reminders"
    }
}
