package com.noor.recovery

import android.app.Application
import com.noor.recovery.notification.NotificationHelper

class NoorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        NotificationHelper.scheduleDaily(this)
    }
}
