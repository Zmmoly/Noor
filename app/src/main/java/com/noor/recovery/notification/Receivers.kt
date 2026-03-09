package com.noor.recovery.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.noor.recovery.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messages = listOf(
            "أنت قوي 💪 استمر في رحلتك نحو الحرية.",
            "كل يوم تصمد فيه هو انتصار حقيقي 🌟",
            "تذكّر: الرغبة مؤقتة، والتعافي دائم 🌅",
            "فخور بك — أنت تكتب قصة نجاحك ✨"
        )
        val msg = messages.random()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("نور — رحلة التعافي")
            .setContentText(msg)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(1, notification)
    }

    companion object {
        const val CHANNEL_ID = "noor_reminders"
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationHelper.scheduleDaily(context)
        }
    }
}
