package com.noor.recovery

import android.app.Application
import com.noor.recovery.notification.MilestoneNotifier
import com.noor.recovery.notification.NotificationHelper
import com.noor.recovery.vpn.BlockListCache
import com.noor.recovery.vpn.BlockListUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NoorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        NotificationHelper.scheduleDaily(this)
        MilestoneNotifier.createChannel(this)

        // حمّل القوائم المحلية فوراً في الذاكرة
        BlockListCache.reload(this)

        // حدّث من الإنترنت في الخلفية إذا مضى أسبوع
        CoroutineScope(Dispatchers.IO).launch {
            BlockListUpdater.updateIfNeeded(this@NoorApplication)
        }
    }
}
