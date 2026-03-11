package com.noor.recovery.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noor.recovery.data.Milestones
import com.noor.recovery.data.RecoveryRepository
import com.noor.recovery.data.RecoverySession
import com.noor.recovery.notification.MilestoneNotifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TimerState(
    val days: Long = 0,
    val hours: Long = 0,
    val minutes: Long = 0,
    val seconds: Long = 0,
    val totalHours: Double = 0.0,
    val ringProgress: Float = 0f    // 0..1 داخل الدورة الحالية (30 يوم)
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = RecoveryRepository(application)
    private val prefs = application.getSharedPreferences("noor_tamper", Context.MODE_PRIVATE)

    private val _session = MutableStateFlow<RecoverySession?>(null)
    val session: StateFlow<RecoverySession?> = _session.asStateFlow()

    private val _timer = MutableStateFlow(TimerState())
    val timer: StateFlow<TimerState> = _timer.asStateFlow()

    private val _completedMilestones = MutableStateFlow<Set<String>>(emptySet())
    val completedMilestones: StateFlow<Set<String>> = _completedMilestones.asStateFlow()

    private var tickJob: Job? = null

    init {
        viewModelScope.launch {
            val s = repo.sessionFlow.first()
            _session.value = s
            if (s != null) startTicking()
        }
    }

    fun startSession(addictionId: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val session = RecoverySession(addictionId, now)
            repo.saveSession(session)
            saveTamperCheckpoint(now, android.os.SystemClock.elapsedRealtime())
            _session.value = session
            startTicking()
        }
    }

    fun resetSession() {
        viewModelScope.launch {
            tickJob?.cancel()
            repo.clearSession()
            prefs.edit().clear().apply()
            _session.value = null
            _timer.value = TimerState()
            _completedMilestones.value = emptySet()
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                val session = _session.value ?: break

                // ── حساب الوقت الآمن ──────────────────────────────
                val safeElapsedMs = safeElapsed(session.startTimeMs)

                val totalSec = safeElapsedMs / 1000L
                val days     = totalSec / 86400
                val hours    = (totalSec % 86400) / 3600
                val minutes  = (totalSec % 3600) / 60
                val seconds  = totalSec % 60
                val totalHours = safeElapsedMs / 3_600_000.0

                // ── ringProgress: يدور كل 30 يوم بدل أن يتجمد ────
                val dayInCycle = days % 30
                val ringProgress = (dayInCycle / 30f).coerceIn(0f, 1f)

                _timer.value = TimerState(days, hours, minutes, seconds, totalHours, ringProgress)

                // ── فحص المراحل الجديدة وإرسال إشعار فوري ─────────
                val nowDone = Milestones.all
                    .filter { totalHours >= it.requiredHours }
                    .map { it.id }
                    .toSet()

                val newlyReached = nowDone - _completedMilestones.value
                if (newlyReached.isNotEmpty()) {
                    _completedMilestones.value = nowDone
                    newlyReached.forEach { milestoneId ->
                        Milestones.all.find { it.id == milestoneId }?.let { milestone ->
                            MilestoneNotifier.notify(getApplication(), milestone)
                        }
                    }
                }

                delay(1000)
            }
        }
    }

    // ── حساب آمن يكتشف تغيير ساعة الجهاز ───────────────────────────
    private fun safeElapsed(startTimeMs: Long): Long {
        val nowWall = System.currentTimeMillis()
        val nowBoot = android.os.SystemClock.elapsedRealtime()

        val cpWall = prefs.getLong("cp_wall", startTimeMs)
        val cpBoot = prefs.getLong("cp_boot", nowBoot)

        val elapsedWall = nowWall - cpWall
        val elapsedBoot = nowBoot - cpBoot

        // إذا تجاوز الفارق دقيقة — يُرجَّح التلاعب → استخدم وقت الإقلاع
        val elapsedSinceCheckpoint = if (elapsedWall > elapsedBoot + 60_000L) {
            elapsedBoot
        } else {
            elapsedWall
        }

        val totalSafe = ((cpWall - startTimeMs) + elapsedSinceCheckpoint).coerceAtLeast(0L)

        // حدّث نقطة التحقق كل 5 دقائق
        if (nowBoot - cpBoot > 5 * 60_000L) {
            saveTamperCheckpoint(nowWall, nowBoot)
        }

        return totalSafe
    }

    private fun saveTamperCheckpoint(wallMs: Long, bootMs: Long) {
        prefs.edit()
            .putLong("cp_wall", wallMs)
            .putLong("cp_boot", bootMs)
            .apply()
    }
}
