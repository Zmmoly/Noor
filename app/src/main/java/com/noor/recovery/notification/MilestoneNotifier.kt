package com.noor.recovery.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.noor.recovery.R
import com.noor.recovery.data.Milestone

object MilestoneNotifier {

    private const val CHANNEL_ID = "noor_milestones"

    fun createChannel(context: Context) {
        val channel = android.app.NotificationChannel(
            CHANNEL_ID,
            "إنجازات التعافي",
            android.app.NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "إشعار فوري عند بلوغ كل مرحلة جديدة"
            enableVibration(true)
        }
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun notify(context: Context, milestone: Milestone) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${milestone.emoji}  إنجاز جديد!")
            .setContentText("وصلت إلى: ${milestone.titleAr} — ${milestone.subtitleAr}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        // معرف فريد لكل مرحلة حتى لا تُلغي إشعاراً سابقاً
        val notifId = milestone.id.hashCode()
        NotificationManagerCompat.from(context).notify(notifId, notification)
    }
}
